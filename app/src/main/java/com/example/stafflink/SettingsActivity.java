package com.example.stafflink;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SettingsActivity extends AppCompatActivity {

    // Profile EditTexts
    private EditText etAdminName, etAdminEmail, etAdminPhone, etCompanyName,
             etIndustry, etLocation, etDepartments, etNewDepartment;

    // Feature Switches (Settings)
    private Switch switchAttendanceSetting, switchDepartmentsSetting,
            switchLeaveSetting, switchOvertimeSetting, switchPayrollSetting;

    // Feature Switches (App Features)
    private Switch switchAttendanceFeature, switchDeptTrackingFeature,
            leaveManagementFeature, multiCompanyFeature, overtimeFeature, payrollFeature;

    // Buttons
    private Button btnSubmit, btnAddDepartment;
    private ImageButton backButton;

    // Firebase
    private DatabaseReference adminRef;
    private String adminKey; // dynamically set

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Get adminKey from SharedPreferences
        SharedPreferences sp = getSharedPreferences("ADMIN_SESSION", MODE_PRIVATE);
        adminKey = sp.getString("adminNode", null);

        if (adminKey == null || adminKey.isEmpty()) {
            SharedPreferences sp2 = getSharedPreferences("ADMIN_SESSION", MODE_PRIVATE);
            adminKey = sp2.getString("adminNode", null);
        }

        if (adminKey == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, Admin_page.class));
            finish();
            return;
        }


        initViews();
        setupFirebase();
        loadProfile();
        loadSettings();
        setupButtonListeners();
    }

    private void initViews() {
        etAdminName = findViewById(R.id.etAdminName);
        etAdminEmail = findViewById(R.id.etAdminEmail);
        etAdminPhone = findViewById(R.id.etAdminPhone);
        etCompanyName = findViewById(R.id.etCompanyName);
//        etCompanyCode = findViewById(R.id.etCompanyCode);
        etIndustry = findViewById(R.id.etIndustry);
        etLocation = findViewById(R.id.etLocation);
        etDepartments = findViewById(R.id.etDepartments);
        etNewDepartment = findViewById(R.id.etNewDepartment);

        switchAttendanceSetting = findViewById(R.id.switchAttendanceSetting);
        switchDepartmentsSetting = findViewById(R.id.switchDepartmentsSetting);
        switchLeaveSetting = findViewById(R.id.switchLeaveSetting);
        switchOvertimeSetting = findViewById(R.id.switchOvertimeSetting);
        switchPayrollSetting = findViewById(R.id.switchPayrollSetting);

        switchAttendanceFeature = findViewById(R.id.switchAttendanceFeature);
        switchDeptTrackingFeature = findViewById(R.id.departmentTrackingFeature);
        leaveManagementFeature = findViewById(R.id.leaveManagementFeature);
        multiCompanyFeature = findViewById(R.id.multiCompanyFeature);
        overtimeFeature = findViewById(R.id.overtimeFeature);
        payrollFeature = findViewById(R.id.payrollFeature);

        btnSubmit = findViewById(R.id.btnSubmit);
        backButton = findViewById(R.id.backButton);
        btnAddDepartment = findViewById(R.id.btnAddDepartment);
    }

    private void setupFirebase() {
        adminRef = FirebaseDatabase.getInstance()
                .getReference("Stafflink")
                .child("admins")
                .child(adminKey);
    }

    // ================= PROFILE =================
    private void loadProfile() {
        adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                // Admin name from adminKey
                String raw = adminKey.replace("admin_", "");
                String adminName = raw.substring(0, 1).toUpperCase() + raw.substring(1);
                etAdminName.setText(adminName != null ? adminName : "");

                // Company info
                DataSnapshot companyInfo = snapshot.child("companyInfo");
                if (companyInfo.exists()) {
                    etAdminEmail.setText(companyInfo.child("companyEmail").getValue(String.class));
                    Object phoneNumber = companyInfo.child("CompanyNumber").getValue();
                    etAdminPhone.setText(phoneNumber != null ? phoneNumber.toString() : "");
                    etCompanyName.setText(companyInfo.child("companyName").getValue(String.class));
                    etIndustry.setText(companyInfo.child("industry").getValue(String.class));
                    etLocation.setText(companyInfo.child("location").getValue(String.class));
                }

                // Company code
//                etCompanyCode.setText(snapshot.child("companyCode").getValue(String.class));

                // Departments dynamically
                DataSnapshot deptSnap = snapshot.child("departments");
                StringBuilder depts = new StringBuilder();
                if (deptSnap.exists()) {
                    for (DataSnapshot d : deptSnap.getChildren()) {
                        depts.append(d.getKey()).append(", ");
                    }
                    if (depts.length() > 2) depts.setLength(depts.length() - 2);
                }
                etDepartments.setText(depts.toString());
                etDepartments.setEnabled(false);

                disableAllFields();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SettingsActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ================= SETTINGS =================
    private void loadSettings() {
        adminRef.child("settings").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                setSwitch(switchAttendanceSetting, snapshot, "attendance");
                setSwitch(switchDepartmentsSetting, snapshot, "departments");
                setSwitch(switchLeaveSetting, snapshot, "leave");
                setSwitch(switchOvertimeSetting, snapshot, "overtime");
                setSwitch(switchPayrollSetting, snapshot, "payroll");

                setSwitch(switchAttendanceFeature, snapshot, "attendanceFeature");
                setSwitch(switchDeptTrackingFeature, snapshot, "departmentFeature");
                setSwitch(leaveManagementFeature, snapshot, "leaveFeature");
                setSwitch(multiCompanyFeature, snapshot, "multiCompanyFeature");
                setSwitch(overtimeFeature, snapshot, "overtimeFeature");
                setSwitch(payrollFeature, snapshot, "payrollFeature");

                disableAllFields();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SettingsActivity.this, "Failed to load settings", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setSwitch(Switch s, DataSnapshot snapshot, String key) {
        Boolean val = snapshot.child(key).getValue(Boolean.class);
        s.setChecked(val != null && val);
    }

    // ================= ENABLE / DISABLE =================
    private void enableAllFields(boolean enable) {
        etAdminName.setEnabled(enable);
        etAdminEmail.setEnabled(enable);
        etAdminPhone.setEnabled(enable);
        etCompanyName.setEnabled(enable);
//        etCompanyCode.setEnabled(enable);
        etIndustry.setEnabled(enable);
        etLocation.setEnabled(enable);
        etNewDepartment.setEnabled(enable);

        switchAttendanceSetting.setEnabled(enable);
        switchDepartmentsSetting.setEnabled(enable);
        switchLeaveSetting.setEnabled(enable);
        switchOvertimeSetting.setEnabled(enable);
        switchPayrollSetting.setEnabled(enable);

        switchAttendanceFeature.setEnabled(enable);
        switchDeptTrackingFeature.setEnabled(enable);
        leaveManagementFeature.setEnabled(enable);
        multiCompanyFeature.setEnabled(enable);
        overtimeFeature.setEnabled(enable);
        payrollFeature.setEnabled(enable);

        btnSubmit.setText(enable ? "Save Settings" : "Edit Settings");
    }

    private void disableAllFields() {
        enableAllFields(false);
    }

    // ================= BUTTON LISTENERS =================
    private void setupButtonListeners() {
        backButton.setOnClickListener(v -> finish());

        btnSubmit.setOnClickListener(v -> {
            if (btnSubmit.getText().toString().equals("Edit Settings")) {
                enableAllFields(true);
            } else {
                saveSettings();
            }
        });

        btnAddDepartment.setOnClickListener(v -> {
            String newDept = etNewDepartment.getText().toString().trim();
            if (!newDept.isEmpty()) {
                // Add as a child node without deleting existing ones
                adminRef.child("departments").child(newDept).setValue("");

                // Update UI dynamically
                String current = etDepartments.getText().toString();
                if (current.isEmpty()) {
                    etDepartments.setText(newDept);
                } else {
                    etDepartments.setText(current + ", " + newDept);
                }

                etNewDepartment.setText("");
                Toast.makeText(this, "Department added ✅", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Enter department name", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ================= SAVE SETTINGS =================
    private void saveSettings() {
        adminRef.child("companyInfo").child("companyEmail").setValue(etAdminEmail.getText().toString().trim());
        adminRef.child("companyInfo").child("CompanyNumber").setValue(etAdminPhone.getText().toString().trim());
        adminRef.child("companyInfo").child("companyName").setValue(etCompanyName.getText().toString().trim());
        adminRef.child("companyInfo").child("industry").setValue(etIndustry.getText().toString().trim());
        adminRef.child("companyInfo").child("location").setValue(etLocation.getText().toString().trim());

//        adminRef.child("companyCode").setValue(etCompanyCode.getText().toString().trim());

        // Removed the line overwriting departments!
        // adminRef.child("departments").setValue(etDepartments.getText().toString().trim());

        adminRef.child("settings").child("attendance").setValue(switchAttendanceSetting.isChecked());
        adminRef.child("settings").child("departments").setValue(switchDepartmentsSetting.isChecked());
        adminRef.child("settings").child("leave").setValue(switchLeaveSetting.isChecked());
        adminRef.child("settings").child("overtime").setValue(switchOvertimeSetting.isChecked());
        adminRef.child("settings").child("payroll").setValue(switchPayrollSetting.isChecked());

        adminRef.child("settings").child("attendanceFeature").setValue(switchAttendanceFeature.isChecked());
        adminRef.child("settings").child("departmentFeature").setValue(switchDeptTrackingFeature.isChecked());
        adminRef.child("settings").child("leaveFeature").setValue(leaveManagementFeature.isChecked());
        adminRef.child("settings").child("multiCompanyFeature").setValue(multiCompanyFeature.isChecked());
        adminRef.child("settings").child("overtimeFeature").setValue(overtimeFeature.isChecked());
        adminRef.child("settings").child("payrollFeature").setValue(payrollFeature.isChecked());

        Toast.makeText(this, "Settings saved successfully ✅", Toast.LENGTH_SHORT).show();
        disableAllFields();
    }
}
