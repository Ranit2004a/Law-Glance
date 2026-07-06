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

public class UserSettingsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userName = "";
    private String userEmail = "";

    private ImageView tempDialogImageView;
    private Uri chosenImageUri;

    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_settings);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        SharedPreferences sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        userName = sharedPref.getString("name", "User");
        userEmail = sharedPref.getString("email", "");

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedUri = result.getData().getData();
                        if (selectedUri != null) {
                            chosenImageUri = selectedUri;
                            if (tempDialogImageView != null) {
                                tempDialogImageView.setImageURI(selectedUri);
                                tempDialogImageView.setImageTintList(null);
                            }
                        }
                    }
                }
        );

        findViewById(R.id.btnSettingsBack).setOnClickListener(v -> finish());

        findViewById(R.id.itemProfile).setOnClickListener(v -> showEditProfileBottomSheet());

        findViewById(R.id.itemPrivacy).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Privacy Settings")
                    .setMessage("Your account is secured. Blocked contacts and disappearing messages can be configured here in a future update.")
                    .setPositiveButton("OK", null)
                    .show();
        });

        findViewById(R.id.itemNotifications).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Notifications Preferences")
                    .setMessage("Inbound appointment notifications and system sounds are enabled by default.")
                    .setPositiveButton("OK", null)
                    .show();
        });

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
        
        // Show phone details container
        View layoutPhoneDetails = view.findViewById(R.id.layoutPhoneDetails);
        layoutPhoneDetails.setVisibility(View.VISIBLE);
        EditText etEditProfilePhone = view.findViewById(R.id.etEditProfilePhone);

        View btnCancel = view.findViewById(R.id.btnCancelEdit);
        View btnSave = view.findViewById(R.id.btnSaveEdit);

        etEditProfileName.setText(userName);
        chosenImageUri = null;

        // Load existing phone from Firestore in real-time
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("Users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            etEditProfilePhone.setText(documentSnapshot.getString("phone"));
                        }
                    });
        }

        SharedPreferences sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        File file = new File(getFilesDir(), "user_profile_picture.jpg");
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

        layoutEditAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newName = etEditProfileName.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            String phone = etEditProfilePhone.getText().toString().trim();
            saveProfileChanges(newName, phone, dialog);
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private void saveProfileChanges(String newName, String phone, BottomSheetDialog dialog) {
        SharedPreferences sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        boolean imageProcessed = false;

        if (chosenImageUri != null) {
            try {
                InputStream is = getContentResolver().openInputStream(chosenImageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                if (bitmap != null) {
                    Bitmap scaled = scaleBitmap(bitmap, 256);

                    File file = new File(getFilesDir(), "user_profile_picture.jpg");
                    FileOutputStream fos = new FileOutputStream(file);
                    scaled.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                    fos.flush();
                    fos.close();

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

        editor.putString("name", newName);
        editor.apply();
        userName = newName;

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", newName);
            updates.put("phone", phone);
            if (imageProcessed) {
                String base64Image = sharedPref.getString("profile_image_base64", "");
                updates.put("profileImageBase64", base64Image);
            }

            db.collection("Users").document(user.getUid())
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(UserSettingsActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(UserSettingsActivity.this, "Failed to sync: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

        File file = new File(getFilesDir(), "user_profile_picture.jpg");
        if (file.exists()) {
            file.delete();
        }

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(UserSettingsActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
