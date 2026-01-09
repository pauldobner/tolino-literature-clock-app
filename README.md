# Native Literature Clock for Tolino (Android App)

A battery-optimized native Android application for Tolino eReaders (specifically Vision 2 / Shine 2 HD and similar running Android 4.4 KitKat). 

Unlike the web-based version, this app utilizes **Root Access** to force the device into a deep "Suspend-to-RAM" sleep state between minutes. This significantly reduces power consumption compared to keeping the CPU awake.

## Features
- **Deep Sleep (Root Suspend):** Wakes up for ~7 seconds every minute to update the screen, then sleeps for the remainder of the minute.
- **Battery Efficient:** Disables Wi-Fi and handles system wake locks during sleep.
- **Offline:** No internet connection required.
- **E-Ink Optimized:** High contrast, large text, minimal refreshes.
- **Battery Logging:** Tracks battery usage over time to help you monitor performance.
- **Multilingual:** Supports English and German quote databases.

## Prerequisites: Root Access

This app relies on **Root Access** to control the system power state (`/sys/power/state`).

**Rooting Guide:**
I used the following tutorial (German) to root the Tolino and enable ADB. It uses a custom boot image and **Magisk** for root management:
[Tolino Root & ADB Installation Guide - ALLESebook.de](https://allesebook.de/anleitung/tolino-page-1-shine-2-hd-vision-1-bis-4-hd-epos-1-root-adb-und-apps-installieren-anleitung-983082/)

**Summary of setup:**
1.  **Unlock & Root:** Follow the tutorial to flash the modified `boot.img`.
2.  **Magisk:** Ensure Magisk is installed and functioning as the Superuser manager.
3.  **ADB:** Verify you have ADB access to the device.

## Installation

1.  **Build the APK** (or download a release):
    ```bash
    ./gradlew assembleDebug
    ```
2.  **Install via ADB**:
    Connect your Tolino via USB and run:
    ```bash
    adb install -r app/build/outputs/apk/debug/app-debug.apk
    ```
3.  **Grant Root Permissions**:
    Open the app on the Tolino. **Magisk** will prompt for root access. Grant it **Permanently**.

## Configuration

### 1. Disable Toast Notifications
For a seamless experience, you must disable the notification that Magisk/Superuser displays every time root access is granted (which happens every minute).

1.  Open the **Magisk** app on your Tolino.
2.  Go to **Settings**.
3.  Find **"Superuser Notification"** (or similar "Toast" setting).
4.  Set it to **"None"** or disable it.

### 2. Autostart (Kiosk Mode)
To make this a true dedicated clock, the app should start automatically when the device boots.

The rooting tutorial includes the **Simple Ink Launcher** (`z_simple.ink.launcher.1.2.apk`).
1.  Set **Simple Ink Launcher** as your default home screen.
2.  In the launcher settings or app list, configure it to launch **Literature Clock** on startup if supported, or simply place it on the first page for easy access.
    *   *Note:* Since the device is always on (just suspending), you rarely need to "boot" it. Just opening the app once keeps it running in the suspend loop.

## Usage Guide

1.  **Start the App:** Open "Literature Clock".
2.  **Enable Suspend:**
    *   Tap the **Gear Icon** (Settings) in the top right.
    *   Select **"Enable Root Suspend"**.
    *   The device will now enter its sleep cycle.
3.  **Battery Logging:**
    *   To monitor power usage, go to Settings -> **Battery Log**.
4.  **Backlight:** 
    *   The app allows the backlight to remain **ON** during sleep.
    *   Simply toggle the backlight using the Tolino hardware button before (or while) the app is running.

## Development

The app is written in Kotlin.

*   `MainActivity.kt`: Handles the UI, Alarm scheduling, and Root Suspend logic.
*   `ShellUtils.kt`: Helper for executing `su` commands.
*   `QuoteRepository.kt`: Parses the CSV files from `assets/`.
