package com.example.stepnotev2;

public class Flashcard {
    private int id;
    private int userId;
    private String frontText;
    private String backText;
    private String createdAt;

    // Constructors
    public Flashcard() {}

    public Flashcard(int userId, String frontText, String backText, String createdAt) {
        this.userId = userId;
        this.frontText = frontText;
        this.backText = backText;
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

    public String getFrontText() {
        return frontText;
    }

    public void setFrontText(String frontText) {
        this.frontText = frontText;
    }

    public String getBackText() {
        return backText;
    }

    public void setBackText(String backText) {
        this.backText = backText;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Flashcard{" +
                "id=" + id +
                ", userId=" + userId +
                ", frontText='" + frontText + '\'' +
                ", backText='" + backText + '\'' +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}