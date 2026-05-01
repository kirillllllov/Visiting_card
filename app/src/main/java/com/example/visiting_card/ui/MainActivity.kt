package com.example.visiting_card.ui

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.visiting_card.R
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_ABOUT
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_EMAIL
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_FULL_NAME
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_PHONE
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_POSITION
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_PROFILE_IMAGE_URI
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_TELEGRAM
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
    private lateinit var editedDataLauncher: ActivityResultLauncher<Intent>
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        val fullNameState = mutableStateOf(sharedPreferences.getString(KEY_FULL_NAME, "") ?: "")
        val positionState = mutableStateOf(sharedPreferences.getString(KEY_POSITION, "") ?: "")
        val phoneState = mutableStateOf(sharedPreferences.getString(KEY_PHONE, "") ?: "")
        val emailState = mutableStateOf(sharedPreferences.getString(KEY_EMAIL, "") ?: "")
        val telegramState = mutableStateOf(sharedPreferences.getString(KEY_TELEGRAM, "") ?: "")
        val aboutState = mutableStateOf(sharedPreferences.getString(KEY_ABOUT, "") ?: "")
        val profileImageUriState = mutableStateOf(sharedPreferences.getString(KEY_PROFILE_IMAGE_URI, null))

        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val imageUri = result.data?.data ?: return@registerForActivityResult
                sharedPreferences.edit()
                    .putString(KEY_PROFILE_IMAGE_URI, imageUri.toString())
                    .apply()
                profileImageUriState.value = imageUri.toString()
            }
        }

        editedDataLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data ?: return@registerForActivityResult
                fullNameState.value = data.getStringExtra(KEY_FULL_NAME) ?: fullNameState.value
                positionState.value = data.getStringExtra(KEY_POSITION) ?: positionState.value
                phoneState.value = data.getStringExtra(KEY_PHONE) ?: phoneState.value
                emailState.value = data.getStringExtra(KEY_EMAIL) ?: emailState.value
                telegramState.value = data.getStringExtra(KEY_TELEGRAM) ?: telegramState.value
                aboutState.value = data.getStringExtra(KEY_ABOUT) ?: aboutState.value
                val imageUriString = data.getStringExtra(KEY_PROFILE_IMAGE_URI)
                if (imageUriString != null) {
                    profileImageUriState.value = imageUriString
                }
            }
        }

        // On first launch (name not set), immediately open the edit screen
        val isFirstLaunch = sharedPreferences.getString(KEY_FULL_NAME, "").isNullOrEmpty()
        if (isFirstLaunch) {
            editedDataLauncher.launch(Intent(this, EditDataActivity::class.java))
        }

        val isDarkTheme = mutableStateOf(false)

        setContent {
            val darkColors = darkColorScheme(
                primary = androidx.compose.ui.graphics.Color.White,
                onPrimary = androidx.compose.ui.graphics.Color.Black,
                background = androidx.compose.ui.graphics.Color(0xFF121212),
                surface = androidx.compose.ui.graphics.Color(0xFF1E1E1E),
                onBackground = androidx.compose.ui.graphics.Color.White,
                onSurface = androidx.compose.ui.graphics.Color.White
            )
            val lightColors = lightColorScheme(
                primary = androidx.compose.ui.graphics.Color.Black,
                onPrimary = androidx.compose.ui.graphics.Color.White,
                background = androidx.compose.ui.graphics.Color.White,
                surface = androidx.compose.ui.graphics.Color.White,
                onBackground = androidx.compose.ui.graphics.Color.Black,
                onSurface = androidx.compose.ui.graphics.Color.Black
            )

            MaterialTheme(colorScheme = if (isDarkTheme.value) darkColors else lightColors) {
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val isDark = isDarkTheme.value

                val iconTint = if (isDark)
                    androidx.compose.ui.graphics.Color.White
                else
                    androidx.compose.ui.graphics.Color.Black

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (isDark) "Светлая тема" else "Тёмная тема",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        isDarkTheme.value = !isDarkTheme.value
                                        scope.launch { drawerState.close() }
                                    }
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            Text(
                                "Редактировать данные",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        editedDataLauncher.launch(
                                            Intent(this@MainActivity, EditDataActivity::class.java)
                                        )
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
                            factory = { inflaterContext ->
                                val root = LayoutInflater.from(inflaterContext)
                                    .inflate(R.layout.activity_main, null)

                                val bottomSheet = root.findViewById<View>(R.id.bottomSheet)
                                bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet).apply {
                                    state = BottomSheetBehavior.STATE_COLLAPSED
                                    peekHeight = 200
                                    isHideable = false
                                }

                                val phoneView = root.findViewById<TextView>(R.id.phone_number)
                                phoneView.setOnClickListener {
                                    val phoneUri = Uri.parse("tel:${phoneView.text}")
                                    startActivity(Intent(Intent.ACTION_DIAL, phoneUri))
                                }

                                val emailView = root.findViewById<TextView>(R.id.email)
                                emailView.setOnClickListener {
                                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                        this.data = Uri.parse("mailto:${emailView.text}")
                                    }
                                    startActivity(emailIntent)
                                }

                                val telegramView = root.findViewById<TextView>(R.id.telegram_info)
                                telegramView.setOnClickListener {
                                    val username = extractUsername(telegramView.text.toString())
                                    if (!username.isNullOrEmpty()) {
                                        startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/$username"))
                                        )
                                    }
                                }

                                val photoView = root.findViewById<ImageView>(R.id.photo)
                                photoView.setOnClickListener {
                                    val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
                                    pickImageLauncher.launch(intent)
                                }

                                val shareButton = root.findViewById<MaterialButton>(R.id.share_button)
                                shareButton.setOnClickListener {
                                    val name = root.findViewById<TextView>(R.id.full_name).text
                                    val pos = root.findViewById<TextView>(R.id.position).text
                                    val phone = root.findViewById<TextView>(R.id.phone_number).text
                                    val emailText = root.findViewById<TextView>(R.id.email).text
                                    val tg = root.findViewById<TextView>(R.id.telegram_info).text
                                    val shareText = buildString {
                                        appendLine("👤 $name")
                                        appendLine("💼 $pos")
                                        appendLine("📞 $phone")
                                        appendLine("✉️ $emailText")
                                        append("📱 $tg")
                                    }
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    startActivity(Intent.createChooser(shareIntent, "Поделиться через"))
                                }

                                val showQrButton = root.findViewById<MaterialButton>(R.id.show_qr_button)
                                showQrButton.setOnClickListener {
                                    val name = root.findViewById<TextView>(R.id.full_name).text.toString()
                                    val phone = root.findViewById<TextView>(R.id.phone_number).text.toString()
                                    val emailText = root.findViewById<TextView>(R.id.email).text.toString()
                                    val contactData = "BEGIN:VCARD\n" +
                                        "VERSION:3.0\n" +
                                        "FN:$name\n" +
                                        "TEL:$phone\n" +
                                        "EMAIL:$emailText\n" +
                                        "END:VCARD"
                                    showQrDialog(generateQrCode(contactData))
                                }

                                val card = root.findViewById<View>(R.id.business_card)
                                card.setOnClickListener {
                                    bottomSheetBehavior?.let { bsb ->
                                        bsb.state = if (bsb.state == BottomSheetBehavior.STATE_EXPANDED)
                                            BottomSheetBehavior.STATE_COLLAPSED
                                        else
                                            BottomSheetBehavior.STATE_EXPANDED
                                    }
                                }

                                root
                            },
                            update = { root ->
                                val dark = isDarkTheme.value

                                // ── Background ──
                                val bgColor = if (dark) Color.parseColor("#121212") else Color.WHITE
                                root.setBackgroundColor(bgColor)

                                // ── Bottom sheet background (rounded top corners) ──
                                root.findViewById<View>(R.id.bottomSheet)
                                    .setBackgroundResource(
                                        if (dark) R.drawable.bottom_sheet_background_dark
                                        else R.drawable.bottom_sheet_background
                                    )

                                // ── Card background ──
                                root.findViewById<CardView>(R.id.business_card)
                                    .setCardBackgroundColor(
                                        if (dark) Color.parseColor("#1E1E1E") else Color.WHITE
                                    )

                                // ── Card text colors ──
                                val cardPrimary = if (dark) Color.WHITE else Color.BLACK
                                val cardSecondary = if (dark) Color.parseColor("#BBBBBB") else Color.parseColor("#555555")
                                root.findViewById<TextView>(R.id.full_name).setTextColor(cardPrimary)
                                root.findViewById<TextView>(R.id.position).setTextColor(cardSecondary)
                                root.findViewById<TextView>(R.id.phone_number).setTextColor(cardPrimary)
                                root.findViewById<TextView>(R.id.telegram_info).setTextColor(cardPrimary)

                                // ── Bottom sheet text colors ──
                                val sheetPrimary = if (dark) Color.parseColor("#EEEEEE") else Color.parseColor("#555555")
                                val sheetHint = if (dark) Color.parseColor("#AAAAAA") else Color.parseColor("#777777")
                                root.findViewById<TextView>(R.id.email).setTextColor(sheetPrimary)
                                root.findViewById<TextView>(R.id.about).setTextColor(sheetHint)

                                // ── Button tint ──
                                val btnColor = if (dark) Color.WHITE else Color.BLACK
                                val btnTextColor = if (dark) Color.BLACK else Color.WHITE
                                val btnColorList = android.content.res.ColorStateList.valueOf(btnColor)
                                val shareBtn = root.findViewById<MaterialButton>(R.id.share_button)
                                val qrBtn = root.findViewById<MaterialButton>(R.id.show_qr_button)
                                shareBtn.backgroundTintList = btnColorList
                                shareBtn.setTextColor(btnTextColor)
                                qrBtn.backgroundTintList = btnColorList
                                qrBtn.setTextColor(btnTextColor)

                                // ── Logo swap ──
                                root.findViewById<ImageView>(R.id.logo).setImageResource(
                                    if (dark) R.drawable.logo_for_dark_theme
                                    else R.drawable.logo_for_light_theme
                                )

                                // ── Data ──
                                root.findViewById<TextView>(R.id.full_name).text = fullNameState.value
                                root.findViewById<TextView>(R.id.position).text = positionState.value
                                root.findViewById<TextView>(R.id.phone_number).text = phoneState.value
                                root.findViewById<TextView>(R.id.email).text = emailState.value
                                root.findViewById<TextView>(R.id.telegram_info).text = telegramState.value
                                root.findViewById<TextView>(R.id.about).text = aboutState.value

                                val photoView = root.findViewById<ImageView>(R.id.photo)
                                val uriStr = profileImageUriState.value
                                if (uriStr != null) {
                                    photoView.setImageURI(Uri.parse(uriStr))
                                }
                            }
                        )

                        // Floating hamburger button (no header bar)
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.padding(4.dp)
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

    private fun generateQrCode(data: String, size: Int = 512): Bitmap {
        val hints = mapOf(EncodeHintType.CHARACTER_SET to "UTF-8")
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(
            data, BarcodeFormat.QR_CODE, size, size, hints
        )
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(
                    x, y,
                    if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                )
            }
        }
        return bmp
    }

    private fun showQrDialog(qrBitmap: Bitmap) {
        val dialog = Dialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_qr_code, null)
        view.findViewById<ImageView>(R.id.qrCodeImageView).setImageBitmap(qrBitmap)
        view.findViewById<MaterialButton>(R.id.closeButton).setOnClickListener { dialog.dismiss() }
        dialog.setContentView(view)
        dialog.show()
    }

    private fun extractUsername(text: String): String? {
        val cleaned = text.removePrefix("@").trim()
        return if (cleaned.isNotEmpty()) cleaned else null
    }
}
