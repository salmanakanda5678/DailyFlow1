package com.dailyroutine.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.dailyactivities.R;
import com.dailyroutine.database.DatabaseHelper;
import com.dailyroutine.model.Task;
import com.dailyroutine.util.AppBlockerService;
import com.dailyroutine.util.PreferenceManager;
import com.dailyroutine.util.ReelsBlockerService;

public class SettingsFragment extends Fragment {

    private Switch switchAppBlocker, switchReelsBlocker, switchPushNotif, switchTaskAlarm, switchDailySummary;
    private CheckBox cbFacebook, cbYoutube, cbInstagram, cbMessenger, cbTiktok;
    private TextView tvProfileName, tvTheme, tvPinStatus;
    private TextView tvRowProfile, tvRowTheme, tvRowPin, tvRowDeleteData;

    private PreferenceManager prefs;
    private DatabaseHelper    db;

    private boolean isRevertingSwitch = false;
    private boolean isRevertingCheckbox = false;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = PreferenceManager.getInstance(requireContext());
        db    = DatabaseHelper.getInstance(requireContext());

        switchAppBlocker   = view.findViewById(R.id.switch_app_blocker);
        switchReelsBlocker = view.findViewById(R.id.switch_reels_blocker);
        switchPushNotif    = view.findViewById(R.id.switch_push_notif);
        switchTaskAlarm    = view.findViewById(R.id.switch_task_alarm);
        switchDailySummary = view.findViewById(R.id.switch_daily_summary);
        tvProfileName      = view.findViewById(R.id.tv_profile_name);
        tvTheme            = view.findViewById(R.id.tv_theme_name);
        tvPinStatus        = view.findViewById(R.id.tv_pin_status);

        cbFacebook  = view.findViewById(R.id.cb_facebook);
        cbYoutube   = view.findViewById(R.id.cb_youtube);
        cbInstagram = view.findViewById(R.id.cb_instagram);
        cbMessenger = view.findViewById(R.id.cb_messenger);
        cbTiktok    = view.findViewById(R.id.cb_tiktok);

        // Load current prefs
        switchAppBlocker.setChecked(prefs.isAppBlockerEnabled());
        if (switchReelsBlocker != null) {
            switchReelsBlocker.setChecked(prefs.isReelsBlockerEnabled() && ReelsBlockerService.isAccessibilityServiceEnabled(requireContext()));
        }
        switchPushNotif.setChecked(prefs.isPushNotifEnabled());
        switchTaskAlarm.setChecked(prefs.isTaskAlarmEnabled());
        switchDailySummary.setChecked(prefs.isDailySummaryEnabled());
        tvProfileName.setText(prefs.getUserName());
        tvTheme.setText(prefs.isDarkMode() ? "ডার্ক মোড (Dark)" : "লাইট মোড (Light)");
        tvPinStatus.setText(prefs.hasPin() ? "চালু আছে" : "বন্ধ আছে");

        // Listeners
        switchAppBlocker.setOnCheckedChangeListener((b, on) -> {
            if (isRevertingSwitch) return;
            if (!on) {
                Task activeFocus = db.getActiveFocusTask();
                if (activeFocus != null) {
                    isRevertingSwitch = true;
                    switchAppBlocker.setChecked(true);
                    isRevertingSwitch = false;
                    Toast.makeText(requireContext(), "🔒 ফোকাস মোড চলাকালীন অ্যাপ ব্লকার বন্ধ করা যাবে না! সময় শেষ না হওয়া পর্যন্ত এটি সক্রিয় থাকবে।", Toast.LENGTH_LONG).show();
                    return;
                }
            }
            prefs.setAppBlockerEnabled(on);
            prefs.setFocusModeActive(on);
            if (on) {
                AppBlockerService.start(requireContext());
                if (getActivity() instanceof com.dailyroutine.MainActivity) {
                    ((com.dailyroutine.MainActivity) getActivity()).requestBatteryOptimizationExemption();
                }
            } else {
                AppBlockerService.stop(requireContext());
            }
        });

        if (switchReelsBlocker != null) {
            switchReelsBlocker.setOnCheckedChangeListener((b, on) -> {
                if (isRevertingSwitch) return;
                if (on) {
                    if (!ReelsBlockerService.isAccessibilityServiceEnabled(requireContext())) {
                        isRevertingSwitch = true;
                        switchReelsBlocker.setChecked(false);
                        isRevertingSwitch = false;
                        showAccessibilityPermissionDialog();
                        return;
                    }
                    prefs.setReelsBlockerEnabled(true);
                    Toast.makeText(requireContext(), "✅ Reels & Shorts Blocker চালু হয়েছে", Toast.LENGTH_SHORT).show();
                } else {
                    prefs.setReelsBlockerEnabled(false);
                    Toast.makeText(requireContext(), "⏸️ Reels & Shorts Blocker বন্ধ করা হয়েছে", Toast.LENGTH_SHORT).show();
                }
            });
        }

        switchPushNotif.setOnCheckedChangeListener((b, on) -> prefs.setPushNotifEnabled(on));
        switchTaskAlarm.setOnCheckedChangeListener((b, on) -> prefs.setTaskAlarmEnabled(on));
        switchDailySummary.setOnCheckedChangeListener((b, on) -> prefs.setDailySummaryEnabled(on));

        // Checkbox listeners with focus lock check
        setupBlockedAppCheckbox(cbFacebook, prefs.isFacebookBlocked(), on -> prefs.setFacebookBlocked(on));
        setupBlockedAppCheckbox(cbYoutube, prefs.isYoutubeBlocked(), on -> prefs.setYoutubeBlocked(on));
        setupBlockedAppCheckbox(cbInstagram, prefs.isInstagramBlocked(), on -> prefs.setInstagramBlocked(on));
        setupBlockedAppCheckbox(cbMessenger, prefs.isMessengerBlocked(), on -> prefs.setMessengerBlocked(on));
        setupBlockedAppCheckbox(cbTiktok, prefs.isTiktokBlocked(), on -> prefs.setTiktokBlocked(on));

