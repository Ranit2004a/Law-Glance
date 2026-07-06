package com.example.ywinked;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "court_reminders_channel";
    private static final String CHANNEL_NAME = "Court Reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        String caseTitle = intent.getStringExtra("case_title");
        String courtName = intent.getStringExtra("court_name");
        int reminderId = intent.getIntExtra("reminder_id", 0);

        if (caseTitle == null) caseTitle = "Court Hearing Reminder";
        if (courtName == null) courtName = "You have a scheduled court event today.";

        // 1. Create Notification Channel if on Android 8.0+
        createNotificationChannel(context);

        // 2. Setup Notification Tap Action (Open CourtRemindersActivity)
        Intent tapIntent = new Intent(context, CourtRemindersActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        // PendingIntent flags
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                reminderId,
                tapIntent,
                pendingFlags
        );

        // 3. Define Alarm Sound & Vibration
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // 4. Build Notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // System alarm icon
                .setContentTitle("Court Hearing: " + caseTitle)
                .setContentText("Location: " + courtName)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(alarmSound)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 500, 200, 500});

        // 5. Send Notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        
        // Check for permission (Required on Android 13+)
        // Although the permission check is handled by the calling activity, we check here
        // to satisfy compile/runtime safety without throwing crashes
        try {
            notificationManager.notify(reminderId, builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for scheduled court hearing dates");
            channel.enableLights(true);
            channel.enableVibration(true);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
