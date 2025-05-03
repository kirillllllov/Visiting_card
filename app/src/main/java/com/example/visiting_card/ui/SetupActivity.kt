package com.example.visiting_card.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.visiting_card.R

class SetupActivity : AppCompatActivity() {

    private lateinit var fullNameInput: EditText
    private lateinit var positionInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var telegramInput: EditText
    private lateinit var interestsInput: EditText
    private lateinit var skillsInput: EditText
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_data)

        fullNameInput = findViewById(R.id.full_name_input)
        positionInput = findViewById(R.id.position_input)
        phoneInput = findViewById(R.id.phone_input)
        emailInput = findViewById(R.id.email_input)
        telegramInput = findViewById(R.id.telegram_input)
        interestsInput = findViewById(R.id.interests_input)
        skillsInput = findViewById(R.id.skills_input)
        saveButton = findViewById(R.id.save_button)

        saveButton.setOnClickListener {
            saveProfileData()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun saveProfileData() {
        val prefs = getSharedPreferences("ProfilePrefs", MODE_PRIVATE)
        prefs.edit()
            .putString("fullName", fullNameInput.text.toString())
            .putString("position", positionInput.text.toString())
            .putString("phone", phoneInput.text.toString())
            .putString("email", emailInput.text.toString())
            .putString("telegram", telegramInput.text.toString())
            .putString("interests", interestsInput.text.toString())
            .putString("skills", skillsInput.text.toString())
            .putBoolean("profileCreated", true)
            .apply()
    }
}
