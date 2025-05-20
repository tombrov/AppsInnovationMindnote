package com.example.mindnote;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

        // Initialize data manager
        dataManager = JournalDataManager.getInstance(this);

        // Initialize views
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        addEntryButton = findViewById(R.id.addEntryButton);
        viewAllButton = findViewById(R.id.viewAllButton);

        // Find the container for recent entries
        recentEntriesContainer = findViewById(R.id.recentEntriesContainer);
        if (recentEntriesContainer == null) {
            // If the exact structure isn't found, let's try to find the parent ScrollView and add our container
            View scrollView = findViewById(R.id.scrollView);
            if (scrollView != null && scrollView instanceof android.widget.ScrollView) {
                LinearLayout parent = (LinearLayout) ((android.widget.ScrollView) scrollView).getChildAt(0);
                recentEntriesContainer = parent;
            }
        }

        // Update streak and entries count
        updateStats();

        // Set Home as selected item
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);

        // Set up bottom navigation listener
        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        int itemId = item.getItemId();
                        if (itemId == R.id.navigation_journal) {
                            // Navigate to Journal activity
                            Intent intent = new Intent(MainActivity.this, JournalActivity.class);
                            startActivity(intent);
                            return true;
                        } else if (itemId == R.id.navigation_notes) {
                            // Navigate to Notes activity
                            Intent intent = new Intent(MainActivity.this, NotesActivity.class);
                            startActivity(intent);
                            return true;
                        } else if (itemId == R.id.navigation_home) {
                            // Already on home
                            return true;
                        } else if (itemId == R.id.navigation_calendar) {
                            // Handle calendar click
                            Toast.makeText(MainActivity.this, "Calendar feature coming soon", Toast.LENGTH_SHORT).show();
                            return true;
                        } else if (itemId == R.id.navigation_profile) {
                            // Handle profile click
                            Toast.makeText(MainActivity.this, "Profile feature coming soon", Toast.LENGTH_SHORT).show();
                            return true;
                        }
                        return false;
                    }
                });

        // Set up add entry button click listener
        addEntryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to Journal activity for new entry
                Intent intent = new Intent(MainActivity.this, JournalActivity.class);
                startActivity(intent);
            }
        });

        // Set up view all button click listener
        viewAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to Notes activity to see all entries
                Intent intent = new Intent(MainActivity.this, NotesActivity.class);
                startActivity(intent);
            }
        });

        // Populate recent entries
        updateRecentEntries();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the data when returning to this activity
        updateStats();
        updateRecentEntries();
    }

    private void updateStats() {
        // Update total entries count
        int entryCount = dataManager.getEntryCount();

        // Find the TextViews within the CardViews
        TextView streakTextView = findViewById(R.id.streakTextView);
        TextView entriesTextView = findViewById(R.id.entriesTextView);

        if (streakTextView != null) {
            streakTextView.setText("7 day streak");  // Static for now
        }

        if (entriesTextView != null) {
            entriesTextView.setText(entryCount + " total entries");
        }
    }

    private void updateRecentEntries() {
        // Ensure we have the container
        if (recentEntriesContainer == null) {
            return;
        }

        // Clear existing entries
        recentEntriesContainer.removeAllViews();

        // Get recent entries (limit to 3)
        List<JournalEntry> entries = dataManager.getAllEntries();
        int entryLimit = Math.min(entries.size(), 3);

        if (entries.isEmpty()) {
            // Show a message if no entries exist
            View emptyView = LayoutInflater.from(this).inflate(R.layout.empty_recent_entries, recentEntriesContainer, false);
            recentEntriesContainer.addView(emptyView);
            return;
        }

        // Add recent entries
        for (int i = 0; i < entryLimit; i++) {
            JournalEntry entry = entries.get(i);

            // Inflate the entry view
            View entryView = LayoutInflater.from(this).inflate(R.layout.item_recent_entry, recentEntriesContainer, false);

            // Get references to views
            TextView dateText = entryView.findViewById(R.id.dateText);
            TextView contentText = entryView.findViewById(R.id.contentText);
            ImageView entryImage = entryView.findViewById(R.id.entryImage);

            // Set data
            String dateDisplay = "";
            if (i == 0) {
                dateDisplay = "Today, " + entry.getFormattedTime();
            } else if (i == 1) {
                dateDisplay = "Yesterday, " + entry.getFormattedTime();
            } else {
                dateDisplay = entry.getShortDate();
            }

            dateText.setText(dateDisplay);
            contentText.setText(entry.getNote());

            // Check if this is a demo entry with image
            String imagePath = entry.getImagePath();
            if (JournalDataManager.isDemoImage(imagePath)) {
                // Set image resource based on the marker
                if (imagePath.equals(JournalDataManager.DEMO_IMAGE_FAMILY)) {
                    entryImage.setImageResource(R.drawable.family_sunset);
                    entryImage.setVisibility(View.VISIBLE);
                } else if (imagePath.equals(JournalDataManager.DEMO_IMAGE_MEDITATION)) {
                    entryImage.setImageResource(R.drawable.meditation_sunrise);
                    entryImage.setVisibility(View.VISIBLE);
                } else if (imagePath.equals(JournalDataManager.DEMO_IMAGE_LIGHTBULB)) {
                    entryImage.setImageResource(R.drawable.lightbulb);
                    entryImage.setVisibility(View.VISIBLE);
                }
            } else {
                // Not a demo entry with image, keep it hidden
                entryImage.setVisibility(View.GONE);
            }

            // Add click listener to view the entry
            final String entryId = entry.getId();
            entryView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, JournalActivity.class);
                    intent.putExtra("entry_id", entryId);
                    startActivity(intent);
                }
            });

            // Add to container
            recentEntriesContainer.addView(entryView);
        }
    }
}