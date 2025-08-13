package com.example.stepnotev2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AudioNotesFragment extends Fragment {

    private static final String TAG = "AudioNotesFragment";
    private static final int PERMISSION_REQUEST_CODE = 1001;

    // UI Components (matching your XML)
    private LinearLayout audioNotesContainer;
    private LinearLayout btnAddAudioNote;
    private TextView tvAudioNotesCount;

    // Audio recording
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private boolean isRecording = false;
    private boolean isPlaying = false;
    private String currentRecordingPath;

    // Database
    private DatabaseHelper databaseHelper;

    // File picker launcher
    private ActivityResultLauncher<Intent> audioPickerLauncher;

    // Audio notes list
    private List<AudioNote> audioNotesList;
    private AudioNote currentPlayingNote;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_audio_notes, container, false);

        databaseHelper = new DatabaseHelper(getContext());
        audioNotesList = new ArrayList<>();

        initViews(view);
        setupAudioPickerLauncher();
        checkPermissions();
        loadAudioNotes();

        return view;
    }

    private void initViews(View view) {
        // Initialize views that exist in your XML
        audioNotesContainer = view.findViewById(R.id.audioNotesContainer);
        btnAddAudioNote = view.findViewById(R.id.btnAddAudioNote);
        tvAudioNotesCount = view.findViewById(R.id.tvAudioNotesCount);

        // Set click listener for add audio note button
        btnAddAudioNote.setOnClickListener(v -> showAddAudioNoteDialog());
    }

    private void setupAudioPickerLauncher() {
        audioPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        Uri audioUri = result.getData().getData();
                        if (audioUri != null) {
                            importSelectedAudioFile(audioUri);
                        }
                    }
                }
        );
    }

    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };

        List<String> missingPermissions = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(getContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }

        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(getActivity(),
                    missingPermissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                Toast.makeText(getContext(), "Permissions required for audio recording", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showAddAudioNoteDialog() {
        String[] options = {"Record Audio", "Import Audio File"};

        new AlertDialog.Builder(getContext())
                .setTitle("Add Audio Note")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        if (isRecording) {
                            stopRecording();
                        } else {
                            startRecording();
                        }
                    } else {
                        importAudioFile();
                    }
                })
                .show();
    }

    private void startRecording() {
        try {
            // Create file path
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "AudioNote_" + timeStamp + ".3gp";
            File audioDir = new File(getContext().getFilesDir(), "audio_notes");
            if (!audioDir.exists()) {
                audioDir.mkdirs();
            }
            currentRecordingPath = new File(audioDir, fileName).getAbsolutePath();

            // Setup MediaRecorder
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setOutputFile(currentRecordingPath);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            mediaRecorder.prepare();
            mediaRecorder.start();

            isRecording = true;

            Log.d(TAG, "Recording started: " + currentRecordingPath);
            Toast.makeText(getContext(), "Recording started - tap to stop", Toast.LENGTH_SHORT).show();

            // Show stop recording dialog
            showStopRecordingDialog();

        } catch (IOException e) {
            Log.e(TAG, "Error starting recording", e);
            Toast.makeText(getContext(), "Error starting recording", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error starting recording", e);
            Toast.makeText(getContext(), "Error starting recording", Toast.LENGTH_SHORT).show();
        }
    }

    private void showStopRecordingDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Recording...")
                .setMessage("Recording in progress. Tap 'Stop' when finished.")
                .setPositiveButton("Stop Recording", (dialog, which) -> stopRecording())
                .setCancelable(false)
                .show();
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;

                isRecording = false;

                // Save to database
                saveRecordingToDatabase();

                Log.d(TAG, "Recording stopped");
                Toast.makeText(getContext(), "Recording saved", Toast.LENGTH_SHORT).show();

            } catch (RuntimeException e) {
                Log.e(TAG, "Error stopping recording", e);
                Toast.makeText(getContext(), "Error saving recording", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveRecordingToDatabase() {
        if (currentRecordingPath != null) {
            User currentUser = databaseHelper.getCurrentLoggedInUser();
            if (currentUser != null) {
                String fileName = new File(currentRecordingPath).getName();
                String title = fileName.replace(".3gp", "").replace("AudioNote_", "Audio Note ");

                // Fixed: Cast long to int for type compatibility
                long result = databaseHelper.addAudioNote((int) currentUser.getId(), title, currentRecordingPath);
                if (result > 0) {
                    loadAudioNotes(); // Refresh list
                    Log.d(TAG, "Audio note saved to database");
                } else {
                    Log.e(TAG, "Failed to save audio note to database");
                }
            }
        }
    }

    private void importAudioFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        audioPickerLauncher.launch(Intent.createChooser(intent, "Select Audio File"));
    }

    private void importSelectedAudioFile(Uri audioUri) {
        try {
            // Get file name
            String fileName = getFileName(audioUri);
            if (fileName == null) {
                fileName = "imported_audio_" + System.currentTimeMillis() + ".mp3";
            }

            // Create destination file
            File audioDir = new File(getContext().getFilesDir(), "audio_notes");
            if (!audioDir.exists()) {
                audioDir.mkdirs();
            }
            File destFile = new File(audioDir, fileName);

            // Copy file
            copyFile(audioUri, destFile);

            // Save to database
            User currentUser = databaseHelper.getCurrentLoggedInUser();
            if (currentUser != null) {
                String title = fileName.replaceAll("\\.[^.]*$", ""); // Remove extension

                // Fixed: Cast long to int for type compatibility
                long result = databaseHelper.addAudioNote((int) currentUser.getId(), title, destFile.getAbsolutePath());
                if (result > 0) {
                    loadAudioNotes();
                    Toast.makeText(getContext(), "Audio file imported successfully", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Audio file imported: " + fileName);
                } else {
                    Toast.makeText(getContext(), "Failed to save audio file", Toast.LENGTH_SHORT).show();
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error importing audio file", e);
            Toast.makeText(getContext(), "Error importing audio file", Toast.LENGTH_SHORT).show();
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting file name from content URI", e);
            }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result;
    }

    private void copyFile(Uri sourceUri, File destFile) throws IOException {
        try (InputStream input = getContext().getContentResolver().openInputStream(sourceUri);
             FileOutputStream output = new FileOutputStream(destFile)) {

            if (input == null) {
                throw new IOException("Cannot open input stream");
            }

            byte[] buffer = new byte[1024];
            int length;
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
        }
    }

    private void loadAudioNotes() {
        User currentUser = databaseHelper.getCurrentLoggedInUser();
        if (currentUser != null) {
            // Fixed: Cast long to int for type compatibility
            audioNotesList = databaseHelper.getUserAudioNotes((int) currentUser.getId());
            displayAudioNotes();
            updateAudioNotesCount();
        }
    }

    private void updateAudioNotesCount() {
        int count = audioNotesList.size();
        tvAudioNotesCount.setText(count + (count == 1 ? " note" : " notes"));
    }

    private void displayAudioNotes() {
        audioNotesContainer.removeAllViews();

        if (audioNotesList.isEmpty()) {
            // Show empty state
            LinearLayout emptyState = new LinearLayout(getContext());
            emptyState.setOrientation(LinearLayout.VERTICAL);
            emptyState.setPadding(32, 64, 32, 64);
            emptyState.setGravity(android.view.Gravity.CENTER);

            TextView emptyTitle = new TextView(getContext());
            emptyTitle.setText("No Audio Notes Yet");
            emptyTitle.setTextSize(18);
            emptyTitle.setTextColor(getResources().getColor(R.color.text_primary));
            emptyTitle.setGravity(android.view.Gravity.CENTER);
            emptyTitle.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView emptyMessage = new TextView(getContext());
            emptyMessage.setText("Tap 'Add Audio Note' to start recording or import audio files!");
            emptyMessage.setTextSize(14);
            emptyMessage.setTextColor(getResources().getColor(R.color.text_secondary));
            emptyMessage.setGravity(android.view.Gravity.CENTER);
            emptyMessage.setPadding(0, 16, 0, 0);

            emptyState.addView(emptyTitle);
            emptyState.addView(emptyMessage);
            audioNotesContainer.addView(emptyState);
            return;
        }

        for (AudioNote audioNote : audioNotesList) {
            View audioNoteView = createAudioNoteView(audioNote);
            audioNotesContainer.addView(audioNoteView);
        }
    }

    private View createAudioNoteView(AudioNote audioNote) {
        // Create audio note item programmatically
        LinearLayout itemLayout = new LinearLayout(getContext());
        itemLayout.setOrientation(LinearLayout.VERTICAL);
        itemLayout.setPadding(16, 16, 16, 16);

        try {
            itemLayout.setBackground(getResources().getDrawable(R.drawable.card_background));
        } catch (Exception e) {
            // Fallback if drawable doesn't exist
            itemLayout.setBackgroundColor(getResources().getColor(android.R.color.white));
        }

        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        itemParams.setMargins(0, 0, 0, 16);
        itemLayout.setLayoutParams(itemParams);

        // Title
        TextView tvTitle = new TextView(getContext());
        tvTitle.setText(audioNote.getTitle());
        tvTitle.setTextSize(16);
        tvTitle.setTextColor(getResources().getColor(R.color.text_primary));
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);

        // Date and duration
        LinearLayout infoLayout = new LinearLayout(getContext());
        infoLayout.setOrientation(LinearLayout.HORIZONTAL);
        infoLayout.setPadding(0, 8, 0, 16);

        TextView tvDuration = new TextView(getContext());
        tvDuration.setText(audioNote.getDuration());
        tvDuration.setTextSize(12);
        tvDuration.setTextColor(getResources().getColor(R.color.text_secondary));

        TextView tvSeparator = new TextView(getContext());
        tvSeparator.setText(" • ");
        tvSeparator.setTextSize(12);
        tvSeparator.setTextColor(getResources().getColor(R.color.text_secondary));

        TextView tvDate = new TextView(getContext());
        tvDate.setText(audioNote.getCreatedAt());
        tvDate.setTextSize(12);
        tvDate.setTextColor(getResources().getColor(R.color.text_secondary));

        infoLayout.addView(tvDuration);
        infoLayout.addView(tvSeparator);
        infoLayout.addView(tvDate);

        // Buttons
        LinearLayout buttonsLayout = new LinearLayout(getContext());
        buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);

        Button btnPlay = new Button(getContext());
        btnPlay.setText(currentPlayingNote != null && currentPlayingNote.getId() == audioNote.getId() && isPlaying ? "Stop" : "Play");
        btnPlay.setTextSize(12);

        try {
            btnPlay.setBackground(getResources().getDrawable(R.drawable.button_background));
            btnPlay.setTextColor(getResources().getColor(R.color.white));
        } catch (Exception e) {
            // Fallback styling
            btnPlay.setBackgroundColor(getResources().getColor(R.color.flashcard_blue));
            btnPlay.setTextColor(getResources().getColor(android.R.color.white));
        }

        LinearLayout.LayoutParams playParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
        );
        playParams.setMargins(0, 0, 8, 0);
        btnPlay.setLayoutParams(playParams);

        Button btnDelete = new Button(getContext());
        btnDelete.setText("Delete");
        btnDelete.setTextSize(12);

        try {
            btnDelete.setBackground(getResources().getDrawable(R.drawable.button_outline));
            btnDelete.setTextColor(getResources().getColor(R.color.text_secondary));
        } catch (Exception e) {
            // Fallback styling
            btnDelete.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            btnDelete.setTextColor(getResources().getColor(R.color.text_secondary));
        }

        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
        );
        deleteParams.setMargins(8, 0, 0, 0);
        btnDelete.setLayoutParams(deleteParams);

        // Button listeners
        btnPlay.setOnClickListener(v -> {
            if (currentPlayingNote != null && currentPlayingNote.getId() == audioNote.getId() && isPlaying) {
                stopPlaying();
            } else {
                playAudioNote(audioNote);
            }
        });

        btnDelete.setOnClickListener(v -> showDeleteConfirmDialog(audioNote));

        buttonsLayout.addView(btnPlay);
        buttonsLayout.addView(btnDelete);

        // Add all views to item
        itemLayout.addView(tvTitle);
        itemLayout.addView(infoLayout);
        itemLayout.addView(buttonsLayout);

        return itemLayout;
    }

    private void playAudioNote(AudioNote audioNote) {
        try {
            // Stop current playback if any
            if (isPlaying) {
                stopPlaying();
            }

            // Check if file exists
            File audioFile = new File(audioNote.getFilePath());
            if (!audioFile.exists()) {
                Toast.makeText(getContext(), "Audio file not found", Toast.LENGTH_SHORT).show();
                return;
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(audioNote.getFilePath());
            mediaPlayer.prepare();
            mediaPlayer.start();

            isPlaying = true;
            currentPlayingNote = audioNote;

            mediaPlayer.setOnCompletionListener(mp -> {
                stopPlaying();
                displayAudioNotes(); // Refresh to update button states
            });

            displayAudioNotes(); // Refresh to update button states
            Toast.makeText(getContext(), "Playing: " + audioNote.getTitle(), Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Log.e(TAG, "Error playing audio", e);
            Toast.makeText(getContext(), "Error playing audio file", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error playing audio", e);
            Toast.makeText(getContext(), "Error playing audio file", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopPlaying() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            } catch (Exception e) {
                Log.e(TAG, "Error stopping media player", e);
            }
        }
        isPlaying = false;
        currentPlayingNote = null;
    }

    private void showDeleteConfirmDialog(AudioNote audioNote) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Audio Note")
                .setMessage("Are you sure you want to delete \"" + audioNote.getTitle() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> deleteAudioNote(audioNote))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAudioNote(AudioNote audioNote) {
        // Stop playing if this note is currently playing
        if (currentPlayingNote != null && currentPlayingNote.getId() == audioNote.getId()) {
            stopPlaying();
        }

        // Delete file
        File audioFile = new File(audioNote.getFilePath());
        if (audioFile.exists()) {
            boolean deleted = audioFile.delete();
            if (!deleted) {
                Log.w(TAG, "Could not delete audio file: " + audioFile.getPath());
            }
        }

        // Delete from database
        boolean deleted = databaseHelper.deleteAudioNote(audioNote.getId());
        if (deleted) {
            loadAudioNotes(); // Refresh list
            Toast.makeText(getContext(), "Audio note deleted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Error deleting audio note", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAudioNotes(); // Refresh when returning to fragment
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Clean up media resources
        if (mediaRecorder != null) {
            try {
                mediaRecorder.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing media recorder", e);
            }
            mediaRecorder = null;
        }

        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing media player", e);
            }
            mediaPlayer = null;
        }

        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // Stop recording if in progress
        if (isRecording) {
            stopRecording();
        }

        // Stop playback if in progress
        if (isPlaying) {
            stopPlaying();
        }
    }
}