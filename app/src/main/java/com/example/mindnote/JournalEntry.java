package com.example.mindnote;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class JournalEntry implements Serializable {
    private String id;
    private Date date;
    private String note;
    private int mood; // 0=happy, 1=neutral, 2=sad
    private List<String> tags;
    private String imagePath; // For future use with photo feature

    public JournalEntry() {
        this.id = UUID.randomUUID().toString();
        this.date = new Date();
        this.tags = new ArrayList<>();
    }

    public JournalEntry(Date date, String note, int mood) {
        this.id = UUID.randomUUID().toString();
        this.date = date;
        this.note = note;
        this.mood = mood;
        this.tags = new ArrayList<>();
    }

    // Getters and setters
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

    // Helper methods
    public String getFormattedDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
        return dateFormat.format(date);
    }

    public String getFormattedTime() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return timeFormat.format(date);
    }

    public String getShortDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        return dateFormat.format(date);
    }

    public String getTagsAsString() {
        if (tags == null || tags.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (String tag : tags) {
            builder.append(tag).append(", ");
        }
        String result = builder.toString();
        if (result.endsWith(", ")) {
            result = result.substring(0, result.length() - 2);
        }
        return result;
    }

    public int getMoodIconResource() {
        switch (mood) {
            case 0:
                return R.drawable.ic_mood_happy;
            case 1:
                return R.drawable.ic_mood_neutral;
            case 2:
                return R.drawable.ic_mood_sad;
            default:
                return R.drawable.ic_mood_neutral;
        }
    }
}