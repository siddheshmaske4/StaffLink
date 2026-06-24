package com.example.stafflink;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;
import android.view.View;

public class PayrollActivity extends AppCompatActivity {

    EditText currency, fullDeduct, halfDeduct, perDayDeduct;
    EditText fullHours, halfHours, paidLeaves, weeklyOff;
    EditText maxOT, otRate, esic, pf;
    Button submit, edit;
    ImageButton back;

    DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payroll);

        // Bind UI elements
        currency = findViewById(R.id.etCurrency);
        fullDeduct = findViewById(R.id.etFullLeaveDeduct);
        halfDeduct = findViewById(R.id.etHalfLeaveDeduct);
        perDayDeduct = findViewById(R.id.etPerDayDeduct);
        fullHours = findViewById(R.id.etFullDayHours);
        halfHours = findViewById(R.id.etHalfDayHours);
        paidLeaves = findViewById(R.id.etPaidLeaves);
        weeklyOff = findViewById(R.id.etWeeklyOff);
        maxOT = findViewById(R.id.etMaxOvertime);
        otRate = findViewById(R.id.etOTRate);
        esic = findViewById(R.id.etESIC);
        pf = findViewById(R.id.etPF);
        submit = findViewById(R.id.btnSubmit);
        edit = findViewById(R.id.btnEdit);
        back = findViewById(R.id.btnBack);

        // Load admin session
        SharedPreferences sp = getSharedPreferences("ADMIN_SESSION", Context.MODE_PRIVATE);
        String adminNode = sp.getString("adminNode", null);

        if (TextUtils.isEmpty(adminNode)) {
            Toast.makeText(this, "No admin session found! Please login again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Firebase reference for payroll rules
        dbRef = FirebaseDatabase.getInstance()
                .getReference("Stafflink")
                .child("admins")
                .child(adminNode)
                .child("payrollRules");

        disableFields();
        submit.setVisibility(View.GONE);
        loadSettings();

        edit.setOnClickListener(v -> {
            enableFields();
            submit.setVisibility(View.VISIBLE);
            edit.setVisibility(View.GONE);
        });

//        submit.setOnClickListener(v -> saveSettings());
        back.setOnClickListener(v -> finish());
        submit.setOnClickListener(v -> {
            if (validate()) saveSettings();
        });

    }

    private void loadSettings() {
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    enableFields();
                    submit.setVisibility(View.VISIBLE);
                    return;
                }

                PayrollData data = snapshot.getValue(PayrollData.class);
                if (data != null) {
                    currency.setText(data.currency != null ? data.currency : "");

                    if (data.deduction != null) {
                        fullDeduct.setText(String.valueOf(data.deduction.fullLeaveDay));
                        halfDeduct.setText(String.valueOf(data.deduction.halfDay));
                        perDayDeduct.setText(String.valueOf(data.deduction.perDay));
                    }

                    if (data.leavePolicy != null) {
                        fullHours.setText(String.valueOf(data.leavePolicy.fullDayHours));
                        halfHours.setText(String.valueOf(data.leavePolicy.halfDayHours));
                        paidLeaves.setText(String.valueOf(data.leavePolicy.paidLeavesPerMonth));
                        weeklyOff.setText(data.leavePolicy.weeklyOff != null ? data.leavePolicy.weeklyOff : "");
                    }

                    if (data.overtime != null) {
                        maxOT.setText(String.valueOf(data.overtime.maxHoursAllowed));
                        otRate.setText(String.valueOf(data.overtime.perHour));
                    }

                    if (data.taxAndCharges != null) {
                        esic.setText(String.valueOf(data.taxAndCharges.esiPercentage));
                        pf.setText(String.valueOf(data.taxAndCharges.pfPercentage));
                    }
                }

                disableFields();
                submit.setVisibility(View.GONE);
                edit.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PayrollActivity.this, "Failed to load payroll settings!", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void disableFields() { setAll(false); }
    private void enableFields() { setAll(true); }

    private void setAll(boolean state) {
        currency.setEnabled(state);
        fullDeduct.setEnabled(state);
        halfDeduct.setEnabled(state);
        perDayDeduct.setEnabled(state);
        fullHours.setEnabled(state);
        halfHours.setEnabled(state);
        paidLeaves.setEnabled(state);
        weeklyOff.setEnabled(state);
        maxOT.setEnabled(state);
        otRate.setEnabled(state);
        esic.setEnabled(state);
        pf.setEnabled(state);
    }

    private void saveSettings() {
        try {
            PayrollData model = new PayrollData();
            model.currency = currency.getText().toString().trim();

            PayrollData.Deduction ded = new PayrollData.Deduction();
            ded.fullLeaveDay = Integer.parseInt(fullDeduct.getText().toString().trim());
            ded.halfDay = Integer.parseInt(halfDeduct.getText().toString().trim());
            ded.perDay = Integer.parseInt(perDayDeduct.getText().toString().trim());
            model.deduction = ded;

            PayrollData.LeavePolicy leave = new PayrollData.LeavePolicy();
            leave.fullDayHours = Integer.parseInt(fullHours.getText().toString().trim());
            leave.halfDayHours = Integer.parseInt(halfHours.getText().toString().trim());
            leave.paidLeavesPerMonth = Integer.parseInt(paidLeaves.getText().toString().trim());
            leave.weeklyOff = weeklyOff.getText().toString().trim();
            model.leavePolicy = leave;

            PayrollData.Overtime ot = new PayrollData.Overtime();
            ot.maxHoursAllowed = Integer.parseInt(maxOT.getText().toString().trim());
            ot.perHour = Integer.parseInt(otRate.getText().toString().trim());
            model.overtime = ot;

            PayrollData.TaxAndCharges tax = new PayrollData.TaxAndCharges();
            tax.esiPercentage = Double.parseDouble(esic.getText().toString().trim());
            tax.pfPercentage = Double.parseDouble(pf.getText().toString().trim());
            model.taxAndCharges = tax;

            dbRef.setValue(model)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Payroll settings saved ✅", Toast.LENGTH_SHORT).show();
                            disableFields();
                            submit.setVisibility(View.GONE);
                            edit.setVisibility(View.VISIBLE);
                        } else {
                            Toast.makeText(this, "Failed to save settings ❌", Toast.LENGTH_LONG).show();
                        }
                    }).addOnFailureListener(e ->
                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers ⚠", Toast.LENGTH_LONG).show();
        }
    }

    // Nested Model Classes
    public static class PayrollData {
        public String currency;
        public Deduction deduction;
        public LeavePolicy leavePolicy;
        public Overtime overtime;
        public TaxAndCharges taxAndCharges;

        public PayrollData() {}

        public static class Deduction { public int fullLeaveDay, halfDay, perDay; public Deduction() {} }
        public static class LeavePolicy { public int fullDayHours, halfDayHours, paidLeavesPerMonth; public String weeklyOff; public LeavePolicy() {} }
        public static class Overtime { public int maxHoursAllowed, perHour; public Overtime() {} }
        public static class TaxAndCharges { public double esiPercentage, pfPercentage; public TaxAndCharges() {} }
    }

    private boolean validate() {

        if (TextUtils.isEmpty(currency.getText())) {
            currency.setError("Required");
            return false;
        }

        int perDay = Integer.parseInt(perDayDeduct.getText().toString());
        int full = Integer.parseInt(fullDeduct.getText().toString());

        if (full > perDay) {
            fullDeduct.setError("Cannot exceed per day salary");
            return false;
        }

        double esiVal = Double.parseDouble(esic.getText().toString());
        double pfVal = Double.parseDouble(pf.getText().toString());

        if (esiVal > 10 || pfVal > 10) {
            Toast.makeText(this, "PF / ESIC too high!", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

}
