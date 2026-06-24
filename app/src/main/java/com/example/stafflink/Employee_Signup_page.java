package com.example.stafflink;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class Employee_Signup_page extends AppCompatActivity {

    private EditText companycodeInput, employeeEmailInput, passwordInput, contactInput;
    private TextView loginTextView;
    private Button signupButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_signup_page);

        companycodeInput = findViewById(R.id.companycodeInput);
        employeeEmailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        signupButton = findViewById(R.id.signupButton);
        loginTextView = findViewById(R.id.logintextview);

        loginTextView.setOnClickListener(v -> {
            startActivity(new Intent(Employee_Signup_page.this, Employee_page.class));
            finish();
        });

        signupButton.setOnClickListener(v -> {
            String Ccode = companycodeInput.getText().toString().trim();
            String employeeEmail = employeeEmailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (Ccode.isEmpty() || employeeEmail.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseReference companiesRef = FirebaseDatabase.getInstance().getReference("companies");

            // Check if company exists
            companiesRef.child(Ccode).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // Generate a unique employee ID (you can also use push().getKey())
                        String empUID = companiesRef.child(Ccode).child("employees").push().getKey();

                        if (empUID == null) {
                            Toast.makeText(Employee_Signup_page.this, "Failed to generate employee ID", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Prepare employee profile with only email and password for now
                        HashMap<String, Object> profileData = new HashMap<>();
                        profileData.put("email", employeeEmail);
                        profileData.put("password", password); // Note: later you can hash this for security

                        // Save under companies/{companyCode}/employees/{emp_UID}/profile
                        companiesRef.child(Ccode)
                                .child("employees")
                                .child(empUID)
                                .child("profile")
                                .setValue(profileData)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(Employee_Signup_page.this,
                                                "Employee registered successfully!", Toast.LENGTH_LONG).show();

                                        // Pass company code to login page
                                        Intent intent = new Intent(Employee_Signup_page.this, Employee_page.class);
                                        intent.putExtra("company_code", Ccode);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(Employee_Signup_page.this,
                                                "Failed to register employee.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(Employee_Signup_page.this, "Company code not found!", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Toast.makeText(Employee_Signup_page.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });



    }
}
