package com.example.visiting_card.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.visiting_card.R
import com.google.android.material.bottomsheet.BottomSheetBehavior

class MainActivity : ComponentActivity() {

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // Найти BottomSheet по ID
        val bottomSheet = findViewById<View>(R.id.bottomSheet)

        if (bottomSheet != null) {
            // Настроить поведение BottomSheet
            bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)

            // Состояние по умолчанию - свернуто
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

            // Высота в свернутом состоянии
            bottomSheetBehavior.peekHeight = 100

            // Чтобы нельзя было полностью скрыть
            bottomSheetBehavior.isHideable = false
        }
        // Номер телефона
        val phoneNumber = findViewById<TextView>(R.id.phone_number)
        phoneNumber.setOnClickListener {
            val phoneUri = Uri.parse("tel:${phoneNumber.text}")
            val intent = Intent(Intent.ACTION_DIAL, phoneUri)
            startActivity(intent)
        }
        val email = findViewById<TextView>(R.id.email)
        email.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${email.text}")
            }
            startActivity(emailIntent)
        }
        // Telegram user id
        val telegramInfo = findViewById<TextView>(R.id.telegram_info)
        telegramInfo.setOnClickListener {
            val username = extractUsername(telegramInfo.text.toString())
            if (username != null) {
                val telegramIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/$username"))
                startActivity(telegramIntent)
            }
        }

        val shareButton = findViewById<Button>(R.id.share_button)
        shareButton.setOnClickListener {
            val fullName = findViewById<TextView>(R.id.full_name).text
            val position = findViewById<TextView>(R.id.position).text
            val phoneNumber = findViewById<TextView>(R.id.phone_number).text
            val email = findViewById<TextView>(R.id.email).text
            val telegramInfoText = findViewById<TextView>(R.id.telegram_info).text.toString()

            val telegramId = telegramInfoText.split("|").firstOrNull()?.trim() ?: ""

            val shareText = """
        👤 $fullName
        💼 $position
        📞 $phoneNumber
        ✉️ $email
        📱 $telegramId
        """.trimIndent()

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(shareIntent, "Поделиться через"))
        }


        // Добавим обработчик клика для переключения состояния BottomSheet
        val businessCard = findViewById<View>(R.id.business_card) // если у вас есть такая карточка
        businessCard?.setOnClickListener {
            // Переключаем состояние BottomSheet
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            } else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    // Метод чтобы вытащить username из строки типа "@user_id | New York City"
    private fun extractUsername(text: String): String? {
        val parts = text.split(" ")
        return parts.firstOrNull()?.removePrefix("@")
    }
}
