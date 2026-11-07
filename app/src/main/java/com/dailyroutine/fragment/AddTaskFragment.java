package com.dailyroutine.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.dailyactivities.R;
import com.dailyroutine.database.DatabaseHelper;
import com.dailyroutine.model.Task;
import com.dailyroutine.util.AlarmHelper;
import com.dailyroutine.util.PreferenceManager;

import java.util.Locale;

public class AddTaskFragment extends Fragment {

    // Category chips
    private static final String[] CATEGORIES = {
        Task.CAT_STUDY, Task.CAT_RELIGION, Task.CAT_EXERCISE,
        Task.CAT_WORK, Task.CAT_FOOD, Task.CAT_SLEEP, Task.CAT_OTHERS
    };
    private static final int[] CAT_IDS = {
        R.id.chip_cat_study, R.id.chip_cat_religion, R.id.chip_cat_exercise,
        R.id.chip_cat_work, R.id.chip_cat_food, R.id.chip_cat_sleep, R.id.chip_cat_others
    };

    // Recurrence chips
    private static final String[] RECURRENCES = {
        Task.REC_NONE, Task.REC_DAILY, Task.REC_WEEKLY
    };
    private static final int[] REC_IDS = {
        R.id.chip_rec_none, R.id.chip_rec_daily, R.id.chip_rec_weekly
    };

    private EditText etTaskName;
    private EditText etDuration;
    private TextView tvTimeDisplay;
    private TimePicker timePicker;
    private Button btnSave;
    private View btnFocus;
    private TextView tvFocusLabel;

    private Task existingTask = null;

    private String selectedCategory  = Task.CAT_STUDY;
    private String selectedRecurrence = Task.REC_NONE;
    private boolean focusEnabled     = false;

    private DatabaseHelper db;

    public static AddTaskFragment newInstance(Task task) {
        AddTaskFragment fragment = new AddTaskFragment();
        fragment.existingTask = task;
        return fragment;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_task, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = DatabaseHelper.getInstance(requireContext());

        etTaskName   = view.findViewById(R.id.et_task_name);
        etDuration   = view.findViewById(R.id.et_duration);
        tvTimeDisplay= view.findViewById(R.id.tv_time_display);
        timePicker   = view.findViewById(R.id.time_picker);
        btnSave      = view.findViewById(R.id.btn_save_task);
        btnFocus     = view.findViewById(R.id.btn_focus_mode);
        tvFocusLabel = view.findViewById(R.id.tv_focus_label);

        timePicker.setIs24HourView(false);
        timePicker.setHour(10);
        timePicker.setMinute(0);

        timePicker.setOnTimeChangedListener((tp, h, m) -> {
            String ampm = h < 12 ? "AM" : "PM";
            int h12 = h % 12 == 0 ? 12 : h % 12;
            tvTimeDisplay.setText(String.format("%d:%02d %s", h12, m, ampm));
        });
        tvTimeDisplay.setText("10:00 AM");

        // Category chips
        setupChips(view, CAT_IDS, CATEGORIES, 0, true);

        // Recurrence chips
        setupChips(view, REC_IDS, RECURRENCES, 0, false);

        // Focus mode toggle button
        if (btnFocus != null) {
            btnFocus.setOnClickListener(v -> {
                focusEnabled = !focusEnabled;
                if (tvFocusLabel != null) {
                    tvFocusLabel.setText(focusEnabled ? "🔕 Focus চালু" : "🔕 Focus Mode");
                    tvFocusLabel.setTextColor(focusEnabled ? Color.parseColor("#2ECC71") : Color.parseColor("#7F8C8D"));
                }
            });
        }

        btnSave.setOnClickListener(v -> saveTask());

        if (existingTask != null) {
            setupForEdit();
        }
    }

    private void setupForEdit() {
        etTaskName.setText(existingTask.getName());
        etDuration.setText(String.valueOf(existingTask.getDurationMinutes()));
        
        try {
            String[] p = existingTask.getStartTime().split(":");
            int h = Integer.parseInt(p[0]);
            int m = Integer.parseInt(p[1]);
            timePicker.setHour(h);
            timePicker.setMinute(m);
            
            String ampm = h < 12 ? "AM" : "PM";
            int h12 = h % 12 == 0 ? 12 : h % 12;
            tvTimeDisplay.setText(String.format("%d:%02d %s", h12, m, ampm));
        } catch (Exception ignored) {}

        // Set Category
        selectedCategory = existingTask.getCategory();
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (CATEGORIES[i].equals(selectedCategory)) {
                setupChips(getView(), CAT_IDS, CATEGORIES, i, true);
                break;
            }
        }

