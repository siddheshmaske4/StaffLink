package com.example.stafflink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stafflink.R;
import com.example.stafflink.EmployeeModel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EmployeeSelectAdapter
        extends RecyclerView.Adapter<EmployeeSelectAdapter.VH> {

    private final List<EmployeeModel> list;
    private final Set<String> selectedIds = new HashSet<>();

    public EmployeeSelectAdapter(List<EmployeeModel> list) {
        this.list = list;
    }

    public Set<String> getSelectedIds() {
        return selectedIds;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_employee_select, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        EmployeeModel employee = list.get(position);

        holder.name.setText(employee.getName());
        holder.email.setText(employee.getEmail());

        // 🔒 Disable checkbox if email missing
        boolean hasEmail = employee.getEmail() != null && !employee.getEmail().isEmpty();
        holder.checkBox.setEnabled(hasEmail);
        holder.checkBox.setVisibility(hasEmail ? View.VISIBLE : View.INVISIBLE);

        // 🔁 Fix RecyclerView checkbox reuse issue
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(selectedIds.contains(employee.getId()));

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedIds.add(employee.getId());
            } else {
                selectedIds.remove(employee.getId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {

        TextView name, email;
        CheckBox checkBox;

        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.txtName);
            email = itemView.findViewById(R.id.txtEmail);
            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }
}
