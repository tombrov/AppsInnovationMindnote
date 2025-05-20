package com.example.mindnote;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class JournalDataManager {
    private static final String PREF_NAME = "journal_entries";
    private static final String KEY_ENTRIES = "entries";
    private static final String KEY_FIRST_RUN = "first_run";
    private static JournalDataManager instance;

    // Special markers for demo images that will be used by the UI
    public static final String DEMO_IMAGE_FAMILY = "demo_family_sunset";
    public static final String DEMO_IMAGE_MEDITATION = "demo_meditation_sunrise";
    public static final String DEMO_IMAGE_LIGHTBULB = "demo_lightbulb";

    private SharedPreferences preferences;
    private List<JournalEntry> entries;
    private Gson gson;

    private JournalDataManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        loadEntries();
        checkFirstRun();
    }

    public static synchronized JournalDataManager getInstance(Context context) {
        if (instance == null) {
            instance = new JournalDataManager(context.getApplicationContext());
        }
        return instance;
    }

    private void loadEntries() {
        String entriesJson = preferences.getString(KEY_ENTRIES, null);
        if (entriesJson != null) {
            Type type = new TypeToken<ArrayList<JournalEntry>>() {}.getType();
            entries = gson.fromJson(entriesJson, type);
        } else {
            entries = new ArrayList<>();
        }
    }

    private void saveEntries() {
        String entriesJson = gson.toJson(entries);
        preferences.edit().putString(KEY_ENTRIES, entriesJson).apply();
    }

    private void checkFirstRun() {
        boolean isFirstRun = preferences.getBoolean(KEY_FIRST_RUN, true);

        if (isFirstRun) {
            // Add demo entries only on first run
            addDemoEntries();

            // Mark first run as complete
            preferences.edit().putBoolean(KEY_FIRST_RUN, false).apply();
        }
    }

    private void addDemoEntries() {
        // Make sure we don't have entries already
        if (!entries.isEmpty()) {
            return;
        }

        // Create calendar for date manipulation
        Calendar calendar = Calendar.getInstance();

        // Demo Entry 1 - Today with Happy mood and family image
        JournalEntry entry1 = new JournalEntry();
        entry1.setNote("I'm grateful for my family's support during tough times. Their encouragement and love mean the world to me.");
        entry1.setMood(0); // Happy
        List<String> tags1 = new ArrayList<>();
        tags1.add("Family");
        tags1.add("Personal");
        entry1.setTags(tags1);
        entry1.setImagePath(DEMO_IMAGE_FAMILY); // Special marker for family image

        // Demo Entry 2 - Yesterday with Neutral mood and meditation image
        JournalEntry entry2 = new JournalEntry();
        calendar.add(Calendar.DAY_OF_MONTH, -1); // Yesterday
        entry2.setDate(calendar.getTime());
        entry2.setNote("Beautiful sunrise on morning walk today. Taking time for myself with meditation is helping me stay centered.");
        entry2.setMood(1); // Neutral
        List<String> tags2 = new ArrayList<>();
        tags2.add("Health");
        tags2.add("Personal");
        entry2.setTags(tags2);
        entry2.setImagePath(DEMO_IMAGE_MEDITATION); // Special marker for meditation image

        // Demo Entry 3 - Two days ago with Sad mood and lightbulb image
        JournalEntry entry3 = new JournalEntry();
        calendar.add(Calendar.DAY_OF_MONTH, -1); // Two days ago
        entry3.setDate(calendar.getTime());
        entry3.setNote("Successfully completed project deadline despite challenges. Proud of the work our team accomplished.");
        entry3.setMood(2); // Sad (though content is positive - this shows different moods)
        List<String> tags3 = new ArrayList<>();
        tags3.add("Work");
        entry3.setTags(tags3);
        entry3.setImagePath(DEMO_IMAGE_LIGHTBULB); // Special marker for lightbulb image

        // Add all entries to the list
        entries.add(entry1);
        entries.add(entry2);
        entries.add(entry3);

        // Save entries to storage
        saveEntries();
    }

    public List<JournalEntry> getAllEntries() {
        // Return a sorted copy of the entries (newest first)
        List<JournalEntry> sortedEntries = new ArrayList<>(entries);
        Collections.sort(sortedEntries, new Comparator<JournalEntry>() {
            @Override
            public int compare(JournalEntry e1, JournalEntry e2) {
                return e2.getDate().compareTo(e1.getDate());
            }
        });
        return sortedEntries;
    }

    public void addEntry(JournalEntry entry) {
        entries.add(entry);
        saveEntries();
    }

    public void updateEntry(JournalEntry entry) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getId().equals(entry.getId())) {
                entries.set(i, entry);
                saveEntries();
                return;
            }
        }
    }

    public void deleteEntry(String entryId) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getId().equals(entryId)) {
                entries.remove(i);
                saveEntries();
                return;
            }
        }
    }

    public JournalEntry getEntryById(String entryId) {
        for (JournalEntry entry : entries) {
            if (entry.getId().equals(entryId)) {
                return entry;
            }
        }
        return null;
    }

    public int getEntryCount() {
        return entries.size();
    }

    // For debugging or when user wants to clear all data
    public void clearAllEntries() {
        entries.clear();
        saveEntries();

        // Optionally add the demo entries back
        addDemoEntries();
    }

    // Helper method to determine if an entry is a demo entry with special image path
    public static boolean isDemoImage(String imagePath) {
        return imagePath != null &&
                (imagePath.equals(DEMO_IMAGE_FAMILY) ||
                        imagePath.equals(DEMO_IMAGE_MEDITATION) ||
                        imagePath.equals(DEMO_IMAGE_LIGHTBULB));
    }
}