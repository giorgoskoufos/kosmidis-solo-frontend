package com.example.myapplication;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.noties.markwon.Markwon;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private EditText userInput;
    private Button sendButton;
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private NavigationView navigationView;

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private List<MessageModel> messageList = new ArrayList<>();

    // Welcome Screen UI elements
    private LinearLayout welcomeLayout;
    private View cardGooglePrompt;

    private int currentConversationId = 1;
    private int currentUserId = -1;

    private Markwon markwon;

    // API Setup
    private final String BASE_URL = "https://app-kosmidis-solo-backend.xadp6y.easypanel.host";

    // Increased timeouts to allow the AI Agent to process external tools without failing
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userInput = findViewById(R.id.userInput);
        sendButton = findViewById(R.id.sendButton);
        drawerLayout = findViewById(R.id.drawer_layout);
        toolbar = findViewById(R.id.toolbar);
        navigationView = findViewById(R.id.nav_view);

        welcomeLayout = findViewById(R.id.welcomeLayout);
        cardGooglePrompt = findViewById(R.id.cardGooglePrompt);

        // --- 1. READ USER DATA ---
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        currentUserId = prefs.getInt("userId", -1);
        String userEmail = prefs.getString("userEmail", "Unknown User");

        if (currentUserId == -1) {
            // Redirect to Login if user ID is missing
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // --- 3. CHECK GOOGLE INTEGRATION STATUS FROM SERVER ---
        // Fetch the true integration status from the database to keep UI in sync
        checkGoogleIntegrationStatus();

        // --- 2. UPDATE EMAIL IN SIDEBAR ---
        View headerView = navigationView.getHeaderView(0);
        TextView tvHeaderEmail = headerView.findViewById(R.id.tvHeaderEmail);
        if (tvHeaderEmail != null) {
            tvHeaderEmail.setText(userEmail);
        }

        // Initialize Markwon for Markdown rendering
        markwon = Markwon.create(this);

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        chatAdapter = new ChatAdapter(messageList, markwon);
        chatRecyclerView.setAdapter(chatAdapter);

        // Sidebar toggle listener
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Sidebar item click listener
        navigationView.setNavigationItemSelectedListener(item -> {
            drawerLayout.closeDrawer(GravityCompat.START);

            if (item.getItemId() == R.id.nav_new_chat) {
                createNewConversation();
            } else if (item.getItemId() == R.id.nav_settings) {
                SettingsMenuDialog settingsDialog = new SettingsMenuDialog();
                settingsDialog.show(getSupportFragmentManager(), "SettingsMenuDialog");
            } else if (item.getItemId() == R.id.nav_logout) {
                // Logout logic
                SharedPreferences preferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                preferences.edit().clear().apply();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            } else {
                // Fetch previous conversation
                currentConversationId = item.getItemId();
                fetchMessages(currentConversationId);
            }
            return true;
        });

        // Send Button Listener
        sendButton.setOnClickListener(v -> {
            String message = userInput.getText().toString();

            if (!message.trim().isEmpty()) {
                // Lock UI and hide keyboard to prevent ANRs
                sendButton.setEnabled(false);
                userInput.setEnabled(false);
                hideKeyboard();

                addMessageToList(message, true, false);
                userInput.setText("");

                new Handler().postDelayed(() -> {
                    addMessageToList("", false, true); // Show typing indicator
                    sendMessageToServer(message);
                }, 400);
            }
        });

        // Click listener for the Google Sync prompt on the empty state screen
        cardGooglePrompt.setOnClickListener(v -> {
            IntegrationsGoogle integrationsGoogle = new IntegrationsGoogle();
            integrationsGoogle.show(getSupportFragmentManager(), "IntegrationsGoogle");
        });

        // Fetch user's conversation history
        fetchConversations();

        // Create a new conversation by default on startup
        createNewConversation();

        // Check if the activity was launched from a deep link (Cold Start)
        handleDeepLink(getIntent());
    }

    // This method catches the intent if the activity is already running in the background (Warm Start)
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDeepLink(intent);
    }

    private void handleDeepLink(Intent intent) {
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri uri = intent.getData();

            // Verify that this is our specific OAuth callback URI
            if (uri != null && "jarvisapp".equals(uri.getScheme()) && "oauth2redirect".equals(uri.getHost())) {

                String status = uri.getQueryParameter("status");
                String isWriteEnabled = uri.getQueryParameter("write");

                if ("success".equals(status)) {
                    // Update SharedPreferences so the prompt disappears permanently
                    SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                    prefs.edit().putBoolean("isGoogleConnected", true).apply();

                    // Refresh UI to hide the card immediately
                    updateWelcomeScreenVisibility();

                    String accessLevel = "true".equals(isWriteEnabled) ? "Full Access (Read & Write)" : "Read Only Access";

                    // Show a modern Material Design dialog confirming the connection
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Connection Successful")
                            .setMessage("J.A.R.V.I.S. is now connected to your Google Workspace with " + accessLevel + ".")
                            .setPositiveButton("Got it", (dialog, which) -> dialog.dismiss())
                            .show();
                } else {
                    Toast.makeText(this, "Failed to connect Google Account.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    // --- HELPER METHODS ---
    private void checkGoogleIntegrationStatus() {
        // Build the URL using the current user's ID
        String url = BASE_URL + "/api/users/" + currentUserId + "/integrations/google/status";

        Request request = new Request.Builder().url(url).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Silently fail if there's a network error, the user can try later
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONObject json = new JSONObject(responseData);
                        boolean isConnected = json.getBoolean("isConnected");

                        // Update the local SharedPreferences with the ultimate truth from the Server
                        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                        prefs.edit().putBoolean("isGoogleConnected", isConnected).apply();

                        // Refresh the UI to hide the prompt if they are connected
                        updateWelcomeScreenVisibility();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
    /**
     * Toggles the Welcome Screen and the Google Prompt based on Chat History and User Status
     */
    private void updateWelcomeScreenVisibility() {
        runOnUiThread(() -> {
            if (messageList.isEmpty()) {
                welcomeLayout.setVisibility(View.VISIBLE);
                chatRecyclerView.setVisibility(View.GONE);

                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                String email = prefs.getString("userEmail", "");
                boolean isGoogleConnected = prefs.getBoolean("isGoogleConnected", false);

                // Show the card ONLY if the user is a Gmail user and hasn't connected yet
                if (email.toLowerCase().endsWith("@gmail.com") && !isGoogleConnected) {
                    cardGooglePrompt.setVisibility(View.VISIBLE);
                } else {
                    cardGooglePrompt.setVisibility(View.GONE);
                }
            } else {
                welcomeLayout.setVisibility(View.GONE);
                chatRecyclerView.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * Hides the software keyboard to prevent the UI from freezing (ANR)
     * while the user waits for the AI response.
     */
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Safely removes the "Typing..." indicator from the RecyclerView.
     */
    private void removeTypingIndicator() {
        if (!messageList.isEmpty() && messageList.get(messageList.size() - 1).isTyping) {
            int removeIndex = messageList.size() - 1;
            messageList.remove(removeIndex);
            chatAdapter.notifyItemRemoved(removeIndex);
        }
    }

    private void addMessageToList(String message, boolean isUser, boolean isTyping) {
        runOnUiThread(() -> {
            messageList.add(new MessageModel(message, isUser, isTyping));
            chatAdapter.notifyItemInserted(messageList.size() - 1);
            chatRecyclerView.smoothScrollToPosition(messageList.size() - 1);

            // Check if we need to hide the welcome screen since a message was added
            updateWelcomeScreenVisibility();
        });
    }

    // --- API CALLS ---

    private void fetchConversations() {
        String url = BASE_URL + "/api/conversations/" + currentUserId;
        Request request = new Request.Builder().url(url).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseData);

                        runOnUiThread(() -> {
                            android.view.Menu menu = navigationView.getMenu();
                            android.view.MenuItem historyItem = menu.findItem(R.id.nav_history_parent);

                            if (historyItem != null) {
                                android.view.SubMenu subMenu = historyItem.getSubMenu();
                                subMenu.clear(); // Clear old items

                                try {
                                    // Dynamically add new history items
                                    for (int i = 0; i < jsonArray.length(); i++) {
                                        JSONObject chatObj = jsonArray.getJSONObject(i);
                                        int convId = chatObj.getInt("id");
                                        String title = chatObj.getString("title");

                                        subMenu.add(R.id.group_history, convId, android.view.Menu.NONE, title);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void fetchMessages(int conversationId) {
        String url = BASE_URL + "/api/messages/" + conversationId;
        Request request = new Request.Builder().url(url).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseData);

                        runOnUiThread(() -> {
                            messageList.clear();
                            try {
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject msgObj = jsonArray.getJSONObject(i);
                                    String sender = msgObj.getString("sender");
                                    String text = msgObj.getString("message_text");

                                    boolean isUser = sender.equals("user");
                                    messageList.add(new MessageModel(text, isUser, false));
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            chatAdapter.notifyDataSetChanged();

                            // Check visibility after loading messages
                            updateWelcomeScreenVisibility();

                            if (!messageList.isEmpty()) {
                                chatRecyclerView.scrollToPosition(messageList.size() - 1);
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void createNewConversation() {
        String url = BASE_URL + "/api/conversations";

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("userId", currentUserId);
            jsonBody.put("title", "New Conversation");

            RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
            Request request = new Request.Builder().url(url).post(body).build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String responseData = response.body().string();
                            JSONObject newChat = new JSONObject(responseData);

                            currentConversationId = newChat.getInt("id");

                            runOnUiThread(() -> {
                                messageList.clear();
                                chatAdapter.notifyDataSetChanged();
                                fetchConversations();

                                // Show welcome screen for the new empty chat
                                updateWelcomeScreenVisibility();
                            });
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendMessageToServer(String messageText) {
        String url = BASE_URL + "/api/chat";

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("conversationId", currentConversationId);
            jsonBody.put("message", messageText);

            RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
            Request request = new Request.Builder().url(url).post(body).build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        removeTypingIndicator();
                        addMessageToList("[Connection Error]", false, false);

                        // Unlock UI
                        userInput.setEnabled(true);
                        sendButton.setEnabled(true);
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String responseData = response.body().string();
                            JSONObject jsonResponse = new JSONObject(responseData);
                            String aiReply = jsonResponse.getString("reply");

                            runOnUiThread(() -> {
                                removeTypingIndicator();
                                addMessageToList(aiReply, false, false);
                                fetchConversations(); // Update history title

                                // Unlock UI
                                userInput.setEnabled(true);
                                sendButton.setEnabled(true);
                            });

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // --- RECYCLERVIEW CLASSES ---

    public static class MessageModel {
        String message;
        boolean isUser;
        boolean isTyping;

        public MessageModel(String message, boolean isUser, boolean isTyping) {
            this.message = message;
            this.isUser = isUser;
            this.isTyping = isTyping;
        }
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        LinearLayout typingLayout;
        View dot1, dot2, dot3;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            typingLayout = itemView.findViewById(R.id.typingLayout);
            dot1 = itemView.findViewById(R.id.dot1);
            dot2 = itemView.findViewById(R.id.dot2);
            dot3 = itemView.findViewById(R.id.dot3);
        }
    }

    public class ChatAdapter extends RecyclerView.Adapter<ChatViewHolder> {
        List<MessageModel> messages;
        Markwon markwon;

        public ChatAdapter(List<MessageModel> messages, Markwon markwon) {
            this.messages = messages;
            this.markwon = markwon;
        }

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            MessageModel model = messages.get(position);

            if (model.isTyping) {
                holder.messageText.setVisibility(View.GONE);
                holder.typingLayout.setVisibility(View.VISIBLE);
                holder.typingLayout.setBackgroundResource(R.drawable.bubble_ai);

                LinearLayout.LayoutParams typingParams = (LinearLayout.LayoutParams) holder.typingLayout.getLayoutParams();
                typingParams.gravity = Gravity.START;
                typingParams.setMargins(0, 0, 64, 0);
                holder.typingLayout.setLayoutParams(typingParams);

                animateDot(holder.dot1, 0);
                animateDot(holder.dot2, 150);
                animateDot(holder.dot3, 300);
            } else {
                holder.messageText.setVisibility(View.VISIBLE);
                holder.typingLayout.setVisibility(View.GONE);

                holder.dot1.clearAnimation();
                holder.dot2.clearAnimation();
                holder.dot3.clearAnimation();

                markwon.setMarkdown(holder.messageText, model.message);

                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.messageText.getLayoutParams();

                if (model.isUser) {
                    params.gravity = Gravity.END;
                    holder.messageText.setBackgroundResource(R.drawable.bubble_user);
                    params.setMargins(64, 0, 0, 0);
                } else {
                    params.gravity = Gravity.START;
                    holder.messageText.setBackgroundResource(R.drawable.bubble_ai);
                    params.setMargins(0, 0, 64, 0);
                }
                holder.messageText.setLayoutParams(params);
            }
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        private void animateDot(View dot, long delay) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(dot, "translationY", 0f, -12f, 0f);
            animator.setDuration(600);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setStartDelay(delay);
            animator.start();
        }
    }

}