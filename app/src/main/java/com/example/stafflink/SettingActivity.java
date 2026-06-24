package com.example.stafflink;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.Map;

/**
 * SettingActivity — Employee profile settings.
 * Inline edit/save pattern matching AdminSettingsActivity.
 *
 * Fields: name, email, phone, department, position, password
 * Loaded from:  companies/{companyCode}/employees/{empId}/profile
 * Saved to:     companies/{companyCode}/employees/{empId}/profile
 */
public class SettingActivity extends AppCompatActivity {

    // ─── Profile fields ───────────────────────────────────────────────────────
    private EditText etName, etEmail, etPhone, etDepartment, etPosition, etPassword;

    // ─── Preferences ──────────────────────────────────────────────────────────
    private Switch switchDarkMode, switchNotifications;

    // ─── Buttons ──────────────────────────────────────────────────────────────
    private Button      btnSubmit;
    private ImageButton btnBack;
    private TextView    btnLogout;

    // ─── State ────────────────────────────────────────────────────────────────
    private boolean isEditMode = false;

    // ─── Firebase ─────────────────────────────────────────────────────────────
    private DatabaseReference profileRef;
    private String companyCode, empId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        bindViews();
        setupSession();
        loadProfile();
        setupListeners();
        disableAllFields(); // start in view-only mode
    }


    // =========================================================================
    // SETUP
    // =========================================================================

    private void bindViews() {
        etName              = findViewById(R.id.etName);
        etEmail             = findViewById(R.id.etEmail);
        etPhone             = findViewById(R.id.etPhone);
        etDepartment        = findViewById(R.id.etDepartment);
        etPosition          = findViewById(R.id.etPosition);
        etPassword          = findViewById(R.id.etPassword);
        switchDarkMode      = findViewById(R.id.switchDarkMode);
        switchNotifications = findViewById(R.id.switchNotifications);
        btnSubmit           = findViewById(R.id.btnSubmit);
        btnBack             = findViewById(R.id.btnBack);
        btnLogout           = findViewById(R.id.btnLogout);
    }

    private void setupSession() {
        SharedPreferences sp = getSharedPreferences("StafflinkPrefs", MODE_PRIVATE);
        companyCode = sp.getString("company_code", "");
        empId       = sp.getString("emp_id", "");

        profileRef = FirebaseDatabase.getInstance()
                .getReference("companies")
                .child(companyCode)
                .child("employees")
                .child(empId)
                .child("profile");
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSubmit.setOnClickListener(v -> {
            if (isEditMode) {
                saveSettings();
            } else {
                enableAllFields();
            }
        });

        btnLogout.setOnClickListener(v -> {
            getSharedPreferences("StafflinkPrefs", MODE_PRIVATE).edit().clear().apply();
            startActivity(new Intent(this, Employee_page.class));
            finish();
        });
    }


    // =========================================================================
    // LOAD profile from Firebase
    // =========================================================================

    private void loadProfile() {
        profileRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                etName.setText(snapshot.child("name").getValue(String.class));
                etEmail.setText(snapshot.child("email").getValue(String.class));

                Object phone = snapshot.child("phone").getValue();
                etPhone.setText(phone != null ? phone.toString() : "");

                etDepartment.setText(snapshot.child("department").getValue(String.class));
                etPosition.setText(snapshot.child("position").getValue(String.class));
                // Password not loaded — security practice
                etPassword.setHint("Leave blank to keep current password");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SettingActivity.this,
                        "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }


    // =========================================================================
    // SAVE all fields to Firebase
    // =========================================================================

    private void saveSettings() {
        String name       = etName.getText().toString().trim();
        String email      = etEmail.getText().toString().trim();
        String phone      = etPhone.getText().toString().trim();
        String department = etDepartment.getText().toString().trim();
        String position   = etPosition.getText().toString().trim();
        String password   = etPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and email are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build update map
        Map<String, Object> updates = new HashMap<>();
        updates.put("name",       name);
        updates.put("email",      email);
        updates.put("phone",      phone);
        updates.put("department", department);
        updates.put("position",   position);

        // Only update password if employee typed a new one
        if (!password.isEmpty()) {
            if (password.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            updates.put("password", password);
        }

        profileRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Settings saved ✅", Toast.LENGTH_SHORT).show();
                    disableAllFields();
                    // Clear password field after save
                    etPassword.setText("");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }


    // =========================================================================
    // ENABLE / DISABLE all fields
    // =========================================================================

    private void enableAllFields() {
        isEditMode = true;
        btnSubmit.setText("Save Settings");

        etName.setEnabled(true);
        etEmail.setEnabled(true);
        etPhone.setEnabled(true);
        etDepartment.setEnabled(true);
        etPosition.setEnabled(true);
        etPassword.setEnabled(true);
        switchDarkMode.setEnabled(true);
        switchNotifications.setEnabled(true);
    }

    private void disableAllFields() {
        isEditMode = false;
        btnSubmit.setText("Edit Settings");

        etName.setEnabled(false);
        etEmail.setEnabled(false);
        etPhone.setEnabled(false);
        etDepartment.setEnabled(false);
        etPosition.setEnabled(false);
        etPassword.setEnabled(false);
        switchDarkMode.setEnabled(false);
        switchNotifications.setEnabled(false);
    }
}