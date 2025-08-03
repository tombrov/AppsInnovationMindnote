package com.example.mindnote;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private static final String PROFILE_IMAGE_PATH = "profile_pictures";

    private ImageView profileImageView;
    private BottomNavigationView bottomNavigationView;
    private TextView emailTextView, statsTextView, lastEntryTextView;
    private EditText displayNameEditText;
    private Button saveNameButton, logoutButton;
    private Switch notificationSwitch;
    private Uri imageUri;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private StorageReference storageRef;
    private JournalDataManager dataManager;

    private final ActivityResultLauncher<Intent> photoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    uploadProfileImage();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        user = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();
        dataManager = JournalDataManager.getInstance(this);

        profileImageView = findViewById(R.id.profileImageView);
        emailTextView = findViewById(R.id.emailTextView);
        displayNameEditText = findViewById(R.id.displayNameEditText);
        statsTextView = findViewById(R.id.statsTextView);
        lastEntryTextView = findViewById(R.id.lastEntryTextView);
        saveNameButton = findViewById(R.id.saveNameButton);
        logoutButton = findViewById(R.id.logoutButton);
        notificationSwitch = findViewById(R.id.notificationSwitch);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        emailTextView.setText(user != null ? user.getEmail() : "Not signed in");
        displayNameEditText.setText(user != null ? user.getDisplayName() : "");

        updateStats();

        saveNameButton.setOnClickListener(v -> saveDisplayName());
        logoutButton.setOnClickListener(v -> logout());
        profileImageView.setOnClickListener(v -> pickImage());

        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token ->
                notificationSwitch.setChecked(true));

        bottomNavigationView.setSelectedItemId(R.id.navigation_profile);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_profile) return true;
            if (id == R.id.navigation_calendar) {
                startActivity(new Intent(this, CalendarActivity.class));
                return true;
            }
            if (id == R.id.navigation_notes) {
                startActivity(new Intent(this, NotesActivity.class));
                return true;
            }
            if (id == R.id.navigation_home) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            }
            if (id == R.id.navigation_journal) {
                startActivity(new Intent(this, JournalActivity.class));
                return true;
            }
            return false;
        });
    }

    private void updateStats() {
        int entryCount = dataManager.getEntryCount();
        int streak = dataManager.calculateStreak();
        Date lastEntry = dataManager.getLastEntryDate();

        statsTextView.setText(entryCount + " entries | " + streak + " day streak");

        if (lastEntry != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            lastEntryTextView.setText("Last entry: " + sdf.format(lastEntry));
        } else {
            lastEntryTextView.setText("Last entry: N/A");
        }
    }

    private void saveDisplayName() {
        String name = displayNameEditText.getText().toString().trim();
        if (name.isEmpty() || user == null) return;

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();

        user.updateProfile(profileUpdates)
                .addOnSuccessListener(unused -> Toast.makeText(this, "Name updated", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show());
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void pickImage() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        photoPickerLauncher.launch(pickIntent);
    }

    private void uploadProfileImage() {
        if (imageUri == null || user == null) return;

        StorageReference ref = storageRef.child(PROFILE_IMAGE_PATH + "/" + user.getUid() + ".jpg");
        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    Glide.with(this).load(uri).into(profileImageView);
                    Toast.makeText(this, "Profile picture updated", Toast.LENGTH_SHORT).show();
                }))
                .addOnFailureListener(e -> Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show());
    }
}
