package com.example.stafflink;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EmployeeManagementActivity extends AppCompatActivity {

    private RecyclerView rvEmployees;
    private EmployeeAdapter adapter;
    private EditText etSearchEmployee;
    private TextView tvCount;

    private final List<EmployeeModel> employeeList = new ArrayList<>();
    private final List<EmployeeModel> displayList = new ArrayList<>();

    private String companyCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_management);

        rvEmployees = findViewById(R.id.recyclerEmployees);
        etSearchEmployee = findViewById(R.id.etSearch);
        tvCount = findViewById(R.id.tvCount);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        companyCode = getIntent().getStringExtra("COMPANY_CODE");
        Log.d("EMP_MGMT", "Company Code: " + companyCode);

        if (companyCode == null || companyCode.trim().isEmpty()) {
            Toast.makeText(this, "Invalid company code", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        rvEmployees.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EmployeeAdapter(displayList);
        adapter.setCompanyCode(companyCode);
        rvEmployees.setAdapter(adapter);

        fetchEmployees();

        etSearchEmployee.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                filterEmployees(s.toString());
            }
        });
    }

    private void fetchEmployees() {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("companies")
                .child(companyCode)
                .child("employees");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                employeeList.clear();
                displayList.clear();

                Log.d("EMP_MGMT", "Employees count: " + snapshot.getChildrenCount());

                for (DataSnapshot empSnap : snapshot.getChildren()) {

                    DataSnapshot profile = empSnap.child("profile");
                    DataSnapshot payroll = empSnap.child("payroll").child("calculated");

                    if (!profile.exists()) continue;

                    String id = empSnap.getKey();
                    String name = getString(profile, "name");
                    String position = getString(profile, "position");
                    String department = getString(profile, "department");
                    String email = getString(profile, "email");
                    String phone = getString(profile, "phone");

                    long baseSalary = getLong(profile, "baseSalary");
                    long finalSalary = getLong(payroll, "finalSalary");
                    long deductions = getLong(payroll, "deductions");
                    long overtime = getLong(payroll, "overtimeMoney");

                    int halfDays = (int) getLong(payroll, "halfDays");
                    int fullDays = (int) getLong(payroll, "fullLeaves");
                    int paidLeaves = (int) getLong(payroll, "paidLeavesUsed");

                    EmployeeModel employee = new EmployeeModel(
                            id,
                            name,
                            position,
                            department,
                            baseSalary,
                            email,
                            phone,
                            finalSalary,
                            deductions,
                            overtime,
                            halfDays,
                            fullDays,
                            paidLeaves
                    );

                    employeeList.add(employee);
                    displayList.add(employee);
                }

                adapter.updateList(displayList);
                tvCount.setText("Total Employees: " + displayList.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EmployeeManagementActivity.this,
                        "DB Error: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void filterEmployees(String query) {
        displayList.clear();

        if (query == null || query.trim().isEmpty()) {
            displayList.addAll(employeeList);
        } else {
            String q = query.toLowerCase(Locale.ROOT);

            for (EmployeeModel e : employeeList) {
                if (e.getName().toLowerCase().contains(q) ||
                        e.getPosition().toLowerCase().contains(q) ||
                        e.getDepartment().toLowerCase().contains(q)) {
                    displayList.add(e);
                }
            }
        }

        adapter.updateList(displayList);
        tvCount.setText(displayList.isEmpty()
                ? "No employees found"
                : "Total Employees: " + displayList.size());
    }

    private String getString(DataSnapshot snap, String key) {
        String v = snap.child(key).getValue(String.class);
        return v == null ? "" : v;
    }

    private long getLong(DataSnapshot snap, String key) {
        Long v = snap.child(key).getValue(Long.class);
        return v == null ? 0 : v;
    }
}
