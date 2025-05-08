package com.example.visiting_card.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.visiting_card.R

class EditDataActivity : ComponentActivity() {
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_data)

        val fullNameInput = findViewById<EditText>(R.id.full_name_input)
        val positionInput = findViewById<EditText>(R.id.position_input)
        val phoneInput = findViewById<EditText>(R.id.phone_input)
        val emailInput = findViewById<EditText>(R.id.email_input)
        val telegramInput = findViewById<EditText>(R.id.telegram_input)
        val interestsInput = findViewById<EditText>(R.id.interests_input)
        val skillsInput = findViewById<EditText>(R.id.skills_input)
        val photoView = findViewById<ImageView>(R.id.edit_photo)
        val saveButton = findViewById<Button>(R.id.save_button)

        // SharedPreferences
        val sharedPref = getSharedPreferences("card_prefs", Context.MODE_PRIVATE)

        // Загрузка текста
        fullNameInput.setText(sharedPref.getString("full_name", ""))
        positionInput.setText(sharedPref.getString("position", ""))
        phoneInput.setText(sharedPref.getString("phone", ""))
        emailInput.setText(sharedPref.getString("email", ""))
        telegramInput.setText(sharedPref.getString("telegram", ""))
        interestsInput.setText(sharedPref.getString("interests", ""))
        skillsInput.setText(sharedPref.getString("skills", ""))

        // Загрузка изображения
        val savedImageUri = sharedPref.getString("profile_image_uri", null)
        savedImageUri?.let {
            selectedImageUri = Uri.parse(it)
            photoView.setImageURI(selectedImageUri)
        }

        // Запуск выбора изображения
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    selectedImageUri = uri
                    photoView.setImageURI(uri)
                }
            }
        }

        // Открыть галерею по нажатию на фото
        photoView.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            pickImageLauncher.launch(intent)
        }

        // Сохранение
        saveButton.setOnClickListener {
            with(sharedPref.edit()) {
                putString("full_name", fullNameInput.text.toString())
                putString("position", positionInput.text.toString())
                putString("phone", phoneInput.text.toString())
                putString("email", emailInput.text.toString())
                putString("telegram", telegramInput.text.toString())
                putString("interests", interestsInput.text.toString())
                putString("skills", skillsInput.text.toString())
                selectedImageUri?.let { putString("profile_image_uri", it.toString()) }
                apply()
            }

            val resultIntent = Intent().apply {
                putExtra("fullName", fullNameInput.text.toString())
                putExtra("position", positionInput.text.toString())
                putExtra("phone", phoneInput.text.toString())
                putExtra("email", emailInput.text.toString())
                putExtra("telegram", telegramInput.text.toString())
                putExtra("interests", interestsInput.text.toString())
                putExtra("skills", skillsInput.text.toString())
                selectedImageUri?.let { putExtra("profile_image_uri", it.toString()) }
            }

            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
}
