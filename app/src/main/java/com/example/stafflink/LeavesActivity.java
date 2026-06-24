package com.example.stafflink;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LeavesActivity — simple mail compose screen (task functionality removed,
 * now handled by the separate Task module).
 *
 * Flow: admin writes subject + message → selects employees → sends.
 * Writes to:
 *   companies/{companyCode}/employees/{empId}/messages/{msgId}
 *   Stafflink/admins/{adminId}/messages/{msgId}  (admin's sent copy)
 */
public class LeavesActivity extends AppCompatActivity {

    private EditText    edtTitle, edtBody;
    private ImageButton btnBack;
    private Button      btnSend;

    private EmployeeSelectAdapter adapter;
    private List<EmployeeModel>   employeeList;

    private String companyCode, adminId, adminName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaves);

        readSessionData();
        bindViews();
        setupRecyclerView();

        if (companyCode != null && adminId != null) {
            loadEmployees();
        }

        btnSend.setOnClickListener(v -> sendMessage());
        btnBack.setOnClickListener(v -> finish());
    }

    // -------------------- SESSION --------------------

    private void readSessionData() {
        Intent i = getIntent();
        companyCode = i.getStringExtra("COMPANY_CODE");
        adminId     = i.getStringExtra("ADMIN_NODE");
        adminName   = i.getStringExtra("ADMIN_NAME"); // optional

        if (companyCode == null || adminId == null) {
            Toast.makeText(this, "Invalid admin session", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    // -------------------- UI --------------------

    private void bindViews() {
        edtTitle = findViewById(R.id.edtTitle);
        edtBody  = findViewById(R.id.edtBody);
        btnBack  = findViewById(R.id.btnBack);
        btnSend  = findViewById(R.id.btnSend);
    }

    private void setupRecyclerView() {
        RecyclerView rv = findViewById(R.id.rvEmployees);
        rv.setLayoutManager(new LinearLayoutManager(this));

        employeeList = new ArrayList<>();
        adapter      = new EmployeeSelectAdapter(employeeList);
        rv.setAdapter(adapter);
    }

    // -------------------- FIREBASE --------------------

    private DatabaseReference getEmployeesRef() {
        return FirebaseDatabase.getInstance()
                .getReference("companies")
                .child(companyCode)
                .child("employees");
    }

    private void loadEmployees() {
        getEmployeesRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
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

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LeavesActivity.this,
                        "Failed to load employees", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // -------------------- SEND MESSAGE --------------------

    private void sendMessage() {
        String title = edtTitle.getText().toString().trim();
        String body  = edtBody.getText().toString().trim();

        if (title.isEmpty() || body.isEmpty()) {
            Toast.makeText(this, "Title and message required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (adapter.getSelectedIds().isEmpty()) {
            Toast.makeText(this, "Select at least one employee", Toast.LENGTH_SHORT).show();
            return;
        }

        String msgId = "msg_" + System.currentTimeMillis();

        Map<String, Object> sender = new HashMap<>();
        sender.put("id",   adminId);
        sender.put("role", "admin");
        if (adminName != null && !adminName.isEmpty()) {
            sender.put("name", adminName);
        }

        Map<String, Object> message = new HashMap<>();
        message.put("type",      "announcement");
        message.put("title",     title);
        message.put("body",      body);
        message.put("isRead",    false);
        message.put("createdAt", ServerValue.TIMESTAMP);
        message.put("sender",    sender);

        // ── Build recipient names ─────────────────────────────────────────
        StringBuilder names = new StringBuilder();
        for (String empId : adapter.getSelectedIds()) {
            EmployeeModel emp = getEmployeeById(empId);
            if (emp != null) {
                if (names.length() > 0) names.append(", ");
                names.append(emp.getName());
            }
        }
        message.put("recipients", names.toString()); // ← add this

        // Write to each selected employee's inbox
        for (String empId : adapter.getSelectedIds()) {
            getEmployeesRef()
                    .child(empId)
                    .child("messages")
                    .child(msgId)
                    .setValue(message);
        }

        // Write a copy to admin's own inbox (sent mails)
        FirebaseDatabase.getInstance()
                .getReference("Stafflink")
                .child("admins")
                .child(adminId)
                .child("messages")
                .child(msgId)
                .setValue(message);

        Toast.makeText(this, "Message sent successfully", Toast.LENGTH_SHORT).show();
        finish();
    }
    /** Finds an EmployeeModel by id from the loaded list */
    private EmployeeModel getEmployeeById(String empId) {
        for (EmployeeModel emp : employeeList) {
            if (empId.equals(emp.getId())) return emp;
        }
        return null;
    }
}