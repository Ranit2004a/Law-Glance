package com.example.ywinked;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Calendar;
import java.util.List;

public class CourtRemindersActivity extends AppCompatActivity {

    private EditText editCaseTitle, editCourtName;
    private Button btnPickDate, btnPickTime, btnSaveReminder;
    private RecyclerView recyclerReminders;
    
    private String selectedDate = "";
    private String selectedTime = "";
    
    private DatabaseHelper dbHelper;
    private List<ReminderModel> reminderList;
    private ReminderAdapter adapter;

    private static final int NOTIFICATION_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_court_reminders);

        dbHelper = new DatabaseHelper(this);

        // 1. Create Notification Channel
        createNotificationChannel();

        // 2. Request runtime Notification permission on Android 13+
        checkNotificationPermission();

        ImageView backBtn = findViewById(R.id.ic_back);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }

        editCaseTitle = findViewById(R.id.editCaseTitle);
        editCourtName = findViewById(R.id.editCourtName);
        btnPickDate = findViewById(R.id.btnPickDate);
        btnPickTime = findViewById(R.id.btnPickTime);
        btnSaveReminder = findViewById(R.id.btnSaveReminder);
        recyclerReminders = findViewById(R.id.recyclerReminders);

        recyclerReminders.setLayoutManager(new LinearLayoutManager(this));
        
        loadReminders();

        btnPickDate.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view, year1, monthOfYear, dayOfMonth) -> {
                        selectedDate = dayOfMonth + "-" + (monthOfYear + 1) + "-" + year1;
                        btnPickDate.setText(selectedDate);

                        // If user picks today's date, validate if selectedTime is in the past, and reset it if so
                        if (!selectedTime.isEmpty()) {
                            final Calendar currentCal = Calendar.getInstance();
                            String todayStr = currentCal.get(Calendar.DAY_OF_MONTH) + "-" + (currentCal.get(Calendar.MONTH) + 1) + "-" + currentCal.get(Calendar.YEAR);
                            if (selectedDate.equals(todayStr)) {
                                String[] timeParts = selectedTime.split(":");
                                int selectedHour = Integer.parseInt(timeParts[0]);
                                int selectedMinute = Integer.parseInt(timeParts[1]);
                                if (selectedHour < currentCal.get(Calendar.HOUR_OF_DAY) || 
                                    (selectedHour == currentCal.get(Calendar.HOUR_OF_DAY) && selectedMinute < currentCal.get(Calendar.MINUTE))) {
                                    selectedTime = "";
                                    btnPickTime.setText("Pick Time");
                                    Toast.makeText(this, "Cleared past time. Please pick a future time.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }, year, month, day);
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            datePickerDialog.show();
        });

        btnPickTime.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                    (view, hourOfDay, minute1) -> {
                        final Calendar currentCal = Calendar.getInstance();
                        String todayStr = currentCal.get(Calendar.DAY_OF_MONTH) + "-" + (currentCal.get(Calendar.MONTH) + 1) + "-" + currentCal.get(Calendar.YEAR);
                        if (!selectedDate.isEmpty() && selectedDate.equals(todayStr)) {
                            if (hourOfDay < currentCal.get(Calendar.HOUR_OF_DAY) || 
                                (hourOfDay == currentCal.get(Calendar.HOUR_OF_DAY) && minute1 < currentCal.get(Calendar.MINUTE))) {
                                Toast.makeText(this, "Please select a future time.", Toast.LENGTH_LONG).show();
                                return;
                            }
                        }
                        selectedTime = String.format("%02d:%02d", hourOfDay, minute1);
                        btnPickTime.setText(selectedTime);
                    }, hour, minute, true);
            timePickerDialog.show();
        });

        btnSaveReminder.setOnClickListener(v -> {
            String title = editCaseTitle.getText().toString().trim();
            String court = editCourtName.getText().toString().trim();

            if (title.isEmpty() || court.isEmpty() || selectedDate.isEmpty() || selectedTime.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Additional double-check validation before saving
            final Calendar currentCal = Calendar.getInstance();
            String todayStr = currentCal.get(Calendar.DAY_OF_MONTH) + "-" + (currentCal.get(Calendar.MONTH) + 1) + "-" + currentCal.get(Calendar.YEAR);
            if (selectedDate.equals(todayStr)) {
                String[] timeParts = selectedTime.split(":");
                int selectedHour = Integer.parseInt(timeParts[0]);
                int selectedMinute = Integer.parseInt(timeParts[1]);
                if (selectedHour < currentCal.get(Calendar.HOUR_OF_DAY) || 
                    (selectedHour == currentCal.get(Calendar.HOUR_OF_DAY) && selectedMinute < currentCal.get(Calendar.MINUTE))) {
                    Toast.makeText(this, "Please select a future time.", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            long id = dbHelper.addReminder(title, court, selectedDate, selectedTime);
            if (id > 0) {
                // Schedule alarm for the newly saved reminder
                scheduleAlarm((int) id, title, court, selectedDate, selectedTime);

                Toast.makeText(this, "Reminder saved successfully!", Toast.LENGTH_SHORT).show();
                editCaseTitle.setText("");
                editCourtName.setText("");
                selectedDate = "";
                selectedTime = "";
                btnPickDate.setText("Pick Date");
                btnPickTime.setText("Pick Time");
                loadReminders();
            } else {
                Toast.makeText(this, "Error saving reminder", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadReminders() {
        reminderList = dbHelper.getAllReminders();
        adapter = new ReminderAdapter(reminderList, reminder -> {
            // Cancel alarm before deleting from DB
            cancelAlarm(reminder.id);
            dbHelper.deleteReminder(reminder.id);
            Toast.makeText(this, "Reminder deleted", Toast.LENGTH_SHORT).show();
            loadReminders();
        });
        recyclerReminders.setAdapter(adapter);
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "court_reminders_channel",
                    "Court Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for scheduled court hearing dates");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void scheduleAlarm(int reminderId, String title, String court, String dateStr, String timeStr) {
        try {
            String[] dateParts = dateStr.split("-");
            String[] timeParts = timeStr.split(":");
            int day = Integer.parseInt(dateParts[0]);
            int month = Integer.parseInt(dateParts[1]) - 1; // Calendar month is 0-indexed
            int year = Integer.parseInt(dateParts[2]);
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            long triggerTime = calendar.getTimeInMillis();

            // Only schedule if time is in the future
            if (triggerTime <= System.currentTimeMillis()) {
                return;
            }

            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, ReminderReceiver.class);
            intent.putExtra("case_title", title);
            intent.putExtra("court_name", court);
            intent.putExtra("reminder_id", reminderId);

            int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    reminderId,
                    intent,
                    pendingFlags
            );

            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cancelAlarm(int reminderId) {
        try {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, ReminderReceiver.class);

            int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    reminderId,
                    intent,
                    pendingFlags
            );

            if (alarmManager != null && pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
