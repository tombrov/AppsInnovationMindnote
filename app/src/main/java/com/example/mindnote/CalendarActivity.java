package com.example.mindnote;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CalendarView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class CalendarActivity extends AppCompatActivity {

    private JournalDataManager dataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        CalendarView calendarView = findViewById(R.id.calendarView);
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

        dataManager = JournalDataManager.getInstance(this);

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, dayOfMonth, 0, 0, 0);
            selected.set(Calendar.MILLISECOND, 0);
            Date selectedDate = selected.getTime();

            List<JournalEntry> allEntries = dataManager.getAllEntries();
            for (JournalEntry entry : allEntries) {
                if (entry.getDate() != null && isSameDay(entry.getDate(), selectedDate)) {
                    Intent intent = new Intent(this, JournalActivity.class);
                    intent.putExtra("entryId", entry.getId());
                    startActivity(intent);
                    break;
                }
            }
        });

        bottomNavigation.setSelectedItemId(R.id.navigation_calendar);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                startActivity(new Intent(this, MainActivity.class));
            } else if (id == R.id.navigation_notes) {
                startActivity(new Intent(this, NotesActivity.class));
            } else if (id == R.id.navigation_journal) {
                startActivity(new Intent(this, JournalActivity.class));
            } else if (id == R.id.navigation_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
            } else if (id == R.id.navigation_calendar) {
                return true;
            } else {
                return false;
            }
            overridePendingTransition(0, 0);
            return true;
        });
    }

    private boolean isSameDay(Date d1, Date d2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(d1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(d2);

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
}
