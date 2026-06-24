package com.example.stafflink;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/**
 * InboxAdapter — simple mail list.
 * Tapping a card shows a popup dialog with the full mail content.
 * (Task-specific UI removed — tasks now live in the separate Task module.)
 */
public class InboxAdapter extends RecyclerView.Adapter<InboxAdapter.VH> {

    private final List<MessageModel> list;
    private final Context context;

    public InboxAdapter(Context context, List<MessageModel> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_inbox, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        MessageModel m = list.get(position);


        holder.title.setText(m.title);

        // Body preview (first 80 chars)
        String preview = m.body != null && m.body.length() > 80
                ? m.body.substring(0, 80) + "…"
                : m.body;

        // Show recipients on separate line from body
        if (m.recipients != null && !m.recipients.isEmpty()) {
            holder.body.setText("👤 " + m.recipients);
            holder.body.setTextColor(Color.parseColor("#1E90FF"));
            holder.body.setTextSize(12f);
            holder.txtPreview.setVisibility(View.VISIBLE);
            holder.txtPreview.setText(preview);
        } else {
            holder.body.setText(preview);
            holder.body.setTextColor(Color.parseColor("#555555"));
            holder.body.setTextSize(14f);
            holder.txtPreview.setVisibility(View.GONE);
        }
        holder.time.setText(DateFormat.getDateTimeInstance().format(new Date(m.createdAt)));

        holder.title.setTypeface(null, m.isRead ? Typeface.NORMAL : Typeface.BOLD);

        // Tap → show full mail in a dialog
        holder.itemView.setOnClickListener(v -> showMailDialog(m));


    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private void showMailDialog(MessageModel m) {
        StringBuilder msg = new StringBuilder();

        if (m.sender != null) {
            String senderLabel = m.sender.name != null ? m.sender.name
                    : (m.sender.role != null ? m.sender.role : "Unknown");
            msg.append("From: ").append(senderLabel).append("\n\n");
        }

        msg.append(m.body != null ? m.body : "");
        msg.append("\n\n");
        msg.append(DateFormat.getDateTimeInstance().format(new Date(m.createdAt)));

        new AlertDialog.Builder(context)
                .setTitle(m.title)
                .setMessage(msg.toString())
                .setPositiveButton("Close", null)
                .show();

        // Mark as read
        m.isRead = true;
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, body, time, txtPreview;

        VH(@NonNull View v) {
            super(v);
            title = v.findViewById(R.id.txtTitle);
            body  = v.findViewById(R.id.txtBody);
            time  = v.findViewById(R.id.txtTime);
            txtPreview = v.findViewById(R.id.txtPreview);
        }
    }
}