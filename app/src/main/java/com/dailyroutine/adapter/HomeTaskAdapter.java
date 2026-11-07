package com.dailyroutine.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dailyactivities.R;
import com.dailyroutine.model.Task;

import java.util.ArrayList;
import java.util.List;

public class HomeTaskAdapter extends RecyclerView.Adapter<HomeTaskAdapter.VH> {

    public interface OnTaskEditListener {
        void onEdit(Task task);
        void onDelete(Task task);
    }

    private List<Task> tasks = new ArrayList<>();
    private OnTaskEditListener editListener;

    public void setTasks(List<Task> t) {
        this.tasks = t;
        notifyDataSetChanged();
    }

    public void setOnTaskEditListener(OnTaskEditListener listener) {
        this.editListener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_home_task, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Task t = tasks.get(pos);
        h.tvTime.setText(formatTime12(t.getStartTime()));
        h.tvEmoji.setText(t.getEmoji());
        h.tvName.setText(t.getName());
        String sub = t.getDurationLabel();
        if (t.isFocusMode()) sub += " • 🔕 Focus";
        h.tvSub.setText(sub);

        // time bg
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(12f);
        gd.setColor(Color.parseColor("#444444"));
        h.tvTime.setBackground(gd);

        h.btnEdit.setOnClickListener(v -> {
            if (editListener != null) editListener.onEdit(t);
        });

        h.itemView.setOnLongClickListener(v -> {
            if (editListener != null) editListener.onDelete(t);
            return true;
        });
    }

    private String formatTime12(String time24) {
        if (time24 == null) return "";
        try {
            String[] p = time24.split(":");
            int h = Integer.parseInt(p[0]);
            int m = Integer.parseInt(p[1]);
            String ampm = h < 12 ? "AM" : "PM";
            if (h == 0) h = 12;
            else if (h > 12) h -= 12;
            return h + ":" + String.format("%02d", m) + "\n" + ampm;
        } catch (Exception e) { return time24; }
    }

    @Override public int getItemCount() { return tasks.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTime, tvEmoji, tvName, tvSub;
        ImageView btnEdit;
        VH(View v) {
            super(v);
            tvTime  = v.findViewById(R.id.tv_home_task_time);
            tvEmoji = v.findViewById(R.id.tv_home_task_emoji);
            tvName  = v.findViewById(R.id.tv_home_task_name);
            tvSub   = v.findViewById(R.id.tv_home_task_sub);
            btnEdit = v.findViewById(R.id.btn_edit_task);
        }
    }
}