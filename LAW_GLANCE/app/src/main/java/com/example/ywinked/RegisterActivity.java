package com.example.ywinked;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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

import java.util.HashMap;
import java.util.Map;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {
    private EditText nameInput, emailInput, passwordInput, confirmPasswordInput;
    private RadioGroup roleRadioGroup;
    private LinearLayout layoutLawyerFields;
    private EditText lawyerSpecialization, lawyerExperience, lawyerCity, lawyerFee, lawyerBarNumber, lawyerAbout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private androidx.activity.result.ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        nameInput = findViewById(R.id.registerUsername);  
        emailInput = findViewById(R.id.registerEmail);    
        passwordInput = findViewById(R.id.registerPassword);
        confirmPasswordInput = findViewById(R.id.registerConfirmPassword);
        Button registerBtn = findViewById(R.id.registerBtn);
        TextView loginLink = findViewById(R.id.loginLink);

        roleRadioGroup = findViewById(R.id.roleRadioGroup);
        layoutLawyerFields = findViewById(R.id.layoutLawyerFields);
        lawyerSpecialization = findViewById(R.id.lawyerSpecialization);
        lawyerExperience = findViewById(R.id.lawyerExperience);
        lawyerCity = findViewById(R.id.lawyerCity);
        lawyerFee = findViewById(R.id.lawyerFee);
        lawyerBarNumber = findViewById(R.id.lawyerBarNumber);
        lawyerAbout = findViewById(R.id.lawyerAbout);

        // Toggle lawyer fields visibility
        roleRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioLawyer) {
                layoutLawyerFields.setVisibility(View.VISIBLE);
            } else {
                layoutLawyerFields.setVisibility(View.GONE);
            }
        });

        // Set OnClickListener for login link
        loginLink.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        // Set OnClickListener for register button
        registerBtn.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            String confirmPassword = confirmPasswordInput.getText().toString().trim();

            // Validate inputs
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get selected role
            String role = "USER";
            int checkedId = roleRadioGroup.getCheckedRadioButtonId();
            if (checkedId == R.id.radioLawyer) {
                role = "LAWYER";
            } else if (checkedId == R.id.radioAdmin) {
                role = "ADMIN";
            }

            // If lawyer, validate extra fields
            if (role.equals("LAWYER")) {
                if (lawyerSpecialization.getText().toString().trim().isEmpty() ||
                        lawyerExperience.getText().toString().trim().isEmpty() ||
                        lawyerCity.getText().toString().trim().isEmpty() ||
                        lawyerFee.getText().toString().trim().isEmpty() ||
                        lawyerBarNumber.getText().toString().trim().isEmpty() ||
                        lawyerAbout.getText().toString().trim().isEmpty()) {
                    Toast.makeText(this, "Please fill in all lawyer details", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            registerUser(name, email, password, role);
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
                            Toast.makeText(RegisterActivity.this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        android.view.View googleRegisterBtn = findViewById(R.id.googleRegisterBtn);
        googleRegisterBtn.setOnClickListener(v -> signIn());
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
                        Toast.makeText(RegisterActivity.this, "Google auth failed: " + errorMsg, Toast.LENGTH_SHORT).show();
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

                        handleUserLogin(user, userName, userEmail, role, approved);
                    } else {
                        // Get selected role from screen
                        String selectedRole = "USER";
                        int checkedId = roleRadioGroup.getCheckedRadioButtonId();
                        if (checkedId == R.id.radioLawyer) {
                            selectedRole = "LAWYER";
                        } else if (checkedId == R.id.radioAdmin) {
                            selectedRole = "ADMIN";
                        }

                        // If Lawyer, verify extra fields are filled
                        if (selectedRole.equals("LAWYER")) {
                            if (lawyerSpecialization.getText().toString().trim().isEmpty() ||
                                    lawyerExperience.getText().toString().trim().isEmpty() ||
                                    lawyerCity.getText().toString().trim().isEmpty() ||
                                    lawyerFee.getText().toString().trim().isEmpty() ||
                                    lawyerBarNumber.getText().toString().trim().isEmpty() ||
                                    lawyerAbout.getText().toString().trim().isEmpty()) {
                                Toast.makeText(RegisterActivity.this, "Please register with email/password to complete lawyer details, or select User role to sign up with Google.", Toast.LENGTH_LONG).show();
                                return;
                            }
                        }

                        registerNewGoogleUser(user, selectedRole);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, "Failed to check user profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void registerNewGoogleUser(FirebaseUser user, String role) {
        String displayName = user.getDisplayName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = "Google User";
        }
        String email = user.getEmail();

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", displayName);
        userData.put("email", email);
        userData.put("role", role);
        userData.put("uid", user.getUid());

        if (role.equals("LAWYER")) {
            userData.put("specialization", lawyerSpecialization.getText().toString().trim());
            userData.put("experience", lawyerExperience.getText().toString().trim());
            userData.put("city", lawyerCity.getText().toString().trim());
            userData.put("fee", lawyerFee.getText().toString().trim());
            userData.put("barCouncilNumber", lawyerBarNumber.getText().toString().trim());
            userData.put("about", lawyerAbout.getText().toString().trim());
            userData.put("approved", false); // Requires admin approval!
        } else {
            userData.put("approved", true); // Clients and Admins don't need approval
        }

        final String finalDisplayName = displayName;
        final String finalEmail = email;
        final String finalRole = role;
        final boolean finalApproved = !role.equals("LAWYER");

        db.collection("Users")
                .document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                    handleUserLogin(user, finalDisplayName, finalEmail, finalRole, finalApproved);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void handleUserLogin(FirebaseUser user, String userName, String userEmail, String role, boolean approved) {
        // Save to SharedPreferences
        android.content.SharedPreferences sharedPref = getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("email", userEmail);
        editor.putString("name", userName);
        editor.putString("uid", user.getUid());
        editor.putString("role", role);
        editor.putBoolean("isLoggedIn", true);
        editor.apply();

        Toast.makeText(RegisterActivity.this, "Welcome " + userName + "!", Toast.LENGTH_SHORT).show();

        // Routing based on role
        Intent intent;
        if (role.equals("LAWYER")) {
            if (approved) {
                intent = new Intent(RegisterActivity.this, LawyerDashboardActivity.class);
            } else {
                intent = new Intent(RegisterActivity.this, WaitingApprovalActivity.class);
            }
        } else if (role.equals("ADMIN")) {
            intent = new Intent(RegisterActivity.this, AdminDashboardActivity.class);
        } else {
            intent = new Intent(RegisterActivity.this, WelcomeActivity.class);
        }
        startActivity(intent);
        finish();
    }

    private void registerUser(String name, String email, String password, String role) {
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("name", name);
                        userData.put("email", email);
                        userData.put("role", role);
                        userData.put("uid", user.getUid());

                        if (role.equals("LAWYER")) {
                            userData.put("specialization", lawyerSpecialization.getText().toString().trim());
                            userData.put("experience", lawyerExperience.getText().toString().trim());
                            userData.put("city", lawyerCity.getText().toString().trim());
                            userData.put("fee", lawyerFee.getText().toString().trim());
                            userData.put("barCouncilNumber", lawyerBarNumber.getText().toString().trim());
                            userData.put("about", lawyerAbout.getText().toString().trim());
                            userData.put("approved", false); // Requires admin approval!
                        } else {
                            userData.put("approved", true); // Clients and Admins don't need approval
                        }

                        db.collection("Users")
                                .document(user.getUid())
                                .set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    user.sendEmailVerification()
                                            .addOnCompleteListener(emailtask -> {
                                                if (emailtask.isSuccessful()) {
                                                    Toast.makeText(RegisterActivity.this,
                                                            "Registration Successful! Verification email sent.",
                                                            Toast.LENGTH_LONG).show();
                                                } else {
                                                    Toast.makeText(RegisterActivity.this,
                                                            "Error sending verification: " + emailtask.getException().getMessage(),
                                                            Toast.LENGTH_LONG).show();
                                                }
                                                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                                startActivity(intent);
                                                finish();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(RegisterActivity.this,
                                            "Failed to save user data: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                    }
                } else {
                    Toast.makeText(RegisterActivity.this,
                            "Registration failed: " + task.getException().getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            });
    }
}
