package com.example.visiting_card.ui

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.launch
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val imageUri = result.data?.data ?: return@registerForActivityResult

                // Сохраняем URI
                getSharedPreferences("VisitingCardData", MODE_PRIVATE)
                    .edit()
                    .putString("profile_image_uri", imageUri.toString())
                    .apply()

                // Обновляем ImageView
                val rootView = findViewById<View>(android.R.id.content)
                val photoView = rootView.findViewById<ImageView>(R.id.photo)
                photoView.setImageURI(imageUri)
            }
        }
        val sharedPreferences: SharedPreferences = getSharedPreferences("VisitingCardData", MODE_PRIVATE)

        // Загрузка данных из SharedPreferences
        var fullName = sharedPreferences.getString("fullName", "Не указано") ?: "Не указано"
        var position = sharedPreferences.getString("position", "Не указано") ?: "Не указано"
        var phone = sharedPreferences.getString("phone", "Не указано") ?: "Не указано"
        var email = sharedPreferences.getString("email", "Не указано") ?: "Не указано"
        var telegram = sharedPreferences.getString("telegram", "Не указано") ?: "Не указано"
        var interests = sharedPreferences.getString("interests", "Не указано") ?: "Не указано"
        var skills = sharedPreferences.getString("skills", "Не указано") ?: "Не указано"

        val isDarkTheme = mutableStateOf(false)

        val editedDataLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data ?: return@registerForActivityResult
                // Получаем данные из результата
                fullName = data.getStringExtra("fullName") ?: fullName
                position = data.getStringExtra("position") ?: position
                phone = data.getStringExtra("phone") ?: phone
                email = data.getStringExtra("email") ?: email
                telegram = data.getStringExtra("telegram") ?: telegram
                interests = data.getStringExtra("interests") ?: interests
                skills = data.getStringExtra("skills") ?: skills

                val imageUriString = data.getStringExtra("profile_image_uri")

                // Сохраняем обновленные данные в SharedPreferences
                with(sharedPreferences.edit()) {
                    putString("fullName", fullName)
                    putString("position", position)
                    putString("phone", phone)
                    putString("email", email)
                    putString("telegram", telegram)
                    putString("interests", interests)
                    putString("skills", skills)
                    if (imageUriString != null) {
                        putString("profile_image_uri", imageUriString)
                    }
                    apply()
                }

                // Обновляем UI
                val rootView = findViewById<View>(android.R.id.content)
                rootView.findViewById<TextView>(R.id.full_name)?.text = fullName
                rootView.findViewById<TextView>(R.id.position)?.text = position
                rootView.findViewById<TextView>(R.id.phone_number)?.text = phone
                rootView.findViewById<TextView>(R.id.email)?.text = email
                rootView.findViewById<TextView>(R.id.telegram_info)?.text = telegram
                rootView.findViewById<TextView>(R.id.interests)?.text = interests
                rootView.findViewById<TextView>(R.id.skills)?.text = skills

                // Обновляем фото, если есть
                imageUriString?.let {
                    val imageUri = Uri.parse(it)
                    rootView.findViewById<ImageView>(R.id.photo)?.setImageURI(imageUri)
                }
            }
        }




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
                            Text(
                                "Сменить тему",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        isDarkTheme.value = !isDarkTheme.value
                                        scope.launch { drawerState.close() }
                                    }
                                    .padding(16.dp)
                            )
                            Text(
                                "Редактировать данные",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        editedDataLauncher.launch(Intent(context, EditDataActivity::class.java))
                                        scope.launch { drawerState.close() }
                                    }
                                    .padding(16.dp)
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
                                    peekHeight = 100
                                    isHideable = false
                                }
                                val photoView = root.findViewById<ImageView>(R.id.photo)
                                val savedUri = sharedPreferences.getString("profile_image_uri", null)
                                if (savedUri != null) {
                                    photoView.setImageURI(Uri.parse(savedUri))
                                }
                                photoView.setOnClickListener {
                                    val intent = Intent(Intent.ACTION_PICK).apply {
                                        type = "image/*"
                                    }
                                    pickImageLauncher.launch(intent)
                                }

                                // Загрузка данных из SharedPreferences в TextView
                                root.findViewById<TextView>(R.id.full_name).text = fullName
                                root.findViewById<TextView>(R.id.position).text = position
                                root.findViewById<TextView>(R.id.phone_number).text = phone
                                root.findViewById<TextView>(R.id.email).text = email
                                root.findViewById<TextView>(R.id.telegram_info).text = telegram
                                root.findViewById<TextView>(R.id.interests).text = interests
                                root.findViewById<TextView>(R.id.skills).text = skills

                                val phoneNumber = root.findViewById<TextView>(R.id.phone_number)
                                phoneNumber.setOnClickListener {
                                    val phoneUri = Uri.parse("tel:${phoneNumber.text}")
                                    val intent = Intent(Intent.ACTION_DIAL, phoneUri)
                                    startActivity(intent)
                                }

                                val email = root.findViewById<TextView>(R.id.email)
                                email.setOnClickListener {
                                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:${email.text}")
                                    }
                                    startActivity(emailIntent)
                                }

                                val telegramInfo = root.findViewById<TextView>(R.id.telegram_info)
                                telegramInfo.setOnClickListener {
                                    val username = extractUsername(telegramInfo.text.toString())
                                    if (username != null) {
                                        val telegramIntent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://t.me/$username")
                                        )
                                        startActivity(telegramIntent)
                                    }
                                }

                                val shareButton = root.findViewById<Button>(R.id.share_button)
                                shareButton.setOnClickListener {
                                    val fullName = root.findViewById<TextView>(R.id.full_name).text
                                    val position = root.findViewById<TextView>(R.id.position).text
                                    val phone = root.findViewById<TextView>(R.id.phone_number).text
                                    val emailText = root.findViewById<TextView>(R.id.email).text
                                    val telegramId =
                                        root.findViewById<TextView>(R.id.telegram_info).text.toString()

                                    val shareText = """
                                        👤 $fullName
                                        💼 $position
                                        📞 $phone
                                        ✉️ $emailText
                                        📱 $telegramId
                                    """.trimIndent()

                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    startActivity(Intent.createChooser(shareIntent, "Поделиться через"))
                                }
                                    val showQrButton = root.findViewById<Button>(R.id.show_qr_button)
                                    showQrButton.setOnClickListener {
                                        val email = root.findViewById<TextView>(R.id.email).text.toString()
                                        val phone = root.findViewById<TextView>(R.id.phone_number).text.toString()
                                        val fullName = root.findViewById<TextView>(R.id.full_name).text.toString()
                                        val contactData = """
                                            BEGIN:VCARD
                                            VERSION:3.0
                                            N:${fullName}
                                            TEL:${phone}
                                            EMAIL:${email}
                                            END:VCARD
                                            """.trimIndent()
                                        val qrBitmap = generateQrCode(contactData)
                                        showQrDialog(qrBitmap)
                                    }

                                val card = root.findViewById<View>(R.id.business_card)
                                card.setOnClickListener {
                                    bottomSheetBehavior?.let {
                                        it.state = if (it.state == BottomSheetBehavior.STATE_EXPANDED)
                                            BottomSheetBehavior.STATE_COLLAPSED
                                        else
                                            BottomSheetBehavior.STATE_EXPANDED
                                    }
                                }

                                root
                            }
                        )
                    }
                }
            }
        }
    }
    fun generateQrCode(data: String, size: Int = 512): Bitmap {
        val hints = mapOf(EncodeHintType.CHARACTER_SET to "UTF-8")
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, size, size, hints)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)

        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        return bmp
    }
    private fun showQrDialog(qrBitmap: Bitmap) {
        val dialog = Dialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_qr_code, null)
        val qrImage = view.findViewById<ImageView>(R.id.qrCodeImageView)
        val closeButton = view.findViewById<Button>(R.id.closeButton)

        qrImage.setImageBitmap(qrBitmap)
        closeButton.setOnClickListener { dialog.dismiss() }

        dialog.setContentView(view)
        dialog.show()
    }


    private fun extractUsername(text: String): String? {
        return text.removePrefix("@").trim()
    }
}
