# Android Project Setup

## Prerequisites

| Tool | Version | Download |
|------|---------|---------|
| Android Studio | Hedgehog 2023.1.1+ | https://developer.android.com/studio |
| JDK | 17 | Bundled with Android Studio |
| Android SDK | API 35 | Install via SDK Manager in Studio |

---

## Step 1 — Copy files into your existing folder structure

The files in this zip go into `pocket-sarkar/android/`.
Your existing folder structure already has all the subdirectories — just copy the files in.

```
pocket-sarkar/
├── android/               ← copy everything here
│   ├── settings.gradle.kts
│   ├── build.gradle.kts
│   ├── gradle.properties
│   ├── gradle/
│   │   ├── libs.versions.toml
│   │   └── wrapper/
│   │       └── gradle-wrapper.properties
│   └── app/
│       ├── build.gradle.kts
│       ├── proguard-rules.pro
│       └── src/main/
│           ├── AndroidManifest.xml
│           ├── kotlin/com/pocketsarkar/   ← all .kt files
│           └── res/                       ← all resource files
```

---

## Step 2 — Open in Android Studio

1. Open Android Studio
2. **File → Open** → navigate to `pocket-sarkar/android/`
3. Click **OK** — Studio will detect `settings.gradle.kts` and set up the project
4. Wait for Gradle sync to finish (first time: 3–5 min, downloads dependencies)

If Gradle sync fails → see Troubleshooting below.

---

## Step 3 — Verify the build

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

## Step 4 — Connect a real device (recommended)

The Gemma 4 E4B model requires a real Android device:
- Minimum: **3GB RAM**, **8GB free storage**
- Enable **USB Debugging**: Settings → Developer Options → USB Debugging

If you only have an emulator for now, you can still test UI — just skip the AI queries until you have the model.

---

## Step 5 — Download the model (when ready for Phase 3)

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
→ Check internet connection. This downloads from Google's Maven repo.
→ If on VPN, try disabling it.

**"SDK location not found"**
→ Create `android/local.properties` with:
```
sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
```
(Android Studio usually creates this automatically on open)

**Build error: "Unresolved reference: DocumentScanner"**
→ The `Icons.Default.DocumentScanner` icon requires `compose-material-icons-extended`.
→ Check `libs.versions.toml` has `compose-material-icons` dependency.
→ Run **File → Invalidate Caches → Restart**

**KSP/Hilt annotation processing errors**
→ Make sure you're using KSP, not kapt, for Room and Hilt (already configured in build.gradle.kts)
→ Clean and rebuild: `.\gradlew.bat clean assembleDebug`

**MediaPipe version not found**
→ Check https://developers.google.com/mediapipe/solutions/genai/llm_inference/android for the latest version
→ Update `mediapipeTasks` in `gradle/libs.versions.toml`
