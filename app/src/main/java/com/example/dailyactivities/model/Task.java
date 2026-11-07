package com.example.dailyactivities.model;

public class Task {

    public static final int STATUS_DONE    = 0;
    public static final int STATUS_RUNNING = 1;
    public static final int STATUS_PENDING = 2;
    public static final int STATUS_LOCKED  = 3;

    public static final String CAT_STUDY    = "পড়াশোনা";
    public static final String CAT_RELIGION = "ধর্মীয়";
    public static final String CAT_EXERCISE = "ব্যায়াম";
    public static final String CAT_WORK     = "কাজ";
    public static final String CAT_FOOD     = "খাবার";
    public static final String CAT_SLEEP    = "ঘুম";
    public static final String CAT_SOCIAL   = "সোশ্যাল";

    private String emoji;
    private String name;
    private String timeLabel;
    private String startTime;
    private String duration;
    private String category;
    private int status;
    private int progress;
    private boolean focusMode;
    private String iconBgColor;
    private boolean notificationOn;
    private boolean alarmOn;

    public Task() {}

    public Task(String emoji, String name, String startTime, String duration,
                String category, int status, String iconBgColor) {
        this.emoji       = emoji;
        this.name        = name;
        this.startTime   = startTime;
        this.duration    = duration;
        this.timeLabel   = startTime + " • " + duration;
        this.category    = category;
        this.status      = status;
        this.iconBgColor = iconBgColor;
        this.progress    = 0;
    }

    public String getEmoji()          { return emoji; }
    public String getName()           { return name; }
    public String getTimeLabel()      { return timeLabel; }
    public String getStartTime()      { return startTime; }
    public String getDuration()       { return duration; }
    public String getCategory()       { return category; }
    public int    getStatus()         { return status; }
    public int    getProgress()       { return progress; }
    public boolean isFocusMode()      { return focusMode; }
    public String getIconBgColor()    { return iconBgColor; }
    public boolean isNotificationOn() { return notificationOn; }
    public boolean isAlarmOn()        { return alarmOn; }

    public void setEmoji(String emoji)               { this.emoji = emoji; }
    public void setName(String name)                 { this.name = name; }
    public void setTimeLabel(String timeLabel)       { this.timeLabel = timeLabel; }
    public void setStartTime(String startTime)       { this.startTime = startTime; }
    public void setDuration(String duration)         { this.duration = duration; }
    public void setCategory(String category)         { this.category = category; }
    public void setStatus(int status)                { this.status = status; }
    public void setProgress(int progress)            { this.progress = progress; }
    public void setFocusMode(boolean focusMode)      { this.focusMode = focusMode; }
    public void setIconBgColor(String iconBgColor)   { this.iconBgColor = iconBgColor; }
    public void setNotificationOn(boolean v)         { this.notificationOn = v; }
    public void setAlarmOn(boolean v)                { this.alarmOn = v; }
}
