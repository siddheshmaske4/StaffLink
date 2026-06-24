package com.example.stafflink;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;

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
 * EmployeeInboxActivity — Employee inbox.
 * Reads from: Stafflink/companies/{companyCode}/employees/{empId}/messages
 * Same adapter as admin inbox, different Firebase path + role passed to detail screen.
 */
public class EmployeeInboxActivity extends AppCompatActivity {

    private RecyclerView     rvInbox;
    private InboxAdapter     adapter;
    private List<MessageModel> messageList;
    private ImageButton      btnBack;

    private String companyCode, empId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox); // reuses same inbox layout

        // Read session
        SharedPreferences sp = getSharedPreferences("StafflinkPrefs", MODE_PRIVATE);
        companyCode = sp.getString("company_code", null);
        empId       = sp.getString("emp_id",       null);

        if (companyCode == null || empId == null) {
            finish();
            return;
        }

        rvInbox = findViewById(R.id.rvInbox);
        rvInbox.setLayoutManager(new LinearLayoutManager(this));

        messageList = new ArrayList<>();
        adapter     = new InboxAdapter(this, messageList);
        rvInbox.setAdapter(adapter);



        loadEmployeeInbox();

        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadEmployeeInbox() {
        FirebaseDatabase.getInstance()
                .getReference("companies")  // ← remove "Stafflink"
                .child(companyCode)
                .child("employees")
                .child(empId)
                .child("messages")
                .orderByChild("createdAt")
                .addValueEventListener(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        messageList.clear();
                        for (DataSnapshot d : snapshot.getChildren()) {
                            try {
                                MessageModel msg = d.getValue(MessageModel.class);
                                if (msg != null) {
                                    msg.messageId = d.getKey();
                                    messageList.add(msg);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        Collections.reverse(messageList);
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}