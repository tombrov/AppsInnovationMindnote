
package com.example.mindnote;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.CalendarView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class CalendarActivity extends AppCompatActivity implements NotesAdapter.OnNoteClickListener {

    private CalendarView calendarView;
    private RecyclerView recyclerView;
    private NotesAdapter adapter;
    private FirebaseFirestore db;
    private List<JournalEntry> entryList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        calendarView = findViewById(R.id.calendarView);
        recyclerView = findViewById(R.id.notesRecyclerView);
        db = FirebaseFirestore.getInstance();

        adapter = new NotesAdapter(entryList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(year, month, dayOfMonth, 0, 0, 0);
            selectedDate.set(Calendar.MILLISECOND, 0);
            loadEntries(selectedDate);
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setSelectedItemId(R.id.navigation_calendar);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.navigation_notes) {
                startActivity(new Intent(this, NotesActivity.class));
                finish();
                return true;
            } else if (id == R.id.navigation_journal) {
                startActivity(new Intent(this, JournalActivity.class));
                finish();
                return true;
            } else if (id == R.id.navigation_calendar) {
                return true;
            } else if (id == R.id.navigation_profile) {
                return true;
            }
            return false;
        });
    }

    private void loadEntries(Calendar selectedDate) {
        Date start = getStartOfDay(selectedDate);
        Date end = getEndOfDay(selectedDate);

        db.collection("entries")
                .whereGreaterThanOrEqualTo("date", start)
                .whereLessThan("date", end)
                .get()
                .addOnSuccessListener(query -> {
                    entryList.clear();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        JournalEntry entry = new JournalEntry();
                        entry.setId(doc.getId());
                        entry.setNote(doc.getString("note"));
                        entry.setMood(doc.contains("mood") ? doc.getLong("mood").intValue() : 1);
                        entry.setImagePath(doc.getString("imagePath"));
                        entry.setTags((List<String>) doc.get("tags"));
                        entry.setDate(doc.getDate("date"));
                        entryList.add(entry);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private Date getStartOfDay(Calendar date) {
        Calendar c = (Calendar) date.clone();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private Date getEndOfDay(Calendar date) {
        Calendar c = (Calendar) date.clone();
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTime();
    }

    @Override
    public void onNoteClick(JournalEntry entry) {
        Intent intent = new Intent(this, JournalActivity.class);
        intent.putExtra("entry", entry);
        startActivity(intent);
    }
}