        // Set Recurrence
        selectedRecurrence = existingTask.getRecurrence();
        for (int i = 0; i < RECURRENCES.length; i++) {
            if (RECURRENCES[i].equals(selectedRecurrence)) {
                setupChips(getView(), REC_IDS, RECURRENCES, i, false);
                break;
            }
        }

        // Set Focus
        focusEnabled = existingTask.isFocusMode();
        if (tvFocusLabel != null) {
            tvFocusLabel.setText(focusEnabled ? "🔕 Focus চালু" : "🔕 Focus Mode");
            tvFocusLabel.setTextColor(focusEnabled ? Color.parseColor("#2ECC71") : Color.parseColor("#7F8C8D"));
        }

        btnSave.setText("আপডেট করুন");
    }

    private void setupChips(View root, int[] ids, String[] labels,
                            int defaultSel, boolean isCategory) {
        for (int i = 0; i < ids.length; i++) {
            final int idx = i;
            TextView chip = root.findViewById(ids[i]);
            if (chip == null) continue;
            // default selection
            if (i == defaultSel) setChipSelected(chip, true);
            chip.setOnClickListener(v -> {
                if (isCategory) selectedCategory  = labels[idx];
                else            selectedRecurrence = labels[idx];
                // Deselect all, select this
                for (int j = 0; j < ids.length; j++) {
                    TextView c = root.findViewById(ids[j]);
                    if (c != null) setChipSelected(c, j == idx);
                }
            });
        }
    }

    private void setChipSelected(TextView chip, boolean sel) {
        chip.setBackgroundResource(sel
            ? R.drawable.chip_selected_bg : R.drawable.chip_default_bg);
        chip.setTextColor(sel
            ? Color.WHITE : Color.parseColor("#7F8C8D"));
    }

    private void saveTask() {
        String name = etTaskName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            etTaskName.setError("কাজের নাম দিন");
            etTaskName.requestFocus();
            return;
        }

        int durMin = 30;
        String durStr = etDuration.getText().toString().trim();
        if (!TextUtils.isEmpty(durStr)) {
            try { durMin = Integer.parseInt(durStr); } catch (NumberFormatException ignored) {}
        }

        int hour   = timePicker.getHour();
        int minute = timePicker.getMinute();
        String time = String.format(Locale.US, "%02d:%02d", hour, minute);

        int excludeId = (existingTask != null) ? existingTask.getId() : -1;
        Task conflict = db.getConflictingTask(time, durMin, excludeId);

        if (conflict != null) {
            String conflictTimeRange = conflict.getFormattedTimeRange();
            String errorMsg = String.format(Locale.getDefault(),
                "⚠️ সময়ের সংঘাত! '%s' (%s) কাজের সময় ইতিমধ্যেই নির্ধারিত আছে। ওভারল্যাপ এড়াতে অন্য সময় নির্বাচন করুন।",
                conflict.getName(), conflictTimeRange);

            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("⏰ সময় ওভারল্যাপ সংঘাত")
                .setMessage(errorMsg)
                .setPositiveButton("ঠিক আছে", null)
                .show();
            return;
        }

        if (existingTask != null) {
            existingTask.setName(name);
            existingTask.setCategory(selectedCategory);
            existingTask.setRecurrence(selectedRecurrence);
            existingTask.setStartTime(time);
            existingTask.setDurationMinutes(durMin);
            existingTask.setFocusMode(focusEnabled);

            if (db.updateTask(existingTask)) {
                PreferenceManager pManager = PreferenceManager.getInstance(requireContext());
                if (pManager.isTaskAlarmEnabled() || pManager.isPushNotifEnabled()) {
                    AlarmHelper.scheduleAlarm(requireContext(), existingTask);
                }
                Toast.makeText(requireContext(), "✅ Task আপডেট হয়েছে!", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            } else {
                Toast.makeText(requireContext(), "❌ আপডেট করা যায়নি", Toast.LENGTH_SHORT).show();
            }
        } else {
            Task task = new Task(name, selectedCategory, selectedRecurrence,
                time, durMin, focusEnabled);

            long id = db.insertTask(task);
            if (id > 0) {
                task.setId((int) id);
                PreferenceManager pManager = PreferenceManager.getInstance(requireContext());
                if (pManager.isTaskAlarmEnabled() || pManager.isPushNotifEnabled()) {
                    AlarmHelper.scheduleAlarm(requireContext(), task);
                }
                Toast.makeText(requireContext(), "✅ Task যোগ হয়েছে!", Toast.LENGTH_SHORT).show();
                etTaskName.setText("");
                etDuration.setText("");
                timePicker.setHour(10);
                timePicker.setMinute(0);
            } else {
                Toast.makeText(requireContext(), "❌ Error! আবার চেষ্টা করুন", Toast.LENGTH_SHORT).show();
            }
        }
    }
}