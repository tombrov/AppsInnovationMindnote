package com.example.mindnote;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.List;

public class NotesActivity extends AppCompatActivity {

    private RecyclerView notesRecyclerView;
    private LinearLayout emptyStateContainer;
    private NotesAdapter notesAdapter;
    private JournalDataManager dataManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        notesRecyclerView = findViewById(R.id.notesRecyclerView);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);
        Button addNoteButton = findViewById(R.id.addNoteButton);
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

        dataManager = JournalDataManager.getInstance(this);
        FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(this);
        dataManager.setAnalytics(analytics);

        notesAdapter = new NotesAdapter(this);
        notesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notesRecyclerView.setAdapter(notesAdapter);

        addNoteButton.setOnClickListener(v -> startActivity(new Intent(this, JournalActivity.class)));

        bottomNavigation.setSelectedItemId(R.id.navigation_notes); // Use correct ID from menu
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_notes) {
                return true; // already here
            } else if (itemId == R.id.navigation_journal) {
                startActivity(new Intent(this, JournalActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.navigation_home) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.navigation_calendar) {
                startActivity(new Intent(this, CalendarActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.navigation_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });

        loadNotes();
    }

    private void loadNotes() {
        dataManager.loadEntriesFromFirestore(entries -> {
            if (entries != null && !entries.isEmpty()) {
                notesAdapter.setEntries(entries);
                emptyStateContainer.setVisibility(View.GONE);
                notesRecyclerView.setVisibility(View.VISIBLE);
            } else {
                notesRecyclerView.setVisibility(View.GONE);
                emptyStateContainer.setVisibility(View.VISIBLE);
            }
        });
    }
}
