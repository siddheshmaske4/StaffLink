package com.example.stafflink;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    Button companyButton, employeeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        companyButton = findViewById(R.id.companyButton);
        employeeButton = findViewById(R.id.employeeButton);

        companyButton.setOnClickListener(v -> {

            boolean isLoggedIn = getSharedPreferences("ADMIN_SESSION", MODE_PRIVATE)
                    .getBoolean("LOGGED_IN", false);
            String adminNode = getSharedPreferences("ADMIN_SESSION", MODE_PRIVATE)
                    .getString("adminNode", null);
            String companyCode = getSharedPreferences("ADMIN_SESSION", MODE_PRIVATE)
                    .getString("COMPANY_CODE", null);

            if (isLoggedIn && adminNode != null && companyCode != null) {
                Intent intent = new Intent(MainActivity.this, AdminDashboardActivity.class);
                intent.putExtra("adminNode", adminNode);
                intent.putExtra("COMPANY_CODE", companyCode);
                startActivity(intent);
                finish();
            } else {
                startActivity(new Intent(MainActivity.this, Admin_page.class));
                finish(); // ← ADD THIS
            }
        });

        employeeButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, Employee_page.class));
            finish(); // ← ADD THIS
        });

        employeeButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, Employee_page.class));
        });
    }
}
