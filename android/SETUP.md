# Android Project Setup

## Prerequisites

| Tool | Version | Download |
|------|---------|---------|
| Android Studio | Koala 2024.1.1+ | https://developer.android.com/studio |
| JDK | 17 | Bundled with Android Studio |
| Android SDK | API 35 | Install via SDK Manager in Studio |

---

## Step 1 â€” Copy files into your existing folder structure

The files in this zip go into `pocket-sarkar/android/`.
Your existing folder structure already has all the subdirectories â€” just copy the files in.

```
pocket-sarkar/
â”œâ”€â”€ android/               â† copy everything here
â”‚   â”œâ”€â”€ settings.gradle.kts
â”‚   â”œâ”€â”€ build.gradle.kts
â”‚   â”œâ”€â”€ gradle.properties
â”‚   â”œâ”€â”€ gradle/
â”‚   â”‚   â”œâ”€â”€ libs.versions.toml
â”‚   â”‚   â””â”€â”€ wrapper/
â”‚   â”‚       â””â”€â”€ gradle-wrapper.properties
â”‚   â””â”€â”€ app/
â”‚       â”œâ”€â”€ build.gradle.kts
â”‚       â”œâ”€â”€ proguard-rules.pro
â”‚       â””â”€â”€ src/main/
â”‚           â”œâ”€â”€ AndroidManifest.xml
â”‚           â”œâ”€â”€ kotlin/com/pocketsarkar/   â† all .kt files
â”‚           â””â”€â”€ res/                       â† all resource files
```

---

## Step 2 â€” Open in Android Studio

1. Open Android Studio
2. **File â†’ Open** â†’ navigate to `pocket-sarkar/android/`
3. Click **OK** â€” Studio will detect `settings.gradle.kts` and set up the project
4. Wait for Gradle sync to finish (first time: 3â€“5 min, downloads dependencies)

If Gradle sync fails â†’ see Troubleshooting below.

---

## Step 3 â€” Verify the build

In Android Studio terminal (or PowerShell in `pocket-sarkar/android/`):

```powershell
.\gradlew.bat assembleDebug
```

Expected output:
```
BUILD SUCCESSFUL in ~2min
pocket-sarkar\android\app\build\outputs\apk\debug\app-debug.apk
```

---

## Step 4 â€” Connect a real device (recommended)

The Gemma 4 E4B model requires a real Android device:
- Minimum: **3GB RAM**, **8GB free storage**
- Enable **USB Debugging**: Settings â†’ Developer Options â†’ USB Debugging

If you only have an emulator for now, you can still test UI â€” just skip the AI queries until you have the model.

---

## Step 5 â€” Download the model (when ready for Phase 3)

```powershell
cd pocket-sarkar
python scripts/download_model/download_model.py --model e4b-int4 --output android/app/src/main/assets/models/
```

The model file (`gemma4-e4b-it-int4.task`, ~1.6GB) goes in `assets/models/` and is gitignored.

---

## What you should see on first run

- App launches with the Pocket Sarkar home screen
- 4 module cards visible (Document Decoder, Scheme Explainer, Opportunity Radar, Rights Companion)
- Tapping any card shows a placeholder "Coming in Phase X" screen
- No crashes

---

## Troubleshooting

**Gradle sync fails with "Could not resolve com.google.mediapipe:tasks-genai"**
â†’ Check internet connection. This downloads from Google's Maven repo.
â†’ If on VPN, try disabling it.

**"SDK location not found"**
â†’ Create `android/local.properties` with:
```
sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
```
(Android Studio usually creates this automatically on open)

**Build error: "Unresolved reference: DocumentScanner"**
â†’ The `Icons.Default.DocumentScanner` icon requires `compose-material-icons-extended`.
â†’ Check `libs.versions.toml` has `compose-material-icons` dependency.
â†’ Run **File â†’ Invalidate Caches â†’ Restart**

**KSP/Hilt annotation processing errors**
â†’ Make sure you're using KSP, not kapt, for Room and Hilt (already configured in build.gradle.kts)
â†’ Clean and rebuild: `.\gradlew.bat clean assembleDebug`

**MediaPipe version not found**
â†’ Check https://developers.google.com/mediapipe/solutions/genai/llm_inference/android for the latest version
â†’ Update `mediapipeTasks` in `gradle/libs.versions.toml`

