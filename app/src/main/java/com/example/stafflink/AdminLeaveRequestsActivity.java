package com.example.stafflink;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AdminLeaveRequestsActivity — read-only view of all leave requests
 * submitted by employees.
 *
 * Firebase read path:
 *   Stafflink/admins/{adminKey}/inbox/{msgId}  where type == "leave"
 *
 * No approve/reject — salary deduction already happens automatically
 * on submission. This is purely a visibility screen for admin.
 */
public class AdminLeaveRequestsActivity extends AppCompatActivity {

    private ImageButton  btnBack;
    private RecyclerView rvLeaveRequests;
    private TextView     txtEmpty;

    private LeaveRequestAdapter   adapter;
    private List<LeaveRequestModel> leaveList = new ArrayList<>();

    private String adminKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_leave_requests);

        SharedPreferences sp = getSharedPreferences("ADMIN_SESSION", MODE_PRIVATE);
        adminKey = sp.getString("adminNode", null);

        if (adminKey == null) {
            Toast.makeText(this, "Session error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        setupRecyclerView();
        loadLeaveRequests();

        btnBack.setOnClickListener(v -> finish());
    }

    private void bindViews() {
        btnBack         = findViewById(R.id.btnBack);
        rvLeaveRequests = findViewById(R.id.rvLeaveRequests);
        txtEmpty        = findViewById(R.id.txtEmpty);
    }

    private void setupRecyclerView() {
        rvLeaveRequests.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LeaveRequestAdapter(this, leaveList, true); // true = show employee email
        rvLeaveRequests.setAdapter(adapter);
    }

    private void loadLeaveRequests() {
        FirebaseDatabase.getInstance()
                .getReference("Stafflink")
                .child("admins")
                .child(adminKey)
                .child("inbox")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        leaveList.clear();

                        for (DataSnapshot d : snapshot.getChildren()) {
                            try {
                                String type = d.child("type").getValue(String.class);
                                if (!"leave".equals(type)) continue;

                                LeaveRequestModel item = d.getValue(LeaveRequestModel.class);
                                if (item != null) {
                                    item.messageId = d.getKey();
                                    leaveList.add(item);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        // Newest first
                        Collections.sort(leaveList,
                                (a, b) -> Long.compare(b.createdAt, a.createdAt));

                        txtEmpty.setVisibility(leaveList.isEmpty() ? View.VISIBLE : View.GONE);
                        rvLeaveRequests.setVisibility(leaveList.isEmpty() ? View.GONE : View.VISIBLE);

                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AdminLeaveRequestsActivity.this,
                                "Failed to load leave requests", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}