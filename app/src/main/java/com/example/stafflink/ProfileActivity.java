package com.example.stafflink;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.content.SharedPreferences;

public class ProfileActivity extends AppCompatActivity {

    TextView tvAdminName, tvDetailEmail, tvDetailPhone, tvCompanyName, tvCompanyCode,
            tvCompanyEmail, tvCompanyNumber, tvIndustry, tvLocation,
            tvEmployeeCount, tvDepartments, tvInitial;

    ImageButton btnBack, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initViews();
        loadAdminProfile();
    }

    private void initViews() {
        tvInitial = findViewById(R.id.tvDetailInitial);
        tvAdminName = findViewById(R.id.tvAdminName);
        tvDetailEmail = findViewById(R.id.tvDetailEmail);
        tvDetailPhone = findViewById(R.id.tvDetailPhone);

        tvCompanyName = findViewById(R.id.tvCompanyName);
        tvCompanyCode = findViewById(R.id.tvCompanyCode);
        tvCompanyEmail = findViewById(R.id.tvCompanyEmail);
        tvCompanyNumber = findViewById(R.id.tvCompanyNumber);
        tvIndustry = findViewById(R.id.tvIndustry);
        tvLocation = findViewById(R.id.tvLocation);
        tvEmployeeCount = findViewById(R.id.tvEmployeeCount);
        tvDepartments = findViewById(R.id.tvDepartments);

        btnBack = findViewById(R.id.btnBack);
        btnLogout = findViewById(R.id.btnLogout);

        btnBack.setOnClickListener(v -> finish());
    }

    private void loadAdminProfile() {

        SharedPreferences sp = getSharedPreferences("ADMIN_SESSION", MODE_PRIVATE);
        String adminKey = sp.getString("adminNode", null);

        if (adminKey == null) {
            Toast.makeText(this, "Admin key missing", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Stafflink")
                .child("admins")
                .child(adminKey);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                if (!snapshot.exists()) {
                    Toast.makeText(ProfileActivity.this, "Admin not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                // -------- PERSONAL DETAILS --------
                String raw = adminKey.replace("admin_", "");
                String adminName = raw.substring(0, 1).toUpperCase() + raw.substring(1);
                String companyEmail = snapshot.child("companyInfo").child("companyEmail").getValue(String.class);

                // CompanyNumber is Long in Firebase → convert safely
                Object numObj = snapshot.child("companyInfo").child("CompanyNumber").getValue();
                String companyNumber = numObj != null ? numObj.toString() : "";

                if (adminName != null) {
                    tvAdminName.setText(adminName);
                    tvInitial.setText(adminName.substring(0, 1).toUpperCase());
                }

                if (companyEmail != null) tvDetailEmail.setText(companyEmail);
                if (!companyNumber.isEmpty()) tvDetailPhone.setText(companyNumber);


                // -------- COMPANY INFO --------
                DataSnapshot company = snapshot.child("companyInfo");

                String companyName = company.child("companyName").getValue(String.class);
                String companyCode = snapshot.child("companyCode").getValue(String.class);
                String industry = company.child("industry").getValue(String.class);
                String location = company.child("location").getValue(String.class);


                if (companyName != null) tvCompanyName.setText(companyName);
                if (companyCode != null) tvCompanyCode.setText("Company Code: " + companyCode);
                if (companyEmail != null) tvCompanyEmail.setText(companyEmail);
                if (!companyNumber.isEmpty()) tvCompanyNumber.setText("Company Number: " + companyNumber);
                if (industry != null) tvIndustry.setText("Industry: " + industry);
                if (location != null) tvLocation.setText("Location: " + location);
                String companyCodeForCount = snapshot.child("companyCode").getValue(String.class);
                if (companyCodeForCount != null) {
                    FirebaseDatabase.getInstance()
                            .getReference("companies")
                            .child(companyCodeForCount)
                            .child("employees")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot empSnapshot) {
                                    tvEmployeeCount.setText("Employees: " + empSnapshot.getChildrenCount());
                                }
                                @Override
                                public void onCancelled(DatabaseError error) {}
                            });
                }


                // -------- DEPARTMENTS LIST --------
                DataSnapshot deptSnap = snapshot.child("departments");
                StringBuilder depts = new StringBuilder();

                for (DataSnapshot d : deptSnap.getChildren()) {
                    depts.append(d.getKey()).append(", ");
                }

                if (depts.length() > 2)
                    depts.setLength(depts.length() - 2);

                tvDepartments.setText("Departments: " + depts);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
