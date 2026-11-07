package com.dailyroutine.fragment;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.dailyactivities.R;
import com.dailyroutine.database.DatabaseHelper;
import com.dailyroutine.model.Task;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatsFragment extends Fragment {

    private LinearLayout barChartLayout;
    private LinearLayout streakLayout;
    private TextView tvStreakSummary;
    private ProgressBar pbStudy, pbReligion, pbExercise;
    private TextView tvStudyPct, tvReligionPct, tvExercisePct;
    private TextView tvTotalTasks, tvCompletedTasks, tvStreakDays, tvCompletionRate;

    private DatabaseHelper db;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = DatabaseHelper.getInstance(requireContext());

        barChartLayout  = view.findViewById(R.id.bar_chart_layout);
        streakLayout    = view.findViewById(R.id.streak_layout);
        tvStreakSummary = view.findViewById(R.id.tv_streak_summary);
        pbStudy         = view.findViewById(R.id.pb_study);
        pbReligion      = view.findViewById(R.id.pb_religion);
        pbExercise      = view.findViewById(R.id.pb_exercise);
        tvStudyPct      = view.findViewById(R.id.tv_study_pct);
        tvReligionPct   = view.findViewById(R.id.tv_religion_pct);
        tvExercisePct   = view.findViewById(R.id.tv_exercise_pct);
        tvTotalTasks    = view.findViewById(R.id.tv_stat_total);
        tvCompletedTasks= view.findViewById(R.id.tv_stat_completed);
        tvStreakDays    = view.findViewById(R.id.tv_stat_streak);
        tvCompletionRate= view.findViewById(R.id.tv_stat_rate);

        loadStats();
    }

    @Override public void onResume() { super.onResume(); loadStats(); }

    private void loadStats() {
        String today = DatabaseHelper.todayStr();
        int total     = db.countTotal(today);
        int completed = db.countCompleted(today);
        int streak    = db.calculateStreak();
        int rate      = total == 0 ? 0 : (completed * 100) / total;

        // Save today's stats into history table
        db.saveDailyHistory(today, total, completed);

        if (tvTotalTasks != null)     tvTotalTasks.setText(String.valueOf(total));
        if (tvCompletedTasks != null) tvCompletedTasks.setText(String.valueOf(completed));
        if (tvStreakDays != null)     tvStreakDays.setText(String.valueOf(streak));
        if (tvCompletionRate != null) tvCompletionRate.setText(rate + "%");

        buildBarChart();
        buildStreakRow();
        buildCategoryStats();
    }

    private void buildBarChart() {
        if (barChartLayout == null) return;
        barChartLayout.removeAllViews();

        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.SUNDAY);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String todayStr = sdf.format(new Date());
        String[] labels = {"রবি", "সোম", "মঙ্গল", "বুধ", "বৃহ", "শুক্র", "শনি"};
        int maxH = 100; // dp max bar height

        for (int i = 0; i < 7; i++) {
            String date = sdf.format(cal.getTime());
            boolean isToday = todayStr.equals(date);

            int total = db.getHistoryTotal(date);
            int comp  = db.getHistoryCompleted(date);
            int pct   = total == 0 ? 0 : (comp * 100) / total;

            // Container Column for each day
            LinearLayout col = new LinearLayout(requireContext());
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams colP = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            col.setLayoutParams(colP);

            // Outer Bar Track Background (100dp height)
            LinearLayout trackView = new LinearLayout(requireContext());
            trackView.setOrientation(LinearLayout.VERTICAL);
            trackView.setGravity(android.view.Gravity.BOTTOM);
            LinearLayout.LayoutParams trackP = new LinearLayout.LayoutParams(dpToPx(18), dpToPx(maxH));
            trackP.bottomMargin = dpToPx(6);
            trackView.setLayoutParams(trackP);

            GradientDrawable trackBg = new GradientDrawable();
            trackBg.setCornerRadius(dpToPx(9));
            trackBg.setColor(Color.parseColor("#EAEFF4")); // subtle background track
            trackView.setBackground(trackBg);

            // Inner Fill Bar View
            View fillBar = new View(requireContext());
            int barHeightDp = pct > 0 ? Math.max(6, (pct * maxH) / 100) : 0;
            LinearLayout.LayoutParams fillP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(barHeightDp));
            fillBar.setLayoutParams(fillP);

            GradientDrawable fillBg = new GradientDrawable();
            fillBg.setCornerRadius(dpToPx(9));
            fillBg.setColor(isToday ? Color.parseColor("#27AE60") : Color.parseColor("#2ECC71"));
            fillBar.setBackground(fillBg);

            trackView.addView(fillBar);
            col.addView(trackView);

            // Day Label
            TextView lbl = new TextView(requireContext());
            lbl.setText(labels[i]);
            lbl.setTextSize(10f);
            lbl.setGravity(android.view.Gravity.CENTER);
            if (isToday) {
                lbl.setTextColor(Color.parseColor("#27AE60"));
                lbl.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                lbl.setTextColor(Color.parseColor("#7F8C8D"));
                lbl.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
            col.addView(lbl);

            barChartLayout.addView(col);
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    private void buildStreakRow() {
        if (streakLayout == null) return;
        streakLayout.removeAllViews();

        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.SUNDAY);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String todayStr = sdf.format(new Date());

        String[] days = {"S", "M", "T", "W", "T", "F", "S"};
        int completedDays = 0;

        for (int i = 0; i < 7; i++) {
            String dateStr = sdf.format(cal.getTime());
            boolean isToday = todayStr.equals(dateStr);

            int comp = db.getHistoryCompleted(dateStr);
            boolean isDone = comp > 0;

            if (isDone) completedDays++;

            TextView cell = new TextView(requireContext());
            cell.setText(days[i]);
            cell.setTextSize(11f);
            cell.setTypeface(null, android.graphics.Typeface.BOLD);
            cell.setGravity(android.view.Gravity.CENTER);

            int size = dpToPx(36);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(dpToPx(3), 0, dpToPx(3), 0);
            cell.setLayoutParams(lp);

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dpToPx(10));

            if (isDone) {
                bg.setColor(isToday ? Color.parseColor("#27AE60") : Color.parseColor("#2ECC71"));
                cell.setTextColor(Color.WHITE);
            } else if (isToday) {
                bg.setColor(Color.parseColor("#D5F5E3"));
                bg.setStroke(dpToPx(2), Color.parseColor("#27AE60"));
                cell.setTextColor(Color.parseColor("#27AE60"));
            } else {
                bg.setColor(Color.parseColor("#E8ECF0"));
                cell.setTextColor(Color.parseColor("#7F8C8D"));
            }
            cell.setBackground(bg);
            streakLayout.addView(cell);
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (tvStreakSummary != null)
            tvStreakSummary.setText("গত ৭ দিনে " + completedDays + " দিন সম্পন্ন");
    }

    private void buildCategoryStats() {
        String today = DatabaseHelper.todayStr();
        List<Task> tasks = db.getTasksByDate(today);

        Map<String, int[]> catMap = new HashMap<>();
        catMap.put(Task.CAT_STUDY,    new int[]{0, 0});
        catMap.put(Task.CAT_RELIGION, new int[]{0, 0});
        catMap.put(Task.CAT_EXERCISE, new int[]{0, 0});

        for (Task t : tasks) {
            int[] arr = catMap.get(t.getCategory());
            if (arr != null) {
                arr[0]++;
                if (t.getStatus().equals(Task.STATUS_COMPLETED)) arr[1]++;
            }
        }

        setPct(pbStudy,    tvStudyPct,    catMap.get(Task.CAT_STUDY),    0);
        setPct(pbReligion, tvReligionPct, catMap.get(Task.CAT_RELIGION), 0);
        setPct(pbExercise, tvExercisePct, catMap.get(Task.CAT_EXERCISE), 0);
    }

    private void setPct(ProgressBar pb, TextView tv, int[] arr, int fallback) {
        int pct = fallback;
        if (arr != null && arr[0] > 0) pct = (arr[1] * 100) / arr[0];
        if (pb != null) pb.setProgress(pct);
        if (tv != null) tv.setText(pct + "%");
    }

    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}