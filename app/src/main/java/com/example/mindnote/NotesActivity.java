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

        notesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        addNoteButton.setOnClickListener(v -> {
            Intent intent = new Intent(NotesActivity.this, JournalActivity.class);
            startActivity(intent);
        });

        bottomNavigation.setSelectedItemId(R.id.navigation_notes);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            } else if (itemId == R.id.navigation_journal) {
                startActivity(new Intent(this, JournalActivity.class));
                return true;
            } else if (itemId == R.id.navigation_calendar) {
                startActivity(new Intent(this, CalendarActivity.class));
                return true;
            } else if (itemId == R.id.navigation_profile) {
                return true;
            } else if (itemId == R.id.navigation_notes) {
                return true;
            }

            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        dataManager.loadEntriesFromFirestore(entries -> {
            if (entries.isEmpty()) {
                emptyStateContainer.setVisibility(View.VISIBLE);
                notesRecyclerView.setVisibility(View.GONE);
            } else {
                emptyStateContainer.setVisibility(View.GONE);
                notesRecyclerView.setVisibility(View.VISIBLE);

                notesAdapter = new NotesAdapter(entries, entry -> {
                    Intent intent = new Intent(NotesActivity.this, EntryDetailActivity.class);
                    intent.putExtra("title", entry.getNote());
                    intent.putExtra("content", entry.getNote()); // or separate content if available
                    intent.putExtra("imageUrl", entry.getImagePath());
                    intent.putExtra("timestamp", entry.getFormattedDate());
                    startActivity(intent);
                });

                notesRecyclerView.setAdapter(notesAdapter);
            }
        });
    }
}
