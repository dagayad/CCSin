# SinCC (Sinhala & English AI Caption Studio)

AI-Powered Sinhala & English Video Caption Studio for TikTok, YouTube Shorts, and Instagram Reels with support for:
- ⚡ **Auto-Transcription**: Sinhala & English speech detection with frame-accurate timing.
- 🎨 **Viral Preset Styles**: MrBeast, Alex Hormozi, Neon Glow, Karaoke Bouncing, Cinematic Minimalist, and 3D Bold.
- 🔤 **Dual Sinhala Font Rendering**:
  - Native **Sinhala Unicode** font support.
  - Automatic detection and conversion for **FM / DL Legacy TrueType fonts** (Tharu Digital, FM Abhaya, DL-Araliya, etc.) without corrupting English text.
- 🔠 **Font Styling**: Bold, Italic, Underline, Uppercase, Tracking, Glow & Shadow.
- 🎬 **1080p Full HD Video Export**: Real-time canvas overlay rendering.
- 📱 **Adaptive UI**: Pinned live caption preview window with smooth scrolling controls.

---

## 🚀 GitHub Actions Auto-Build APK Workflow

This repository includes an automated GitHub Actions workflow (`.github/workflows/android-apk.yml`) that automatically compiles and packages the Android `.apk` whenever you push code or trigger a build.

> **Important – updating the app on your phone:** Android only installs an update if it is signed with the same key as the installed copy. Add a repo secret named `DEBUG_KEYSTORE_BASE64` (see the workflow comments) so every CI build uses the same signing key. If you change the key, uninstall the old app first.
>
> **Important – AI features:** The AI features (captions, style matcher, translation) call Firebase AI Logic, which requires Firebase App Check. Register each phone's App Check debug token in the Firebase console, or the AI calls will be rejected.

### How to get the APK:

1. **Automatic Build on Push:**
   - Whenever you push to the `main` or `master` branch, GitHub Actions automatically starts building the APK.
2. **Download from Actions:**
   - Go to your repository on GitHub.
   - Click on the **Actions** tab at the top.
   - Click on the latest run under **Android APK Build**.
   - Scroll down to the **Artifacts** section at the bottom.
   - Click on **SinCc-debug-apk** to download the zip file containing your ready-to-install `.apk`.
3. **Manual Trigger (Run Workflow):**
   - In the **Actions** tab, select **Android APK Build** from the left sidebar.
   - Click **Run workflow**, choose your branch, and click the green **Run workflow** button.
4. **Automatic GitHub Releases:**
   - If you create and push a git tag (for example `git tag v1.0.0 && git push origin v1.0.0`), GitHub Actions will automatically create a **GitHub Release** and attach the `.apk` directly to the release page.

---

## 🛠️ Local Build Instructions

```bash
# Clone the repository
git clone <your-repo-url>
cd <repo-folder>

# Make gradlew executable
chmod +x gradlew

# Build Debug APK locally
./gradlew assembleDebug

# Output APK path:
# app/build/outputs/apk/debug/app-debug.apk
```
