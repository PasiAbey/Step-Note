package com.example.stepnotev2;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AudioNotesFragment extends Fragment {

    private static final String TAG = "AudioNotesFragment";

    private LinearLayout audioNotesContainer;
    private LinearLayout btnAddAudioNote;
    private TextView tvAudioNotesCount;
    private DatabaseHelper databaseHelper;

    // Media player for audio playback
    private MediaPlayer mediaPlayer;
    private BottomSheetDialog currentPlaybackDialog;
    private Handler playbackHandler = new Handler();
    private Runnable updateSeekBarRunnable;

    // File picker launcher
    private ActivityResultLauncher<Intent> audioPickerLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_audio_notes, container, false);

        databaseHelper = new DatabaseHelper(getContext());

        initViews(view);
        setupFilePicker();
        setupClickListeners();
        loadAudioNotes();

        return view;
    }

    private void initViews(View view) {
        audioNotesContainer = view.findViewById(R.id.audioNotesContainer);
        btnAddAudioNote = view.findViewById(R.id.btnAddAudioNote);
        tvAudioNotesCount = view.findViewById(R.id.tvAudioNotesCount);
    }

    private void setupFilePicker() {
        audioPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        Uri audioUri = result.getData().getData();
                        if (audioUri != null) {
                            importAudioFile(audioUri);
                        }
                    }
                }
        );
    }

    private void setupClickListeners() {
        btnAddAudioNote.setOnClickListener(v -> showAddAudioNoteDialog());
    }

    private void loadAudioNotes() {
        User currentUser = databaseHelper.getCurrentLoggedInUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please sign in to view audio notes", Toast.LENGTH_SHORT).show();
            return;
        }

        List<AudioNote> audioNotes = databaseHelper.getUserAudioNotes(currentUser.getId());
        displayAudioNotes(audioNotes);

        // Update count
        tvAudioNotesCount.setText(audioNotes.size() + " notes");
    }

    private void displayAudioNotes(List<AudioNote> audioNotes) {
        audioNotesContainer.removeAllViews();

        if (audioNotes.isEmpty()) {
            showEmptyState();
            return;
        }

        for (AudioNote audioNote : audioNotes) {
            View audioNoteView = createAudioNoteView(audioNote);
            audioNotesContainer.addView(audioNoteView);
        }
    }

    private View createAudioNoteView(AudioNote audioNote) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.item_audio_note, null);

        TextView tvTitle = view.findViewById(R.id.tvAudioTitle);
        TextView tvDuration = view.findViewById(R.id.tvAudioDuration);
        TextView tvCreatedDate = view.findViewById(R.id.tvCreatedDate);
        TextView btnDelete = view.findViewById(R.id.btnDelete);

        // Set audio note data
        tvTitle.setText(audioNote.getTitle());
        tvDuration.setText(audioNote.getDuration());

        // Format and set creation date
        String formattedDate = formatDate(audioNote.getCreatedAt());
        tvCreatedDate.setText(formattedDate);

        // Set click listener for entire view to play audio
        view.setOnClickListener(v -> playAudioNote(audioNote));

        // Set delete button click listener - FIXED VERSION
        btnDelete.setOnClickListener(v -> {
            // Remove the click listener from parent to prevent play action
            view.setOnClickListener(null);

            // Show delete confirmation
            showDeleteConfirmDialog(audioNote);

            // Restore the click listener after a short delay
            view.postDelayed(() -> {
                view.setOnClickListener(clickView -> playAudioNote(audioNote));
            }, 100);
        });

        return view;
    }

    private void playAudioNote(AudioNote audioNote) {
        // Check if file exists
        File audioFile = new File(audioNote.getFilePath());
        if (!audioFile.exists()) {
            Toast.makeText(getContext(), "Audio file not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Stop any currently playing audio
        stopCurrentPlayback();

        try {
            // Show playback dialog
            showAudioPlaybackDialog(audioNote);
        } catch (Exception e) {
            Log.e(TAG, "Error playing audio: " + e.getMessage());
            Toast.makeText(getContext(), "Error playing audio file", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAudioPlaybackDialog(AudioNote audioNote) {
        // Launch full-screen audio player activity
        Intent intent = new Intent(getContext(), AudioPlayerActivity.class);
        intent.putExtra("AUDIO_TITLE", audioNote.getTitle());
        intent.putExtra("AUDIO_FILE_PATH", audioNote.getFilePath());
        intent.putExtra("AUDIO_DURATION", audioNote.getDuration());
        startActivity(intent);
    }

    private void updateSeekBar(SeekBar seekBar, TextView tvCurrentTime) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            updateSeekBarRunnable = new Runnable() {
                @Override
                public void run() {
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        int currentPosition = mediaPlayer.getCurrentPosition();
                        seekBar.setProgress(currentPosition);
                        tvCurrentTime.setText(formatTime(currentPosition));
                        playbackHandler.postDelayed(this, 1000);
                    }
                }
            };
            playbackHandler.post(updateSeekBarRunnable);
        }
    }

    private void stopCurrentPlayback() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.reset();
                mediaPlayer.release();
                mediaPlayer = null;
            } catch (Exception e) {
                Log.e(TAG, "Error stopping playback: " + e.getMessage());
            }
        }

        if (updateSeekBarRunnable != null) {
            playbackHandler.removeCallbacks(updateSeekBarRunnable);
        }
    }

    private String formatTime(int milliseconds) {
        int seconds = milliseconds / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    private void showDeleteConfirmDialog(AudioNote audioNote) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Audio Note")
                .setMessage("Are you sure you want to delete \"" + audioNote.getTitle() + "\"?\n\nThis action cannot be undone.")
                .setIcon(R.drawable.ic_warning)
                .setPositiveButton("Delete", (dialog, which) -> deleteAudioNote(audioNote))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAudioNote(AudioNote audioNote) {
        // Delete from database
        boolean deleted = databaseHelper.deleteAudioNote(audioNote.getId());

        if (deleted) {
            // Delete physical file
            try {
                File audioFile = new File(audioNote.getFilePath());
                if (audioFile.exists()) {
                    audioFile.delete();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error deleting audio file: " + e.getMessage());
            }

            Toast.makeText(getContext(), "Audio note deleted successfully! 🗑️", Toast.LENGTH_SHORT).show();
            loadAudioNotes(); // Refresh the list
        } else {
            Toast.makeText(getContext(), "Failed to delete audio note", Toast.LENGTH_SHORT).show();
        }
    }

    private void showEmptyState() {
        // Create the empty state view programmatically instead of inflating
        LinearLayout emptyStateLayout = new LinearLayout(getContext());
        emptyStateLayout.setOrientation(LinearLayout.VERTICAL);
        emptyStateLayout.setGravity(android.view.Gravity.CENTER);
        emptyStateLayout.setPadding(32, 80, 32, 32);

        // Create and configure the empty state icon
        TextView iconView = new TextView(getContext());
        iconView.setText("🎵");
        iconView.setTextSize(48);
        iconView.setGravity(android.view.Gravity.CENTER);
        iconView.setAlpha(0.6f);

        // Create and configure the title
        TextView titleView = new TextView(getContext());
        titleView.setText("No Audio Notes Yet");
        titleView.setTextSize(20);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setTextColor(getResources().getColor(R.color.text_primary, null));
        titleView.setGravity(android.view.Gravity.CENTER);

        // Create and configure the description
        TextView descView = new TextView(getContext());
        descView.setText("Tap 'Add Audio Note' below to import your first audio file!");
        descView.setTextSize(14);
        descView.setTextColor(getResources().getColor(R.color.text_secondary, null));
        descView.setGravity(android.view.Gravity.CENTER);

        // Set layout parameters with margins
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        iconParams.setMargins(0, 0, 0, 24);
        iconView.setLayoutParams(iconParams);

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.setMargins(0, 0, 0, 8);
        titleView.setLayoutParams(titleParams);

        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        descParams.setMargins(0, 0, 0, 0);
        descView.setLayoutParams(descParams);

        // Add all views to the empty state layout
        emptyStateLayout.addView(iconView);
        emptyStateLayout.addView(titleView);
        emptyStateLayout.addView(descView);

        // Add the empty state to the container
        audioNotesContainer.addView(emptyStateLayout);
    }

    // FIXED: Import audio files instead of recording
    private void showAddAudioNoteDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Add Audio Note")
                .setMessage("Import an audio file from your device to add as an audio note.")
                .setIcon(R.drawable.ic_audio_notes)
                .setPositiveButton("📁 Browse Files", (dialog, which) -> openFilePicker())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            audioPickerLauncher.launch(Intent.createChooser(intent, "Select Audio File"));
        } catch (Exception e) {
            Log.e(TAG, "Error opening file picker: " + e.getMessage());
            Toast.makeText(getContext(), "Error opening file picker", Toast.LENGTH_SHORT).show();
        }
    }

    private void importAudioFile(Uri audioUri) {
        try {
            // Get file name and details
            String fileName = getFileName(audioUri);
            if (fileName == null) {
                fileName = "Audio_" + System.currentTimeMillis() + ".mp3";
            }

            // Create app directory for audio files
            File audioDir = new File(getContext().getExternalFilesDir(null), "audio_notes");
            if (!audioDir.exists()) {
                audioDir.mkdirs();
            }

            // Create destination file
            File destinationFile = new File(audioDir, fileName);

            // Copy file from URI to app directory
            copyFile(audioUri, destinationFile);

            // Get audio duration
            String duration = getAudioDuration(destinationFile.getAbsolutePath());

            // Show dialog to enter title
            showTitleInputDialog(destinationFile.getAbsolutePath(), fileName, duration);

        } catch (Exception e) {
            Log.e(TAG, "Error importing audio file: " + e.getMessage());
            Toast.makeText(getContext(), "Error importing audio file", Toast.LENGTH_SHORT).show();
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void copyFile(Uri sourceUri, File destinationFile) throws Exception {
        try (InputStream inputStream = getContext().getContentResolver().openInputStream(sourceUri);
             FileOutputStream outputStream = new FileOutputStream(destinationFile)) {

            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        }
    }

    private String getAudioDuration(String filePath) {
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(filePath);
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            retriever.release();

            if (durationStr != null) {
                int duration = Integer.parseInt(durationStr);
                return formatTime(duration);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting audio duration: " + e.getMessage());
        }
        return "Unknown";
    }

    private void showTitleInputDialog(String filePath, String fileName, String duration) {
        // Create input field
        EditText input = new EditText(getContext());
        input.setHint("Enter audio note title");

        // Pre-fill with filename (without extension)
        String defaultTitle = fileName;
        int dotIndex = defaultTitle.lastIndexOf('.');
        if (dotIndex > 0) {
            defaultTitle = defaultTitle.substring(0, dotIndex);
        }
        input.setText(defaultTitle);
        input.selectAll();

        // Create a final reference to defaultTitle for use in lambda
        final String finalDefaultTitle = defaultTitle;

        new AlertDialog.Builder(getContext())
                .setTitle("Audio Note Title")
                .setMessage("Duration: " + duration)
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String title = input.getText().toString().trim();
                    if (title.isEmpty()) {
                        title = finalDefaultTitle; // Use final variable
                    }
                    saveAudioNote(title, filePath, duration);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Delete the copied file if user cancels
                    deleteFileIfExists(filePath);
                })
                .show();
    }

    private void saveAudioNote(String title, String filePath, String duration) {
        User currentUser = databaseHelper.getCurrentLoggedInUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please sign in to save audio notes", Toast.LENGTH_SHORT).show();
            // Delete the file if user not logged in
            deleteFileIfExists(filePath);
            return;
        }

        try {
            // Save to database using the correct method signature: addAudioNote(int userId, String title, String filePath)
            long result = databaseHelper.addAudioNote(currentUser.getId(), title, filePath);

            if (result != -1) {
                // Also update the duration separately since it's not in the main addAudioNote method
                databaseHelper.updateAudioNote((int)result, title, duration);

                Toast.makeText(getContext(), "Audio note imported successfully! 🎵", Toast.LENGTH_SHORT).show();
                loadAudioNotes(); // Refresh the list
                Log.d(TAG, "Audio note saved successfully with ID: " + result);
            } else {
                Toast.makeText(getContext(), "Failed to save audio note to database", Toast.LENGTH_SHORT).show();
                // Delete the file if database save failed
                deleteFileIfExists(filePath);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving audio note: " + e.getMessage());
            Toast.makeText(getContext(), "Error saving audio note: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            // Delete the file if there was an exception
            deleteFileIfExists(filePath);
        }
    }

    // Helper method for safe file deletion
    private void deleteFileIfExists(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    Log.d(TAG, "Cleaned up file: " + filePath);
                } else {
                    Log.w(TAG, "Failed to delete file: " + filePath);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting file: " + e.getMessage());
        }
    }

    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private String formatDate(String dateString) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            Date date = inputFormat.parse(dateString);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateString; // Return original if parsing fails
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAudioNotes(); // Refresh when returning to fragment
    }

    @Override
    public void onPause() {
        super.onPause();
        stopCurrentPlayback(); // Stop any playing audio when leaving fragment
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopCurrentPlayback();
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}