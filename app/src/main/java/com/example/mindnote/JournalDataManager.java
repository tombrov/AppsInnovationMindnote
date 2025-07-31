package com.example.mindnote;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class JournalDataManager {

    private static final String TAG = "JournalDataManager";

    private static JournalDataManager instance;
    private final FirebaseFirestore db;
    private final FirebaseUser user;
    private FirebaseAnalytics analytics;
    private final List<JournalEntry> entries = new ArrayList<>();

    public static final String DEMO_IMAGE_FAMILY = "demo_family_sunset";
    public static final String DEMO_IMAGE_MEDITATION = "demo_meditation_sunrise";
    public static final String DEMO_IMAGE_LIGHTBULB = "demo_lightbulb";

    public static boolean isDemoImage(String imagePath) {
        return imagePath != null &&
                (imagePath.equals(DEMO_IMAGE_FAMILY) ||
                        imagePath.equals(DEMO_IMAGE_MEDITATION) ||
                        imagePath.equals(DEMO_IMAGE_LIGHTBULB));
    }

    private JournalDataManager(Context context) {
        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
    }

    public static synchronized JournalDataManager getInstance(Context context) {
        if (instance == null) {
            instance = new JournalDataManager(context.getApplicationContext());
        }
        return instance;
    }

    public FirebaseAnalytics setAnalytics(FirebaseAnalytics analytics) {
        this.analytics = analytics;
        return analytics;
    }

    private CollectionReference getUserEntriesRef() {
        if (user == null) return null;
        return db.collection("users").document(user.getUid()).collection("entries");
    }

    public interface FirestoreCallback {
        void onComplete(List<JournalEntry> result);
    }

    public void loadEntriesFromFirestore(FirestoreCallback callback) {
        CollectionReference ref = getUserEntriesRef();
        if (ref == null) return;

        ref.orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    entries.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        JournalEntry entry = doc.toObject(JournalEntry.class);
                        entry.setId(doc.getId());
                        entries.add(entry);
                    }

                    if (analytics != null) {
                        Bundle bundle = new Bundle();
                        bundle.putInt("entry_count", entries.size());
                        analytics.logEvent("entries_loaded", bundle);
                    }

                    callback.onComplete(entries);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading Firestore entries", e);
                    callback.onComplete(new ArrayList<>());

                    if (analytics != null) {
                        Bundle bundle = new Bundle();
                        bundle.putString("error", e.getMessage());
                        analytics.logEvent("entries_load_failed", bundle);
                    }
                });
    }

    public void saveEntry(JournalEntry entry) {
        CollectionReference ref = getUserEntriesRef();
        if (ref == null) return;

        Map<String, Object> entryMap = new HashMap<>();
        entryMap.put("title", entry.getTitle());
        entryMap.put("note", entry.getNote());
        entryMap.put("mood", entry.getMood());
        entryMap.put("tags", entry.getTags());
        entryMap.put("imagePath", entry.getImagePath());
        entryMap.put("date", FieldValue.serverTimestamp());

        ref.add(entryMap)
                .addOnSuccessListener(docRef -> {
                    entry.setId(docRef.getId());
                    Log.d(TAG, "Entry added with ID: " + docRef.getId());

                    if (analytics != null) {
                        Bundle bundle = new Bundle();
                        bundle.putString("entry_action", "created");
                        analytics.logEvent("journal_entry", bundle);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Add entry failed", e));
    }

    public void deleteEntry(String entryId, Consumer<Boolean> callback) {
        CollectionReference ref = getUserEntriesRef();
        if (ref == null) return;

        ref.document(entryId).delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Deleted entry ID: " + entryId);
                    if (analytics != null) {
                        Bundle bundle = new Bundle();
                        bundle.putString("entry_action", "deleted");
                        analytics.logEvent("journal_entry", bundle);
                    }
                    callback.accept(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Delete entry failed", e);
                    callback.accept(false);
                });
    }

    public JournalEntry getEntryById(String entryId) {
        for (JournalEntry entry : entries) {
            if (entry.getId() != null && entry.getId().equals(entryId)) {
                return entry;
            }
        }
        return null;
    }

    public void fetchEntryById(String entryId, Consumer<JournalEntry> callback) {
        CollectionReference ref = getUserEntriesRef();
        if (ref == null || entryId == null) {
            callback.accept(null);
            return;
        }

        ref.document(entryId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                JournalEntry entry = doc.toObject(JournalEntry.class);
                if (entry != null) {
                    entry.setId(doc.getId());
                }
                callback.accept(entry);
            } else {
                callback.accept(null);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to fetch entry by ID", e);
            callback.accept(null);
        });
    }

    public int getEntryCount() {
        return entries.size();
    }

    public List<JournalEntry> getAllEntriesCached() {
        return new ArrayList<>(entries);
    }

    public List<JournalEntry> getAllEntries() {
        return getAllEntriesCached();
    }

    public List<JournalEntry> getEntriesByTag(String tag) {
        List<JournalEntry> filtered = new ArrayList<>();
        for (JournalEntry entry : entries) {
            if (entry.getTags() != null && entry.getTags().contains(tag)) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    public List<JournalEntry> getEntriesByMood(String moodLabel) {
        List<JournalEntry> filtered = new ArrayList<>();
        for (JournalEntry entry : entries) {
            if (entry.getMoodEmoji().equalsIgnoreCase(moodLabel)) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    public Map<String, Integer> getMoodCounts() {
        Map<String, Integer> moodMap = new HashMap<>();
        for (JournalEntry entry : entries) {
            String mood = entry.getMoodEmoji();
            if (mood != null) {
                moodMap.put(mood, moodMap.getOrDefault(mood, 0) + 1);
            }
        }
        return moodMap;
    }

    public int calculateStreak() {
        if (entries.isEmpty()) return 0;

        entries.sort((e1, e2) -> e2.getDate().compareTo(e1.getDate())); // newest first

        int streak = 1;
        Date prevDate = entries.get(0).getDate();
        if (prevDate == null) return 0;

        for (int i = 1; i < entries.size(); i++) {
            Date current = entries.get(i).getDate();
            if (current == null) continue;

            long diff = prevDate.getTime() - current.getTime();
            long daysBetween = diff / (1000 * 60 * 60 * 24);

            if (daysBetween == 1) {
                streak++;
                prevDate = current;
            } else if (daysBetween > 1) {
                break;
            }
        }
        return streak;
    }

    public Date getLastEntryDate() {
        if (entries.isEmpty()) return null;
        Date latest = null;

        for (JournalEntry entry : entries) {
            Date entryDate = entry.getDate();
            if (entryDate != null && (latest == null || entryDate.after(latest))) {
                latest = entryDate;
            }
        }
        return latest;
    }

    public void deleteTagFromAllEntries(String tagToDelete) {
        CollectionReference ref = getUserEntriesRef();
        if (ref == null) return;

        ref.get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                List<String> entryTags = (List<String>) doc.get("tags");
                if (entryTags != null && entryTags.contains(tagToDelete)) {
                    entryTags.remove(tagToDelete);
                    ref.document(doc.getId()).update("tags", entryTags)
                            .addOnSuccessListener(aVoid ->
                                    Log.d(TAG, "Removed tag '" + tagToDelete + "' from entry: " + doc.getId()))
                            .addOnFailureListener(e ->
                                    Log.e(TAG, "Failed to remove tag from entry: " + doc.getId(), e));
                }
            }
        }).addOnFailureListener(e ->
                Log.e(TAG, "Failed to fetch entries for tag cleanup", e));
    }

    public void clearCache() {
        entries.clear();
    }
}
