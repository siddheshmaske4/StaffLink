package com.example.stafflink;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AdminTaskMonitorActivity — monitors ALL tasks across ALL employees.
 *
 * Firebase read path:
 *   companies/{companyCode}/employees/{empId}/tasks/{taskId}
 */
public class AdminTaskMonitorActivity extends AppCompatActivity {

    private ImageButton  btnBack;
    private Spinner      spinnerFilter;
    private RecyclerView rvTasks;
    private TextView     txtEmpty;

    private TaskAdapter        adapter;
    private List<TaskModel>    allTasks = new ArrayList<>();
    private List<TaskModel>    filtered = new ArrayList<>();
    private String companyCode, adminNodeKey;

    private static final String[] FILTER_OPTIONS = {
            "All", "Pending", "In Progress", "Completed", "Approved", "Rejected"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_task_monitor);

        SharedPreferences sp = getSharedPreferences("ADMIN_SESSION", MODE_PRIVATE);
        adminNodeKey = getIntent().getStringExtra("ADMIN_NODE");
        companyCode  = getIntent().getStringExtra("COMPANY_CODE");
        if (adminNodeKey == null) adminNodeKey = sp.getString("adminNode",    null);
        if (companyCode  == null) companyCode  = sp.getString("COMPANY_CODE", null);

        if (companyCode == null) {
            Toast.makeText(this, "Session error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        setupFilter();
        setupRecyclerView();
        loadAllTasks();

        btnBack.setOnClickListener(v -> finish());
    }

    private void bindViews() {
        btnBack       = findViewById(R.id.btnBack);
        spinnerFilter = findViewById(R.id.spinnerFilter);
        rvTasks       = findViewById(R.id.rvTasks);
        txtEmpty      = findViewById(R.id.txtEmpty);
    }

    private void setupFilter() {
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, FILTER_OPTIONS);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(filterAdapter);
        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                applyFilter(FILTER_OPTIONS[pos]);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupRecyclerView() {
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter(this, filtered);
        rvTasks.setAdapter(adapter);

        adapter.setOnTaskClickListener(task -> {
            Intent intent = new Intent(this, TaskDetailActivity.class);
            intent.putExtra("ROLE",         "admin");
            intent.putExtra("COMPANY_CODE", companyCode);
            intent.putExtra("ADMIN_NODE",   adminNodeKey);
            intent.putExtra("EMP_ID",       task.assignedTo);
            intent.putExtra("TASK_ID",      task.taskId);
            startActivity(intent);
        });
    }

    private void loadAllTasks() {
        FirebaseDatabase.getInstance()
                .getReference("companies")
                .child(companyCode)
                .child("employees")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        android.util.Log.d("TASKMONITOR", "employees: " + snapshot.getChildrenCount());
                        allTasks.clear();
                        for (DataSnapshot empSnap : snapshot.getChildren()) {
                            String empId = empSnap.getKey();
                            for (DataSnapshot taskSnap : empSnap.child("tasks").getChildren()) {
                                try {
                                    TaskModel task = taskSnap.getValue(TaskModel.class);
                                    if (task != null) {
                                        task.taskId = taskSnap.getKey();
                                        if (task.assignedTo == null) task.assignedTo = empId;
                                        allTasks.add(task);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        Collections.sort(allTasks,
                                (a, b) -> Long.compare(b.createdAt, a.createdAt));
                        String selected = (String) spinnerFilter.getSelectedItem();
                        applyFilter(selected != null ? selected : "All");
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AdminTaskMonitorActivity.this,
                                "Failed to load tasks", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void applyFilter(String filter) {
        filtered.clear();
        for (TaskModel task : allTasks) {
            if ("All".equals(filter)) {
                filtered.add(task);
            } else {
                String statusFilter = filterToStatus(filter);
                if (statusFilter != null && statusFilter.equals(task.status)) {
                    filtered.add(task);
                }
            }
        }
        txtEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        rvTasks.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
        adapter.notifyDataSetChanged();
    }

    private String filterToStatus(String label) {
        switch (label) {
            case "Pending":     return TaskModel.STATUS_PENDING;
            case "In Progress": return TaskModel.STATUS_IN_PROGRESS;
            case "Completed":   return TaskModel.STATUS_COMPLETED;
            case "Approved":    return TaskModel.STATUS_APPROVED;
            case "Rejected":    return TaskModel.STATUS_REJECTED;
            default:            return null;
        }
    }
}