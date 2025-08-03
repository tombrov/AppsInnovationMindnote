# 🧠 Mindnote - Daily Gratitude & Journaling App

Mindnote is a mobile journaling application built in **Java using Android Studio**, designed to help users track their daily gratitude, mood, and reflections. The app emphasizes mental wellness, journaling consistency, and smooth user experience through Firebase-powered features.

---

## 🌟 Project Overview

This app was developed as part of the **"Advanced Topics in App Innovations"** course. Our final project focused on connecting the app to the internet using **Firebase**, applying modern Android development techniques, and showcasing real mobile capabilities like image uploads and persistent authentication.

The final version of the app includes:
- 📅 Daily journaling with date-based navigation
- 😊 Mood selection with emoji icons
- 🏷️ Tagging system with tag reuse and management
- 🖼️ Image upload from gallery with Firebase Storage
- 🔥 Streak counter with dynamic emoji logic
- 📊 Stats page showing user insights
- 🔐 Firebase Authentication (Google Sign-in)
- 📡 Cloud Firestore for dynamic data
- 🧪 Firebase Analytics and Crashlytics for insights and stability

---

## 🔧 Tech Stack

- **Java** (Android SDK)
- **XML Layouts** (Material Design & ConstraintLayout)
- **Firebase** (Authentication, Firestore, Storage, Analytics, Crashlytics, FCM)
- **Glide** for efficient image loading and caching
- **MVVM-inspired structure** using singleton data manager

---

## 🔐 Firebase Integration

| Feature        | Implementation                                                                                           |
|----------------|-----------------------------------------------------------------------------------------------------------|
| Analytics      | Logged user events such as creating entries, editing, deleting, and navigating the calendar.              |
| Crashlytics    | Integrated to capture runtime exceptions and report errors in real-time.                                   |
| Firestore      | All journal entries, images, tags, and profile data are dynamically stored and loaded via Firestore.      |
| Authentication | Implemented Google Sign-In for persistent user sessions.                                                  |
| Storage        | Uploaded journal images are saved in Firebase Storage and referenced in Firestore to be displayed later.  |

---

## 📱 Core Features

### ✍️ Journal Entry
Users can:
- Write thoughts for the day
- Add an image from their gallery
- Choose their mood (😊😐😞)
- Add and reuse tags
- Save entry to Firestore with timestamp

### 🔥 Streak Tracking
- Tracks consecutive daily entries.
- Displays a 🔥 emoji after reaching a 3-day streak.
- Automatically updates based on entry dates.

### 🧠 Stats & Profile
- View total entries, last activity, and current streak.
- Editable display name and profile picture.
- Profile photo is stored in Firebase Storage.

### 🗓️ Calendar Navigation
- Allows browsing and editing past entries.
- Highlights days with entries using custom UI.
- Ensures smooth interaction with Firestore.

### 📝 Notes List
- Scrollable list of journal entries sorted by date.
- Includes empty state with custom vector illustration and hints.
- Clicking a note opens it in a detail view for editing.

---

## 📸 Phone Capabilities Used

- **Gallery Access**: User selects images from device to upload and display in journal.
- **Persistent Login**: Keeps the user authenticated between app sessions using Firebase Auth.
- **Notifications Support (Firebase Messaging)**: Ready to handle push notifications in future iterations.

---

## ➕ Third-Party Library Used

### Glide 🖼️
- Glide was used for efficient image loading and caching.
- Automatically handles image scaling and memory usage.
- Used in journal entries and profile image loading.

---

## 🚧 Challenges & Solutions

### 🔄 Persistent Auth & UI State
FirebaseAuth state wasn't properly updating across sessions. We added listener-based logic to detect login status and update UI accordingly.

### 🖼️ Image Upload Errors
We fixed issues around URI parsing and Firebase Storage path naming by using a unique timestamped filename pattern and robust URI handling.

### 🔁 Tag Persistence
Tags were not saved globally. We added Firestore syncing logic to load and update tags with `Set<String>` and ensured chip group updates.

### 🧪 Crash Fixes
We handled null edge cases using defensive programming and used Crashlytics logs to trace hard-to-find issues during profile editing and note loading.

---

## 📁 Folder Structure (Summary)

Mindnote/
├── activities/
│ ├── MainActivity.java
│ ├── JournalActivity.java
│ ├── NotesActivity.java
│ ├── ProfileActivity.java
│ └── EntryDetailActivity.java
│ └── NotesAdapter.java
│ └── JournalDataManager.java
├── res/
│ ├── layout/
│ ├── drawable/
│ ├── values/


---

## 🧪 Quality Assurance (QA)

We tested the app using both manual and real-device testing strategies:

- ✅ **Smoke Testing** after major feature additions
- ✅ **Regression Testing** on profile, entries, and tag reuse logic
- ✅ **Sanity Testing** after authentication changes
- ✅ **Exploratory Testing** to identify edge-case bugs (e.g., deleting the last tag, switching profile images rapidly)
- ✅ **Crashlytics Logs** for production error tracking

---

🎓 *Developed for: Advanced Topics in App Innovations*  
👨‍🏫 Instructor: Mr. Uri Dvir  
📍 University Course Project (2025)
