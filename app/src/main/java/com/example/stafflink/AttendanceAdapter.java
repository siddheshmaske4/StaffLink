package com.example.stafflink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.VH> {

    private final List<AttendanceModel> list;

    public AttendanceAdapter(List<AttendanceModel> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance_employee, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        AttendanceModel m = list.get(position);

        h.name.setText(m.getEmployeeName());
        h.status.setText("Status: " + (m.getStatus() != null ? m.getStatus() : "--"));
        h.checkIn.setText("In: " + (m.getCheckIn() != null ? m.getCheckIn() : "--"));
        h.checkOut.setText("Out: " + (m.getCheckOut() != null ? m.getCheckOut() : "--"));
        h.overtime.setText("Overtime: " + m.getOvertimeHours() + "h");
        h.leaves.setText("Leaves  •  Full: " + m.getFullDays() +
                "  Half: " + m.getHalfDays() +
                "  Paid: " + m.getPaidLeaves());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView name, status, checkIn, checkOut, overtime, leaves;

        VH(@NonNull View v) {
            super(v);
            name = v.findViewById(R.id.empName);
            status = v.findViewById(R.id.empStatus);
            checkIn = v.findViewById(R.id.empCheckIn);
            checkOut = v.findViewById(R.id.empCheckOut);
            overtime = v.findViewById(R.id.empOvertime);
            leaves = v.findViewById(R.id.empLeaves);
        }
    }
}
