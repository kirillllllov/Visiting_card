# Visiting Card — Android App

## Overview
A native Android application ("Визитка") — a digital business card that lets users store, edit, and share their contact info, with QR code generation, social network management, pinch-to-zoom, and per-element visibility controls.

## Important Note
This is a **native Android application** and cannot run in Replit's web preview. It requires Android SDK, Java/Kotlin toolchain, and an Android device or emulator.

To build and run:
```bash
./gradlew assembleDebug
./gradlew installDebug
```

## Technology Stack
- **Language**: Kotlin 2.0.21
- **UI Framework**: Hybrid — Jetpack Compose (shell + drawer + dialogs) + XML Views (business card & bottom sheet)
- **Build System**: Gradle with Kotlin DSL and Version Catalogs
- **Min SDK**: 24 (Android 7.0) | **Target/Compile SDK**: 35

## Key Dependencies
- Material Components 1.12.0
- ZXing 3.5.1 (QR code generation)
- Jetpack Compose BOM 2024.09.00
- AndroidX Core KTX, Lifecycle, Activity Compose, ConstraintLayout, CardView

## Project Structure
```
app/src/main/
├── java/com/example/visiting_card/ui/
│   ├── MainActivity.kt               # Main screen: Compose drawer + AndroidView card
│   ├── EditDataActivity.kt           # Legacy edit activity (still handles email; companion constants)
│   ├── EditCardInfoActivity.kt       # Combined edit: data fields + visibility toggles + photo
│   ├── AddSocialNetworksActivity.kt  # Manage social network accounts (themed, status-bar-safe)
│   └── SocialNetworkUtils.kt         # Social network data class, load/save/URL builder
├── res/
│   ├── layout/
│   │   ├── activity_main.xml                # Business card + BottomSheet (CoordinatorLayout)
│   │   ├── activity_edit_data.xml           # Legacy edit form (no Telegram)
│   │   ├── activity_edit_card_info.xml      # Combined edit + visibility toggles
│   │   ├── activity_add_social_networks.xml # Social networks management (fitsSystemWindows)
│   │   ├── dialog_qr_code.xml              # QR code display dialog (dynamic title)
│   │   ├── dialog_add_social_network.xml   # Add network dialog (Spinner + TextInput)
│   │   └── item_social_network.xml         # Row in the social networks list
│   ├── drawable/
│   │   ├── bottom_sheet_background.xml
│   │   ├── bottom_sheet_background_dark.xml
│   │   ├── circle_background.xml
│   │   ├── drag_handle.xml
│   │   ├── ic_arrow_back.xml
│   │   ├── ic_delete.xml
│   │   ├── logo_for_light_theme.png
│   │   └── logo_for_dark_theme.png
│   └── values/
│       ├── colors.xml   # Accent = #000000
│       ├── styles.xml
│       └── themes.xml
└── AndroidManifest.xml
```

## Drawer Menu
- **Изменить информацию на визитке** → EditCardInfoActivity (data + visibility)
- **Добавить соц.сети** → AddSocialNetworksActivity
- **Настройки** → Compose AlertDialog with RadioButton theme toggle (Светлая / Тёмная тема)

## Business Card Features
- Pinch-to-zoom (ScaleGestureDetector) with spring-back animation on finger release
- Single tap → toggles bottom sheet (COLLAPSED ↔ HALF_EXPANDED, peekHeight = 60dp)
- Tapping phone number → QR code (vCard format, scan to add to contacts)
- Tapping social info → opens selected social network URL
- Visibility of position, phone, logo, and social network individually controllable

## Bottom Sheet Buttons
1. **Поделиться** — text share via system sheet
2. **Поделиться номером телефона** — phone vCard QR
3. **Поделиться email** — `mailto:` QR
4. **Поделиться соц.сетями** — pick single network (URL QR) or "share all" (vCard NOTE)

## EditCardInfoActivity (replaces Редактировать данные)
- Data fields: ФИО, Должность, Телефон, О себе, Profile photo
- Visibility checkboxes: Должность / Номер телефона / Логотип / Соц.сеть
- When "Соц.сеть" checked: RadioGroup populated with all added social networks
- Saves to SharedPreferences, returns RESULT_OK

## SharedPreferences Contract
Prefs name: `"VisitingCardData"` — all keys in `EditDataActivity.Companion`:
- Data: `fullName`, `position`, `phone`, `email`, `about`, `social_networks`, `profile_image_uri`
- Visibility: `show_position`, `show_phone`, `show_logo`, `show_social`, `selected_social_index`
- Theme: `theme_dark`

## Theme System
- `isDarkTheme: MutableState<Boolean>` — initialized from `theme_dark` pref
- Compose layer: `MaterialTheme(colorScheme = ...)`
- XML layer: `AndroidView` `update` lambda sets colors, button tints, logo resource
- AddSocialNetworksActivity reads `theme_dark` on launch and applies colors programmatically
