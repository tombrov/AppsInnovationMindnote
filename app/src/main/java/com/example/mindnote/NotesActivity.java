package com.example.mindnote;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class NotesActivity extends AppCompatActivity {

    private RecyclerView notesRecyclerView;
    private LinearLayout emptyStateContainer;
    private NotesAdapter notesAdapter;
    private JournalDataManager dataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        notesRecyclerView = findViewById(R.id.notesRecyclerView);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);
        Button addNoteButton = findViewById(R.id.addNoteButton);
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

        dataManager = JournalDataManager.getInstance(this);

        notesAdapter = new NotesAdapter(this);
        notesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notesRecyclerView.setAdapter(notesAdapter);

        addNoteButton.setOnClickListener(v ->
                startActivity(new Intent(this, JournalActivity.class)));

        bottomNavigation.setSelectedItemId(R.id.navigation_notes);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                startActivity(new Intent(this, MainActivity.class));
            } else if (id == R.id.navigation_calendar) {
                startActivity(new Intent(this, CalendarActivity.class));
            } else if (id == R.id.navigation_journal) {
                startActivity(new Intent(this, JournalActivity.class));
            } else if (id == R.id.navigation_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
            } else if (id == R.id.navigation_notes) {
                return true;
            } else {
                return false;
            }
            overridePendingTransition(0, 0);
            return true;
        });

        loadNotes();
        enableSwipeToDelete();
    }

    private void loadNotes() {
        dataManager.loadEntriesFromFirestore(entries -> {
            if (entries.isEmpty()) {
                emptyStateContainer.setVisibility(View.VISIBLE);
                notesRecyclerView.setVisibility(View.GONE);
            } else {
                emptyStateContainer.setVisibility(View.GONE);
                notesRecyclerView.setVisibility(View.VISIBLE);
                notesAdapter.setEntries(entries);
            }
        });
    }

    private void enableSwipeToDelete() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private final ColorDrawable background = new ColorDrawable(ContextCompat.getColor(NotesActivity.this, R.color.deleteRed));
            private final Drawable deleteIcon = ContextCompat.getDrawable(NotesActivity.this, R.drawable.ic_delete);
            private final int iconMargin = 32;

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                JournalEntry entry = notesAdapter.getEntryAt(position);
                confirmDelete(entry, position);
            }

            @Override
            public void onChildDraw(@NonNull Canvas canvas, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;

                if (dX < 0) {
                    background.setBounds(itemView.getRight() + (int) dX,
                            itemView.getTop(), itemView.getRight(), itemView.getBottom());
                    background.draw(canvas);

                    if (deleteIcon != null) {
                        int iconTop = itemView.getTop() + (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                        int iconLeft = itemView.getRight() - iconMargin - deleteIcon.getIntrinsicWidth();
                        int iconRight = itemView.getRight() - iconMargin;
                        int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();

                        deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                        deleteIcon.draw(canvas);
                    }
                }

                super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        new ItemTouchHelper(swipeCallback).attachToRecyclerView(notesRecyclerView);
    }

    private void confirmDelete(JournalEntry entry, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Entry")
                .setMessage("Are you sure you want to delete this entry?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dataManager.deleteEntry(entry.getId(), success -> {
                        if (success) {
                            notesAdapter.removeEntryAt(position);
                            Toast.makeText(this, "Entry deleted", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", (dialog, which) -> notesAdapter.notifyItemChanged(position))
                .show();
    }
}
