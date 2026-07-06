package com.example.ywinked;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LawyerDashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Greeting Header
    private TextView tvLawyerWelcome, tvLawyerInitial;
    private ImageView ivLawyerProfilePic;

    // Badges & Dot
    private View viewBellDot;
    private TextView tvCountInboundBadge;

    // Navigation Screens
    private View layoutLawyerHome, layoutLawyerCalendar, layoutLawyerClients;

    // Bottom Navigation Bar
    private View navItemHome, navItemCalendar, navItemClients;
    private ImageView ivNavHome, ivNavCalendar, ivNavClients;
    private TextView tvNavHome, tvNavCalendar, tvNavClients;
    private int currentNav = 0; // 0 = Home, 1 = Calendar, 2 = Clients

    // Empty state cards, layouts, summaries & tabs
    private View cardPendingSummary, cardTodaySummary;
    private TextView tvPendingSummaryTitle, tvPendingSummarySubtitle;
    private TextView tvTodaySummaryTitle, tvTodaySummarySubtitle;
    private View sectionPendingRequests, sectionTodayAppointments;
    private TextView pillAllActivity, pillRecentCases, pillArchives;
    private View cardPendingRequests, cardTodaySchedule;
    private int currentTab = 0; // 0 = All Activity, 1 = Recent Cases, 2 = Archives

    // Data lists
    private List<Appointment> pendingList = new ArrayList<>();
    private List<Appointment> todayList = new ArrayList<>();
    private List<Appointment> historyList = new ArrayList<>();
    private List<Appointment> historyListFiltered = new ArrayList<>();
    private List<Appointment> fullCalendarList = new ArrayList<>();
    private List<ClientContact> clientsList = new ArrayList<>();

    // Recycler adapters
    private AgendaAdapter agendaAdapter; // keep for backward compatibility
    private HistoryAdapter historyAdapter;
    private CalendarListAdapter calendarListAdapter;
    private ClientsListAdapter clientsListAdapter;

    private String lawyerName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lawyer_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 1. Initialize Header Views
        tvLawyerWelcome = findViewById(R.id.tvLawyerWelcome);
        tvLawyerInitial = findViewById(R.id.tvLawyerInitial);
        viewBellDot = findViewById(R.id.viewBellDot);
        tvCountInboundBadge = findViewById(R.id.tvCountInboundBadge);

        // 2. Initialize Screen Containers
        layoutLawyerHome = findViewById(R.id.layoutLawyerHome);
        layoutLawyerCalendar = findViewById(R.id.layoutLawyerCalendar);
        layoutLawyerClients = findViewById(R.id.layoutLawyerClients);

        // 3. Initialize Bottom Nav Views
        navItemHome = findViewById(R.id.navItemHome);
        navItemCalendar = findViewById(R.id.navItemCalendar);
        navItemClients = findViewById(R.id.navItemClients);

        ivNavHome = findViewById(R.id.ivNavHome);
        ivNavCalendar = findViewById(R.id.ivNavCalendar);
        ivNavClients = findViewById(R.id.ivNavClients);

        tvNavHome = findViewById(R.id.tvNavHome);
        tvNavCalendar = findViewById(R.id.tvNavCalendar);
        tvNavClients = findViewById(R.id.tvNavClients);

        // Empty state cards, layouts, summaries & tabs
        cardTodaySummary = findViewById(R.id.cardTodaySummary);
        tvTodaySummaryTitle = findViewById(R.id.tvTodaySummaryTitle);
        tvTodaySummarySubtitle = findViewById(R.id.tvTodaySummarySubtitle);
        cardPendingSummary = findViewById(R.id.cardPendingSummary);
        tvPendingSummaryTitle = findViewById(R.id.tvPendingSummaryTitle);
        tvPendingSummarySubtitle = findViewById(R.id.tvPendingSummarySubtitle);
        sectionPendingRequests = findViewById(R.id.sectionPendingRequests);
        sectionTodayAppointments = findViewById(R.id.sectionTodayAppointments);
        pillAllActivity = findViewById(R.id.pillAllActivity);
        pillRecentCases = findViewById(R.id.pillRecentCases);
        pillArchives = findViewById(R.id.pillArchives);
        cardPendingRequests = findViewById(R.id.cardPendingRequests);
        cardTodaySchedule = findViewById(R.id.cardTodaySchedule);

        // 4. Setup Recyclers
        setupRecyclers();

        ivLawyerProfilePic = findViewById(R.id.ivLawyerProfilePic);

        // Session Information Setup
        loadProfileUi();

        // Click Actions
        findViewById(R.id.btnNotificationBell).setOnClickListener(v -> showNotificationsDialog());
        findViewById(R.id.lawyerProfileCard).setOnClickListener(v -> {
            Intent intent = new Intent(LawyerDashboardActivity.this, LawyerSettingsActivity.class);
            startActivity(intent);
        });
        findViewById(R.id.btnViewAllHistory).setOnClickListener(v -> selectNav(1)); // Redirect to calendar

        navItemHome.setOnClickListener(v -> selectNav(0));
        navItemCalendar.setOnClickListener(v -> selectNav(1));
        navItemClients.setOnClickListener(v -> selectNav(2));

        // Stats Cards & summary card clicks to open dedicated pending requests activity
        cardPendingRequests.setOnClickListener(v -> {
            Intent intent = new Intent(LawyerDashboardActivity.this, LawyerPendingRequestsActivity.class);
            startActivity(intent);
        });
        cardPendingSummary.setOnClickListener(v -> {
            Intent intent = new Intent(LawyerDashboardActivity.this, LawyerPendingRequestsActivity.class);
            startActivity(intent);
        });

        // Pill clicks
        pillAllActivity.setOnClickListener(v -> setTabFilter(0));
        pillRecentCases.setOnClickListener(v -> setTabFilter(1));
        pillArchives.setOnClickListener(v -> setTabFilter(2));

        // Start checking approval and load records
        checkApprovalStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfileUi();
    }

    private void loadProfileUi() {
        SharedPreferences sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        lawyerName = sharedPref.getString("name", "");
        tvLawyerWelcome.setText("Adv. " + lawyerName);

        java.io.File file = new java.io.File(getFilesDir(), "profile_picture.jpg");
        if (file.exists()) {
            ivLawyerProfilePic.setImageURI(android.net.Uri.fromFile(file));
            ivLawyerProfilePic.setVisibility(View.VISIBLE);
            tvLawyerInitial.setVisibility(View.GONE);
        } else {
            String base64Image = sharedPref.getString("profile_image_base64", "");
            if (!base64Image.isEmpty()) {
                try {
                    byte[] decodedBytes = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
                    android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    ivLawyerProfilePic.setImageBitmap(bitmap);
                    ivLawyerProfilePic.setVisibility(View.VISIBLE);
                    tvLawyerInitial.setVisibility(View.GONE);
                } catch (Exception e) {
                    e.printStackTrace();
                    ivLawyerProfilePic.setVisibility(View.GONE);
                    tvLawyerInitial.setVisibility(View.VISIBLE);
                }
            } else {
                ivLawyerProfilePic.setVisibility(View.GONE);
                tvLawyerInitial.setVisibility(View.VISIBLE);
                if (!lawyerName.isEmpty()) {
                    tvLawyerInitial.setText(String.valueOf(lawyerName.charAt(0)).toUpperCase());
                }
            }
        }
    }

    private String getFormattedWelcomeDate(Calendar c) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        String dateStr = sdf.format(c.getTime());
        // Append suffix (st, nd, rd, th)
        int day = c.get(Calendar.DAY_OF_MONTH);
        String suffix = "th";
        if (day >= 11 && day <= 13) {
            suffix = "th";
        } else {
            switch (day % 10) {
                case 1:  suffix = "st"; break;
                case 2:  suffix = "nd"; break;
                case 3:  suffix = "rd"; break;
                default: suffix = "th"; break;
            }
        }
        return dateStr.replace(" " + day + ",", " " + day + suffix + ",");
    }

    private void showProfileMenu(View anchor) {
        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, anchor);
        popup.getMenu().add("Profile Information");
        popup.getMenu().add("Sign Out");
        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Sign Out")) {
                logoutUser();
            } else {
                SharedPreferences sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
                String email = sharedPref.getString("email", "");
                
                new AlertDialog.Builder(this)
                        .setTitle("Lawyer Profile")
                        .setMessage("Name: Adv. " + lawyerName + "\nEmail: " + email + "\nRole: Registered Lawyer")
                        .setPositiveButton("OK", null)
                        .show();
            }
            return true;
        });
        popup.show();
    }

    private void logoutUser() {
        mAuth.signOut();

        SharedPreferences sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(LawyerDashboardActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void checkApprovalStatus() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            logoutUser();
            return;
        }

        db.collection("Users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Boolean approved = doc.getBoolean("approved");
                        if (approved != null && !approved) {
                            Intent intent = new Intent(LawyerDashboardActivity.this, WaitingApprovalActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            fetchAppointments();
                        }
                    } else {
                        logoutUser();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to verify approval: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    fetchAppointments();
                });
    }

    private void selectNav(int navIndex) {
        currentNav = navIndex;
        updateBottomNavUi(navIndex);

        layoutLawyerHome.setVisibility(navIndex == 0 ? View.VISIBLE : View.GONE);
        layoutLawyerCalendar.setVisibility(navIndex == 1 ? View.VISIBLE : View.GONE);
        layoutLawyerClients.setVisibility(navIndex == 2 ? View.VISIBLE : View.GONE);

        if (navIndex == 2) {
            computeClientsList();
        }
    }

    private void updateBottomNavUi(int selectedIndex) {
        int activeColor = Color.parseColor("#FF8C00");
        int inactiveColor = Color.parseColor("#8E8E9F");

        ivNavHome.setColorFilter(selectedIndex == 0 ? activeColor : inactiveColor);
        tvNavHome.setTextColor(selectedIndex == 0 ? activeColor : inactiveColor);

        ivNavCalendar.setColorFilter(selectedIndex == 1 ? activeColor : inactiveColor);
        tvNavCalendar.setTextColor(selectedIndex == 1 ? activeColor : inactiveColor);

        ivNavClients.setColorFilter(selectedIndex == 2 ? activeColor : inactiveColor);
        tvNavClients.setTextColor(selectedIndex == 2 ? activeColor : inactiveColor);
    }

    private void setupRecyclers() {
        // Home recyclers
        RecyclerView recyclerHistory = findViewById(R.id.recyclerHistory);
        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));

        historyAdapter = new HistoryAdapter(historyListFiltered);
        recyclerHistory.setAdapter(historyAdapter);

        // Screen recyclers
        RecyclerView recyclerCalendarList = findViewById(R.id.recyclerCalendarList);
        RecyclerView recyclerClientsList = findViewById(R.id.recyclerClientsList);

        recyclerCalendarList.setLayoutManager(new LinearLayoutManager(this));
        recyclerClientsList.setLayoutManager(new LinearLayoutManager(this));

        calendarListAdapter = new CalendarListAdapter(fullCalendarList);
        clientsListAdapter = new ClientsListAdapter(clientsList);

        recyclerCalendarList.setAdapter(calendarListAdapter);
        recyclerClientsList.setAdapter(clientsListAdapter);
    }

    private void setTabFilter(int tab) {
        currentTab = tab;
        int orangeColor = Color.parseColor("#FF8C00");

        if (tab == 0) { // All Activity
            pillAllActivity.setBackgroundResource(R.drawable.button_orange_border);
            pillAllActivity.setBackgroundTintList(null);
            pillAllActivity.setTextColor(Color.WHITE);

            pillRecentCases.setBackgroundResource(R.drawable.edittext_background);
            pillRecentCases.setBackgroundTintList(null);
            pillRecentCases.setTextColor(Color.parseColor("#8E8E9F"));

            pillArchives.setBackgroundResource(R.drawable.edittext_background);
            pillArchives.setBackgroundTintList(null);
            pillArchives.setTextColor(Color.parseColor("#8E8E9F"));

            // Filter layouts
            sectionPendingRequests.setVisibility(View.VISIBLE);
            sectionTodayAppointments.setVisibility(View.VISIBLE);

            historyListFiltered.clear();
            historyListFiltered.addAll(historyList);
            historyAdapter.notifyDataSetChanged();
        } else if (tab == 1) { // Recent Cases (Accepted)
            pillAllActivity.setBackgroundResource(R.drawable.edittext_background);
            pillAllActivity.setBackgroundTintList(null);
            pillAllActivity.setTextColor(Color.parseColor("#8E8E9F"));

            pillRecentCases.setBackgroundResource(R.drawable.button_orange_border);
            pillRecentCases.setBackgroundTintList(null);
            pillRecentCases.setTextColor(Color.WHITE);

            pillArchives.setBackgroundResource(R.drawable.edittext_background);
            pillArchives.setBackgroundTintList(null);
            pillArchives.setTextColor(Color.parseColor("#8E8E9F"));

            // Filter layouts
            sectionPendingRequests.setVisibility(View.GONE);
            sectionTodayAppointments.setVisibility(View.VISIBLE);

            historyListFiltered.clear();
            for (Appointment app : historyList) {
                if (app.status.equalsIgnoreCase("ACCEPTED")) {
                    historyListFiltered.add(app);
                }
            }
            historyAdapter.notifyDataSetChanged();
        } else { // Archives (Rejected/Cancelled)
            pillAllActivity.setBackgroundResource(R.drawable.edittext_background);
            pillAllActivity.setBackgroundTintList(null);
            pillAllActivity.setTextColor(Color.parseColor("#8E8E9F"));

            pillRecentCases.setBackgroundResource(R.drawable.edittext_background);
            pillRecentCases.setBackgroundTintList(null);
            pillRecentCases.setTextColor(Color.parseColor("#8E8E9F"));

            pillArchives.setBackgroundResource(R.drawable.button_orange_border);
            pillArchives.setBackgroundTintList(null);
            pillArchives.setTextColor(Color.WHITE);

            // Filter layouts
            sectionPendingRequests.setVisibility(View.GONE);
            sectionTodayAppointments.setVisibility(View.GONE);

            historyListFiltered.clear();
            for (Appointment app : historyList) {
                if (app.status.equalsIgnoreCase("REJECTED") || app.status.equalsIgnoreCase("CANCELLED")) {
                    historyListFiltered.add(app);
                }
            }
            historyAdapter.notifyDataSetChanged();
        }
    }

    private void fetchAppointments() {
        if (lawyerName.isEmpty()) return;

        String formattedLawyerName = "Adv. " + lawyerName;

        db.collection("Appointments")
                .whereEqualTo("lawyerName", formattedLawyerName)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(LawyerDashboardActivity.this, "Error fetching appointments: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        pendingList.clear();
                        todayList.clear();
                        historyList.clear();
                        fullCalendarList.clear();

                        Calendar c = Calendar.getInstance();
                        String todayDate = c.get(Calendar.DAY_OF_MONTH) + "-" + (c.get(Calendar.MONTH) + 1) + "-" + c.get(Calendar.YEAR);

                        for (DocumentSnapshot doc : value.getDocuments()) {
                            String id = doc.getId();
                            String userName = doc.getString("userName");
                            String userEmail = doc.getString("userEmail"); // Email might not exist, we'll map fallback
                            if (userEmail == null) userEmail = "client@gmail.com";
                            String date = doc.getString("date");
                            String time = doc.getString("time");
                            String specialization = doc.getString("specialization");
                            String status = doc.getString("status");
                            if (status == null) status = "PENDING";

                            Appointment app = new Appointment(id, userName, userEmail, date, time, specialization, status);
                            fullCalendarList.add(app);

                            if (status.equalsIgnoreCase("PENDING")) {
                                pendingList.add(app);
                            } else if (status.equalsIgnoreCase("ACCEPTED") && date != null && date.equals(todayDate)) {
                                todayList.add(app);
                            } else {
                                historyList.add(app);
                            }
                        }

                        // Toggle Pending Empty States & Summary
                        int pendingCount = pendingList.size();
                        if (pendingCount == 0) {
                            tvPendingSummaryTitle.setText("No pending appointment requests");
                            tvPendingSummarySubtitle.setText("You're all caught up!");
                            tvCountInboundBadge.setVisibility(View.GONE);
                        } else {
                            tvPendingSummaryTitle.setText(pendingCount + " Pending Requests");
                            tvPendingSummarySubtitle.setText("Tap to review and manage requests");
                            tvCountInboundBadge.setVisibility(View.VISIBLE);
                            tvCountInboundBadge.setText(pendingCount + " NEW");
                        }

                        // Toggle Today Empty States & Summary Click Actions ("keep it blank if there is no appointment today")
                        int todayCount = todayList.size();
                        if (todayCount == 0) {
                            tvTodaySummaryTitle.setText("No appointments scheduled for today");
                            tvTodaySummarySubtitle.setText("Enjoy your free time!");
                            
                            // Set inactive ("blank") click actions
                            cardTodaySummary.setOnClickListener(null);
                            cardTodaySchedule.setOnClickListener(null);
                        } else {
                            tvTodaySummaryTitle.setText(todayCount + " Appointments Today");
                            tvTodaySummarySubtitle.setText("Tap to view today's schedule");
                            
                            // Set click actions to open today's schedule activity
                            cardTodaySummary.setOnClickListener(v -> {
                                Intent intent = new Intent(LawyerDashboardActivity.this, LawyerTodayAppointmentsActivity.class);
                                startActivity(intent);
                            });
                            cardTodaySchedule.setOnClickListener(v -> {
                                Intent intent = new Intent(LawyerDashboardActivity.this, LawyerTodayAppointmentsActivity.class);
                                startActivity(intent);
                            });
                        }

                        // Notification alert bell
                        viewBellDot.setVisibility(pendingCount > 0 ? View.VISIBLE : View.GONE);

                        // Refresh lists
                        calendarListAdapter.notifyDataSetChanged();
                        setTabFilter(currentTab);
                        
                        // If clients screen is active, refresh clients too
                        if (currentNav == 2) {
                            computeClientsList();
                        }
                    }
                });
    }

    private void computeClientsList() {
        clientsList.clear();
        Map<String, ClientContact> contactMap = new HashMap<>();

        // Group unique clients from all historical appointments
        for (Appointment app : fullCalendarList) {
            String clientName = app.userName;
            String clientEmail = app.userEmail;
            if (clientName == null || clientName.isEmpty()) continue;

            if (contactMap.containsKey(clientName)) {
                ClientContact contact = contactMap.get(clientName);
                if (contact != null) {
                    contact.sessionCount += 1;
                }
            } else {
                contactMap.put(clientName, new ClientContact(clientName, clientEmail, 1));
            }
        }

        clientsList.addAll(contactMap.values());
        clientsListAdapter.notifyDataSetChanged();
    }

    private void acceptAppointment(String appointmentId) {
        db.collection("Appointments")
                .document(appointmentId)
                .update("status", "ACCEPTED")
                .addOnSuccessListener(aVoid -> Toast.makeText(LawyerDashboardActivity.this, "Request Accepted", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(LawyerDashboardActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void rejectAppointment(String appointmentId) {
        db.collection("Appointments")
                .document(appointmentId)
                .update("status", "REJECTED")
                .addOnSuccessListener(aVoid -> Toast.makeText(LawyerDashboardActivity.this, "Request Rejected", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(LawyerDashboardActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showNotificationsDialog() {
        int count = pendingList.size();
        if (count == 0) {
            Toast.makeText(this, "You have no pending requests.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Inbound Request Notifications")
                .setMessage("You have " + count + " new appointment requests awaiting review.")
                .setPositiveButton("Review Now", (dialog, which) -> {
                    selectNav(0); // Go home
                })
                .setNegativeButton("Dismiss", null)
                .show();
    }

    // --- Bottom Sheet Request Review Drawer ---

    private void showReviewSheet(Appointment app) {
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
        tvLabel.setTypeface(null, Typeface.BOLD);
        tvLabel.setLetterSpacing(0.05f);

        TextView tvValue = new TextView(this);
        tvValue.setText(value);
        tvValue.setTextColor(Color.parseColor("#FFFFFF"));
        tvValue.setTextSize(14);
        tvValue.setPadding(0, 4, 0, 0);

        row.addView(tvLabel);
        row.addView(tvValue);
        container.addView(row);
    }

    // --- Appointment Data Holder Class ---

    public static class Appointment {
        String id;
        String userName;
        String userEmail;
        String date;
        String time;
        String specialization;
        String status;

        public Appointment(String id, String userName, String userEmail, String date, String time, String specialization, String status) {
            this.id = id;
            this.userName = userName;
            this.userEmail = userEmail;
            this.date = date;
            this.time = time;
            this.specialization = specialization != null ? specialization : "General Consultation";
            this.status = status;
        }
    }

    // --- Client Contact Holder Class ---

    public static class ClientContact {
        String name;
        String email;
        int sessionCount;

        public ClientContact(String name, String email, int sessionCount) {
            this.name = name;
            this.email = email;
            this.sessionCount = sessionCount;
        }
    }

    // --- Recycler Adapters ---

    // 1. Today's Agenda Timeline Adapter
    private class AgendaAdapter extends RecyclerView.Adapter<AgendaAdapter.ViewHolder> {
        private final List<Appointment> list;

        public AgendaAdapter(List<Appointment> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lawyer_agenda, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            // First item is always mock placeholder to match reference mockup
            if (position == 0) {
                holder.cardActive.setVisibility(View.GONE);
                holder.cardPlaceholder.setVisibility(View.VISIBLE);
                holder.timelineDot.setBackgroundResource(R.drawable.timeline_dot_placeholder);
            } else {
                Appointment app = list.get(position - 1);
                holder.cardActive.setVisibility(View.VISIBLE);
                holder.cardPlaceholder.setVisibility(View.GONE);
                holder.timelineDot.setBackgroundResource(R.drawable.timeline_dot_accepted);

                holder.tvClientName.setText(app.userName);
                holder.tvTime.setText(app.time);
                holder.tvSpecialization.setText(app.specialization);
                holder.tvStatus.setText(app.status.toUpperCase());
            }

            // Hide vertical timeline extension line at the very end
            holder.timelineLine.setVisibility(position == getItemCount() - 1 ? View.INVISIBLE : View.VISIBLE);
        }

        @Override
        public int getItemCount() {
            // Placeholder item + actual list
            return 1 + list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            View timelineLine, timelineDot;
            View cardActive, cardPlaceholder;
            TextView tvClientName, tvTime, tvSpecialization, tvStatus;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                timelineLine = itemView.findViewById(R.id.timelineLine);
                timelineDot = itemView.findViewById(R.id.timelineDot);
                cardActive = itemView.findViewById(R.id.cardAgendaActive);
                cardPlaceholder = itemView.findViewById(R.id.cardAgendaPlaceholder);

                tvClientName = itemView.findViewById(R.id.tvAgendaClientName);
                tvTime = itemView.findViewById(R.id.tvAgendaTime);
                tvSpecialization = itemView.findViewById(R.id.tvAgendaSpecialization);
                tvStatus = itemView.findViewById(R.id.tvAgendaStatus);
            }
        }
    }



    // 3. History Adapter
    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private final List<Appointment> list;

        public HistoryAdapter(List<Appointment> list) {
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
            Appointment app = list.get(position);
            holder.tvClientName.setText("Client: " + app.userName);
            holder.tvDateTime.setText(app.date + " at " + app.time);
            holder.tvIssueType.setText("Issue: " + app.specialization);
            holder.tvStatusText.setText(app.status.toUpperCase());

            String initials = "C";
            if (app.userName != null && !app.userName.trim().isEmpty()) {
                initials = String.valueOf(app.userName.trim().charAt(0)).toUpperCase();
            }
            holder.tvAvatarText.setText(initials);

            if (app.status.equalsIgnoreCase("ACCEPTED")) {
                holder.tvStatusText.setTextColor(Color.parseColor("#30D158"));
                holder.tvStatusText.setBackgroundResource(R.drawable.badge_accepted);
            } else if (app.status.equalsIgnoreCase("PENDING")) {
                holder.tvStatusText.setTextColor(Color.parseColor("#FF9F0A"));
                holder.tvStatusText.setBackgroundResource(R.drawable.badge_pending);
            } else {
                holder.tvStatusText.setTextColor(Color.parseColor("#FF453A"));
                holder.tvStatusText.setBackgroundResource(R.drawable.badge_rejected);
            }
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

    // 4. Calendar Screen List Adapter
    private class CalendarListAdapter extends RecyclerView.Adapter<CalendarListAdapter.ViewHolder> {
        private final List<Appointment> list;

        public CalendarListAdapter(List<Appointment> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lawyer_appointment, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Appointment app = list.get(position);
            holder.tvClientName.setText("Client: " + app.userName);
            holder.tvDateTime.setText(app.date + " at " + app.time);
            holder.tvSpecialization.setText("Issue: " + app.specialization);
            holder.tvAppointmentStatus.setText(app.status.toUpperCase());

            if (app.userName != null && !app.userName.isEmpty()) {
                holder.tvClientAvatar.setText(String.valueOf(app.userName.charAt(0)).toUpperCase());
            } else {
                holder.tvClientAvatar.setText("C");
            }

            // Status Badge
            if (app.status.equalsIgnoreCase("PENDING")) {
                holder.tvAppointmentStatus.setTextColor(Color.parseColor("#FF8C00"));
                holder.tvAppointmentStatus.setBackgroundResource(R.drawable.badge_pending);
            } else if (app.status.equalsIgnoreCase("ACCEPTED")) {
                holder.tvAppointmentStatus.setTextColor(Color.parseColor("#30D158"));
                holder.tvAppointmentStatus.setBackgroundResource(R.drawable.badge_accepted);
            } else {
                holder.tvAppointmentStatus.setTextColor(Color.parseColor("#FF453A"));
                holder.tvAppointmentStatus.setBackgroundResource(R.drawable.badge_rejected);
            }

            // Quick actions inside general list
            if (app.status.equalsIgnoreCase("PENDING")) {
                holder.layoutActionButtons.setVisibility(View.VISIBLE);
                holder.btnAccept.setOnClickListener(v -> acceptAppointment(app.id));
                holder.btnReject.setOnClickListener(v -> rejectAppointment(app.id));
            } else {
                holder.layoutActionButtons.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvClientName, tvDateTime, tvSpecialization, tvAppointmentStatus, tvClientAvatar;
            View layoutActionButtons;
            MaterialButton btnAccept, btnReject;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvClientName = itemView.findViewById(R.id.tvClientName);
                tvDateTime = itemView.findViewById(R.id.tvDateTime);
                tvSpecialization = itemView.findViewById(R.id.tvSpecialization);
                tvAppointmentStatus = itemView.findViewById(R.id.tvAppointmentStatus);
                tvClientAvatar = itemView.findViewById(R.id.tvClientAvatar);
                layoutActionButtons = itemView.findViewById(R.id.layoutActionButtons);
                btnAccept = itemView.findViewById(R.id.btnAccept);
                btnReject = itemView.findViewById(R.id.btnReject);
            }
        }
    }

    // 5. Clients Directory Adapter
    private class ClientsListAdapter extends RecyclerView.Adapter<ClientsListAdapter.ViewHolder> {
        private final List<ClientContact> list;

        public ClientsListAdapter(List<ClientContact> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lawyer_client, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ClientContact client = list.get(position);
            holder.tvClientName.setText(client.name);
            holder.tvClientEmail.setText(client.email);
            holder.tvSessionCount.setText(client.sessionCount + (client.sessionCount == 1 ? " session" : " sessions"));

            if (!client.name.isEmpty()) {
                holder.tvAvatar.setText(String.valueOf(client.name.charAt(0)).toUpperCase());
            } else {
                holder.tvAvatar.setText("C");
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvAvatar, tvClientName, tvClientEmail, tvSessionCount;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvAvatar = itemView.findViewById(R.id.tvClientInitial);
                tvClientName = itemView.findViewById(R.id.tvClientName);
                tvClientEmail = itemView.findViewById(R.id.tvClientEmail);
                tvSessionCount = itemView.findViewById(R.id.tvClientSessionCount);
            }
        }
    }
}
