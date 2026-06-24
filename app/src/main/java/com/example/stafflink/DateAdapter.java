package com.example.stafflink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DateAdapter extends RecyclerView.Adapter<DateAdapter.DateVH> {

    private final List<DateAttendanceModel> dateList;

    public DateAdapter(List<DateAttendanceModel> dateList) {
        this.dateList = dateList;
    }

    @NonNull
    @Override
    public DateVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.date_item, parent, false);
        return new DateVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull DateVH holder, int position) {
        DateAttendanceModel dateItem = dateList.get(position);

        holder.tvDate.setText(dateItem.getDate());

        // Setup nested RecyclerView
        AttendanceAdapter empAdapter = new AttendanceAdapter(dateItem.getEmployees());
        holder.recyclerEmployees.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
        holder.recyclerEmployees.setAdapter(empAdapter);

        // Expand/Collapse logic
        holder.btnExpand.setOnClickListener(v -> {
            if (holder.recyclerEmployees.getVisibility() == View.GONE) {
                holder.recyclerEmployees.setVisibility(View.VISIBLE);
                holder.btnExpand.setRotation(180); // arrow down
            } else {
                holder.recyclerEmployees.setVisibility(View.GONE);
                holder.btnExpand.setRotation(0); // arrow right
            }
        });

        // Initially collapsed
        holder.recyclerEmployees.setVisibility(View.GONE);
        holder.btnExpand.setRotation(0);
    }

    @Override
    public int getItemCount() {
        return dateList.size();
    }

    static class DateVH extends RecyclerView.ViewHolder {
        TextView tvDate;
        ImageButton btnExpand;
        RecyclerView recyclerEmployees;

        DateVH(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            btnExpand = itemView.findViewById(R.id.btnExpandDate);
            recyclerEmployees = itemView.findViewById(R.id.recyclerEmployees);
        }
    }
}
