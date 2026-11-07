package com.dailyroutine.model;

public class Task {
    // Categories
    public static final String CAT_STUDY    = "পড়াশোনা";
    public static final String CAT_RELIGION = "ধর্মীয়";
    public static final String CAT_EXERCISE = "ব্যায়াম";
    public static final String CAT_WORK     = "কাজ";
    public static final String CAT_FOOD     = "খাবার";
    public static final String CAT_SLEEP    = "ঘুম";
    public static final String CAT_OTHERS   = "অন্যান্য";

    // Recurrence
    public static final String REC_NONE    = "কোনো নয়";
    public static final String REC_DAILY   = "প্রতিদিন";
    public static final String REC_WEEKLY  = "সাপ্তাহিক";

    // Status
    public static final String STATUS_PENDING   = "pending";
    public static final String STATUS_ONGOING   = "ongoing";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_SKIPPED   = "skipped";
    public static final String STATUS_LOCKED    = "locked";

    private int    id;
    private String name;
    private String category;
    private String recurrence;
    private String startTime;      // "HH:mm"
    private int    durationMinutes;
    private String status;
    private boolean focusMode;
    private String dateAdded;      // "yyyy-MM-dd"

    public Task() { this.status = STATUS_PENDING; this.recurrence = REC_NONE; }

    public Task(String name, String category, String recurrence,
                String startTime, int durationMinutes, boolean focusMode) {
        this.name = name;
        this.category = category;
        this.recurrence = recurrence;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.focusMode = focusMode;
        this.status = STATUS_PENDING;
    }

    // ---- Emoji helper ----
    public String getEmoji() {
        if (category == null) return "📌";
        switch (category) {
            case CAT_STUDY:    return "📚";
            case CAT_RELIGION: return "🕌";
            case CAT_EXERCISE: return "🏃";
            case CAT_WORK:     return "💼";
            case CAT_FOOD:     return "🍽️";
            case CAT_SLEEP:    return "😴";
            case CAT_OTHERS:   return "📌";
            default:           return "📌";
        }
    }

    /** Returns background color (hex string) for the category icon */
    public String getIconBgHex() {
        if (category == null) return "#F0F4F8";
        switch (category) {
            case CAT_STUDY:    return "#E8F4FF";
            case CAT_RELIGION: return "#FFF8E1";
            case CAT_EXERCISE: return "#FEF0EF";
            case CAT_WORK:     return "#F0EEFF";
            case CAT_FOOD:     return "#FFF8E1";
            case CAT_SLEEP:    return "#E8F8F0";
            case CAT_OTHERS:   return "#F5F5F5";
            default:           return "#F0F4F8";
        }
    }

    /** Formatted duration label */
    public String getDurationLabel() {
        if (durationMinutes < 60) return durationMinutes + " মিনিট";
        int h = durationMinutes / 60, m = durationMinutes % 60;
        return m == 0 ? h + " ঘণ্টা" : h + " ঘণ্টা " + m + " মিনিট";
    }

    /** Formatted time range (e.g. 7:00 AM – 7:50 AM) */
    public String getFormattedTimeRange() {
        if (startTime == null || startTime.isEmpty()) return "";
        try {
            String[] p = startTime.split(":");
            int startH = Integer.parseInt(p[0]);
            int startM = Integer.parseInt(p[1]);
            int totalMins = startH * 60 + startM + durationMinutes;

            int endH = (totalMins / 60) % 24;
            int endM = totalMins % 60;

            String startAmpm = startH < 12 ? "AM" : "PM";
            int sH12 = startH % 12 == 0 ? 12 : startH % 12;

            String endAmpm = endH < 12 ? "AM" : "PM";
            int eH12 = endH % 12 == 0 ? 12 : endH % 12;

            return String.format(java.util.Locale.US, "%d:%02d %s – %d:%02d %s",
                    sH12, startM, startAmpm, eH12, endM, endAmpm);
        } catch (Exception e) {
            return startTime;
        }
    }

    // ---- Getters / Setters ----
    public int    getId()              { return id; }
    public void   setId(int id)        { this.id = id; }
    public String getName()            { return name; }
    public void   setName(String n)    { this.name = n; }
    public String getCategory()        { return category; }
    public void   setCategory(String c){ this.category = c; }
    public String getRecurrence()      { return recurrence; }
    public void   setRecurrence(String r){ this.recurrence = r; }
    public String getStartTime()       { return startTime; }
    public void   setStartTime(String t){ this.startTime = t; }
    public int    getDurationMinutes()       { return durationMinutes; }
    public void   setDurationMinutes(int d)  { this.durationMinutes = d; }
    public String getStatus()          { return status; }
    public void   setStatus(String s)  { this.status = s; }
    public boolean isFocusMode()       { return focusMode; }
    public void   setFocusMode(boolean f){ this.focusMode = f; }
    public String getDateAdded()       { return dateAdded; }
    public void   setDateAdded(String d){ this.dateAdded = d; }
}