package com.dailyroutine.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.dailyroutine.MainActivity;
import com.dailyroutine.database.DatabaseHelper;
import com.dailyroutine.model.Task;
import com.example.dailyactivities.R;

public class AppBlockerService extends Service {

    private static boolean inOurApp = false;
    private static final String CHANNEL_ID = "AppBlockerChannel";
    private static final int NOTIFICATION_ID = 101;

    private boolean isPackageBlocked(String packageName) {
        if (packageName == null || packageName.isEmpty()) return false;
        PreferenceManager prefs = PreferenceManager.getInstance(this);

        String pkg = packageName.toLowerCase();

        if (pkg.contains("facebook.orca") || pkg.contains("messenger")) {
            return prefs.isMessengerBlocked();
        }
        if (pkg.contains("facebook")) {
            return prefs.isFacebookBlocked();
        }
        if (pkg.contains("youtube")) {
            return prefs.isYoutubeBlocked();
        }
        if (pkg.contains("instagram")) {
            return prefs.isInstagramBlocked();
        }
        if (pkg.contains("musically") || pkg.contains("trill") || pkg.contains("tiktok")) {
            return prefs.isTiktokBlocked();
        }

        return false;
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isRunning = false;
    private WindowManager windowManager;
    private View blockerOverlay;
    private boolean isOverlayShowing = false;
    private String lastFocusEndTime = "";

    private String lastKnownApp = "";

    private final Runnable checker = new Runnable() {
        @Override
        public void run() {
            if (!isRunning) return;

            DatabaseHelper db = DatabaseHelper.getInstance(AppBlockerService.this);
            PreferenceManager prefs = PreferenceManager.getInstance(AppBlockerService.this);
            Task activeFocus = db.getActiveFocusTask();

            // সার্ভিসটি কেবল তখনই বন্ধ হবে যখন অ্যাপ ব্লকার অফ থাকবে এবং কোনো সক্রিয় ফোকাস টাস্ক থাকবে না
            if (!prefs.isAppBlockerEnabled() && activeFocus == null) {
                stopSelf();
                return;
            }

            checkForegroundApp();
            updateNotificationIfTimeChanged();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startAsForeground();
        if (!isRunning) {
            isRunning = true;
            handler.post(checker);
        }
        return START_STICKY;
    }

    private void startAsForeground() {
        PreferenceManager prefs = PreferenceManager.getInstance(this);
        DatabaseHelper db = DatabaseHelper.getInstance(this);

        Task activeFocus = db.getActiveFocusTask();
        boolean isBlockingActive = prefs.isAppBlockerEnabled() || activeFocus != null;

        String title = isBlockingActive ? "Focus Mode চালু আছে" : "Focus Mode Standby";
        String content = "পরবর্তী ফোকাস টাস্কের জন্য অপেক্ষা করা হচ্ছে...";

        if (isBlockingActive) {
            if (activeFocus != null) {
                String focusEndTime = calculateEndTimeOnly(activeFocus);
                content = "Facebook, YouTube বন্ধ • " + focusEndTime + " পর্যন্ত";
            } else {
                content = "App Blocker সক্রিয় আছে";
            }
        }

        lastFocusEndTime = activeFocus != null ? calculateEndTimeOnly(activeFocus) : "standby";

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private String calculateEndTimeOnly(Task task) {
        try {
            String[] p = task.getStartTime().split(":");
            int h = Integer.parseInt(p[0]);
            int m = Integer.parseInt(p[1]);
            int totalMins = h * 60 + m + task.getDurationMinutes();
            int endH = (totalMins / 60) % 24;
            int endM = totalMins % 60;

            String ampm = endH < 12 ? "AM" : "PM";
            if (endH == 0) endH = 12;
            else if (endH > 12) endH -= 12;

            return endH + ":" + String.format("%02d", endM) + " " + ampm;
        } catch (Exception e) { return "7:00 PM"; }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "App Blocker", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void checkForegroundApp() {
        // ১. আমাদের নিজের অ্যাপ সামনে থাকলে ব্লক করার প্রয়োজন নেই
        if (inOurApp) {
            hideOverlay();
            return;
        }

        PreferenceManager prefs = PreferenceManager.getInstance(this);
        DatabaseHelper db = DatabaseHelper.getInstance(this);

        // ২. যদি অ্যাপ ব্লকার অন থাকে অথবা ফোকাস টাস্ক রানিং থাকে, তবে ব্লকিং চেক হবে
        Task activeFocus = db.getActiveFocusTask();
        boolean shouldBlock = prefs.isAppBlockerEnabled() || activeFocus != null;

        if (!shouldBlock) {
            hideOverlay();
            return;
        }

        String currentApp = getForegroundPackage();

        // ৩. যদি নতুন কোনো অ্যাপের তথ্য না পাওয়া যায়, তবে আগের অ্যাপটিকেই ব্যবহার করো
        if (currentApp.isEmpty()) {
            currentApp = lastKnownApp;
        } else {
            lastKnownApp = currentApp;
        }

        // ৪. যদি আমাদের অ্যাপ বা লঞ্চার সামনে থাকে, তবে হাইড করো
        if (currentApp.isEmpty() || currentApp.equals(getPackageName()) || currentApp.contains("launcher")) {
            hideOverlay();
            return;
        }

        if (isPackageBlocked(currentApp)) {
            showOverlay();
        } else {
            hideOverlay();
        }
    }

    private void showOverlay() {
        if (isOverlayShowing) return;
        handler.post(() -> {
            try {
                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                                WindowManager.LayoutParams.TYPE_PHONE,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT);

                blockerOverlay = LayoutInflater.from(this).inflate(R.layout.layout_blocker_overlay, null);
                Button btnExit = blockerOverlay.findViewById(R.id.btn_exit_to_home);
                btnExit.setOnClickListener(v -> {
                    Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                    homeIntent.addCategory(Intent.CATEGORY_HOME);
                    homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(homeIntent);
                });

                windowManager.addView(blockerOverlay, params);
                isOverlayShowing = true;
            } catch (Exception ignored) {}
        });
    }

    private void hideOverlay() {
        if (!isOverlayShowing) return;
        handler.post(() -> {
            try {
                if (blockerOverlay != null) {
                    windowManager.removeView(blockerOverlay);
                }
            } catch (Exception ignored) {}
            isOverlayShowing = false;
        });
    }

    private String getForegroundPackage() {
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) return "";
        long now = System.currentTimeMillis();
        UsageEvents events = usm.queryEvents(now - 1000 * 30, now);
        UsageEvents.Event event = new UsageEvents.Event();
        String lastPkg = "";
        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            int type = event.getEventType();
            if (type == UsageEvents.Event.MOVE_TO_FOREGROUND || type == 15 /* ACTIVITY_RESUMED */) {
                lastPkg = event.getPackageName();
            }
        }
        return lastPkg;
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, AppBlockerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent);
        else context.startService(intent);
    }

