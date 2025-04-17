# APK Installation Guide

This guide provides instructions on how to install the Face Metrics APK on your Android device.

## Prerequisites

- Android device with Android 5.0 (Lollipop) or higher
- USB cable to connect your device to your computer (optional)
- "Unknown sources" option enabled in your Android device's security settings

## Installation Methods

### Method 1: Direct Download on Device

1. Download the APK file to your Android device
2. Tap on the downloaded file in your device's notification panel or file manager
3. Tap "Install" when prompted
4. Tap "Open" after installation completes

### Method 2: Using USB Connection

1. Connect your Android device to your computer via USB cable
2. Enable file transfer mode on your Android device
3. Copy the APK file from your computer to your Android device's storage
4. Disconnect the device and use a file manager app to locate and tap the APK
5. Tap "Install" when prompted
6. Tap "Open" after installation completes

### Method 3: Using ADB (Advanced)

1. Connect your Android device to your computer via USB cable
2. Enable USB debugging in your device's Developer Options
3. Open command prompt (Windows) or terminal (Mac/Linux) on your computer
4. Navigate to the directory containing the APK file
5. Run the command: `adb install app-debug.apk`
6. Wait for "Success" message indicating the app has been installed

## Verifying Installation

After installation, you can find the Face Metrics app in your app drawer. The app icon will appear with the app name "Face Metrics".

## Troubleshooting

If you encounter installation issues:

1. **"App not installed" error:**
   - Check if you have enough storage space
   - Uninstall any previous versions of the app
   - Restart your device and try again

2. **"Blocked by Play Protect" warning:**
   - Tap "Install Anyway" to proceed
   - This warning appears because the app is not from the Google Play Store

3. **"Unknown sources" setting:**
   - Go to Settings > Security > Unknown sources (or Settings > Apps & notifications > Special app access > Install unknown apps)
   - Enable the option for your file manager or browser

4. **ADB connection issues:**
   - Ensure USB debugging is enabled
   - Try different USB cables or ports
   - Install/reinstall ADB drivers on your computer