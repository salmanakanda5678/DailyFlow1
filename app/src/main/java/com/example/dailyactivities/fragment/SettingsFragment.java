package com.example.dailyactivities.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.dailyactivities.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Focus & App control toggles
        SwitchMaterial switchAppBlocker  = view.findViewById(R.id.switch_app_blocker);
        SwitchMaterial switchPushNotif   = view.findViewById(R.id.switch_push_notif);
        SwitchMaterial switchTaskAlarm   = view.findViewById(R.id.switch_task_alarm);
        SwitchMaterial switchDailySummary= view.findViewById(R.id.switch_daily_summary);

        switchAppBlocker.setChecked(true);
        switchPushNotif.setChecked(true);
        switchTaskAlarm.setChecked(true);
        switchDailySummary.setChecked(false);

        switchAppBlocker.setOnCheckedChangeListener((v, checked) -> {
            String msg = checked ? "App Blocker চালু করা হয়েছে" : "App Blocker বন্ধ করা হয়েছে";
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        });

        // Account / profile rows
        LinearLayout rowProfile = view.findViewById(R.id.row_profile);
        LinearLayout rowTheme   = view.findViewById(R.id.row_theme);
        LinearLayout rowAppLock = view.findViewById(R.id.row_pin);
        LinearLayout rowDelete  = view.findViewById(R.id.row_delete);

        rowProfile.setOnClickListener(v ->
                Toast.makeText(getContext(), "প্রোফাইল এডিট (শীঘ্রই আসছে)", Toast.LENGTH_SHORT).show()
        );

        rowTheme.setOnClickListener(v ->
                Toast.makeText(getContext(), "থিম পরিবর্তন (শীঘ্রই আসছে)", Toast.LENGTH_SHORT).show()
        );

        rowAppLock.setOnClickListener(v ->
                Toast.makeText(getContext(), "PIN সেট করুন (শীঘ্রই আসছে)", Toast.LENGTH_SHORT).show()
        );

        rowDelete.setOnClickListener(v -> showDeleteConfirmation());
    }

    private void showDeleteConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("সব ডেটা মুছুন?")
                .setMessage("এই কাজটি ফিরিয়ে আনা যাবে না। সব routine মুছে যাবে।")
                .setNegativeButton("বাতিল", null)
                .setPositiveButton("মুছুন", (dialog, which) ->
                        Toast.makeText(getContext(), "সব ডেটা মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                )
                .show();
    }
}
