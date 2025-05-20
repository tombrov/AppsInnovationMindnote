package com.example.mindnote;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class JournalActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_IMAGE_PICK = 101;
    private static final int REQUEST_CODE_PERMISSION = 102;

    private BottomNavigationView bottomNavigationView;
    private ImageButton backButton;
    private ImageButton moreButton;
    private TextView dateText;

    private TextView moodHappy, moodNeutral, moodSad;
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
    private FirebaseAnalytics analytics;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journal);

        analytics = FirebaseAnalytics.getInstance(this);
        Bundle analyticsBundle = new Bundle();
        analyticsBundle.putString("screen", "journal_activity_opened");
        analytics.logEvent("screen_view", analyticsBundle);

        TextView pageTitle = findViewById(R.id.pageTitle); // Replace with your actual ID

        if (isEditMode) {
            pageTitle.setText("Edit Entry");
        } else {
            pageTitle.setText("New Entry");
        }

        dataManager = JournalDataManager.getInstance(this);
        initViews();

        String entryId = getIntent().getStringExtra("entry_id");
        if (entryId != null) {
            isEditMode = true;
            currentEntry = dataManager.getEntryById(entryId);
            if (currentEntry != null) {
                populateEntryData();
                Bundle editBundle = new Bundle();
                editBundle.putString("event_type", "edit_entry");
                editBundle.putString("entry_id", entryId);
                analytics.logEvent("journal_entry_edit", editBundle);
            } else {
                currentEntry = new JournalEntry();
            }
        } else {
            currentEntry = new JournalEntry();
            Bundle newEntryBundle = new Bundle();
            newEntryBundle.putString("event_type", "create_entry");
            analytics.logEvent("journal_entry_create", newEntryBundle);
        }

        setCurrentDate();
        setupButtons();
        setupBottomNavigation();
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        moreButton = findViewById(R.id.moreButton);
        dateText = findViewById(R.id.dateText);

        moodHappy = findViewById(R.id.moodHappy);
        moodNeutral = findViewById(R.id.moodNeutral);
        moodSad = findViewById(R.id.moodSad);

        gratitudeInput = findViewById(R.id.gratitudeInput);
        tagWork = findViewById(R.id.tagWork);
        tagFamily = findViewById(R.id.tagFamily);
        tagHealth = findViewById(R.id.tagHealth);
        tagPersonal = findViewById(R.id.tagPersonal);
        addPhotoButton = findViewById(R.id.addPhotoButton);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);
        entryImage = findViewById(R.id.entryImage);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
    }

    private void populateEntryData() {
        if (currentEntry.getMood() >= 0 && currentEntry.getMood() <= 2) {
            selectMood(currentEntry.getMood());
        }
        gratitudeInput.setText(currentEntry.getNote());

        List<String> tags = currentEntry.getTags();
        if (tags != null) {
            if (tags.contains("Work")) tagWork.setChecked(true);
            if (tags.contains("Family")) tagFamily.setChecked(true);
            if (tags.contains("Health")) tagHealth.setChecked(true);
            if (tags.contains("Personal")) tagPersonal.setChecked(true);
        }

        dateText.setText(currentEntry.getFormattedDate());

        if (entryImage != null) {
            String imagePath = currentEntry.getImagePath();
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
                Glide.with(this).load(imagePath).into(entryImage);
                entryImage.setVisibility(View.VISIBLE);
            } else {
                entryImage.setVisibility(View.GONE);
            }
        }
    }

    private void setCurrentDate() {
        if (!isEditMode) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
            dateText.setText(dateFormat.format(new Date()));
        }
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.navigation_journal);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_journal) return true;
            else if (id == R.id.navigation_notes) startActivity(new Intent(this, NotesActivity.class));
            else if (id == R.id.navigation_home) startActivity(new Intent(this, MainActivity.class));
            else return true;
            finish();
            return true;
        });
    }

    private void selectMood(int index) {
        TextView[] moodViews = {moodHappy, moodNeutral, moodSad};

        if (selectedMoodIndex == index) {
            // Unselect if already selected
            moodViews[index].setBackground(null);
            moodViews[index].setTextColor(Color.BLACK);
            selectedMoodIndex = -1;
            Toast.makeText(this, "Mood unselected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Set selection
        for (int i = 0; i < moodViews.length; i++) {
            if (i == index) {
                moodViews[i].setBackgroundResource(R.drawable.mood_selected_background);
                moodViews[i].setTextColor(Color.WHITE);
            } else {
                moodViews[i].setBackground(null);
                moodViews[i].setTextColor(Color.BLACK);
            }
        }

        selectedMoodIndex = index;
        String[] moodNames = {"Happy", "Neutral", "Sad"};
        Toast.makeText(this, "Selected mood: " + moodNames[index], Toast.LENGTH_SHORT).show();
    }


    private void setupButtons() {
        moodHappy.setOnClickListener(v -> selectMood(0));
        moodNeutral.setOnClickListener(v -> selectMood(1));
        moodSad.setOnClickListener(v -> selectMood(2));

        backButton.setOnClickListener(v -> onBackPressed());
        moreButton.setOnClickListener(v -> Toast.makeText(this, "More options", Toast.LENGTH_SHORT).show());

        addPhotoButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_CODE_PERMISSION);
            } else {
                pickImageFromGallery();
            }
        });

        saveButton.setOnClickListener(v -> saveEntry());
        cancelButton.setOnClickListener(v -> finish());
    }

    private void saveEntry() {
        String note = gratitudeInput.getText().toString().trim();

        if (selectedMoodIndex == -1) {
            Toast.makeText(this, "Please select your mood", Toast.LENGTH_SHORT).show();
            return;
        }

        if (note.isEmpty()) {
            Toast.makeText(this, "Please write a gratitude note", Toast.LENGTH_SHORT).show();
            return;
        }

        currentEntry.setMood(selectedMoodIndex);
        currentEntry.setNote(note);

        List<String> tags = new ArrayList<>();
        if (tagWork.isChecked()) tags.add("Work");
        if (tagFamily.isChecked()) tags.add("Family");
        if (tagHealth.isChecked()) tags.add("Health");
        if (tagPersonal.isChecked()) tags.add("Personal");
        currentEntry.setTags(tags);

        if (selectedImageUri != null) {
            uploadImageAndSaveEntry();
        } else {
            persistEntry();
        }
    }

    private void uploadImageAndSaveEntry() {
        String fileName = "journal_images/" + System.currentTimeMillis() + ".jpg";
        StorageReference storageRef = FirebaseStorage.getInstance().getReference(fileName);
        storageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot ->
                        storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            currentEntry.setImagePath(uri.toString());
                            persistEntry();
                        }))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show();
                    persistEntry();
                });
    }

    private void persistEntry() {
        if (isEditMode) {
            dataManager.updateEntry(currentEntry);
            Toast.makeText(this, "Entry updated!", Toast.LENGTH_SHORT).show();
        } else {
            dataManager.addEntry(currentEntry);
            Toast.makeText(this, "Entry saved!", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            pickImageFromGallery();
        } else {
            Toast.makeText(this, "Permission denied to read media", Toast.LENGTH_SHORT).show();
        }
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_IMAGE_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_IMAGE_PICK &&
                resultCode == RESULT_OK &&
                data != null) {
            selectedImageUri = data.getData();
            entryImage.setVisibility(View.VISIBLE);
            entryImage.setImageURI(selectedImageUri);
        }
    }
}
