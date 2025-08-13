package com.example.stepnotev2;

public class AudioNote {
    private int id;
    private int userId;
    private String title;
    private String filePath;
    private String duration;
    private String createdAt;

    // Constructors
    public AudioNote() {}

    public AudioNote(int userId, String title, String filePath, String duration, String createdAt) {
        this.userId = userId;
        this.title = title;
        this.filePath = filePath;
        this.duration = duration;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "AudioNote{" +
                "id=" + id +
                ", userId=" + userId +
                ", title='" + title + '\'' +
                ", filePath='" + filePath + '\'' +
                ", duration='" + duration + '\'' +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}