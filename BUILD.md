# Building ResChange APK

## Requirements
- JDK 17+
- Android SDK with build-tools (API 35)
- Or: Android Studio (handles SDK automatically)

## One-command build (terminal)

```bash
# 1. Generate proper Gradle wrapper
gradle wrapper --gradle-version=8.7

# 2. Build debug APK
./gradlew assembleDebug

# APK will be at:
# app/build/outputs/apk/debug/app-debug.apk
```

## Android Studio
1. File → Open → select this folder
2. Let Gradle sync finish (downloads deps automatically)
3. Build → Build Bundle(s)/APK(s) → Build APK(s)
4. Click "locate" in the notification to find the APK

## SDK setup (if needed)
```bash
# Install via sdkmanager:
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

## Install to device
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Notes
- Requires Shizuku OR root on the device
- Shizuku must be running before opening the app for Shizuku mode
- Root mode works without Shizuku but boot-restore also works
- ADB fallback instructions are shown in-app for devices with neither
