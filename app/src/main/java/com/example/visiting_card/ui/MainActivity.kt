package com.example.visiting_card.ui

import android.app.Dialog
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.visiting_card.R
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_ABOUT
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_EMAIL
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_FULL_NAME
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
import kotlin.math.atan2

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
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

        // ── NFC setup ──────────────────────────────────────────────────────
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        nfcPendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        // ── Profile init ───────────────────────────────────────────────────
        val initialActiveId = ProfileManager.initIfNeeded(prefs)

        // ── Card state ─────────────────────────────────────────────────────
        val fullNameState        = mutableStateOf(prefs.getString(KEY_FULL_NAME, "") ?: "")
        val positionState        = mutableStateOf(prefs.getString(KEY_POSITION, "") ?: "")
        val phoneState           = mutableStateOf(prefs.getString(KEY_PHONE, "") ?: "")
        val aboutState           = mutableStateOf(prefs.getString(KEY_ABOUT, "") ?: "")
        val profileImageUriState = mutableStateOf(prefs.getString(KEY_PROFILE_IMAGE_URI, null))
        val showPositionState    = mutableStateOf(prefs.getBoolean(KEY_SHOW_POSITION, true))
        val showPhoneState       = mutableStateOf(prefs.getBoolean(KEY_SHOW_PHONE, true))
        val showLogoState        = mutableStateOf(prefs.getBoolean(KEY_SHOW_LOGO, true))
        val showSocialState      = mutableStateOf(prefs.getBoolean(KEY_SHOW_SOCIAL, false))
        val selectedSocialIdx    = mutableStateOf(prefs.getInt(KEY_SELECTED_SOCIAL_INDEX, -1))
        val isDarkTheme          = mutableStateOf(prefs.getBoolean(KEY_THEME_DARK, false))

        // ── Profile state ──────────────────────────────────────────────────
        val activeProfileIdState    = mutableStateOf(initialActiveId)
        val activeProfileLabelState = mutableStateOf(
            ProfileManager.getAllProfiles(prefs)
                .firstOrNull { it.id == initialActiveId }?.label ?: ProfileManager.DEFAULT_LABEL
        )
        val allProfilesState = mutableStateOf(ProfileManager.getAllProfiles(prefs))

        // ── Dialog state ───────────────────────────────────────────────────
        val showSettingsDialog   = mutableStateOf(false)
        val showAddProfileDialog = mutableStateOf(false)
        val addProfileNameInput  = mutableStateOf("")
        val showRenameDialogForId = mutableStateOf<String?>(null)
        val renameInput          = mutableStateOf("")
        val showDeleteConfirmId  = mutableStateOf<String?>(null)

        // ── Helper: reload all card states from prefs ──────────────────────
        val reloadFromPrefs: () -> Unit = {
            fullNameState.value        = prefs.getString(KEY_FULL_NAME, "") ?: ""
            positionState.value        = prefs.getString(KEY_POSITION, "") ?: ""
            phoneState.value           = prefs.getString(KEY_PHONE, "") ?: ""
            aboutState.value           = prefs.getString(KEY_ABOUT, "") ?: ""
            profileImageUriState.value = prefs.getString(KEY_PROFILE_IMAGE_URI, null)
            showPositionState.value    = prefs.getBoolean(KEY_SHOW_POSITION, true)
            showPhoneState.value       = prefs.getBoolean(KEY_SHOW_PHONE, true)
            showLogoState.value        = prefs.getBoolean(KEY_SHOW_LOGO, true)
            showSocialState.value      = prefs.getBoolean(KEY_SHOW_SOCIAL, false)
            selectedSocialIdx.value    = prefs.getInt(KEY_SELECTED_SOCIAL_INDEX, -1)
        }

        // ── Photo picker ───────────────────────────────────────────────────
        pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data ?: return@registerForActivityResult
                prefs.edit().putString(KEY_PROFILE_IMAGE_URI, uri.toString()).apply()
                profileImageUriState.value = uri.toString()
                ProfileManager.syncActiveFromMainPrefs(
                    prefs, activeProfileIdState.value, activeProfileLabelState.value
                )
                allProfilesState.value = ProfileManager.getAllProfiles(prefs)
            }
        }

        // ── Edit card info launcher ────────────────────────────────────────
        cardInfoLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                reloadFromPrefs()
                ProfileManager.syncActiveFromMainPrefs(
                    prefs, activeProfileIdState.value, activeProfileLabelState.value
                )
                allProfilesState.value = ProfileManager.getAllProfiles(prefs)
            }
        }

        // ── Social networks launcher ───────────────────────────────────────
        socialNetworksLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { _ ->
            // Sync social networks to active profile store
            ProfileManager.syncActiveFromMainPrefs(
                prefs, activeProfileIdState.value, activeProfileLabelState.value
            )
            allProfilesState.value = ProfileManager.getAllProfiles(prefs)
        }

        // First launch: open edit screen if name is empty
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
                val iconTint    = if (isDark)
                    androidx.compose.ui.graphics.Color.White
                else
                    androidx.compose.ui.graphics.Color.Black

                // ── Add profile dialog ─────────────────────────────────────
                if (showAddProfileDialog.value) {
                    AlertDialog(
                        onDismissRequest = {
                            showAddProfileDialog.value = false
                            addProfileNameInput.value  = ""
                        },
                        title = { Text("Новый профиль") },
                        text  = {
                            OutlinedTextField(
                                value         = addProfileNameInput.value,
                                onValueChange = { addProfileNameInput.value = it },
                                label         = { Text("Название") },
                                placeholder   = { Text("Работа, Личное, Фриланс…") },
                                singleLine    = true,
                                modifier      = Modifier.fillMaxWidth()
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val label = addProfileNameInput.value.trim()
                                        .ifEmpty { "Профиль ${allProfilesState.value.size + 1}" }
                                    val newProfile = ProfileManager.createAndSwitchTo(
                                        prefs,
                                        activeProfileIdState.value,
                                        activeProfileLabelState.value,
                                        label
                                    )
                                    activeProfileIdState.value    = newProfile.id
                                    activeProfileLabelState.value = newProfile.label
                                    allProfilesState.value        = ProfileManager.getAllProfiles(prefs)
                                    reloadFromPrefs()
                                    showAddProfileDialog.value = false
                                    addProfileNameInput.value  = ""
                                    scope.launch { drawerState.close() }
                                }
                            ) { Text("Создать") }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showAddProfileDialog.value = false
                                addProfileNameInput.value  = ""
                            }) { Text("Отмена") }
                        }
                    )
                }

                // ── Rename profile dialog ──────────────────────────────────
                showRenameDialogForId.value?.let { renameId ->
                    AlertDialog(
                        onDismissRequest = { showRenameDialogForId.value = null },
                        title = { Text("Переименовать профиль") },
                        text  = {
                            OutlinedTextField(
                                value         = renameInput.value,
                                onValueChange = { renameInput.value = it },
                                label         = { Text("Название") },
                                singleLine    = true,
                                modifier      = Modifier.fillMaxWidth()
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val newLabel = renameInput.value.trim()
                                    if (newLabel.isNotEmpty()) {
                                        ProfileManager.renameProfile(prefs, renameId, newLabel)
                                        if (renameId == activeProfileIdState.value) {
                                            activeProfileLabelState.value = newLabel
                                        }
                                        allProfilesState.value = ProfileManager.getAllProfiles(prefs)
                                    }
                                    showRenameDialogForId.value = null
                                }
                            ) { Text("Сохранить") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRenameDialogForId.value = null }) {
                                Text("Отмена")
                            }
                        }
                    )
                }

                // ── Delete confirmation dialog ─────────────────────────────
                showDeleteConfirmId.value?.let { deleteId ->
                    val profileToDelete = allProfilesState.value.firstOrNull { it.id == deleteId }
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirmId.value = null },
                        title = { Text("Удалить профиль?") },
                        text  = {
                            Text("Профиль «${profileToDelete?.label ?: ""}» и все его данные будут удалены.")
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val newActive = ProfileManager.deleteProfile(
                                        prefs, deleteId,
                                        activeProfileIdState.value,
                                        activeProfileLabelState.value
                                    )
                                    if (newActive != null) {
                                        activeProfileIdState.value    = newActive.id
                                        activeProfileLabelState.value = newActive.label
                                        reloadFromPrefs()
                                    }
                                    allProfilesState.value = ProfileManager.getAllProfiles(prefs)
                                    showDeleteConfirmId.value = null
                                }
                            ) { Text("Удалить", color = androidx.compose.ui.graphics.Color.Red) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirmId.value = null }) {
                                Text("Отмена")
                            }
                        }
                    )
                }

                // ── Settings dialog ────────────────────────────────────────
                if (showSettingsDialog.value) {
                    val scrollState = rememberScrollState()
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text("Настройки") },
                        text  = {
                            Column(modifier = Modifier.verticalScroll(scrollState)) {

                                // ── Theme ──────────────────────────────────
                                Text(
                                    "Тема оформления",
                                    style    = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier          = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            isDarkTheme.value = false
                                            prefs.edit().putBoolean(KEY_THEME_DARK, false).apply()
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    RadioButton(
                                        selected = !isDarkTheme.value,
                                        onClick  = {
                                            isDarkTheme.value = false
                                            prefs.edit().putBoolean(KEY_THEME_DARK, false).apply()
                                        }
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Светлая тема")
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier          = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            isDarkTheme.value = true
                                            prefs.edit().putBoolean(KEY_THEME_DARK, true).apply()
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    RadioButton(
                                        selected = isDarkTheme.value,
                                        onClick  = {
                                            isDarkTheme.value = true
                                            prefs.edit().putBoolean(KEY_THEME_DARK, true).apply()
                                        }
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Тёмная тема")
                                }

                                Spacer(Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(Modifier.height(10.dp))

                                // ── Visibility ─────────────────────────────
                                Text(
                                    "Отображение на визитке",
                                    style    = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )

                                @Composable
                                fun visRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier          = Modifier
                                            .fillMaxWidth()
                                            .clickable { onToggle(!checked) }
                                            .padding(vertical = 2.dp)
                                    ) {
                                        Checkbox(checked = checked, onCheckedChange = onToggle)
                                        Spacer(Modifier.width(4.dp))
                                        Text(label)
                                    }
                                }

                                visRow("Должность", showPositionState.value) { v ->
                                    showPositionState.value = v
                                    prefs.edit().putBoolean(KEY_SHOW_POSITION, v).apply()
                                }
                                visRow("Номер телефона", showPhoneState.value) { v ->
                                    showPhoneState.value = v
                                    prefs.edit().putBoolean(KEY_SHOW_PHONE, v).apply()
                                }
                                visRow("Логотип", showLogoState.value) { v ->
                                    showLogoState.value = v
                                    prefs.edit().putBoolean(KEY_SHOW_LOGO, v).apply()
                                }
                                visRow("Социальная сеть", showSocialState.value) { v ->
                                    showSocialState.value = v
                                    prefs.edit().putBoolean(KEY_SHOW_SOCIAL, v).apply()
                                }

                                val dialogNetworks = SocialNetworkUtils.loadNetworks(prefs, KEY_SOCIAL_NETWORKS)
                                if (showSocialState.value) {
                                    Spacer(Modifier.height(6.dp))
                                    if (dialogNetworks.isEmpty()) {
                                        Text(
                                            "Нет добавленных соц.сетей.\nДобавьте через меню «Социальные сети».",
                                            style    = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                                        )
                                    } else {
                                        Text(
                                            "Выберите соц.сеть для визитки:",
                                            style    = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                                        )
                                        dialogNetworks.forEachIndexed { idx, network ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier          = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedSocialIdx.value = idx
                                                        prefs.edit().putInt(KEY_SELECTED_SOCIAL_INDEX, idx).apply()
                                                    }
                                                    .padding(vertical = 2.dp, horizontal = 8.dp)
                                            ) {
                                                RadioButton(
                                                    selected = selectedSocialIdx.value == idx,
                                                    onClick  = {
                                                        selectedSocialIdx.value = idx
                                                        prefs.edit().putInt(KEY_SELECTED_SOCIAL_INDEX, idx).apply()
                                                    }
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text("${network.type}: ${network.username}")
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showSettingsDialog.value = false }) {
                                Text("Закрыть")
                            }
                        }
                    )
                }

                // ── Navigation Drawer ──────────────────────────────────────
                ModalNavigationDrawer(
                    drawerState   = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Spacer(Modifier.height(16.dp))

                            // ── Profiles section header ────────────────────
                            Text(
                                "ПРОФИЛИ",
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                                style    = MaterialTheme.typography.labelSmall,
                                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.5.sp
                            )

                            // ── Profile rows ───────────────────────────────
                            allProfilesState.value.forEach { profile ->
                                val isActive = profile.id == activeProfileIdState.value
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier          = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !isActive) {
                                            val switched = ProfileManager.switchTo(
                                                prefs,
                                                activeProfileIdState.value,
                                                activeProfileLabelState.value,
                                                profile.id
                                            )
                                            if (switched != null) {
                                                activeProfileIdState.value    = switched.id
                                                activeProfileLabelState.value = switched.label
                                                allProfilesState.value        = ProfileManager.getAllProfiles(prefs)
                                                reloadFromPrefs()
                                                scope.launch { drawerState.close() }
                                            }
                                        }
                                        .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                                ) {
                                    // Active checkmark or spacer
                                    if (isActive) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Активный профиль",
                                            modifier = Modifier.size(18.dp),
                                            tint     = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Spacer(Modifier.size(18.dp))
                                    }

                                    Spacer(Modifier.width(10.dp))

                                    // Profile label
                                    Text(
                                        text       = profile.label,
                                        modifier   = Modifier.weight(1f),
                                        style      = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                                    )

                                    // Rename button
                                    IconButton(
                                        onClick  = {
                                            renameInput.value          = profile.label
                                            showRenameDialogForId.value = profile.id
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Переименовать",
                                            modifier = Modifier.size(16.dp),
                                            tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }

                                    // Delete button (hidden if only 1 profile)
                                    if (allProfilesState.value.size > 1) {
                                        IconButton(
                                            onClick  = { showDeleteConfirmId.value = profile.id },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Удалить профиль",
                                                modifier = Modifier.size(16.dp),
                                                tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                            )
                                        }
                                    }
                                }
                            }

                            // ── Add profile ────────────────────────────────
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier          = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        addProfileNameInput.value  = ""
                                        showAddProfileDialog.value = true
                                    }
                                    .padding(start = 12.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Добавить профиль",
                                    modifier = Modifier.size(18.dp),
                                    tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Добавить профиль",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                            // ── Menu items ─────────────────────────────────
                            Text(
                                "Информация на визитке",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        cardInfoLauncher.launch(
                                            Intent(this@MainActivity, EditCardInfoActivity::class.java)
                                        )
                                        scope.launch { drawerState.close() }
                                    }
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                            Text(
                                "Социальные сети",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        socialNetworksLauncher.launch(
                                            Intent(this@MainActivity, AddSocialNetworksActivity::class.java)
                                        )
                                        scope.launch { drawerState.close() }
                                    }
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                            Text(
                                "Настройки",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showSettingsDialog.value = true
                                        scope.launch { drawerState.close() }
                                    }
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory  = { ctx ->
                                val root = layoutInflater.inflate(R.layout.activity_main, null)

                                // ── Bottom sheet ───────────────────────────
                                val bottomSheet = root.findViewById<View>(R.id.bottomSheet)
                                val peekPx      = (60 * ctx.resources.displayMetrics.density).toInt()
                                bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet).apply {
                                    state            = BottomSheetBehavior.STATE_COLLAPSED
                                    peekHeight       = peekPx
                                    isHideable       = false
                                    halfExpandedRatio = 0.45f
                                }

                                // ── NFC button ─────────────────────────────
                                val nfcBtn = root.findViewById<MaterialButton>(R.id.share_nfc_button)
                                if (nfcAdapter != null) nfcBtn.visibility = View.VISIBLE
                                nfcBtn.setOnClickListener {
                                    when {
                                        nfcAdapter == null ->
                                            Toast.makeText(this@MainActivity, "NFC не поддерживается на этом устройстве", Toast.LENGTH_LONG).show()
                                        !nfcAdapter!!.isEnabled ->
                                            Toast.makeText(this@MainActivity, "Включите NFC в настройках устройства", Toast.LENGTH_LONG).show()
                                        else -> showNfcWriteDialog()
                                    }
                                }

                                // ── Business card: pinch/rotate/tap ───────
                                val businessCard = root.findViewById<CardView>(R.id.business_card)
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
                                            businessCard.scaleX = scaleFactor
                                            businessCard.scaleY = scaleFactor
                                            return true
                                        }
                                    })

                                val gestureDetector = GestureDetector(ctx,
                                    object : GestureDetector.SimpleOnGestureListener() {
                                        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                                            bottomSheetBehavior?.let { bsb ->
                                                bsb.state = if (bsb.state == BottomSheetBehavior.STATE_COLLAPSED)
                                                    BottomSheetBehavior.STATE_HALF_EXPANDED
                                                else
                                                    BottomSheetBehavior.STATE_COLLAPSED
                                            }
                                            return true
                                        }
                                    })

                                businessCard.setOnTouchListener { v, event ->
                                    scaleDetector.onTouchEvent(event)
                                    gestureDetector.onTouchEvent(event)
                                    when (event.actionMasked) {
                                        MotionEvent.ACTION_DOWN ->
                                            v.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                                        MotionEvent.ACTION_POINTER_DOWN ->
                                            if (event.pointerCount == 2) prevAngle = fingerAngle(event)
                                        MotionEvent.ACTION_MOVE ->
                                            if (event.pointerCount >= 2 && !prevAngle.isNaN()) {
                                                val angle = fingerAngle(event)
                                                val delta = angle - prevAngle
                                                if (delta in -30f..30f) {
                                                    cardRotation = (cardRotation + delta).coerceIn(-35f, 35f)
                                                    v.rotation = cardRotation
                                                }
                                                prevAngle = angle
                                            }
                                        MotionEvent.ACTION_POINTER_UP ->
                                            prevAngle = Float.NaN
                                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                            prevAngle = Float.NaN
                                            val needsReset = scaleFactor != 1f || cardRotation != 0f
                                            scaleFactor  = 1f
                                            cardRotation = 0f
                                            v.animate()
                                                .scaleX(1f).scaleY(1f).rotation(0f)
                                                .setDuration(if (needsReset) 300L else 0L)
                                                .setInterpolator(DecelerateInterpolator())
                                                .withEndAction { v.setLayerType(View.LAYER_TYPE_NONE, null) }
                                                .start()
                                        }
                                    }
                                    true
                                }

                                // ── Phone → QR vCard ──────────────────────
                                root.findViewById<TextView>(R.id.phone_number).setOnClickListener {
                                    val name  = root.findViewById<TextView>(R.id.full_name).text.toString()
                                    val phone = root.findViewById<TextView>(R.id.phone_number).text.toString()
                                    showQrDialog(
                                        generateQrCode("BEGIN:VCARD\nVERSION:3.0\nFN:$name\nTEL:$phone\nEND:VCARD"),
                                        "Поделиться номером"
                                    )
                                }

                                // ── Social info → QR ──────────────────────
                                root.findViewById<TextView>(R.id.telegram_info).setOnClickListener {
                                    val networks = SocialNetworkUtils.loadNetworks(prefs, KEY_SOCIAL_NETWORKS)
                                    val idx      = prefs.getInt(KEY_SELECTED_SOCIAL_INDEX, -1)
                                    if (idx >= 0 && idx < networks.size) {
                                        val url = SocialNetworkUtils.getNetworkUrl(
                                            networks[idx].type, networks[idx].username
                                        )
                                        showQrDialog(generateQrCode(url), networks[idx].type)
                                    }
                                }

                                // ── Photo → gallery ────────────────────────
                                root.findViewById<ImageView>(R.id.photo).setOnClickListener {
                                    pickImageLauncher.launch(
                                        Intent(Intent.ACTION_PICK).apply { type = "image/*" }
                                    )
                                }

                                // ── Share text ─────────────────────────────
                                root.findViewById<MaterialButton>(R.id.share_button).setOnClickListener {
                                    val name  = root.findViewById<TextView>(R.id.full_name).text
                                    val pos   = root.findViewById<TextView>(R.id.position).text
                                    val phone = root.findViewById<TextView>(R.id.phone_number).text
                                    val about = root.findViewById<TextView>(R.id.about).text
                                    val shareText = buildString {
                                        appendLine("👤 $name")
                                        if (pos.isNotEmpty())   appendLine("💼 $pos")
                                        if (phone.isNotEmpty()) appendLine("📞 $phone")
                                        if (about.isNotEmpty()) append("ℹ️ $about")
                                    }
                                    startActivity(Intent.createChooser(
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareText.trim())
                                        }, "Поделиться через"
                                    ))
                                }

                                // ── Share phone QR ─────────────────────────
                                root.findViewById<MaterialButton>(R.id.share_phone_button).setOnClickListener {
                                    val name  = root.findViewById<TextView>(R.id.full_name).text.toString()
                                    val phone = root.findViewById<TextView>(R.id.phone_number).text.toString()
                                    showQrDialog(
                                        generateQrCode("BEGIN:VCARD\nVERSION:3.0\nFN:$name\nTEL:$phone\nEND:VCARD"),
                                        "Поделиться номером"
                                    )
                                }

                                // ── Share email QR ─────────────────────────
                                root.findViewById<MaterialButton>(R.id.share_email_button).setOnClickListener {
                                    val email = prefs.getString(KEY_EMAIL, "") ?: ""
                                    if (email.isBlank()) return@setOnClickListener
                                    showQrDialog(generateQrCode("mailto:$email"), "Поделиться email")
                                }

                                // ── Share social networks ──────────────────
                                root.findViewById<MaterialButton>(R.id.share_social_button).setOnClickListener {
                                    val name = root.findViewById<TextView>(R.id.full_name).text.toString()
                                    showSocialShareDialog(name)
                                }

                                root
                            },
                            update = { root ->
                                val dark = isDarkTheme.value

                                root.setBackgroundColor(
                                    if (dark) Color.parseColor("#121212") else Color.WHITE
                                )
                                root.findViewById<View>(R.id.bottomSheet).setBackgroundResource(
                                    if (dark) R.drawable.bottom_sheet_background_dark
                                    else R.drawable.bottom_sheet_background
                                )
                                root.findViewById<CardView>(R.id.business_card)
                                    .setCardBackgroundColor(
                                        if (dark) Color.parseColor("#1E1E1E") else Color.WHITE
                                    )

                                val cardPrimary   = if (dark) Color.WHITE else Color.BLACK
                                val cardSecondary = if (dark) Color.parseColor("#BBBBBB")
                                                   else Color.parseColor("#555555")
                                root.findViewById<TextView>(R.id.full_name).setTextColor(cardPrimary)
                                root.findViewById<TextView>(R.id.position).setTextColor(cardSecondary)
                                root.findViewById<TextView>(R.id.phone_number).setTextColor(cardPrimary)
                                root.findViewById<TextView>(R.id.telegram_info).setTextColor(cardPrimary)

                                val sheetHint = if (dark) Color.parseColor("#AAAAAA")
                                               else Color.parseColor("#777777")
                                root.findViewById<TextView>(R.id.about).setTextColor(sheetHint)

                                val btnBg   = if (dark) Color.WHITE else Color.BLACK
                                val btnText = if (dark) Color.BLACK else Color.WHITE
                                val btnTint = android.content.res.ColorStateList.valueOf(btnBg)
                                listOf(
                                    R.id.share_button,
                                    R.id.share_phone_button,
                                    R.id.share_email_button,
                                    R.id.share_social_button,
                                    R.id.share_nfc_button
                                ).forEach { id ->
                                    root.findViewById<MaterialButton>(id).also { btn ->
                                        btn.backgroundTintList = btnTint
                                        btn.setTextColor(btnText)
                                    }
                                }

                                root.findViewById<ImageView>(R.id.logo).setImageResource(
                                    if (dark) R.drawable.logo_for_dark_theme
                                    else R.drawable.logo_for_light_theme
                                )

                                // ── Data ──────────────────────────────────
                                root.findViewById<TextView>(R.id.full_name).text = fullNameState.value
                                root.findViewById<TextView>(R.id.about).text     = aboutState.value

                                // ── Visibility ─────────────────────────────
                                val posView    = root.findViewById<TextView>(R.id.position)
                                val phoneView  = root.findViewById<TextView>(R.id.phone_number)
                                val logoView   = root.findViewById<ImageView>(R.id.logo)
                                val socialView = root.findViewById<TextView>(R.id.telegram_info)

                                posView.text       = positionState.value
                                posView.visibility = if (showPositionState.value) View.VISIBLE else View.GONE

                                phoneView.text       = phoneState.value
                                phoneView.visibility = if (showPhoneState.value) View.VISIBLE else View.GONE

                                logoView.visibility = if (showLogoState.value) View.VISIBLE else View.GONE

                                if (showSocialState.value) {
                                    val networks = SocialNetworkUtils.loadNetworks(prefs, KEY_SOCIAL_NETWORKS)
                                    val idx      = selectedSocialIdx.value
                                    if (idx >= 0 && idx < networks.size) {
                                        socialView.text       = "${networks[idx].type}: ${networks[idx].username}"
                                        socialView.visibility = View.VISIBLE
                                    } else {
                                        socialView.visibility = View.GONE
                                    }
                                } else {
                                    socialView.visibility = View.GONE
                                }

                                val uriStr = profileImageUriState.value
                                if (uriStr != null) {
                                    root.findViewById<ImageView>(R.id.photo).setImageURI(Uri.parse(uriStr))
                                } else {
                                    root.findViewById<ImageView>(R.id.photo).setImageResource(R.drawable.photo)
                                }
                            }
                        )

                        // Floating hamburger
                        IconButton(
                            onClick  = { scope.launch { drawerState.open() } },
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(top = 28.dp, start = 4.dp)
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Меню", tint = iconTint)
                        }
                    }
                }
            }
        }
    }

    // ── NFC lifecycle ──────────────────────────────────────────────────────
    override fun onResume() {
        super.onResume()
        if (nfcWriteMode) {
            nfcAdapter?.enableForegroundDispatch(this, nfcPendingIntent, null, null)
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (nfcWriteMode &&
            (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action ||
             NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action ||
             NfcAdapter.ACTION_TECH_DISCOVERED == intent.action)
        ) {
            val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }
            tag?.let { writeNdefToTag(it) }
        }
    }

    // ── NFC helpers ────────────────────────────────────────────────────────
    private fun buildVCard(): String {
        val prefs    = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val name     = prefs.getString(KEY_FULL_NAME, "") ?: ""
        val phone    = prefs.getString(KEY_PHONE, "") ?: ""
        val email    = prefs.getString(KEY_EMAIL, "") ?: ""
        val position = prefs.getString(KEY_POSITION, "") ?: ""
        val networks = SocialNetworkUtils.loadNetworks(prefs, KEY_SOCIAL_NETWORKS)
        return buildString {
            appendLine("BEGIN:VCARD")
            appendLine("VERSION:3.0")
            if (name.isNotBlank())     appendLine("FN:$name")
            if (phone.isNotBlank())    appendLine("TEL:$phone")
            if (email.isNotBlank())    appendLine("EMAIL:$email")
            if (position.isNotBlank()) appendLine("TITLE:$position")
            if (networks.isNotEmpty()) {
                appendLine("NOTE:${networks.joinToString(", ") { "${it.type}: ${it.username}" }}")
            }
            append("END:VCARD")
        }
    }

    private fun showNfcWriteDialog() {
        nfcWriteMode = true
        nfcAdapter?.enableForegroundDispatch(this, nfcPendingIntent, null, null)
        nfcWriteDialog = android.app.AlertDialog.Builder(this)
            .setTitle("Запись на NFC-метку")
            .setMessage(
                "Поднесите телефон к NFC-метке (наклейке или карточке).\n\n" +
                "Контакт будет записан в формате vCard — любой телефон считает его без приложения."
            )
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
                nfcWriteMode = false
                nfcAdapter?.disableForegroundDispatch(this)
            }
            .setCancelable(false)
            .show()
    }

    private fun writeNdefToTag(tag: Tag) {
        val ndefMessage = NdefMessage(arrayOf(
            NdefRecord.createMime("text/x-vCard", buildVCard().toByteArray(Charsets.UTF_8))
        ))
        try {
            val ndef           = Ndef.get(tag)
            val ndefFormatable = NdefFormatable.get(tag)
            when {
                ndef != null -> {
                    ndef.connect()
                    when {
                        !ndef.isWritable ->
                            runOnUiThread { Toast.makeText(this, "NFC-метка защищена от записи", Toast.LENGTH_LONG).show() }
                        ndef.maxSize < ndefMessage.byteArrayLength ->
                            runOnUiThread { Toast.makeText(this, "NFC-метка слишком маленькая для контакта", Toast.LENGTH_LONG).show() }
                        else -> {
                            ndef.writeNdefMessage(ndefMessage)
                            runOnUiThread {
                                nfcWriteDialog?.dismiss()
                                nfcWriteMode = false
                                nfcAdapter?.disableForegroundDispatch(this)
                                Toast.makeText(this, "✓ Контакт записан на NFC-метку!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    ndef.close()
                }
                ndefFormatable != null -> {
                    ndefFormatable.connect()
                    ndefFormatable.format(ndefMessage)
                    ndefFormatable.close()
                    runOnUiThread {
                        nfcWriteDialog?.dismiss()
                        nfcWriteMode = false
                        nfcAdapter?.disableForegroundDispatch(this)
                        Toast.makeText(this, "✓ Контакт записан на NFC-метку!", Toast.LENGTH_SHORT).show()
                    }
                }
                else -> runOnUiThread {
                    Toast.makeText(this, "Эта NFC-метка не поддерживает запись", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this, "Ошибка записи: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ── Social sharing dialog ──────────────────────────────────────────────
    private fun showSocialShareDialog(ownerName: String) {
        val prefs    = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val networks = SocialNetworkUtils.loadNetworks(prefs, KEY_SOCIAL_NETWORKS)

        if (networks.isEmpty()) {
            android.app.AlertDialog.Builder(this)
                .setTitle("Социальные сети")
                .setMessage("Нет добавленных социальных сетей.\n\nОткройте меню «Социальные сети».")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val dialog  = android.app.AlertDialog.Builder(this).setTitle("Выберите соц.сеть").create()
        val density = resources.displayMetrics.density
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16 * density).toInt(), 8, (16 * density).toInt(), 8)
        }

        networks.forEach { network ->
            content.addView(MaterialButton(this).apply {
                text               = "${network.type}: ${network.username}"
                setTextColor(Color.WHITE)
                backgroundTintList = android.content.res.ColorStateList.valueOf(Color.BLACK)
                cornerRadius       = (24 * density).toInt()
                layoutParams       = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = (10 * density).toInt() }
                setOnClickListener {
                    dialog.dismiss()
                    showQrDialog(
                        generateQrCode(SocialNetworkUtils.getNetworkUrl(network.type, network.username)),
                        network.type
                    )
                }
            })
        }

        content.addView(MaterialButton(this).apply {
            text               = "Поделиться всеми соц.сетями"
            setTextColor(Color.BLACK)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            strokeColor        = android.content.res.ColorStateList.valueOf(Color.BLACK)
            strokeWidth        = (1 * density).toInt()
            cornerRadius       = (24 * density).toInt()
            layoutParams       = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = (6 * density).toInt() }
            setOnClickListener {
                dialog.dismiss()
                val text = buildString {
                    appendLine("Социальные сети — $ownerName:")
                    networks.forEach { n -> appendLine("${n.type}: ${n.username}") }
                }.trim()
                startActivity(Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                        putExtra(Intent.EXTRA_TITLE, "Социальные сети")
                    }, "Добавить в заметки"
                ))
            }
        })

        dialog.setView(content)
        dialog.show()
    }

    // ── QR generation ─────────────────────────────────────────────────────
    private fun generateQrCode(data: String, size: Int = 512): Bitmap {
        val hints     = mapOf(EncodeHintType.CHARACTER_SET to "UTF-8")
        val bitMatrix: BitMatrix = MultiFormatWriter()
            .encode(data, BarcodeFormat.QR_CODE, size, size, hints)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }

    private fun showQrDialog(qrBitmap: Bitmap, title: String = "QR-код") {
        val dialog = Dialog(this)
        val view   = layoutInflater.inflate(R.layout.dialog_qr_code, null)
        view.findViewById<TextView>(R.id.qr_title).text = title
        view.findViewById<ImageView>(R.id.qrCodeImageView).setImageBitmap(qrBitmap)
        view.findViewById<MaterialButton>(R.id.closeButton).setOnClickListener { dialog.dismiss() }
        dialog.setContentView(view)
        dialog.show()
    }
}
