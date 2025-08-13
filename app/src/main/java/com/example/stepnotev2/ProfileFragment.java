package com.example.stepnotev2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ProfileFragment extends Fragment {

    private ImageView profileImageView, btnChangeProfilePicture;
    private TextView tvUserName, tvUserJoinDate;
    private LinearLayout btnChangeInformation, btnSignOut;

    // Database helper
    private DatabaseHelper databaseHelper;
    private User currentUser;

    // Image picker launcher
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<String> permissionLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        databaseHelper = new DatabaseHelper(getContext());

        // Check if user is logged in
        currentUser = databaseHelper.getCurrentLoggedInUser();
        if (currentUser == null) {
            // No user logged in, redirect to sign in
            redirectToSignIn();
            return view;
        }

        initViews(view);
        setupImagePickerLaunchers();
        loadUserData();
        setupClickListeners();

        return view;
    }

    private void redirectToSignIn() {
        Toast.makeText(getContext(), "Please sign in to continue", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getContext(), SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    private void initViews(View view) {
        profileImageView = view.findViewById(R.id.profileImageView);
        btnChangeProfilePicture = view.findViewById(R.id.btnChangeProfilePicture);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserJoinDate = view.findViewById(R.id.tvUserJoinDate);
        btnChangeInformation = view.findViewById(R.id.btnChangeInformation);
        btnSignOut = view.findViewById(R.id.btnSignOut);
    }

    private void setupImagePickerLaunchers() {
        // Image picker launcher
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            handleSelectedImage(imageUri);
                        }
                    }
                }
        );

        // Permission launcher
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openImagePicker();
                    } else {
                        Toast.makeText(getContext(), "Permission required to access images", Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void loadUserData() {
        // Refresh current user data from database
        currentUser = databaseHelper.getCurrentLoggedInUser();

        if (currentUser != null) {
            // Set user data to views
            tvUserName.setText(currentUser.getName());

            // Handle join date with fallback
            String joinDate = currentUser.getJoinDate();
            if (joinDate != null && !joinDate.isEmpty()) {
                // Format the date nicely if it contains time
                if (joinDate.contains(" ")) {
                    joinDate = joinDate.split(" ")[0]; // Get just the date part (YYYY-MM-DD)
                }
                tvUserJoinDate.setText("Member since " + joinDate);
            } else {
                // Fallback to current date if no join date
                String currentDate = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
                tvUserJoinDate.setText("Member since " + currentDate);
            }

            // Load profile image
            loadProfileImage(currentUser.getProfileImagePath());

            android.util.Log.d("ProfileFragment", "User data loaded: " + currentUser.toString());
        } else {
            android.util.Log.e("ProfileFragment", "No current user found");
            redirectToSignIn();
        }
    }

    private void loadProfileImage(String imagePath) {
        if (imagePath != null && !imagePath.isEmpty()) {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                try {
                    Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                    if (bitmap != null) {
                        // Apply circular crop to loaded image
                        Bitmap circularBitmap = getCircularBitmap(bitmap);
                        profileImageView.setImageBitmap(circularBitmap);
                        android.util.Log.d("ProfileFragment", "Profile image loaded from: " + imagePath);
                        return;
                    }
                } catch (Exception e) {
                    android.util.Log.e("ProfileFragment", "Error loading profile image: " + e.getMessage());
                }
            }
        }

        // Set default profile image
        profileImageView.setImageResource(R.drawable.ic_profile_default);
        android.util.Log.d("ProfileFragment", "Using default profile image");
    }

    private void setupClickListeners() {
        // Camera button click listener
        btnChangeProfilePicture.setOnClickListener(v -> checkPermissionAndOpenImagePicker());

        btnChangeInformation.setOnClickListener(v -> showChangeInformationDialog());

        btnSignOut.setOnClickListener(v -> showSignOutConfirmationDialog());
    }

    private void checkPermissionAndOpenImagePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            } else {
                openImagePicker();
            }
        } else {
            // Below Android 13 uses READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            } else {
                openImagePicker();
            }
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Add multiple MIME types for better compatibility
        String[] mimeTypes = {"image/jpeg", "image/png", "image/jpg", "image/webp"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        try {
            imagePickerLauncher.launch(Intent.createChooser(intent, "Select Profile Picture"));
        } catch (Exception e) {
            Toast.makeText(getContext(), "No image picker found", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSelectedImage(Uri imageUri) {
        if (currentUser == null) {
            Toast.makeText(getContext(), "User session expired. Please sign in again.", Toast.LENGTH_SHORT).show();
            redirectToSignIn();
            return;
        }

        try {
            // Load and process the image
            InputStream inputStream = getContext().getContentResolver().openInputStream(imageUri);
            if (inputStream != null) {
                Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();

                if (originalBitmap != null) {
                    // Resize the image first
                    Bitmap resizedBitmap = resizeImage(originalBitmap, 512, 512);

                    // Apply circular crop
                    Bitmap circularBitmap = getCircularBitmap(resizedBitmap);

                    // Save the circular image
                    String savedImagePath = saveCircularImageToInternalStorage(circularBitmap);

                    if (savedImagePath != null) {
                        // Update profile image view
                        profileImageView.setImageBitmap(circularBitmap);

                        // Update in database
                        int result = databaseHelper.updateUserProfile(currentUser.getId(),
                                currentUser.getName(), currentUser.getEmail(), savedImagePath);

                        if (result > 0) {
                            // Update current user object
                            currentUser.setProfileImagePath(savedImagePath);
                            Toast.makeText(getContext(), "Profile picture updated! 📸✨", Toast.LENGTH_SHORT).show();
                            android.util.Log.d("ProfileFragment", "Profile image updated in database: " + savedImagePath);
                        } else {
                            Toast.makeText(getContext(), "Error updating profile picture in database", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Toast.makeText(getContext(), "Error saving profile picture", Toast.LENGTH_SHORT).show();
                    }

                    // Clean up bitmaps
                    if (originalBitmap != resizedBitmap) {
                        originalBitmap.recycle();
                    }
                    if (resizedBitmap != circularBitmap) {
                        resizedBitmap.recycle();
                    }

                } else {
                    Toast.makeText(getContext(), "Error loading selected image", Toast.LENGTH_SHORT).show();
                }
            }

        } catch (Exception e) {
            Toast.makeText(getContext(), "Error updating profile picture: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private Bitmap getCircularBitmap(Bitmap bitmap) {
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());

        // Create a square bitmap
        Bitmap squareBitmap = Bitmap.createBitmap(bitmap,
                (bitmap.getWidth() - size) / 2,
                (bitmap.getHeight() - size) / 2,
                size, size);

        // Create circular bitmap
        Bitmap circularBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(circularBitmap);

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // Draw circle
        float radius = size / 2f;
        canvas.drawCircle(radius, radius, radius, paint);

        // Apply source image with circular mask
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(squareBitmap, 0, 0, paint);

        // Clean up
        if (squareBitmap != bitmap) {
            squareBitmap.recycle();
        }

        return circularBitmap;
    }

    private String saveCircularImageToInternalStorage(Bitmap circularBitmap) {
        try {
            // Create profile images directory
            File profileDir = new File(getContext().getFilesDir(), "profile_images");
            if (!profileDir.exists()) {
                profileDir.mkdirs();
            }

            // Delete old profile image if exists
            if (currentUser != null && currentUser.getProfileImagePath() != null) {
                try {
                    File oldImageFile = new File(currentUser.getProfileImagePath());
                    if (oldImageFile.exists()) {
                        oldImageFile.delete();
                        android.util.Log.d("ProfileFragment", "Old profile image deleted");
                    }
                } catch (Exception e) {
                    android.util.Log.e("ProfileFragment", "Error deleting old profile image: " + e.getMessage());
                }
            }

            // Create unique filename
            String timestamp = String.valueOf(System.currentTimeMillis());
            String fileName = "profile_circular_" + currentUser.getId() + "_" + timestamp + ".png";
            File destinationFile = new File(profileDir, fileName);

            // Save as PNG to preserve transparency
            try (FileOutputStream outputStream = new FileOutputStream(destinationFile)) {
                boolean saved = circularBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);

                if (saved) {
                    android.util.Log.d("ProfileFragment", "Profile image saved to: " + destinationFile.getAbsolutePath());
                    return destinationFile.getAbsolutePath();
                }
            }
        } catch (Exception e) {
            android.util.Log.e("ProfileFragment", "Error saving profile image: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private Bitmap resizeImage(Bitmap originalBitmap, int maxWidth, int maxHeight) {
        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();

        // Calculate scale factor
        float scaleWidth = ((float) maxWidth) / width;
        float scaleHeight = ((float) maxHeight) / height;
        float scale = Math.min(scaleWidth, scaleHeight);

        // Calculate new dimensions
        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);

        // Create resized bitmap
        return Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);
    }

    private void showChangeInformationDialog() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "User session expired. Please sign in again.", Toast.LENGTH_SHORT).show();
            redirectToSignIn();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Change Information");

        // Create dialog layout
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);

        // Name input
        EditText nameInput = new EditText(getContext());
        nameInput.setHint("Enter your name");
        nameInput.setText(currentUser.getName());
        nameInput.setMinLines(1);
        layout.addView(nameInput);

        // Email input
        EditText emailInput = new EditText(getContext());
        emailInput.setHint("Enter your email");
        emailInput.setText(currentUser.getEmail());
        emailInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailInput.setMinLines(1);
        emailInput.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        ((LinearLayout.LayoutParams) emailInput.getLayoutParams()).topMargin = 20;
        layout.addView(emailInput);

        // Password input
        EditText passwordInput = new EditText(getContext());
        passwordInput.setHint("Enter new password (optional)");
        passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setMinLines(1);
        passwordInput.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        ((LinearLayout.LayoutParams) passwordInput.getLayoutParams()).topMargin = 20;
        layout.addView(passwordInput);

        builder.setView(layout);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newName = nameInput.getText().toString().trim();
            String newEmail = emailInput.getText().toString().trim();
            String newPassword = passwordInput.getText().toString().trim();

            // Validate input
            if (newName.isEmpty()) {
                Toast.makeText(getContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newEmail.isEmpty()) {
                Toast.makeText(getContext(), "Email cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                Toast.makeText(getContext(), "Please enter a valid email", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if email is taken by another user
            if (!newEmail.equals(currentUser.getEmail()) && databaseHelper.isEmailExists(newEmail)) {
                Toast.makeText(getContext(), "Email already exists. Please use a different email.", Toast.LENGTH_LONG).show();
                return;
            }

            // Update user information in database
            int result = databaseHelper.updateUserProfile(currentUser.getId(), newName, newEmail, currentUser.getProfileImagePath());

            if (!newPassword.isEmpty()) {
                if (newPassword.length() < 6) {
                    Toast.makeText(getContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                    return;
                }
                int passwordResult = databaseHelper.updateUserPassword(currentUser.getId(), newPassword);
                android.util.Log.d("ProfileFragment", "Password update result: " + passwordResult);
            }

            if (result > 0) {
                // Update current user object
                currentUser.setName(newName);
                currentUser.setEmail(newEmail);

                Toast.makeText(getContext(), "Information updated successfully! ✅", Toast.LENGTH_SHORT).show();
                loadUserData(); // Refresh displayed data

                android.util.Log.d("ProfileFragment", "User information updated: " + currentUser.toString());
            } else {
                Toast.makeText(getContext(), "Error updating information", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showSignOutConfirmationDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?\n\nYour data will be saved and you can sign in again later.")
                .setIcon(R.drawable.ic_warning)
                .setPositiveButton("Sign Out", (dialog, which) -> {
                    signOutUser();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void signOutUser() {
        if (currentUser != null) {
            // Update user login status in database
            databaseHelper.updateUserLoginStatus(currentUser.getId(), false);
            android.util.Log.d("ProfileFragment", "User signed out: " + currentUser.getEmail());
        }

        // Clear current user
        currentUser = null;

        Toast.makeText(getContext(), "Signed out successfully! 👋", Toast.LENGTH_SHORT).show();

        // Navigate to sign in screen
        Intent intent = new Intent(getContext(), SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when returning to this fragment
        if (currentUser != null) {
            loadUserData();
        } else {
            // Check if user is still logged in
            currentUser = databaseHelper.getCurrentLoggedInUser();
            if (currentUser == null) {
                redirectToSignIn();
            } else {
                loadUserData();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}