package com.example.stafflink;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * MailFragment — Employee leave request screen.
 *
 * Top half: leave request form (unchanged).
 * Bottom half: "Your Leave History" — list of this employee's own
 *              past leave requests, read from their messages node.
 *
 * Fixes applied:
 *  - SharedPreferences key corrected to "StafflinkPrefs" (was "StaffLinkPrefs")
 *  - Admin lookup reads companyCode directly from admin node (was nested
 *    under companyInfo, which doesn't exist)
 *  - Added "leaveType" and "employeeEmail" fields to the saved message so
 *    the admin Leave Requests view and this history list can display them
 *    without extra Firebase lookups.
 */
public class MailFragment extends Fragment {

    EditText edtTitle, edtBody;
    RadioGroup radioLeaveType;
    Button btnSubmitLeave;

    RecyclerView rvLeaveHistory;
    TextView txtNoLeaveHistory;

    DatabaseReference rootRef;
    LeaveRequestAdapter historyAdapter;
    List<LeaveRequestModel> historyList = new ArrayList<>();

    String companyCode, employeeId, employeeEmail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_mail, container, false);

        edtTitle          = view.findViewById(R.id.edtTitle);
        edtBody           = view.findViewById(R.id.edtBody);
        radioLeaveType    = view.findViewById(R.id.radioLeaveType);
        btnSubmitLeave    = view.findViewById(R.id.btnSubmitLeave);
        rvLeaveHistory    = view.findViewById(R.id.rvLeaveHistory);
        txtNoLeaveHistory = view.findViewById(R.id.txtNoLeaveHistory);

        rootRef = FirebaseDatabase.getInstance().getReference();

        // ── Session — fixed key casing: "StafflinkPrefs" ──────────────────────
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("StafflinkPrefs", getContext().MODE_PRIVATE);

        companyCode   = prefs.getString("company_code", "");
        employeeId    = prefs.getString("emp_id", "");
        employeeEmail = prefs.getString("email", "");

        btnSubmitLeave.setOnClickListener(v -> submitLeave());

        setupLeaveHistory();
        loadLeaveHistory();

        return view;
    }

    // -------------------- SUBMIT --------------------

    private void submitLeave() {

        String title = edtTitle.getText().toString().trim();
        String body = edtBody.getText().toString().trim();

        if (title.isEmpty() || body.isEmpty()) {
            Toast.makeText(getContext(), "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedId = radioLeaveType.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(getContext(), "Select leave type", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton selectedRadio = getView().findViewById(selectedId);
        String leaveType = selectedRadio.getText().toString(); // Full Day or Half Day

        findAdminAndSend(title, body, leaveType);
    }

    // 🔍 Find admin dynamically using companyCode
    private void findAdminAndSend(String title, String body, String leaveType) {

        rootRef.child("Stafflink").child("admins")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        for (DataSnapshot adminSnap : snapshot.getChildren()) {

                            // companyCode is stored directly under the admin node
                            String adminCompanyCode =
                                    adminSnap.child("companyCode").getValue(String.class);

                            if (companyCode.equals(adminCompanyCode)) {

                                String adminKey = adminSnap.getKey();
                                sendLeaveToAdmin(adminKey, title, body, leaveType);
                                return;
                            }
                        }

                        Toast.makeText(getContext(), "Admin not found", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // 📩 Send message + update leave counts
    private void sendLeaveToAdmin(String adminKey,
                                  String title,
                                  String body,
                                  String leaveType) {

        String msgId = "msg_" + System.currentTimeMillis();
        long time = System.currentTimeMillis();

        Map<String, Object> sender = new HashMap<>();
        sender.put("id", employeeId);
        sender.put("role", "employee");

        Map<String, Object> message = new HashMap<>();
        message.put("title", title);
        message.put("body", body);
        message.put("createdAt", time);
        message.put("isRead", false);
        message.put("type", "leave");
        message.put("leaveType", leaveType);          // "Full Day" | "Half Day"
        message.put("employeeEmail", employeeEmail);  // shown in admin view
        message.put("sender", sender);

        // 📥 Admin inbox (separate "inbox" node, used only for leave requests)
        rootRef.child("Stafflink")
                .child("admins")
                .child(adminKey)
                .child("inbox")
                .child(msgId)
                .setValue(message);

        // 📥 Employee message copy — also powers "Your Leave History" below
        rootRef.child("companies")
                .child(companyCode)
                .child("employees")
                .child(employeeId)
                .child("messages")
                .child(msgId)
                .setValue(message);

        // 📆 Update leave counters for payroll
        updateLeaveStats(leaveType);

        Toast.makeText(getContext(), "Leave sent to admin", Toast.LENGTH_SHORT).show();

        edtTitle.setText("");
        edtBody.setText("");
        radioLeaveType.clearCheck();
    }

    // 📊 Dynamic payroll leave impact
    private void updateLeaveStats(String leaveType) {

        String monthKey = new SimpleDateFormat("yyyy-MM", Locale.getDefault())
                .format(new Date());

        DatabaseReference leaveRef = rootRef
                .child("companies")
                .child(companyCode)
                .child("employees")
                .child(employeeId)
                .child("leaves")
                .child(monthKey);

        leaveRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                int fullDays = snapshot.child("fullDaysUsed").getValue(Integer.class) == null ? 0 :
                        snapshot.child("fullDaysUsed").getValue(Integer.class);

                int halfDays = snapshot.child("halfDaysUsed").getValue(Integer.class) == null ? 0 :
                        snapshot.child("halfDaysUsed").getValue(Integer.class);

                if (leaveType.equalsIgnoreCase("Full Day")) {
                    fullDays++;
                } else {
                    halfDays++;
                }

                Map<String, Object> updates = new HashMap<>();
                updates.put("fullDaysUsed", fullDays);
                updates.put("halfDaysUsed", halfDays);

                leaveRef.updateChildren(updates);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }


    // =========================================================================
    // LEAVE HISTORY — employee's own past requests
    // =========================================================================

    private void setupLeaveHistory() {
        rvLeaveHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        historyAdapter = new LeaveRequestAdapter(getContext(), historyList, false); // false = own history, hide employee email
        rvLeaveHistory.setAdapter(historyAdapter);
    }

    private void loadLeaveHistory() {
        if (companyCode == null || companyCode.isEmpty() || employeeId == null || employeeId.isEmpty()) {
            return;
        }

        rootRef.child("companies")
                .child(companyCode)
                .child("employees")
                .child(employeeId)
                .child("messages")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        historyList.clear();

                        for (DataSnapshot d : snapshot.getChildren()) {
                            try {
                                String type = d.child("type").getValue(String.class);
                                if (!"leave".equals(type)) continue;

                                LeaveRequestModel item = d.getValue(LeaveRequestModel.class);
                                if (item != null) {
                                    item.messageId = d.getKey();
                                    historyList.add(item);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        // Newest first
                        Collections.sort(historyList,
                                (a, b) -> Long.compare(b.createdAt, a.createdAt));

                        if (txtNoLeaveHistory != null) {
                            txtNoLeaveHistory.setVisibility(
                                    historyList.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                        if (rvLeaveHistory != null) {
                            rvLeaveHistory.setVisibility(
                                    historyList.isEmpty() ? View.GONE : View.VISIBLE);
                        }

                        historyAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}