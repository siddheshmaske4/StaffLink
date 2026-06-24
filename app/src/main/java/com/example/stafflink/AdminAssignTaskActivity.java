package com.example.stafflink;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * AdminAssignTaskActivity
 * Admin fills task details, selects employees, assigns task.
 *
 * Firebase write path:
 *   companies/{companyCode}/employees/{empId}/tasks/{taskId}
 */
public class AdminAssignTaskActivity extends AppCompatActivity {

    private EditText    edtTitle, edtDescription;
    private TextView    txtDeadlineLabel;
    private Button      btnDeadline, btnAssign;
    private Spinner     spinnerPriority;
    private ImageButton btnBack;
    private RecyclerView rvEmployees;

    private EmployeeSelectAdapter empAdapter;
    private List<EmployeeModel>   employeeList = new ArrayList<>();

    private String companyCode, adminId, adminName;
    private String selectedDeadline = "";

    private static final String[] PRIORITIES = {"High", "Medium", "Low"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assign_task);

        // Session
        companyCode = getIntent().getStringExtra("COMPANY_CODE");
        adminId     = getIntent().getStringExtra("ADMIN_NODE");

        SharedPreferences sp = getSharedPreferences("ADMIN_SESSION", MODE_PRIVATE);
        if (companyCode == null) companyCode = sp.getString("COMPANY_CODE", null);
        if (adminId     == null) adminId     = sp.getString("adminNode",    null);
        adminName = sp.getString("adminName", "Admin");

        if (companyCode == null || adminId == null) {
            Toast.makeText(this, "Session error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        setupPrioritySpinner();
        setupDeadlinePicker();
        setupEmployeeList();
        loadEmployees();

        btnAssign.setOnClickListener(v -> assignTask());
        btnBack.setOnClickListener(v -> finish());
    }

    private void bindViews() {
        edtTitle        = findViewById(R.id.edtTaskTitle);
        edtDescription  = findViewById(R.id.edtTaskDescription);
        txtDeadlineLabel = findViewById(R.id.txtDeadlineLabel);
        btnDeadline     = findViewById(R.id.btnDeadline);
        btnAssign       = findViewById(R.id.btnAssignTask);
        spinnerPriority = findViewById(R.id.spinnerPriority);
        btnBack         = findViewById(R.id.btnBack);
        rvEmployees     = findViewById(R.id.rvEmployees);

        if (btnAssign == null) {
            android.util.Log.d("TASKASSIGN", "btnAssign is NULL");
        } else {
            android.util.Log.d("TASKASSIGN", "btnAssign is OK");
        }
    }




    private void setupPrioritySpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, PRIORITIES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(adapter);
        spinnerPriority.setSelection(1); // default Medium
    }

    private void setupDeadlinePicker() {
        txtDeadlineLabel.setText("No deadline set");
        btnDeadline.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this,
                    (view, year, month, day) -> {
                        selectedDeadline = String.format(Locale.getDefault(),
                                "%04d-%02d-%02d", year, month + 1, day);
                        txtDeadlineLabel.setText("📅 " + selectedDeadline);
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
    }

    private void setupEmployeeList() {
        rvEmployees.setLayoutManager(new LinearLayoutManager(this));
        empAdapter = new EmployeeSelectAdapter(employeeList);
        rvEmployees.setAdapter(empAdapter);
    }

    private void loadEmployees() {


        FirebaseDatabase.getInstance()
                .getReference("companies")
                .child(companyCode)
                .child("employees")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        android.util.Log.d("TASKASSIGN", "employees loaded: " + snapshot.getChildrenCount());


                        employeeList.clear();
                        for (DataSnapshot e : snapshot.getChildren()) {
                            EmployeeModel model = new EmployeeModel();
                            model.setId(e.getKey());
                            model.setExpanded(false);
                            String name  = e.child("profile").child("name").getValue(String.class);
                            String email = e.child("profile").child("email").getValue(String.class);
                            model.setName(name   != null ? name  : "No Name");
                            model.setEmail(email != null ? email : "");
                            employeeList.add(model);
                        }
                        empAdapter.notifyDataSetChanged();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AdminAssignTaskActivity.this,
                                "Failed to load employees", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void assignTask() {

        android.util.Log.d("TASKASSIGN", "assignTask called");
        android.util.Log.d("TASKASSIGN", "selected ids: " + empAdapter.getSelectedIds().toString());
        android.util.Log.d("TASKASSIGN", "companyCode: " + companyCode);
        android.util.Log.d("TASKASSIGN", "adminId: " + adminId);

        String title = edtTitle.getText().toString().trim();
        String desc  = edtDescription.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Task title required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (desc.isEmpty()) {
            Toast.makeText(this, "Task description required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (empAdapter.getSelectedIds().isEmpty()) {
            Toast.makeText(this, "Select at least one employee", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedDeadline.isEmpty()) {
            Toast.makeText(this, "Please set a deadline", Toast.LENGTH_SHORT).show();
            return;
        }

        String priority = getPriorityValue(spinnerPriority.getSelectedItemPosition());
        String taskId   = "task_" + System.currentTimeMillis();

        DatabaseReference empRef = FirebaseDatabase.getInstance()
                .getReference("companies")
                .child(companyCode)
                .child("employees");

        for (String empId : empAdapter.getSelectedIds()) {
            EmployeeModel emp = getEmployeeById(empId);
            String empName = emp != null ? emp.getName() : empId;

            TaskModel task = new TaskModel(
                    title, desc,
                    empId, empName,
                    adminId, adminName,
                    selectedDeadline, priority
            );

            empRef.child(empId)
                    .child("tasks")
                    .child(taskId)
                    .setValue(task);
        }

        Toast.makeText(this, "Task assigned successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private String getPriorityValue(int position) {
        switch (position) {
            case 0:  return TaskModel.PRIORITY_HIGH;
            case 2:  return TaskModel.PRIORITY_LOW;
            default: return TaskModel.PRIORITY_MEDIUM;
        }
    }

    private EmployeeModel getEmployeeById(String empId) {
        for (EmployeeModel emp : employeeList) {
            if (empId.equals(emp.getId())) return emp;
        }
        return null;
    }
}