package com.example.ywinked;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class LawyerTodayAppointmentsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private RecyclerView recyclerTodayAppointmentsList;
    private List<LawyerDashboardActivity.Appointment> todayList = new ArrayList<>();
    private TodayAppointmentsAdapter adapter;
    private String lawyerName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lawyer_today_appointments);

        db = FirebaseFirestore.getInstance();

        // Bind Back Button
        ImageView ivTodayBack = findViewById(R.id.ivTodayBack);
        ivTodayBack.setOnClickListener(v -> finish());

        // Bind Views
        recyclerTodayAppointmentsList = findViewById(R.id.recyclerTodayAppointmentsList);
        recyclerTodayAppointmentsList.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TodayAppointmentsAdapter(todayList);
        recyclerTodayAppointmentsList.setAdapter(adapter);

        // Load Session lawyerName
        SharedPreferences sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        lawyerName = sharedPref.getString("name", "");

        fetchTodayAppointments();
    }

    private void fetchTodayAppointments() {
        if (lawyerName.isEmpty()) return;

        String formattedLawyerName = "Adv. " + lawyerName;

        db.collection("Appointments")
                .whereEqualTo("lawyerName", formattedLawyerName)
                .whereEqualTo("status", "ACCEPTED")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        todayList.clear();
                        Calendar c = Calendar.getInstance();
                        String todayDate = c.get(Calendar.DAY_OF_MONTH) + "-" + (c.get(Calendar.MONTH) + 1) + "-" + c.get(Calendar.YEAR);

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

                            if (date != null && date.equals(todayDate)) {
                                LawyerDashboardActivity.Appointment app = new LawyerDashboardActivity.Appointment(
                                        id, userName, userEmail, date, time, specialization, status
                                );
                                todayList.add(app);
                            }
                        }

                        adapter.notifyDataSetChanged();

                        if (todayList.isEmpty()) {
                            // If empty (e.g. cancelled/modified by user while open), just exit
                            finish();
                        }
                    }
                });
    }

    // RecyclerView Adapter replicating HistoryAdapter layout style
    private class TodayAppointmentsAdapter extends RecyclerView.Adapter<TodayAppointmentsAdapter.ViewHolder> {
        private final List<LawyerDashboardActivity.Appointment> list;

        public TodayAppointmentsAdapter(List<LawyerDashboardActivity.Appointment> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lawyer_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            LawyerDashboardActivity.Appointment app = list.get(position);
            holder.tvClientName.setText("Client: " + app.userName);
            holder.tvDateTime.setText(app.date + " at " + app.time);
            holder.tvIssueType.setText("Issue: " + app.specialization);
            holder.tvStatusText.setText(app.status.toUpperCase());

            String initials = "C";
            if (app.userName != null && !app.userName.trim().isEmpty()) {
                initials = String.valueOf(app.userName.trim().charAt(0)).toUpperCase();
            }
            holder.tvAvatarText.setText(initials);

            holder.tvStatusText.setTextColor(Color.parseColor("#30D158"));
            holder.tvStatusText.setBackgroundResource(R.drawable.badge_accepted);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvClientName, tvStatusText, tvDateTime, tvIssueType, tvAvatarText;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvClientName = itemView.findViewById(R.id.tvHistoryClientName);
                tvStatusText = itemView.findViewById(R.id.tvHistoryStatusText);
                tvDateTime = itemView.findViewById(R.id.tvHistoryDateTime);
                tvIssueType = itemView.findViewById(R.id.tvHistoryIssueType);
                tvAvatarText = itemView.findViewById(R.id.tvHistoryAvatarText);
            }
        }
    }
}
