package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class SettingsMenuDialog extends BottomSheetDialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_settings_menu, container, false);

        LinearLayout llMenuItemProfile = view.findViewById(R.id.llMenuItemProfilePC);
        LinearLayout llMenuItemLogout = view.findViewById(R.id.llMenuItemLogoutPC);
        LinearLayout llMenuItemApps = view.findViewById(R.id.llMenuItemAppsPC);

        ImageView ivMenuItemAppsIcon = view.findViewById(R.id.ivMenuItemAppsIcon);
        TextView tvMenuItemAppsText = view.findViewById(R.id.tvMenuItemAppsText);

        SharedPreferences preferences = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);

        // 1. Profile Customization
        llMenuItemProfile.setOnClickListener(v -> {
            dismiss(); // Close the Bottom Sheet
            // Open the renamed Activity
            startActivity(new Intent(requireContext(), ProfileCustomisationActivity.class));
        });

        // 2. Logout
        llMenuItemLogout.setOnClickListener(v -> {
            dismiss();
            preferences.edit().clear().apply(); // Clear saved user data

            // Return to LoginActivity and clear the back stack
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // 3. Google Integration (Gmail Check Logic)
        String userEmail = preferences.getString("userEmail", "");

        if (userEmail != null && userEmail.toLowerCase().endsWith("@gmail.com")) {
            // User has a Gmail account: Unlock the option
            llMenuItemApps.setClickable(true);
            llMenuItemApps.setFocusable(true);
            llMenuItemApps.setEnabled(true);

            // Restore the ripple effect for touch feedback
            TypedValue outValue = new TypedValue();
            requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            llMenuItemApps.setBackgroundResource(outValue.resourceId);

            // Restore colors from the Dark Theme (Icon: Hint, Text: Light Gray)
            ivMenuItemAppsIcon.setColorFilter(Color.parseColor("#D1C4E9"));
            tvMenuItemAppsText.setTextColor(Color.parseColor("#E0E0E0"));
            tvMenuItemAppsText.setText("Connected Apps");

            // Add the OnClickListener
            llMenuItemApps.setOnClickListener(v -> {
                dismiss(); // Close the main settings menu

                // Open the Google-specific integrations sheet
                IntegrationsGoogle integrationsGoogle = new IntegrationsGoogle();
                integrationsGoogle.show(getParentFragmentManager(), "IntegrationsGoogle");
            });
        }

        return view;
    }
}