package com.example.mindnote;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.snackbar.Snackbar;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
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
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.*;

public class JournalActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_IMAGE_PICK = 101;
    private static final int REQUEST_CODE_PERMISSION = 102;

    private BottomNavigationView bottomNavigationView;
    private ImageButton backButton, moreButton;
    private TextView dateText, pageTitle, moodHappy, moodNeutral, moodSad;
    private EditText gratitudeInput;
    private Button addPhotoButton, saveButton, deleteButton;
    private TextView cancelButton;
    private ImageView entryImage;
    private TextInputEditText tagInputEditText;
    private ChipGroup tagsChipGroup;

    private int selectedMoodIndex = -1;
    private JournalDataManager dataManager;
    private JournalEntry currentEntry;
    private boolean isEditMode = false;
    private FirebaseAnalytics analytics;
    private Uri selectedImageUri;
    private FirebaseFirestore db;

    private Set<String> selectedTags = new HashSet<>();
    private Set<String> previouslyUsedTags = new HashSet<>();

    private String originalNote = "";
    private int originalMood = -1;
    private Set<String> originalTags = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journal);
        initFirebase();
        FirebaseAnalytics analytics = JournalDataManager.getInstance(this).setAnalytics(FirebaseAnalytics.getInstance(this));
        initViews();
        initEntryState();
        loadTags();
        setupTagInput();
        setCurrentDate();
        setupButtons();
        setupBottomNavigation();
        setupGratitudeWatcher();
        updateVisibilityForEditMode();
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        dateText = findViewById(R.id.dateText);
        moodHappy = findViewById(R.id.moodHappy);
        moodNeutral = findViewById(R.id.moodNeutral);
        moodSad = findViewById(R.id.moodSad);
        gratitudeInput = findViewById(R.id.gratitudeInput);
        addPhotoButton = findViewById(R.id.addPhotoButton);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);
        entryImage = findViewById(R.id.entryImage);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        pageTitle = findViewById(R.id.pageTitle);
        deleteButton = findViewById(R.id.deleteEntryButton);
        tagInputEditText = findViewById(R.id.tagInputEditText);
        tagsChipGroup = findViewById(R.id.tagsChipGroup);
    }

    private void initFirebase() {
        analytics = FirebaseAnalytics.getInstance(this);
        db = FirebaseFirestore.getInstance();
        dataManager = JournalDataManager.getInstance(this);
    }

    private void initEntryState() {
        currentEntry = (JournalEntry) getIntent().getSerializableExtra("entry");
        if (currentEntry != null && currentEntry.getId() != null) {
            setEditMode(currentEntry);
        } else {
            String entryId = getIntent().getStringExtra("entry_id");
            if (entryId != null) {
                currentEntry = dataManager.getEntryById(entryId);
                if (currentEntry != null) {
                    setEditMode(currentEntry);
                } else {
                    setNewEntryMode();
                }
            } else {
                setNewEntryMode();
            }
        }
    }

    private void setEditMode(JournalEntry entry) {
        isEditMode = true;
        populateEntryData();
        pageTitle.setText("Edit Entry");
        deleteButton.setVisibility(View.VISIBLE);
        deleteButton.setOnClickListener(v -> {
            dataManager.deleteEntry(entry.getId(), success -> {
                Snackbar.make(findViewById(android.R.id.content),
                        success ? "Entry deleted" : "Failed to delete entry", Snackbar.LENGTH_SHORT).show();
                if (success) finish();
            });
        });
    }

    private void setNewEntryMode() {
        isEditMode = false;
        currentEntry = new JournalEntry();
        pageTitle.setText("New Entry");
        deleteButton.setVisibility(View.GONE);
    }

    private void loadTags() {
        dataManager.loadTagsFromFirestore(tags -> {
            previouslyUsedTags.addAll(tags);
            for (String tag : previouslyUsedTags) {
                addTagChip(tag, selectedTags.contains(tag));
            }
        });
    }

    private void setupTagInput() {
        tagInputEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {

                String newTag = tagInputEditText.getText().toString().trim();
                if (!newTag.isEmpty() && !selectedTags.contains(newTag)) {
                    selectedTags.add(newTag);
                    previouslyUsedTags.add(newTag);
                    addTagChip(newTag, true);
                    tagInputEditText.setText("");
                    dataManager.saveTagsToFirestore(previouslyUsedTags);
                    checkForChanges();
                }
                return true;
            }
            return false;
        });
    }

    private void setupGratitudeWatcher() {
        gratitudeInput.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { checkForChanges(); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }

    private void updateVisibilityForEditMode() {
        if (isEditMode) {
            saveButton.setVisibility(View.GONE);
            cancelButton.setVisibility(View.GONE);
        }
    }

    private void populateEntryData() {
        if (currentEntry.getMood() >= 0 && currentEntry.getMood() <= 2) {
            selectMood(currentEntry.getMood());
        }
        originalMood = currentEntry.getMood();

        originalNote = currentEntry.getNote();
        gratitudeInput.setText(originalNote);
        dateText.setText(currentEntry.getFormattedDate());

        if (currentEntry.getTags() != null) {
            selectedTags.addAll(currentEntry.getTags());
            originalTags.addAll(currentEntry.getTags());
        }

        String imagePath = currentEntry.getImagePath();
        if (JournalDataManager.isDemoImage(imagePath)) {
            if (imagePath.equals(JournalDataManager.DEMO_IMAGE_FAMILY)) entryImage.setImageResource(R.drawable.family_sunset);
            else if (imagePath.equals(JournalDataManager.DEMO_IMAGE_MEDITATION)) entryImage.setImageResource(R.drawable.meditation_sunrise);
            else if (imagePath.equals(JournalDataManager.DEMO_IMAGE_LIGHTBULB)) entryImage.setImageResource(R.drawable.lightbulb);
            entryImage.setVisibility(View.VISIBLE);
        } else if (imagePath != null && !imagePath.isEmpty()) {
            Glide.with(this).load(imagePath).into(entryImage);
            entryImage.setVisibility(View.VISIBLE);
            selectedImageUri = Uri.parse(imagePath); // Ensure it's not skipped on save
        } else {
            entryImage.setVisibility(View.GONE);
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
            else if (id == R.id.navigation_calendar) startActivity(new Intent(this, CalendarActivity.class));
            else return true;
            finish();
            return true;
        });
    }

    private void selectMood(int index) {
        TextView[] moodViews = {moodHappy, moodNeutral, moodSad};

        if (selectedMoodIndex == index) {
            moodViews[index].setBackground(null);
            moodViews[index].setTextColor(Color.BLACK);
            selectedMoodIndex = -1;
        } else {
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
        }
        checkForChanges();
    }

    private void checkForChanges() {
        boolean noteChanged = !originalNote.equals(gratitudeInput.getText().toString().trim());
        boolean moodChanged = (originalMood != selectedMoodIndex);
        boolean tagsChanged = !originalTags.equals(selectedTags);

        if (noteChanged || moodChanged || tagsChanged) {
            saveButton.setVisibility(View.VISIBLE);
            cancelButton.setVisibility(View.VISIBLE);
        } else {
            saveButton.setVisibility(View.GONE);
            cancelButton.setVisibility(View.GONE);
        }
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_CODE_IMAGE_PICK);
        } else {
            Snackbar.make(findViewById(android.R.id.content), "No app found to pick an image", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void setupButtons() {
        moodHappy.setOnClickListener(v -> selectMood(0));
        moodNeutral.setOnClickListener(v -> selectMood(1));
        moodSad.setOnClickListener(v -> selectMood(2));
        addPhotoButton.setOnClickListener(v -> requestImagePermissions());
        backButton.setOnClickListener(v -> onBackPressed());
        saveButton.setOnClickListener(v -> saveEntry());
        cancelButton.setOnClickListener(v -> finish());
    }

    private void requestImagePermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        REQUEST_CODE_PERMISSION);
            } else {
                launchImagePicker();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_CODE_PERMISSION);
            } else {
                launchImagePicker();
            }
        }
    }

    private void launchImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_CODE_IMAGE_PICK);
        } else {
            Snackbar.make(findViewById(android.R.id.content), "No app found to pick an image", Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchImagePicker();
        } else {
            Snackbar.make(findViewById(android.R.id.content), "Permission denied to read media", Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                entryImage.setImageURI(selectedImageUri);
                entryImage.setVisibility(View.VISIBLE);
                Snackbar.make(findViewById(android.R.id.content), "Image selected", Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(findViewById(android.R.id.content), "Failed to retrieve image", Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private void saveEntry() {
        String note = gratitudeInput.getText().toString().trim();
        if (selectedMoodIndex == -1) {
            Snackbar.make(findViewById(android.R.id.content), "Please select your mood", Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (note.isEmpty()) {
            Snackbar.make(findViewById(android.R.id.content), "Please write a gratitude note", Snackbar.LENGTH_SHORT).show();
            return;
        }

        currentEntry.setMood(selectedMoodIndex);
        currentEntry.setNote(note);
        currentEntry.setTags(new ArrayList<>(selectedTags));

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
                    Snackbar.make(findViewById(android.R.id.content), "Image upload failed", Snackbar.LENGTH_SHORT).show();
                    persistEntry();
                });
    }

    private void persistEntry() {
        if (isEditMode) {
            dataManager.updateEntry(currentEntry);
            Snackbar.make(findViewById(android.R.id.content), "Entry updated!", Snackbar.LENGTH_SHORT).show();
        } else {
            dataManager.addEntry(currentEntry);
            Snackbar.make(findViewById(android.R.id.content), "Entry saved!", Snackbar.LENGTH_SHORT).show();
        }
        finish();
    }

    private void addTagChip(String tag, boolean selected) {
        Chip chip = new Chip(this);
        chip.setText(tag);
        chip.setCheckable(true);
        chip.setChecked(selected);

        chip.setTextSize(14);
        chip.setChipStartPadding(12f);
        chip.setChipEndPadding(12f);
        chip.setChipBackgroundColorResource(R.color.white);
        chip.setChipStrokeWidth(1f);
        chip.setChipStrokeColorResource(R.color.light_gray);
        chip.setTextColor(ContextCompat.getColor(this, R.color.black));
        chip.setElevation(2f);

        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 0, 8, 0);
        chip.setLayoutParams(params);

        chip.setOnClickListener(v -> {
            if (chip.isChecked()) {
                selectedTags.add(tag);
            } else {
                selectedTags.remove(tag);
            }
            checkForChanges();
        });

        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            tagsChipGroup.removeView(chip);
            selectedTags.remove(tag);
            previouslyUsedTags.remove(tag);
            dataManager.saveTagsToFirestore(previouslyUsedTags);
            dataManager.deleteTagFromAllEntries(tag);
            checkForChanges();
        });

        tagsChipGroup.addView(chip);
    }
}
