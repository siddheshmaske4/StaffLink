package com.example.stafflink;

import androidx.annotation.NonNull;
import com.google.firebase.database.*;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class PayrollCalculator {

    private final Context context;
    private String companyCode, adminNodeKey;
    private DatabaseReference companyRef;

    public PayrollCalculator(Context context) {
        this.context = context;

        // Get dynamic session values
        SharedPreferences sp = context.getSharedPreferences("ADMIN_SESSION", Context.MODE_PRIVATE);
        adminNodeKey = sp.getString("adminNode", null);
        companyCode = sp.getString("COMPANY_CODE", null);

        if (adminNodeKey == null || companyCode == null) {
            Log.e("PayrollCalculator", "Admin session missing!");
            return;
        }

        companyRef = FirebaseDatabase.getInstance()
                .getReference("companies")
                .child(companyCode);
    }

    // Calculate payroll for all employees for a month
    public void calculatePayrollForMonth(String month) {
        if (companyRef == null) return;

        DatabaseReference rulesRef = FirebaseDatabase.getInstance()
                .getReference("Stafflink/admins")
                .child(adminNodeKey)
                .child("payrollRules");

        rulesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot rulesSnap) {
                companyRef.child("employees").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot empSnap) {
                        for (DataSnapshot emp : empSnap.getChildren()) {
                            calculatePayrollForEmployee(emp, month, rulesSnap);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void calculatePayrollForEmployee(DataSnapshot empSnap, String month,  DataSnapshot rulesSnap) {
        String empId = empSnap.getKey();
        final int baseSalary = empSnap.child("profile/baseSalary").getValue(Integer.class) != null ?
                empSnap.child("profile/baseSalary").getValue(Integer.class) : 0;


        // Fetch admin payroll rules
        DatabaseReference rulesRef = FirebaseDatabase.getInstance()
                .getReference("Stafflink/admins")
                .child(adminNodeKey)
                .child("payrollRules");

        rulesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot rulesSnap) {

                // --- Read rules ---
                int fullLeaveDeduct = rulesSnap.child("deduction/fullLeaveDay").getValue(Integer.class) != null ?
                        rulesSnap.child("deduction/fullLeaveDay").getValue(Integer.class) : 0;
                int halfLeaveDeduct = rulesSnap.child("deduction/halfDay").getValue(Integer.class) != null ?
                        rulesSnap.child("deduction/halfDay").getValue(Integer.class) : 0;

                int paidLeaves = rulesSnap.child("leavePolicy/paidLeavesPerMonth").getValue(Integer.class) != null ?
                        rulesSnap.child("leavePolicy/paidLeavesPerMonth").getValue(Integer.class) : 0;

                int maxOT = rulesSnap.child("overtime/maxHoursAllowed").getValue(Integer.class) != null ?
                        rulesSnap.child("overtime/maxHoursAllowed").getValue(Integer.class) : 0;
                int otRate = rulesSnap.child("overtime/perHour").getValue(Integer.class) != null ?
                        rulesSnap.child("overtime/perHour").getValue(Integer.class) : 0;

                double pfPercent = rulesSnap.child("taxAndCharges/pfPercentage").getValue(Double.class) != null ?
                        rulesSnap.child("taxAndCharges/pfPercentage").getValue(Double.class) : 0;
                double esiPercent = rulesSnap.child("taxAndCharges/esiPercentage").getValue(Double.class) != null ?
                        rulesSnap.child("taxAndCharges/esiPercentage").getValue(Double.class) : 0;

                // --- Attendance & leaves ---
                int presentDays = 0;
                int fullLeavesUsed = 0;
                int halfLeavesUsed = 0;
                int overtimeHours = 0;

                DataSnapshot attendanceSnap = empSnap.child("attendance");
                for (DataSnapshot day : attendanceSnap.getChildren()) {
                    if (day.getKey().startsWith(month)) { // only current month
                        String status = day.child("status").getValue(String.class);
                        int ot = day.child("overtimeHours").getValue(Integer.class) != null ?
                                day.child("overtimeHours").getValue(Integer.class) : 0;
                        overtimeHours += ot;

                        if ("Present".equals(status)) presentDays++;
                        else if ("HalfDay".equals(status)) halfLeavesUsed++;
                        else if ("Absent".equals(status)) fullLeavesUsed++;
                    }
                }

                DataSnapshot leavesSnap = empSnap.child("leaves").child(month);
                if (leavesSnap.exists()) {
                    fullLeavesUsed += leavesSnap.child("fullDaysUsed").getValue(Integer.class) != null ?
                            leavesSnap.child("fullDaysUsed").getValue(Integer.class) : 0;
                    halfLeavesUsed += leavesSnap.child("halfDaysUsed").getValue(Integer.class) != null ?
                            leavesSnap.child("halfDaysUsed").getValue(Integer.class) : 0;
                }

                // --- Calculate payroll ---
                int deduction = fullLeavesUsed * fullLeaveDeduct + halfLeavesUsed * halfLeaveDeduct;
                int allowedOT = Math.min(overtimeHours, maxOT);
                int otMoney = allowedOT * otRate;
                int pfDeducted = (int) Math.round(baseSalary * (pfPercent / 100));
                int esiDeducted = (int) Math.round(baseSalary * (esiPercent / 100));
                int finalSalary = baseSalary - deduction - pfDeducted - esiDeducted + otMoney;

                // --- Save payroll ---
                DatabaseReference payrollRef = companyRef.child("employees")
                        .child(empId)
                        .child("payroll")
                        .child("calculated")
                        .child(month);

                Map<String, Object> payrollData = new HashMap<>();
                payrollData.put("baseSalary", baseSalary);
                payrollData.put("deductions", deduction);
                payrollData.put("pfDeducted", pfDeducted);
                payrollData.put("esiDeducted", esiDeducted);
                payrollData.put("overtimeHours", overtimeHours);
                payrollData.put("overtimeMoney", otMoney);
                payrollData.put("presentDays", presentDays);
                payrollData.put("fullLeaves", fullLeavesUsed);
                payrollData.put("halfDays", halfLeavesUsed);
                payrollData.put("finalSalary", finalSalary);
                payrollData.put("calculatedAt", System.currentTimeMillis());

                payrollRef.setValue(payrollData);
                payrollRef.child("rulesSnapshot").setValue(rulesSnap.getValue());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
