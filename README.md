# B1gBr0ther - Activity Tracking Application

B1gBr0ther is an Android application designed to track user activities through various methods including gesture recognition, GPS location tracking, and voice commands. This project was developed as part of a school assignment.

## Project Setup Instructions

### Prerequisites

- Android Studio (2023.3.1 or newer recommended)
- JDK 11 or higher
- Android SDK with API level 34 (Android 14)
- A physical Android device or emulator with API level 34+
- Git (optional, for version control)

## Installation Instructions

### Option 1: Direct APK Installation (Recommended)

1. Download the APK file from the latest release
2. Enable "Install from Unknown Sources" in your Android phone's settings if not already enabled
3. Use your phone's package installer to install the APK
4. Launch the B1gBr0ther app from your app drawer

### Option 2: Running from Android Studio

1. **Clone or download the project**
   ```
   git clone https://github.com/HakanEgeOzerol/B1gBr0ther.git
   ```
   Or download and extract the ZIP file from the repository.

2. **Open the project in Android Studio**
   - Launch Android Studio
   - Select "Open an existing Android Studio project"
   - Navigate to the B1gBr0ther directory and click "Open"

3. **Sync Gradle**
   - Wait for the initial Gradle sync to complete
   - If prompted to update Gradle, follow the instructions

### Fixing the Gradle Wrapper

If you see errors like `Could not find or load main class org.gradle.wrapper.GradleWrapperMain` or Gradle version errors, follow these steps:

1. Make sure you have these files in your project root:
    - `gradlew` (shell script)
    - `gradlew.bat` (Windows batch file)
    - `gradle/wrapper/gradle-wrapper.jar`
    - `gradle/wrapper/gradle-wrapper.properties`
2. If `gradle-wrapper.jar` is missing, download it from:
    - [https://repo1.maven.org/maven2/org/gradle/gradle-wrapper/8.11.1/gradle-wrapper-8.11.1.jar](https://repo1.maven.org/maven2/org/gradle/gradle-wrapper/8.11.1/gradle-wrapper-8.11.1.jar)
    - Place it in `gradle/wrapper/` and rename to `gradle-wrapper.jar` if needed.
3. Edit `gradle/wrapper/gradle-wrapper.properties` and set:
    - `distributionUrl=https\://services.gradle.org/distributions/gradle-8.11.1-all.zip`
4. Run `./gradlew --version` (or `gradlew.bat --version` on Windows) to verify Gradle 8.11.1 is used.

### Building and Running the App

1. **Connect an Android device**
   - Enable Developer options and USB debugging on your device
   - Connect your device to your computer via USB
   - Alternatively, set up an Android Virtual Device (AVD) in Android Studio

2. **Build and run the app**
   - Click the "Run" button (green triangle) in Android Studio
   - Select your device from the list and click "OK"
   - Wait for the app to build and install on your device

3. **Verify installation**
   - The app should launch automatically on your device
   - You should see the main screen with navigation options

## Features

- **Activity Tracking**: Track time spent on different tasks
- **Gesture Recognition**: Detect user activities through device motion
- **Voice Commands**: Control the app using voice recognition
- **Dashboard**: Visualize activity history and statistics
- **Task Management**: Add, edit, and categorize tasks
- **Data Export**: Export activity data in various formats (CSV, JSON, HTML, etc.)
- **Local Database**: Store all tracking data locally on the device

## Troubleshooting

- **Gradle Build Issues**
  - If you see errors about missing files or classes, double-check the Gradle wrapper files and version
  - Try "File > Invalidate Caches / Restart" in Android Studio

- **Voice Recognition Issues**
  - Ensure the Vosk model is properly downloaded (see above)
  - Check that microphone permissions are granted to the app
  - Verify that the device has a working microphone

- **Database Issues**
  - If you encounter database errors, try uninstalling and reinstalling the app
  - The app uses Room database which should handle migrations automatically

- **Sensor Issues**
  - Some features require specific sensors (accelerometer, gyroscope)
  - Verify your device has the necessary sensors for full functionality(microphone, gyroscope, accelerometer).

## Project Structure

- `app/src/main/java/com/b1gbr0ther/` - Main source code
- `app/src/main/res/` - Resources (layouts, strings, etc.)
- `app/src/main/assets/` - Asset files