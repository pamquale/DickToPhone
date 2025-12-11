# DickToPhone 🎙️

**DickToPhone** is a feature-rich Android voice recorder application built with **Java**. 
It demonstrates the correct implementation of background audio recording using **Foreground Services** and secure file management via **Storage Access Framework (SAF)**, compliant with modern Android standards (Android 8.0 - Android 14).

## ✨ Features

### Core Functionality
* **Background Recording:** Continues recording even when the app is minimized or the screen is off (using Foreground Service).
* **Persistent Notification:** displays recording status and allows stopping the recording from the notification shade.
* **Scoped Storage (SAF):** Users can choose **any folder** on the device (including SD cards) to save recordings. The permission is persisted across reboots.
* **Timer & Duration Limit:** Visual timer during recording and auto-stop feature after a configured duration.

### 🚀 Advanced Features (Bonus Tasks)
* **📁 Playlist & Explorer:** Built-in "My Recordings" screen (`RecyclerView`) to view and manage recorded files.
* **▶️ Playback:** Listen to recordings directly within the app.
* **📤 Share:** Long-press any recording to share it via messengers, email, etc.
* **🎚️ Audio Quality:** Configurable settings for **High** (128kbps, 44kHz) or **Low** (64kbps, 22kHz) quality.
* **💾 Storage Check:** Prevents recording start if free space is less than 5MB.
* **🔄 Loop Recording:** Option to automatically start a new recording file when the timer limit is reached.

## 📱 Screenshots

| Main Screen | Recording | Settings | My Recordings |
|:-----------:|:---------:|:--------:|:-------------:|
| <img src="screenshots/main.png" width="200"/> | <img src="screenshots/recording.png" width="200"/> | <img src="screenshots/settings.png" width="200"/> | <img src="screenshots/list.png" width="200"/> |

*(Note: Add your screenshots to a `screenshots` folder in your repo to make them visible)*

## 🛠️ Tech Stack

* **Language:** Java 17
* **Min SDK:** 26 (Android 8.0)
* **Target SDK:** 34 (Android 14)
* **Architecture:** MVC
* **Key Components:**
    * `MediaRecorder` (AAC/M4A format)
    * `Foreground Service`
    * `Storage Access Framework` (SAF)
    * `RecyclerView`
    * `AndroidX Preference`

## ⚙️ Setup & Installation

1.  Clone the repository:
    ```bash
    git clone [https://github.com/YOUR_USERNAME/DickToPhone.git](https://github.com/YOUR_USERNAME/DickToPhone.git)
    ```
2.  Open the project in **Android Studio**.
3.  Sync project with Gradle files.
4.  Run on an emulator or physical device (Android 8.0+ required).

## 📖 Usage

1.  **Grant Permissions:** On the first launch, allow Microphone and Notification access.
2.  **Select Folder (Optional):** Go to *Settings* -> *Save Folder* to choose where files will be stored. If not selected, files are saved in the app's internal private storage.
3.  **Record:** Press **Start**. You can minimize the app; the recording will continue.
4.  **Stop:** Press **Stop** in the app or the **Stop** button in the notification.
5.  **Listen:** Click "My Recordings" to view and play your files.

## 📄 License

This project is created for educational purposes as part of the "Mobile Platform Programming" course.

---
**Developed by:** [Your Name]
