package com.dailyroutine.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dailyactivities.R;
import com.dailyroutine.adapter.HomeTaskAdapter;
import com.dailyroutine.database.DatabaseHelper;
import com.dailyroutine.model.Task;
import com.dailyroutine.util.PreferenceManager;

import android.os.Handler;
import android.os.Looper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextView tvGreeting, tvDate, tvProgressLabel;
    private ProgressBar progressBar, pbCurrentTask;
    private TextView tvFocusStatus, tvFocusDetail;
    private View focusCard;
    private TextView tvCurrentTask, tvCurrentTaskTime;
    private View currentTaskCard, noOngoingCard;
    private Button btnComplete, btnSkip;
    private RecyclerView rvUpcoming;
    private HomeTaskAdapter adapter;

    private DatabaseHelper db;
    private PreferenceManager prefs;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateTimeRunnable = new Runnable() {
        @Override
        public void run() {
            updateGreetingAndDate();
            loadData();
            handler.postDelayed(this, 60000);
        }
    };

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db    = DatabaseHelper.getInstance(requireContext());
        prefs = PreferenceManager.getInstance(requireContext());

        tvGreeting       = view.findViewById(R.id.tv_greeting);
        tvDate           = view.findViewById(R.id.tv_date);
        tvProgressLabel  = view.findViewById(R.id.tv_progress_label);
        progressBar      = view.findViewById(R.id.progress_bar);
        focusCard        = view.findViewById(R.id.card_focus_mode);
        tvFocusStatus    = view.findViewById(R.id.tv_focus_status);
        tvFocusDetail    = view.findViewById(R.id.tv_focus_detail);
        currentTaskCard  = view.findViewById(R.id.card_current_task);
        noOngoingCard    = view.findViewById(R.id.card_no_ongoing);
        tvCurrentTask    = view.findViewById(R.id.tv_current_task_name);
        tvCurrentTaskTime= view.findViewById(R.id.tv_current_task_time);
        pbCurrentTask    = view.findViewById(R.id.pb_current_task);
        btnComplete      = view.findViewById(R.id.btn_complete);
        btnSkip          = view.findViewById(R.id.btn_skip);
        rvUpcoming       = view.findViewById(R.id.rv_upcoming);

        adapter = new HomeTaskAdapter();
        adapter.setOnTaskEditListener(new HomeTaskAdapter.OnTaskEditListener() {
            @Override
            public void onEdit(Task task) {
                if (isFocusTaskLocked(task)) {
                    Toast.makeText(requireContext(), "🔒 ফোকাস মোড চলাকালীন এই টাস্কটি এডিট করা যাবে না!", Toast.LENGTH_SHORT).show();
                    return;
                }
                getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, AddTaskFragment.newInstance(task))
                    .addToBackStack(null)
                    .commit();
            }

            @Override
            public void onDelete(Task task) {
                if (isFocusTaskLocked(task)) {
                    Toast.makeText(requireContext(), "🔒 ফোকাস মোড চলাকালীন এই টাস্কটি ডিলিট করা যাবে না!", Toast.LENGTH_SHORT).show();
                    return;
                }
                new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("ডিলিট করবেন?")
                    .setMessage("আপনি কি নিশ্চিতভাবে '" + task.getName() + "' টাস্কটি ডিলিট করতে চান?")
                    .setPositiveButton("হ্যাঁ, ডিলিট", (dialog, which) -> {
                        com.dailyroutine.util.AlarmHelper.cancelAlarm(requireContext(), task.getId());
                        db.deleteTask(task.getId());
                        loadData();
                        Toast.makeText(requireContext(), "টাস্কটি ডিলিট করা হয়েছে", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("না", null)
                    .show();
            }
        });

        rvUpcoming.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvUpcoming.setAdapter(adapter);
        rvUpcoming.setNestedScrollingEnabled(false);

        loadData();
    }

    @Override public void onResume() {
        super.onResume();
        loadData();
        handler.post(updateTimeRunnable);
    }

    @Override public void onPause() {
        super.onPause();
        handler.removeCallbacks(updateTimeRunnable);
    }

    private boolean isFocusTaskLocked(Task task) {
        if (task == null || !task.isFocusMode()) return false;
        if (Task.STATUS_COMPLETED.equals(task.getStatus())) return false;
        
        Task activeFocus = db.getActiveFocusTask();
        if (activeFocus != null && activeFocus.getId() == task.getId()) {
            return true;
        }

        try {
            Calendar now = Calendar.getInstance();
            int currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
            String[] p = task.getStartTime().split(":");
            int startMins = Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
            int endMins = startMins + task.getDurationMinutes();
            return (currentMinutes >= startMins && currentMinutes < endMins) || Task.STATUS_ONGOING.equals(task.getStatus());
        } catch (Exception e) {
            return false;
        }
    }

    private void loadData() {
        String today = DatabaseHelper.todayStr();
        updateGreetingAndDate();

        // ১. আজকের সকল টাস্ক লোড করি
        List<Task> allTasks = db.getTasksByDate(today);

        // ২. অটো-কমপ্লিট লজিক: সময় পার হয়ে গেলে অটো সম্পন্ন মার্ক করো
        Calendar now = Calendar.getInstance();
        int currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        boolean taskStatusChanged = false;

        for (Task t : allTasks) {
            if (t.getStatus().equals(Task.STATUS_PENDING) || t.getStatus().equals(Task.STATUS_ONGOING)) {
                try {
                    String[] p = t.getStartTime().split(":");
                    int startMins = Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
                    int endMins = startMins + t.getDurationMinutes();

                    if (currentMinutes >= endMins) {
                        db.updateStatus(t.getId(), Task.STATUS_COMPLETED);
                        t.setStatus(Task.STATUS_COMPLETED);
                        taskStatusChanged = true;
                    }
                } catch (Exception ignored) {}
            }
        }
        
        // যদি কোনো টাস্ক অটো-কমপ্লিট হয়, তবে প্রগ্রেস বার আপডেট করার জন্য আবার ডাটা চেক করো
        if (taskStatusChanged) {
            // No need to reload allTasks as we updated the objects inside the list
        }

        // ৩. প্রগ্রেস বার আপডেট
        int completed = 0;
        for (Task t : allTasks) if (t.getStatus().equals(Task.STATUS_COMPLETED)) completed++;
        int total = allTasks.size();
        int pct = (total == 0) ? 0 : (completed * 100) / total;
        progressBar.setProgress(pct);
        tvProgressLabel.setText(completed + "/" + total + " সম্পন্ন");

        // ৩. ফোকাস মোড লজিক
        Task activeFocus = db.getActiveFocusTask();
        if (prefs.isAppBlockerEnabled() && activeFocus != null) {
            focusCard.setVisibility(View.VISIBLE);
            String endTime = calculateEndTimeOnly(activeFocus);
            tvFocusDetail.setText("Facebook, YouTube বন্ধ • " + endTime + " পর্যন্ত");
        } else {
            focusCard.setVisibility(View.GONE);
        }

        // ৪. বর্তমান চলমান (Ongoing) টাস্ক লজিক
        Task ongoing = db.getOngoingTask();
        if (ongoing == null) ongoing = findTaskByTime(allTasks);

        if (ongoing != null) {
            currentTaskCard.setVisibility(View.VISIBLE);
            noOngoingCard.setVisibility(View.GONE);
            tvCurrentTask.setText(ongoing.getEmoji() + " " + ongoing.getName());
            
            int remaining = calculateRemainingMinutes(ongoing);
            int progress = calculateProgress(ongoing);
            tvCurrentTaskTime.setText(formatTimeRange(ongoing.getStartTime(), ongoing.getDurationMinutes(), remaining));
            pbCurrentTask.setProgress(progress);

            final Task current = ongoing;
            btnComplete.setOnClickListener(v -> {
                if (isFocusTaskLocked(current)) {
                    Toast.makeText(requireContext(), "🔒 ফোকাস মোড চলছে! সময় শেষ না হওয়া পর্যন্ত এই টাস্কটি সম্পূর্ণ বা বন্ধ করা যাবে না।", Toast.LENGTH_LONG).show();
                    return;
                }
                db.updateStatus(current.getId(), Task.STATUS_COMPLETED);
                loadData();
            });
            btnSkip.setOnClickListener(v -> {
                if (isFocusTaskLocked(current)) {
                    Toast.makeText(requireContext(), "🔒 ফোকাস মোড চলছে! সময় শেষ না হওয়া পর্যন্ত এই টাস্কটি স্কিপ করা যাবে না।", Toast.LENGTH_LONG).show();
                    return;
                }
                db.updateStatus(current.getId(), Task.STATUS_SKIPPED);
                loadData();
            });
        } else {
            currentTaskCard.setVisibility(View.GONE);
            noOngoingCard.setVisibility(View.VISIBLE);
        }

        // ৫. নিচের লিস্টে আজকের সব টাস্ক সেট করি
        adapter.setTasks(allTasks);
    }

    private String formatTimeRange(String start, int durMin, int remainingMin) {
        try {
            String[] p = start.split(":");
            int h = Integer.parseInt(p[0]);
            int m = Integer.parseInt(p[1]);
            int endH = (h + (m + durMin) / 60) % 24;
            int endM = (m + durMin) % 60;
            return format12(h, m) + " – " + format12(endH, endM) + " • " + remainingMin + " মিনিট বাকি";
        } catch (Exception e) { return ""; }
    }

    private int calculateRemainingMinutes(Task task) {
        try {
            String[] p = task.getStartTime().split(":");
            int startMins = Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
            int endMins = startMins + task.getDurationMinutes();
            Calendar now = Calendar.getInstance();
            int currentMins = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
            return Math.max(0, endMins - currentMins);
        } catch (Exception e) { return 0; }
    }

    private int calculateProgress(Task task) {
        try {
            String[] p = task.getStartTime().split(":");
            int startMins = Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
            int total = task.getDurationMinutes();
            Calendar now = Calendar.getInstance();
            int currentMins = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
            int elapsed = currentMins - startMins;
            if (elapsed < 0) return 0;
            if (elapsed >= total) return 100;
            return (elapsed * 100) / total;
        } catch (Exception e) { return 0; }
    }

    private String format12(int h, int m) {
        String ampm = h < 12 ? "AM" : "PM";
        int h12 = (h == 0) ? 12 : (h > 12 ? h - 12 : h);
        return String.format("%d:%02d %s", h12, m, ampm);
    }

    private String calculateEndTimeOnly(Task task) {
        try {
            String[] p = task.getStartTime().split(":");
            int startMins = Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
            int endMins = startMins + task.getDurationMinutes();
            return format12((endMins / 60) % 24, endMins % 60);
        } catch (Exception e) { return ""; }
    }

    private void updateGreetingAndDate() {
        tvGreeting.setText(getGreeting() + ", " + prefs.getUserName() + "! 👋");
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("bn", "BD"));
        tvDate.setText("আজ " + sdf.format(new Date()));
    }

    private String getGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 12)  return "শুভ সকাল";
        if (hour >= 12 && hour < 16) return "শুভ দুপুর";
        if (hour >= 16 && hour < 18) return "শুভ বিকেল";
        if (hour >= 18 && hour < 20) return "শুভ সন্ধ্যা";
        return "শুভ রাত্রি";
    }

    private Task findTaskByTime(List<Task> tasks) {
        Calendar now = Calendar.getInstance();
        int currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        for (Task t : tasks) {
            if (t.getStatus().equals(Task.STATUS_COMPLETED) || t.getStatus().equals(Task.STATUS_SKIPPED)) continue;
            try {
                String[] p = t.getStartTime().split(":");
                int startMins = Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
                int endMins = startMins + t.getDurationMinutes();
                if (currentMinutes >= startMins && currentMinutes < endMins) return t;
            } catch (Exception ignored) {}
        }
        return null;
    }
}