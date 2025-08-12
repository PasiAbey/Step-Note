package com.example.stepnotev2;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AudioNote {
    private long id;
    private String title;
    private String duration;
    private String filePath;
    private String fileName;
    private long fileSize;
    private String createdDate;

    // Default constructor
    public AudioNote() {
        this.createdDate = getCurrentDateTime();
    }

    // Constructor with parameters
    public AudioNote(String title, String duration) {
        this.title = title;
        this.duration = duration;
        this.createdDate = getCurrentDateTime();
    }

    // Constructor with all parameters
    public AudioNote(long id, String title, String duration, String filePath,
                     String fileName, long fileSize, String createdDate) {
        this.id = id;
        this.title = title;
        this.duration = duration;
        this.filePath = filePath;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.createdDate = createdDate != null ? createdDate : getCurrentDateTime();
    }

    // Helper method to get current date and time
    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    // Display duration in MM:SS or HH:MM:SS format
    public String getDisplayDuration() {
        if (duration == null || duration.isEmpty()) {
            return "00:00";
        }

        // If duration is already in display format (MM:SS or HH:MM:SS), return as is
        if (duration.contains(":")) {
            return duration;
        }

        try {
            // If duration is in milliseconds, convert to MM:SS format
            long durationMs = Long.parseLong(duration);
            return formatDuration(durationMs);
        } catch (NumberFormatException e) {
            // If parsing fails, return the original duration or default
            return duration.isEmpty() ? "00:00" : duration;
        }
    }

    // Helper method to format duration from milliseconds to MM:SS or HH:MM:SS
    private String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        seconds = seconds % 60;
        minutes = minutes % 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }

    // Check if audio file exists and is valid
    public boolean hasAudioFile() {
        // Check if file path exists and is not empty
        if (filePath == null || filePath.trim().isEmpty()) {
            return false;
        }

        // Check if the file actually exists on the device
        try {
            File audioFile = new File(filePath);
            return audioFile.exists() && audioFile.isFile() && audioFile.length() > 0;
        } catch (Exception e) {
            // If there's any error checking the file, consider it as not having audio file
            return false;
        }
    }

    // Check if audio file is playable (has valid extension)
    public boolean isAudioFilePlayable() {
        if (!hasAudioFile()) {
            return false;
        }

        // Check if file has a valid audio extension
        String fileName = getFileName();
        if (fileName == null || fileName.trim().isEmpty()) {
            // Try to get filename from file path
            if (filePath != null && !filePath.trim().isEmpty()) {
                File file = new File(filePath);
                fileName = file.getName();
            }
        }

        if (fileName != null) {
            String lowerFileName = fileName.toLowerCase();
            return lowerFileName.endsWith(".mp3") ||
                    lowerFileName.endsWith(".wav") ||
                    lowerFileName.endsWith(".m4a") ||
                    lowerFileName.endsWith(".aac") ||
                    lowerFileName.endsWith(".3gp") ||
                    lowerFileName.endsWith(".ogg") ||
                    lowerFileName.endsWith(".flac");
        }

        return false;
    }

    // Get audio file status as human-readable string
    public String getAudioFileStatus() {
        if (!hasAudioFile()) {
            return "No audio file";
        } else if (!isAudioFilePlayable()) {
            return "Invalid audio format";
        } else {
            return "Ready to play";
        }
    }

    // Format file size in human-readable format (B, KB, MB, GB, TB)
    public String getFormattedFileSize() {
        if (fileSize <= 0) {
            return "0 B";
        }

        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = fileSize;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        // Format with appropriate decimal places
        if (unitIndex == 0) {
            return String.format(Locale.getDefault(), "%.0f %s", size, units[unitIndex]); // Bytes - no decimals
        } else {
            return String.format(Locale.getDefault(), "%.1f %s", size, units[unitIndex]); // KB, MB, etc. - 1 decimal
        }
    }

    // Alias for getFormattedFileSize() for backward compatibility
    public String getDisplayFileSize() {
        return getFormattedFileSize();
    }

    // Format created date for display
    public String getDisplayDate() {
        if (createdDate == null || createdDate.isEmpty()) {
            return "Unknown";
        }

        try {
            // If the date is in database format (yyyy-MM-dd HH:mm:ss), format it nicely
            if (createdDate.contains("-") && createdDate.contains(" ")) {
                // Parse and format the date
                String[] parts = createdDate.split(" ");
                if (parts.length >= 2) {
                    String datePart = parts[0];
                    String timePart = parts[1];

                    // Convert to more readable format
                    String[] dateParts = datePart.split("-");
                    if (dateParts.length == 3) {
                        String year = dateParts[0];
                        String month = dateParts[1];
                        String day = dateParts[2];

                        String[] timeParts = timePart.split(":");
                        if (timeParts.length >= 2) {
                            String hour = timeParts[0];
                            String minute = timeParts[1];

                            return String.format(Locale.getDefault(), "%s/%s/%s %s:%s", day, month, year, hour, minute);
                        }
                    }
                }
            }

            return createdDate;
        } catch (Exception e) {
            return createdDate;
        }
    }

    // Get the actual file size from the filesystem
    public long getActualFileSize() {
        if (!hasAudioFile()) {
            return 0;
        }

        try {
            File audioFile = new File(filePath);
            return audioFile.length();
        } catch (Exception e) {
            return fileSize; // Return stored size if can't read actual file
        }
    }

    // Update the stored file size with the actual file size
    public void updateFileSizeFromFile() {
        long actualSize = getActualFileSize();
        if (actualSize > 0) {
            this.fileSize = actualSize;
        }
    }

    // Delete the audio file from the filesystem
    public boolean deleteAudioFile() {
        if (!hasAudioFile()) {
            return true; // Consider it successful if there's no file to delete
        }

        try {
            File audioFile = new File(filePath);
            boolean deleted = audioFile.delete();

            if (deleted) {
                // Clear the file path and related info after successful deletion
                this.filePath = null;
                this.fileName = null;
                this.fileSize = 0;
            }

            return deleted;
        } catch (Exception e) {
            return false;
        }
    }

    // Get audio file extension
    public String getFileExtension() {
        if (fileName == null || fileName.trim().isEmpty()) {
            if (filePath != null && !filePath.trim().isEmpty()) {
                File file = new File(filePath);
                String name = file.getName();
                if (name != null && name.contains(".")) {
                    return name.substring(name.lastIndexOf("."));
                }
            }
            return "";
        }

        if (fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf("."));
        }

        return "";
    }

    // Check if this is a valid audio note (has title and either duration or file)
    public boolean isValid() {
        return title != null && !title.trim().isEmpty() &&
                (hasAudioFile() || (duration != null && !duration.trim().isEmpty()));
    }

    // Create a copy of this audio note
    public AudioNote copy() {
        AudioNote copy = new AudioNote();
        copy.setId(this.id);
        copy.setTitle(this.title);
        copy.setDuration(this.duration);
        copy.setFilePath(this.filePath);
        copy.setFileName(this.fileName);
        copy.setFileSize(this.fileSize);
        copy.setCreatedDate(this.createdDate);
        return copy;
    }

    @Override
    public String toString() {
        return "AudioNote{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", duration='" + duration + '\'' +
                ", displayDuration='" + getDisplayDuration() + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileName='" + fileName + '\'' +
                ", fileSize=" + fileSize +
                ", formattedFileSize='" + getFormattedFileSize() + '\'' +
                ", createdDate='" + createdDate + '\'' +
                ", displayDate='" + getDisplayDate() + '\'' +
                ", hasAudioFile=" + hasAudioFile() +
                ", isPlayable=" + isAudioFilePlayable() +
                ", isValid=" + isValid() +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        AudioNote audioNote = (AudioNote) obj;
        return id == audioNote.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}