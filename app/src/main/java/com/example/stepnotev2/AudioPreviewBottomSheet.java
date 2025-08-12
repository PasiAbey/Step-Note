package com.example.stepnotev2;

import android.Manifest;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class AudioPreviewBottomSheet extends BottomSheetDialogFragment implements MotionDetectorService.MotionListener {

    private TextView tvAudioTitle, tvCurrentTime, tvTotalTime, tvMotionStatus;
    private ImageView btnPlayPause, btnClose, btnPrevious, btnNext, btnMotionToggle;
    private SeekBar seekBar;

    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler();
    private boolean isPlaying = false;
    private boolean isMotionDetectionEnabled = false;
    private boolean wasPlayingBeforePause = false;
    private AudioNote audioNote;
    private OnAudioControlListener listener;

    // Motion detection service
    private MotionDetectorService motionService;
    private boolean isBoundToService = false;

    // Interface for handling audio controls from parent activity
    public interface OnAudioControlListener {
        void onPreviousTrack();
        void onNextTrack();
        void onClose();
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MotionDetectorService.MotionDetectorBinder binder = (MotionDetectorService.MotionDetectorBinder) service;
            motionService = binder.getService();
            motionService.setMotionListener(AudioPreviewBottomSheet.this);
            isBoundToService = true;

            updateMotionUI();
            android.util.Log.d("AudioPreview", "Connected to MotionDetectorService");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            motionService = null;
            isBoundToService = false;
            android.util.Log.d("AudioPreview", "Disconnected from MotionDetectorService");
        }
    };

    public static AudioPreviewBottomSheet newInstance(AudioNote audioNote) {
        AudioPreviewBottomSheet fragment = new AudioPreviewBottomSheet();
        Bundle args = new Bundle();
        args.putString("title", audioNote.getTitle());
        args.putString("filePath", audioNote.getFilePath());
        args.putString("duration", audioNote.getDuration());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme);

        // Get audio note data from arguments
        if (getArguments() != null) {
            audioNote = new AudioNote();
            audioNote.setTitle(getArguments().getString("title", "Unknown"));
            audioNote.setFilePath(getArguments().getString("filePath", ""));
            audioNote.setDuration(getArguments().getString("duration", "0:00"));
        }

        // Bind to motion detection service
        bindToMotionService();
    }

    private void bindToMotionService() {
        if (getContext() != null) {
            Intent intent = new Intent(getContext(), MotionDetectorService.class);
            getContext().startService(intent);
            getContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            View bottomSheetInternal = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheetInternal != null) {
                BottomSheetBehavior.from(bottomSheetInternal).setState(BottomSheetBehavior.STATE_EXPANDED);
                BottomSheetBehavior.from(bottomSheetInternal).setSkipCollapsed(true);
                BottomSheetBehavior.from(bottomSheetInternal).setHideable(true);
            }
        });

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_audio_preview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupAudioPlayer();
        setupClickListeners();
        updateUI();
    }

    private void initViews(View view) {
        tvAudioTitle = view.findViewById(R.id.tvAudioTitle);
        tvCurrentTime = view.findViewById(R.id.tvCurrentTime);
        tvTotalTime = view.findViewById(R.id.tvTotalTime);
        tvMotionStatus = view.findViewById(R.id.tvMotionStatus);
        btnPlayPause = view.findViewById(R.id.btnPlayPause);
        btnClose = view.findViewById(R.id.btnClose);
        btnPrevious = view.findViewById(R.id.btnPrevious);
        btnNext = view.findViewById(R.id.btnNext);
        btnMotionToggle = view.findViewById(R.id.btnMotionToggle);
        seekBar = view.findViewById(R.id.seekBar);
    }

    private void setupAudioPlayer() {
        if (audioNote != null && audioNote.getFilePath() != null && !audioNote.getFilePath().isEmpty()) {
            try {
                mediaPlayer = new MediaPlayer();

                // Check if file exists
                File audioFile = new File(audioNote.getFilePath());
                if (audioFile.exists()) {
                    mediaPlayer.setDataSource(audioNote.getFilePath());
                    mediaPlayer.prepareAsync();

                    mediaPlayer.setOnPreparedListener(mp -> {
                        int duration = mediaPlayer.getDuration();
                        seekBar.setMax(duration);
                        tvTotalTime.setText(formatTime(duration));
                        updateSeekBar();
                    });

                    mediaPlayer.setOnCompletionListener(mp -> {
                        isPlaying = false;
                        btnPlayPause.setImageResource(R.drawable.ic_play);
                        seekBar.setProgress(0);
                        tvCurrentTime.setText("0:00");

                        // Stop motion detection when audio completes
                        if (isMotionDetectionEnabled && motionService != null) {
                            motionService.stopMotionDetection();
                            isMotionDetectionEnabled = false;
                            updateMotionUI();
                        }
                    });

                } else {
                    Toast.makeText(getContext(), "Audio file not found", Toast.LENGTH_SHORT).show();
                }

            } catch (IOException e) {
                Toast.makeText(getContext(), "Error loading audio file", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else {
            // No audio file attached
            btnPlayPause.setEnabled(false);
            btnMotionToggle.setEnabled(false);
            seekBar.setEnabled(false);
            tvTotalTime.setText("No file");
        }
    }

    private void setupClickListeners() {
        btnPlayPause.setOnClickListener(v -> togglePlayPause());

        btnMotionToggle.setOnClickListener(v -> toggleMotionDetection());

        btnClose.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClose();
            }
            dismiss();
        });

        btnPrevious.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPreviousTrack();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNextTrack();
            }
        });

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

    private void togglePlayPause() {
        if (mediaPlayer == null) {
            Toast.makeText(getContext(), "No audio file to play", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isPlaying) {
            // Pause
            mediaPlayer.pause();
            isPlaying = false;
            btnPlayPause.setImageResource(R.drawable.ic_play);

            // Stop motion detection when manually paused
            if (isMotionDetectionEnabled && motionService != null) {
                motionService.stopMotionDetection();
                isMotionDetectionEnabled = false;
                updateMotionUI();
                Toast.makeText(getContext(), "Motion detection disabled", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Play
            mediaPlayer.start();
            isPlaying = true;
            btnPlayPause.setImageResource(R.drawable.ic_pause);
            updateSeekBar();
        }
    }

    private void toggleMotionDetection() {
        if (!checkMotionPermission()) {
            requestMotionPermission();
            return;
        }

        if (motionService == null) {
            Toast.makeText(getContext(), "Motion service not available", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isMotionDetectionEnabled) {
            // Disable motion detection
            motionService.stopMotionDetection();
            isMotionDetectionEnabled = false;
            Toast.makeText(getContext(), "🏃‍♂️ Motion detection OFF", Toast.LENGTH_SHORT).show();
        } else {
            // Enable motion detection
            if (!isPlaying) {
                Toast.makeText(getContext(), "▶️ Start playing audio first", Toast.LENGTH_SHORT).show();
                return;
            }

            motionService.startMotionDetection();
            isMotionDetectionEnabled = true;
            Toast.makeText(getContext(), "🏃‍♂️ Motion detection ON - Audio will pause when you stop moving", Toast.LENGTH_LONG).show();
        }

        updateMotionUI();
    }

    private boolean checkMotionPermission() {
        if (getContext() == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACTIVITY_RECOGNITION)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true; // No permission needed for older versions
    }

    private void requestMotionPermission() {
        if (getActivity() == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 1001);
        }
    }

    private void updateMotionUI() {
        if (btnMotionToggle == null || tvMotionStatus == null) return;

        if (isMotionDetectionEnabled) {
            btnMotionToggle.setImageResource(R.drawable.ic_motion_on);
            btnMotionToggle.setBackgroundTintList(getResources().getColorStateList(R.color.flashcard_blue, null));

            if (motionService != null && motionService.isMoving()) {
                tvMotionStatus.setText("🏃‍♂️ Moving - Playing");
                tvMotionStatus.setTextColor(getResources().getColor(R.color.progress_foreground, null));
            } else {
                tvMotionStatus.setText("🚶‍♂️ Ready - Start moving");
                tvMotionStatus.setTextColor(getResources().getColor(R.color.text_secondary, null));
            }
            tvMotionStatus.setVisibility(View.VISIBLE);
        } else {
            btnMotionToggle.setImageResource(R.drawable.ic_motion_off);
            btnMotionToggle.setBackgroundTintList(getResources().getColorStateList(R.color.progress_background, null));
            tvMotionStatus.setVisibility(View.GONE);
        }
    }

    private void updateSeekBar() {
        if (mediaPlayer != null && isPlaying) {
            int currentPosition = mediaPlayer.getCurrentPosition();
            seekBar.setProgress(currentPosition);
            tvCurrentTime.setText(formatTime(currentPosition));

            handler.postDelayed(this::updateSeekBar, 1000);
        }
    }

    private void updateUI() {
        if (audioNote != null) {
            tvAudioTitle.setText(audioNote.getTitle());
            tvCurrentTime.setText("0:00");

            // If no file path, show duration from audioNote
            if (audioNote.getFilePath() == null || audioNote.getFilePath().isEmpty()) {
                tvTotalTime.setText(audioNote.getDuration());
            }
        }

        updateMotionUI();
    }

    private String formatTime(int milliseconds) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) - TimeUnit.MINUTES.toSeconds(minutes);
        return String.format("%d:%02d", minutes, seconds);
    }

    // Motion detection callbacks
    @Override
    public void onMotionStarted() {
        getActivity().runOnUiThread(() -> {
            if (wasPlayingBeforePause && mediaPlayer != null && !isPlaying) {
                // Resume audio when motion starts
                mediaPlayer.start();
                isPlaying = true;
                btnPlayPause.setImageResource(R.drawable.ic_pause);
                updateSeekBar();
                wasPlayingBeforePause = false;

                Toast.makeText(getContext(), "▶️ Motion detected - Resuming audio", Toast.LENGTH_SHORT).show();
            }
            updateMotionUI();
        });
    }

    @Override
    public void onMotionStopped() {
        getActivity().runOnUiThread(() -> {
            if (isPlaying && mediaPlayer != null) {
                // Pause audio when motion stops
                mediaPlayer.pause();
                isPlaying = false;
                btnPlayPause.setImageResource(R.drawable.ic_play);
                wasPlayingBeforePause = true;

                Toast.makeText(getContext(), "⏸️ No motion detected - Pausing audio", Toast.LENGTH_SHORT).show();
            }
            updateMotionUI();
        });
    }

    @Override
    public void onMotionUpdate(boolean isMoving) {
        getActivity().runOnUiThread(() -> updateMotionUI());
    }

    public void setOnAudioControlListener(OnAudioControlListener listener) {
        this.listener = listener;
    }

    public void updateAudioNote(AudioNote newAudioNote) {
        this.audioNote = newAudioNote;

        // Release current player
        if (mediaPlayer != null) {
            if (isPlaying) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
            isPlaying = false;
        }

        // Stop motion detection when changing tracks
        if (isMotionDetectionEnabled && motionService != null) {
            motionService.stopMotionDetection();
            isMotionDetectionEnabled = false;
        }
        wasPlayingBeforePause = false;

        // Setup new audio
        setupAudioPlayer();
        updateUI();
        btnPlayPause.setImageResource(R.drawable.ic_play);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Stop motion detection
        if (isMotionDetectionEnabled && motionService != null) {
            motionService.stopMotionDetection();
        }

        // Unbind from service
        if (isBoundToService && getContext() != null) {
            getContext().unbindService(serviceConnection);
            isBoundToService = false;
        }

        // Release media player resources
        if (mediaPlayer != null) {
            if (isPlaying) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }

        // Remove handler callbacks
        handler.removeCallbacksAndMessages(null);
    }
}