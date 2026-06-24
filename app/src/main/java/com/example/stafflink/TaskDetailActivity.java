package com.example.stafflink;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class TaskDetailActivity extends AppCompatActivity {

    // ─── UI ──────────────────────────────────────────────────────────────────
    private ImageButton  btnBack;
    private TextView     txtTitle, txtDescription, txtDeadline, txtPriority,
            txtStatus, txtProgress, txtSeenStatus, txtAssignedBy,
            txtWorkNotes; // ← shows saved notes (both admin + employee)

    // Admin-only
    private LinearLayout layoutAdminActions, layoutCompletionRequest;
    private Button       btnApprove, btnReject;

    // Employee-only
    private LinearLayout layoutEmployeeActions;
    private SeekBar      seekBarProgress;
    private Button       btnMarkSeen, btnStartTask, btnRequestCompletion, btnSaveNotes;
    private EditText     etWorkNotes; // ← employee types notes here

    // ─── Data ─────────────────────────────────────────────────────────────────
    private String role, taskId, empId, companyCode, adminNode;
    private TaskModel currentTask;
    private DatabaseReference taskRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        role        = getIntent().getStringExtra("ROLE");
        taskId      = getIntent().getStringExtra("TASK_ID");
        empId       = getIntent().getStringExtra("EMP_ID");
        companyCode = getIntent().getStringExtra("COMPANY_CODE");
        adminNode   = getIntent().getStringExtra("ADMIN_NODE");

        if (role == null || taskId == null || empId == null || companyCode == null) {
            Toast.makeText(this, "Missing task data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        btnBack.setOnClickListener(v -> finish());

        taskRef = FirebaseDatabase.getInstance()
                .getReference("companies")
                .child(companyCode)
                .child("employees")
                .child(empId)
                .child("tasks")
                .child(taskId);

        loadTask();
    }

    private void bindViews() {
        btnBack                 = findViewById(R.id.btnBack);
        txtTitle                = findViewById(R.id.txtTaskTitle);
        txtDescription          = findViewById(R.id.txtTaskDescription);
        txtDeadline             = findViewById(R.id.txtTaskDeadline);
        txtPriority             = findViewById(R.id.txtTaskPriority);
        txtStatus               = findViewById(R.id.txtTaskStatus);
        txtProgress             = findViewById(R.id.txtTaskProgress);
        txtSeenStatus           = findViewById(R.id.txtSeenStatus);
        txtAssignedBy           = findViewById(R.id.txtAssignedBy);
        txtWorkNotes            = findViewById(R.id.txtWorkNotes); // ← new

        layoutAdminActions      = findViewById(R.id.layoutAdminActions);
        layoutCompletionRequest = findViewById(R.id.layoutCompletionRequest);
        btnApprove              = findViewById(R.id.btnApprove);
        btnReject               = findViewById(R.id.btnReject);

        layoutEmployeeActions   = findViewById(R.id.layoutEmployeeActions);
        seekBarProgress         = findViewById(R.id.seekBarProgress);
        btnMarkSeen             = findViewById(R.id.btnMarkSeen);
        btnStartTask            = findViewById(R.id.btnStartTask);
        btnRequestCompletion    = findViewById(R.id.btnRequestCompletion);
        btnSaveNotes            = findViewById(R.id.btnSaveNotes); // ← new
        etWorkNotes             = findViewById(R.id.etWorkNotes);  // ← new
    }

    private void loadTask() {
        taskRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentTask = snapshot.getValue(TaskModel.class);
                if (currentTask == null) {
                    Toast.makeText(TaskDetailActivity.this,
                            "Task not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                currentTask.taskId = snapshot.getKey();
                renderUI();

                if ("employee".equals(role) && !currentTask.isRead) {
                    taskRef.child("isRead").setValue(true);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TaskDetailActivity.this,
                        "Failed to load task", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderUI() {
        txtTitle.setText(currentTask.title);
        txtDescription.setText(currentTask.description);
        txtSeenStatus.setText(currentTask.isRead ? "Seen ✓" : "Not seen");
        txtAssignedBy.setText("Assigned by: " + (currentTask.assignedByName != null
                ? currentTask.assignedByName : "Admin"));
        txtDeadline.setText("📅 Due: " + (currentTask.deadline != null
                ? currentTask.deadline : "No deadline"));
        txtPriority.setText("Priority: " + currentTask.getPriorityDisplay());
        txtStatus.setText("Status: " + currentTask.getStatusDisplay());
        txtStatus.setTextColor(currentTask.getStatusColor());
        txtProgress.setText("Progress: " + currentTask.progress + "%");

        // ── Show saved work notes (both admin and employee see this) ──────────
        if (currentTask.workNotes != null && !currentTask.workNotes.isEmpty()) {
            txtWorkNotes.setText(currentTask.workNotes);
            txtWorkNotes.setTextColor(getResources().getColor(android.R.color.black, null));
        } else {
            txtWorkNotes.setText("No notes added yet");
            txtWorkNotes.setTextColor(getResources().getColor(android.R.color.darker_gray, null));
        }

        // ── Pre-fill notes input for employee ─────────────────────────────────
        if ("employee".equals(role) && etWorkNotes != null
                && currentTask.workNotes != null) {
            etWorkNotes.setText(currentTask.workNotes);
        }

        if ("admin".equals(role)) {
            showAdminActions();
        } else {
            showEmployeeActions();
        }
    }

    // =========================================================================
    // ADMIN
    // =========================================================================

    private void showAdminActions() {
        layoutAdminActions.setVisibility(View.VISIBLE);
        layoutEmployeeActions.setVisibility(View.GONE);

        if (currentTask.isPendingApproval()) {
            layoutCompletionRequest.setVisibility(View.VISIBLE);
            btnApprove.setOnClickListener(v -> approveCompletion());
            btnReject.setOnClickListener(v -> rejectCompletion());
        } else {
            layoutCompletionRequest.setVisibility(View.GONE);
        }
    }

    private void approveCompletion() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("completionApproved", true);
        updates.put("status",             TaskModel.STATUS_APPROVED);
        updates.put("updatedAt",          System.currentTimeMillis());
        taskRef.updateChildren(updates);
        Toast.makeText(this, "Task approved ✓", Toast.LENGTH_SHORT).show();
        layoutCompletionRequest.setVisibility(View.GONE);
    }

    private void rejectCompletion() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("completionRequested", false);
        updates.put("status",              TaskModel.STATUS_IN_PROGRESS);
        updates.put("updatedAt",           System.currentTimeMillis());
        taskRef.updateChildren(updates);
        Toast.makeText(this, "Task rejected — sent back to employee", Toast.LENGTH_SHORT).show();
        layoutCompletionRequest.setVisibility(View.GONE);
    }

    // =========================================================================
    // EMPLOYEE
    // =========================================================================

    private void showEmployeeActions() {
        layoutEmployeeActions.setVisibility(View.VISIBLE);
        layoutAdminActions.setVisibility(View.GONE);

        // Mark Seen
        btnMarkSeen.setVisibility(currentTask.isRead ? View.GONE : View.VISIBLE);
        btnMarkSeen.setOnClickListener(v -> {
            taskRef.child("isRead").setValue(true);
            btnMarkSeen.setVisibility(View.GONE);
        });

        // Start Task
        boolean isPending = TaskModel.STATUS_PENDING.equals(currentTask.status);
        btnStartTask.setVisibility(isPending ? View.VISIBLE : View.GONE);
        btnStartTask.setOnClickListener(v -> {
            Map<String, Object> updates = new HashMap<>();
            updates.put("status",    TaskModel.STATUS_IN_PROGRESS);
            updates.put("updatedAt", System.currentTimeMillis());
            taskRef.updateChildren(updates);
            Toast.makeText(this, "Task started!", Toast.LENGTH_SHORT).show();
            btnStartTask.setVisibility(View.GONE);
        });

        // Progress seekbar — only enabled when in progress
        seekBarProgress.setProgress(currentTask.progress);
        boolean isInProgress = TaskModel.STATUS_IN_PROGRESS.equals(currentTask.status);
        seekBarProgress.setEnabled(isInProgress);
        seekBarProgress.setAlpha(isInProgress ? 1f : 0.4f);
        seekBarProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                txtProgress.setText("Progress: " + progress + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override
            public void onStopTrackingTouch(SeekBar s) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("progress",  s.getProgress());
                updates.put("status",    s.getProgress() > 0
                        ? TaskModel.STATUS_IN_PROGRESS
                        : TaskModel.STATUS_PENDING);
                updates.put("updatedAt", System.currentTimeMillis());
                taskRef.updateChildren(updates);
            }
        });

        // ── Save Notes button ─────────────────────────────────────────────────
        btnSaveNotes.setOnClickListener(v -> {
            String notes = etWorkNotes.getText().toString().trim();
            if (notes.isEmpty()) {
                Toast.makeText(this, "Please write your work notes first",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            taskRef.child("workNotes").setValue(notes)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(this, "Notes saved ✓", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to save notes",
                                    Toast.LENGTH_SHORT).show());
        });

        // Request Completion — only at 100% progress
        boolean canRequest = !currentTask.completionRequested
                && !currentTask.completionApproved
                && !TaskModel.STATUS_APPROVED.equals(currentTask.status)
                && currentTask.progress >= 100;
        btnRequestCompletion.setVisibility(canRequest ? View.VISIBLE : View.GONE);
        btnRequestCompletion.setOnClickListener(v -> {
            // Must have notes before requesting completion
            String notes = etWorkNotes.getText().toString().trim();
            if (notes.isEmpty()) {
                Toast.makeText(this,
                        "Please add work notes before requesting completion",
                        Toast.LENGTH_LONG).show();
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("completionRequested", true);
            updates.put("status",              TaskModel.STATUS_COMPLETED);
            updates.put("progress",            100);
            updates.put("workNotes",           notes); // save notes with completion request
            updates.put("updatedAt",           System.currentTimeMillis());
            taskRef.updateChildren(updates);
            Toast.makeText(this, "Completion requested!", Toast.LENGTH_SHORT).show();
            btnRequestCompletion.setVisibility(View.GONE);
        });
    }
}