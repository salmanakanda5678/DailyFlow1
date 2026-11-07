package com.dailyroutine.util;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {
    private static final String PREF_NAME = "DailyRoutinePrefs";

    // Keys
    private static final String KEY_USER_NAME         = "user_name";
    private static final String KEY_APP_BLOCKER       = "app_blocker";
    private static final String KEY_PUSH_NOTIF        = "push_notification";
    private static final String KEY_TASK_ALARM        = "task_alarm";
    private static final String KEY_DAILY_SUMMARY     = "daily_summary";
    private static final String KEY_FOCUS_MODE_ON     = "focus_mode_on";
    private static final String KEY_FOCUS_END_TIME    = "focus_end_time";
    private static final String KEY_APP_LOCK_PIN      = "app_lock_pin";
    private static final String KEY_DARK_MODE         = "dark_mode";
    private static final String KEY_ENTERTAINMENT_START = "entertainment_start";
    private static final String KEY_ENTERTAINMENT_END   = "entertainment_end";

    // Blocked Apps
    private static final String KEY_BLOCK_FACEBOOK    = "block_facebook";
    private static final String KEY_BLOCK_YOUTUBE     = "block_youtube";
    private static final String KEY_BLOCK_INSTAGRAM   = "block_instagram";
    private static final String KEY_BLOCK_TIKTOK      = "block_tiktok";
    private static final String KEY_BLOCK_MESSENGER   = "block_messenger";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    private static PreferenceManager instance;

    public static synchronized PreferenceManager getInstance(Context ctx) {
        if (instance == null) instance = new PreferenceManager(ctx.getApplicationContext());
        return instance;
    }

    private PreferenceManager(Context context) {
        prefs  = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    // User
    public String getUserName()            { return prefs.getString(KEY_USER_NAME, "Salman"); }
    public void   setUserName(String name) { editor.putString(KEY_USER_NAME, name).apply(); }

    // Toggles
    public boolean isAppBlockerEnabled()        { return prefs.getBoolean(KEY_APP_BLOCKER, false); }
    public void    setAppBlockerEnabled(boolean v){ editor.putBoolean(KEY_APP_BLOCKER, v).apply(); }

    public boolean isPushNotifEnabled()         { return prefs.getBoolean(KEY_PUSH_NOTIF, true); }
    public void    setPushNotifEnabled(boolean v){ editor.putBoolean(KEY_PUSH_NOTIF, v).apply(); }

    public boolean isTaskAlarmEnabled()         { return prefs.getBoolean(KEY_TASK_ALARM, true); }
    public void    setTaskAlarmEnabled(boolean v){ editor.putBoolean(KEY_TASK_ALARM, v).apply(); }

    public boolean isDailySummaryEnabled()         { return prefs.getBoolean(KEY_DAILY_SUMMARY, false); }
    public void    setDailySummaryEnabled(boolean v){ editor.putBoolean(KEY_DAILY_SUMMARY, v).apply(); }

    public boolean isDarkMode()         { return prefs.getBoolean(KEY_DARK_MODE, false); }
    public void    setDarkMode(boolean v){ editor.putBoolean(KEY_DARK_MODE, v).apply(); }

    // Focus mode
    public boolean isFocusModeActive()         { return prefs.getBoolean(KEY_FOCUS_MODE_ON, true); }
    public void    setFocusModeActive(boolean v){ editor.putBoolean(KEY_FOCUS_MODE_ON, v).apply(); }
    public String  getFocusEndTime()            { return prefs.getString(KEY_FOCUS_END_TIME, "7:00 PM"); }
    public void    setFocusEndTime(String t)    { editor.putString(KEY_FOCUS_END_TIME, t).apply(); }

    // Entertainment window
    public String getEntertainmentStart()         { return prefs.getString(KEY_ENTERTAINMENT_START, "19:00"); }
    public void   setEntertainmentStart(String t) { editor.putString(KEY_ENTERTAINMENT_START, t).apply(); }
    public String getEntertainmentEnd()           { return prefs.getString(KEY_ENTERTAINMENT_END, "20:00"); }
    public void   setEntertainmentEnd(String t)   { editor.putString(KEY_ENTERTAINMENT_END, t).apply(); }

    // PIN
    public String getAppLockPin()          { return prefs.getString(KEY_APP_LOCK_PIN, ""); }
    public void   setAppLockPin(String pin){ editor.putString(KEY_APP_LOCK_PIN, pin).apply(); }
    public boolean hasPin()                { return !getAppLockPin().isEmpty(); }

    // Blocked Apps Getters/Setters
    public boolean isFacebookBlocked()   { return prefs.getBoolean(KEY_BLOCK_FACEBOOK, true); }
    public void setFacebookBlocked(boolean v) { editor.putBoolean(KEY_BLOCK_FACEBOOK, v).apply(); }

    public boolean isYoutubeBlocked()    { return prefs.getBoolean(KEY_BLOCK_YOUTUBE, true); }
    public void setYoutubeBlocked(boolean v)  { editor.putBoolean(KEY_BLOCK_YOUTUBE, v).apply(); }

    public boolean isInstagramBlocked()  { return prefs.getBoolean(KEY_BLOCK_INSTAGRAM, true); }
    public void setInstagramBlocked(boolean v){ editor.putBoolean(KEY_BLOCK_INSTAGRAM, v).apply(); }

    public boolean isTiktokBlocked()     { return prefs.getBoolean(KEY_BLOCK_TIKTOK, true); }
    public void setTiktokBlocked(boolean v)   { editor.putBoolean(KEY_BLOCK_TIKTOK, v).apply(); }

    public boolean isMessengerBlocked()  { return prefs.getBoolean(KEY_BLOCK_MESSENGER, true); }
    public void setMessengerBlocked(boolean v){ editor.putBoolean(KEY_BLOCK_MESSENGER, v).apply(); }

    // Reels & Shorts Blocker
    public boolean isReelsBlockerEnabled() { return prefs.getBoolean("reels_blocker", true); }
    public void setReelsBlockerEnabled(boolean v) { editor.putBoolean("reels_blocker", v).apply(); }
}