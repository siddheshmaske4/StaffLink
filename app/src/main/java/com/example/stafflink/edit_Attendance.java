package com.example.stafflink;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class edit_Attendance extends AppCompatActivity {

    EditText etPunchIn, etPunchOut, etLateAfter, etAbsentAfter;
    Button btnEditRules, btnSaveRules;
    ImageButton btnBack;
    DatabaseReference attendanceRulesRef;
    String adminNodeKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_attendance);

        // 1️⃣ Get adminNodeKey
        adminNodeKey = getIntent().getStringExtra("ADMIN_NODE_KEY");

        if (adminNodeKey == null) {
            Toast.makeText(this, "Admin not identified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2️⃣ Init Views
        etPunchIn = findViewById(R.id.etPunchIn);
        etPunchOut = findViewById(R.id.etPunchOut);
        etLateAfter = findViewById(R.id.etLateAfter);
        etAbsentAfter = findViewById(R.id.etAbsentAfter);

        btnEditRules = findViewById(R.id.btnEditRules);
        btnSaveRules = findViewById(R.id.btnSaveRules);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        // 3️⃣ Firebase Path
        attendanceRulesRef = FirebaseDatabase.getInstance()
                .getReference("Stafflink")
                .child("admins")
                .child(adminNodeKey)
                .child("attendanceRules");

        // 4️⃣ Lock fields initially
        setEditable(false);

        // 5️⃣ Fetch existing rules
        fetchAttendanceRules();

        // 6️⃣ Edit button
        btnEditRules.setOnClickListener(v -> {
            setEditable(true);
            btnEditRules.setVisibility(View.GONE);
            btnSaveRules.setVisibility(View.VISIBLE);
        });

        // 7️⃣ Save button
        btnSaveRules.setOnClickListener(v -> saveRules());
    }

    // 🔹 Fetch rules
    private void fetchAttendanceRules() {
        attendanceRulesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()) return;

                // Work hours
                etPunchIn.setText(snapshot.child("workHours")
                        .child("punchInTime").getValue(String.class));

                etPunchOut.setText(snapshot.child("workHours")
                        .child("punchOutTime").getValue(String.class));

                // Late rule
                etLateAfter.setText(snapshot.child("lateRules")
                        .child("markAsLateAfter").getValue(String.class));

                // Absent rule
                etAbsentAfter.setText(snapshot.child("absentRules")
                        .child("markAsAbsentIfNoCheckInBy").getValue(String.class));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(edit_Attendance.this,
                        "Failed to load rules", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 🔹 Save / Update rules
    private void saveRules() {

        String punchIn = etPunchIn.getText().toString().trim();
        String punchOut = etPunchOut.getText().toString().trim();
        String lateAfter = etLateAfter.getText().toString().trim();
        String absentAfter = etAbsentAfter.getText().toString().trim();

        if (punchIn.isEmpty() || punchOut.isEmpty()
                || lateAfter.isEmpty() || absentAfter.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        attendanceRulesRef.child("workHours").child("punchInTime").setValue(punchIn);
        attendanceRulesRef.child("workHours").child("punchOutTime").setValue(punchOut);

        attendanceRulesRef.child("lateRules").child("markAsLateAfter").setValue(lateAfter);

        attendanceRulesRef.child("absentRules")
                .child("markAsAbsentIfNoCheckInBy")
                .setValue(absentAfter)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "Attendance rules updated", Toast.LENGTH_SHORT).show();

                        setEditable(false);
                        btnSaveRules.setVisibility(View.GONE);
                        btnEditRules.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(this,
                                "Update failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // 🔹 Enable / Disable EditTexts
    private void setEditable(boolean enabled) {
        etPunchIn.setEnabled(enabled);
        etPunchOut.setEnabled(enabled);
        etLateAfter.setEnabled(enabled);
        etAbsentAfter.setEnabled(enabled);
    }
}
