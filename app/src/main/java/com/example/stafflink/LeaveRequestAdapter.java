package com.example.stafflink;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * LeaveRequestAdapter — shared adapter for:
 *  - AdminLeaveRequestsActivity (showEmployee = true, shows who sent it)
 *  - MailFragment leave history list (showEmployee = false, it's the employee's own)
 *
 * Layout: item_leave_request.xml
 */
public class LeaveRequestAdapter extends RecyclerView.Adapter<LeaveRequestAdapter.VH> {

    private final Context context;
    private final List<LeaveRequestModel> list;
    private final boolean showEmployee;

    public LeaveRequestAdapter(Context context, List<LeaveRequestModel> list, boolean showEmployee) {
        this.context      = context;
        this.list         = list;
        this.showEmployee = showEmployee;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_leave_request, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        LeaveRequestModel item = list.get(position);

        holder.txtTitle.setText(item.title);
        holder.txtBody.setText(item.body);

        String leaveType = item.leaveType != null ? item.leaveType : "Full Day";
        holder.txtLeaveType.setText(leaveType);
        holder.txtLeaveType.setTextColor(
                "Half Day".equals(leaveType) ? Color.parseColor("#FF8C00") : Color.parseColor("#1E90FF")
        );

        String date = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                .format(new Date(item.createdAt));
        holder.txtDate.setText(date);

        if (showEmployee) {
            holder.txtEmployee.setVisibility(View.VISIBLE);
            holder.txtEmployee.setText(item.employeeEmail != null ? item.employeeEmail : "Unknown employee");
        } else {
            holder.txtEmployee.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView txtTitle, txtBody, txtLeaveType, txtDate, txtEmployee;

        VH(@NonNull View v) {
            super(v);
            txtTitle     = v.findViewById(R.id.txtLeaveTitle);
            txtBody      = v.findViewById(R.id.txtLeaveBody);
            txtLeaveType = v.findViewById(R.id.txtLeaveType);
            txtDate      = v.findViewById(R.id.txtLeaveDate);
            txtEmployee  = v.findViewById(R.id.txtLeaveEmployee);
        }
    }
}