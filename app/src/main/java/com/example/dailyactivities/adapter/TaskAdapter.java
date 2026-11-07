package com.example.dailyactivities.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dailyactivities.R;
import com.example.dailyactivities.model.Task;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

    private List<Task> tasks;
    private final OnTaskClickListener listener;

    public interface OnTaskClickListener {
        void onTaskToggle(Task task, int position);
    }

    public TaskAdapter(List<Task> tasks, OnTaskClickListener listener) {
        this.tasks = tasks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Task task = tasks.get(position);

        holder.tvEmoji.setText(task.getEmoji());
        holder.tvName.setText(task.getName());
        holder.tvTime.setText(task.getTimeLabel());

        if (task.isFocusMode()) {
            holder.tvTime.setText(task.getTimeLabel() + " • 🔕 Focus");
        }

        switch (task.getStatus()) {
            case Task.STATUS_DONE:
                holder.tvBadge.setText("✓ সম্পন্ন");
                holder.tvBadge.setTextColor(Color.parseColor("#27AE60"));
                break;

            case Task.STATUS_RUNNING:
                holder.tvBadge.setText("চলছে");
                holder.tvBadge.setTextColor(Color.parseColor("#F39C12"));
                break;

            case Task.STATUS_PENDING:
                holder.tvBadge.setText("বাকি");
                holder.tvBadge.setTextColor(Color.parseColor("#7F8C8D"));
                break;

            case Task.STATUS_LOCKED:
                holder.tvBadge.setText("লক্ড 🔒");
                holder.tvBadge.setTextColor(Color.parseColor("#FF6B35"));
                break;
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskToggle(task, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public void updateList(List<Task> newList) {
        this.tasks = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmoji;
        TextView tvName;
        TextView tvTime;
        TextView tvBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmoji       = itemView.findViewById(R.id.tv_task_emoji);
            tvName        = itemView.findViewById(R.id.tv_task_name);
            tvTime        = itemView.findViewById(R.id.tv_task_time);
            tvBadge       = itemView.findViewById(R.id.tv_task_badge);
        }
    }
}
