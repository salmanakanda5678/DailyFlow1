package com.dailyroutine.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dailyactivities.R;
import com.dailyroutine.adapter.TaskAdapter;
import com.dailyroutine.database.DatabaseHelper;
import com.dailyroutine.model.Task;

import java.util.List;

public class TasksFragment extends Fragment implements TaskAdapter.OnTaskClickListener {

    private RecyclerView rvTasks;
    private TaskAdapter  adapter;
    private TextView     tvTitle;
    private DatabaseHelper db;

    // Filter chips
    private String[] filterLabels = {"সব", "সকাল", "দুপুর", "বিকেল", "রাত"};
    private int[]    filterIds    = {
        R.id.chip_all, R.id.chip_morning, R.id.chip_noon,
        R.id.chip_afternoon, R.id.chip_night
    };
    private String currentFilter = "সব";

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tasks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = DatabaseHelper.getInstance(requireContext());

        tvTitle = view.findViewById(R.id.tv_tasks_title);
        rvTasks = view.findViewById(R.id.rv_tasks);
        rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TaskAdapter(this);
        rvTasks.setAdapter(adapter);

        // Filter chips
        for (int i = 0; i < filterIds.length; i++) {
            final String label = filterLabels[i];
            TextView chip = view.findViewById(filterIds[i]);
            chip.setOnClickListener(v -> {
                currentFilter = label;
                updateChipStyles(view);
                adapter.applyFilter(label);
            });
        }
        updateChipStyles(view);
        loadTasks();
    }

    @Override public void onResume() { super.onResume(); loadTasks(); }

    private void loadTasks() {
        List<Task> tasks = db.getTasksByDate(DatabaseHelper.todayStr());
        int completed = 0;
        for (Task t : tasks) if (t.getStatus().equals(Task.STATUS_COMPLETED)) completed++;
        tvTitle.setText("আজকের Routine");
        View v = getView();
        if (v != null) {
            TextView tvSub = v.findViewById(R.id.tv_tasks_sub);
            if (tvSub != null) tvSub.setText(completed + "/" + tasks.size() + " সম্পন্ন");
        }
        adapter.setTasks(tasks);
        adapter.applyFilter(currentFilter);
    }

    private void updateChipStyles(View root) {
        int GREEN = 0xFF2ECC71, WHITE = 0xFFFFFFFF,
            BORDER = 0xFFE8ECF0, MUTED = 0xFF7F8C8D;
        for (int i = 0; i < filterIds.length; i++) {
            TextView chip = root.findViewById(filterIds[i]);
            if (chip == null) continue;
            boolean active = filterLabels[i].equals(currentFilter);
            chip.setBackgroundResource(active
                ? R.drawable.chip_selected_bg
                : R.drawable.chip_default_bg);
            chip.setTextColor(active ? WHITE : MUTED);
        }
    }

    private boolean isFocusTaskLocked(Task task) {
        if (task == null || !task.isFocusMode()) return false;
        if (Task.STATUS_COMPLETED.equals(task.getStatus())) return false;

        Task activeFocus = db.getActiveFocusTask();
        if (activeFocus != null && activeFocus.getId() == task.getId()) {
            return true;
        }

        try {
            java.util.Calendar now = java.util.Calendar.getInstance();
            int currentMinutes = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE);
            String[] p = task.getStartTime().split(":");
            int startMins = Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
            int endMins = startMins + task.getDurationMinutes();
            return (currentMinutes >= startMins && currentMinutes < endMins) || Task.STATUS_ONGOING.equals(task.getStatus());
        } catch (Exception e) {
            return false;
        }
    }

    @Override public void onTaskClick(Task task) {
        // Could open detail dialog; for now show toast
        Toast.makeText(requireContext(),
            task.getName() + " – " + task.getStatus(), Toast.LENGTH_SHORT).show();
    }

    @Override public void onStatusToggle(Task task) {
        if (isFocusTaskLocked(task)) {
            Toast.makeText(requireContext(), "🔒 ফোকাস মোড চলছে! সময় শেষ না হওয়া পর্যন্ত এই টাস্কটির স্ট্যাটাস পরিবর্তন করা যাবে না।", Toast.LENGTH_LONG).show();
            return;
        }
        String next;
        switch (task.getStatus()) {
            case Task.STATUS_PENDING:   next = Task.STATUS_COMPLETED; break;
            case Task.STATUS_COMPLETED: next = Task.STATUS_PENDING;   break;
            case Task.STATUS_ONGOING:   next = Task.STATUS_COMPLETED; break;
            default: return; // locked → can't toggle
        }
        db.updateStatus(task.getId(), next);
        loadTasks();
    }

    @Override public void onTaskLongClick(Task task) {
        if (isFocusTaskLocked(task)) {
            Toast.makeText(requireContext(), "🔒 ফোকাস মোড চলাকালীন এই টাস্কটি ডিলিট করা যাবে না!", Toast.LENGTH_SHORT).show();
            return;
        }
        new android.app.AlertDialog.Builder(requireContext())
            .setTitle("ডিলিট করবেন?")
            .setMessage("আপনি কি নিশ্চিতভাবে '" + task.getName() + "' টাস্কটি ডিলিট করতে চান?")
            .setPositiveButton("হ্যাঁ, ডিলিট", (dialog, which) -> {
                db.deleteTask(task.getId());
                loadTasks();
                Toast.makeText(requireContext(), "টাস্কটি ডিলিট করা হয়েছে", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("না", null)
            .show();
    }
}