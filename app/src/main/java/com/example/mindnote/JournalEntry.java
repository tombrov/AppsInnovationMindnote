package com.example.mindnote;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

public class JournalEntry implements Serializable {
    private String id;
    private String title; // ✅ NEW FIELD
    private Date date;
    private String note;
    private int mood;
    private List<String> tags;
    private String imagePath;

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

    public JournalEntry(String title, String content, String imagePath, Timestamp timestamp) {
        this.title = title;
        this.note = content;
        this.imagePath = imagePath;
        this.date = timestamp.toDate();
        this.tags = new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; } // ✅
    public void setTitle(String title) { this.title = title; } // ✅

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public int getMood() { return mood; }
    public void setMood(int mood) { this.mood = mood; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public void addTag(String tag) {
        if (this.tags == null) this.tags = new ArrayList<>();
        this.tags.add(tag);
    }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    @Exclude
    public String getFormattedDate() {
        if (date == null) return "Just Now";
        return new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(date);
    }

    @Exclude
    public String getFormattedTime() {
        if (date == null) return "Unknown time";
        return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(date);
    }

    @Exclude
    public String getShortDate() {
        if (date == null) return "Unknown date";
        return new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date);
    }

    @Exclude
    public String getTagsAsString() {
        if (tags == null || tags.isEmpty()) return "";
        return String.join(", ", tags);
    }

    @Exclude
    public String getMoodEmoji() {
        switch (mood) {
            case 0: return "😊";
            case 1: return "😐";
            case 2: return "😢";
            default: return "😐";
        }
    }
}
