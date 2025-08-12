package com.example.stepnotev2;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity {

    private LinearLayout navHome, navFlashcards, navAudioNotes, navProfile;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initViews();
        setupNavigation();

        // Load default fragment (Home)
        loadFragment(new HomeFragment());
        setActiveNavigation(navHome);
    }

    private void initViews() {
        navHome = findViewById(R.id.navHome);
        navFlashcards = findViewById(R.id.navFlashcards);
        navAudioNotes = findViewById(R.id.navAudioNotes);
        navProfile = findViewById(R.id.navProfile);

        fragmentManager = getSupportFragmentManager();
    }

    private void setupNavigation() {
        navHome.setOnClickListener(v -> {
            loadFragment(new HomeFragment());
            setActiveNavigation(navHome);
        });

        navFlashcards.setOnClickListener(v -> {
            loadFragment(new FlashcardsFragment());
            setActiveNavigation(navFlashcards);
        });

        navAudioNotes.setOnClickListener(v -> {
            loadFragment(new AudioNotesFragment());
            setActiveNavigation(navAudioNotes);
        });

        navProfile.setOnClickListener(v -> {
            loadFragment(new ProfileFragment());
            setActiveNavigation(navProfile);
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    private void setActiveNavigation(LinearLayout activeNav) {
        // Reset all navigation items
        resetNavigation();

        // Set active state for selected navigation
        activeNav.setAlpha(1.0f);
    }

    private void resetNavigation() {
        navHome.setAlpha(0.6f);
        navFlashcards.setAlpha(0.6f);
        navAudioNotes.setAlpha(0.6f);
        navProfile.setAlpha(0.6f);
    }

    // Public methods for navigation from HomeFragment
    public void switchToFlashcards() {
        loadFragment(new FlashcardsFragment());
        setActiveNavigation(navFlashcards);
    }

    public void switchToAudioNotes() {
        loadFragment(new AudioNotesFragment());
        setActiveNavigation(navAudioNotes);
    }

    public void switchToProfile() {
        loadFragment(new ProfileFragment());
        setActiveNavigation(navProfile);
    }
}