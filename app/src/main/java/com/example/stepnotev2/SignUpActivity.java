package com.example.stepnotev2;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SignUpActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword;
    private LinearLayout btnSignUp;
    private ImageView btnBack;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        databaseHelper = new DatabaseHelper(this);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupClickListeners() {
        btnSignUp.setOnClickListener(v -> performSignUp());

        btnBack.setOnClickListener(v -> {
            finish(); // Go back to sign in
        });
    }

    private void performSignUp() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(name)) {
            etName.setError("Name is required");
            etName.requestFocus();
            return;
        }

        if (name.length() < 2) {
            etName.setError("Name must be at least 2 characters");
            etName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        // Check if email already exists
        if (databaseHelper.isEmailExists(email)) {
            etEmail.setError("Email already exists. Please use a different email.");
            etEmail.requestFocus();
            return;
        }

        // Register user using the correct method name
        long userId = databaseHelper.addUser(name, email, password);

        if (userId != -1) {
            // After successful registration, automatically log the user in
            User newUser = databaseHelper.loginUser(email, password);

            if (newUser != null) {
                Toast.makeText(this, "Account created successfully! Welcome, " + name + "! 🎉", Toast.LENGTH_LONG).show();
                navigateToMain();
            } else {
                // Fallback: Registration succeeded but login failed
                Toast.makeText(this, "Account created! Please sign in to continue.", Toast.LENGTH_LONG).show();
                navigateToSignIn();
            }
        } else {
            Toast.makeText(this, "Failed to create account. Please try again.", Toast.LENGTH_LONG).show();
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToSignIn() {
        Intent intent = new Intent(this, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}