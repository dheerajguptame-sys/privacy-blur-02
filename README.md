# Privacy Blur - Android Application Source Code

Package Name: `com.privacyblur.app`
Target: Android 15 (API 35), Min SDK: Android 8.0 (API 26)

## How to Build the APK (.apk)

### Option A: Using Android Studio (Recommended)
1. Open Android Studio (Ladybug 2024.2+ or Hedgehog/Iguana).
2. Click **File > Open** and choose this extracted directory.
3. Allow Gradle to sync automatically (requires JDK 17 or JDK 21).
4. In the top menu bar, click:
   **Build > Build Bundle(s) / APK(s) > Build APK(s)**
5. Android Studio will build the APK and show a pop-up:
   "APK(s) generated successfully for 1 module: Locate"
6. Click **Locate**. You will find:
   `app/build/outputs/apk/debug/app-debug.apk`

### Option B: Using Command Line / Terminal
- On Windows:
  `gradlew.bat assembleDebug`
- On Mac / Linux:
  `./gradlew assembleDebug`

The generated APK will be at:
`app/build/outputs/apk/debug/app-debug.apk`

### Option C: GitHub Actions Free Cloud Build
Push this code to any GitHub repository. The workflow `.github/workflows/build-apk.yml` will build the APK automatically on GitHub's cloud runners in ~2 minutes!
Download the APK from GitHub Actions > Artifacts.
