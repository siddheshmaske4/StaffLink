package com.example.stafflink;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView welcomeText, companyNameText, companyCodeText;
    private CardView employeesCard, payrollCard, attendanceCard, leavesCard,
            settingsCard, profileCard, taskMonitorCard, assignTaskCard,
            leaveRequestsCard; // ← new
    private ImageView logout, inboxIcon;

    private String adminNodeKey, companyCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Bind UI
        welcomeText       = findViewById(R.id.adminWelcomeText);
        companyNameText   = findViewById(R.id.companyNameText);
        companyCodeText   = findViewById(R.id.companyCodeText);
        employeesCard     = findViewById(R.id.employeesCard);
        payrollCard       = findViewById(R.id.payrollCard);
        attendanceCard    = findViewById(R.id.attendanceCard);
        leavesCard        = findViewById(R.id.leavesCard);
        settingsCard      = findViewById(R.id.settingsCard);
        profileCard       = findViewById(R.id.profileCard);
        taskMonitorCard   = findViewById(R.id.taskMonitorCard);
        assignTaskCard    = findViewById(R.id.assignTaskCard);
        leaveRequestsCard = findViewById(R.id.leaveRequestsCard); // ← new
        logout            = findViewById(R.id.logoutIcon);
        inboxIcon         = findViewById(R.id.inboxIcon);
        CardView calcPayrollCard = findViewById(R.id.calcPayrollCard);

        // Load session
        SharedPreferences sp = getSharedPreferences("ADMIN_SESSION", MODE_PRIVATE);
        adminNodeKey = sp.getString("adminNode", null);
        companyCode  = sp.getString("COMPANY_CODE", null);

        if (adminNodeKey == null || companyCode == null) {
            Toast.makeText(this, "No admin session found!", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, Admin_page.class));
            finish();
            return;
        }

        if (companyCode != null) {
            companyCodeText.setText("Code: " + companyCode);
        }

        // Fetch company name
        DatabaseReference adminRef = FirebaseDatabase.getInstance()
                .getReference("Stafflink/admins")
                .child(adminNodeKey)
                .child("companyInfo")
                .child("companyName");

        adminRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                companyNameText.setText("Company: " + snapshot.getValue(String.class));
            } else {
                companyNameText.setText("Company: Not Found");
            }
        }).addOnFailureListener(e -> {
            companyNameText.setText("Company: Error");
            Toast.makeText(this, "Failed to load company name", Toast.LENGTH_SHORT).show();
        });

        // Logout
        logout.setOnClickListener(v -> {
            sp.edit().clear().apply();
            Toast.makeText(this, "Logged Out!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, Admin_page.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Card clicks
        employeesCard.setOnClickListener(v -> openEmployeeManagement());
        payrollCard.setOnClickListener(v -> openActivity(PayrollActivity.class));
        attendanceCard.setOnClickListener(v -> openAttendance());
        leavesCard.setOnClickListener(v -> openLeaveActivity());
        settingsCard.setOnClickListener(v -> openActivity(SettingsActivity.class));
        profileCard.setOnClickListener(v -> openActivity(ProfileActivity.class));
        calcPayrollCard.setOnClickListener(v -> triggerPayrollAndOpen());
        inboxIcon.setOnClickListener(v -> openInbox());
        assignTaskCard.setOnClickListener(v -> openAssignTask());
        taskMonitorCard.setOnClickListener(v -> openTaskMonitor());

        // ── NEW: Leave Requests card ───────────────────────────────────────────
        leaveRequestsCard.setOnClickListener(v -> openLeaveRequests());
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void openEmployeeManagement() {
        Intent i = new Intent(this, EmployeeManagementActivity.class);
        i.putExtra("COMPANY_CODE", companyCode);
        i.putExtra("ADMIN_NODE", adminNodeKey);
        startActivity(i);
    }

    private void openAttendance() {
        Intent i = new Intent(this, AttendanceActivity.class);
        i.putExtra("COMPANY_CODE", companyCode);
        i.putExtra("ADMIN_NODE", adminNodeKey);
        startActivity(i);
    }

    private void openLeaveActivity() {
        Intent i = new Intent(this, LeavesActivity.class);
        i.putExtra("COMPANY_CODE", companyCode);
        i.putExtra("ADMIN_NODE", adminNodeKey);
        startActivity(i);
    }

    private void openInbox() {
        Intent i = new Intent(this, InboxActivity.class);
        i.putExtra("COMPANY_CODE", companyCode);
        i.putExtra("ADMIN_NODE", adminNodeKey);
        startActivity(i);
    }

    private void openAssignTask() {
        Intent i = new Intent(this, AdminAssignTaskActivity.class);
        i.putExtra("COMPANY_CODE", companyCode);
        i.putExtra("ADMIN_NODE",   adminNodeKey);
        startActivity(i);
    }

    private void openTaskMonitor() {
        Intent i = new Intent(this, AdminTaskMonitorActivity.class);
        i.putExtra("COMPANY_CODE", companyCode);
        i.putExtra("ADMIN_NODE",   adminNodeKey);
        startActivity(i);
    }

    // ── NEW ──────────────────────────────────────────────────────────────────
    private void openLeaveRequests() {
        Intent i = new Intent(this, AdminLeaveRequestsActivity.class);
        i.putExtra("COMPANY_CODE", companyCode);
        i.putExtra("ADMIN_NODE",   adminNodeKey);
        startActivity(i);
    }

    private void openActivity(Class<?> cls) {
        startActivity(new Intent(this, cls));
    }

    private void triggerPayrollAndOpen() {
        Toast.makeText(this, "Calculating payroll for all employees...", Toast.LENGTH_SHORT).show();
        Calendar cal = Calendar.getInstance();
        String month = String.format("%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);
        PayrollCalculator calculator = new PayrollCalculator(AdminDashboardActivity.this);
        calculator.calculatePayrollForMonth(month);
    }


}