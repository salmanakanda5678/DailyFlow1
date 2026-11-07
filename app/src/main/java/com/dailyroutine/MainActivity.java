package com.dailyroutine;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.dailyactivities.R;
import com.dailyroutine.fragment.AddTaskFragment;
import com.dailyroutine.fragment.HomeFragment;
import com.dailyroutine.fragment.SettingsFragment;
import com.dailyroutine.fragment.StatsFragment;
import com.dailyroutine.fragment.TasksFragment;
import com.dailyroutine.util.AppBlockerService;
import com.dailyroutine.util.PreferenceManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    public static boolean isAppVisible = false;
    private PreferenceManager prefs;
    private static boolean isUnlocked = false;
    private AlertDialog pinDialog;
    private AlertDialog permissionDialog;

    @Override
    protected void onStart() {
        super.onStart();
        isAppVisible = true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.getBooleanExtra("blocked_app", false)) {
            showBlockingScreen();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = PreferenceManager.getInstance(this);

        // Apply Dark Mode Theme
        if (prefs.isDarkMode()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);

        if (isTrialExpired()) {
            showExpiryScreen();
            return;
        }

        setContentView(R.layout.activity_main);

        if (getIntent().getBooleanExtra("blocked_app", false)) {
            showBlockingScreen();
            return;
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                Fragment frag;
                int id = item.getItemId();

                if      (id == R.id.nav_home)     frag = new HomeFragment();
                else if (id == R.id.nav_tasks)    frag = new TasksFragment();
                else if (id == R.id.nav_add)      frag = new AddTaskFragment();
                else if (id == R.id.nav_stats)    frag = new StatsFragment();
                else if (id == R.id.nav_settings) frag = new SettingsFragment();
                else return false;

                loadFragment(frag);
                return true;
            });
        }

        if (!checkAndEnforceMandatoryPermissions()) {
            return;
        }

        View container = findViewById(R.id.fragment_container);

        if (prefs.hasPin() && !isUnlocked) {
            if (bottomNav != null) bottomNav.setVisibility(View.GONE);
            if (container != null) container.setVisibility(View.GONE);
            showPinLock();
        } else {
            if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);
            if (container != null) container.setVisibility(View.VISIBLE);
            loadFragment(new HomeFragment());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Signal that our app is currently in foreground
        AppBlockerService.setInOurApp(true);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        View container = findViewById(R.id.fragment_container);

        // 1. Mandatory Permissions check
        if (!checkAndEnforceMandatoryPermissions()) {
            return;
        }

        // 2. PIN Lock check
        if (prefs.hasPin() && !isUnlocked) {
            if (bottomNav != null) bottomNav.setVisibility(View.GONE);
            if (container != null) container.setVisibility(View.GONE);
            showPinLock();
        } else {
            if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);
            if (container != null) container.setVisibility(View.VISIBLE);

            if (getSupportFragmentManager().findFragmentById(R.id.fragment_container) == null) {
                loadFragment(new HomeFragment());
            }
        }

        if (prefs.isAppBlockerEnabled()) {
            AppBlockerService.start(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        AppBlockerService.setInOurApp(false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        isAppVisible = false;

        if (prefs.hasPin()) {
            isUnlocked = false;

            BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
            View container = findViewById(R.id.fragment_container);
            if (bottomNav != null) bottomNav.setVisibility(View.GONE);
            if (container != null) container.setVisibility(View.GONE);
        }
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private boolean hasOverlayPermission() {
        return android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M ||
                Settings.canDrawOverlays(this);
    }

    private boolean checkAndEnforceMandatoryPermissions() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        View container = findViewById(R.id.fragment_container);

        if (!hasUsageStatsPermission()) {
            if (bottomNav != null) bottomNav.setVisibility(View.GONE);
            if (container != null) container.setVisibility(View.GONE);
            showMandatoryUsageAccessDialog();
            return false;
        }

        if (!hasOverlayPermission()) {
            if (bottomNav != null) bottomNav.setVisibility(View.GONE);
            if (container != null) container.setVisibility(View.GONE);
            showMandatoryOverlayDialog();
            return false;
        }

        if (permissionDialog != null && permissionDialog.isShowing()) {
            permissionDialog.dismiss();
        }

        return true;
    }

    private void showMandatoryUsageAccessDialog() {
        if (isFinishing() || isDestroyed()) return;
        if (permissionDialog != null && permissionDialog.isShowing()) return;

        permissionDialog = new AlertDialog.Builder(this)
                .setTitle("Usage Access Required")
                .setMessage("App Blocker কাজ করার জন্য Usage Access পারমিশন দেওয়া আবশ্যক। পারমিশন ছাড়া অ্যাপে প্রবেশ করা যাবে না।")
                .setCancelable(false)
                .setPositiveButton("Settings এ যান", (d, w) -> {
                    try {
                        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                    } catch (Exception e) {
                        Toast.makeText(this, "Settings খোলা যাচ্ছে না", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("অ্যাপ বন্ধ করুন", (d, w) -> finish())
                .show();
    }

    private void showMandatoryOverlayDialog() {
        if (isFinishing() || isDestroyed()) return;
        if (permissionDialog != null && permissionDialog.isShowing()) return;

        permissionDialog = new AlertDialog.Builder(this)
                .setTitle("Overlay Permission Required")
                .setMessage("অন্য অ্যাপ ব্লক করার জন্য 'Display over other apps' পারমিশন দেওয়া আবশ্যক। পারমিশন ছাড়া অ্যাপে প্রবেশ করা যাবে না।")
                .setCancelable(false)
                .setPositiveButton("Settings এ যান", (d, w) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                        intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    } catch (Exception e) {
                        try {
                            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
                        } catch (Exception ignored) {}
                    }
                })
                .setNegativeButton("অ্যাপ বন্ধ করুন", (d, w) -> finish())
                .show();
    }

    public boolean isIgnoringBatteryOptimizations() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.os.PowerManager pm = (android.os.PowerManager) getSystemService(Context.POWER_SERVICE);
            return pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
        }
        return true;
    }

    public void requestBatteryOptimizationExemption() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && !isIgnoringBatteryOptimizations()) {
            new AlertDialog.Builder(this)
                .setTitle("🔋 Battery Saver Optimization বন্ধ করুন")
                .setMessage("ব্যাকগ্রাউন্ডে ফোকাস মোড ও অ্যাপ ব্লকার নিরবচ্ছিন্নভাবে চালু রাখার জন্য ব্যাটারি অপটিমাইজেশন 'Don't optimize' বা 'Unrestricted' করা অত্যন্ত প্রয়োজন।")
                .setPositiveButton("Settings এ যান", (d, w) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    } catch (Exception e) {
                        try {
                            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                            startActivity(intent);
                        } catch (Exception ignored) {}
                    }
                })
                .setNegativeButton("পরে করব", null)
                .show();
        }
    }

    private void showExpiryScreen() {
        new AlertDialog.Builder(this)
                .setTitle("Trial Period Ended")
                .setMessage("আপনার ১০ দিনের ট্রায়াল পিরিয়ড শেষ হয়ে গেছে। দয়া করে পূর্ণ ভার্সনটি সংগ্রহ করুন।")
                .setCancelable(false)
                .setPositiveButton("Exit", (d, w) -> finish())
                .show();
    }

    private boolean isTrialExpired() {
        try {
            long installTime = getPackageManager()
                    .getPackageInfo(getPackageName(), 0)
                    .firstInstallTime;

            long currentTime = System.currentTimeMillis();
            long tenDaysInMillis = 10L * 24 * 60 * 60 * 1000; // 10 Days

            return (currentTime - installTime) > tenDaysInMillis;
        } catch (Exception e) {
            return false;
        }
    }

    private void showBlockingScreen() {
        new AlertDialog.Builder(this)
                .setTitle("Focus Mode Active!")
                .setMessage("এই অ্যাপটি এখন ব্লক করা আছে। নিজের কাজে মনোযোগ দিন! 🚀")
                .setCancelable(false)
                .setPositiveButton("Home এ যান", (d, w) -> {
                    Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                    homeIntent.addCategory(Intent.CATEGORY_HOME);
                    homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(homeIntent);
                    finish();
                })
                .show();
    }

    public static void setUnlocked(boolean unlocked) {
        isUnlocked = unlocked;
    }

    private void loadFragment(Fragment fragment) {
        if (isFinishing() || isDestroyed()) return;
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commitAllowingStateLoss();
    }

    private void showPinLock() {
        if (pinDialog != null && pinDialog.isShowing()) return;

        EditText etPin = new EditText(this);
        etPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        etPin.setHint("৪ সংখ্যার PIN দিন");
        etPin.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        etPin.setTextSize(24);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(80, 60, 80, 40);
        layout.addView(etPin);

        pinDialog = new AlertDialog.Builder(this)
            .setTitle("নিরাপত্তা লক")
            .setMessage("আপনার PIN দিয়ে আনলক করুন")
            .setView(layout)
            .setCancelable(false)
            .setPositiveButton("আনলক", null)
            .setNegativeButton("বন্ধ করুন", (d, w) -> finish())
            .create();

        pinDialog.show();

        pinDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String entered = etPin.getText().toString();
            if (entered.equals(prefs.getAppLockPin())) {
                isUnlocked = true;
                pinDialog.dismiss();

                BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
                View container = findViewById(R.id.fragment_container);
                if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);
                if (container != null) container.setVisibility(View.VISIBLE);

                if (getSupportFragmentManager().findFragmentById(R.id.fragment_container) == null) {
                    loadFragment(new HomeFragment());
                }
            } else {
                Toast.makeText(this, "❌ ভুল PIN!", Toast.LENGTH_SHORT).show();
                etPin.setText("");
            }
        });
    }
}
