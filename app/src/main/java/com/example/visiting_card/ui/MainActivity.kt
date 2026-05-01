package com.example.visiting_card.ui

import android.app.Dialog
import android.content.Intent
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.visiting_card.R
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_EMAIL
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_FULL_NAME
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_INTERESTS
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_PHONE
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_POSITION
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_PROFILE_IMAGE_URI
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_SKILLS
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_TELEGRAM
import com.example.visiting_card.ui.EditDataActivity.Companion.PREFS_NAME
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import android.graphics.Bitmap
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
        val interestsState = mutableStateOf(sharedPreferences.getString(KEY_INTERESTS, "") ?: "")
        val skillsState = mutableStateOf(sharedPreferences.getString(KEY_SKILLS, "") ?: "")
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
                interestsState.value = data.getStringExtra(KEY_INTERESTS) ?: interestsState.value
                skillsState.value = data.getStringExtra(KEY_SKILLS) ?: skillsState.value

                val imageUriString = data.getStringExtra(KEY_PROFILE_IMAGE_URI)
                if (imageUriString != null) {
                    profileImageUriState.value = imageUriString
                }
            }
        }

        val isDarkTheme = mutableStateOf(false)

        setContent {
            MaterialTheme(
                colorScheme = if (isDarkTheme.value) darkColorScheme() else lightColorScheme()
            ) {
                val context = LocalContext.current
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Сменить тему",
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
                                            Intent(context, EditDataActivity::class.java)
                                        )
                                        scope.launch { drawerState.close() }
                                    }
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("Визитка") },
                                navigationIcon = {
                                    IconButton(onClick = {
                                        scope.launch { drawerState.open() }
                                    }) {
                                        Icon(Icons.Default.Menu, contentDescription = "Меню")
                                    }
                                }
                            )
                        }
                    ) { innerPadding ->
                        AndroidView(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
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

                                    val qrBitmap = generateQrCode(contactData)
                                    showQrDialog(qrBitmap)
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
                                root.findViewById<TextView>(R.id.full_name).text = fullNameState.value
                                root.findViewById<TextView>(R.id.position).text = positionState.value
                                root.findViewById<TextView>(R.id.phone_number).text = phoneState.value
                                root.findViewById<TextView>(R.id.email).text = emailState.value
                                root.findViewById<TextView>(R.id.telegram_info).text = telegramState.value
                                root.findViewById<TextView>(R.id.interests).text = interestsState.value
                                root.findViewById<TextView>(R.id.skills).text = skillsState.value

                                val photoView = root.findViewById<ImageView>(R.id.photo)
                                val uriStr = profileImageUriState.value
                                if (uriStr != null) {
                                    photoView.setImageURI(Uri.parse(uriStr))
                                }
                            }
                        )
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
                    if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                )
            }
        }
        return bmp
    }

    private fun showQrDialog(qrBitmap: Bitmap) {
        val dialog = Dialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_qr_code, null)
        val qrImage = view.findViewById<ImageView>(R.id.qrCodeImageView)
        val closeButton = view.findViewById<MaterialButton>(R.id.closeButton)

        qrImage.setImageBitmap(qrBitmap)
        closeButton.setOnClickListener { dialog.dismiss() }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun extractUsername(text: String): String? {
        val cleaned = text.removePrefix("@").trim()
        return if (cleaned.isNotEmpty()) cleaned else null
    }
}
