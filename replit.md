# Visiting Card — Android App

## Overview
A native Android application ("Визитка") — a digital business card that lets users store, edit, and share their contact info, with QR code generation and social network management.

## Important Note
This is a **native Android application** and cannot run in Replit's web preview. It requires Android SDK, Java/Kotlin toolchain, and an Android device or emulator.

To build and run locally:
```bash
./gradlew assembleDebug
./gradlew installDebug
```

## Technology Stack
- **Language**: Kotlin 2.0.21
- **UI Framework**: Hybrid — Jetpack Compose (shell + drawer) + XML Views (business card & bottom sheet)
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
│   ├── MainActivity.kt               # Main screen: Compose drawer shell + AndroidView XML card
│   ├── EditDataActivity.kt           # Edit profile info (View-based with MaterialToolbar)
│   ├── AddSocialNetworksActivity.kt  # Manage social network accounts (list + add/delete)
│   └── SocialNetworkUtils.kt         # Social network data class, load/save/URL builder
├── res/
│   ├── layout/
│   │   ├── activity_main.xml                # Business card + BottomSheet (CoordinatorLayout)
│   │   ├── activity_edit_data.xml           # Edit form with MaterialToolbar + TextInputLayouts
│   │   ├── activity_add_social_networks.xml # Social networks management screen
│   │   ├── dialog_qr_code.xml              # QR code display dialog (dynamic title)
│   │   ├── dialog_add_social_network.xml   # Add network dialog (Spinner + TextInput)
│   │   └── item_social_network.xml         # Row in the social networks list
│   ├── drawable/
│   │   ├── bottom_sheet_background.xml       # White with rounded top corners
│   │   ├── bottom_sheet_background_dark.xml  # Dark (#1E1E1E) with rounded top corners
│   │   ├── circle_background.xml             # Circular photo background
│   │   ├── drag_handle.xml                   # Bottom sheet drag indicator
│   │   ├── ic_arrow_back.xml                 # Back arrow vector
│   │   ├── ic_delete.xml                     # Delete (X) icon vector
│   │   ├── logo_for_light_theme.png          # Logo shown on light background
│   │   └── logo_for_dark_theme.png           # Logo shown on dark background
│   ├── values/
│   │   ├── colors.xml   # Accent = #000000 (black); light/dark theme color tokens
│   │   ├── styles.xml   # AppTheme (MaterialComponents) with black accent
│   │   └── themes.xml   # Theme.Visiting_card (Material3 DayNight, for MainActivity)
│   └── font/garamond_classico_sc.ttf
└── AndroidManifest.xml
```

## Features
- Business card display (name, position, phone, Telegram) in a CardView
- Floating hamburger menu button (positioned below status bar) opens Compose ModalNavigationDrawer
- Bottom sheet slides up with: photo, email, about text, and 4 action buttons:
  - **"Поделиться"** — shares contact info as text via system share sheet
  - **"Поделиться номером телефона"** — QR code with phone vCard (scan → add to contacts)
  - **"Поделиться email"** — QR code with `mailto:` URI (scan → open email client)
  - **"Поделиться соц.сетями"** — dialog to pick single social network URL QR or "all" vCard QR
- Drawer menu: theme toggle | edit data | **Добавить соц.сети**
- Social networks screen: supports Telegram, Instagram, Facebook, Twitter/X, LinkedIn, YouTube, TikTok, VK; stored as JSON in prefs under `social_networks` key
- Edit profile stored in SharedPreferences (prefs: `"VisitingCardData"`)
- On first launch (empty name), EditDataActivity opens automatically
- Profile photo from gallery (via photo picker)
- Clickable phone (dial), email (compose), Telegram (open app) fields
- Full light/dark theme toggle — affects Compose drawer AND XML views (background, card, fonts, buttons, logo)

## SharedPreferences Contract
All data uses prefs name `"VisitingCardData"` with keys (all in `EditDataActivity.Companion`):
- `fullName`, `position`, `phone`, `email`, `telegram`, `about`, `social_networks`, `profile_image_uri`

## Theme System
`isDarkTheme: MutableState<Boolean>` drives both layers:
- **Compose layer**: `MaterialTheme(colorScheme = if (dark) darkColors else lightColors)`
- **XML layer**: `AndroidView` `update` lambda applies colors programmatically:
  - Root background, CardView background, BottomSheet background (swaps drawable)
  - Card text colors (primary/secondary)
  - BottomSheet text colors
  - All 4 button `backgroundTintList` + text color
  - Logo `setImageResource` (light ↔ dark logo)

## Bug History (resolved)
1. SharedPreferences name/key mismatch between activities → unified via companion constants
2. Invalid button color `#E000` → replaced with `@color/accent` (#000000)
3. UI never updated post-edit → Compose `MutableState` + `AndroidView` `update` lambda
4. `extractUsername` not checking empty → `isNullOrEmpty()` added
5. Duplicate Material dependency → consolidated to 1.12.0
6. Redundant DrawerLayout + Toolbar in XML → removed; Compose handles both
7. Dead `QrCodeDialog.kt` DialogFragment → deleted
8. vCard used `N:` instead of `FN:` → fixed
9. Bitmap config `RGB_565` → `ARGB_8888`
