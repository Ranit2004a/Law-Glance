package com.example.ywinked;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class LawyerPendingRequestsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private RecyclerView recyclerPendingRequestsList;
    private View layoutEmptyState;
    private List<LawyerDashboardActivity.Appointment> pendingList = new ArrayList<>();
    private PendingRequestsAdapter adapter;
    private String lawyerName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lawyer_pending_requests);

        db = FirebaseFirestore.getInstance();

        // Bind Back Button
        ImageView ivPendingBack = findViewById(R.id.ivPendingBack);
        ivPendingBack.setOnClickListener(v -> finish());

        // Bind Views
        recyclerPendingRequestsList = findViewById(R.id.recyclerPendingRequestsList);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);

        recyclerPendingRequestsList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PendingRequestsAdapter(pendingList);
        recyclerPendingRequestsList.setAdapter(adapter);

        // Load Session lawyerName
        SharedPreferences sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        lawyerName = sharedPref.getString("name", "");

        fetchPendingRequests();
    }

    private void fetchPendingRequests() {
        if (lawyerName.isEmpty()) return;

        String formattedLawyerName = "Adv. " + lawyerName;

        db.collection("Appointments")
                .whereEqualTo("lawyerName", formattedLawyerName)
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        pendingList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            String id = doc.getId();
                            String userName = doc.getString("userName");
                            String userEmail = doc.getString("userEmail");
                            if (userEmail == null) userEmail = "client@gmail.com";
                            String date = doc.getString("date");
                            String time = doc.getString("time");
                            String specialization = doc.getString("specialization");
                            String status = doc.getString("status");
                            if (status == null) status = "PENDING";

                            LawyerDashboardActivity.Appointment app = new LawyerDashboardActivity.Appointment(
                                    id, userName, userEmail, date, time, specialization, status
                            );
                            pendingList.add(app);
                        }

                        adapter.notifyDataSetChanged();

                        if (pendingList.isEmpty()) {
                            recyclerPendingRequestsList.setVisibility(View.GONE);
                            layoutEmptyState.setVisibility(View.VISIBLE);
                        } else {
                            recyclerPendingRequestsList.setVisibility(View.VISIBLE);
                            layoutEmptyState.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void acceptAppointment(String appointmentId) {
        db.collection("Appointments")
                .document(appointmentId)
                .update("status", "ACCEPTED")
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Request Accepted", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void rejectAppointment(String appointmentId) {
        db.collection("Appointments")
                .document(appointmentId)
                .update("status", "REJECTED")
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Request Rejected", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showReviewSheet(LawyerDashboardActivity.Appointment app) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_lawyer_request_review, null);

        LinearLayout layoutFields = view.findViewById(R.id.layoutReviewFields);
        MaterialButton btnAccept = view.findViewById(R.id.btnReviewAccept);
        MaterialButton btnReject = view.findViewById(R.id.btnReviewReject);

        layoutFields.removeAllViews();

        addDetailField(layoutFields, "Client Full Name", app.userName);
        addDetailField(layoutFields, "Client Email", app.userEmail);
        addDetailField(layoutFields, "Requested Specialization", app.specialization);
        addDetailField(layoutFields, "Appointment Date", app.date);
        addDetailField(layoutFields, "Appointment Time", app.time);
        addDetailField(layoutFields, "Booking ID", app.id);

        btnAccept.setOnClickListener(v -> {
            dialog.dismiss();
            acceptAppointment(app.id);
        });

        btnReject.setOnClickListener(v -> {
            dialog.dismiss();
            rejectAppointment(app.id);
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private void addDetailField(LinearLayout container, String label, String value) {
        if (value == null || value.trim().isEmpty()) return;

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, 0, 0, 16);

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label.toUpperCase());
        tvLabel.setTextColor(Color.parseColor("#8E8E9F"));
        tvLabel.setTextSize(10);
        tvLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        tvLabel.setLetterSpacing(0.05f);

        TextView tvValue = new TextView(this);
        tvValue.setText(value);
        tvValue.setTextColor(Color.parseColor("#FFFFFF"));
        tvValue.setTextSize(15);
        tvValue.setTypeface(null, android.graphics.Typeface.BOLD);
        tvValue.setPadding(0, 4, 0, 0);

        row.addView(tvLabel);
        row.addView(tvValue);
        container.addView(row);
    }

    // RecyclerView Adapter
    private class PendingRequestsAdapter extends RecyclerView.Adapter<PendingRequestsAdapter.ViewHolder> {
        private final List<LawyerDashboardActivity.Appointment> list;

        public PendingRequestsAdapter(List<LawyerDashboardActivity.Appointment> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lawyer_inbound, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            LawyerDashboardActivity.Appointment app = list.get(position);
            holder.tvClientName.setText(app.userName);
            holder.tvDetails.setText(app.specialization);

            String initials = "C";
            if (app.userName != null && !app.userName.trim().isEmpty()) {
                String[] parts = app.userName.trim().split("\\s+");
                if (parts.length > 1) {
                    initials = (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
                } else if (parts[0].length() > 0) {
                    initials = parts[0].substring(0, 1).toUpperCase();
                }
            }
            holder.tvAvatarText.setText(initials);

            holder.btnReview.setOnClickListener(v -> showReviewSheet(app));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvClientName, tvDetails, tvAvatarText;
            MaterialButton btnReview;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvClientName = itemView.findViewById(R.id.tvInboundClientName);
                tvDetails = itemView.findViewById(R.id.tvInboundDetails);
                tvAvatarText = itemView.findViewById(R.id.tvInboundAvatarText);
                btnReview = itemView.findViewById(R.id.btnInboundReview);
            }
        }
    }
}
