package com.example.stafflink;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.*;

public class EmployeeDetailActivity extends Fragment {

    private TextView tvInitial, tvName, tvPosition,
            tvEmail, tvPhone, tvDepartment, tvStatus, tvBaseSalary;

    private ImageView btnSettings; // 🔥 NEW

    private String empId, companyCode;
    private DatabaseReference profileRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_employee_detail, container, false);

        bindViews(view);
        loadSession();
        setupClickListeners(); // 🔥 NEW

        if (empId == null || companyCode == null) {
            Toast.makeText(getContext(), "Session error", Toast.LENGTH_SHORT).show();
            return view;
        }

        profileRef = FirebaseDatabase.getInstance()
                .getReference("companies")
                .child(companyCode)
                .child("employees")
                .child(empId)
                .child("profile");

        fetchProfile();

        return view;
    }

    private void bindViews(View v) {

        tvInitial = v.findViewById(R.id.tvInitial);
        tvName = v.findViewById(R.id.tvName);
        tvPosition = v.findViewById(R.id.tvPosition);

        tvEmail = v.findViewById(R.id.tvEmail);
        tvPhone = v.findViewById(R.id.tvPhone);
        tvDepartment = v.findViewById(R.id.tvDepartment);
        tvStatus = v.findViewById(R.id.tvStatus);

        tvBaseSalary = v.findViewById(R.id.tvBaseSalary);

        // 🔥 NEW (Settings button)
        btnSettings = v.findViewById(R.id.btnSettings);
    }

    private void setupClickListeners() {

        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                if (getActivity() != null) {
                    Intent intent = new Intent(getActivity(), SettingActivity.class);
                    startActivity(intent);
                }
            });
        }
    }

    private void loadSession() {

        empId = requireActivity()
                .getSharedPreferences("StafflinkPrefs", getContext().MODE_PRIVATE)
                .getString("emp_id", null);

        companyCode = requireActivity()
                .getSharedPreferences("StafflinkPrefs", getContext().MODE_PRIVATE)
                .getString("company_code", null);
    }

    private void fetchProfile() {

        profileRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {

                if (!s.exists()) return;

                String name = get(s,"name");
                String email = get(s,"email");
                String phone = get(s,"phone");
                String department = get(s,"department");
                String position = get(s,"position");
                String status = get(s,"status");

                Long salary = s.child("baseSalary").getValue(Long.class);

                tvName.setText(name);
                tvPosition.setText(position);
                tvEmail.setText("Email: " + email);
                tvPhone.setText("Phone: " + phone);
                tvDepartment.setText("Department: " + department);
                tvStatus.setText("Status: " + status);

                tvBaseSalary.setText("₹ " + (salary != null ? salary : 0));

                if (name != null && !name.isEmpty())
                    tvInitial.setText(name.substring(0,1).toUpperCase());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String get(DataSnapshot s, String k) {
        String v = s.child(k).getValue(String.class);
        return v != null ? v : "-";
    }
}