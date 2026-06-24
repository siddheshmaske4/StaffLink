package com.example.stafflink;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.*;

import java.util.*;

public class AttendanceActivity extends AppCompatActivity {

    private TextView punchIn, punchOut, lateAllowed, absentRule;
    private ImageButton btnBack;
    private ImageView editAttendanceData;
    private ImageView btnGeofencing;
    private RecyclerView recyclerDates;
    private EditText searchDate;

    private DatabaseReference rootRef, rootReg;
    private String adminKey, companyCode;

    private DateAdapter dateAdapter;
    private final List<DateAttendanceModel> dateList = new ArrayList<>();
    private final List<DateAttendanceModel> fullDateList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        companyCode = getIntent().getStringExtra("COMPANY_CODE");
        adminKey = getIntent().getStringExtra("ADMIN_NODE");

        if (companyCode == null || adminKey == null) {
            Toast.makeText(this, "Invalid data", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        rootRef = FirebaseDatabase.getInstance().getReference("Stafflink");
        rootReg = FirebaseDatabase.getInstance().getReference("companies");

        initViews();
        loadAttendanceRules();
        loadAttendanceGroupedByDate();
    }

    private void initViews() {
        punchIn = findViewById(R.id.punchInTime);
        punchOut = findViewById(R.id.punchOutTime);
        lateAllowed = findViewById(R.id.lateAllowed);
        absentRule = findViewById(R.id.absentRule);
        btnBack = findViewById(R.id.btnBack);
        editAttendanceData = findViewById(R.id.editAttendanceData);
        recyclerDates = findViewById(R.id.recyclerDates);
        searchDate = findViewById(R.id.searchEmployee); // Reusing the search EditText

        recyclerDates.setLayoutManager(new LinearLayoutManager(this));
        dateAdapter = new DateAdapter(dateList);
        recyclerDates.setAdapter(dateAdapter);

        btnBack.setOnClickListener(v -> finish());
        editAttendanceData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(AttendanceActivity.this, edit_Attendance.class);
                i.putExtra("ADMIN_NODE_KEY", adminKey);
                startActivity(i);
                finish();
            }
        });

        searchDate.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterDates(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnGeofencing = findViewById(R.id.btnGeofencing);
        btnGeofencing.setOnClickListener(v -> {
            Intent i = new Intent(this, GeofencingSettingsActivity.class);
            startActivity(i);
        });
    }

    private void filterDates(String query) {
        String lowerQuery = query.toLowerCase().trim();
        dateList.clear();

        if (lowerQuery.isEmpty()) {
            dateList.addAll(fullDateList);
        } else {
            for (DateAttendanceModel item : fullDateList) {
                if (item.getDate().toLowerCase().contains(lowerQuery)) {
                    dateList.add(item);
                }
            }
        }
        dateAdapter.notifyDataSetChanged();
    }

    private void loadAttendanceRules() {
        rootRef.child("admins")
                .child(adminKey)
                .child("attendanceRules")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        if (!snap.exists()) return;
                        punchIn.setText("Punch In: " + getString(snap, "workHours/punchInTime"));
                        punchOut.setText("Punch Out: " + getString(snap, "workHours/punchOutTime"));
                        lateAllowed.setText("Late after: " + getString(snap, "lateRules/markAsLateAfter"));
                        absentRule.setText("Absent if no check-in by: " + getString(snap, "absentRules/markAsAbsentIfNoCheckInBy"));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadAttendanceGroupedByDate() {
        dateList.clear();
        fullDateList.clear();

        rootReg.child(companyCode).child("employees")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Map<String, List<AttendanceModel>> map = new TreeMap<>(Collections.reverseOrder());

                        for (DataSnapshot emp : snapshot.getChildren()) {
                            String name = emp.child("profile").child("name").getValue(String.class);
                            if (name == null) name = "Unknown";

                            DataSnapshot attendanceSnap = emp.child("attendance");
                            if (!attendanceSnap.exists()) continue;

                            for (DataSnapshot dateSnap : attendanceSnap.getChildren()) {
                                String date = dateSnap.getKey();
                                String status = get(dateSnap, "status");
                                String checkIn = dateSnap.child("checkIn").getValue() != null
                                        ? dateSnap.child("checkIn").getValue().toString()
                                        : "";
                                String checkOut = dateSnap.child("checkOut").getValue() != null
                                        ? dateSnap.child("checkOut").getValue().toString()
                                        : "";
                                int overtime = dateSnap.child("overtimeHours").getValue(Integer.class) == null ? 0 :
                                        dateSnap.child("overtimeHours").getValue(Integer.class);

                                int full = emp.child("leaves").child(date.substring(0, 7))
                                        .child("fullDaysUsed").getValue(Integer.class) == null ? 0 :
                                        emp.child("leaves").child(date.substring(0, 7))
                                                .child("fullDaysUsed").getValue(Integer.class);

                                int half = emp.child("leaves").child(date.substring(0, 7))
                                        .child("halfDaysUsed").getValue(Integer.class) == null ? 0 :
                                        emp.child("leaves").child(date.substring(0, 7))
                                                .child("halfDaysUsed").getValue(Integer.class);

                                int paid = emp.child("leaves").child(date.substring(0, 7))
                                        .child("paidLeavesUsed").getValue(Integer.class) == null ? 0 :
                                        emp.child("leaves").child(date.substring(0, 7))
                                                .child("paidLeavesUsed").getValue(Integer.class);

                                AttendanceModel attendance = new AttendanceModel(name, status,
                                        checkIn, checkOut, overtime, full, half, paid);

                                map.computeIfAbsent(date, k -> new ArrayList<>()).add(attendance);
                            }
                        }

                        for (String date : map.keySet()) {
                            dateList.add(new DateAttendanceModel(date, map.get(date)));
                        }

                        fullDateList.addAll(dateList); // save full list
                        dateAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private String get(DataSnapshot s, String k) {
        String v = s.child(k).getValue(String.class);
        return v == null ? "--" : v;
    }

    private String getString(DataSnapshot snap, String path) {
        String v = snap.child(path).getValue(String.class);
        return v == null ? "--" : v;
    }
}
