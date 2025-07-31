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

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private MaterialButton addEntryButton;
    private TextView viewAllButton;
    private LinearLayout recentEntriesContainer;
    private JournalDataManager dataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dataManager = JournalDataManager.getInstance(this);
        FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(this);
        dataManager.setAnalytics(analytics);

        bottomNavigationView = findViewById(R.id.bottomNavigation);
        addEntryButton = findViewById(R.id.addEntryButton);
        viewAllButton = findViewById(R.id.viewAllButton);
        recentEntriesContainer = findViewById(R.id.recentEntriesContainer);

        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) return true;
            if (id == R.id.navigation_calendar) {
                startActivity(new Intent(this, CalendarActivity.class));
                return true;
            }
            if (id == R.id.navigation_notes) {
                startActivity(new Intent(this, NotesActivity.class));
                return true;
            }
            if (id == R.id.navigation_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            if (id == R.id.navigation_journal) {
                startActivity(new Intent(this, JournalActivity.class));
                return true;
            }
            return false;
        });

        addEntryButton.setOnClickListener(v ->
                startActivity(new Intent(this, JournalActivity.class)));

        viewAllButton.setOnClickListener(v ->
                startActivity(new Intent(this, NotesActivity.class)));

        loadRecentEntries();
    }

    private void loadRecentEntries() {
        dataManager.loadEntriesFromFirestore(entries -> {
            recentEntriesContainer.removeAllViews();
            List<JournalEntry> recent = entries.size() > 3 ? entries.subList(0, 3) : entries;

            LayoutInflater inflater = LayoutInflater.from(this);
            for (JournalEntry entry : recent) {
                View card = inflater.inflate(R.layout.item_recent_entry, recentEntriesContainer, false);
                TextView dateText = card.findViewById(R.id.dateText);
                TextView noteText = card.findViewById(R.id.contentText);
                ImageView entryImage = card.findViewById(R.id.entryImage);

                dateText.setText(entry.getShortDate());
                noteText.setText(entry.getNote());

                if (entry.getImagePath() != null && !JournalDataManager.isDemoImage(entry.getImagePath())) {
                    Glide.with(this).load(entry.getImagePath()).into(entryImage);
                    entryImage.setVisibility(View.VISIBLE);
                } else {
                    entryImage.setVisibility(View.GONE);
                }

                card.setOnClickListener(v -> {
                    Intent intent = new Intent(this, JournalActivity.class);
                    intent.putExtra("entryId", entry.getId());
                    startActivity(intent);
                });

                recentEntriesContainer.addView(card);
            }
        });
    }
}
