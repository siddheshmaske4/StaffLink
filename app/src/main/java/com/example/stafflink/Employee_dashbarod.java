package com.example.stafflink;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class Employee_dashbarod extends AppCompatActivity {

    TextView txtCircle, txtUsername, txtEmail;
    Button btnPunchIn, btnPunchOut;
    ImageButton btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_employee_dashbarod);

        // Bind UI
        txtCircle = findViewById(R.id.txtCircle);
        txtUsername = findViewById(R.id.txtUsername);
        txtEmail = findViewById(R.id.txtEmail);
        btnPunchIn = findViewById(R.id.btnPunchIn);
        btnPunchOut = findViewById(R.id.btnPunchOut);
        btnLogout = findViewById(R.id.btnLogout);

        // Get saved login info
        SharedPreferences prefs = getSharedPreferences("StafflinkPrefs", MODE_PRIVATE);
        String email = prefs.getString("email", "");

        if (!email.isEmpty()) {
            String firstLetter = email.substring(0, 1).toUpperCase();

            txtCircle.setText(firstLetter);
            txtUsername.setText(firstLetter + email.substring(1, email.indexOf("@")));
            txtEmail.setText(email);
        }

        // Punch In
        btnPunchIn.setOnClickListener(v ->
                Toast.makeText(this, "Punch In Successful", Toast.LENGTH_SHORT).show()
        );

        // Punch Out
        btnPunchOut.setOnClickListener(v ->
                Toast.makeText(this, "Punch Out Successful", Toast.LENGTH_SHORT).show()
        );

        // Logout
        btnLogout.setOnClickListener(v -> {

            getSharedPreferences("StafflinkPrefs", MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply();

            startActivity(new Intent(Employee_dashbarod.this, Employee_page.class));
            finish();
        });
    }
}
