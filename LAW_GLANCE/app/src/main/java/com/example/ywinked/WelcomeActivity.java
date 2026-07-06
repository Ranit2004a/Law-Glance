package com.example.ywinked;

import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class WelcomeActivity extends AppCompatActivity {

    private ImageView logo;
    private View btnProfile;
    private ImageView ivUserProfilePic;
    private TextView tvUserInitial;

    private CardView cardAskLegal, cardBookLawyer, cardLegalDocs, cardLearnRights, cardCourtReminders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Top Bar
        logo = findViewById(R.id.logo);
        btnProfile = findViewById(R.id.btnProfile);
        ivUserProfilePic = findViewById(R.id.ivUserProfilePic);
        tvUserInitial = findViewById(R.id.tvUserInitial);

        loadProfileUi();

        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, UserSettingsActivity.class);
            startActivity(intent);
        });

        // Feature Cards
        cardAskLegal = findViewById(R.id.cardAskLegal);
        cardBookLawyer = findViewById(R.id.cardBookLawyer); // Add IDs in XML if needed
        cardLegalDocs = findViewById(R.id.cardLegalDocs);
        cardLearnRights = findViewById(R.id.cardLearnRights);
        cardCourtReminders = findViewById(R.id.cardCourtReminders);

        // Click listeners for cards
        cardAskLegal.setOnClickListener(v -> {
            // Open Legal Chatbot Activity
            Intent intent = new Intent(WelcomeActivity.this, LegalChatbotActivity.class);
            startActivity(intent);
        });

        cardBookLawyer.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, BookLawyerActivity.class);
            startActivity(intent);
        });

        cardLegalDocs.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, LegalDocumentsActivity.class);
            startActivity(intent);
        });

        cardLearnRights.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, RightsSearchActivity.class);
            startActivity(intent);
        });

        cardCourtReminders.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, CourtRemindersActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfileUi();
    }

    private void loadProfileUi() {
        SharedPreferences sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String name = sharedPref.getString("name", "User");

        java.io.File file = new java.io.File(getFilesDir(), "user_profile_picture.jpg");
        if (file.exists()) {
            ivUserProfilePic.setImageURI(android.net.Uri.fromFile(file));
            ivUserProfilePic.setVisibility(View.VISIBLE);
            tvUserInitial.setVisibility(View.GONE);
        } else {
            String base64Image = sharedPref.getString("profile_image_base64", "");
            if (!base64Image.isEmpty()) {
                try {
                    byte[] decodedBytes = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
                    android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    ivUserProfilePic.setImageBitmap(bitmap);
                    ivUserProfilePic.setVisibility(View.VISIBLE);
                    tvUserInitial.setVisibility(View.GONE);
                } catch (Exception e) {
                    e.printStackTrace();
                    ivUserProfilePic.setVisibility(View.GONE);
                    tvUserInitial.setVisibility(View.VISIBLE);
                }
            } else {
                ivUserProfilePic.setVisibility(View.GONE);
                tvUserInitial.setVisibility(View.VISIBLE);
                if (!name.isEmpty()) {
                    tvUserInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
                }
            }
        }
    }
}

