package com.example.mindnote;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JournalActivity extends AppCompatActivity {

    private EditText titleInput, contentInput, tagInput;
    private ImageView previewImage;
    private TextView moodHappy, moodNeutral, moodSad;
    private ChipGroup tagChipGroup;
    private Button pickImageButton, captureImageButton, removeImageButton, saveEntryButton;
    private BottomNavigationView bottomNavigation;

    private Uri imageUri;
    private FirebaseUser user;
    private String editingEntryId = null;

    private int selectedMood = 0;
    private final Set<String> tags = new HashSet<>();

    private final ActivityResultLauncher<PickVisualMediaRequest> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    imageUri = uri;
                    Glide.with(this).load(uri).into(previewImage);
                    previewImage.setVisibility(ImageView.VISIBLE);
                    removeImageButton.setVisibility(Button.VISIBLE);
                    pickImageButton.setVisibility(Button.GONE);
                    captureImageButton.setVisibility(Button.GONE);
                } else {
                    Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                    if (photo != null) {
                        previewImage.setImageBitmap(photo);
                        imageUri = getImageUri(photo);
                        previewImage.setVisibility(ImageView.VISIBLE);
                        removeImageButton.setVisibility(Button.VISIBLE);
                        pickImageButton.setVisibility(Button.GONE);
                        captureImageButton.setVisibility(Button.GONE);
                    } else {
                        Toast.makeText(this, "Failed to capture photo", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journal);

        titleInput = findViewById(R.id.titleInput);
        contentInput = findViewById(R.id.contentInput);
        previewImage = findViewById(R.id.previewImage);
        pickImageButton = findViewById(R.id.pickImageButton);
        captureImageButton = findViewById(R.id.captureImageButton);
        removeImageButton = findViewById(R.id.removeImageButton);
        saveEntryButton = findViewById(R.id.saveEntryButton);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        tagInput = findViewById(R.id.tagInput);
        tagChipGroup = findViewById(R.id.tagChipGroup);
        moodHappy = findViewById(R.id.moodHappy);
        moodNeutral = findViewById(R.id.moodNeutral);
        moodSad = findViewById(R.id.moodSad);

        user = FirebaseAuth.getInstance().getCurrentUser();

        pickImageButton.setOnClickListener(v -> launchGallery());
        captureImageButton.setOnClickListener(v -> launchCamera());
        removeImageButton.setOnClickListener(v -> removeImage());
        saveEntryButton.setOnClickListener(v -> saveEntry());

        setupMoodSelection();
        setupTagInput();

        // Check if editing an existing entry
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("entryId")) {
            editingEntryId = intent.getStringExtra("entryId");
            JournalEntry existingEntry = JournalDataManager.getInstance(this).getEntryById(editingEntryId);
            if (existingEntry != null) {
                populateFieldsForEditing(existingEntry);
            }
        }

        bottomNavigation.setSelectedItemId(R.id.navigation_journal);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                startActivity(new Intent(this, MainActivity.class));
            } else if (id == R.id.navigation_notes) {
                startActivity(new Intent(this, NotesActivity.class));
            } else if (id == R.id.navigation_journal) {
                return true;
            } else if (id == R.id.navigation_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
            } else if (id == R.id.navigation_calendar) {
                startActivity(new Intent(this, CalendarActivity.class));
            } else {
                return false;
            }
            overridePendingTransition(0, 0);
            return true;
        });
    }

    private void populateFieldsForEditing(JournalEntry entry) {
        titleInput.setText(entry.getTitle());
        contentInput.setText(entry.getNote());
        selectMood(entry.getMood());

        tags.clear();
        tagChipGroup.removeAllViews();
        if (entry.getTags() != null) {
            for (String tag : entry.getTags()) {
                tags.add(tag);
                Chip chip = new Chip(this);
                chip.setText(tag);
                chip.setCloseIconVisible(true);
                chip.setOnCloseIconClickListener(view -> {
                    tagChipGroup.removeView(chip);
                    tags.remove(tag);
                });
                tagChipGroup.addView(chip);
            }
        }

        if (entry.getImagePath() != null) {
            imageUri = Uri.parse(entry.getImagePath());
            Glide.with(this).load(imageUri).into(previewImage);
            previewImage.setVisibility(ImageView.VISIBLE);
            removeImageButton.setVisibility(Button.VISIBLE);
            pickImageButton.setVisibility(Button.GONE);
            captureImageButton.setVisibility(Button.GONE);
        }
    }

    private void setupMoodSelection() {
        moodHappy.setOnClickListener(v -> selectMood(0));
        moodNeutral.setOnClickListener(v -> selectMood(1));
        moodSad.setOnClickListener(v -> selectMood(2));
        selectMood(0); // default
    }

    private void selectMood(int mood) {
        selectedMood = mood;
        moodHappy.setAlpha(mood == 0 ? 1f : 0.3f);
        moodNeutral.setAlpha(mood == 1 ? 1f : 0.3f);
        moodSad.setAlpha(mood == 2 ? 1f : 0.3f);
    }

    private void setupTagInput() {
        tagInput.setOnEditorActionListener((v, actionId, event) -> {
            if ((event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) ||
                    actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                String tag = tagInput.getText().toString().trim();
                if (!tag.isEmpty() && !tags.contains(tag)) {
                    tags.add(tag);
                    Chip chip = new Chip(this);
                    chip.setText(tag);
                    chip.setCloseIconVisible(true);
                    chip.setOnCloseIconClickListener(view -> {
                        tagChipGroup.removeView(chip);
                        tags.remove(tag);
                    });
                    tagChipGroup.addView(chip);
                    tagInput.setText("");
                }
                return true;
            }
            return false;
        });
    }

    private void removeImage() {
        imageUri = null;
        previewImage.setImageDrawable(null);
        previewImage.setVisibility(ImageView.GONE);
        removeImageButton.setVisibility(Button.GONE);
        pickImageButton.setVisibility(Button.VISIBLE);
        captureImageButton.setVisibility(Button.VISIBLE);
    }

    private void launchGallery() {
        galleryLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void launchCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(intent);
    }

    private Uri getImageUri(Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "CapturedImage", null);
        return Uri.parse(path);
    }

    private void saveEntry() {
        String title = titleInput.getText().toString().trim();
        String content = contentInput.getText().toString().trim();

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "Please fill in both title and content", Toast.LENGTH_SHORT).show();
            return;
        }

        if (user == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        JournalEntry entry;
        if (editingEntryId != null) {
            entry = JournalDataManager.getInstance(this).getEntryById(editingEntryId);
            if (entry == null) {
                entry = new JournalEntry();
            }
        } else {
            entry = new JournalEntry();
        }

        entry.setTitle(title);
        entry.setNote(content);
        entry.setMood(selectedMood);
        entry.setTags(new ArrayList<>(tags));
        entry.setDate(new Date());
        entry.setImagePath(imageUri != null ? imageUri.toString() : null);

        JournalDataManager.getInstance(this).saveEntry(entry);

        Toast.makeText(this, "Entry saved", Toast.LENGTH_SHORT).show();
        finish();
    }
}
