//package com.example.stafflink;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.Toast;
//import androidx.appcompat.app.AppCompatActivity;
//import com.google.firebase.database.*;
//
//import java.util.HashMap;
//import java.util.Map;
//
//public class CompanyCodeSetupActivity extends AppCompatActivity {
//
//    private EditText companyCodeInput;
//    private Button setCompanyButton;
//    private DatabaseReference adminsRef;
//    private DatabaseReference companiesRef;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_company_code_setup);
//
//        companyCodeInput = findViewById(R.id.companyCodeInput);
//        setCompanyButton = findViewById(R.id.setCompanyButton);
//
//        // Get adminID and password from previous login
//        String adminID = getIntent().getStringExtra("ADMIN_ID");
//        String adminNodeKey = getIntent().getStringExtra("adminNode");
//
//
//        // Firebase references
//        adminsRef = FirebaseDatabase.getInstance().getReference("Stafflink/admins").child(adminNodeKey);
//        companiesRef = FirebaseDatabase.getInstance().getReference("companies");
//
//        setCompanyButton.setOnClickListener(v -> {
//            String companyCode = companyCodeInput.getText().toString().trim();
//
//            if (companyCode.isEmpty()) {
//                Toast.makeText(this, "Enter Company Code ❌", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            // 1️⃣ Save company code under admin node
//            adminsRef.child("companyCode").setValue(companyCode)
//                    .addOnCompleteListener(task -> {
//                        if (task.isSuccessful()) {
//                            // 2️⃣ Check if company node exists
//                            companiesRef.child(companyCode).addListenerForSingleValueEvent(new ValueEventListener() {
//                                @Override
//                                public void onDataChange(DataSnapshot snapshot) {
//                                    if (!snapshot.exists()) {
//                                        // Create company node with empty employees
//                                        Map<String, Object> companyData = new HashMap<>();
//                                        companyData.put("employees", new HashMap<>()); // empty initially
//                                        companiesRef.child(companyCode);
//                                    }
//
//                                    // 3️⃣ Save session locally
//                                    getSharedPreferences("ADMIN_SESSION", MODE_PRIVATE)
//                                            .edit()
//                                            .putString("ADMIN_ID", adminID)
//                                            .putString("COMPANY_CODE", companyCode)
//                                            .apply();
//
//                                    Toast.makeText(CompanyCodeSetupActivity.this, "Company Code Set ✅", Toast.LENGTH_SHORT).show();
//
//                                    // 4️⃣ Redirect to Admin Dashboard
//                                    Intent i = new Intent(CompanyCodeSetupActivity.this, AdminDashboardActivity.class);
//                                    i.putExtra("ADMIN_ID", adminID);
//                                    i.putExtra("COMPANY_CODE", companyCode);
//                                    startActivity(i);
//                                    finish();
//                                }
//
//                                @Override
//                                public void onCancelled(DatabaseError error) {
//                                    Toast.makeText(CompanyCodeSetupActivity.this, "Firebase Error ❌", Toast.LENGTH_SHORT).show();
//                                }
//                            });
//                        } else {
//                            Toast.makeText(this, "Failed to set company code ❌", Toast.LENGTH_SHORT).show();
//                        }
//                    });
//        });
//    }
//}
//

package com.example.stafflink;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class CompanyCodeSetupActivity extends AppCompatActivity {

    private EditText companyCodeInput;
    private Button setCompanyButton;

    private DatabaseReference adminRef;
    private DatabaseReference companiesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_code_setup);

        companyCodeInput = findViewById(R.id.companyCodeInput);
        setCompanyButton = findViewById(R.id.setCompanyButton);

        // Data from previous screen
        String adminID = getIntent().getStringExtra("ADMIN_ID");
        String adminNodeKey = getIntent().getStringExtra("adminNode");

        // Firebase references
        adminRef = FirebaseDatabase.getInstance()
                .getReference("Stafflink")
                .child("admins")
                .child(adminNodeKey);

        companiesRef = FirebaseDatabase.getInstance()
                .getReference("companies");

        setCompanyButton.setOnClickListener(v -> {

            String companyCode = companyCodeInput.getText().toString().trim();

            if (companyCode.isEmpty()) {
                Toast.makeText(this, "Enter Company Code ❌", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1️⃣ Save company code under ADMIN
            adminRef.child("companyCode").setValue(companyCode)
                    .addOnSuccessListener(unused -> {

                        // 2️⃣ Ensure company node exists
                        companiesRef.child(companyCode)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot snapshot) {

                                        if (!snapshot.exists()) {

                                            Map<String, Object> companyData = new HashMap<>();
                                            companyData.put("employees", new HashMap<>());
                                            companyData.put("createdAt", System.currentTimeMillis());

                                            // ✅ THIS LINE CREATES THE NODE
                                            companiesRef.child(companyCode).setValue(companyData);
                                        }

                                        // 3️⃣ Save session locally
                                        getSharedPreferences("ADMIN_SESSION", MODE_PRIVATE)
                                                .edit()
                                                .putString("ADMIN_ID", adminID)
                                                .putString("COMPANY_CODE", companyCode)
                                                .apply();

                                        Toast.makeText(
                                                CompanyCodeSetupActivity.this,
                                                "Company Code Set ✅",
                                                Toast.LENGTH_SHORT
                                        ).show();

                                        // 4️⃣ Go to dashboard
                                        Intent i = new Intent(
                                                CompanyCodeSetupActivity.this,
                                                AdminDashboardActivity.class
                                        );
                                        i.putExtra("ADMIN_ID", adminID);
                                        i.putExtra("COMPANY_CODE", companyCode);
                                        startActivity(i);
                                        finish();
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError error) {
                                        Toast.makeText(
                                                CompanyCodeSetupActivity.this,
                                                "Firebase Error ❌",
                                                Toast.LENGTH_SHORT
                                        ).show();
                                    }
                                });

                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(
                                    this,
                                    "Failed to save company code ❌",
                                    Toast.LENGTH_SHORT
                            ).show()
                    );
        });
    }
}
