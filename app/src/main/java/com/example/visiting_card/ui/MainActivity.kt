package com.example.visiting_card.ui

import android.app.Dialog
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.example.visiting_card.R
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_ABOUT
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_CARD_BG1
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_CARD_BG2
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_CARD_TEMPLATE
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_CARD_TEXT_COLOR
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_EMAIL
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_FULL_NAME
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_LOGO_IMAGE_URI
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_PHONE
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_POSITION
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_PROFILE_IMAGE_URI
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_SELECTED_SOCIAL_INDEX
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_SHOW_LOGO
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_SHOW_PHONE
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_SHOW_POSITION
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_SHOW_SOCIAL
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_SOCIAL_NETWORKS
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_THEME_DARK
import com.example.visiting_card.ui.EditDataActivity.Companion.PREFS_NAME
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs
import kotlin.math.atan2

// ── Card design presets ────────────────────────────────────────────────────
data class CardTheme(val name: String, val bg1: String, val bg2: String, val textColor: String)

val CARD_THEMES = listOf(
    CardTheme("Авто",     "",        "",        ""),
    CardTheme("Классика", "#FFFFFF", "#FFFFFF", "#000000"),
    CardTheme("Тёмный",   "#1C1C1E", "#2C2C2E", "#FFFFFF"),
    CardTheme("Индиго",   "#1a1a2e", "#16213e", "#FFFFFF"),
    CardTheme("Океан",    "#2193b0", "#6dd5ed", "#FFFFFF"),
    CardTheme("Лес",      "#134E5E", "#71B280", "#FFFFFF"),
    CardTheme("Закат",    "#f093fb", "#f5576c", "#FFFFFF"),
    CardTheme("Золото",   "#F7971E", "#FFD200", "#2C2C2E"),
    CardTheme("Лаванда",  "#667eea", "#764ba2", "#FFFFFF"),
    CardTheme("Уголь",    "#232526", "#414345", "#FFFFFF"),
    CardTheme("Вишня",    "#870000", "#190A05", "#FFFFFF"),
    CardTheme("Мята",     "#00b4d8", "#90e0ef", "#003049"),
)

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickLogoLauncher: ActivityResultLauncher<Intent>
    private lateinit var cardInfoLauncher: ActivityResultLauncher<Intent>
    private lateinit var socialNetworksLauncher: ActivityResultLauncher<Intent>
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null

    // ── NFC ────────────────────────────────────────────────────────────────
    private var nfcAdapter: NfcAdapter? = null
    private var nfcPendingIntent: PendingIntent? = null
    private var nfcWriteMode = false
    private var nfcWriteDialog: android.app.AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        nfcPendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val initialActiveId = ProfileManager.initIfNeeded(prefs)

        // ── State ──────────────────────────────────────────────────────────
        val fullNameState        = mutableStateOf(prefs.getString(KEY_FULL_NAME, "") ?: "")
        val positionState        = mutableStateOf(prefs.getString(KEY_POSITION, "") ?: "")
        val phoneState           = mutableStateOf(prefs.getString(KEY_PHONE, "") ?: "")
        val emailState           = mutableStateOf(prefs.getString(KEY_EMAIL, "") ?: "")
        val aboutState           = mutableStateOf(prefs.getString(KEY_ABOUT, "") ?: "")
        val profileImageUriState = mutableStateOf(prefs.getString(KEY_PROFILE_IMAGE_URI, null))
        val logoImageUriState    = mutableStateOf(prefs.getString(KEY_LOGO_IMAGE_URI, null))
        val showPositionState    = mutableStateOf(prefs.getBoolean(KEY_SHOW_POSITION, true))
        val showPhoneState       = mutableStateOf(prefs.getBoolean(KEY_SHOW_PHONE, true))
        val showLogoState        = mutableStateOf(prefs.getBoolean(KEY_SHOW_LOGO, true))
        val showSocialState      = mutableStateOf(prefs.getBoolean(KEY_SHOW_SOCIAL, false))
        val selectedSocialIdx    = mutableStateOf(prefs.getInt(KEY_SELECTED_SOCIAL_INDEX, -1))
        val isDarkTheme          = mutableStateOf(prefs.getBoolean(KEY_THEME_DARK, false))
        val cardBg1State         = mutableStateOf(prefs.getString(KEY_CARD_BG1, "") ?: "")
        val cardBg2State         = mutableStateOf(prefs.getString(KEY_CARD_BG2, "") ?: "")
        val cardTextColorState   = mutableStateOf(prefs.getString(KEY_CARD_TEXT_COLOR, "") ?: "")
        val cardTemplateState    = mutableStateOf(prefs.getInt(KEY_CARD_TEMPLATE, 0))

        // ── Profile state ──────────────────────────────────────────────────
        val activeProfileIdState    = mutableStateOf(initialActiveId)
        val activeProfileLabelState = mutableStateOf(
            ProfileManager.getAllProfiles(prefs).firstOrNull { it.id == initialActiveId }?.label
                ?: ProfileManager.DEFAULT_LABEL
        )
        val allProfilesState = mutableStateOf(ProfileManager.getAllProfiles(prefs))

        // ── Dialog state ───────────────────────────────────────────────────
        val showSettingsDialog    = mutableStateOf(false)
        val showCardDesignDialog  = mutableStateOf(false)
        val showAddProfileDialog  = mutableStateOf(false)
        val addProfileNameInput   = mutableStateOf("")
        val showRenameDialogForId = mutableStateOf<String?>(null)
        val renameInput           = mutableStateOf("")
        val showDeleteConfirmId   = mutableStateOf<String?>(null)

        // ── Reload helper ──────────────────────────────────────────────────
        val reloadFromPrefs: () -> Unit = {
            fullNameState.value        = prefs.getString(KEY_FULL_NAME, "") ?: ""
            positionState.value        = prefs.getString(KEY_POSITION, "") ?: ""
            phoneState.value           = prefs.getString(KEY_PHONE, "") ?: ""
            emailState.value           = prefs.getString(KEY_EMAIL, "") ?: ""
            aboutState.value           = prefs.getString(KEY_ABOUT, "") ?: ""
            profileImageUriState.value = prefs.getString(KEY_PROFILE_IMAGE_URI, null)
            logoImageUriState.value    = prefs.getString(KEY_LOGO_IMAGE_URI, null)
            showPositionState.value    = prefs.getBoolean(KEY_SHOW_POSITION, true)
            showPhoneState.value       = prefs.getBoolean(KEY_SHOW_PHONE, true)
            showLogoState.value        = prefs.getBoolean(KEY_SHOW_LOGO, true)
            showSocialState.value      = prefs.getBoolean(KEY_SHOW_SOCIAL, false)
            selectedSocialIdx.value    = prefs.getInt(KEY_SELECTED_SOCIAL_INDEX, -1)
            cardBg1State.value         = prefs.getString(KEY_CARD_BG1, "") ?: ""
            cardBg2State.value         = prefs.getString(KEY_CARD_BG2, "") ?: ""
            cardTextColorState.value   = prefs.getString(KEY_CARD_TEXT_COLOR, "") ?: ""
            cardTemplateState.value    = prefs.getInt(KEY_CARD_TEMPLATE, 0)
        }

        // ── Card listeners helper (called after every template inflation) ──
        fun setupCardListeners(cardInner: FrameLayout) {
            cardInner.findViewById<TextView?>(R.id.phone_number)?.setOnClickListener {
                showQrDialog(
                    generateQrCode("BEGIN:VCARD\nVERSION:3.0\nFN:${fullNameState.value}\nTEL:${phoneState.value}\nEND:VCARD"),
                    "Поделиться номером"
                )
            }
            cardInner.findViewById<TextView?>(R.id.telegram_info)?.setOnClickListener {
                val networks = SocialNetworkUtils.loadNetworks(prefs, KEY_SOCIAL_NETWORKS)
                val idx      = prefs.getInt(KEY_SELECTED_SOCIAL_INDEX, -1)
                if (idx >= 0 && idx < networks.size) {
                    showQrDialog(generateQrCode(SocialNetworkUtils.getNetworkUrl(networks[idx].type, networks[idx].username)), networks[idx].type)
                }
            }
            cardInner.findViewById<ImageView?>(R.id.logo)?.setOnClickListener {
                pickLogoLauncher.launch(Intent(Intent.ACTION_PICK).apply { type = "image/*" })
            }
            cardInner.findViewById<ImageView?>(R.id.card_photo)?.setOnClickListener {
                pickImageLauncher.launch(Intent(Intent.ACTION_PICK).apply { type = "image/*" })
            }
        }

        // ── Activity result launchers ──────────────────────────────────────
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data ?: return@registerForActivityResult
                prefs.edit().putString(KEY_PROFILE_IMAGE_URI, uri.toString()).apply()
                profileImageUriState.value = uri.toString()
                ProfileManager.syncActiveFromMainPrefs(prefs, activeProfileIdState.value, activeProfileLabelState.value)
                allProfilesState.value = ProfileManager.getAllProfiles(prefs)
            }
        }

        pickLogoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data ?: return@registerForActivityResult
                prefs.edit().putString(KEY_LOGO_IMAGE_URI, uri.toString()).apply()
                logoImageUriState.value = uri.toString()
                ProfileManager.syncActiveFromMainPrefs(prefs, activeProfileIdState.value, activeProfileLabelState.value)
                allProfilesState.value = ProfileManager.getAllProfiles(prefs)
            }
        }

        cardInfoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                reloadFromPrefs()
                ProfileManager.syncActiveFromMainPrefs(prefs, activeProfileIdState.value, activeProfileLabelState.value)
                allProfilesState.value = ProfileManager.getAllProfiles(prefs)
            }
        }

        socialNetworksLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            ProfileManager.syncActiveFromMainPrefs(prefs, activeProfileIdState.value, activeProfileLabelState.value)
            allProfilesState.value = ProfileManager.getAllProfiles(prefs)
        }

        if (prefs.getString(KEY_FULL_NAME, "").isNullOrEmpty()) {
            cardInfoLauncher.launch(Intent(this, EditCardInfoActivity::class.java))
        }

        setContent {
            val darkColors = darkColorScheme(
                primary      = androidx.compose.ui.graphics.Color.White,
                onPrimary    = androidx.compose.ui.graphics.Color.Black,
                background   = androidx.compose.ui.graphics.Color(0xFF121212),
                surface      = androidx.compose.ui.graphics.Color(0xFF1E1E1E),
                onBackground = androidx.compose.ui.graphics.Color.White,
                onSurface    = androidx.compose.ui.graphics.Color.White
            )
            val lightColors = lightColorScheme(
                primary      = androidx.compose.ui.graphics.Color.Black,
                onPrimary    = androidx.compose.ui.graphics.Color.White,
                background   = androidx.compose.ui.graphics.Color.White,
                surface      = androidx.compose.ui.graphics.Color.White,
                onBackground = androidx.compose.ui.graphics.Color.Black,
                onSurface    = androidx.compose.ui.graphics.Color.Black
            )

            MaterialTheme(colorScheme = if (isDarkTheme.value) darkColors else lightColors) {
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scope       = rememberCoroutineScope()
                val isDark      = isDarkTheme.value
                val iconTint    = if (isDark) androidx.compose.ui.graphics.Color.White
                                  else        androidx.compose.ui.graphics.Color.Black
                val diagramColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                val diagramFade  = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)

                // ── Add profile dialog ─────────────────────────────────────
                if (showAddProfileDialog.value) {
                    AlertDialog(
                        onDismissRequest = { showAddProfileDialog.value = false; addProfileNameInput.value = "" },
                        title = { Text("Новый профиль") },
                        text  = {
                            OutlinedTextField(
                                value = addProfileNameInput.value, onValueChange = { addProfileNameInput.value = it },
                                label = { Text("Название") }, placeholder = { Text("Работа, Личное…") },
                                singleLine = true, modifier = Modifier.fillMaxWidth()
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val label = addProfileNameInput.value.trim().ifEmpty { "Профиль ${allProfilesState.value.size + 1}" }
                                val np = ProfileManager.createAndSwitchTo(prefs, activeProfileIdState.value, activeProfileLabelState.value, label)
                                activeProfileIdState.value = np.id; activeProfileLabelState.value = np.label
                                allProfilesState.value = ProfileManager.getAllProfiles(prefs); reloadFromPrefs()
                                showAddProfileDialog.value = false; addProfileNameInput.value = ""
                                scope.launch { drawerState.close() }
                            }) { Text("Создать") }
                        },
                        dismissButton = { TextButton(onClick = { showAddProfileDialog.value = false; addProfileNameInput.value = "" }) { Text("Отмена") } }
                    )
                }

                // ── Rename dialog ──────────────────────────────────────────
                showRenameDialogForId.value?.let { renameId ->
                    AlertDialog(
                        onDismissRequest = { showRenameDialogForId.value = null },
                        title = { Text("Переименовать профиль") },
                        text  = { OutlinedTextField(value = renameInput.value, onValueChange = { renameInput.value = it }, label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
                        confirmButton = {
                            TextButton(onClick = {
                                val lbl = renameInput.value.trim()
                                if (lbl.isNotEmpty()) { ProfileManager.renameProfile(prefs, renameId, lbl); if (renameId == activeProfileIdState.value) activeProfileLabelState.value = lbl; allProfilesState.value = ProfileManager.getAllProfiles(prefs) }
                                showRenameDialogForId.value = null
                            }) { Text("Сохранить") }
                        },
                        dismissButton = { TextButton(onClick = { showRenameDialogForId.value = null }) { Text("Отмена") } }
                    )
                }

                // ── Delete confirm ─────────────────────────────────────────
                showDeleteConfirmId.value?.let { deleteId ->
                    val lbl = allProfilesState.value.firstOrNull { it.id == deleteId }?.label ?: ""
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirmId.value = null },
                        title = { Text("Удалить профиль?") },
                        text  = { Text("Профиль «$lbl» и все его данные будут удалены.") },
                        confirmButton = {
                            TextButton(onClick = {
                                val na = ProfileManager.deleteProfile(prefs, deleteId, activeProfileIdState.value, activeProfileLabelState.value)
                                if (na != null) { activeProfileIdState.value = na.id; activeProfileLabelState.value = na.label; reloadFromPrefs() }
                                allProfilesState.value = ProfileManager.getAllProfiles(prefs); showDeleteConfirmId.value = null
                            }) { Text("Удалить", color = androidx.compose.ui.graphics.Color.Red) }
                        },
                        dismissButton = { TextButton(onClick = { showDeleteConfirmId.value = null }) { Text("Отмена") } }
                    )
                }

                // ── Card design dialog ─────────────────────────────────────
                if (showCardDesignDialog.value) {
                    AlertDialog(
                        onDismissRequest = { showCardDesignDialog.value = false },
                        title = { Text("Дизайн карточки") },
                        text  = {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

                                // ── Template selection ─────────────────────
                                Text("Макет", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    val templates = listOf("Классика", "Фото+текст", "Минимал")
                                    templates.forEachIndexed { idx, name ->
                                        val isSel = cardTemplateState.value == idx
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.clickable {
                                                cardTemplateState.value = idx
                                                prefs.edit().putInt(KEY_CARD_TEMPLATE, idx).apply()
                                                ProfileManager.syncActiveFromMainPrefs(prefs, activeProfileIdState.value, activeProfileLabelState.value)
                                                allProfilesState.value = ProfileManager.getAllProfiles(prefs)
                                            }.padding(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(88.dp).height(54.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surface)
                                                    .border(if (isSel) 2.dp else 1.dp,
                                                        if (isSel) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                        RoundedCornerShape(8.dp))
                                                    .padding(6.dp)
                                            ) {
                                                when (idx) {
                                                    0 -> { // Classic
                                                        Box(Modifier.size(12.dp, 3.dp).align(Alignment.TopStart).background(diagramColor, RoundedCornerShape(1.dp)))
                                                        Box(Modifier.size(20.dp, 3.dp).align(Alignment.TopEnd).background(diagramColor, RoundedCornerShape(1.dp)))
                                                        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Box(Modifier.size(30.dp, 4.dp).background(diagramColor, RoundedCornerShape(1.dp)))
                                                            Spacer(Modifier.height(3.dp))
                                                            Box(Modifier.size(20.dp, 3.dp).background(diagramFade, RoundedCornerShape(1.dp)))
                                                        }
                                                        Box(Modifier.size(16.dp, 3.dp).align(Alignment.BottomCenter).background(diagramFade, RoundedCornerShape(1.dp)))
                                                    }
                                                    1 -> { // Horizontal
                                                        Box(Modifier.size(22.dp, 22.dp).clip(RoundedCornerShape(11.dp)).background(diagramFade).align(Alignment.CenterStart))
                                                        Column(Modifier.align(Alignment.CenterStart).padding(start = 28.dp)) {
                                                            Box(Modifier.size(32.dp, 4.dp).background(diagramColor, RoundedCornerShape(1.dp)))
                                                            Spacer(Modifier.height(3.dp))
                                                            Box(Modifier.size(22.dp, 3.dp).background(diagramFade, RoundedCornerShape(1.dp)))
                                                            Spacer(Modifier.height(3.dp))
                                                            Box(Modifier.size(18.dp, 2.dp).background(diagramFade, RoundedCornerShape(1.dp)))
                                                        }
                                                    }
                                                    2 -> { // Minimal
                                                        Box(Modifier.size(36.dp, 5.dp).align(Alignment.Center).offset(y = (-6).dp).background(diagramColor, RoundedCornerShape(1.dp)))
                                                        Box(Modifier.size(52.dp, 1.dp).align(Alignment.Center).background(diagramFade))
                                                        Box(Modifier.size(24.dp, 3.dp).align(Alignment.Center).offset(y = 7.dp).background(diagramFade, RoundedCornerShape(1.dp)))
                                                        Box(Modifier.size(14.dp, 3.dp).align(Alignment.BottomStart).background(diagramFade, RoundedCornerShape(1.dp)))
                                                        Box(Modifier.size(14.dp, 3.dp).align(Alignment.BottomEnd).background(diagramFade, RoundedCornerShape(1.dp)))
                                                    }
                                                }
                                            }
                                            Text(name, fontSize = 10.sp, maxLines = 1,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.padding(top = 4.dp))
                                        }
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                                // ── Color selection ────────────────────────
                                Text("Цвет", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 8.dp))
                                CARD_THEMES.chunked(3).forEach { row ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                        row.forEach { theme ->
                                            val isSel = theme.bg1 == cardBg1State.value
                                            val c1 = if (theme.bg1.isNotEmpty()) androidx.compose.ui.graphics.Color(Color.parseColor(theme.bg1))
                                                     else if (isDark) androidx.compose.ui.graphics.Color(0xFF1E1E1E) else androidx.compose.ui.graphics.Color.White
                                            val c2 = if (theme.bg2.isNotEmpty()) androidx.compose.ui.graphics.Color(Color.parseColor(theme.bg2)) else c1
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.padding(4.dp).clickable {
                                                    cardBg1State.value = theme.bg1; cardBg2State.value = theme.bg2; cardTextColorState.value = theme.textColor
                                                    prefs.edit().putString(KEY_CARD_BG1, theme.bg1).putString(KEY_CARD_BG2, theme.bg2).putString(KEY_CARD_TEXT_COLOR, theme.textColor).apply()
                                                    ProfileManager.syncActiveFromMainPrefs(prefs, activeProfileIdState.value, activeProfileLabelState.value)
                                                    allProfilesState.value = ProfileManager.getAllProfiles(prefs)
                                                    showCardDesignDialog.value = false
                                                }
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(72.dp).height(90.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(Brush.linearGradient(listOf(c1, c2)))
                                                        .then(if (isSel) Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)) else Modifier)
                                                )
                                                Text(theme.name, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp).width(72.dp),
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                                            }
                                        }
                                        repeat(3 - row.size) { Spacer(Modifier.width(80.dp)) }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                        },
                        confirmButton = { TextButton(onClick = { showCardDesignDialog.value = false }) { Text("Закрыть") } }
                    )
                }

                // ── Settings dialog ────────────────────────────────────────
                if (showSettingsDialog.value) {
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text("Настройки") },
                        text  = {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                Text("Тема оформления", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { isDarkTheme.value = false; prefs.edit().putBoolean(KEY_THEME_DARK, false).apply() }.padding(vertical = 4.dp)) {
                                    RadioButton(selected = !isDarkTheme.value, onClick = { isDarkTheme.value = false; prefs.edit().putBoolean(KEY_THEME_DARK, false).apply() })
                                    Spacer(Modifier.width(6.dp)); Text("Светлая тема")
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { isDarkTheme.value = true; prefs.edit().putBoolean(KEY_THEME_DARK, true).apply() }.padding(vertical = 4.dp)) {
                                    RadioButton(selected = isDarkTheme.value, onClick = { isDarkTheme.value = true; prefs.edit().putBoolean(KEY_THEME_DARK, true).apply() })
                                    Spacer(Modifier.width(6.dp)); Text("Тёмная тема")
                                }
                                Spacer(Modifier.height(12.dp)); HorizontalDivider(); Spacer(Modifier.height(10.dp))
                                Text("Отображение на визитке", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 4.dp))

                                @Composable
                                fun visRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onToggle(!checked) }.padding(vertical = 2.dp)) {
                                        Checkbox(checked = checked, onCheckedChange = onToggle); Spacer(Modifier.width(4.dp)); Text(label)
                                    }
                                }
                                visRow("Должность", showPositionState.value) { v -> showPositionState.value = v; prefs.edit().putBoolean(KEY_SHOW_POSITION, v).apply() }
                                visRow("Номер телефона", showPhoneState.value) { v -> showPhoneState.value = v; prefs.edit().putBoolean(KEY_SHOW_PHONE, v).apply() }
                                visRow("Логотип", showLogoState.value) { v -> showLogoState.value = v; prefs.edit().putBoolean(KEY_SHOW_LOGO, v).apply() }
                                visRow("Социальная сеть", showSocialState.value) { v -> showSocialState.value = v; prefs.edit().putBoolean(KEY_SHOW_SOCIAL, v).apply() }

                                val nets = SocialNetworkUtils.loadNetworks(prefs, KEY_SOCIAL_NETWORKS)
                                if (showSocialState.value && nets.isNotEmpty()) {
                                    Spacer(Modifier.height(4.dp))
                                    nets.forEachIndexed { idx, n ->
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { selectedSocialIdx.value = idx; prefs.edit().putInt(KEY_SELECTED_SOCIAL_INDEX, idx).apply() }.padding(horizontal = 8.dp, vertical = 2.dp)) {
                                            RadioButton(selected = selectedSocialIdx.value == idx, onClick = { selectedSocialIdx.value = idx; prefs.edit().putInt(KEY_SELECTED_SOCIAL_INDEX, idx).apply() })
                                            Spacer(Modifier.width(4.dp)); Text("${n.type}: ${n.username}")
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = { TextButton(onClick = { showSettingsDialog.value = false }) { Text("Закрыть") } }
                    )
                }

                // ── Navigation Drawer ──────────────────────────────────────
                ModalNavigationDrawer(
                    drawerState   = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Spacer(Modifier.height(16.dp))
                            Text("ПРОФИЛИ", modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)

                            allProfilesState.value.forEach { profile ->
                                val isActive = profile.id == activeProfileIdState.value
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().clickable(enabled = !isActive) {
                                        val sw = ProfileManager.switchTo(prefs, activeProfileIdState.value, activeProfileLabelState.value, profile.id)
                                        if (sw != null) { activeProfileIdState.value = sw.id; activeProfileLabelState.value = sw.label; allProfilesState.value = ProfileManager.getAllProfiles(prefs); reloadFromPrefs(); scope.launch { drawerState.close() } }
                                    }.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                                ) {
                                    if (isActive) Icon(Icons.Default.Check, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                    else Spacer(Modifier.size(18.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text(profile.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal)
                                    IconButton(onClick = { renameInput.value = profile.label; showRenameDialogForId.value = profile.id }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Default.Edit, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                    }
                                    if (allProfilesState.value.size > 1) {
                                        IconButton(onClick = { showDeleteConfirmId.value = profile.id }, modifier = Modifier.size(36.dp)) {
                                            Icon(Icons.Default.Close, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
                                        }
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { addProfileNameInput.value = ""; showAddProfileDialog.value = true }
                                    .padding(start = 12.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)) {
                                Icon(Icons.Default.Add, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                Spacer(Modifier.width(10.dp))
                                Text("Добавить профиль", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            }

                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                            listOf(
                                "Информация на визитке" to { cardInfoLauncher.launch(Intent(this@MainActivity, EditCardInfoActivity::class.java)); scope.launch { drawerState.close() } },
                                "Социальные сети"       to { socialNetworksLauncher.launch(Intent(this@MainActivity, AddSocialNetworksActivity::class.java)); scope.launch { drawerState.close() } },
                                "Дизайн карточки"       to { showCardDesignDialog.value = true; scope.launch { drawerState.close() } },
                                "Настройки"             to { showSettingsDialog.value = true; scope.launch { drawerState.close() } }
                            ).forEachIndexed { i, (label, action) ->
                                Text(label, modifier = Modifier.fillMaxWidth().clickable { action() }.padding(horizontal = 20.dp, vertical = 14.dp), style = MaterialTheme.typography.bodyLarge)
                                if (i < 3) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory  = { ctx ->
                                val root = layoutInflater.inflate(R.layout.activity_main, null)

                                // Bottom sheet setup
                                val bottomSheet = root.findViewById<View>(R.id.bottomSheet)
                                val peekPx      = (60 * ctx.resources.displayMetrics.density).toInt()
                                bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet).apply {
                                    state = BottomSheetBehavior.STATE_COLLAPSED; peekHeight = peekPx
                                    isHideable = false; halfExpandedRatio = 0.45f
                                }

                                // Bottom sheet buttons
                                root.findViewById<MaterialButton>(R.id.share_phone_button).setOnClickListener {
                                    showQrDialog(generateQrCode("BEGIN:VCARD\nVERSION:3.0\nFN:${fullNameState.value}\nTEL:${phoneState.value}\nEND:VCARD"), "Поделиться номером")
                                }
                                root.findViewById<MaterialButton>(R.id.share_email_button).setOnClickListener {
                                    val email = prefs.getString(KEY_EMAIL, "") ?: ""
                                    if (email.isNotBlank()) showQrDialog(generateQrCode("mailto:$email"), "Поделиться email")
                                }
                                root.findViewById<MaterialButton>(R.id.share_social_button).setOnClickListener {
                                    showSocialShareDialog(fullNameState.value)
                                }
                                root.findViewById<MaterialButton>(R.id.export_vcf_button).setOnClickListener {
                                    shareVCardFile()
                                }
                                root.findViewById<ImageView>(R.id.photo).setOnClickListener {
                                    pickImageLauncher.launch(Intent(Intent.ACTION_PICK).apply { type = "image/*" })
                                }

                                // Enable outline clipping on card for gradient themes
                                val businessCard = root.findViewById<CardView>(R.id.business_card)
                                businessCard.clipToOutline = true

                                // Touch handler: scale + rotate + tap + swipe
                                var scaleFactor  = 1f
                                var prevAngle    = Float.NaN
                                var cardRotation = 0f

                                val fingerAngle: (MotionEvent) -> Float = { ev ->
                                    val dx = ev.getX(0) - ev.getX(1)
                                    val dy = ev.getY(0) - ev.getY(1)
                                    Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                }

                                val scaleDetector = ScaleGestureDetector(ctx,
                                    object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                                        override fun onScale(d: ScaleGestureDetector): Boolean {
                                            scaleFactor = (scaleFactor * d.scaleFactor).coerceIn(1f, 3f)
                                            businessCard.scaleX = scaleFactor; businessCard.scaleY = scaleFactor
                                            return true
                                        }
                                    })

                                val gestureDetector = GestureDetector(ctx,
                                    object : GestureDetector.SimpleOnGestureListener() {
                                        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                                            bottomSheetBehavior?.let { bsb ->
                                                bsb.state = if (bsb.state == BottomSheetBehavior.STATE_COLLAPSED)
                                                    BottomSheetBehavior.STATE_HALF_EXPANDED
                                                else BottomSheetBehavior.STATE_COLLAPSED
                                            }
                                            return true
                                        }

                                        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                                            val e1x = e1?.x ?: return false
                                            val dx  = e2.x - e1x
                                            val dy  = e2.y - (e1?.y ?: e2.y)
                                            if (abs(dx) < 120f || abs(dx) < abs(dy) * 1.2f) return false
                                            val profiles   = allProfilesState.value
                                            if (profiles.size <= 1) return false
                                            val currentIdx = profiles.indexOfFirst { it.id == activeProfileIdState.value }
                                            val nextIdx    = if (dx < 0) (currentIdx + 1).coerceAtMost(profiles.size - 1)
                                                             else        (currentIdx - 1).coerceAtLeast(0)
                                            if (nextIdx == currentIdx) return false
                                            val sw = ProfileManager.switchTo(prefs, activeProfileIdState.value, activeProfileLabelState.value, profiles[nextIdx].id)
                                            if (sw != null) {
                                                activeProfileIdState.value    = sw.id
                                                activeProfileLabelState.value = sw.label
                                                allProfilesState.value        = ProfileManager.getAllProfiles(prefs)
                                                reloadFromPrefs()
                                                businessCard.animate().scaleX(0.93f).scaleY(0.93f).setDuration(100)
                                                    .withEndAction { businessCard.animate().scaleX(1f).scaleY(1f).setDuration(180).setInterpolator(DecelerateInterpolator()).start() }
                                                    .start()
                                            }
                                            return true
                                        }
                                    })

                                businessCard.setOnTouchListener { v, event ->
                                    scaleDetector.onTouchEvent(event)
                                    gestureDetector.onTouchEvent(event)
                                    when (event.actionMasked) {
                                        MotionEvent.ACTION_DOWN -> v.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                                        MotionEvent.ACTION_POINTER_DOWN -> if (event.pointerCount == 2) prevAngle = fingerAngle(event)
                                        MotionEvent.ACTION_MOVE ->
                                            if (event.pointerCount >= 2 && !prevAngle.isNaN()) {
                                                val angle = fingerAngle(event); val delta = angle - prevAngle
                                                if (delta in -30f..30f) { cardRotation = (cardRotation + delta).coerceIn(-35f, 35f); v.rotation = cardRotation }
                                                prevAngle = angle
                                            }
                                        MotionEvent.ACTION_POINTER_UP -> prevAngle = Float.NaN
                                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                            prevAngle = Float.NaN
                                            val needsReset = scaleFactor != 1f || cardRotation != 0f
                                            scaleFactor = 1f; cardRotation = 0f
                                            v.animate().scaleX(1f).scaleY(1f).rotation(0f)
                                                .setDuration(if (needsReset) 300L else 0L)
                                                .setInterpolator(DecelerateInterpolator())
                                                .withEndAction { v.setLayerType(View.LAYER_TYPE_NONE, null) }
                                                .start()
                                        }
                                    }
                                    true
                                }

                                root
                            },
                            update = { root ->
                                val dark     = isDarkTheme.value
                                val bg1      = cardBg1State.value
                                val bg2      = cardBg2State.value
                                val ctxt     = cardTextColorState.value
                                val hasCustom = bg1.isNotEmpty()

                                // ── Bottom sheet background ────────────────
                                root.setBackgroundColor(if (dark) Color.parseColor("#121212") else Color.WHITE)
                                root.findViewById<View>(R.id.bottomSheet).setBackgroundResource(
                                    if (dark) R.drawable.bottom_sheet_background_dark else R.drawable.bottom_sheet_background
                                )

                                // ── Bottom sheet photo theme ───────────────
                                root.findViewById<ImageView>(R.id.photo).setBackgroundResource(
                                    if (dark) R.drawable.circle_background_editable_dark else R.drawable.circle_background_editable
                                )

                                // ── Email display ──────────────────────────
                                val emailView = root.findViewById<TextView>(R.id.email_text)
                                val email = emailState.value
                                emailView.text = email
                                emailView.setTextColor(if (dark) Color.parseColor("#BBBBBB") else Color.parseColor("#555555"))
                                emailView.visibility = if (email.isNotEmpty()) View.VISIBLE else View.GONE

                                // ── About ──────────────────────────────────
                                root.findViewById<TextView>(R.id.about).also { v ->
                                    v.text = aboutState.value
                                    v.setTextColor(if (dark) Color.parseColor("#AAAAAA") else Color.parseColor("#777777"))
                                }

                                // ── Bottom sheet buttons ───────────────────
                                val btnBg   = if (dark) Color.WHITE else Color.BLACK
                                val btnText = if (dark) Color.BLACK else Color.WHITE
                                val btnTint = android.content.res.ColorStateList.valueOf(btnBg)
                                listOf(R.id.share_phone_button, R.id.share_email_button, R.id.share_social_button, R.id.export_vcf_button).forEach { id ->
                                    root.findViewById<MaterialButton>(id).also { btn -> btn.backgroundTintList = btnTint; btn.setTextColor(btnText) }
                                }

                                // ── Bottom sheet profile photo ─────────────
                                root.findViewById<ImageView>(R.id.photo).also { pv ->
                                    val uri = profileImageUriState.value
                                    if (uri != null) pv.setImageURI(Uri.parse(uri))
                                    else pv.setImageResource(R.drawable.photo)
                                }

                                // ── Card template: detect change, re-inflate ─
                                val cardInner = root.findViewById<FrameLayout>(R.id.card_inner)
                                val cardView  = root.findViewById<CardView>(R.id.business_card)
                                val templateResId = when (cardTemplateState.value) {
                                    1    -> R.layout.layout_card_horizontal
                                    2    -> R.layout.layout_card_minimal
                                    else -> R.layout.layout_card_classic
                                }
                                if (cardInner.tag as? Int != templateResId) {
                                    cardInner.removeAllViews()
                                    layoutInflater.inflate(templateResId, cardInner, true)
                                    cardInner.tag = templateResId
                                    setupCardListeners(cardInner)
                                }

                                // ── Card background ────────────────────────
                                if (hasCustom) {
                                    val c1     = Color.parseColor(bg1)
                                    val c2     = if (bg2.isNotEmpty()) Color.parseColor(bg2) else c1
                                    val radius = 16f * root.resources.displayMetrics.density
                                    cardView.setCardBackgroundColor(Color.TRANSPARENT)
                                    cardInner.background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(c1, c2)).apply { cornerRadius = radius }
                                    val tc = if (ctxt.isNotEmpty()) Color.parseColor(ctxt) else Color.WHITE
                                    root.findViewById<TextView?>(R.id.full_name)?.setTextColor(tc)
                                    root.findViewById<TextView?>(R.id.position)?.setTextColor(tc)
                                    root.findViewById<TextView?>(R.id.phone_number)?.setTextColor(tc)
                                    root.findViewById<TextView?>(R.id.telegram_info)?.setTextColor(tc)
                                    root.findViewById<View?>(R.id.divider_line)?.setBackgroundColor(tc)
                                } else {
                                    cardView.setCardBackgroundColor(if (dark) Color.parseColor("#1E1E1E") else Color.WHITE)
                                    cardInner.background = null
                                    val primary   = if (dark) Color.WHITE else Color.BLACK
                                    val secondary = if (dark) Color.parseColor("#BBBBBB") else Color.parseColor("#555555")
                                    root.findViewById<TextView?>(R.id.full_name)?.setTextColor(primary)
                                    root.findViewById<TextView?>(R.id.position)?.setTextColor(secondary)
                                    root.findViewById<TextView?>(R.id.phone_number)?.setTextColor(primary)
                                    root.findViewById<TextView?>(R.id.telegram_info)?.setTextColor(primary)
                                    root.findViewById<View?>(R.id.divider_line)?.setBackgroundColor(if (dark) Color.parseColor("#888888") else Color.parseColor("#333333"))
                                }

                                // ── Logo ───────────────────────────────────
                                root.findViewById<ImageView?>(R.id.logo)?.also { logo ->
                                    val logoUri = logoImageUriState.value
                                    if (logoUri != null) logo.setImageURI(Uri.parse(logoUri))
                                    else logo.setImageResource(if (dark) R.drawable.logo_for_dark_theme else R.drawable.logo_for_light_theme)
                                    logo.visibility = if (showLogoState.value) View.VISIBLE else View.GONE
                                }

                                // ── Card data + visibility ─────────────────
                                root.findViewById<TextView?>(R.id.full_name)?.text = fullNameState.value

                                root.findViewById<TextView?>(R.id.position)?.also { v ->
                                    v.text = positionState.value
                                    v.visibility = if (showPositionState.value) View.VISIBLE else View.GONE
                                }
                                root.findViewById<TextView?>(R.id.phone_number)?.also { v ->
                                    v.text = phoneState.value
                                    v.visibility = if (showPhoneState.value) View.VISIBLE else View.GONE
                                }
                                if (showSocialState.value) {
                                    val networks = SocialNetworkUtils.loadNetworks(prefs, KEY_SOCIAL_NETWORKS)
                                    val idx      = selectedSocialIdx.value
                                    root.findViewById<TextView?>(R.id.telegram_info)?.also { v ->
                                        if (idx >= 0 && idx < networks.size) { v.text = "${networks[idx].type}: ${networks[idx].username}"; v.visibility = View.VISIBLE }
                                        else v.visibility = View.GONE
                                    }
                                } else root.findViewById<TextView?>(R.id.telegram_info)?.visibility = View.GONE

                                // ── card_photo (horizontal template) ───────
                                root.findViewById<ImageView?>(R.id.card_photo)?.also { cpv ->
                                    val uri = profileImageUriState.value
                                    if (uri != null) cpv.setImageURI(Uri.parse(uri))
                                    else cpv.setImageResource(R.drawable.photo)
                                    cpv.setBackgroundResource(if (dark) R.drawable.circle_background_editable_dark else R.drawable.circle_background_editable)
                                }
                            }
                        )

                        // Hamburger button
                        IconButton(onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.align(Alignment.TopStart).padding(top = 28.dp, start = 4.dp)) {
                            Icon(Icons.Default.Menu, "Меню", tint = iconTint)
                        }
                    }
                }
            }
        }
    }

    // ── NFC lifecycle ──────────────────────────────────────────────────────
    override fun onResume() { super.onResume(); if (nfcWriteMode) nfcAdapter?.enableForegroundDispatch(this, nfcPendingIntent, null, null) }
    override fun onPause() { super.onPause(); nfcAdapter?.disableForegroundDispatch(this) }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (nfcWriteMode && (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action || NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action || NfcAdapter.ACTION_TECH_DISCOVERED == intent.action)) {
            val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
                            else @Suppress("DEPRECATION") intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            tag?.let { writeNdefToTag(it) }
        }
    }

    // ── vCard helpers ──────────────────────────────────────────────────────
    private fun buildVCard(): String {
        val prefs    = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val networks = SocialNetworkUtils.loadNetworks(prefs, KEY_SOCIAL_NETWORKS)
        return buildString {
            appendLine("BEGIN:VCARD"); appendLine("VERSION:3.0")
            prefs.getString(KEY_FULL_NAME, "")?.takeIf { it.isNotBlank() }?.let { appendLine("FN:$it") }
            prefs.getString(KEY_PHONE, "")?.takeIf    { it.isNotBlank() }?.let { appendLine("TEL:$it") }
            prefs.getString(KEY_EMAIL, "")?.takeIf    { it.isNotBlank() }?.let { appendLine("EMAIL:$it") }
            prefs.getString(KEY_POSITION, "")?.takeIf { it.isNotBlank() }?.let { appendLine("TITLE:$it") }
            if (networks.isNotEmpty()) appendLine("NOTE:${networks.joinToString(", ") { "${it.type}: ${it.username}" }}")
            append("END:VCARD")
        }
    }

    private fun shareVCardFile() {
        try {
            val file = File(cacheDir, "contact.vcf"); file.writeText(buildVCard())
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/x-vcard"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Поделиться контактом"))
        } catch (e: Exception) { Toast.makeText(this, "Ошибка: ${e.localizedMessage}", Toast.LENGTH_LONG).show() }
    }

    // ── NFC write ──────────────────────────────────────────────────────────
    private fun showNfcWriteDialog() {
        nfcWriteMode = true; nfcAdapter?.enableForegroundDispatch(this, nfcPendingIntent, null, null)
        nfcWriteDialog = android.app.AlertDialog.Builder(this)
            .setTitle("Запись на NFC-метку")
            .setMessage("Поднесите телефон к NFC-метке.\n\nКонтакт будет записан в формате vCard.")
            .setNegativeButton("Отмена") { d, _ -> d.dismiss(); nfcWriteMode = false; nfcAdapter?.disableForegroundDispatch(this) }
            .setCancelable(false).show()
    }

    private fun writeNdefToTag(tag: Tag) {
        val msg = NdefMessage(arrayOf(NdefRecord.createMime("text/x-vCard", buildVCard().toByteArray(Charsets.UTF_8))))
        try {
            val ndef = Ndef.get(tag); val ndefF = NdefFormatable.get(tag)
            when {
                ndef != null -> {
                    ndef.connect()
                    when {
                        !ndef.isWritable  -> runOnUiThread { Toast.makeText(this, "NFC-метка защищена от записи", Toast.LENGTH_LONG).show() }
                        ndef.maxSize < msg.byteArrayLength -> runOnUiThread { Toast.makeText(this, "NFC-метка слишком маленькая", Toast.LENGTH_LONG).show() }
                        else -> { ndef.writeNdefMessage(msg); runOnUiThread { nfcWriteDialog?.dismiss(); nfcWriteMode = false; nfcAdapter?.disableForegroundDispatch(this); Toast.makeText(this, "✓ Записано!", Toast.LENGTH_SHORT).show() } }
                    }
                    ndef.close()
                }
                ndefF != null -> { ndefF.connect(); ndefF.format(msg); ndefF.close(); runOnUiThread { nfcWriteDialog?.dismiss(); nfcWriteMode = false; nfcAdapter?.disableForegroundDispatch(this); Toast.makeText(this, "✓ Записано!", Toast.LENGTH_SHORT).show() } }
                else -> runOnUiThread { Toast.makeText(this, "NFC-метка не поддерживает запись", Toast.LENGTH_LONG).show() }
            }
        } catch (e: Exception) { runOnUiThread { Toast.makeText(this, "Ошибка: ${e.localizedMessage}", Toast.LENGTH_LONG).show() } }
    }

    // ── Social share dialog ────────────────────────────────────────────────
    private fun showSocialShareDialog(ownerName: String) {
        val prefs    = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val networks = SocialNetworkUtils.loadNetworks(prefs, KEY_SOCIAL_NETWORKS)
        if (networks.isEmpty()) { android.app.AlertDialog.Builder(this).setTitle("Социальные сети").setMessage("Нет добавленных соц.сетей.").setPositiveButton("OK", null).show(); return }
        val dialog  = android.app.AlertDialog.Builder(this).setTitle("Выберите соц.сеть").create()
        val density = resources.displayMetrics.density
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding((16 * density).toInt(), 8, (16 * density).toInt(), 8) }
        networks.forEach { n ->
            content.addView(MaterialButton(this).apply {
                text = "${n.type}: ${n.username}"; setTextColor(Color.WHITE); backgroundTintList = android.content.res.ColorStateList.valueOf(Color.BLACK)
                cornerRadius = (24 * density).toInt()
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.bottomMargin = (10 * density).toInt() }
                setOnClickListener { dialog.dismiss(); showQrDialog(generateQrCode(SocialNetworkUtils.getNetworkUrl(n.type, n.username)), n.type) }
            })
        }
        content.addView(MaterialButton(this).apply {
            text = "Поделиться всеми соц.сетями"; setTextColor(Color.BLACK); backgroundTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            strokeColor = android.content.res.ColorStateList.valueOf(Color.BLACK); strokeWidth = (1 * density).toInt(); cornerRadius = (24 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.topMargin = (6 * density).toInt() }
            setOnClickListener {
                dialog.dismiss()
                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, buildString { appendLine("Соц.сети — $ownerName:"); networks.forEach { n -> appendLine("${n.type}: ${n.username}") } }.trim()) }, "Добавить в заметки"))
            }
        })
        dialog.setView(content); dialog.show()
    }

    // ── QR ─────────────────────────────────────────────────────────────────
    private fun generateQrCode(data: String, size: Int = 512): Bitmap {
        val bits = MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.CHARACTER_SET to "UTF-8"))
        val bmp  = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) for (y in 0 until size) bmp.setPixel(x, y, if (bits[x, y]) Color.BLACK else Color.WHITE)
        return bmp
    }

    private fun showQrDialog(qrBitmap: Bitmap, title: String = "QR-код") {
        val dialog = Dialog(this); val view = layoutInflater.inflate(R.layout.dialog_qr_code, null)
        view.findViewById<TextView>(R.id.qr_title).text = title
        view.findViewById<ImageView>(R.id.qrCodeImageView).setImageBitmap(qrBitmap)
        view.findViewById<MaterialButton>(R.id.closeButton).setOnClickListener { dialog.dismiss() }
        dialog.setContentView(view); dialog.show()
    }
}
