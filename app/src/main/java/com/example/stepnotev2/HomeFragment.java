package com.example.stepnotev2;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private static final int PERMISSION_REQUEST_ACTIVITY_RECOGNITION = 1001;

    private TextView tvWelcomeMessage;
    private TextView tvDailyStepsCount;
    private TextView tvLearningDays;
    private TextView tvTotalStepsCount;
    private ProgressBar progressBarSteps;
    private LinearLayout sectionFlashcards, sectionAudioNotes;

    private DatabaseHelper databaseHelper;
    private StepCountReceiver stepCountReceiver;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        databaseHelper = new DatabaseHelper(getContext());

        initViews(view);
        requestActivityRecognitionPermission();
        loadUserWelcomeMessage();
        loadStepData();
        setupClickListeners();

        return view;
    }

    private void initViews(View view) {
        tvWelcomeMessage = view.findViewById(R.id.tvWelcomeMessage);
        tvDailyStepsCount = view.findViewById(R.id.tvDailyStepsCount);
        tvLearningDays = view.findViewById(R.id.tvLearningDays);
        tvTotalStepsCount = view.findViewById(R.id.tvTotalStepsCount);
        progressBarSteps = view.findViewById(R.id.progressBarSteps);
        sectionFlashcards = view.findViewById(R.id.sectionFlashcards);
        sectionAudioNotes = view.findViewById(R.id.sectionAudioNotes);
    }

    private void requestActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACTIVITY_RECOGNITION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                        PERMISSION_REQUEST_ACTIVITY_RECOGNITION);
            } else {
                startStepCounterService();
            }
        } else {
            startStepCounterService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_ACTIVITY_RECOGNITION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startStepCounterService();
            }
        }
    }

    private void startStepCounterService() {
        try {
            Intent serviceIntent = new Intent(getContext(), StepCounterService.class);
            getContext().startService(serviceIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error starting step counter service: " + e.getMessage());
        }
    }

    private void loadUserWelcomeMessage() {
        User currentUser = databaseHelper.getCurrentLoggedInUser();

        if (currentUser != null) {
            String fullName = currentUser.getName();
            String firstName = getFirstName(fullName);
            String welcomeMessage = "Welcome back! " + firstName;
            tvWelcomeMessage.setText(welcomeMessage);
        } else {
            tvWelcomeMessage.setText("Welcome back!");
        }
    }

    private void loadStepData() {
        User currentUser = databaseHelper.getCurrentLoggedInUser();
        if (currentUser != null) {
            try {
                // Load today's steps
                int todaySteps = databaseHelper.getTodaySteps(currentUser.getId());
                int todayGoal = databaseHelper.getTodayGoal(currentUser.getId());

                // Update step count display
                tvDailyStepsCount.setText(String.valueOf(todaySteps));

                // Update progress bar
                int progress = (int) ((float) todaySteps / todayGoal * 100);
                progressBarSteps.setProgress(Math.min(progress, 100));

                // Load user statistics
                DatabaseHelper.UserStats stats = databaseHelper.getUserStats(currentUser.getId());
                tvLearningDays.setText(String.valueOf(stats.totalDays));
                tvTotalStepsCount.setText(String.valueOf(stats.totalSteps));
            } catch (Exception e) {
                Log.e(TAG, "Error loading step data: " + e.getMessage());
            }
        }
    }

    private String getFirstName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "";
        }
        String[] nameParts = fullName.trim().split("\\s+");
        return nameParts[0];
    }

    private void setupClickListeners() {
        sectionFlashcards.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToFlashcards();
            }
        });

        sectionAudioNotes.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToAudioNotes();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserWelcomeMessage();
        loadStepData();

        // Register broadcast receiver for step updates with Android 14+ compatibility
        registerStepCountReceiver();
    }

    @Override
    public void onPause() {
        super.onPause();

        // Unregister broadcast receiver safely
        unregisterStepCountReceiver();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Make sure receiver is unregistered
        unregisterStepCountReceiver();

        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }

    /**
     * Safely register the step count receiver with Android 14+ compatibility
     */
    private void registerStepCountReceiver() {
        try {
            // Only register if not already registered
            if (stepCountReceiver == null) {
                stepCountReceiver = new StepCountReceiver();
                IntentFilter filter = new IntentFilter("STEP_COUNT_UPDATED");

                // Android 14+ (API 34+) requires explicit export flag
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    getContext().registerReceiver(stepCountReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
                } else {
                    getContext().registerReceiver(stepCountReceiver, filter);
                }

                Log.d(TAG, "Step count receiver registered successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error registering step count receiver: " + e.getMessage());
        }
    }

    /**
     * Safely unregister the step count receiver
     */
    private void unregisterStepCountReceiver() {
        try {
            if (stepCountReceiver != null && getContext() != null) {
                getContext().unregisterReceiver(stepCountReceiver);
                stepCountReceiver = null;
                Log.d(TAG, "Step count receiver unregistered successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering step count receiver: " + e.getMessage());
            // Set to null anyway to avoid memory leaks
            stepCountReceiver = null;
        }
    }

    // Broadcast receiver for real-time step updates
    private class StepCountReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if ("STEP_COUNT_UPDATED".equals(intent.getAction())) {
                    int stepCount = intent.getIntExtra("step_count", 0);

                    // Update UI with new step count
                    if (tvDailyStepsCount != null) {
                        tvDailyStepsCount.setText(String.valueOf(stepCount));
                    }

                    // Update progress bar
                    User currentUser = databaseHelper.getCurrentLoggedInUser();
                    if (currentUser != null && progressBarSteps != null) {
                        int goal = databaseHelper.getTodayGoal(currentUser.getId());
                        int progress = (int) ((float) stepCount / goal * 100);
                        progressBarSteps.setProgress(Math.min(progress, 100));
                    }

                    // Refresh total stats
                    loadStepData();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing step count update: " + e.getMessage());
            }
        }
    }
}