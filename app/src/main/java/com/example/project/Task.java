package com.example.project;

public class Task {
    public String title;
    public String description;
    public Long deadline;
    public boolean reminderEnabled;
    public boolean isCompleted;

    public Task(String title, String description, Long deadline) {
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.reminderEnabled = false;
        this.isCompleted = false;
    }
}