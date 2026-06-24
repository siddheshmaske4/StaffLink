package com.example.stafflink;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.*;

public class Employee_page extends AppCompatActivity {

    private TextInputEditText companyCodeInput, emailInput, passwordInput;
    private Button loginButton;
    private TextView signupTextView;

    private DatabaseReference companiesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_page);

        // Initialize views
        companyCodeInput = findViewById(R.id.companyCodeInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.employeeButton);
        signupTextView = findViewById(R.id.signuptextview);

        // Firebase reference
        companiesRef = FirebaseDatabase.getInstance().getReference("companies");

        // Check if already logged in
        SharedPreferences prefs = getSharedPreferences("StafflinkPrefs", MODE_PRIVATE);
        String savedCompany = prefs.getString("company_code", null);
        String savedEmpID = prefs.getString("emp_id", null);

        if (savedCompany != null && savedEmpID != null) {
            // Already logged in → go to dashboard
            Intent intent = new Intent(Employee_page.this, bottom_nav.class);
            startActivity(intent);
            finish();
            return;
        }

        // Signup click
        signupTextView.setOnClickListener(v -> {
            startActivity(new Intent(Employee_page.this, Employee_Signup_page.class));
            finish();
        });

        // Login click
        loginButton.setOnClickListener(v -> {
            String companyCode = companyCodeInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (companyCode.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if company exists
            companiesRef.child(companyCode).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot companySnapshot) {
                    if (companySnapshot.exists() && companySnapshot.hasChild("employees")) {
                        boolean[] found = {false};

                        // Loop through employees
                        for (DataSnapshot empSnapshot : companySnapshot.child("employees").getChildren()) {

                            try {
                                android.util.Log.d("EMPDATA", "emp: " + empSnapshot.getKey());
                                for (DataSnapshot field : empSnapshot.child("profile").getChildren()) {
                                    android.util.Log.d("EMPDATA", "field: " + field.getKey()
                                            + " = " + field.getValue()
                                            + " type: " + (field.getValue() != null ? field.getValue().getClass().getSimpleName() : "null"));
                                }

                                DataSnapshot profile = empSnapshot.child("profile");
                                String empEmail    = profile.child("email").getValue(String.class);
                                String empPassword = profile.child("password").getValue(String.class);

                                if (empEmail == null || empPassword == null) continue;

                                if (email.equals(empEmail) && password.equals(empPassword)) {
                                    found[0] = true;
                                    getSharedPreferences("StafflinkPrefs", MODE_PRIVATE)
                                            .edit()
                                            .putString("company_code", companyCode)
                                            .putString("emp_id", empSnapshot.getKey())
                                            .putString("email", email)
                                            .apply();
                                    Toast.makeText(Employee_page.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(Employee_page.this, bottom_nav.class);
                                    startActivity(intent);
                                    finish();
                                    break;
                                }
                            } catch (Exception e) {
                                android.util.Log.e("EMPDATA", "Crash on emp: " + empSnapshot.getKey() + " error: " + e.getMessage());
                            }
                        }

                        if (!found[0]) {
                            Toast.makeText(Employee_page.this, "Invalid email or password", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Toast.makeText(Employee_page.this, "Company code not found", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Toast.makeText(Employee_page.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
