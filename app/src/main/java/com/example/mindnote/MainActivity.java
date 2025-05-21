package com.example.mindnote;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private MaterialButton addEntryButton;
    private TextView viewAllButton;
    private TextView streakCountText;
    private TextView entriesCountText;
    private LinearLayout recentEntriesContainer;
    private JournalDataManager dataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dataManager = JournalDataManager.getInstance(this);

        bottomNavigationView = findViewById(R.id.bottomNavigation);
        addEntryButton = findViewById(R.id.addEntryButton);
        viewAllButton = findViewById(R.id.viewAllButton);

        recentEntriesContainer = findViewById(R.id.recentEntriesContainer);
        if (recentEntriesContainer == null) {
            View scrollView = findViewById(R.id.scrollView);
            if (scrollView instanceof android.widget.ScrollView) {
                LinearLayout parent = (LinearLayout) ((android.widget.ScrollView) scrollView).getChildAt(0);
                recentEntriesContainer = parent;
            }
        }

        updateStats();
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_journal) {
                startActivity(new Intent(MainActivity.this, JournalActivity.class));
                return true;
            } else if (itemId == R.id.navigation_notes) {
                startActivity(new Intent(MainActivity.this, NotesActivity.class));
                return true;
            } else if (itemId == R.id.navigation_home) {
                return true;
            } else if (itemId == R.id.navigation_calendar) {
                startActivity(new Intent(MainActivity.this, CalendarActivity.class));
                return true;
            } else if (itemId == R.id.navigation_profile) {
                Snackbar.make(findViewById(android.R.id.content), "Profile feature coming soon", Snackbar.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        addEntryButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, JournalActivity.class)));
        viewAllButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, NotesActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStats();
        updateRecentEntries();
    }

    private void updateStats() {
        int entryCount = dataManager.getEntryCount();

        TextView streakTextView = findViewById(R.id.streakTextView);
        ImageView streakFlameIcon = findViewById(R.id.streakFlameIcon);
        TextView entriesTextView = findViewById(R.id.entriesTextView);

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
        List<JournalEntry> entries = dataManager.getAllEntries();

        entries.sort((e1, e2) -> e2.getDate().compareTo(e1.getDate()));

        int streak = 0;
        long currentDay = System.currentTimeMillis() / (1000 * 60 * 60 * 24);

        for (JournalEntry entry : entries) {
            long entryDay = entry.getDate().getTime() / (1000 * 60 * 60 * 24);
            if (entryDay == currentDay - streak) {
                streak++;
            } else if (entryDay < currentDay - streak) {
                break;
            }
        }

        return streak;
    }

    private void updateRecentEntries() {
        if (recentEntriesContainer == null) return;
        recentEntriesContainer.removeAllViews();

        dataManager.loadEntriesFromFirestore(entries -> {
            int entryLimit = Math.min(entries.size(), 3);

            if (entries.isEmpty()) {
                if (recentEntriesContainer.getChildCount() == 0) {
                    View emptyView = LayoutInflater.from(this).inflate(R.layout.empty_recent_entries, recentEntriesContainer, false);
                    recentEntriesContainer.addView(emptyView);
                }
                return;
            }

            for (int i = 0; i < entryLimit; i++) {
                JournalEntry entry = entries.get(i);
                View entryView = LayoutInflater.from(this).inflate(R.layout.item_recent_entry, recentEntriesContainer, false);

                TextView dateText = entryView.findViewById(R.id.dateText);
                TextView contentText = entryView.findViewById(R.id.contentText);
                ImageView entryImage = entryView.findViewById(R.id.entryImage);

                String dateDisplay = (i == 0)
                        ? "Today, " + entry.getFormattedTime()
                        : (i == 1)
                        ? "Yesterday, " + entry.getFormattedTime()
                        : entry.getShortDate();

                dateText.setText(dateDisplay);
                contentText.setText(entry.getNote());

                String imagePath = entry.getImagePath();
                if (JournalDataManager.isDemoImage(imagePath)) {
                    if (imagePath.equals(JournalDataManager.DEMO_IMAGE_FAMILY)) {
                        entryImage.setImageResource(R.drawable.family_sunset);
                    } else if (imagePath.equals(JournalDataManager.DEMO_IMAGE_MEDITATION)) {
                        entryImage.setImageResource(R.drawable.meditation_sunrise);
                    } else if (imagePath.equals(JournalDataManager.DEMO_IMAGE_LIGHTBULB)) {
                        entryImage.setImageResource(R.drawable.lightbulb);
                    }
                    entryImage.setVisibility(View.VISIBLE);
                } else if (imagePath != null && !imagePath.isEmpty()) {
                    Glide.with(this)
                            .load(imagePath)
                            .into(entryImage);
                    entryImage.setVisibility(View.VISIBLE);
                } else {
                    entryImage.setVisibility(View.GONE);
                }

                entryView.setOnClickListener(v -> {
                    Intent intent = new Intent(MainActivity.this, JournalActivity.class);
                    intent.putExtra("entry_id", entry.getId());
                    startActivity(intent);
                });

                recentEntriesContainer.addView(entryView);
            }
        });
    }
}