    public static void stop(Context context) { context.stopService(new Intent(context, AppBlockerService.class)); }

    private void updateNotificationIfTimeChanged() {
        PreferenceManager prefs = PreferenceManager.getInstance(this);
        DatabaseHelper db = DatabaseHelper.getInstance(this);

        Task activeFocus = db.getActiveFocusTask();
        boolean isBlockingActive = prefs.isAppBlockerEnabled() || activeFocus != null;

        String currentKey = isBlockingActive ? (activeFocus != null ? calculateEndTimeOnly(activeFocus) : "active") : "standby";

        if (!currentKey.equals(lastFocusEndTime)) {
            startAsForeground();
        }
    }

    public static void setInOurApp(boolean value) {
        inOurApp = value;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        inOurApp = false;

        PreferenceManager prefs = PreferenceManager.getInstance(this);
        DatabaseHelper db = DatabaseHelper.getInstance(this);

        if (prefs.isAppBlockerEnabled() || db.getActiveFocusTask() != null) {
            Intent restartServiceIntent = new Intent(getApplicationContext(), AppBlockerService.class);
            restartServiceIntent.setPackage(getPackageName());

            PendingIntent restartServicePendingIntent = PendingIntent.getService(
                    getApplicationContext(), 1, restartServiceIntent,
                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

            android.app.AlarmManager alarmService = (android.app.AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmService != null) {
                alarmService.set(
                        android.app.AlarmManager.ELAPSED_REALTIME,
                        android.os.SystemClock.elapsedRealtime() + 500,
                        restartServicePendingIntent);
            }
        }
    }

    @Override public void onDestroy() { isRunning = false; hideOverlay(); super.onDestroy(); }
    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
