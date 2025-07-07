package com.example.mindnote;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private NotesAdapter adapter;
    private List<JournalEntry> allEntries = new ArrayList<>();
    private List<JournalEntry> filteredEntries = new ArrayList<>();
    private JournalDataManager dataManager;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        calendarView = findViewById(R.id.calendarView);
        RecyclerView notesRecyclerView = findViewById(R.id.notesRecyclerView);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.navigation_calendar);

        dataManager = JournalDataManager.getInstance(this);
        adapter = new NotesAdapter(filteredEntries, this::openEntryDetail);

        notesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notesRecyclerView.setAdapter(adapter);

        loadEntries();

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, dayOfMonth);
            filterEntriesByDate(selected.getTime());
        });

        setupBottomNavigation();
    }

    private void loadEntries() {
        dataManager.loadEntriesFromFirestore(entries -> {
            allEntries.clear();
            allEntries.addAll(entries);
            filterEntriesByDate(new Date(calendarView.getDate()));
        });
    }

    private void filterEntriesByDate(Date selectedDate) {
        filteredEntries.clear();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String target = fmt.format(selectedDate);

        for (JournalEntry entry : allEntries) {
            if (entry.getDate() != null && fmt.format(entry.getDate()).equals(target)) {
                filteredEntries.add(entry);
            }
        }

        adapter.notifyDataSetChanged();

        if (filteredEntries.isEmpty()) {
            Toast.makeText(this, "No entries for selected date", Toast.LENGTH_SHORT).show();
        }
    }

    private void openEntryDetail(JournalEntry entry) {
        Intent intent = new Intent(this, EntryDetailActivity.class);
        intent.putExtra("entryId", entry.getId());
        startActivity(intent);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.navigation_calendar);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            } else if (itemId == R.id.navigation_journal) {
                startActivity(new Intent(this, JournalActivity.class));
                return true;
            } else if (itemId == R.id.navigation_notes) {
                startActivity(new Intent(this, NotesActivity.class));
                return true;
            } else if (itemId == R.id.navigation_calendar) {
                return true;
            } else if (itemId == R.id.navigation_profile) {
                return true;
            }
            return false;
        });
    }
}
