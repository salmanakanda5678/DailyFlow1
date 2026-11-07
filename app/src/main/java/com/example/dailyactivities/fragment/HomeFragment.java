package com.example.dailyactivities.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.example.dailyactivities.adapter.UpcomingTaskAdapter;
import com.dailyroutine.database.DatabaseHelper;
import com.dailyroutine.model.Task;
import com.dailyroutine.util.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextView tvGreeting, tvDate, tvProgressLabel;
    private ProgressBar progressBar;
    private View focusCard;
    private TextView tvFocusStatus, tvFocusDetail;
    private TextView tvCurrentTask, tvCurrentTaskTime;
    private View currentTaskCard, noOngoingCard;
    private Button btnComplete, btnSkip;
    private RecyclerView rvUpcoming;

    private PreferenceManager prefs;
    private DatabaseHelper db;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateTimeRunnable = new Runnable() {
        @Override
        public void run() {
            updateGreetingAndDate();
            checkTaskVisibility();
            handler.postDelayed(this, 60000);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = PreferenceManager.getInstance(requireContext());
        db = DatabaseHelper.getInstance(requireContext());

        // Views
        tvGreeting = view.findViewById(R.id.tv_greeting);
        tvDate = view.findViewById(R.id.tv_date);
        tvProgressLabel = view.findViewById(R.id.tv_progress_label);
        progressBar = view.findViewById(R.id.progress_bar);
        focusCard = view.findViewById(R.id.card_focus_mode);
        tvFocusStatus = view.findViewById(R.id.tv_focus_status);
        tvFocusDetail = view.findViewById(R.id.tv_focus_detail);
        currentTaskCard = view.findViewById(R.id.card_current_task);
        noOngoingCard = view.findViewById(R.id.card_no_ongoing);
        tvCurrentTask = view.findViewById(R.id.tv_current_task_name);
        tvCurrentTaskTime = view.findViewById(R.id.tv_current_task_time);
        btnComplete = view.findViewById(R.id.btn_complete);
        btnSkip = view.findViewById(R.id.btn_skip);
        rvUpcoming = view.findViewById(R.id.rv_upcoming);

        rvUpcoming.setLayoutManager(new LinearLayoutManager(getContext()));
        rvUpcoming.setNestedScrollingEnabled(false);

        loadData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
        handler.post(updateTimeRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(updateTimeRunnable);
    }

    private void loadData() {
        String today = DatabaseHelper.todayStr();

        updateGreetingAndDate();

        // Progress
        int completed = db.countCompleted(today);
        int total = db.countTotal(today);
        if (total == 0) total = 1;
        int pct = (completed * 100) / total;
        progressBar.setProgress(pct);
        tvProgressLabel.setText(completed + "/" + total + " সম্পন্ন");

        // Focus mode
        if (prefs.isFocusModeActive()) {
            focusCard.setVisibility(View.VISIBLE);
            tvFocusStatus.setText("Focus Mode চালু আছে");
            tvFocusDetail.setText("Facebook, YouTube বন্ধ • " + prefs.getFocusEndTime() + " পর্যন্ত");
        } else {
            focusCard.setVisibility(View.GONE);
        }

        List<Task> allToday = db.getTasksByDate(today);

        // Current Task
        Task ongoing = db.getOngoingTask();
        if (ongoing == null) {
            ongoing = findTaskByTime(allToday);
        }

        if (ongoing != null) {
            currentTaskCard.setVisibility(View.VISIBLE);
            noOngoingCard.setVisibility(View.GONE);
            tvCurrentTask.setText(ongoing.getEmoji() + " " + ongoing.getName());
            tvCurrentTaskTime.setText(ongoing.getStartTime() + " • " + ongoing.getDurationMinutes() + " মিনিট বাকি");

            final Task taskToUpdate = ongoing;
            btnComplete.setOnClickListener(v -> {
                db.updateStatus(taskToUpdate.getId(), Task.STATUS_COMPLETED);
                Toast.makeText(requireContext(), "✅ সম্পন্ন হয়েছে!", Toast.LENGTH_SHORT).show();
                loadData();
            });
            btnSkip.setOnClickListener(v -> {
                db.updateStatus(taskToUpdate.getId(), Task.STATUS_PENDING);
                Toast.makeText(requireContext(), "⏭ পরের জন্য রাখা হল", Toast.LENGTH_SHORT).show();
                loadData();
            });
        } else {
            currentTaskCard.setVisibility(View.GONE);
            noOngoingCard.setVisibility(View.VISIBLE);
        }

        // Upcoming
        List<Task> upcoming = new ArrayList<>();
        for (Task t : allToday) {
            if (t.getStatus().equals(Task.STATUS_PENDING)) {
                upcoming.add(t);
                if (upcoming.size() >= 4) break;
            }
        }
        
        // This fragment might need adjustments based on its specific UpcomingTaskAdapter
        rvUpcoming.setAdapter(new UpcomingTaskAdapter(upcoming));
    }

    private void updateGreetingAndDate() {
        if (tvGreeting != null && prefs != null) {
            tvGreeting.setText(getGreeting() + ", " + prefs.getUserName() + "! 👋");
        }
        if (tvDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("bn", "BD"));
            tvDate.setText("আজ " + sdf.format(new Date()));
        }
    }

    private void checkTaskVisibility() {
        // Refresh full data to check ongoing task changes
        loadData();
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
            if (t.getStatus().equals(Task.STATUS_COMPLETED)) continue;
            if (t.getStartTime() == null) continue;

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
}
