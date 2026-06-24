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
 * InboxActivity — Admin inbox.
 * Reads from: Stafflink/admins/{adminKey}/messages
 * Change: added item click → opens MessageDetailActivity with role="admin"
 */
public class InboxActivity extends AppCompatActivity {

    private RecyclerView rvInbox;
    private InboxAdapter adapter;
    private List<MessageModel> messageList;
    private String adminKey;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);

        SharedPreferences sp = getSharedPreferences("ADMIN_SESSION", MODE_PRIVATE);

        // Try intent first, fallback to SharedPreferences
        adminKey = getIntent().getStringExtra("ADMIN_NODE");
        if (adminKey == null || adminKey.isEmpty()) {
            adminKey = sp.getString("adminNode", null);
        }
        if (adminKey == null || adminKey.isEmpty()) {
            finish();
            return;
        }

        rvInbox = findViewById(R.id.rvInbox);
        rvInbox.setLayoutManager(new LinearLayoutManager(this));

        messageList = new ArrayList<>();
        adapter = new InboxAdapter(this, messageList);
        rvInbox.setAdapter(adapter);


        loadAdminInbox();

        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadAdminInbox() {
        FirebaseDatabase.getInstance()
                .getReference("Stafflink")
                .child("admins")
                .child(adminKey)
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
                                    msg.messageId = d.getKey(); // ← set key manually
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