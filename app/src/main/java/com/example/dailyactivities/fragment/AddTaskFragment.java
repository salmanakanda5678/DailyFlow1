package com.example.dailyactivities.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.dailyactivities.R;
import com.example.dailyactivities.model.DataManager;
import com.example.dailyactivities.model.Task;

public class AddTaskFragment extends Fragment {

    private EditText etTaskName;
    private EditText etDuration;
    private TimePicker timePicker;
    private Button btnSave;

    private String selectedCategory = Task.CAT_STUDY;
    private String selectedEmoji    = "📚";
    private String selectedIconBg   = "#E8F4FF";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_task, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etTaskName = view.findViewById(R.id.et_task_name);
        etDuration = view.findViewById(R.id.et_duration);
        timePicker = view.findViewById(R.id.time_picker);
        btnSave    = view.findViewById(R.id.btn_save_task);

        timePicker.setIs24HourView(false);

        setupCategoryChips(view);

        btnSave.setOnClickListener(v -> saveTask());
    }

    private void setupCategoryChips(View view) {
        View.OnClickListener listener = v -> {
            int id = v.getId();
            if (id == R.id.chip_cat_study) {
                selectedCategory = Task.CAT_STUDY;
                selectedEmoji = "📚";
                selectedIconBg = "#E8F4FF";
            } else if (id == R.id.chip_cat_religion) {
                selectedCategory = Task.CAT_RELIGION;
                selectedEmoji = "🕌";
                selectedIconBg = "#FFF8E1";
            } else if (id == R.id.chip_cat_exercise) {
                selectedCategory = Task.CAT_EXERCISE;
                selectedEmoji = "🏃";
                selectedIconBg = "#FEF0EF";
            } else if (id == R.id.chip_cat_work) {
                selectedCategory = Task.CAT_WORK;
                selectedEmoji = "💼";
                selectedIconBg = "#F0EEFF";
            } else if (id == R.id.chip_cat_food) {
                selectedCategory = Task.CAT_FOOD;
                selectedEmoji = "🍽️";
                selectedIconBg = "#FFF0EB";
            } else if (id == R.id.chip_cat_sleep) {
                selectedCategory = Task.CAT_SLEEP;
                selectedEmoji = "😴";
                selectedIconBg = "#E8F4FF";
            }
        };

        view.findViewById(R.id.chip_cat_study).setOnClickListener(listener);
        view.findViewById(R.id.chip_cat_religion).setOnClickListener(listener);
        view.findViewById(R.id.chip_cat_exercise).setOnClickListener(listener);
        view.findViewById(R.id.chip_cat_work).setOnClickListener(listener);
        view.findViewById(R.id.chip_cat_food).setOnClickListener(listener);
        view.findViewById(R.id.chip_cat_sleep).setOnClickListener(listener);
    }

    private void saveTask() {
        String name = etTaskName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            etTaskName.setError("কাজের নাম লিখুন");
            return;
        }

        String duration = etDuration.getText().toString().trim() + " মিনিট";

        int hour   = timePicker.getHour();
        int minute = timePicker.getMinute();
        String amPm = hour >= 12 ? "PM" : "AM";
        int h12 = hour % 12 == 0 ? 12 : hour % 12;
        String startTime = String.format("%d:%02d %s", h12, minute, amPm);

        Task task = new Task(selectedEmoji, name, startTime, duration,
                selectedCategory, Task.STATUS_PENDING, selectedIconBg);

        DataManager.getInstance().addTask(task);

        Toast.makeText(getContext(), "✓ \"" + name + "\" যোগ করা হয়েছে!", Toast.LENGTH_SHORT).show();
        etTaskName.setText("");
        etDuration.setText("");
    }
}
