package com.example.stepnotev2;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.util.Locale;

public class AudioPlayerActivity extends AppCompatActivity {

    private static final String TAG = "AudioPlayerActivity";

    // UI Components
    private TextView tvAudioTitle, tvAudioSubtitle, tvCurrentTime, tvTotalTime;
    private TextView tvPlaybackSpeed, tvMotionStatus, tvMotionToggle;
    private SeekBar seekBar;
    private ImageView btnBack, btnPlayPause, btnPrevious, btnNext;
    private ImageView btnMotionToggle, btnVolume, btnTimer;
    private ImageView btnShuffle, btnRepeat, btnOptions;
    private LinearLayout btnSpeedControl; // Fixed: This should be LinearLayout, not ImageView

    // Media Player
    private MediaPlayer mediaPlayer;
    private Handler playbackHandler = new Handler();
    private Runnable updateSeekBarRunnable;

    // Audio Info
    private String audioTitle;
    private String audioFilePath;
    private String audioDuration;

    // Playback State
    private boolean isMotionDetectionEnabled = false;
    private float playbackSpeed = 1.0f;
    private boolean isRepeatEnabled = false;
    private boolean isShuffleEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);

        try {
            // Get audio info from intent
            getAudioInfoFromIntent();

            // Initialize views
            initViews();

            // Setup click listeners
            setupClickListeners();

            // Setup audio player
            setupAudioPlayer();

            // Update UI
            updateUI();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage());
            Toast.makeText(this, "Error loading audio player", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void getAudioInfoFromIntent() {
        Intent intent = getIntent();
        audioTitle = intent.getStringExtra("AUDIO_TITLE");
        audioFilePath = intent.getStringExtra("AUDIO_FILE_PATH");
        audioDuration = intent.getStringExtra("AUDIO_DURATION");

        if (audioTitle == null) audioTitle = "Unknown Audio";
        if (audioDuration == null) audioDuration = "00:00";

        Log.d(TAG, "Audio info - Title: " + audioTitle + ", Path: " + audioFilePath + ", Duration: " + audioDuration);
    }

    private void initViews() {
        try {
            // Header
            btnBack = findViewById(R.id.btnBack);
            btnOptions = findViewById(R.id.btnOptions);

            // Motion Detection
            btnMotionToggle = findViewById(R.id.btnMotionToggle);
            tvMotionStatus = findViewById(R.id.tvMotionStatus);
            tvMotionToggle = findViewById(R.id.tvMotionToggle);

            // Audio Info
            tvAudioTitle = findViewById(R.id.tvAudioTitle);
            tvAudioSubtitle = findViewById(R.id.tvAudioSubtitle);

            // Progress
            seekBar = findViewById(R.id.seekBar);
            tvCurrentTime = findViewById(R.id.tvCurrentTime);
            tvTotalTime = findViewById(R.id.tvTotalTime);

            // Main Controls
            btnPlayPause = findViewById(R.id.btnPlayPause);
            btnPrevious = findViewById(R.id.btnPrevious);
            btnNext = findViewById(R.id.btnNext);
            btnShuffle = findViewById(R.id.btnShuffle);
            btnRepeat = findViewById(R.id.btnRepeat);

            // Additional Controls - Fixed casting
            btnSpeedControl = findViewById(R.id.btnSpeedControl); // This is LinearLayout
            tvPlaybackSpeed = findViewById(R.id.tvPlaybackSpeed);
            btnVolume = findViewById(R.id.btnVolume);
            btnTimer = findViewById(R.id.btnTimer);

            Log.d(TAG, "Views initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage());
            throw e; // Re-throw to be caught in onCreate
        }
    }

    private void setupClickListeners() {
        try {
            // Back button
            if (btnBack != null) {
                btnBack.setOnClickListener(v -> finish());
            }

            // Play/Pause
            if (btnPlayPause != null) {
                btnPlayPause.setOnClickListener(v -> togglePlayPause());
            }

            // Motion Detection
            if (btnMotionToggle != null) {
                btnMotionToggle.setOnClickListener(v -> toggleMotionDetection());
            }

            // Speed Control - Fixed: Use LinearLayout click listener
            if (btnSpeedControl != null) {
                btnSpeedControl.setOnClickListener(v -> cyclePlaybackSpeed());
            }

            // Repeat
            if (btnRepeat != null) {
                btnRepeat.setOnClickListener(v -> toggleRepeat());
            }

            // Shuffle
            if (btnShuffle != null) {
                btnShuffle.setOnClickListener(v -> toggleShuffle());
            }

            // Previous/Next (placeholder for now)
            if (btnPrevious != null) {
                btnPrevious.setOnClickListener(v ->
                        Toast.makeText(this, "Previous audio", Toast.LENGTH_SHORT).show());
            }

            if (btnNext != null) {
                btnNext.setOnClickListener(v ->
                        Toast.makeText(this, "Next audio", Toast.LENGTH_SHORT).show());
            }

            // Volume (placeholder)
            if (btnVolume != null) {
                btnVolume.setOnClickListener(v ->
                        Toast.makeText(this, "Volume control", Toast.LENGTH_SHORT).show());
            }

            // Timer (placeholder)
            if (btnTimer != null) {
                btnTimer.setOnClickListener(v ->
                        Toast.makeText(this, "Sleep timer", Toast.LENGTH_SHORT).show());
            }

            // Options (placeholder)
            if (btnOptions != null) {
                btnOptions.setOnClickListener(v ->
                        Toast.makeText(this, "More options", Toast.LENGTH_SHORT).show());
            }

            // Seek bar
            if (seekBar != null) {
                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser && mediaPlayer != null) {
                            mediaPlayer.seekTo(progress);
                            tvCurrentTime.setText(formatTime(progress));
                        }
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}
                });
            }

            Log.d(TAG, "Click listeners set up successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up click listeners: " + e.getMessage());
        }
    }

    private void setupAudioPlayer() {
        try {
            // Check if file path is provided
            if (audioFilePath == null || audioFilePath.isEmpty()) {
                Toast.makeText(this, "No audio file specified", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Check if file exists
            File audioFile = new File(audioFilePath);
            if (!audioFile.exists()) {
                Toast.makeText(this, "Audio file not found: " + audioFilePath, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Initialize MediaPlayer
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(audioFilePath);
            mediaPlayer.prepare();

            int duration = mediaPlayer.getDuration();
            if (seekBar != null) {
                seekBar.setMax(duration);
            }
            if (tvTotalTime != null) {
                tvTotalTime.setText(formatTime(duration));
            }

            // Set completion listener
            mediaPlayer.setOnCompletionListener(mp -> {
                if (btnPlayPause != null) {
                    btnPlayPause.setImageResource(R.drawable.ic_play);
                }
                if (seekBar != null) {
                    seekBar.setProgress(0);
                }
                if (tvCurrentTime != null) {
                    tvCurrentTime.setText("0:00");
                }
                stopSeekBarUpdate();

                if (isRepeatEnabled) {
                    startPlayback();
                }
            });

            Log.d(TAG, "Audio player set up successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up audio player: " + e.getMessage());
            Toast.makeText(this, "Error loading audio file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void updateUI() {
        try {
            if (tvAudioTitle != null) {
                tvAudioTitle.setText(audioTitle);
            }
            if (tvAudioSubtitle != null) {
                tvAudioSubtitle.setText("Audio Note • " + audioDuration);
            }
            if (tvCurrentTime != null) {
                tvCurrentTime.setText("0:00");
            }
            if (tvTotalTime != null) {
                tvTotalTime.setText(audioDuration);
            }

            updateMotionUI();
            updateSpeedUI();
            updateRepeatUI();
            updateShuffleUI();

            Log.d(TAG, "UI updated successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error updating UI: " + e.getMessage());
        }
    }

    private void togglePlayPause() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    if (btnPlayPause != null) {
                        btnPlayPause.setImageResource(R.drawable.ic_play);
                    }
                    stopSeekBarUpdate();
                } else {
                    startPlayback();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error toggling play/pause: " + e.getMessage());
                Toast.makeText(this, "Error controlling playback", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startPlayback() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.start();
                if (btnPlayPause != null) {
                    btnPlayPause.setImageResource(R.drawable.ic_pause);
                }
                updateSeekBar();
            } catch (Exception e) {
                Log.e(TAG, "Error starting playback: " + e.getMessage());
                Toast.makeText(this, "Error starting playback", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void toggleMotionDetection() {
        isMotionDetectionEnabled = !isMotionDetectionEnabled;
        updateMotionUI();

        String status = isMotionDetectionEnabled ? "enabled" : "disabled";
        Toast.makeText(this, "Motion detection " + status, Toast.LENGTH_SHORT).show();
    }

    private void updateMotionUI() {
        try {
            if (btnMotionToggle != null && tvMotionStatus != null && tvMotionToggle != null) {
                if (isMotionDetectionEnabled) {
                    btnMotionToggle.setImageResource(R.drawable.ic_motion_on);
                    tvMotionStatus.setText("Will pause when you stop moving");
                    tvMotionToggle.setText("ON");
                    tvMotionToggle.setTextColor(getColor(R.color.flashcard_blue));
                } else {
                    btnMotionToggle.setImageResource(R.drawable.ic_motion_off);
                    tvMotionStatus.setText("Auto-pause when you stop moving");
                    tvMotionToggle.setText("OFF");
                    tvMotionToggle.setTextColor(getColor(R.color.text_secondary));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating motion UI: " + e.getMessage());
        }
    }

    private void cyclePlaybackSpeed() {
        if (playbackSpeed == 1.0f) {
            playbackSpeed = 1.25f;
        } else if (playbackSpeed == 1.25f) {
            playbackSpeed = 1.5f;
        } else if (playbackSpeed == 1.5f) {
            playbackSpeed = 2.0f;
        } else if (playbackSpeed == 2.0f) {
            playbackSpeed = 0.75f;
        } else {
            playbackSpeed = 1.0f;
        }

        updateSpeedUI();
        applyPlaybackSpeed();
    }

    private void updateSpeedUI() {
        try {
            if (tvPlaybackSpeed != null) {
                tvPlaybackSpeed.setText(playbackSpeed + "x");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating speed UI: " + e.getMessage());
        }
    }

    private void applyPlaybackSpeed() {
        if (mediaPlayer != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            try {
                mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(playbackSpeed));
                Toast.makeText(this, "Speed: " + playbackSpeed + "x", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Error setting playback speed: " + e.getMessage());
            }
        }
    }

    private void toggleRepeat() {
        isRepeatEnabled = !isRepeatEnabled;
        updateRepeatUI();

        String status = isRepeatEnabled ? "enabled" : "disabled";
        Toast.makeText(this, "Repeat " + status, Toast.LENGTH_SHORT).show();
    }

    private void updateRepeatUI() {
        try {
            if (btnRepeat != null) {
                btnRepeat.setAlpha(isRepeatEnabled ? 1.0f : 0.7f);
                btnRepeat.setImageResource(isRepeatEnabled ? R.drawable.ic_repeat_on : R.drawable.ic_repeat);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating repeat UI: " + e.getMessage());
        }
    }

    private void toggleShuffle() {
        isShuffleEnabled = !isShuffleEnabled;
        updateShuffleUI();

        String status = isShuffleEnabled ? "enabled" : "disabled";
        Toast.makeText(this, "Shuffle " + status, Toast.LENGTH_SHORT).show();
    }

    private void updateShuffleUI() {
        try {
            if (btnShuffle != null) {
                btnShuffle.setAlpha(isShuffleEnabled ? 1.0f : 0.7f);
                btnShuffle.setImageResource(isShuffleEnabled ? R.drawable.ic_shuffle_on : R.drawable.ic_shuffle);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating shuffle UI: " + e.getMessage());
        }
    }

    private void updateSeekBar() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            updateSeekBarRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                            int currentPosition = mediaPlayer.getCurrentPosition();
                            if (seekBar != null) {
                                seekBar.setProgress(currentPosition);
                            }
                            if (tvCurrentTime != null) {
                                tvCurrentTime.setText(formatTime(currentPosition));
                            }
                            playbackHandler.postDelayed(this, 1000);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error in seekbar update: " + e.getMessage());
                    }
                }
            };
            playbackHandler.post(updateSeekBarRunnable);
        }
    }

    private void stopSeekBarUpdate() {
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

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            try {
                mediaPlayer.pause();
                if (btnPlayPause != null) {
                    btnPlayPause.setImageResource(R.drawable.ic_play);
                }
                stopSeekBarUpdate();
            } catch (Exception e) {
                Log.e(TAG, "Error in onPause: " + e.getMessage());
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.reset();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            stopSeekBarUpdate();
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy: " + e.getMessage());
        }
    }
}