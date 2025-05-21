package com.example.mindnote;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class JournalEntry implements Serializable {
    private String id;             // Firestore document ID
    private Date date;            // Entry date
    private String note;          // User's note
    private int mood;             // 0=happy, 1=neutral, 2=sad
    private List<String> tags;    // List of tags
    private String imagePath;     // Optional image path

    public JournalEntry() {
        this.date = new Date();
        this.tags = new ArrayList<>();
    }

    public JournalEntry(Date date, String note, int mood) {
        this.date = date;
        this.note = note;
        this.mood = mood;
        this.tags = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public int getMood() {
        return mood;
    }

    public void setMood(int mood) {
        this.mood = mood;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void addTag(String tag) {
        if (this.tags == null) {
            this.tags = new ArrayList<>();
        }
        this.tags.add(tag);
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getFormattedDate() {
        if (date == null) return "Just Now";
        return new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(date);
    }

    public String getFormattedTime() {
        if (date == null) return "Unknown time";
        return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(date);
    }

    public String getShortDate() {
        if (date == null) return "Unknown date";
        return new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date);
    }

    public String getTagsAsString() {
        if (tags == null || tags.isEmpty()) return "";
        return String.join(", ", tags);
    }

    public String getMoodEmoji() {
        switch (mood) {
            case 0: return "😊"; // happy
            case 1: return "😐"; // neutral
            case 2: return "😢"; // sad
            default: return "😐";
        }
    }
}