package com.example.dailyactivities.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dailyactivities.R;
import com.example.dailyactivities.adapter.TaskAdapter;
import com.example.dailyactivities.model.DataManager;
import com.example.dailyactivities.model.Task;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.List;

public class TasksFragment extends Fragment implements TaskAdapter.OnTaskClickListener {

    private RecyclerView rvTasks;
    private TaskAdapter adapter;
    private List<Task> allTasks;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tasks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        allTasks = DataManager.getInstance().getAllTasks();

        rvTasks = view.findViewById(R.id.rv_tasks);
        rvTasks.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new TaskAdapter(new ArrayList<>(allTasks), this);
        rvTasks.setAdapter(adapter);

        view.findViewById(R.id.chip_all).setOnClickListener(v -> filterTasks(R.id.chip_all));
        view.findViewById(R.id.chip_morning).setOnClickListener(v -> filterTasks(R.id.chip_morning));
        view.findViewById(R.id.chip_noon).setOnClickListener(v -> filterTasks(R.id.chip_noon));
        view.findViewById(R.id.chip_afternoon).setOnClickListener(v -> filterTasks(R.id.chip_afternoon));
        view.findViewById(R.id.chip_night).setOnClickListener(v -> filterTasks(R.id.chip_night));
    }

    private void filterTasks(int chipId) {
        List<Task> filtered = new ArrayList<>();

        if (chipId == R.id.chip_all) {
            filtered.addAll(allTasks);

        } else if (chipId == R.id.chip_morning) {
            for (Task t : allTasks) {
                String time = t.getStartTime();
                if (time.contains("AM") || time.contains("৫") || time.contains("৯") || time.contains("১০") || time.contains("১১")) {
                    filtered.add(t);
                }
            }

        } else if (chipId == R.id.chip_noon) {
            for (Task t : allTasks) {
                String time = t.getStartTime();
                if (time.contains("PM") && (time.contains("১:") || time.contains("২:") || time.contains("৩:"))) {
                    filtered.add(t);
                }
            }

        } else if (chipId == R.id.chip_afternoon) {
            for (Task t : allTasks) {
                String time = t.getStartTime();
                if (time.contains("PM") && (time.contains("৪:") || time.contains("৫:") || time.contains("৬:"))) {
                    filtered.add(t);
                }
            }

        } else if (chipId == R.id.chip_night) {
            for (Task t : allTasks) {
                String time = t.getStartTime();
                if (time.contains("PM") && (time.contains("৭:") || time.contains("৮:") || time.contains("৯:") || time.contains("১০:"))) {
                    filtered.add(t);
                }
            }
        }

        adapter.updateList(filtered);
    }

    @Override
    public void onTaskToggle(Task task, int position) {
        task.setStatus(Task.STATUS_DONE);
        adapter.notifyItemChanged(position);
        Toast.makeText(getContext(), task.getName() + " সম্পন্ন! 🎉", Toast.LENGTH_SHORT).show();
    }
}
