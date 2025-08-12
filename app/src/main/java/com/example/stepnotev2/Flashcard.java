package com.example.stepnotev2;

public class Flashcard {
    private long id;  // Changed from int to long
    private String question;
    private String answer;
    private String createdDate;

    // Default constructor
    public Flashcard() {}

    // Constructor with parameters
    public Flashcard(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }

    // Constructor with all parameters
    public Flashcard(long id, String question, String answer, String createdDate) {
        this.id = id;
        this.question = question;
        this.answer = answer;
        this.createdDate = createdDate;
    }

    // Getters and Setters
    public long getId() {  // Changed return type from int to long
        return id;
    }

    public void setId(long id) {  // Changed parameter type from int to long
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

    @Override
    public String toString() {
        return "Flashcard{" +
                "id=" + id +
                ", question='" + question + '\'' +
                ", answer='" + answer + '\'' +
                ", createdDate='" + createdDate + '\'' +
                '}';
    }
}