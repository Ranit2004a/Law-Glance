package com.example.ywinked;

import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button getStartedBtn;
    private VideoView welcomeVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is already logged in
        SharedPreferences sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        boolean isLoggedIn = sharedPref.getBoolean("isLoggedIn", false);
        if (isLoggedIn) {
            String role = sharedPref.getString("role", "USER");
            Intent intent;
            if (role.equals("LAWYER")) {
                intent = new Intent(MainActivity.this, LawyerDashboardActivity.class);
            } else if (role.equals("ADMIN")) {
                intent = new Intent(MainActivity.this, AdminDashboardActivity.class);
            } else {
                intent = new Intent(MainActivity.this, WelcomeActivity.class);
            }
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        getStartedBtn = findViewById(R.id.getStartedBtn);
        welcomeVideo = findViewById(R.id.welcomeVideo);

        // Set video from res/raw
        Uri videoUri = Uri.parse("android.resource://"
                + getPackageName() + "/" + R.raw.welcome_video);

        welcomeVideo.setVideoURI(videoUri);

        // Loop video
        welcomeVideo.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(true);
                welcomeVideo.start();
            }
        });

        // Button click → LoginActivity
        getStartedBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        welcomeVideo.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        welcomeVideo.pause();
    }
}
