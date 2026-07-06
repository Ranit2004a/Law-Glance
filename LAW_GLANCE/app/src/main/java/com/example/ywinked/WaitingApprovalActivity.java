package com.example.ywinked;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class WaitingApprovalActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView tvMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_approval);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        tvMessage = findViewById(R.id.tvMessage);

        SharedPreferences sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String name = sharedPref.getString("name", "");
        if (!name.isEmpty()) {
            tvMessage.setText("Thank you, Adv. " + name + "!\n\nYour lawyer profile has been submitted. The administrator is currently reviewing your registration. You will be able to log in once your profile is approved.");
        }

        Button btnRefresh = findViewById(R.id.btnRefresh);
        Button btnLogout = findViewById(R.id.btnLogout);

        btnRefresh.setOnClickListener(v -> checkApprovalStatus());

        btnLogout.setOnClickListener(v -> logoutUser());
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
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean approved = documentSnapshot.getBoolean("approved");
                        if (approved != null && approved) {
                            // Update SharedPreferences isLoggedIn and approved status
                            SharedPreferences sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putBoolean("isLoggedIn", true);
                            editor.apply();

                            Toast.makeText(WaitingApprovalActivity.this, "Your profile is approved!", Toast.LENGTH_SHORT).show();
                            
                            // Redirect to LawyerDashboardActivity
                            Intent intent = new Intent(WaitingApprovalActivity.this, LawyerDashboardActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(WaitingApprovalActivity.this, "Account still pending approval. Please wait.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(WaitingApprovalActivity.this, "Error: Profile not found.", Toast.LENGTH_SHORT).show();
                        logoutUser();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(WaitingApprovalActivity.this, "Network error. Please try again: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void logoutUser() {
        mAuth.signOut();

        // Clear user session
        SharedPreferences sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(WaitingApprovalActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
