package com.example.stafflink;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;

public class Admin_page extends AppCompatActivity {

    private Button loginButton;
    private EditText adminIDInput, passwordInput;
    private DatabaseReference adminsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_page);

        loginButton = findViewById(R.id.adminLoginButton);
        adminIDInput = findViewById(R.id.adminIDInput);
        passwordInput = findViewById(R.id.adminpasswordInput);

        // Reference to admins node in Firebase
        adminsRef = FirebaseDatabase.getInstance()
                .getReference("Stafflink/admins");

        loginButton.setOnClickListener(v -> {
            String adminID = adminIDInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (TextUtils.isEmpty(adminID) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Enter Admin ID and Password ❌", Toast.LENGTH_SHORT).show();
                return;
            }

            verifyAdminCredentials(adminID, password);
        });
    }

    private void verifyAdminCredentials(String adminID, String password) {
        adminsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean isValid = false;
                String fetchedCompanyCode = null;
                String adminNodeKey = null;

                for (DataSnapshot adminNode : snapshot.getChildren()) {
                    String dbAdminID = adminNode.child("adminID").getValue(String.class);
                    String dbPassword = adminNode.child("password").getValue(String.class);
                    String dbCompanyCode = adminNode.child("companyCode").getValue(String.class);

                    if (adminID.equals(dbAdminID) && password.equals(dbPassword)) {
                        isValid = true;
                        fetchedCompanyCode = dbCompanyCode;
                        adminNodeKey = adminNode.getKey(); // Key like "admin_techcorp"
                        break;
                    }
                }

                if (isValid) {
                    // Save adminNode for session
                    SharedPreferences sp = getSharedPreferences("ADMIN_SESSION", MODE_PRIVATE);
                    sp.edit()
                            .putString("adminNode", adminNodeKey)
                            .putString("COMPANY_CODE", fetchedCompanyCode != null ? fetchedCompanyCode : "")
                            .putBoolean("LOGGED_IN", true)
                            .apply();


                    if (TextUtils.isEmpty(fetchedCompanyCode)) {
                        // First-time login → setup company
                        Intent i = new Intent(Admin_page.this, CompanyCodeSetupActivity.class);
                        i.putExtra("ADMIN_ID", adminID);
                        i.putExtra("adminNode", adminNodeKey);
                        startActivity(i);
                    } else {
                        // Existing admin → open dashboard
                        Intent i = new Intent(Admin_page.this, AdminDashboardActivity.class);
                        i.putExtra("ADMIN_ID", adminID);
                        i.putExtra("COMPANY_CODE", fetchedCompanyCode);
                        startActivity(i);
                    }
                    finish();
                } else {
                    Toast.makeText(Admin_page.this, "Invalid Admin ID or Password ❌", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Admin_page.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
