package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class SettingsMenuDialog extends BottomSheetDialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.from(requireContext()).inflate(R.layout.dialog_settings_menu, container, false);

        LinearLayout llMenuItemProfile = view.findViewById(R.id.llMenuItemProfilePC);
        LinearLayout llMenuItemLogout = view.findViewById(R.id.llMenuItemLogoutPC);

        // 1. Εξατομίκευση Προφίλ
        llMenuItemProfile.setOnClickListener(v -> {
            dismiss(); // Κλείνει το Bottom Sheet
            // Ανοίγει τη νέα μετονομασμένη Activity
            startActivity(new Intent(requireContext(), ProfileCustomisationActivity.class));
        });

        // 2. Αποσύνδεση (Logout): Move logic from MainActivity to here
        llMenuItemLogout.setOnClickListener(v -> {
            dismiss();
            SharedPreferences preferences = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
            preferences.edit().clear().apply(); // Σβήνουμε τα αποθηκευμένα στοιχεία

            // Γυρνάμε στο LoginActivity
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Καθαρίζει το stack
            startActivity(intent);
        });

        //3. Google Integration
        LinearLayout llMenuItemApps = view.findViewById(R.id.llMenuItemAppsPC);

        llMenuItemApps.setOnClickListener(v -> {
            dismiss(); // Close the main settings menu

            // Open the Google-specific integrations sheet
            IntegrationsGoogle integrationsGoogle = new IntegrationsGoogle();
            integrationsGoogle.show(getParentFragmentManager(), "IntegrationsGoogle");
        });

        return view;
    }
}

