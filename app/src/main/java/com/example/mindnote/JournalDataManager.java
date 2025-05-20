package com.example.mindnote;

import android.content.Context;
import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;


public class JournalDataManager {

    private static final String COLLECTION_NAME = "entries";
    private static final String TAG = "JournalDataManager";

    private static JournalDataManager instance;
    private final FirebaseFirestore db;
    private final List<JournalEntry> entries = new ArrayList<>();

    // Demo image constants
    public static final String DEMO_IMAGE_FAMILY = "demo_family_sunset";
    public static final String DEMO_IMAGE_MEDITATION = "demo_meditation_sunrise";
    public static final String DEMO_IMAGE_LIGHTBULB = "demo_lightbulb";

    // Check if the image path matches a demo placeholder
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

    public interface FirestoreCallback {
        void onComplete(List<JournalEntry> result);
    }

    // Load entries from Firestore into local memory (call on app startup or screen entry)
    public void loadEntriesFromFirestore(FirestoreCallback callback) {
        db.collection(COLLECTION_NAME)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    entries.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        JournalEntry entry = doc.toObject(JournalEntry.class);
                        entry.setId(doc.getId()); // Track document ID
                        entries.add(entry);
                    }

                    // Sort newest to oldest
                    entries.sort((e1, e2) -> e2.getDate().compareTo(e1.getDate()));
                    callback.onComplete(entries);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading Firestore entries", e);
                    callback.onComplete(new ArrayList<>());
                });
    }

    // Add new entry to Firestore
    public void addEntry(JournalEntry entry) {
        Map<String, Object> entryMap = new HashMap<>();
        entryMap.put("note", entry.getNote());
        entryMap.put("mood", entry.getMood());
        entryMap.put("tags", entry.getTags());
        entryMap.put("imagePath", entry.getImagePath());
        entryMap.put("date", FieldValue.serverTimestamp()); // 🔥 Important

        db.collection(COLLECTION_NAME)
                .add(entryMap)
                .addOnSuccessListener(docRef -> {
                    entry.setId(docRef.getId());
                    Log.d(TAG, "Entry added with ID: " + docRef.getId());
                })
                .addOnFailureListener(e -> Log.e(TAG, "Add entry failed", e));
    }

    // Update existing entry in Firestore by ID
    public void updateEntry(JournalEntry entry) {
        if (entry.getId() == null) {
            Log.e(TAG, "Cannot update entry — ID is null.");
            return;
        }

        db.collection(COLLECTION_NAME)
                .document(entry.getId())
                .set(entry)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Updated entry ID: " + entry.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Update entry failed", e));
    }

    // Delete entry from Firestore
    public void deleteEntry(String entryId) {
        db.collection(COLLECTION_NAME)
                .document(entryId)
                .delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Deleted entry ID: " + entryId))
                .addOnFailureListener(e -> Log.e(TAG, "Delete entry failed", e));
    }

    // Get entry from memory by ID
    public JournalEntry getEntryById(String entryId) {
        for (JournalEntry entry : entries) {
            if (entry.getId() != null && entry.getId().equals(entryId)) {
                return entry;
            }
        }
        return null;
    }

    // Return total entries in memory
    public int getEntryCount() {
        return entries.size();
    }

    // Return cached entries (for UI rendering)
    public List<JournalEntry> getAllEntriesCached() {
        return new ArrayList<>(entries);
    }

    // Backward compatibility for MainActivity, NotesActivity, NotesAdapter
    public List<JournalEntry> getAllEntries() {
        return getAllEntriesCached();
    }
}
