package com.example.mindnote;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class JournalActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private ImageButton backButton;
    private ImageButton moreButton;
    private TextView dateText;
    private ImageButton[] moodButtons;
    private EditText gratitudeInput;
    private Chip tagWork, tagFamily, tagHealth, tagPersonal;
    private Button addPhotoButton;
    private Button saveButton;
    private TextView cancelButton;
    private ImageView entryImage;
    private int selectedMoodIndex = -1;
    private JournalDataManager dataManager;
    private JournalEntry currentEntry;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journal);

        // Initialize data manager
        dataManager = JournalDataManager.getInstance(this);

        // Initialize views
        initViews();

        // Check if we're editing an existing entry
        String entryId = getIntent().getStringExtra("entry_id");
        if (entryId != null) {
            isEditMode = true;
            currentEntry = dataManager.getEntryById(entryId);
            if (currentEntry != null) {
                populateEntryData();
            } else {
                // Entry not found, create new
                currentEntry = new JournalEntry();
            }
        } else {
            // Create a new entry
            currentEntry = new JournalEntry();
        }

        // Set current date
        setCurrentDate();

        // Set up buttons
        setupButtons();

        // Set up bottom navigation
        setupBottomNavigation();
    }

    private void initViews() {
        // Top bar
        backButton = findViewById(R.id.backButton);
        moreButton = findViewById(R.id.moreButton);

        // Date
        dateText = findViewById(R.id.dateText);

        // Mood buttons
        moodButtons = new ImageButton[3];
        moodButtons[0] = findViewById(R.id.moodHappy);
        moodButtons[1] = findViewById(R.id.moodNeutral);
        moodButtons[2] = findViewById(R.id.moodSad);

        // Gratitude input
        gratitudeInput = findViewById(R.id.gratitudeInput);

        // Tags
        tagWork = findViewById(R.id.tagWork);
        tagFamily = findViewById(R.id.tagFamily);
        tagHealth = findViewById(R.id.tagHealth);
        tagPersonal = findViewById(R.id.tagPersonal);

        // Action buttons
        addPhotoButton = findViewById(R.id.addPhotoButton);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);

        // Entry image view (if it exists in the layout)
        entryImage = findViewById(R.id.entryImage);

        // Bottom navigation
        bottomNavigationView = findViewById(R.id.bottomNavigation);
    }

    private void populateEntryData() {
        // Set mood - IMPORTANT to call selectMood to highlight the right mood icon
        if (currentEntry.getMood() >= 0 && currentEntry.getMood() < moodButtons.length) {
            selectMood(currentEntry.getMood());
        }

        // Set note text
        gratitudeInput.setText(currentEntry.getNote());

        // Set tags
        List<String> tags = currentEntry.getTags();
        if (tags != null) {
            for (String tag : tags) {
                switch (tag) {
                    case "Work":
                        tagWork.setChecked(true);
                        break;
                    case "Family":
                        tagFamily.setChecked(true);
                        break;
                    case "Health":
                        tagHealth.setChecked(true);
                        break;
                    case "Personal":
                        tagPersonal.setChecked(true);
                        break;
                }
            }
        }

        // Set date
        dateText.setText(currentEntry.getFormattedDate());

        // Handle image for demo entries
        if (entryImage != null) {
            String imagePath = currentEntry.getImagePath();
            if (JournalDataManager.isDemoImage(imagePath)) {
                if (imagePath.equals(JournalDataManager.DEMO_IMAGE_FAMILY)) {
                    entryImage.setImageResource(R.drawable.family_sunset);
                    entryImage.setVisibility(View.VISIBLE);
                } else if (imagePath.equals(JournalDataManager.DEMO_IMAGE_MEDITATION)) {
                    entryImage.setImageResource(R.drawable.meditation_sunrise);
                    entryImage.setVisibility(View.VISIBLE);
                } else if (imagePath.equals(JournalDataManager.DEMO_IMAGE_LIGHTBULB)) {
                    entryImage.setImageResource(R.drawable.lightbulb);
                    entryImage.setVisibility(View.VISIBLE);
                } else {
                    entryImage.setVisibility(View.GONE);
                }
            } else {
                entryImage.setVisibility(View.GONE);
            }
        }
    }

    private void setCurrentDate() {
        if (!isEditMode) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
            String currentDate = dateFormat.format(new Date());
            dateText.setText(currentDate);
        }
    }

    private void setupButtons() {
        // Set up mood buttons
        for (int i = 0; i < moodButtons.length; i++) {
            final int index = i;
            moodButtons[i].setOnClickListener(v -> selectMood(index));
        }

        // Back button returns to previous screen
        backButton.setOnClickListener(v -> {
            onBackPressed();
        });

        // More button shows options
        moreButton.setOnClickListener(v -> {
            Toast.makeText(JournalActivity.this, "More options", Toast.LENGTH_SHORT).show();
        });

        // Add photo button
        addPhotoButton.setOnClickListener(v -> {
            Toast.makeText(JournalActivity.this, "Add photo clicked", Toast.LENGTH_SHORT).show();
        });

        // Save button
        saveButton.setOnClickListener(v -> {
            saveEntry();
        });

        // Cancel button
        cancelButton.setOnClickListener(v -> {
            finish();
        });
    }

    private void selectMood(int index) {
        // Reset all mood buttons to full opacity
        for (ImageButton button : moodButtons) {
            button.setAlpha(1.0f);
        }

        // Select new mood and reduce its opacity to show it's selected
        selectedMoodIndex = index;
        moodButtons[selectedMoodIndex].setAlpha(0.6f);

        // Add a border or background to make the selection more obvious
        // You could also change the background here or add a selection indicator

        String[] moodNames = {"Happy", "Neutral", "Sad"};
        Toast.makeText(this, "Selected mood: " + moodNames[index], Toast.LENGTH_SHORT).show();
    }

    private void saveEntry() {
        // Validate input
        if (selectedMoodIndex == -1) {
            Toast.makeText(this, "Please select your mood", Toast.LENGTH_SHORT).show();
            return;
        }

        if (gratitudeInput.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Please write a gratitude note", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update entry data
        currentEntry.setMood(selectedMoodIndex);
        currentEntry.setNote(gratitudeInput.getText().toString().trim());

        // Get selected tags
        List<String> tags = new ArrayList<>();
        if (tagWork.isChecked()) tags.add("Work");
        if (tagFamily.isChecked()) tags.add("Family");
        if (tagHealth.isChecked()) tags.add("Health");
        if (tagPersonal.isChecked()) tags.add("Personal");
        currentEntry.setTags(tags);

        // Preserve demo image path if this is a demo entry
        if (!isEditMode) {
            currentEntry.setImagePath(null); // Clear image path for new entries
        }

        // Save entry
        if (isEditMode) {
            dataManager.updateEntry(currentEntry);
            Toast.makeText(this, "Entry updated!", Toast.LENGTH_SHORT).show();
        } else {
            dataManager.addEntry(currentEntry);
            Toast.makeText(this, "Entry saved!", Toast.LENGTH_SHORT).show();
        }

        // Return to previous screen
        finish();
    }

    private void setupBottomNavigation() {
        // Set Journal as selected item
        bottomNavigationView.setSelectedItemId(R.id.navigation_journal);

        // Set up bottom navigation listener
        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        int itemId = item.getItemId();
                        if (itemId == R.id.navigation_journal) {
                            // Already on journal page
                            return true;
                        } else if (itemId == R.id.navigation_notes) {
                            // Navigate to notes
                            Intent intent = new Intent(JournalActivity.this, NotesActivity.class);
                            startActivity(intent);
                            finish();
                            return true;
                        } else if (itemId == R.id.navigation_home) {
                            // Return to home
                            Intent intent = new Intent(JournalActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                            return true;
                        } else if (itemId == R.id.navigation_calendar) {
                            // Handle calendar click (future feature)
                            return true;
                        } else if (itemId == R.id.navigation_profile) {
                            // Handle profile click (future feature)
                            return true;
                        }
                        return false;
                    }
                });
    }
}