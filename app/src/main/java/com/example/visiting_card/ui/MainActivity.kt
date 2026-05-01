package com.example.visiting_card.ui

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var cardInfoLauncher: ActivityResultLauncher<Intent>
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        // ── Compose state ──────────────────────────────────────────────────
        val fullNameState         = mutableStateOf(prefs.getString(KEY_FULL_NAME, "") ?: "")
        val positionState         = mutableStateOf(prefs.getString(KEY_POSITION, "") ?: "")
        val phoneState            = mutableStateOf(prefs.getString(KEY_PHONE, "") ?: "")
        val aboutState            = mutableStateOf(prefs.getString(KEY_ABOUT, "") ?: "")
        val profileImageUriState  = mutableStateOf(prefs.getString(KEY_PROFILE_IMAGE_URI, null))
        val showPositionState     = mutableStateOf(prefs.getBoolean(KEY_SHOW_POSITION, true))
        val showPhoneState        = mutableStateOf(prefs.getBoolean(KEY_SHOW_PHONE, true))
        val showLogoState         = mutableStateOf(prefs.getBoolean(KEY_SHOW_LOGO, true))
        val showSocialState       = mutableStateOf(prefs.getBoolean(KEY_SHOW_SOCIAL, false))
        val selectedSocialIdx     = mutableStateOf(prefs.getInt(KEY_SELECTED_SOCIAL_INDEX, -1))
        val isDarkTheme           = mutableStateOf(prefs.getBoolean(KEY_THEME_DARK, false))
        val showSettingsDialog    = mutableStateOf(false)

        // ── Photo picker ───────────────────────────────────────────────────
        pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data ?: return@registerForActivityResult
                prefs.edit().putString(KEY_PROFILE_IMAGE_URI, uri.toString()).apply()
                profileImageUriState.value = uri.toString()
            }
        }

        // ── Edit card info launcher ────────────────────────────────────────
        cardInfoLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
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
        }

        // First launch: open edit screen if name is empty
        if (prefs.getString(KEY_FULL_NAME, "").isNullOrEmpty()) {
            cardInfoLauncher.launch(Intent(this, EditCardInfoActivity::class.java))
        }

        setContent {
            val darkColors = darkColorScheme(
                primary       = androidx.compose.ui.graphics.Color.White,
                onPrimary     = androidx.compose.ui.graphics.Color.Black,
                background    = androidx.compose.ui.graphics.Color(0xFF121212),
                surface       = androidx.compose.ui.graphics.Color(0xFF1E1E1E),
                onBackground  = androidx.compose.ui.graphics.Color.White,
                onSurface     = androidx.compose.ui.graphics.Color.White
            )
            val lightColors = lightColorScheme(
                primary       = androidx.compose.ui.graphics.Color.Black,
                onPrimary     = androidx.compose.ui.graphics.Color.White,
                background    = androidx.compose.ui.graphics.Color.White,
                surface       = androidx.compose.ui.graphics.Color.White,
                onBackground  = androidx.compose.ui.graphics.Color.Black,
                onSurface     = androidx.compose.ui.graphics.Color.Black
            )

            MaterialTheme(colorScheme = if (isDarkTheme.value) darkColors else lightColors) {
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scope       = rememberCoroutineScope()
                val isDark      = isDarkTheme.value
                val iconTint    = if (isDark)
                    androidx.compose.ui.graphics.Color.White
                else
                    androidx.compose.ui.graphics.Color.Black

                // ── Settings dialog ────────────────────────────────────────
                if (showSettingsDialog.value) {
                    AlertDialog(
                        onDismissRequest = { showSettingsDialog.value = false },
                        title = { Text("Настройки") },
                        text = {
                            Column {
                                Text(
                                    "Тема оформления",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            isDarkTheme.value = false
                                            prefs.edit().putBoolean(KEY_THEME_DARK, false).apply()
                                        }
                                        .padding(vertical = 6.dp)
                                ) {
                                    RadioButton(
                                        selected = !isDarkTheme.value,
                                        onClick  = {
                                            isDarkTheme.value = false
                                            prefs.edit().putBoolean(KEY_THEME_DARK, false).apply()
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Светлая тема")
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            isDarkTheme.value = true
                                            prefs.edit().putBoolean(KEY_THEME_DARK, true).apply()
                                        }
                                        .padding(vertical = 6.dp)
                                ) {
                                    RadioButton(
                                        selected = isDarkTheme.value,
                                        onClick  = {
                                            isDarkTheme.value = true
                                            prefs.edit().putBoolean(KEY_THEME_DARK, true).apply()
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Тёмная тема")
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

                ModalNavigationDrawer(
                    drawerState   = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Spacer(modifier = Modifier.height(16.dp))

                            // Edit card info
                            Text(
                                "Изменить информацию на визитке",
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

                            // Add social networks
                            Text(
                                "Добавить соц.сети",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        startActivity(
                                            Intent(this@MainActivity, AddSocialNetworksActivity::class.java)
                                        )
                                        scope.launch { drawerState.close() }
                                    }
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                            // Settings (theme)
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

                                // ── Bottom sheet setup ─────────────────────
                                val bottomSheet = root.findViewById<View>(R.id.bottomSheet)
                                val peekPx = (60 * ctx.resources.displayMetrics.density).toInt()
                                bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet).apply {
                                    state            = BottomSheetBehavior.STATE_COLLAPSED
                                    peekHeight       = peekPx
                                    isHideable       = false
                                    halfExpandedRatio = 0.45f
                                }

                                // ── Business card: pinch-to-zoom + tap ─────
                                val businessCard = root.findViewById<CardView>(R.id.business_card)
                                var scaleFactor  = 1f

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
                                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                            if (scaleFactor != 1f) {
                                                scaleFactor = 1f
                                                v.animate().scaleX(1f).scaleY(1f).setDuration(250).start()
                                            }
                                        }
                                    }
                                    true
                                }

                                // ── Phone number → QR (add to contacts) ───
                                root.findViewById<TextView>(R.id.phone_number).setOnClickListener {
                                    val name  = root.findViewById<TextView>(R.id.full_name).text.toString()
                                    val phone = root.findViewById<TextView>(R.id.phone_number).text.toString()
                                    val vCard = "BEGIN:VCARD\nVERSION:3.0\nFN:$name\nTEL:$phone\nEND:VCARD"
                                    showQrDialog(generateQrCode(vCard), "Поделиться номером")
                                }

                                // ── Telegram/social info → open link ───────
                                root.findViewById<TextView>(R.id.telegram_info).setOnClickListener {
                                    val networks = SocialNetworkUtils.loadNetworks(prefs, KEY_SOCIAL_NETWORKS)
                                    val idx      = prefs.getInt(KEY_SELECTED_SOCIAL_INDEX, -1)
                                    if (idx >= 0 && idx < networks.size) {
                                        val url = SocialNetworkUtils.getNetworkUrl(
                                            networks[idx].type, networks[idx].username
                                        )
                                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                                }

                                // ── Photo → pick from gallery ──────────────
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
                                        if (pos.isNotEmpty()) appendLine("💼 $pos")
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

                                // ── Share phone → QR vCard ────────────────
                                root.findViewById<MaterialButton>(R.id.share_phone_button).setOnClickListener {
                                    val name  = root.findViewById<TextView>(R.id.full_name).text.toString()
                                    val phone = root.findViewById<TextView>(R.id.phone_number).text.toString()
                                    val vCard = "BEGIN:VCARD\nVERSION:3.0\nFN:$name\nTEL:$phone\nEND:VCARD"
                                    showQrDialog(generateQrCode(vCard), "Поделиться номером")
                                }

                                // ── Share email → QR mailto ────────────────
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

                                // Background
                                root.setBackgroundColor(
                                    if (dark) Color.parseColor("#121212") else Color.WHITE
                                )

                                // Bottom sheet background
                                root.findViewById<View>(R.id.bottomSheet).setBackgroundResource(
                                    if (dark) R.drawable.bottom_sheet_background_dark
                                    else R.drawable.bottom_sheet_background
                                )

                                // Card background
                                root.findViewById<CardView>(R.id.business_card)
                                    .setCardBackgroundColor(
                                        if (dark) Color.parseColor("#1E1E1E") else Color.WHITE
                                    )

                                // Card text colors
                                val cardPrimary   = if (dark) Color.WHITE else Color.BLACK
                                val cardSecondary = if (dark) Color.parseColor("#BBBBBB")
                                else Color.parseColor("#555555")

                                root.findViewById<TextView>(R.id.full_name).setTextColor(cardPrimary)
                                root.findViewById<TextView>(R.id.position).setTextColor(cardSecondary)
                                root.findViewById<TextView>(R.id.phone_number).setTextColor(cardPrimary)
                                root.findViewById<TextView>(R.id.telegram_info).setTextColor(cardPrimary)

                                // Bottom sheet text colors
                                val sheetHint = if (dark) Color.parseColor("#AAAAAA")
                                else Color.parseColor("#777777")
                                root.findViewById<TextView>(R.id.about).setTextColor(sheetHint)

                                // Button tints
                                val btnBg   = if (dark) Color.WHITE else Color.BLACK
                                val btnText = if (dark) Color.BLACK else Color.WHITE
                                val btnTint = android.content.res.ColorStateList.valueOf(btnBg)
                                listOf(
                                    R.id.share_button,
                                    R.id.share_phone_button,
                                    R.id.share_email_button,
                                    R.id.share_social_button
                                ).forEach { id ->
                                    root.findViewById<MaterialButton>(id).also { btn ->
                                        btn.backgroundTintList = btnTint
                                        btn.setTextColor(btnText)
                                    }
                                }

                                // Logo
                                root.findViewById<ImageView>(R.id.logo).setImageResource(
                                    if (dark) R.drawable.logo_for_dark_theme
                                    else R.drawable.logo_for_light_theme
                                )

                                // ── Data ──────────────────────────────────
                                root.findViewById<TextView>(R.id.full_name).text = fullNameState.value
                                root.findViewById<TextView>(R.id.about).text     = aboutState.value

                                // ── Visibility toggles ─────────────────────
                                val posView   = root.findViewById<TextView>(R.id.position)
                                val phoneView = root.findViewById<TextView>(R.id.phone_number)
                                val logoView  = root.findViewById<ImageView>(R.id.logo)
                                val socialView = root.findViewById<TextView>(R.id.telegram_info)

                                posView.text = positionState.value
                                posView.visibility = if (showPositionState.value) View.VISIBLE else View.GONE

                                phoneView.text = phoneState.value
                                phoneView.visibility = if (showPhoneState.value) View.VISIBLE else View.GONE

                                logoView.visibility = if (showLogoState.value) View.VISIBLE else View.GONE

                                if (showSocialState.value) {
                                    val networks = SocialNetworkUtils.loadNetworks(prefs, KEY_SOCIAL_NETWORKS)
                                    val idx = selectedSocialIdx.value
                                    if (idx >= 0 && idx < networks.size) {
                                        socialView.text = "${networks[idx].type}: ${networks[idx].username}"
                                        socialView.visibility = View.VISIBLE
                                    } else {
                                        socialView.visibility = View.GONE
                                    }
                                } else {
                                    socialView.visibility = View.GONE
                                }

                                // Profile photo
                                val uriStr = profileImageUriState.value
                                if (uriStr != null) {
                                    root.findViewById<ImageView>(R.id.photo)
                                        .setImageURI(Uri.parse(uriStr))
                                }
                            }
                        )

                        // Floating hamburger — offset below status bar
                        IconButton(
                            onClick  = { scope.launch { drawerState.open() } },
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(top = 28.dp, start = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Меню",
                                tint = iconTint
                            )
                        }
                    }
                }
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
                .setMessage("Нет добавленных социальных сетей.\n\nОткройте меню «Добавить соц.сети».")
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
                text = "${network.type}: ${network.username}"
                setTextColor(Color.WHITE)
                backgroundTintList = android.content.res.ColorStateList.valueOf(Color.BLACK)
                cornerRadius = (24 * density).toInt()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = (10 * density).toInt() }
                setOnClickListener {
                    dialog.dismiss()
                    val url = SocialNetworkUtils.getNetworkUrl(network.type, network.username)
                    showQrDialog(generateQrCode(url), network.type)
                }
            })
        }

        content.addView(MaterialButton(this).apply {
            text  = "Поделиться всеми соц.сетями"
            setTextColor(Color.BLACK)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            strokeColor  = android.content.res.ColorStateList.valueOf(Color.BLACK)
            strokeWidth  = (1 * density).toInt()
            cornerRadius = (24 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = (6 * density).toInt() }
            setOnClickListener {
                dialog.dismiss()
                val qrText = SocialNetworkUtils.buildAllNetworksQrText(ownerName, networks)
                showQrDialog(generateQrCode(qrText), "Все соц.сети")
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
