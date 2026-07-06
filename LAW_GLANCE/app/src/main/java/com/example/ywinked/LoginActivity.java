package com.example.ywinked;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {
    private EditText loginEmail, loginPassword;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private androidx.activity.result.ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Views
        loginEmail = findViewById(R.id.loginEmail);
        loginPassword = findViewById(R.id.loginPassword);
        Button loginBtn = findViewById(R.id.loginBtn);
        Button registerBtn = findViewById(R.id.registerBtn);

        // Open RegisterActivity when "Register" is clicked
        registerBtn.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // Forgot Password click event
        TextView forgotPasswordTv = findViewById(R.id.forgotPassword);
        forgotPasswordTv.setOnClickListener(v -> {
            EditText resetEmail = new EditText(v.getContext());
            resetEmail.setHint("Enter your email");
            resetEmail.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

            // Standard Android padding/margin for dialog input
            android.widget.FrameLayout container = new android.widget.FrameLayout(v.getContext());
            android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.leftMargin = 50;
            params.rightMargin = 50;
            params.topMargin = 20;
            resetEmail.setLayoutParams(params);
            container.addView(resetEmail);

            new androidx.appcompat.app.AlertDialog.Builder(LoginActivity.this)
                    .setTitle("Reset Password")
                    .setMessage("Enter your email to receive a password reset link.")
                    .setView(container)
                    .setPositiveButton("Send", (dialog, which) -> {
                        String email = resetEmail.getText().toString().trim();
                        if (email.isEmpty()) {
                            Toast.makeText(LoginActivity.this, "Please enter your email", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        mAuth.sendPasswordResetEmail(email)
                                .addOnCompleteListener(resetTask -> {
                                    if (resetTask.isSuccessful()) {
                                        Toast.makeText(LoginActivity.this, "Reset link sent to " + email, Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(LoginActivity.this, "Failed to send reset link: " + resetTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Login Button click event
        loginBtn.setOnClickListener(v -> {
            String email = loginEmail.getText().toString().trim();
            String password = loginPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            loginUser(email, password);
        });

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            if (account != null) {
                                firebaseAuthWithGoogle(account.getIdToken());
                            }
                        } catch (ApiException e) {
                            Toast.makeText(LoginActivity.this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        android.view.View googleSignInBtn = findViewById(R.id.googleSignInBtn);
        googleSignInBtn.setOnClickListener(v -> signIn());
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkOrRegisterGoogleUser(user);
                        }
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Authentication failed.";
                        Toast.makeText(LoginActivity.this, "Google auth failed: " + errorMsg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkOrRegisterGoogleUser(FirebaseUser user) {
        db.collection("Users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String userName = documentSnapshot.getString("name");
                        String userEmail = documentSnapshot.getString("email");
                        String role = documentSnapshot.getString("role");
                        if (role == null) role = "USER";
                        Boolean approved = documentSnapshot.getBoolean("approved");
                        if (approved == null) approved = true;
                        String base64 = documentSnapshot.getString("profileImageBase64");
                        if (base64 == null) base64 = "";

                        handleUserLogin(user, userName, userEmail, role, approved, base64);
                    } else {
                        registerNewGoogleUser(user);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LoginActivity.this, "Failed to check user profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void registerNewGoogleUser(FirebaseUser user) {
        String displayName = user.getDisplayName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = "Google User";
        }
        String email = user.getEmail();
        String role = "USER"; // Default client role

        Map<String, Object> userData = new java.util.HashMap<>();
        userData.put("name", displayName);
        userData.put("email", email);
        userData.put("role", role);
        userData.put("uid", user.getUid());
        userData.put("approved", true);

        final String finalDisplayName = displayName;
        final String finalEmail = email;
        final String finalRole = role;
        db.collection("Users")
                .document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> handleUserLogin(user, finalDisplayName, finalEmail, finalRole, true, ""))
                .addOnFailureListener(e -> {
                    Toast.makeText(LoginActivity.this, "Failed to create user profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void handleUserLogin(FirebaseUser user, String userName, String userEmail, String role, boolean approved, String profileImageBase64) {
        // Save to SharedPreferences
        SharedPreferences sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("email", userEmail);
        editor.putString("name", userName);
        editor.putString("uid", user.getUid());
        editor.putString("role", role);
        editor.putBoolean("isLoggedIn", true);
        if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
            editor.putString("profile_image_base64", profileImageBase64);
        } else {
            editor.putString("profile_image_base64", "");
        }
        editor.apply();

        Toast.makeText(LoginActivity.this, "Welcome " + userName + "!", Toast.LENGTH_SHORT).show();

        // Routing based on role
        Intent intent;
        if (role.equals("LAWYER")) {
            if (approved) {
                intent = new Intent(LoginActivity.this, LawyerDashboardActivity.class);
            } else {
                intent = new Intent(LoginActivity.this, WaitingApprovalActivity.class);
            }
        } else if (role.equals("ADMIN")) {
            intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, WelcomeActivity.class);
        }
        startActivity(intent);
        finish();
    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        // Check email verification status
                        if (!user.isEmailVerified()) {
                            Toast.makeText(LoginActivity.this, "Please verify your email address. A verification link has been sent.", Toast.LENGTH_LONG).show();
                            user.sendEmailVerification();
                            mAuth.signOut();
                            return;
                        }

                        // Fetch user profile from Firestore
                        db.collection("Users")
                                .document(user.getUid())
                                .get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (documentSnapshot.exists()) {
                                        String userName = documentSnapshot.getString("name");
                                        String userEmail = documentSnapshot.getString("email");
                                        String role = documentSnapshot.getString("role");
                                        if (role == null) role = "USER";
                                        Boolean approved = documentSnapshot.getBoolean("approved");
                                        if (approved == null) approved = true;
                                        String base64 = documentSnapshot.getString("profileImageBase64");
                                        if (base64 == null) base64 = "";

                                        handleUserLogin(user, userName, userEmail, role, approved, base64);
                                    } else {
                                        Toast.makeText(LoginActivity.this, "User profile not found in database.", Toast.LENGTH_LONG).show();
                                        mAuth.signOut();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(LoginActivity.this, "Failed to retrieve profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    mAuth.signOut();
                                });
                    }
                } else {
                    String errorMsg = task.getException() != null ? task.getException().getMessage() : "Authentication failed.";
                    Toast.makeText(LoginActivity.this, "Login failed: " + errorMsg, Toast.LENGTH_LONG).show();
                }
            });
    }
}