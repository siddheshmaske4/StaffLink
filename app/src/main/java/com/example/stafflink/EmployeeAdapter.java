package com.example.stafflink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder> {

    private List<EmployeeModel> employeeList;
    private String companyCode; // needed to save salary to correct Firebase path

    public EmployeeAdapter(List<EmployeeModel> employeeList) {
        this.employeeList = employeeList;
    }

    /** Call this from EmployeeManagementActivity after fetching employees */
    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    @NonNull
    @Override
    public EmployeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_employee_card, parent, false);
        return new EmployeeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmployeeViewHolder holder, int position) {
        EmployeeModel e = employeeList.get(position);

        holder.tvName.setText(e.getName());
        holder.tvRole.setText(
                e.getPosition() + (e.getDepartment().isEmpty() ? "" : " • " + e.getDepartment())
        );
        holder.tvSalary.setText("₹" + e.getBaseSalary());
        holder.tvAvatar.setText(
                e.getName().isEmpty() ? "?" : e.getName().substring(0, 1).toUpperCase()
        );

        holder.tvEmail.setText("Email: " + e.getEmail());
        holder.tvPhone.setText("Phone: " + e.getPhone());
        holder.tvPosition.setText("Position: " + e.getPosition());
        holder.tvDepartment.setText("Department: " + e.getDepartment());
        holder.tvBaseSalary.setText("Base Salary: ₹" + e.getBaseSalary());
        holder.tvFinalSalary.setText("Final Salary: ₹" + e.getFinalSalary());
        holder.tvDeductions.setText("Deductions: ₹" + e.getDeductions());
        holder.tvOvertime.setText("Overtime: ₹" + e.getOvertime());
        holder.tvHalfDaysUsed.setText("Half Days: " + e.getHalfDaysUsed());
        holder.tvFullDaysUsed.setText("Full Days: " + e.getFullDaysUsed());
        holder.tvPaidLeavesUsed.setText("Paid Leaves: " + e.getPaidLeavesUsed());

        // Pre-fill salary field with current value
        if (e.getBaseSalary() > 0) {
            holder.etBaseSalary.setText(String.valueOf(e.getBaseSalary()));
        } else {
            holder.etBaseSalary.setText("");
        }

        // Expand / collapse
        holder.expandableLayout.setVisibility(e.isExpanded() ? View.VISIBLE : View.GONE);
        holder.btnExpand.setRotation(e.isExpanded() ? 180f : 0f);

        holder.btnExpand.setOnClickListener(v -> {
            e.setExpanded(!e.isExpanded());
            notifyItemChanged(position);
        });

        // ── Save salary button ────────────────────────────────────────────────
        holder.btnSetSalary.setOnClickListener(v -> {
            String salaryStr = holder.etBaseSalary.getText().toString().trim();

            if (salaryStr.isEmpty()) {
                Toast.makeText(v.getContext(), "Enter a salary amount", Toast.LENGTH_SHORT).show();
                return;
            }

            long salary;
            try {
                salary = Long.parseLong(salaryStr);
            } catch (NumberFormatException ex) {
                Toast.makeText(v.getContext(), "Invalid salary amount", Toast.LENGTH_SHORT).show();
                return;
            }

            if (salary <= 0) {
                Toast.makeText(v.getContext(), "Salary must be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }

            if (companyCode == null || companyCode.isEmpty()) {
                Toast.makeText(v.getContext(), "Session error", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save to Firebase: companies/{companyCode}/employees/{empId}/profile/baseSalary
            FirebaseDatabase.getInstance()
                    .getReference("companies")
                    .child(companyCode)
                    .child("employees")
                    .child(e.getId())
                    .child("profile")
                    .child("baseSalary")
                    .setValue(salary)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(v.getContext(),
                                "Salary saved for " + e.getName() + " ✓",
                                Toast.LENGTH_SHORT).show();
                        // Update model so card shows new salary immediately
                        holder.tvBaseSalary.setText("Base Salary: ₹" + salary);
                        holder.tvSalary.setText("₹" + salary);
                    })
                    .addOnFailureListener(ex ->
                            Toast.makeText(v.getContext(),
                                    "Failed to save salary", Toast.LENGTH_SHORT).show());
        });
    }

    @Override
    public int getItemCount() {
        return employeeList.size();
    }

    public void updateList(List<EmployeeModel> newList) {
        employeeList = newList;
        notifyDataSetChanged();
    }

    static class EmployeeViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvRole, tvAvatar, tvSalary;
        TextView tvEmail, tvPhone, tvPosition, tvDepartment;
        TextView tvBaseSalary, tvFinalSalary, tvDeductions, tvOvertime;
        TextView tvHalfDaysUsed, tvFullDaysUsed, tvPaidLeavesUsed;
        EditText etBaseSalary;
        Button   btnSetSalary;
        ImageButton btnExpand;
        LinearLayout expandableLayout;

        EmployeeViewHolder(@NonNull View itemView) {
            super(itemView);

            tvName     = itemView.findViewById(R.id.tvEmployeeName);
            tvRole     = itemView.findViewById(R.id.tvEmployeeRole);
            tvAvatar   = itemView.findViewById(R.id.tvAvatar);
            tvSalary   = itemView.findViewById(R.id.tvEmployeeSalary);

            tvEmail    = itemView.findViewById(R.id.tvEmployeeEmail);
            tvPhone    = itemView.findViewById(R.id.tvEmployeePhone);
            tvPosition = itemView.findViewById(R.id.tvEmployeePosition);
            tvDepartment  = itemView.findViewById(R.id.tvEmployeeDepartment);
            tvBaseSalary  = itemView.findViewById(R.id.tvEmployeeBaseSalary);
            tvFinalSalary = itemView.findViewById(R.id.tvEmployeeFinalSalary);
            tvDeductions  = itemView.findViewById(R.id.tvEmployeeDeductions);
            tvOvertime    = itemView.findViewById(R.id.tvEmployeeOvertime);
            tvHalfDaysUsed   = itemView.findViewById(R.id.tvEmployeeHalfDays);
            tvFullDaysUsed   = itemView.findViewById(R.id.tvEmployeeFullDays);
            tvPaidLeavesUsed = itemView.findViewById(R.id.tvEmployeePaidLeaves);

            etBaseSalary = itemView.findViewById(R.id.etBaseSalary);
            btnSetSalary = itemView.findViewById(R.id.btnSetSalary);
            btnExpand    = itemView.findViewById(R.id.btnExpand);
            expandableLayout = itemView.findViewById(R.id.expandableLayout);
        }
    }
}