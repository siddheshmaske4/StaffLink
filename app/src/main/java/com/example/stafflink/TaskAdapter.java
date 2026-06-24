package com.example.stafflink;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * TaskAdapter — dedicated adapter for TaskModel.
 * Used in: AdminAssignTaskActivity, EmployeeTaskActivity, AdminTaskMonitorActivity
 * Layout: item_task.xml
 */
public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.VH> {

    public interface OnTaskClickListener {
        void onTaskClick(TaskModel task);
    }

    private final Context            context;
    private final List<TaskModel>    list;
    private       OnTaskClickListener listener;

    public TaskAdapter(Context context, List<TaskModel> list) {
        this.context = context;
        this.list    = list;
    }

    public void setOnTaskClickListener(OnTaskClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_task, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        TaskModel task = list.get(position);

        // Title — bold if unread
        holder.txtTitle.setText(task.title);
        holder.txtTitle.setTypeface(null, task.isRead ? Typeface.NORMAL : Typeface.BOLD);

        // Assigned to (shown in admin monitor view)
        if (task.assignedToName != null && !task.assignedToName.isEmpty()) {
            holder.txtAssignedTo.setVisibility(View.VISIBLE);
            holder.txtAssignedTo.setText("👤 " + task.assignedToName);
        } else {
            holder.txtAssignedTo.setVisibility(View.GONE);
        }

        // Priority
        holder.txtPriority.setText(task.getPriorityDisplay());

        // Status
        holder.txtStatus.setText(task.getStatusDisplay());
        holder.txtStatus.setTextColor(task.getStatusColor());

        // Progress
        holder.progressBar.setProgress(task.progress);
        holder.txtProgress.setText(task.progress + "%");

        // Deadline
        if (task.deadline != null && !task.deadline.isEmpty()) {
            holder.txtDeadline.setVisibility(View.VISIBLE);
            holder.txtDeadline.setText("📅 Due: " + task.deadline);
        } else {
            holder.txtDeadline.setVisibility(View.GONE);
        }

        // Completion request badge
        if (task.isPendingApproval()) {
            holder.txtCompletionBadge.setVisibility(View.VISIBLE);
        } else {
            holder.txtCompletionBadge.setVisibility(View.GONE);
        }

        // Timestamp
        String time = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                .format(new Date(task.createdAt));
        holder.txtTime.setText(time);

        // Click
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTaskClick(task);
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView    txtTitle, txtAssignedTo, txtPriority, txtStatus,
                txtProgress, txtDeadline, txtTime, txtCompletionBadge;
        ProgressBar progressBar;

        VH(@NonNull View v) {
            super(v);
            txtTitle           = v.findViewById(R.id.txtTaskTitle);
            txtAssignedTo      = v.findViewById(R.id.txtAssignedTo);
            txtPriority        = v.findViewById(R.id.txtTaskPriority);
            txtStatus          = v.findViewById(R.id.txtTaskStatus);
            progressBar        = v.findViewById(R.id.taskProgressBar);
            txtProgress        = v.findViewById(R.id.txtTaskProgress);
            txtDeadline        = v.findViewById(R.id.txtTaskDeadline);
            txtTime            = v.findViewById(R.id.txtTaskTime);
            txtCompletionBadge = v.findViewById(R.id.txtCompletionBadge);
        }
    }
}