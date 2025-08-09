package com.example.stepnotev2;

public class Flashcard {
    private int id;
    private String question;
    private String answer;
    private String createdDate;

    // Constructors
    public Flashcard() {}

    public Flashcard(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }

    public Flashcard(int id, String question, String answer, String createdDate) {
        this.id = id;
        this.question = question;
        this.answer = answer;
        this.createdDate = createdDate;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }
}