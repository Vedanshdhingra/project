package com.example.project;

import android.graphics.Paint;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.TaskViewHolder> {
    private List<Task> tasks;
    private OnTaskClickListener listener;

    public interface OnTaskClickListener {
        void onTaskClick(Task task, int position);
        void onTaskStatusChanged(Task task, int position);
    }

    public TasksAdapter(List<Task> tasks, OnTaskClickListener listener) {
        this.tasks = tasks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.title.setText(task.title);
        
        if (task.description != null && !task.description.isEmpty()) {
            holder.description.setVisibility(View.VISIBLE);
            holder.description.setText(task.description);
        } else {
            holder.description.setVisibility(View.GONE);
        }

        if (task.deadline != null) {
            holder.deadline.setVisibility(View.VISIBLE);
            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
            cal.setTimeInMillis(task.deadline);
            holder.deadline.setText("⏰ " + DateFormat.format("dd MMM, hh:mm a", cal));
        } else {
            holder.deadline.setVisibility(View.GONE);
        }

        holder.reminder.setVisibility(task.reminderEnabled ? View.VISIBLE : View.GONE);
        
        holder.checkbox.setOnCheckedChangeListener(null);
        holder.checkbox.setChecked(task.isCompleted);
        updateTitleStrike(holder.title, task.isCompleted);

        holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.isCompleted = isChecked;
            updateTitleStrike(holder.title, isChecked);
            if (listener != null) listener.onTaskStatusChanged(task, position);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTaskClick(task, position);
        });
    }

    private void updateTitleStrike(TextView textView, boolean isCompleted) {
        if (isCompleted) {
            textView.setPaintFlags(textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            textView.setAlpha(0.5f);
        } else {
            textView.setPaintFlags(textView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            textView.setAlpha(1.0f);
        }
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView title, description, deadline;
        CheckBox checkbox;
        ImageView reminder;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.taskTitle);
            description = itemView.findViewById(R.id.taskDescription);
            deadline = itemView.findViewById(R.id.taskDeadline);
            checkbox = itemView.findViewById(R.id.taskCheckbox);
            reminder = itemView.findViewById(R.id.reminderIndicator);
        }
    }
}