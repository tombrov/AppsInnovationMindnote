package com.example.mindnote;

import android.content.Context;
import android.util.Log;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
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

    public void deleteTagFromAllEntries(String tagToDelete) {
        CollectionReference ref = getUserEntriesRef();
        if (ref == null) return;

        ref.get().addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        List<String> entryTags = (List<String>) doc.get("tags");
                        if (entryTags != null && entryTags.contains(tagToDelete)) {
                            entryTags.remove(tagToDelete);
                            ref.document(doc.getId()).update("tags", entryTags)
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
        CollectionReference ref = getUserEntriesRef();
        if (ref == null) return;

        Map<String, Object> entryMap = new HashMap<>();
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

    public void saveEntry(JournalEntry entry) {
        addEntry(entry);
    }

    public void updateEntry(JournalEntry entry) {
        if (entry.getId() == null) {
            Log.e(TAG, "Cannot update entry — ID is null.");
            return;
        }

        CollectionReference ref = getUserEntriesRef();
        if (ref == null) return;

        ref.document(entry.getId()).set(entry)
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