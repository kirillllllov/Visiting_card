package com.example.visiting_card.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.visiting_card.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class EditDataActivity : ComponentActivity() {
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private var selectedImageUri: Uri? = null

    companion object {
        const val PREFS_NAME = "VisitingCardData"
        const val KEY_FULL_NAME = "fullName"
        const val KEY_POSITION = "position"
        const val KEY_PHONE = "phone"
        const val KEY_EMAIL = "email"
        const val KEY_TELEGRAM = "telegram"
        const val KEY_INTERESTS = "interests"
        const val KEY_SKILLS = "skills"
        const val KEY_PROFILE_IMAGE_URI = "profile_image_uri"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_data)

        val toolbar = findViewById<MaterialToolbar>(R.id.edit_toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val fullNameInput = findViewById<TextInputEditText>(R.id.full_name_input)
        val positionInput = findViewById<TextInputEditText>(R.id.position_input)
        val phoneInput = findViewById<TextInputEditText>(R.id.phone_input)
        val emailInput = findViewById<TextInputEditText>(R.id.email_input)
        val telegramInput = findViewById<TextInputEditText>(R.id.telegram_input)
        val interestsInput = findViewById<TextInputEditText>(R.id.interests_input)
        val skillsInput = findViewById<TextInputEditText>(R.id.skills_input)
        val photoView = findViewById<ImageView>(R.id.edit_photo)
        val saveButton = findViewById<MaterialButton>(R.id.save_button)

        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fullNameInput.setText(sharedPref.getString(KEY_FULL_NAME, ""))
        positionInput.setText(sharedPref.getString(KEY_POSITION, ""))
        phoneInput.setText(sharedPref.getString(KEY_PHONE, ""))
        emailInput.setText(sharedPref.getString(KEY_EMAIL, ""))
        telegramInput.setText(sharedPref.getString(KEY_TELEGRAM, ""))
        interestsInput.setText(sharedPref.getString(KEY_INTERESTS, ""))
        skillsInput.setText(sharedPref.getString(KEY_SKILLS, ""))

        val savedImageUri = sharedPref.getString(KEY_PROFILE_IMAGE_URI, null)
        savedImageUri?.let {
            selectedImageUri = Uri.parse(it)
            photoView.setImageURI(selectedImageUri)
        }

        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    selectedImageUri = uri
                    photoView.setImageURI(uri)
                }
            }
        }

        photoView.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            pickImageLauncher.launch(intent)
        }

        saveButton.setOnClickListener {
            val fullName = fullNameInput.text.toString().trim()
            val position = positionInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val telegram = telegramInput.text.toString().trim()
            val interests = interestsInput.text.toString().trim()
            val skills = skillsInput.text.toString().trim()

            with(sharedPref.edit()) {
                putString(KEY_FULL_NAME, fullName)
                putString(KEY_POSITION, position)
                putString(KEY_PHONE, phone)
                putString(KEY_EMAIL, email)
                putString(KEY_TELEGRAM, telegram)
                putString(KEY_INTERESTS, interests)
                putString(KEY_SKILLS, skills)
                selectedImageUri?.let { putString(KEY_PROFILE_IMAGE_URI, it.toString()) }
                apply()
            }

            val resultIntent = Intent().apply {
                putExtra(KEY_FULL_NAME, fullName)
                putExtra(KEY_POSITION, position)
                putExtra(KEY_PHONE, phone)
                putExtra(KEY_EMAIL, email)
                putExtra(KEY_TELEGRAM, telegram)
                putExtra(KEY_INTERESTS, interests)
                putExtra(KEY_SKILLS, skills)
                selectedImageUri?.let { putExtra(KEY_PROFILE_IMAGE_URI, it.toString()) }
            }

            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
}
