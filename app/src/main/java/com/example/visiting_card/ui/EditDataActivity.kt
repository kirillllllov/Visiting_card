package com.example.visiting_card.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.ComponentActivity
import com.example.visiting_card.R

class EditDataActivity : ComponentActivity() {
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
        val saveButton = findViewById<Button>(R.id.save_button)

        // Загрузка данных из SharedPreferences
        val sharedPref = getSharedPreferences("card_prefs", Context.MODE_PRIVATE)
        fullNameInput.setText(sharedPref.getString("full_name", ""))
        positionInput.setText(sharedPref.getString("position", ""))
        phoneInput.setText(sharedPref.getString("phone", ""))
        emailInput.setText(sharedPref.getString("email", ""))
        telegramInput.setText(sharedPref.getString("telegram", ""))
        interestsInput.setText(sharedPref.getString("interests", ""))
        skillsInput.setText(sharedPref.getString("skills", ""))

        saveButton.setOnClickListener {
            // Сохраняем данные в SharedPreferences
            with(sharedPref.edit()) {
                putString("full_name", fullNameInput.text.toString())
                putString("position", positionInput.text.toString())
                putString("phone", phoneInput.text.toString())
                putString("email", emailInput.text.toString())
                putString("telegram", telegramInput.text.toString())
                putString("interests", interestsInput.text.toString())
                putString("skills", skillsInput.text.toString())
                apply()
            }

            // Возвращаем обновленные данные в MainActivity
            val resultIntent = Intent()
            resultIntent.putExtra("fullName", fullNameInput.text.toString())
            resultIntent.putExtra("position", positionInput.text.toString())
            resultIntent.putExtra("phone", phoneInput.text.toString())
            resultIntent.putExtra("email", emailInput.text.toString())
            resultIntent.putExtra("telegram", telegramInput.text.toString())
            resultIntent.putExtra("interests", interestsInput.text.toString())
            resultIntent.putExtra("skills", skillsInput.text.toString())
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
}
