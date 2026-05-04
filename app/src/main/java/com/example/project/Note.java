package com.example.project;

public class Note {
    public String title;
    public String content;
    public int color;
    public long lastModified;
    public boolean isPinned;
    public Long deadline; // Nullable, stores timestamp
    public boolean isSecret;

    public Note(String title, String content, int color) {
        this.title = title;
        this.content = content;
        this.color = color;
        this.lastModified = System.currentTimeMillis();
        this.isPinned = false;
        this.deadline = null;
        this.isSecret = false;
    }

    public boolean isUrgent() {
        if (deadline == null) return false;
        long now = System.currentTimeMillis();
        // Urgent if deadline is within next 12 hours or already passed
        return deadline - now < 12 * 60 * 60 * 1000;
    }
}