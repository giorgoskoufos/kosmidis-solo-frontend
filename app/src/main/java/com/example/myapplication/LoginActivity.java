package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
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

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvGoToRegister;

    private final String BASE_URL = "https://app-kosmidis-solo-backend.xadp6y.easypanel.host";

    private final OkHttpClient client = new OkHttpClient();
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- AUTO-LOGIN CHECK ---
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        if (prefs.getInt("userId", -1) != -1) {
            // Υπάρχει ήδη αποθηκευμένο session! Πάμε απευθείας στο MainActivity
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etLoginEmail);
        etPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);

        // Πάμε στην οθόνη Εγγραφής
        tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        // Κουμπί Σύνδεσης
        btnLogin.setOnClickListener(v -> loginUser());
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Παρακαλώ συμπληρώστε όλα τα πεδία", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = BASE_URL + "/api/login";

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("email", email);
            jsonBody.put("password", password);

            RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
            Request request = new Request.Builder().url(url).post(body).build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Σφάλμα σύνδεσης", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseData = response.body().string();
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            try {
                                JSONObject json = new JSONObject(responseData);
                                int userId = json.getInt("userId");

                                // ==================================================
                                // Η ΜΑΓΕΙΑ ΓΙΝΕΤΑΙ ΕΔΩ!
                                // ΣΩΖΟΥΜΕ ΤΟ USER_ID & TO EMAIL ΚΡΥΦΑ ΣΤΟ ΚΙΝΗΤΟ
                                // ==================================================
                                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                                prefs.edit()
                                        .putInt("userId", userId)
                                        .putString("userEmail", email) // <--- ΕΔΩ ΣΩΖΕΤΑΙ!
                                        .apply();

                                Toast.makeText(LoginActivity.this, "Επιτυχής σύνδεση!", Toast.LENGTH_SHORT).show();

                                // Ανοίγουμε το MainActivity
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                finish();

                            } catch (JSONException e) { e.printStackTrace(); }
                        } else {
                            try {
                                JSONObject errorJson = new JSONObject(responseData);
                                Toast.makeText(LoginActivity.this, errorJson.getString("error"), Toast.LENGTH_LONG).show();
                            } catch (JSONException e) {
                                Toast.makeText(LoginActivity.this, "Λάθος στοιχεία", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            });
        } catch (JSONException e) { e.printStackTrace(); }
    }
}