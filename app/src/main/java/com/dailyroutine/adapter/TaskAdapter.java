package com.dailyroutine.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dailyactivities.R;
import com.dailyroutine.model.Task;

import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.VH> {

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
        void onStatusToggle(Task task);
        void onTaskLongClick(Task task);
    }

    private List<Task> allTasks = new ArrayList<>();
    private List<Task> filteredTasks = new ArrayList<>();
    private OnTaskClickListener listener;
    private String currentFilter = "সব";

    public TaskAdapter(OnTaskClickListener l) { this.listener = l; }

    public void setTasks(List<Task> tasks) {
        this.allTasks = tasks;
        applyFilter(currentFilter);
    }

    public void applyFilter(String filter) {
        currentFilter = filter;
        filteredTasks.clear();
        for (Task t : allTasks) {
            if (filter.equals("সব") || matchesTimeFilter(t, filter)) {
                filteredTasks.add(t);
            }
        }
        notifyDataSetChanged();
    }

    private boolean matchesTimeFilter(Task t, String filter) {
        if (t.getStartTime() == null) return false;
        String[] parts = t.getStartTime().split(":");
        if (parts.length < 2) return false;
        int h = Integer.parseInt(parts[0]);
        switch (filter) {
            case "সকাল":  return h >= 4  && h < 12;
            case "দুপুর":  return h >= 12 && h < 15;
            case "বিকেল":  return h >= 15 && h < 19;
            case "রাত":    return h >= 19 || h < 4;
            default: return true;
        }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_task, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Task t = filteredTasks.get(pos);

        // Emoji icon background
        h.tvEmoji.setText(t.getEmoji());
        try {
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(36f);
            bg.setColor(Color.parseColor(t.getIconBgHex()));
            h.tvEmoji.setBackground(bg);
        } catch (Exception ignored) {}

        h.tvName.setText(t.getName());

        // Time + duration
        String timeLabel = t.getStartTime() != null
            ? formatTime(t.getStartTime()) + " • " + t.getDurationLabel()
            : t.getDurationLabel();
        if (t.isFocusMode()) timeLabel += " • 🔕 Focus";
        h.tvTime.setText(timeLabel);

        // Status badge
        setStatusBadge(h, t);

        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onTaskClick(t); });
        h.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onTaskLongClick(t);
            return true;
        });
        h.tvBadge.setOnClickListener(v -> { if (listener != null) listener.onStatusToggle(t); });
    }

    private void setStatusBadge(VH h, Task t) {
        h.tvBadge.setVisibility(View.VISIBLE);
        switch (t.getStatus()) {
            case Task.STATUS_COMPLETED:
                h.tvBadge.setText("✓ সম্পন্ন");
                setBadgeBg(h.tvBadge, "#2ECC71", "#E8F8F0");
                break;
            case Task.STATUS_ONGOING:
                h.tvBadge.setText("চলছে");
                setBadgeBg(h.tvBadge, "#F1C40F", "#FFFBEA");
                break;
            case Task.STATUS_LOCKED:
                h.tvBadge.setText("লকড 🔒");
                setBadgeBg(h.tvBadge, "#E74C3C", "#FEF0EF");
                break;
            case Task.STATUS_SKIPPED:
                h.tvBadge.setText("স্কিপড ⏭");
                setBadgeBg(h.tvBadge, "#7F8C8D", "#F0F4F8");
                break;
            default:
                h.tvBadge.setVisibility(View.GONE);
                break;
        }
    }

    private void setBadgeBg(TextView tv, String textColor, String bgColor) {
        try {
            tv.setTextColor(Color.parseColor(textColor));
            GradientDrawable gd = new GradientDrawable();
            gd.setCornerRadius(40f);
            gd.setColor(Color.parseColor(bgColor));
            tv.setBackground(gd);
        } catch (Exception ignored) {}
    }

    private String formatTime(String time24) {
        try {
            String[] p = time24.split(":");
            int h = Integer.parseInt(p[0]);
            int m = Integer.parseInt(p[1]);
            String ampm = h < 12 ? "AM" : "PM";
            if (h == 0) h = 12;
            else if (h > 12) h -= 12;
            return String.format("%d:%02d %s", h, m, ampm);
        } catch (Exception e) { return time24; }
    }

    @Override public int getItemCount() { return filteredTasks.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvEmoji, tvName, tvTime, tvBadge;
        VH(View v) {
            super(v);
            tvEmoji = v.findViewById(R.id.tv_task_emoji);
            tvName  = v.findViewById(R.id.tv_task_name);
            tvTime  = v.findViewById(R.id.tv_task_time);
            tvBadge = v.findViewById(R.id.tv_task_badge);
        }
    }
}