# Visiting Card - Android App

## Overview
A native Android application ("Визитка") — a digital business card that lets users store, edit, and share their contact info, with QR code (vCard format) generation for easy contact sharing.

## Important Note
This is a **native Android application** and cannot run in Replit's web preview. It requires Android SDK, Java/Kotlin toolchain, and an Android device or emulator.

To build and run locally:
```bash
./gradlew assembleDebug
./gradlew installDebug
```

## Technology Stack
- **Language**: Kotlin 2.0.21
- **UI Framework**: Hybrid — Jetpack Compose (main screen shell) + XML Views (business card & bottom sheet)
- **Build System**: Gradle with Kotlin DSL and Version Catalogs
- **Min SDK**: 24 (Android 7.0) | **Target/Compile SDK**: 35

## Key Dependencies
- Material Components 1.12.0 (MaterialToolbar, TextInputLayout, MaterialButton)
- ZXing 3.5.1 (QR code generation)
- Jetpack Compose BOM 2024.09.00
- AndroidX Core KTX, Lifecycle, Activity Compose, ConstraintLayout, CardView

## Project Structure
```
app/src/main/
├── java/com/example/visiting_card/ui/
│   ├── MainActivity.kt       # Main screen — Compose shell + AndroidView XML card
│   └── EditDataActivity.kt   # Edit profile info (View-based)
├── res/
│   ├── layout/
│   │   ├── activity_main.xml      # Business card + BottomSheet layout
│   │   ├── activity_edit_data.xml # Edit form with Material TextInputLayouts
│   │   └── dialog_qr_code.xml     # QR code display dialog
│   ├── drawable/
│   │   ├── bottom_sheet_background.xml  # Rounded top corners
│   │   ├── circle_background.xml        # Circular photo background
│   │   ├── drag_handle.xml              # Bottom sheet drag indicator
│   │   └── ic_arrow_back.xml            # Back arrow for EditDataActivity toolbar
│   ├── values/
│   │   ├── colors.xml   # Colors including primary_red (#D32F2F)
│   │   ├── styles.xml   # AppTheme (Material Components)
│   │   └── themes.xml   # Theme.Visiting_card (Material3, for MainActivity)
│   └── font/garamond_classico_sc.ttf
└── AndroidManifest.xml
```

## Features
- Business card display (name, position, phone, Telegram)
- Bottom sheet with profile photo, email, interests, skills, action buttons
- Edit profile data stored in SharedPreferences (prefs: `"VisitingCardData"`)
- Profile photo selection from gallery
- QR code generation (vCard 3.0 format)
- Share contact info via system share sheet
- Clickable phone (dial), email (compose), Telegram (open app) links
- Dark/Light theme toggle via Compose drawer

## SharedPreferences Contract
All data uses prefs name `"VisitingCardData"` with keys:
- `fullName`, `position`, `phone`, `email`, `telegram`, `interests`, `skills`, `profile_image_uri`

## Bugs Fixed
1. **SharedPreferences mismatch** — EditDataActivity used wrong prefs name (`card_prefs`) and key (`full_name`); now both activities use a shared companion constants.
2. **Invalid color `#E000`** — All buttons now use valid `@color/primary_red` (#D32F2F).
3. **UI never updated after edit** — Replaced local `var` fields with `MutableState`; AndroidView now has a proper `update` lambda driven by Compose recomposition.
4. **extractUsername empty string** — Now checks `isNullOrEmpty()` before opening Telegram URL.
5. **Duplicate Material dependency** — Removed conflicting 1.4.0 and 1.10.0 entries; uses single 1.12.0.
6. **Redundant DrawerLayout/Toolbar in XML** — Removed; Compose's ModalNavigationDrawer and Scaffold TopAppBar handle these.
7. **Dead code** — Removed unused `QrCodeDialog.kt` DialogFragment.
8. **vCard field name** — Changed `N:` to `FN:` for proper display name in vCard 3.0.
9. **Bitmap config** — Changed `RGB_565` to `ARGB_8888` for proper QR code rendering.

## UI Improvements
- MaterialButton (rounded, red) replaces raw Button with broken backgroundTint
- TextInputLayout.OutlinedBox fields with proper inputType in edit screen
- Profile photo moved to top of edit screen with helper text
- MaterialToolbar with back arrow added to EditDataActivity
- BottomSheet is now a NestedScrollView (scrollable) instead of fixed-height LinearLayout
- Improved spacing and typography throughout
