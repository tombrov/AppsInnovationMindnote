package com.example.mindnote;

import android.content.Context;
import android.util.Log;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.Set;

public class JournalDataManager {

    private static final String COLLECTION_NAME = "entries";
    private static final String TAG = "JournalDataManager";

    private static JournalDataManager instance;
    private final FirebaseFirestore db;
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
    }

    public static synchronized JournalDataManager getInstance(Context context) {
        if (instance == null) {
            instance = new JournalDataManager(context.getApplicationContext());
        }
        return instance;
    }

    public void setAnalytics(FirebaseAnalytics analytics) {
        this.analytics = analytics;
    }

    public interface FirestoreCallback {
        void onComplete(List<JournalEntry> result);
    }

    public void loadEntriesFromFirestore(FirestoreCallback callback) {
        db.collection(COLLECTION_NAME)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    entries.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        if (doc.getId().equals("tags")) continue;
                        JournalEntry entry = doc.toObject(JournalEntry.class);
                        entry.setId(doc.getId());
                        entries.add(entry);
                    }
                    entries.sort((e1, e2) -> {
                        Date d1 = e1.getDate();
                        Date d2 = e2.getDate();

                        if (d1 == null && d2 == null) return 0;
                        if (d1 == null) return 1;
                        if (d2 == null) return -1;
                        return d2.compareTo(d1);
                    });

                    callback.onComplete(entries);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading Firestore entries", e);
                    callback.onComplete(new ArrayList<>());
                });
    }

    public void deleteTagFromAllEntries(String tagToDelete) {
        db.collection(COLLECTION_NAME)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        if (doc.getId().equals("tags")) continue;

                        List<String> entryTags = (List<String>) doc.get("tags");
                        if (entryTags != null && entryTags.contains(tagToDelete)) {
                            entryTags.remove(tagToDelete);
                            db.collection(COLLECTION_NAME)
                                    .document(doc.getId())
                                    .update("tags", entryTags)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Removed tag '" + tagToDelete + "' from entry: " + doc.getId());
                                        if (analytics != null) {
                                            Bundle bundle = new Bundle();
                                            bundle.putString("tag_action", "removed");
                                            analytics.logEvent("tag_event", bundle);
                                        }
                                    })
                                    .addOnFailureListener(e ->
                                            Log.e(TAG, "Failed to remove tag from entry: " + doc.getId(), e));
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch entries for tag cleanup", e));
    }

    public void addEntry(JournalEntry entry) {
        Map<String, Object> entryMap = new HashMap<>();
        entryMap.put("note", entry.getNote());
        entryMap.put("mood", entry.getMood());
        entryMap.put("tags", entry.getTags());
        entryMap.put("imagePath", entry.getImagePath());
        entryMap.put("date", FieldValue.serverTimestamp());

        db.collection(COLLECTION_NAME)
                .add(entryMap)
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

    public void updateEntry(JournalEntry entry) {
        if (entry.getId() == null) {
            Log.e(TAG, "Cannot update entry — ID is null.");
            return;
        }

        db.collection(COLLECTION_NAME)
                .document(entry.getId())
                .set(entry)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Updated entry ID: " + entry.getId());
                    if (analytics != null) {
                        Bundle bundle = new Bundle();
                        bundle.putString("entry_action", "edited");
                        analytics.logEvent("journal_entry", bundle);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Update entry failed", e));
    }

    public void deleteEntry(String entryId, Consumer<Boolean> callback) {
        db.collection(COLLECTION_NAME)
                .document(entryId)
                .delete()
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

    public void loadTagsFromFirestore(Consumer<List<String>> callback) {
        db.collection(COLLECTION_NAME)
                .document("tags")
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        List<String> tags = (List<String>) doc.get("tags");
                        callback.accept(tags != null ? tags : new ArrayList<>());
                    } else {
                        callback.accept(new ArrayList<>());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("TAG_LOAD", "Failed to load tags", e);
                    callback.accept(new ArrayList<>());
                });
    }

    public void saveTagsToFirestore(Set<String> allTags) {
        Map<String, Object> data = new HashMap<>();
        data.put("tags", new ArrayList<>(allTags));
        db.collection(COLLECTION_NAME)
                .document("tags")
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    Log.d("TAG_SAVE", "Tags updated");
                    if (analytics != null) {
                        Bundle bundle = new Bundle();
                        bundle.putString("tag_action", "created");
                        analytics.logEvent("tag_event", bundle);
                    }
                })
                .addOnFailureListener(e -> Log.e("TAG_SAVE", "Failed to save tags", e));
    }

    public JournalEntry getEntryById(String entryId) {
        for (JournalEntry entry : entries) {
            if (entry.getId() != null && entry.getId().equals(entryId)) {
                return entry;
            }
        }
        return null;
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
}
