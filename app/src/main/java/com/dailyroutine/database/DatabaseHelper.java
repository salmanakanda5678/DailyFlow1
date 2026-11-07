package com.dailyroutine.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.dailyroutine.model.Task;
import com.dailyroutine.util.AlarmHelper;
import com.dailyroutine.util.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME    = "daily_routine.db";
    private static final int    DB_VERSION = 1;

    // Table
    private static final String TABLE_TASKS = "tasks";
    // Columns
    private static final String COL_ID       = "id";
    private static final String COL_NAME     = "name";
    private static final String COL_CATEGORY = "category";
    private static final String COL_REC      = "recurrence";
    private static final String COL_TIME     = "start_time";
    private static final String COL_DUR      = "duration_minutes";
    private static final String COL_STATUS   = "status";
    private static final String COL_FOCUS    = "focus_mode";
    private static final String COL_DATE     = "date_added";

    private static final String CREATE_TABLE =
        "CREATE TABLE " + TABLE_TASKS + " (" +
        COL_ID       + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COL_NAME     + " TEXT NOT NULL, " +
        COL_CATEGORY + " TEXT, " +
        COL_REC      + " TEXT, " +
        COL_TIME     + " TEXT, " +
        COL_DUR      + " INTEGER DEFAULT 30, " +
        COL_STATUS   + " TEXT DEFAULT 'pending', " +
        COL_FOCUS    + " INTEGER DEFAULT 0, " +
        COL_DATE     + " TEXT" +
        ")";

    // History Table
    private static final String TABLE_HISTORY        = "daily_history";
    private static final String COL_HIST_DATE       = "hist_date";
    private static final String COL_HIST_TOTAL      = "total_count";
    private static final String COL_HIST_COMPLETED  = "completed_count";

    private static final String CREATE_HISTORY_TABLE =
        "CREATE TABLE IF NOT EXISTS " + TABLE_HISTORY + " (" +
        COL_HIST_DATE      + " TEXT PRIMARY KEY, " +
        COL_HIST_TOTAL     + " INTEGER DEFAULT 0, " +
        COL_HIST_COMPLETED + " INTEGER DEFAULT 0" +
        ")";

    private static DatabaseHelper instance;
    private final Context mContext;

    public static synchronized DatabaseHelper getInstance(Context ctx) {
        if (instance == null) instance = new DatabaseHelper(ctx.getApplicationContext());
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.mContext = context;
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
        db.execSQL(CREATE_HISTORY_TABLE);
    }

    @Override public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        db.execSQL(CREATE_HISTORY_TABLE);
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        onCreate(db);
    }

    // ── Default tasks ─────────────────────────────────────────────────────────
    private void insertDefaults(SQLiteDatabase db) {
        String today = todayStr();
        Object[][] defaults = {
            {"ফজরের নামাজ",    Task.CAT_RELIGION, Task.REC_DAILY,  "05:15", 15,  false, Task.STATUS_COMPLETED},
            {"Quran তেলাওয়াত", Task.CAT_RELIGION, Task.REC_DAILY,  "09:30", 30,  false, Task.STATUS_ONGOING},
            {"পড়াশোনা",        Task.CAT_STUDY,    Task.REC_DAILY,  "10:00", 90,  true,  Task.STATUS_PENDING},
            {"ব্যায়াম",         Task.CAT_EXERCISE, Task.REC_DAILY,  "11:30", 30,  false, Task.STATUS_PENDING},
            {"দুপুরের খাবার",   Task.CAT_FOOD,     Task.REC_DAILY,  "13:00", 45,  false, Task.STATUS_PENDING},
            {"কাজ / প্রজেক্ট",  Task.CAT_WORK,     Task.REC_DAILY,  "14:00", 180, true,  Task.STATUS_PENDING},
            {"Social Media সময়",Task.CAT_SLEEP,    Task.REC_DAILY,  "19:00", 60,  false, Task.STATUS_LOCKED},
        };
        for (Object[] d : defaults) {
            ContentValues cv = new ContentValues();
            cv.put(COL_NAME,     (String)  d[0]);
            cv.put(COL_CATEGORY, (String)  d[1]);
            cv.put(COL_REC,      (String)  d[2]);
            cv.put(COL_TIME,     (String)  d[3]);
            cv.put(COL_DUR,      (Integer) d[4]);
            cv.put(COL_FOCUS,    ((Boolean)d[5]) ? 1 : 0);
            cv.put(COL_STATUS,   (String)  d[6]);
            cv.put(COL_DATE,     today);
            long id = db.insert(TABLE_TASKS, null, cv);

            // Schedule alarms for default tasks
            if (id > 0 && PreferenceManager.getInstance(mContext).isTaskAlarmEnabled()) {
                Task t = new Task();
                t.setId((int)id);
                t.setName((String)d[0]);
                t.setCategory((String)d[1]);
                t.setRecurrence((String)d[2]);
                t.setStartTime((String)d[3]);
                t.setDurationMinutes((Integer)d[4]);
                t.setFocusMode((Boolean)d[5]);
                t.setStatus((String)d[6]);
                AlarmHelper.scheduleAlarm(mContext, t);
            }
        }
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public long insertTask(Task t) {
        ContentValues cv = toCV(t);
        cv.put(COL_DATE, todayStr());
        return getWritableDatabase().insert(TABLE_TASKS, null, cv);
    }

    public boolean updateTask(Task t) {
        int rows = getWritableDatabase()
            .update(TABLE_TASKS, toCV(t), COL_ID + "=?",
                new String[]{String.valueOf(t.getId())});
        return rows > 0;
    }

    public boolean updateStatus(int id, String status) {
        ContentValues cv = new ContentValues();
        cv.put(COL_STATUS, status);
        int rows = getWritableDatabase()
            .update(TABLE_TASKS, cv, COL_ID + "=?",
                new String[]{String.valueOf(id)});
        if (rows > 0) {
            String today = todayStr();
            saveDailyHistory(today, countTotal(today), countCompleted(today));
        }
        return rows > 0;
    }

    public boolean deleteTask(int id) {
        Task activeFocus = getActiveFocusTask();
        if (activeFocus != null && activeFocus.getId() == id) {
            return false;
        }
        return getWritableDatabase()
            .delete(TABLE_TASKS, COL_ID + "=?",
                new String[]{String.valueOf(id)}) > 0;
    }

    public void deleteAllTasks() {
        Task activeFocus = getActiveFocusTask();
        if (activeFocus != null) {
            getWritableDatabase().delete(TABLE_TASKS, COL_ID + "!=?",
                new String[]{String.valueOf(activeFocus.getId())});
        } else {
            getWritableDatabase().delete(TABLE_TASKS, null, null);
        }
    }

    public List<Task> getAllTasks() {
        return query(null, null);
    }

    public List<Task> getTasksByDate(String date) {
        // আজকের নির্দিষ্ট তারিখের কাজ অথবা প্রতিদিনের রুটিন (Daily Recurrence) লোড হবে
        List<Task> tasks = query(COL_DATE + "=? OR " + COL_REC + "=?",
            new String[]{date, Task.REC_DAILY});

        // প্রতিদিনের রুটিন (Daily) টাস্কগুলোর স্ট্যাটাস প্রতিদিন রিসেট করার লজিক
        for (Task t : tasks) {
            if (Task.REC_DAILY.equals(t.getRecurrence()) && !date.equals(t.getDateAdded())) {
                // যদি এটি অন্য কোনো দিনের (পুরানো) ডেইলি টাস্ক হয়, তবে এর স্ট্যাটাস রিসেট করে আজকের জন্য আপডেট করো
                t.setStatus(Task.STATUS_PENDING);
                t.setDateAdded(date);
                updateTask(t);
                // ডাটাবেসে ডেট আপডেট করার জন্য updateTask-এ ডেট ফিল্ড যোগ করা প্রয়োজন, অথবা আলাদা মেথড
                updateTaskDateAndStatus(t.getId(), date, Task.STATUS_PENDING);
            }
        }
        return tasks;
    }

    private void updateTaskDateAndStatus(int id, String date, String status) {
        ContentValues cv = new ContentValues();
        cv.put(COL_DATE, date);
        cv.put(COL_STATUS, status);
        getWritableDatabase().update(TABLE_TASKS, cv, COL_ID + "=?",
            new String[]{String.valueOf(id)});
    }

    public List<Task> getCompletedTasksForDate(String date) {
        return query(
            "(" + COL_DATE + "=? OR " + COL_REC + "=?) AND " + COL_STATUS + "=?",
            new String[]{date, Task.REC_DAILY, Task.STATUS_COMPLETED});
    }

    public Task getOngoingTask() {
        List<Task> list = query(COL_STATUS + "=?", new String[]{Task.STATUS_ONGOING});
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * বর্তমানে কোনো Focus Task চলছে কি না তা খুঁজে বের করে।
     * প্রথমে 'ongoing' স্ট্যাটাস চেক করে, তারপর বর্তমান সময়ের ভিত্তিতে চেক করে।
     */
    public Task getActiveFocusTask() {
        // ১. ম্যানুয়ালি স্টার্ট করা ongoing focus task চেক করি
        List<Task> ongoing = query(COL_STATUS + "=? AND " + COL_FOCUS + "=1", new String[]{Task.STATUS_ONGOING});
        if (!ongoing.isEmpty()) return ongoing.get(0);

        // ২. বর্তমান সময়ের ভিত্তিতে কোন focus task হওয়ার কথা তা চেক করি
        String today = todayStr();
        Calendar now = Calendar.getInstance();
        int currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

        List<Task> tasks = getTasksByDate(today);
        for (Task t : tasks) {
            // যদি টাস্কটি focus mode-এর হয় এবং সম্পন্ন না হয়ে থাকে
            if (!t.isFocusMode() || t.getStatus().equals(Task.STATUS_COMPLETED)) continue;

            try {
                String[] p = t.getStartTime().split(":");
                int startMins = Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
                int endMins = startMins + t.getDurationMinutes();

                if (currentMinutes >= startMins && currentMinutes < endMins) {
                    return t;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * Checks if a proposed task time range overlaps with any existing task for today.
     * @param startTime "HH:mm"
     * @param durationMinutes duration in minutes
     * @param excludeTaskId ID of task to ignore (when updating existing task), or -1 for new task.
     * @return Overlapping Task if conflict exists, or null if no conflict.
     */
    public Task getConflictingTask(String startTime, int durationMinutes, int excludeTaskId) {
        try {
            String[] p = startTime.split(":");
            int startB = Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
            int endB = startB + durationMinutes;

            String today = todayStr();
            List<Task> existingTasks = getTasksByDate(today);

            for (Task t : existingTasks) {
                if (t.getId() == excludeTaskId) continue;

                String[] tP = t.getStartTime().split(":");
                int startA = Integer.parseInt(tP[0]) * 60 + Integer.parseInt(tP[1]);
                int endA = startA + t.getDurationMinutes();

                // Mathematical overlap check: startA < endB && startB < endA
                if (startA < endB && startB < endA) {
                    return t;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<Task> query(String where, String[] args) {
        List<Task> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query(
            TABLE_TASKS, null, where, args, null, null, COL_TIME + " ASC");
        if (c.moveToFirst()) {
            do { list.add(fromCursor(c)); } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    private Task fromCursor(Cursor c) {
        Task t = new Task();
        t.setId(      c.getInt(   c.getColumnIndexOrThrow(COL_ID)));
        t.setName(    c.getString(c.getColumnIndexOrThrow(COL_NAME)));
        t.setCategory(c.getString(c.getColumnIndexOrThrow(COL_CATEGORY)));
        t.setRecurrence(c.getString(c.getColumnIndexOrThrow(COL_REC)));
        t.setStartTime( c.getString(c.getColumnIndexOrThrow(COL_TIME)));
        t.setDurationMinutes(c.getInt(c.getColumnIndexOrThrow(COL_DUR)));
        t.setStatus(  c.getString(c.getColumnIndexOrThrow(COL_STATUS)));
        t.setFocusMode(c.getInt(  c.getColumnIndexOrThrow(COL_FOCUS)) == 1);
        t.setDateAdded(c.getString(c.getColumnIndexOrThrow(COL_DATE)));
        return t;
    }

    private ContentValues toCV(Task t) {
        ContentValues cv = new ContentValues();
        cv.put(COL_NAME,     t.getName());
        cv.put(COL_CATEGORY, t.getCategory());
        cv.put(COL_REC,      t.getRecurrence());
        cv.put(COL_TIME,     t.getStartTime());
        cv.put(COL_DUR,      t.getDurationMinutes());
        cv.put(COL_STATUS,   t.getStatus());
        cv.put(COL_FOCUS,    t.isFocusMode() ? 1 : 0);
        return cv;
    }

    public static String todayStr() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    /** Count completed tasks for given date */
    public int countCompleted(String date) {
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT COUNT(*) FROM " + TABLE_TASKS +
            " WHERE (" + COL_DATE + "=? OR " + COL_REC + "=?) AND " + COL_STATUS + "=?",
            new String[]{date, Task.REC_DAILY, Task.STATUS_COMPLETED});
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    /** Count total tasks for given date */
    public int countTotal(String date) {
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT COUNT(*) FROM " + TABLE_TASKS +
            " WHERE " + COL_DATE + "=? OR " + COL_REC + "=?",
            new String[]{date, Task.REC_DAILY});
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    public void saveDailyHistory(String date, int total, int completed) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_HIST_DATE, date);
        cv.put(COL_HIST_TOTAL, total);
        cv.put(COL_HIST_COMPLETED, completed);
        db.insertWithOnConflict(TABLE_HISTORY, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public int getHistoryTotal(String date) {
        String today = todayStr();
        if (today.equals(date)) {
            int t = countTotal(date);
            saveDailyHistory(today, t, countCompleted(date));
            return t;
        }
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_HISTORY, new String[]{COL_HIST_TOTAL},
            COL_HIST_DATE + "=?", new String[]{date}, null, null, null);
        int total = 0;
        if (c.moveToFirst()) total = c.getInt(0);
        c.close();
        return total;
    }

    public int getHistoryCompleted(String date) {
        String today = todayStr();
        if (today.equals(date)) {
            int comp = countCompleted(date);
            saveDailyHistory(today, countTotal(date), comp);
            return comp;
        }
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_HISTORY, new String[]{COL_HIST_COMPLETED},
            COL_HIST_DATE + "=?", new String[]{date}, null, null, null);
        int completed = 0;
        if (c.moveToFirst()) completed = c.getInt(0);
        c.close();
        return completed;
    }

    public int calculateStreak() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        int streak = 0;

        String todayStr = sdf.format(cal.getTime());
        int todayCompleted = countCompleted(todayStr);

        if (todayCompleted > 0) {
            streak++;
            cal.add(Calendar.DAY_OF_YEAR, -1);
            while (true) {
                String d = sdf.format(cal.getTime());
                int comp = getHistoryCompleted(d);
                if (comp > 0) {
                    streak++;
                    cal.add(Calendar.DAY_OF_YEAR, -1);
                } else {
                    break;
                }
            }
        } else {
            // Check yesterday
            cal.add(Calendar.DAY_OF_YEAR, -1);
            while (true) {
                String d = sdf.format(cal.getTime());
                int comp = getHistoryCompleted(d);
                if (comp > 0) {
                    streak++;
                    cal.add(Calendar.DAY_OF_YEAR, -1);
                } else {
                    break;
                }
            }
        }
        return streak;
    }
}