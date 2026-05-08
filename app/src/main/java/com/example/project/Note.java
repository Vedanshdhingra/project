package com.example.project;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class Note {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String content;
    public int color;
    public long lastModified;
    public boolean isPinned;
    public Long deadline;
    public boolean isSecret;
    public boolean sessionUnlocked = false;

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
        return deadline - now < 12 * 60 * 60 * 1000;
    }
}