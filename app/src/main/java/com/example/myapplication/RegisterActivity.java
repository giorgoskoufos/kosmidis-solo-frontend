package com.example.myapplication;

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

public class RegisterActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnRegister;
    private TextView tvGoToLogin;

    private final OkHttpClient client = new OkHttpClient();
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etEmail = findViewById(R.id.etRegisterEmail);
        etPassword = findViewById(R.id.etRegisterPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        // Αν πατήσει "Έχω ήδη λογαριασμό", γυρνάμε πίσω στο Login
        tvGoToLogin.setOnClickListener(v -> finish());

        // Κουμπί Εγγραφής
        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Παρακαλώ συμπληρώστε όλα τα πεδία", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = "https://app-kosmidis-solo-backend.xadp6y.easypanel.host/api/register";

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("email", email);
            jsonBody.put("password", password);

            RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
            Request request = new Request.Builder().url(url).post(body).build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Σφάλμα σύνδεσης με τον Server", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseData = response.body().string();
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(RegisterActivity.this, "Η εγγραφή πέτυχε! Συνδεθείτε.", Toast.LENGTH_LONG).show();
                            finish(); // Κλείνει αυτή την οθόνη και γυρνάει στο Login
                        } else {
                            try {
                                JSONObject errorJson = new JSONObject(responseData);
                                Toast.makeText(RegisterActivity.this, errorJson.getString("error"), Toast.LENGTH_LONG).show();
                            } catch (JSONException e) {
                                Toast.makeText(RegisterActivity.this, "Σφάλμα εγγραφής", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            });
        } catch (JSONException e) { e.printStackTrace(); }
    }
}