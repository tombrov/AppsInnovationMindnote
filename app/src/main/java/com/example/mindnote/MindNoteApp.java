package com.example.mindnote;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class MindNoteApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
    }
}