        // Account rows
        view.findViewById(R.id.row_profile).setOnClickListener(v -> showProfileDialog());
        view.findViewById(R.id.row_theme).setOnClickListener(v -> showThemeDialog());
        view.findViewById(R.id.row_pin).setOnClickListener(v -> showPinDialog());
        view.findViewById(R.id.row_delete).setOnClickListener(v -> showDeleteDialog());
    }

    private interface CheckboxUpdater {
        void update(boolean on);
    }

    private void setupBlockedAppCheckbox(CheckBox cb, boolean initialValue, CheckboxUpdater updater) {
        cb.setChecked(initialValue);
        cb.setOnCheckedChangeListener((b, on) -> {
            if (isRevertingCheckbox) return;
            Task activeFocus = db.getActiveFocusTask();
            if (activeFocus != null) {
                isRevertingCheckbox = true;
                cb.setChecked(!on); // revert
                isRevertingCheckbox = false;
                Toast.makeText(requireContext(), "🔒 ফোকাস মোড চলাকালীন ব্লক করা অ্যাপের তালিকা পরিবর্তন করা যাবে না!", Toast.LENGTH_LONG).show();
                return;
            }
            updater.update(on);
        });
    }

    private void showThemeDialog() {
        String[] themes = {"লাইট মোড (Light)", "ডার্ক মোড (Dark)"};
        int checkedItem = prefs.isDarkMode() ? 1 : 0;

        new AlertDialog.Builder(requireContext())
            .setTitle("থিম নির্বাচন করুন")
            .setSingleChoiceItems(themes, checkedItem, (dialog, which) -> {
                boolean isDark = (which == 1);
                prefs.setDarkMode(isDark);
                
                // Apply theme immediately
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    isDark ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES 
                           : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                );
                
                tvTheme.setText(themes[which]);
                dialog.dismiss();
                Toast.makeText(requireContext(), "✅ থিম পরিবর্তন করা হয়েছে", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("বাতিল", null)
            .show();
    }

    private void showProfileDialog() {
        EditText et = new EditText(requireContext());
        et.setHint("আপনার নাম লিখুন");
        et.setText(prefs.getUserName());
        new AlertDialog.Builder(requireContext())
            .setTitle("প্রোফাইল")
            .setView(et)
            .setPositiveButton("সেভ করুন", (d, w) -> {
                String name = et.getText().toString().trim();
                if (!name.isEmpty()) {
                    prefs.setUserName(name);
                    tvProfileName.setText(name);
                    Toast.makeText(requireContext(), "✅ নাম আপডেট হয়েছে", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("বাতিল", null)
            .show();
    }

    private void showPinDialog() {
        EditText et = new EditText(requireContext());
        et.setHint("4 সংখ্যার PIN দিন");
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER |
            android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        new AlertDialog.Builder(requireContext())
            .setTitle("App Lock / PIN")
            .setView(et)
            .setPositiveButton("সেট করুন", (d, w) -> {
                String pin = et.getText().toString().trim();
                if (pin.length() == 4) {
                    prefs.setAppLockPin(pin);
                    com.dailyroutine.MainActivity.setUnlocked(true);
                    tvPinStatus.setText("চালু আছে");
                    Toast.makeText(requireContext(), "🔒 PIN সেট হয়েছে", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "4 সংখ্যার PIN দিন", Toast.LENGTH_SHORT).show();
                }
            })
            .setNeutralButton("PIN সরাও", (d, w) -> {
                prefs.setAppLockPin("");
                tvPinStatus.setText("বন্ধ আছে");
            })
            .setNegativeButton("বাতিল", null)
            .show();
    }

    private void showDeleteDialog() {
        Task activeFocus = db.getActiveFocusTask();
        if (activeFocus != null) {
            Toast.makeText(requireContext(), "🔒 ফোকাস মোড চলছে! সময় শেষ না হওয়া পর্যন্ত সব ডেটা মোছা যাবে না।", Toast.LENGTH_LONG).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
            .setTitle("⚠️ সব ডেটা মুছুন")
            .setMessage("সব task এবং routine মুছে যাবে। আপনি কি নিশ্চিত?")
            .setPositiveButton("হ্যাঁ, মুছুন", (d, w) -> {
                db.deleteAllTasks();
                Toast.makeText(requireContext(), "🗑️ সব ডেটা মুছে গেছে", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("না", null)
            .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (switchReelsBlocker != null && prefs != null && isAdded()) {
            boolean isServiceEnabled = ReelsBlockerService.isAccessibilityServiceEnabled(requireContext());
            if (isServiceEnabled && !prefs.isReelsBlockerEnabled()) {
                prefs.setReelsBlockerEnabled(true);
            }
            switchReelsBlocker.setChecked(prefs.isReelsBlockerEnabled() && isServiceEnabled);
        }
    }

    private void showAccessibilityPermissionDialog() {
        new AlertDialog.Builder(requireContext())
            .setTitle("Accessibility Permission প্রয়োজন")
            .setMessage("YouTube Shorts ও Facebook/Instagram Reels সনাক্ত করে অটোমেটিক বন্ধ রাখতে 'DailyFlow Reels Blocker' অন করা প্রয়োজন।\n\nSettings > Accessibility > Installed Apps এ গিয়ে DailyFlow Reels Blocker চালু করুন।")
            .setPositiveButton("Settings এ যান", (d, w) -> {
                try {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Accessibility Settings খোলা যাচ্ছে না", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("বাতিল", null)
            .show();
    }
}