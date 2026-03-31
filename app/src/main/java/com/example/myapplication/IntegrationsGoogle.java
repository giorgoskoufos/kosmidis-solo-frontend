package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class IntegrationsGoogle extends BottomSheetDialogFragment {

    private SwitchMaterial switchRead, switchWrite;
    private Button btnSync;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Load your dynamic integrations layout
        View view = inflater.inflate(R.layout.dialog_integrations, container, false);

        // Initialize UI components from your XML IDs
        switchRead = view.findViewById(R.id.switch_read_perms);
        switchWrite = view.findViewById(R.id.switch_write_perms);
        btnSync = view.findViewById(R.id.btn_google_connect);

        // 1. Logic for Read Toggle
        switchRead.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                // Αν ο χρήστης ΚΛΕΙΣΕΙ το Read, τότε το Full ΠΡΕΠΕΙ να σβήσει
                // γιατί δεν υπάρχει Write χωρίς Read.
                if (switchWrite.isChecked()) {
                    switchWrite.setChecked(false);
                }
            }
        });

        // 2. Logic for Full Access Toggle
        switchWrite.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Αν ο χρήστης ΑΝΟΙΞΕΙ το Full, τότε το Read ΠΡΕΠΕΙ να ανάψει αυτόματα.
                if (!switchRead.isChecked()) {
                    switchRead.setChecked(true);
                }
            }
        });

        btnSync.setOnClickListener(v -> {
            startGoogleSync();
        });

        return view;
    }

    private void startGoogleSync() {
        // 1. Get the current user ID and Email from SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        int userId = prefs.getInt("userId", -1);
        String userEmail = prefs.getString("userEmail", ""); // Εδώ παίρνουμε το email

        // Safety check: Αν δεν έχουμε userId ή email, δεν προχωράμε
        if (userId == -1 || userEmail.isEmpty()) {
            Toast.makeText(requireContext(), "User session error. Please re-login.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Capture the state of the switches (Read/Write)
        boolean canRead = switchRead.isChecked();
        boolean canWrite = switchWrite.isChecked();

        // 3. Construct the authentication URL for your Node.js server
        // Προσθέτουμε την παράμετρο &email=...
        // Χρησιμοποιούμε Uri.encode για να διαχειριστούμε σωστά ειδικούς χαρακτήρες όπως το @
        String baseUrl = "https://app-kosmidis-solo-backend.xadp6y.easypanel.host/api/auth/google";
        String authUrl = baseUrl + "?userId=" + userId
                + "&email=" + Uri.encode(userEmail)
                + "&read=" + canRead
                + "&write=" + canWrite;

        // 4. Open the System Browser for Google OAuth
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
        startActivity(intent);

        dismiss(); // Close the bottom sheet after starting the flow
    }
}