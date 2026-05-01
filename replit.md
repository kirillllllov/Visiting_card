# Visiting Card - Android App

## Overview
This is a native Android application called "Visiting Card" (Визитка). It functions as a digital business card app that allows users to store, edit, and share their contact information, and generates a QR code (vCard format) for easy contact sharing.

## Important Note
This is a **native Android application** and cannot run directly in Replit's web preview pane. It requires:
- Android SDK
- Java/Kotlin toolchain
- Android device or emulator

To build and run this app, you would need to use Android Studio on your local machine or a service that provides Android build environments.

## Technology Stack
- **Language**: Kotlin 2.0.21
- **UI Framework**: Hybrid of Jetpack Compose + XML Layouts
- **Build System**: Gradle with Kotlin DSL and Version Catalogs
- **Min SDK**: 24 (Android 7.0)
- **Target/Compile SDK**: 35 (Android 15)

## Key Dependencies
- Material Design 3
- ZXing (for QR code generation)
- Jetpack Compose BOM 2024.09.00
- AndroidX Core KTX, Lifecycle, Activity Compose, ConstraintLayout
- AppCompat

## Project Structure
```
Visiting_card/
├── app/
│   └── src/main/
│       ├── java/com/example/visiting_card/ui/
│       │   ├── MainActivity.kt        # Main screen with business card display & QR
│       │   ├── EditDataActivity.kt    # Edit profile information
│       │   └── theme/                 # Compose theme files (Color, Theme, Type)
│       ├── res/
│       │   ├── layout/               # XML layouts for activities and dialogs
│       │   ├── values/               # Styles, colors, strings
│       │   └── drawable/ & mipmap/   # Icons and graphics
│       └── AndroidManifest.xml
├── gradle/
│   └── libs.versions.toml            # Centralized dependency versions
├── build.gradle                      # Top-level build config
└── settings.gradle                   # Project settings
```

## Features
- Display digital business card with contact info (name, position, phone, email, Telegram)
- Edit profile data stored via SharedPreferences
- Profile photo selection from gallery
- QR code generation (vCard format) for contact sharing
- Share contact info via system share sheet
- Dark/Light theme toggle via navigation drawer
- Clickable phone, email, and Telegram links

## Building Locally
```bash
# Using Gradle Wrapper
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```
