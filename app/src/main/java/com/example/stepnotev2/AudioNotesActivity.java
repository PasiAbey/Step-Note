package com.example.stepnotev2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.widget.EditText;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class AudioNotesActivity extends AppCompatActivity {

    private LinearLayout audioNotesContainer, btnAddAudioNote;
    private TextView tvAudioNotesCount;

    // Database
    private DatabaseHelper databaseHelper;

    // Audio file import
    private ActivityResultLauncher<Intent> audioPickerLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    // Audio notes data
    private List<AudioNote> audioNotes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_notes);

        databaseHelper = new DatabaseHelper(this);

        initViews();
        setupFileImportLaunchers();
        loadAudioNotesFromDatabase();
        setupAudioNotesList();
        updateAudioNotesCount();
        setupNavigationListeners();
        setupAudioNotesListeners();
    }

    private void initViews() {
        audioNotesContainer = findViewById(R.id.audioNotesContainer);
        btnAddAudioNote = findViewById(R.id.btnAddAudioNote);
        tvAudioNotesCount = findViewById(R.id.tvAudioNotesCount);
    }

    private void setupFileImportLaunchers() {
        // Audio file picker launcher
        audioPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri audioUri = result.getData().getData();
                        if (audioUri != null) {
                            handleSelectedAudioFile(audioUri);
                        }
                    }
                }
        );

        // Permission request launcher
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openAudioFilePicker();
                    } else {
                        Toast.makeText(this, "Permission required to access audio files", Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void loadAudioNotesFromDatabase() {
        audioNotes = databaseHelper.getAllAudioNotes();

        // Debug logging
        android.util.Log.d("AUDIO_NOTES_DEBUG", "Total audio notes loaded: " + audioNotes.size());
        for (int i = 0; i < audioNotes.size(); i++) {
            android.util.Log.d("AUDIO_NOTES_DEBUG", "Note " + (i+1) + ": " + audioNotes.get(i).toString());
        }

        if (audioNotes.isEmpty()) {
            Toast.makeText(this, "No audio notes yet. Add your first audio note!", Toast.LENGTH_LONG).show();
        }
    }

    private void setupAudioNotesList() {
        if (audioNotesContainer == null) return;

        audioNotesContainer.removeAllViews();

        if (audioNotes.isEmpty()) {
            // Show empty state
            TextView emptyView = new TextView(this);
            emptyView.setText("No audio notes yet.\nAdd your first audio note!");
            emptyView.setTextColor(getResources().getColor(R.color.text_secondary, null));
            emptyView.setTextSize(16);
            emptyView.setGravity(android.view.Gravity.CENTER);
            emptyView.setPadding(32, 64, 32, 64);
            audioNotesContainer.addView(emptyView);
            return;
        }

        for (int i = 0; i < audioNotes.size(); i++) {
            View audioNoteView = createAudioNoteItemView(audioNotes.get(i), i);
            audioNotesContainer.addView(audioNoteView);
        }
    }

    private View createAudioNoteItemView(AudioNote audioNote, int position) {
        View view = getLayoutInflater().inflate(R.layout.item_audio_note, audioNotesContainer, false);

        TextView tvAudioTitle = view.findViewById(R.id.tvAudioTitle);
        TextView tvAudioDuration = view.findViewById(R.id.tvAudioDuration);
        TextView tvCreatedDate = view.findViewById(R.id.tvCreatedDate);
        TextView tvFileStatus = view.findViewById(R.id.tvFileStatus);
        TextView btnDelete = view.findViewById(R.id.btnDelete);

        // Bind data with text truncation
        tvAudioTitle.setText(truncateText(audioNote.getTitle(), 35)); // Limit title to 35 characters
        tvAudioDuration.setText(audioNote.getDisplayDuration());

        // Format date to show only date part
        String createdDate = audioNote.getCreatedDate();
        if (createdDate != null && createdDate.contains(" ")) {
            createdDate = createdDate.split(" ")[0];
        }
        tvCreatedDate.setText(createdDate != null ? createdDate : "Unknown");

        // Show file status with truncated filename
        tvFileStatus.setText(getFormattedFileInfo(audioNote));
        tvFileStatus.setVisibility(View.VISIBLE);

        // Set click listeners
        view.setOnClickListener(v -> onAudioNoteClick(audioNote, position));
        btnDelete.setOnClickListener(v -> onDeleteClick(audioNote, position));

        return view;
    }

    private void updateAudioNotesCount() {
        if (tvAudioNotesCount != null) {
            tvAudioNotesCount.setText(audioNotes.size() + " notes");
        }
    }

    private void setupAudioNotesListeners() {
        // Add new audio note
        btnAddAudioNote.setOnClickListener(v -> showAddAudioNoteOptions());
    }

    private void showAddAudioNoteOptions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Audio Note");
        builder.setMessage("How would you like to add your audio note?");

        builder.setPositiveButton("Import Audio File", (dialog, which) -> {
            checkPermissionAndOpenFilePicker();
        });

        builder.setNeutralButton("Create Manually", (dialog, which) -> {
            showAddAudioNoteDialog();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void checkPermissionAndOpenFilePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses READ_MEDIA_AUDIO
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO);
            } else {
                openAudioFilePicker();
            }
        } else {
            // Below Android 13 uses READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            } else {
                openAudioFilePicker();
            }
        }
    }

    private void openAudioFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Add multiple MIME types for better compatibility
        String[] mimeTypes = {"audio/mpeg", "audio/mp4", "audio/wav", "audio/3gp", "audio/aac", "audio/ogg"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        try {
            audioPickerLauncher.launch(Intent.createChooser(intent, "Select Audio File"));
        } catch (Exception e) {
            Toast.makeText(this, "No file manager found", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSelectedAudioFile(Uri audioUri) {
        try {
            // Get file information
            String fileName = getFileName(audioUri);
            long fileSize = getFileSize(audioUri);
            String duration = formatFileSize(fileSize); // Using file size as duration for now

            // Copy file to app's internal storage
            String copiedFilePath = copyAudioFileToInternalStorage(audioUri, fileName);

            if (copiedFilePath != null) {
                // Show dialog to confirm and add details
                showImportedAudioDialog(fileName, duration, copiedFilePath, fileSize);
            } else {
                Toast.makeText(this, "Failed to import audio file", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Error importing audio file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String getFileName(Uri uri) {
        String fileName = "Unknown";
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileName;
    }

    private long getFileSize(Uri uri) {
        long fileSize = 0;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex != -1) {
                    fileSize = cursor.getLong(sizeIndex);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileSize;
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private String copyAudioFileToInternalStorage(Uri sourceUri, String fileName) {
        try {
            // Create audio directory in internal storage
            File audioDir = new File(getFilesDir(), "audio_notes");
            if (!audioDir.exists()) {
                audioDir.mkdirs();
            }

            // Create unique file name to avoid conflicts
            String timestamp = String.valueOf(System.currentTimeMillis());
            String extension = getFileExtension(fileName);
            String uniqueFileName = "audio_" + timestamp + extension;

            File destinationFile = new File(audioDir, uniqueFileName);

            // Copy file
            try (InputStream inputStream = getContentResolver().openInputStream(sourceUri);
                 FileOutputStream outputStream = new FileOutputStream(destinationFile)) {

                if (inputStream != null) {
                    byte[] buffer = new byte[4096];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }
                    return destinationFile.getAbsolutePath();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf("."));
        }
        return ".mp3"; // Default extension
    }

    private void showImportedAudioDialog(String fileName, String fileInfo, String filePath, long fileSize) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Import Audio File");

        // Create dialog layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);

        // Title input (pre-filled with file name)
        EditText titleInput = new EditText(this);
        titleInput.setHint("Enter audio note title");
        titleInput.setText(fileName.replaceFirst("[.][^.]+$", "")); // Remove extension
        titleInput.setMinLines(1);
        layout.addView(titleInput);

        // File info display
        TextView fileInfoView = new TextView(this);
        fileInfoView.setText("📎 File: " + fileName + "\n📊 Size: " + fileInfo);
        fileInfoView.setTextColor(getResources().getColor(R.color.text_secondary, null));
        fileInfoView.setTextSize(14);
        fileInfoView.setPadding(0, 20, 0, 10);
        layout.addView(fileInfoView);

        builder.setView(layout);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String title = titleInput.getText().toString().trim();

            if (!title.isEmpty()) {
                // Add new audio note with file to database
                long id = databaseHelper.addAudioNote(title, fileInfo, filePath, fileName, fileSize);

                if (id != -1) {
                    Toast.makeText(this, "Audio file imported successfully! 🎵", Toast.LENGTH_SHORT).show();
                    refreshData();
                } else {
                    Toast.makeText(this, "Error saving to database", Toast.LENGTH_SHORT).show();
                    // Delete the copied file if database save failed
                    try {
                        new File(filePath).delete();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            // Delete the copied file if user cancels
            try {
                new File(filePath).delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        builder.show();
    }

    private void showAddAudioNoteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Audio Note");

        // Create dialog layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);

        // Title input
        EditText titleInput = new EditText(this);
        titleInput.setHint("Enter audio note title");
        titleInput.setMinLines(1);
        layout.addView(titleInput);

        // Duration input
        EditText durationInput = new EditText(this);
        durationInput.setHint("Enter duration (e.g., 5 min)");
        durationInput.setMinLines(1);
        durationInput.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        ((LinearLayout.LayoutParams) durationInput.getLayoutParams()).topMargin = 20;
        layout.addView(durationInput);

        builder.setView(layout);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String title = titleInput.getText().toString().trim();
            String duration = durationInput.getText().toString().trim();

            if (!title.isEmpty() && !duration.isEmpty()) {
                // Add new audio note without file to database
                long id = databaseHelper.addAudioNote(title, duration);

                if (id != -1) {
                    Toast.makeText(this, "Audio note added successfully!", Toast.LENGTH_SHORT).show();
                    refreshData();
                } else {
                    Toast.makeText(this, "Error saving to database", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void refreshData() {
        loadAudioNotesFromDatabase();
        setupAudioNotesList();
        updateAudioNotesCount();
    }

    // Text truncation helper methods
    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    private String truncateFileName(String fileName, int maxLength) {
        if (fileName == null || fileName.isEmpty()) return "Unknown file";

        // If filename is short enough, return as is
        if (fileName.length() <= maxLength) return fileName;

        // Try to preserve file extension
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            String name = fileName.substring(0, lastDotIndex);
            String extension = fileName.substring(lastDotIndex);

            // Calculate how much space we have for the name part
            int availableSpace = maxLength - extension.length() - 3; // 3 for "..."

            if (availableSpace > 0) {
                return name.substring(0, Math.min(name.length(), availableSpace)) + "..." + extension;
            }
        }

        // Fallback: just truncate normally
        return fileName.substring(0, maxLength - 3) + "...";
    }

    private String getFormattedFileInfo(AudioNote audioNote) {
        if (!audioNote.hasAudioFile()) {
            return "No audio file attached";
        }

        String fileName = truncateFileName(audioNote.getFileName(), 25); // Limit to 25 characters
        String fileSize = audioNote.getFormattedFileSize();

        return "📎 " + fileName + " (" + fileSize + ")";
    }

    // Audio note click handlers - Click opens preview
    public void onAudioNoteClick(AudioNote audioNote, int position) {
        if (audioNote.hasAudioFile()) {
            // Show audio preview bottom sheet when user clicks the audio note
            showAudioPreview(audioNote, position);
        } else {
            // Show options for notes without audio files
            showAudioNoteOptionsDialog(audioNote, position);
        }
    }

    private void showAudioPreview(AudioNote audioNote, int position) {
        AudioPreviewBottomSheet bottomSheet = AudioPreviewBottomSheet.newInstance(audioNote);

        bottomSheet.setOnAudioControlListener(new AudioPreviewBottomSheet.OnAudioControlListener() {
            @Override
            public void onPreviousTrack() {
                // Navigate to previous audio note
                if (position > 0) {
                    AudioNote prevNote = audioNotes.get(position - 1);
                    bottomSheet.updateAudioNote(prevNote);
                    Toast.makeText(AudioNotesActivity.this, "⏮️ Previous: " + prevNote.getTitle(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(AudioNotesActivity.this, "This is the first track", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNextTrack() {
                // Navigate to next audio note
                if (position < audioNotes.size() - 1) {
                    AudioNote nextNote = audioNotes.get(position + 1);
                    bottomSheet.updateAudioNote(nextNote);
                    Toast.makeText(AudioNotesActivity.this, "⏭️ Next: " + nextNote.getTitle(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(AudioNotesActivity.this, "This is the last track", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onClose() {
                Toast.makeText(AudioNotesActivity.this, "Audio preview closed", Toast.LENGTH_SHORT).show();
            }
        });

        bottomSheet.show(getSupportFragmentManager(), "AudioPreviewBottomSheet");
    }

    private void showAudioNoteOptionsDialog(AudioNote audioNote, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(audioNote.getTitle());
        builder.setMessage("This audio note doesn't have an audio file attached. What would you like to do?");

        builder.setPositiveButton("Edit", (dialog, which) -> {
            showEditAudioNoteDialog(audioNote, position);
        });

        builder.setNeutralButton("Attach Audio File", (dialog, which) -> {
            // Option to attach audio file to existing note
            Toast.makeText(this, "Audio file attachment - Coming Soon!", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    public void onDeleteClick(AudioNote audioNote, int position) {
        showDeleteConfirmationDialog(audioNote, position);
    }

    private void showEditAudioNoteDialog(AudioNote audioNote, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Audio Note");

        // Create dialog layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);

        // Pre-filled title input
        EditText titleInput = new EditText(this);
        titleInput.setHint("Enter audio note title");
        titleInput.setText(audioNote.getTitle());
        titleInput.setMinLines(1);
        layout.addView(titleInput);

        // Pre-filled duration input
        EditText durationInput = new EditText(this);
        durationInput.setHint("Enter duration");
        durationInput.setText(audioNote.getDuration());
        durationInput.setMinLines(1);
        durationInput.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        ((LinearLayout.LayoutParams) durationInput.getLayoutParams()).topMargin = 20;
        layout.addView(durationInput);

        // File info if exists
        if (audioNote.hasAudioFile()) {
            TextView fileInfoView = new TextView(this);
            fileInfoView.setText("📎 " + audioNote.getFileName() + " (" + audioNote.getFormattedFileSize() + ")");
            fileInfoView.setTextColor(getResources().getColor(R.color.text_secondary, null));
            fileInfoView.setTextSize(14);
            fileInfoView.setPadding(0, 20, 0, 0);
            layout.addView(fileInfoView);
        }

        builder.setView(layout);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String title = titleInput.getText().toString().trim();
            String duration = durationInput.getText().toString().trim();

            if (!title.isEmpty() && !duration.isEmpty()) {
                int result = databaseHelper.updateAudioNote(audioNote.getId(), title, duration);
                if (result > 0) {
                    Toast.makeText(this, "Audio note updated successfully!", Toast.LENGTH_SHORT).show();
                    refreshData();
                } else {
                    Toast.makeText(this, "Error updating audio note", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showDeleteConfirmationDialog(AudioNote audioNote, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Audio Note")
                .setMessage("Are you sure you want to delete this audio note?\n\n" + audioNote.getTitle() +
                        (audioNote.hasAudioFile() ? "\n\n⚠️ This will also delete the audio file." : ""))
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Delete audio file if exists
                    if (audioNote.hasAudioFile()) {
                        try {
                            new File(audioNote.getFilePath()).delete();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    // Delete from database
                    databaseHelper.deleteAudioNote(audioNote.getId());
                    Toast.makeText(this, "Audio note deleted successfully!", Toast.LENGTH_SHORT).show();
                    refreshData();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupNavigationListeners() {
        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        findViewById(R.id.navFlashcards).setOnClickListener(v -> {
            startActivity(new Intent(this, FlashcardsActivity.class));
            finish();
        });

        findViewById(R.id.navAudioNotes).setOnClickListener(v ->
                Toast.makeText(this, "You're already on Audio Notes!", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this activity
        refreshData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}