package com.example.ywinked;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class BookLawyerActivity extends AppCompatActivity {

    private RecyclerView recyclerLawyers;
    private LawyerAdapter adapter;
    private List<LawyerModel> lawyerList;
    private List<LawyerModel> fullLawyerList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_lawyer);

        ImageView backBtn = findViewById(R.id.ic_back);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }

        // Center aligned title with colored text
        TextView txtTitle = findViewById(R.id.txtTitle);
        if (txtTitle != null) {
            txtTitle.setText(android.text.Html.fromHtml("Book a <font color='#FF9F0A'>Lawyer</font>"));
        }

        recyclerLawyers = findViewById(R.id.recyclerLawyers);
        recyclerLawyers.setLayoutManager(new LinearLayoutManager(this));

        // Create lawyers list and adapter
        lawyerList = new ArrayList<>();
        fullLawyerList = new ArrayList<>();
        adapter = new LawyerAdapter(lawyerList, this::showBookingDateTimePickers);
        recyclerLawyers.setAdapter(adapter);

        // Set up real-time search filter
        android.widget.EditText searchBar = findViewById(R.id.searchBar);
        if (searchBar != null) {
            searchBar.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterLawyers(s.toString());
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });
        }

        fetchApprovedLawyers();
    }

    private void filterLawyers(String query) {
        String cleanQuery = query.toLowerCase().trim();
        lawyerList.clear();
        if (cleanQuery.isEmpty()) {
            lawyerList.addAll(fullLawyerList);
        } else {
            for (LawyerModel lawyer : fullLawyerList) {
                if (lawyer.name.toLowerCase().contains(cleanQuery) ||
                    lawyer.specialization.toLowerCase().contains(cleanQuery) ||
                    (lawyer.city != null && lawyer.city.toLowerCase().contains(cleanQuery))) {
                    lawyerList.add(lawyer);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void fetchApprovedLawyers() {
        FirebaseFirestore.getInstance().collection("Users")
                .whereEqualTo("role", "LAWYER")
                .whereEqualTo("approved", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    lawyerList.clear();
                    fullLawyerList.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("name");
                        String specialization = doc.getString("specialization");
                        String experience = doc.getString("experience");
                        String fee = doc.getString("fee");
                        String city = doc.getString("city");
                        if (city == null || city.isEmpty()) {
                            city = "India";
                        }
                        String base64 = doc.getString("profileImageBase64");
                        if (base64 == null) {
                            base64 = "";
                        }

                        LawyerModel lawyer = new LawyerModel("Adv. " + name, specialization, experience, "₹" + fee, city, base64);
                        lawyerList.add(lawyer);
                        fullLawyerList.add(lawyer);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(BookLawyerActivity.this, "Failed to load lawyers: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showBookingDateTimePickers(LawyerModel lawyer) {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    String selectedDate = dayOfMonth + "-" + (monthOfYear + 1) + "-" + year1;
                    showTimePicker(lawyer, selectedDate);
                }, year, month, day);
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void showTimePicker(LawyerModel lawyer, String selectedDate) {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute1) -> {
                    final Calendar currentCal = Calendar.getInstance();
                    String todayStr = currentCal.get(Calendar.DAY_OF_MONTH) + "-" + (currentCal.get(Calendar.MONTH) + 1) + "-" + currentCal.get(Calendar.YEAR);
                    if (selectedDate.equals(todayStr)) {
                        if (hourOfDay < currentCal.get(Calendar.HOUR_OF_DAY) || 
                            (hourOfDay == currentCal.get(Calendar.HOUR_OF_DAY) && minute1 < currentCal.get(Calendar.MINUTE))) {
                            Toast.makeText(this, "Please select a future time.", Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    String selectedTime = String.format("%02d:%02d", hourOfDay, minute1);
                    bookAppointmentOnBackend(lawyer, selectedDate, selectedTime);
                }, hour, minute, true);
        timePickerDialog.show();
    }

    private void bookAppointmentOnBackend(LawyerModel lawyer, String date, String time) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String userName = sharedPref.getString("name", "Client");

        Map<String, Object> appointment = new HashMap<>();
        appointment.put("userId", uid);
        appointment.put("userName", userName);
        appointment.put("lawyerName", lawyer.name);
        appointment.put("specialization", lawyer.specialization);
        appointment.put("date", date);
        appointment.put("time", time);
        appointment.put("status", "PENDING"); // Request status is initially pending
        appointment.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance().collection("Appointments")
                .add(appointment)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(BookLawyerActivity.this, "Success! Appointment requested with " + lawyer.name + " on " + date + " at " + time + ".", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(BookLawyerActivity.this, "Booking failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
