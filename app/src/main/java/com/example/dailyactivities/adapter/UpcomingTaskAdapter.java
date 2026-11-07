package com.example.dailyactivities.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dailyactivities.R;
import com.dailyroutine.model.Task;
import java.util.List;

public class UpcomingTaskAdapter extends RecyclerView.Adapter<UpcomingTaskAdapter.ViewHolder> {

    private final List<Task> tasks;

    public UpcomingTaskAdapter(List<Task> tasks) {
        this.tasks = tasks;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_upcoming, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Task task = tasks.get(position);

        holder.tvTimeBadge.setText(task.getStartTime());
        holder.tvName.setText(task.getEmoji() + " " + task.getName());
        holder.tvDuration.setText(task.getDurationLabel() + (task.isFocusMode() ? " • 🔕 Focus" : ""));
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTimeBadge;
        TextView tvName;
        TextView tvDuration;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTimeBadge = itemView.findViewById(R.id.tv_upcoming_time);
            tvName      = itemView.findViewById(R.id.tv_upcoming_name);
            tvDuration  = itemView.findViewById(R.id.tv_upcoming_duration);
        }
    }
}
