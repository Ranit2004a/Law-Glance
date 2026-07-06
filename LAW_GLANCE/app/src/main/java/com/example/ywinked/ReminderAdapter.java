package com.example.ywinked;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ViewHolder> {

    public interface OnDeleteClickListener {
        void onDeleteClick(ReminderModel reminder);
    }

    private final List<ReminderModel> list;
    private final OnDeleteClickListener listener;

    public ReminderAdapter(List<ReminderModel> list, OnDeleteClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reminder, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReminderModel reminder = list.get(position);
        holder.txtCaseTitle.setText(reminder.caseTitle);
        holder.txtCourtName.setText("Court: " + reminder.courtName);
        holder.txtDateTime.setText("Hearing: " + reminder.hearingDate + " at " + reminder.hearingTime);
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(reminder));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtCaseTitle, txtCourtName, txtDateTime;
        Button btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtCaseTitle = itemView.findViewById(R.id.txtCaseTitle);
            txtCourtName = itemView.findViewById(R.id.txtCourtName);
            txtDateTime = itemView.findViewById(R.id.txtDateTime);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
