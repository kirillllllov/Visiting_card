package com.example.visiting_card.ui

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
import androidx.activity.result.contract.ActivityResultContracts
@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

                // Сохраняем обновленные данные в SharedPreferences
                with(sharedPreferences.edit()) {
                    putString("fullName", fullName)
                    putString("position", position)
                    putString("phone", phone)
                    putString("email", email)
                    putString("telegram", telegram)
                    putString("interests", interests)
                    putString("skills", skills)
                    apply()
                }

                // Обновляем TextView в MainActivity
                val rootView = findViewById<View>(android.R.id.content)
                rootView.findViewById<TextView>(R.id.full_name)?.text = fullName
                rootView.findViewById<TextView>(R.id.position)?.text = position
                rootView.findViewById<TextView>(R.id.phone_number)?.text = phone
                rootView.findViewById<TextView>(R.id.email)?.text = email
                rootView.findViewById<TextView>(R.id.telegram_info)?.text = telegram
                rootView.findViewById<TextView>(R.id.interests)?.text = interests
                rootView.findViewById<TextView>(R.id.skills)?.text = skills
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
                                    val telegramInfoText =
                                        root.findViewById<TextView>(R.id.telegram_info).text.toString()

                                    val telegramId =
                                        telegramInfoText.split("|").firstOrNull()?.trim() ?: ""

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

    private fun extractUsername(text: String): String? {
        val parts = text.split(" ")
        return parts.firstOrNull()?.removePrefix("@")
    }
}
