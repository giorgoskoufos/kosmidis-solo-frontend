package com.example.myapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProfileCustomisationActivity extends AppCompatActivity {

    private EditText etFirstNamePC, etLastNamePC, etAgePC, etProfessionPC, etInterestsPC;
    private Button btnSaveProfilePC, btnBackPC;

    private int currentUserId = -1;
    private final OkHttpClient client = new OkHttpClient();
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_customisation); // Μετονομάστηκε!

        etFirstNamePC = findViewById(R.id.etPCFirstName);
        etLastNamePC = findViewById(R.id.etPCLastName);
        etAgePC = findViewById(R.id.etPCAge);
        etProfessionPC = findViewById(R.id.etPCProfession);
        etInterestsPC = findViewById(R.id.etPCInterests);
        btnSaveProfilePC = findViewById(R.id.btnSaveProfilePC);
        btnBackPC = findViewById(R.id.btnBackPC);

        // Τραβάμε το ID του χρήστη
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        currentUserId = prefs.getInt("userId", -1);

        if (currentUserId != -1) {
            loadProfilePC(); // Φορτώνουμε τα υπάρχοντα στοιχεία από τον Server
        } else {
            Toast.makeText(this, "Σφάλμα: Χρήστης δεν βρέθηκε", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnBackPC.setOnClickListener(v -> finish()); // Επιστροφή στο Chat
        btnSaveProfilePC.setOnClickListener(v -> saveProfilePC()); // Αποθήκευση
    }

    private void loadProfilePC() {
        String url = "https://app-kosmidis-solo-backend.xadp6y.easypanel.host/api/profile/" + currentUserId;

        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONObject json = new JSONObject(responseData);

                        runOnUiThread(() -> {
                            // optString για contrast, handling nulls
                            etFirstNamePC.setText(json.optString("first_name", ""));
                            etLastNamePC.setText(json.optString("last_name", ""));

                            String ageStr = json.optString("age", "");
                            if (!ageStr.equals("null") && !ageStr.isEmpty()) {
                                etAgePC.setText(ageStr);
                            } else {
                                etAgePC.setText("");
                            }

                            etProfessionPC.setText(json.optString("profession", ""));
                            etInterestsPC.setText(json.optString("interests", ""));
                        });
                    } catch (JSONException e) { e.printStackTrace(); }
                }
            }
        });
    }

    private void saveProfilePC() {
        String url = "https://app-kosmidis-solo-backend.xadp6y.easypanel.host/api/profile";

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("userId", currentUserId);
            jsonBody.put("firstName", etFirstNamePC.getText().toString().trim());
            jsonBody.put("lastName", etLastNamePC.getText().toString().trim());

            String ageStr = etAgePC.getText().toString().trim();
            jsonBody.put("age", ageStr.isEmpty() ? JSONObject.NULL : Integer.parseInt(ageStr));

            jsonBody.put("profession", etProfessionPC.getText().toString().trim());
            jsonBody.put("interests", etInterestsPC.getText().toString().trim());

            RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
            Request request = new Request.Builder().url(url).post(body).build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> Toast.makeText(ProfileCustomisationActivity.this, "Σφάλμα σύνδεσης", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(ProfileCustomisationActivity.this, "Το προφίλ αποθηκεύτηκε!", Toast.LENGTH_SHORT).show();
                            finish(); // Κλείνει η οθόνη και γυρνάμε στο Chat
                        } else {
                            Toast.makeText(ProfileCustomisationActivity.this, "Σφάλμα αποθήκευσης", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        } catch (JSONException | NumberFormatException e) {
            e.printStackTrace();
            Toast.makeText(this, "Παρακαλώ ελέγξτε τα δεδομένα σας", Toast.LENGTH_SHORT).show();
        }
    }
}