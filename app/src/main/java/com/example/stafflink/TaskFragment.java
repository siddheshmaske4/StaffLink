package com.example.stafflink;

import android.content.Intent;
import android.content.SharedPreferences;
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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TaskFragment — Fragment version of EmployeeTaskActivity.
 * Used in bottom_nav so Tasks tab stays inside the nav bar.
 */
public class TaskFragment extends Fragment {

    private RecyclerView    rvTasks;
    private TextView        txtEmpty;

    private TaskAdapter     adapter;
    private List<TaskModel> taskList = new ArrayList<>();
    private String companyCode, empId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_task, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Session
        SharedPreferences sp = requireActivity()
                .getSharedPreferences("StafflinkPrefs", requireContext().MODE_PRIVATE);
        companyCode = sp.getString("company_code", null);
        empId       = sp.getString("emp_id",       null);

        if (companyCode == null || empId == null) {
            Toast.makeText(getContext(), "Session error", Toast.LENGTH_SHORT).show();
            return;
        }

        rvTasks  = view.findViewById(R.id.rvTasks);
        txtEmpty = view.findViewById(R.id.txtEmpty);

        // Setup RecyclerView
        rvTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TaskAdapter(getContext(), taskList);
        rvTasks.setAdapter(adapter);

        // Click → open TaskDetailActivity
        adapter.setOnTaskClickListener(task -> {
            Intent intent = new Intent(getContext(), TaskDetailActivity.class);
            intent.putExtra("ROLE",         "employee");
            intent.putExtra("COMPANY_CODE", companyCode);
            intent.putExtra("EMP_ID",       empId);
            intent.putExtra("TASK_ID",      task.taskId);
            startActivity(intent);
        });

        loadTasks();
    }

    private void loadTasks() {
        FirebaseDatabase.getInstance()
                .getReference("companies")
                .child(companyCode)
                .child("employees")
                .child(empId)
                .child("tasks")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        taskList.clear();
                        for (DataSnapshot d : snapshot.getChildren()) {
                            try {
                                TaskModel task = d.getValue(TaskModel.class);
                                if (task != null) {
                                    task.taskId = d.getKey();
                                    taskList.add(task);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        Collections.sort(taskList,
                                (a, b) -> Long.compare(b.createdAt, a.createdAt));

                        if (txtEmpty != null)
                            txtEmpty.setVisibility(taskList.isEmpty() ? View.VISIBLE : View.GONE);
                        if (rvTasks != null)
                            rvTasks.setVisibility(taskList.isEmpty() ? View.GONE : View.VISIBLE);

                        if (adapter != null) adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (getContext() != null)
                            Toast.makeText(getContext(),
                                    "Failed to load tasks", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}