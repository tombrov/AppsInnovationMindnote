package com.example.mindnote;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private LinearLayout recentEntriesContainer;
    private JournalDataManager dataManager;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user == null) {
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            } else {
                initializeUI();
                loadRecentEntries();
            }
        };

        mAuth.addAuthStateListener(mAuthListener);
    }

    private void initializeUI() {
        dataManager = JournalDataManager.getInstance(this);
        FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(this);
        dataManager.setAnalytics(analytics);

        MaterialButton addEntryButton = findViewById(R.id.addEntryButton);
        TextView viewAllButton = findViewById(R.id.viewAllButton);
        recentEntriesContainer = findViewById(R.id.recentEntriesContainer);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);

        // Set "Home" as selected
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);

        addEntryButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, JournalActivity.class);
            startActivity(intent);
        });

        viewAllButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NotesActivity.class);
            startActivity(intent);
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                return true;
            } else if (itemId == R.id.navigation_journal) {
                startActivity(new Intent(MainActivity.this, JournalActivity.class));
                return true;
            } else if (itemId == R.id.navigation_notes) {
                startActivity(new Intent(MainActivity.this, NotesActivity.class));
                return true;
            } else if (itemId == R.id.navigation_calendar) {
                startActivity(new Intent(MainActivity.this, CalendarActivity.class));
                return true;
            } else if (itemId == R.id.navigation_profile) {
                return true;
            }
            return false;
        });
    }

    private void loadRecentEntries() {
        dataManager.loadEntriesFromFirestore(entries -> {
            recentEntriesContainer.removeAllViews();

            int max = Math.min(entries.size(), 3);
            for (int i = 0; i < max; i++) {
                JournalEntry entry = entries.get(i);
                View entryView = LayoutInflater.from(this).inflate(R.layout.item_recent_entry, recentEntriesContainer, false);

                TextView notePreview = entryView.findViewById(R.id.contentText);
                TextView entryDate = entryView.findViewById(R.id.dateText);
                ImageView imageView = entryView.findViewById(R.id.entryImage);

                notePreview.setText(entry.getNote());
                entryDate.setText(entry.getShortDate());

                if (entry.getImagePath() != null && !entry.getImagePath().isEmpty()) {
                    Glide.with(this).load(entry.getImagePath()).into(imageView);
                } else {
                    imageView.setVisibility(View.GONE);
                }

                entryView.setOnClickListener(v -> {
                    Intent detailIntent = new Intent(MainActivity.this, EntryDetailActivity.class);
                    detailIntent.putExtra("entryId", entry.getId());
                    startActivity(detailIntent);
                });

                recentEntriesContainer.addView(entryView);
            }

            updateStats();
        });
    }

    private void updateStats() {
        int entryCount = dataManager.getEntryCount();

        TextView streakTextView = findViewById(R.id.streakTextView);
        TextView entriesTextView = findViewById(R.id.entriesTextView);
        ImageView streakFlameIcon = findViewById(R.id.streakFlameIcon);

        int streak = calculateStreak();
        if (streakTextView != null) {
            streakTextView.setText(streak + " day streak");
        }

        if (streakFlameIcon != null) {
            streakFlameIcon.setVisibility(streak >= 3 ? View.VISIBLE : View.GONE);
        }

        if (entriesTextView != null) {
            entriesTextView.setText(entryCount + " total entries");
        }
    }

    private int calculateStreak() {
        List<JournalEntry> allEntries = dataManager.getAllEntries();
        if (allEntries.isEmpty()) return 0;

        int streak = 0;
        Calendar today = Calendar.getInstance();

        for (JournalEntry entry : allEntries) {
            Calendar entryDate = Calendar.getInstance();
            entryDate.setTime(entry.getDate());

            if (streak == 0 && isSameDay(entryDate, today)) {
                streak++;
            } else {
                today.add(Calendar.DATE, -1);
                if (isSameDay(entryDate, today)) {
                    streak++;
                } else {
                    break;
                }
            }
        }
        return streak;
    }

    private boolean isSameDay(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAuth != null && mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }
}