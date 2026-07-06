package com.example.ywinked;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Stats
    private View layoutStats;
    private TextView tvCountUsers, tvCountLawyers, tvCountAppointments;
    private View viewBellDot;
    private int pendingLawyersCount = 0;
    private int pendingAppointmentsCount = 0;

    private ImageView ivAdminProfilePic;
    private TextView tvAdminInitial;

    // Tabs
    private MaterialButton btnTabLawyers, btnTabUsers, btnTabAppointments;
    private int currentTab = 0; // 0 = Lawyers, 1 = Clients, 2 = Bookings

    // Search and Filter
    private EditText etSearch;
    private View btnFilter;

    // Empty State
    private View layoutEmptyState;
    private ImageView ivEmptyStateIcon;
    private TextView tvEmptyStateText;

    // Bottom Navigation
    private View navItemDashboard, navItemLawyers, navItemClients, navItemBookings;
    private ImageView ivNavDashboard, ivNavLawyers, ivNavClients, ivNavBookings;
    private TextView tvNavDashboard, tvNavLawyers, tvNavClients, tvNavBookings;
    private int currentNav = 0; // 0 = Dashboard, 1 = Lawyers, 2 = Clients, 3 = Bookings

    // Recycler List
    private RecyclerView recyclerAdminContent;
    private List<AdminItem> masterList = new ArrayList<>();
    private List<AdminItem> displayedList = new ArrayList<>();
    private AdminAdapter adapter;

    // Filters state
    private String filterLawyerStatus = "ALL"; // "ALL", "APPROVED", "PENDING"
    private String filterBookingStatus = "ALL"; // "ALL", "PENDING", "ACCEPTED", "REJECTED"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 1. Initialize Views
        layoutStats = findViewById(R.id.layoutStats);
        tvCountUsers = findViewById(R.id.tvCountUsers);
        tvCountLawyers = findViewById(R.id.tvCountLawyers);
        tvCountAppointments = findViewById(R.id.tvCountAppointments);

        btnTabLawyers = findViewById(R.id.btnTabLawyers);
        btnTabUsers = findViewById(R.id.btnTabUsers);
        btnTabAppointments = findViewById(R.id.btnTabAppointments);

        etSearch = findViewById(R.id.etSearch);
        btnFilter = findViewById(R.id.btnFilter);

        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        ivEmptyStateIcon = findViewById(R.id.ivEmptyStateIcon);
        tvEmptyStateText = findViewById(R.id.tvEmptyStateText);

        recyclerAdminContent = findViewById(R.id.recyclerAdminContent);
        recyclerAdminContent.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AdminAdapter(displayedList);
        recyclerAdminContent.setAdapter(adapter);

        // Top bar actions
        viewBellDot = findViewById(R.id.viewBellDot);
        View btnNotificationBell = findViewById(R.id.btnNotificationBell);
        btnNotificationBell.setOnClickListener(v -> showNotificationsDialog());

        // Setup notification listeners
        startNotificationListeners();

        View btnAdminAvatar = findViewById(R.id.btnAdminAvatar);
        tvAdminInitial = findViewById(R.id.tvAdminInitial);
        ivAdminProfilePic = findViewById(R.id.ivAdminProfilePic);
        
        loadProfileUi();

        btnAdminAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AdminSettingsActivity.class);
            startActivity(intent);
        });

        // Bottom Navigation Views
        navItemDashboard = findViewById(R.id.navItemDashboard);
        navItemLawyers = findViewById(R.id.navItemLawyers);
        navItemClients = findViewById(R.id.navItemClients);
        navItemBookings = findViewById(R.id.navItemBookings);

        ivNavDashboard = findViewById(R.id.ivNavDashboard);
        ivNavLawyers = findViewById(R.id.ivNavLawyers);
        ivNavClients = findViewById(R.id.ivNavClients);
        ivNavBookings = findViewById(R.id.ivNavBookings);

        tvNavDashboard = findViewById(R.id.tvNavDashboard);
        tvNavLawyers = findViewById(R.id.tvNavLawyers);
        tvNavClients = findViewById(R.id.tvNavClients);
        tvNavBookings = findViewById(R.id.tvNavBookings);

        // 2. Setup Actions / Listeners
        btnTabLawyers.setOnClickListener(v -> selectTab(0));
        btnTabUsers.setOnClickListener(v -> selectTab(1));
        btnTabAppointments.setOnClickListener(v -> selectTab(2));

        navItemDashboard.setOnClickListener(v -> selectNav(0));
        navItemLawyers.setOnClickListener(v -> selectNav(1));
        navItemClients.setOnClickListener(v -> selectNav(2));
        navItemBookings.setOnClickListener(v -> selectNav(3));

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterAndDisplayList();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnFilter.setOnClickListener(v -> showFilterDialog());

        // 3. Load stats
        fetchStatsCounters();

        // 4. Initial Selection
        selectNav(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfileUi();
    }

    private void loadProfileUi() {
        SharedPreferences sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String name = sharedPref.getString("name", "Admin");

        java.io.File file = new java.io.File(getFilesDir(), "admin_profile_picture.jpg");
        if (file.exists()) {
            ivAdminProfilePic.setImageURI(android.net.Uri.fromFile(file));
            ivAdminProfilePic.setVisibility(View.VISIBLE);
            tvAdminInitial.setVisibility(View.GONE);
        } else {
            String base64Image = sharedPref.getString("profile_image_base64", "");
            if (!base64Image.isEmpty()) {
                try {
                    byte[] decodedBytes = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
                    android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    ivAdminProfilePic.setImageBitmap(bitmap);
                    ivAdminProfilePic.setVisibility(View.VISIBLE);
                    tvAdminInitial.setVisibility(View.GONE);
                } catch (Exception e) {
                    e.printStackTrace();
                    ivAdminProfilePic.setVisibility(View.GONE);
                    tvAdminInitial.setVisibility(View.VISIBLE);
                }
            } else {
                ivAdminProfilePic.setVisibility(View.GONE);
                tvAdminInitial.setVisibility(View.VISIBLE);
                if (!name.isEmpty()) {
                    tvAdminInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
                }
            }
        }
    }

    private void logoutUser() {
        mAuth.signOut();

        SharedPreferences sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void selectNav(int navIndex) {
        currentNav = navIndex;
        updateBottomNavUi(navIndex);

        if (navIndex == 0) {
            // Dashboard Mode
            layoutStats.setVisibility(View.VISIBLE);
            findViewById(R.id.layoutTabs).setVisibility(View.VISIBLE);
            selectTab(currentTab); // refresh selected tab content
        } else if (navIndex == 1) {
            // Lawyers list Mode
            layoutStats.setVisibility(View.GONE);
            findViewById(R.id.layoutTabs).setVisibility(View.GONE);
            selectTab(0);
        } else if (navIndex == 2) {
            // Clients list Mode
            layoutStats.setVisibility(View.GONE);
            findViewById(R.id.layoutTabs).setVisibility(View.GONE);
            selectTab(1);
        } else if (navIndex == 3) {
            // Bookings list Mode
            layoutStats.setVisibility(View.GONE);
            findViewById(R.id.layoutTabs).setVisibility(View.GONE);
            selectTab(2);
        }
    }

    private void updateBottomNavUi(int selectedIndex) {
        int activeColor = Color.parseColor("#FF8C00");
        int inactiveColor = Color.parseColor("#8E8E9F");

        ivNavDashboard.setColorFilter(selectedIndex == 0 ? activeColor : inactiveColor);
        tvNavDashboard.setTextColor(selectedIndex == 0 ? activeColor : inactiveColor);

        ivNavLawyers.setColorFilter(selectedIndex == 1 ? activeColor : inactiveColor);
        tvNavLawyers.setTextColor(selectedIndex == 1 ? activeColor : inactiveColor);

        ivNavClients.setColorFilter(selectedIndex == 2 ? activeColor : inactiveColor);
        tvNavClients.setTextColor(selectedIndex == 2 ? activeColor : inactiveColor);

        ivNavBookings.setColorFilter(selectedIndex == 3 ? activeColor : inactiveColor);
        tvNavBookings.setTextColor(selectedIndex == 3 ? activeColor : inactiveColor);
    }

    private void selectTab(int tabIndex) {
        currentTab = tabIndex;

        // Visual feedback on tabs
        btnTabLawyers.setTextColor(tabIndex == 0 ? Color.parseColor("#FFFFFF") : Color.parseColor("#8E8E9F"));
        btnTabLawyers.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                tabIndex == 0 ? Color.parseColor("#FF8C00") : Color.TRANSPARENT));

        btnTabUsers.setTextColor(tabIndex == 1 ? Color.parseColor("#FFFFFF") : Color.parseColor("#8E8E9F"));
        btnTabUsers.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                tabIndex == 1 ? Color.parseColor("#FF8C00") : Color.TRANSPARENT));

        btnTabAppointments.setTextColor(tabIndex == 2 ? Color.parseColor("#FFFFFF") : Color.parseColor("#8E8E9F"));
        btnTabAppointments.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                tabIndex == 2 ? Color.parseColor("#FF8C00") : Color.TRANSPARENT));

        // Load data corresponding to the selected tab
        loadTabData();
    }

    private void fetchStatsCounters() {
        // Count Clients (role == USER)
        db.collection("Users")
                .whereEqualTo("role", "USER")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        tvCountUsers.setText(String.valueOf(value.size()));
                    }
                });

        // Count Lawyers (role == LAWYER)
        db.collection("Users")
                .whereEqualTo("role", "LAWYER")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        tvCountLawyers.setText(String.valueOf(value.size()));
                    }
                });

        // Count Appointments
        db.collection("Appointments")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        tvCountAppointments.setText(String.valueOf(value.size()));
                    }
                });
    }

    private void loadTabData() {
        masterList.clear();
        displayedList.clear();
        adapter.notifyDataSetChanged();

        if (currentTab == 0) {
            // Load Lawyers
            db.collection("Users")
                    .whereEqualTo("role", "LAWYER")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        masterList.clear();
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String uid = doc.getId();
                            String name = doc.getString("name");
                            String email = doc.getString("email");
                            String spec = doc.getString("specialization");
                            String barNum = doc.getString("barCouncilNumber");
                            String exp = doc.getString("experience");
                            String city = doc.getString("city");
                            String fee = doc.getString("fee");
                            String about = doc.getString("about");
                            Boolean approved = doc.getBoolean("approved");
                            if (approved == null) approved = false;

                            masterList.add(AdminItem.createLawyer(uid, name, email, spec, barNum, exp, city, fee, about, approved));
                        }
                        filterAndDisplayList();
                    });
        } else if (currentTab == 1) {
            // Load Clients (Users)
            db.collection("Users")
                    .whereEqualTo("role", "USER")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        masterList.clear();
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String uid = doc.getId();
                            String name = doc.getString("name");
                            String email = doc.getString("email");

                            masterList.add(AdminItem.createClient(uid, name, email));
                        }
                        filterAndDisplayList();
                    });
        } else if (currentTab == 2) {
            // Load Bookings (Appointments)
            db.collection("Appointments")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        masterList.clear();
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String id = doc.getId();
                            String client = doc.getString("userName");
                            String lawyer = doc.getString("lawyerName");
                            String date = doc.getString("date");
                            String time = doc.getString("time");
                            String spec = doc.getString("specialization");
                            String status = doc.getString("status");
                            if (status == null) status = "PENDING";

                            masterList.add(AdminItem.createBooking(id, client, lawyer, date, time, spec, status));
                        }
                        filterAndDisplayList();
                    });
        }
    }

    private void filterAndDisplayList() {
        String query = etSearch.getText().toString().trim().toLowerCase();
        displayedList.clear();

        for (AdminItem item : masterList) {
            // 1. Text Search Filter
            boolean matchesSearch = false;
            if (query.isEmpty()) {
                matchesSearch = true;
            } else {
                if (item.type.equals("LAWYER")) {
                    matchesSearch = (item.name != null && item.name.toLowerCase().contains(query)) ||
                                    (item.email != null && item.email.toLowerCase().contains(query)) ||
                                    (item.specialization != null && item.specialization.toLowerCase().contains(query)) ||
                                    (item.barCouncilNumber != null && item.barCouncilNumber.toLowerCase().contains(query));
                } else if (item.type.equals("CLIENT")) {
                    matchesSearch = (item.name != null && item.name.toLowerCase().contains(query)) ||
                                    (item.email != null && item.email.toLowerCase().contains(query));
                } else if (item.type.equals("BOOKING")) {
                    matchesSearch = (item.clientName != null && item.clientName.toLowerCase().contains(query)) ||
                                    (item.lawyerName != null && item.lawyerName.toLowerCase().contains(query)) ||
                                    (item.date != null && item.date.toLowerCase().contains(query)) ||
                                    (item.time != null && item.time.toLowerCase().contains(query)) ||
                                    (item.specialization != null && item.specialization.toLowerCase().contains(query)) ||
                                    (item.status != null && item.status.toLowerCase().contains(query));
                }
            }

            // 2. Status Filter
            boolean matchesFilter = true;
            if (item.type.equals("LAWYER")) {
                if (!filterLawyerStatus.equals("ALL")) {
                    matchesFilter = item.status.equalsIgnoreCase(filterLawyerStatus);
                }
            } else if (item.type.equals("BOOKING")) {
                if (!filterBookingStatus.equals("ALL")) {
                    matchesFilter = item.status.equalsIgnoreCase(filterBookingStatus);
                }
            }

            if (matchesSearch && matchesFilter) {
                displayedList.add(item);
            }
        }

        adapter.notifyDataSetChanged();

        // Empty state check
        if (displayedList.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            if (currentTab == 0) {
                tvEmptyStateText.setText("No other lawyers registered in this segment");
                ivEmptyStateIcon.setImageResource(R.drawable.ic_empty_person_vector);
            } else if (currentTab == 1) {
                tvEmptyStateText.setText("No other clients registered in this segment");
                ivEmptyStateIcon.setImageResource(R.drawable.ic_people_vector);
            } else {
                tvEmptyStateText.setText("No other bookings registered in this segment");
                ivEmptyStateIcon.setImageResource(R.drawable.ic_calendar_vector);
            }
        } else {
            layoutEmptyState.setVisibility(View.GONE);
        }
    }

    private void showFilterDialog() {
        if (currentTab == 0) {
            // Lawyers Filter
            String[] options = {"All Lawyers", "Approved Only", "Pending Approval"};
            int checkedItem = 0;
            if (filterLawyerStatus.equals("APPROVED")) checkedItem = 1;
            else if (filterLawyerStatus.equals("PENDING")) checkedItem = 2;

            new AlertDialog.Builder(this)
                    .setTitle("Filter Lawyers")
                    .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                        if (which == 0) filterLawyerStatus = "ALL";
                        else if (which == 1) filterLawyerStatus = "APPROVED";
                        else if (which == 2) filterLawyerStatus = "PENDING";
                    })
                    .setPositiveButton("Apply", (dialog, which) -> filterAndDisplayList())
                    .setNegativeButton("Cancel", null)
                    .show();
        } else if (currentTab == 2) {
            // Bookings Filter
            String[] options = {"All Bookings", "Pending", "Accepted", "Rejected"};
            int checkedItem = 0;
            if (filterBookingStatus.equals("PENDING")) checkedItem = 1;
            else if (filterBookingStatus.equals("ACCEPTED")) checkedItem = 2;
            else if (filterBookingStatus.equals("REJECTED")) checkedItem = 3;

            new AlertDialog.Builder(this)
                    .setTitle("Filter Bookings")
                    .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                        if (which == 0) filterBookingStatus = "ALL";
                        else if (which == 1) filterBookingStatus = "PENDING";
                        else if (which == 2) filterBookingStatus = "ACCEPTED";
                        else if (which == 3) filterBookingStatus = "REJECTED";
                    })
                    .setPositiveButton("Apply", (dialog, which) -> filterAndDisplayList())
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            Toast.makeText(this, "No filters available for Clients", Toast.LENGTH_SHORT).show();
        }
    }

    // --- Action Methods ---

    private void approveLawyer(String uid) {
        db.collection("Users")
                .document(uid)
                .update("approved", true)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AdminDashboardActivity.this, "Lawyer Approved Successfully", Toast.LENGTH_SHORT).show();
                    loadTabData();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AdminDashboardActivity.this, "Failed to approve: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateLawyerStatus(String uid, boolean approve) {
        db.collection("Users")
                .document(uid)
                .update("approved", approve)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AdminDashboardActivity.this, approve ? "Lawyer Restored" : "Lawyer Suspended", Toast.LENGTH_SHORT).show();
                    loadTabData();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AdminDashboardActivity.this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteUser(String uid) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to permanently delete this user? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("Users")
                            .document(uid)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(AdminDashboardActivity.this, "User deleted successfully", Toast.LENGTH_SHORT).show();
                                loadTabData();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(AdminDashboardActivity.this, "Failed to delete user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateBookingStatus(String id, String status) {
        db.collection("Appointments")
                .document(id)
                .update("status", status)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AdminDashboardActivity.this, "Booking marked as " + status, Toast.LENGTH_SHORT).show();
                    loadTabData();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AdminDashboardActivity.this, "Failed to update booking: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteBooking(String id) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Booking")
                .setMessage("Are you sure you want to delete this appointment booking record?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("Appointments")
                            .document(id)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(AdminDashboardActivity.this, "Booking deleted successfully", Toast.LENGTH_SHORT).show();
                                loadTabData();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(AdminDashboardActivity.this, "Failed to delete booking: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPopupMenu(View anchorView, AdminItem item) {
        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, anchorView);
        
        if (item.type.equals("LAWYER")) {
            if (item.status.equalsIgnoreCase("PENDING")) {
                popup.getMenu().add("Approve Profile");
            } else {
                popup.getMenu().add("Suspend Profile");
            }
            popup.getMenu().add("Delete Profile");
            popup.getMenu().add("View Details");
        } else if (item.type.equals("CLIENT")) {
            popup.getMenu().add("Delete Account");
            popup.getMenu().add("View Details");
        } else if (item.type.equals("BOOKING")) {
            if (item.status.equalsIgnoreCase("PENDING")) {
                popup.getMenu().add("Accept Booking");
                popup.getMenu().add("Reject Booking");
            }
            popup.getMenu().add("Delete Booking");
            popup.getMenu().add("View Details");
        }

        popup.setOnMenuItemClickListener(menuItem -> {
            String title = menuItem.getTitle().toString();
            if (title.equals("Approve Profile")) {
                approveLawyer(item.id);
            } else if (title.equals("Suspend Profile")) {
                updateLawyerStatus(item.id, false);
            } else if (title.equals("Delete Profile") || title.equals("Delete Account")) {
                deleteUser(item.id);
            } else if (title.equals("Accept Booking")) {
                updateBookingStatus(item.id, "ACCEPTED");
            } else if (title.equals("Reject Booking")) {
                updateBookingStatus(item.id, "REJECTED");
            } else if (title.equals("Delete Booking")) {
                deleteBooking(item.id);
            } else if (title.equals("View Details")) {
                showDetailSheet(item);
            }
            return true;
        });
        
        popup.show();
    }

    // --- Bottom Sheet Details Drawer ---

    private void showDetailSheet(AdminItem item) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_admin_detail, null);

        TextView tvDetailAvatar = view.findViewById(R.id.tvDetailAvatar);
        TextView tvDetailTitle = view.findViewById(R.id.tvDetailTitle);
        TextView tvDetailSub = view.findViewById(R.id.tvDetailSub);
        TextView tvDetailStatus = view.findViewById(R.id.tvDetailStatus);
        LinearLayout layoutFields = view.findViewById(R.id.layoutDetailFields);

        MaterialButton btnPrimary = view.findViewById(R.id.btnDetailPrimary);
        MaterialButton btnSecondary = view.findViewById(R.id.btnDetailSecondary);

        // Bind common fields
        tvDetailTitle.setText(item.name);
        tvDetailStatus.setText(item.status.toUpperCase());

        String initial = "A";
        String displayName = item.name;
        if (displayName.startsWith("Adv. ")) displayName = displayName.substring(5);
        if (!displayName.isEmpty()) initial = String.valueOf(displayName.charAt(0)).toUpperCase();
        tvDetailAvatar.setText(initial);

        // Color status badge
        if (item.status.equalsIgnoreCase("PENDING")) {
            tvDetailStatus.setTextColor(Color.parseColor("#FF9F0A"));
            tvDetailStatus.setBackgroundResource(R.drawable.badge_pending);
        } else if (item.status.equalsIgnoreCase("APPROVED") || item.status.equalsIgnoreCase("ACTIVE") || item.status.equalsIgnoreCase("ACCEPTED")) {
            tvDetailStatus.setTextColor(Color.parseColor("#30D158"));
            tvDetailStatus.setBackgroundResource(R.drawable.badge_accepted);
        } else {
            tvDetailStatus.setTextColor(Color.parseColor("#FF453A"));
            tvDetailStatus.setBackgroundResource(R.drawable.badge_rejected);
        }

        layoutFields.removeAllViews();

        if (item.type.equals("LAWYER")) {
            tvDetailSub.setText(item.specialization);
            
            addDetailField(layoutFields, "Email Address", item.email);
            addDetailField(layoutFields, "Bar Council Number", item.barCouncilNumber);
            addDetailField(layoutFields, "Years of Experience", item.experience);
            addDetailField(layoutFields, "Location City", item.city);
            addDetailField(layoutFields, "Consultation Fee", "₹" + item.fee);
            addDetailField(layoutFields, "About / Bio", item.about);

            // Configure actions
            btnSecondary.setText("DELETE PROFILE");
            btnSecondary.setOnClickListener(v -> {
                dialog.dismiss();
                deleteUser(item.id);
            });

            if (item.status.equalsIgnoreCase("PENDING")) {
                btnPrimary.setVisibility(View.VISIBLE);
                btnPrimary.setText("APPROVE PROFILE");
                btnPrimary.setOnClickListener(v -> {
                    dialog.dismiss();
                    approveLawyer(item.id);
                });
            } else {
                btnPrimary.setVisibility(View.VISIBLE);
                btnPrimary.setText("SUSPEND LAWYER");
                btnPrimary.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF9F0A")));
                btnPrimary.setOnClickListener(v -> {
                    dialog.dismiss();
                    updateLawyerStatus(item.id, false);
                });
            }

        } else if (item.type.equals("CLIENT")) {
            tvDetailSub.setText("Registered Client User");
            
            addDetailField(layoutFields, "Email Address", item.email);
            addDetailField(layoutFields, "Account User ID", item.id);
            addDetailField(layoutFields, "User Role", "Client");

            btnPrimary.setVisibility(View.GONE);
            btnSecondary.setText("DELETE ACCOUNT");
            btnSecondary.setOnClickListener(v -> {
                dialog.dismiss();
                deleteUser(item.id);
            });

        } else if (item.type.equals("BOOKING")) {
            tvDetailSub.setText(item.specialization);

            addDetailField(layoutFields, "Client Name", item.clientName);
            addDetailField(layoutFields, "Lawyer Name", item.lawyerName);
            addDetailField(layoutFields, "Appointment Date", item.date);
            addDetailField(layoutFields, "Appointment Time", item.time);
            addDetailField(layoutFields, "Booking Ref ID", item.id);

            if (item.status.equalsIgnoreCase("PENDING")) {
                btnPrimary.setVisibility(View.VISIBLE);
                btnPrimary.setText("ACCEPT BOOKING");
                btnPrimary.setOnClickListener(v -> {
                    dialog.dismiss();
                    updateBookingStatus(item.id, "ACCEPTED");
                });

                btnSecondary.setText("REJECT BOOKING");
                btnSecondary.setOnClickListener(v -> {
                    dialog.dismiss();
                    updateBookingStatus(item.id, "REJECTED");
                });
            } else {
                btnPrimary.setVisibility(View.GONE);
                btnSecondary.setText("DELETE BOOKING RECORD");
                btnSecondary.setOnClickListener(v -> {
                    dialog.dismiss();
                    deleteBooking(item.id);
                });
            }
        }

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

    // --- Admin Data Model Wrapper ---

    private static class AdminItem {
        String id;
        String type; // "LAWYER", "CLIENT", "BOOKING"
        String name;
        String email;
        String status; // "PENDING", "APPROVED", "ACTIVE", "ACCEPTED", "REJECTED"

        // Lawyer fields
        String specialization;
        String barCouncilNumber;
        String experience;
        String city;
        String fee;
        String about;

        // Booking fields
        String clientName;
        String lawyerName;
        String date;
        String time;

        public static AdminItem createLawyer(String id, String name, String email, String spec, String barNum, String exp, String city, String fee, String about, boolean approved) {
            AdminItem item = new AdminItem();
            item.id = id;
            item.type = "LAWYER";
            item.name = "Adv. " + name;
            item.email = email;
            item.status = approved ? "APPROVED" : "PENDING";
            item.specialization = spec;
            item.barCouncilNumber = barNum;
            item.experience = exp;
            item.city = city;
            item.fee = fee;
            item.about = about;
            return item;
        }

        public static AdminItem createClient(String id, String name, String email) {
            AdminItem item = new AdminItem();
            item.id = id;
            item.type = "CLIENT";
            item.name = name;
            item.email = email;
            item.status = "ACTIVE";
            return item;
        }

        public static AdminItem createBooking(String id, String clientName, String lawyerName, String date, String time, String spec, String status) {
            AdminItem item = new AdminItem();
            item.id = id;
            item.type = "BOOKING";
            item.clientName = clientName;
            item.lawyerName = lawyerName;
            item.date = date;
            item.time = time;
            item.specialization = spec != null ? spec : "General Consultation";
            item.status = status;
            item.name = "Booking: " + clientName + " with " + lawyerName;
            item.email = "Client: " + clientName + " | Lawyer: " + lawyerName;
            return item;
        }
    }

    private void startNotificationListeners() {
        // Listener for pending Lawyers
        db.collection("Users")
                .whereEqualTo("role", "LAWYER")
                .whereEqualTo("approved", false)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        pendingLawyersCount = value.size();
                        updateNotificationDot();
                    }
                });

        // Listener for pending Bookings
        db.collection("Appointments")
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        pendingAppointmentsCount = value.size();
                        updateNotificationDot();
                    }
                });
    }

    private void updateNotificationDot() {
        if (viewBellDot != null) {
            boolean hasPending = pendingLawyersCount > 0 || pendingAppointmentsCount > 0;
            viewBellDot.setVisibility(hasPending ? View.VISIBLE : View.GONE);
        }
    }

    private void showNotificationsDialog() {
        if (pendingLawyersCount == 0 && pendingAppointmentsCount == 0) {
            Toast.makeText(this, "You have no pending notification requests.", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder message = new StringBuilder("You have pending action items:\n");
        if (pendingLawyersCount > 0) {
            message.append("\n• ").append(pendingLawyersCount).append(" Lawyer profile(s) awaiting approval.");
        }
        if (pendingAppointmentsCount > 0) {
            message.append("\n• ").append(pendingAppointmentsCount).append(" Appointment booking(s) awaiting action.");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Pending Requests Notification")
                .setMessage(message.toString())
                .setCancelable(true);

        if (pendingLawyersCount > 0) {
            builder.setPositiveButton("Manage Lawyers", (dialog, which) -> {
                selectNav(1); // Go to Lawyers list mode
            });
        }
        
        if (pendingAppointmentsCount > 0) {
            if (pendingLawyersCount > 0) {
                builder.setNeutralButton("Manage Bookings", (dialog, which) -> {
                    selectNav(3); // Go to Bookings list mode
                });
            } else {
                builder.setPositiveButton("Manage Bookings", (dialog, which) -> {
                    selectNav(3); // Go to Bookings list mode
                });
            }
        }

        builder.setNegativeButton("Dismiss", null);
        builder.show();
    }

    // --- RecyclerView Adapter Implementation ---

    private class AdminAdapter extends RecyclerView.Adapter<AdminAdapter.ViewHolder> {

        private List<AdminItem> items;

        public AdminAdapter(List<AdminItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_list, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AdminItem item = items.get(position);

            holder.tvTitle.setText(item.name);
            holder.tvSub1.setText(item.email);
            holder.tvStatus.setText(item.status.toUpperCase());

            // Avatar initial
            String displayName = item.name;
            if (displayName.startsWith("Adv. ")) displayName = displayName.substring(5);
            if (displayName.startsWith("Booking: ")) displayName = displayName.substring(9);
            if (!displayName.isEmpty()) {
                holder.tvAvatar.setText(String.valueOf(displayName.charAt(0)).toUpperCase());
            } else {
                holder.tvAvatar.setText("A");
            }

            // Subtitle 2 / Specialization row
            if (item.type.equals("LAWYER")) {
                holder.tvSub2.setText(item.specialization);
                holder.tvSub2.setVisibility(View.VISIBLE);
                
                holder.layoutBarRow.setVisibility(View.VISIBLE);
                holder.tvBarNum.setText("Bar #: " + item.barCouncilNumber);
            } else if (item.type.equals("CLIENT")) {
                holder.tvSub2.setText("Client User");
                holder.tvSub2.setVisibility(View.VISIBLE);
                
                holder.layoutBarRow.setVisibility(View.GONE);
            } else if (item.type.equals("BOOKING")) {
                holder.tvSub2.setText(item.specialization);
                holder.tvSub2.setVisibility(View.VISIBLE);
                
                holder.layoutBarRow.setVisibility(View.VISIBLE);
                holder.tvBarNum.setText("Date: " + item.date + " | Time: " + item.time);
            }

            // Status badges styling
            if (item.status.equalsIgnoreCase("PENDING")) {
                holder.tvStatus.setTextColor(Color.parseColor("#FF9F0A")); // Orange
                holder.tvStatus.setBackgroundResource(R.drawable.badge_pending);
            } else if (item.status.equalsIgnoreCase("APPROVED") || item.status.equalsIgnoreCase("ACTIVE") || item.status.equalsIgnoreCase("ACCEPTED")) {
                holder.tvStatus.setTextColor(Color.parseColor("#30D158")); // Green
                holder.tvStatus.setBackgroundResource(R.drawable.badge_accepted);
            } else {
                holder.tvStatus.setTextColor(Color.parseColor("#FF453A")); // Red
                holder.tvStatus.setBackgroundResource(R.drawable.badge_rejected);
            }

            // Approve quick action button
            if (item.type.equals("LAWYER") && item.status.equalsIgnoreCase("PENDING")) {
                holder.layoutActions.setVisibility(View.VISIBLE);
                holder.btnApprove.setOnClickListener(v -> approveLawyer(item.id));
            } else {
                holder.layoutActions.setVisibility(View.GONE);
            }

            // Full profile / menu clicks
            holder.btnViewProfile.setOnClickListener(v -> showDetailSheet(item));
            holder.btnMenu.setOnClickListener(v -> showPopupMenu(v, item));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvSub1, tvSub2, tvStatus, tvAvatar, tvBarNum;
            View layoutBarRow, layoutActions;
            MaterialButton btnApprove, btnViewProfile;
            ImageView btnMenu;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvAdminItemTitle);
                tvSub1 = itemView.findViewById(R.id.tvAdminItemSub1);
                tvSub2 = itemView.findViewById(R.id.tvAdminItemSub2);
                tvStatus = itemView.findViewById(R.id.tvAdminItemStatus);
                tvAvatar = itemView.findViewById(R.id.tvAdminItemAvatar);
                
                layoutBarRow = itemView.findViewById(R.id.layoutBarNumberRow);
                tvBarNum = itemView.findViewById(R.id.tvAdminItemBarNumber);
                
                layoutActions = itemView.findViewById(R.id.layoutAdminActions);
                btnApprove = itemView.findViewById(R.id.btnAdminApprove);
                btnViewProfile = itemView.findViewById(R.id.btnViewFullProfile);
                btnMenu = itemView.findViewById(R.id.btnAdminItemMenu);
            }
        }
    }
}
