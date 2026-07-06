package com.example.ywinked;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class LawyerSettingsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String lawyerName = "";
    private String lawyerEmail = "";

    // Dialog image reference for gallery callback
    private ImageView tempDialogImageView;
    private Uri chosenImageUri;

    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lawyer_settings);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        SharedPreferences sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        lawyerName = sharedPref.getString("name", "Lawyer");
        lawyerEmail = sharedPref.getString("email", "");

        // Initialize gallery picker launcher
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedUri = result.getData().getData();
                        if (selectedUri != null) {
                            chosenImageUri = selectedUri;
                            if (tempDialogImageView != null) {
                                tempDialogImageView.setImageURI(selectedUri);
                                tempDialogImageView.setImageTintList(null); // clear default tint
                            }
                        }
                    }
                }
        );

        // Back Button
        findViewById(R.id.btnSettingsBack).setOnClickListener(v -> finish());

        // 1. Profile click - Edit Name & Choose Picture from Gallery
        findViewById(R.id.itemProfile).setOnClickListener(v -> showEditProfileBottomSheet());

        // 2. Privacy click
        findViewById(R.id.itemPrivacy).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Privacy Settings")
                    .setMessage("Your account is secured. Blocked contacts and disappearing messages can be configured here in a future update.")
                    .setPositiveButton("OK", null)
                    .show();
        });

        // 3. Notifications click
        findViewById(R.id.itemNotifications).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Notifications Preferences")
                    .setMessage("Inbound appointment notifications and system sounds are enabled by default.")
                    .setPositiveButton("OK", null)
                    .show();
        });

        // 4. Log out click
        findViewById(R.id.itemLogout).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Confirm Log Out")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Log Out", (dialog, which) -> logoutUser())
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void showEditProfileBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);

        tempDialogImageView = view.findViewById(R.id.ivEditProfilePic);
        View layoutEditAvatar = view.findViewById(R.id.layoutEditAvatar);
        EditText etEditProfileName = view.findViewById(R.id.etEditProfileName);
        
        // Show Lawyer details container and get fields
        View layoutLawyerDetails = view.findViewById(R.id.layoutLawyerDetails);
        layoutLawyerDetails.setVisibility(View.VISIBLE);
        EditText etEditSpecialization = view.findViewById(R.id.etEditSpecialization);
        EditText etEditExperience = view.findViewById(R.id.etEditExperience);
        EditText etEditCity = view.findViewById(R.id.etEditCity);
        EditText etEditFee = view.findViewById(R.id.etEditFee);
        EditText etEditBarNumber = view.findViewById(R.id.etEditBarNumber);
        EditText etEditAbout = view.findViewById(R.id.etEditAbout);

        View btnCancel = view.findViewById(R.id.btnCancelEdit);
        View btnSave = view.findViewById(R.id.btnSaveEdit);

        etEditProfileName.setText(lawyerName);
        chosenImageUri = null;

        // Query Firestore to load existing lawyer details in real-time
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("Users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            etEditSpecialization.setText(documentSnapshot.getString("specialization"));
                            etEditExperience.setText(documentSnapshot.getString("experience"));
                            etEditCity.setText(documentSnapshot.getString("city"));
                            etEditFee.setText(documentSnapshot.getString("fee"));
                            etEditBarNumber.setText(documentSnapshot.getString("barCouncilNumber"));
                            etEditAbout.setText(documentSnapshot.getString("about"));
                        }
                    });
        }

        // Load existing image if any
        SharedPreferences sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        File file = new File(getFilesDir(), "profile_picture.jpg");
        if (file.exists()) {
            tempDialogImageView.setImageURI(Uri.fromFile(file));
            tempDialogImageView.setImageTintList(null);
        } else {
            String base64Image = sharedPref.getString("profile_image_base64", "");
            if (!base64Image.isEmpty()) {
                try {
                    byte[] decoded = Base64.decode(base64Image, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                    tempDialogImageView.setImageBitmap(bitmap);
                    tempDialogImageView.setImageTintList(null);
                } catch (Exception e) {
                    tempDialogImageView.setImageResource(R.drawable.ic_profile_vector);
                    tempDialogImageView.setImageTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#8E8E9F")));
                }
            } else {
                tempDialogImageView.setImageResource(R.drawable.ic_profile_vector);
                tempDialogImageView.setImageTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#8E8E9F")));
            }
        }

        // Image container click - launch gallery
        layoutEditAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        // Cancel
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Save Changes
        btnSave.setOnClickListener(v -> {
            String newName = etEditProfileName.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            String specialization = etEditSpecialization.getText().toString().trim();
            String experience = etEditExperience.getText().toString().trim();
            String city = etEditCity.getText().toString().trim();
            String fee = etEditFee.getText().toString().trim();
            String barNumber = etEditBarNumber.getText().toString().trim();
            String about = etEditAbout.getText().toString().trim();

            saveProfileChanges(newName, specialization, experience, city, fee, barNumber, about, dialog);
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private void saveProfileChanges(String newName, String specialization, String experience, String city, String fee, String barNumber, String about, BottomSheetDialog dialog) {
        SharedPreferences sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        boolean imageProcessed = false;

        if (chosenImageUri != null) {
            try {
                InputStream is = getContentResolver().openInputStream(chosenImageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                if (bitmap != null) {
                    Bitmap scaled = scaleBitmap(bitmap, 256);

                    // 1. Save locally as file
                    File file = new File(getFilesDir(), "profile_picture.jpg");
                    FileOutputStream fos = new FileOutputStream(file);
                    scaled.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                    fos.flush();
                    fos.close();

                    // 2. Convert to Base64 for syncing/sharedPref
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    scaled.compress(Bitmap.CompressFormat.JPEG, 90, baos);
                    byte[] bytes = baos.toByteArray();
                    String base64Image = Base64.encodeToString(bytes, Base64.DEFAULT);

                    editor.putString("profile_image_base64", base64Image);
                    imageProcessed = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to process image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        // Update name in shared preferences
        editor.putString("name", newName);
        editor.apply();
        lawyerName = newName;

        // Sync to Firestore
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", newName);
            updates.put("specialization", specialization);
            updates.put("experience", experience);
            updates.put("city", city);
            updates.put("fee", fee);
            updates.put("barCouncilNumber", barNumber);
            updates.put("about", about);
            if (imageProcessed) {
                String base64Image = sharedPref.getString("profile_image_base64", "");
                updates.put("profileImageBase64", base64Image);
            }

            db.collection("Users").document(user.getUid())
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(LawyerSettingsActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(LawyerSettingsActivity.this, "Failed to sync online: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
        } else {
            dialog.dismiss();
        }
    }

    private Bitmap scaleBitmap(Bitmap bitmap, int maxDimension) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float ratio = (float) width / (float) height;
        int newWidth = maxDimension;
        int newHeight = maxDimension;
        if (ratio > 1.0f) {
            newHeight = (int) ((float) maxDimension / ratio);
        } else {
            newWidth = (int) ((float) maxDimension * ratio);
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private void logoutUser() {
        mAuth.signOut();

        SharedPreferences sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.apply();

        // Also delete profile picture file on logout
        File file = new File(getFilesDir(), "profile_picture.jpg");
        if (file.exists()) {
            file.delete();
        }

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(LawyerSettingsActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
