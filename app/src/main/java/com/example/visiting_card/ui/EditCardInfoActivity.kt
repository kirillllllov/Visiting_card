package com.example.visiting_card.ui

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.visiting_card.R
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_ABOUT
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_FULL_NAME
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_PHONE
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_POSITION
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_PROFILE_IMAGE_URI
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_THEME_DARK
import com.example.visiting_card.ui.EditDataActivity.Companion.PREFS_NAME
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText

class EditCardInfoActivity : ComponentActivity() {

    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_card_info)

        val prefs  = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isDark = prefs.getBoolean(KEY_THEME_DARK, false)

        val toolbar = findViewById<MaterialToolbar>(R.id.edit_card_toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val fullNameInput = findViewById<TextInputEditText>(R.id.eci_full_name_input)
        val positionInput = findViewById<TextInputEditText>(R.id.eci_position_input)
        val phoneInput    = findViewById<TextInputEditText>(R.id.eci_phone_input)
        val aboutInput    = findViewById<TextInputEditText>(R.id.eci_about_input)
        val photoView     = findViewById<ImageView>(R.id.eci_photo)

        fullNameInput.setText(prefs.getString(KEY_FULL_NAME, ""))
        positionInput.setText(prefs.getString(KEY_POSITION, ""))
        phoneInput.setText(prefs.getString(KEY_PHONE, ""))
        aboutInput.setText(prefs.getString(KEY_ABOUT, ""))

        prefs.getString(KEY_PROFILE_IMAGE_URI, null)?.let {
            selectedImageUri = Uri.parse(it)
            photoView.setImageURI(selectedImageUri)
        }

        applyTheme(isDark)

        pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    selectedImageUri = uri
                    photoView.setImageURI(uri)
                }
            }
        }
        photoView.setOnClickListener {
            pickImageLauncher.launch(Intent(Intent.ACTION_PICK).apply { type = "image/*" })
        }

        findViewById<MaterialButton>(R.id.eci_save_button).setOnClickListener {
            with(prefs.edit()) {
                putString(KEY_FULL_NAME, fullNameInput.text.toString().trim())
                putString(KEY_POSITION,  positionInput.text.toString().trim())
                putString(KEY_PHONE,     phoneInput.text.toString().trim())
                putString(KEY_ABOUT,     aboutInput.text.toString().trim())
                selectedImageUri?.let { putString(KEY_PROFILE_IMAGE_URI, it.toString()) }
                apply()
            }
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun applyTheme(isDark: Boolean) {
        val bgColor       = if (isDark) Color.parseColor("#121212") else Color.WHITE
        val textPrimary   = if (isDark) Color.WHITE else Color.BLACK
        val textSecondary = if (isDark) Color.parseColor("#BBBBBB") else Color.parseColor("#777777")
        val strokeColor   = if (isDark) Color.parseColor("#666666") else Color.parseColor("#AAAAAA")
        val btnBg         = if (isDark) Color.WHITE else Color.BLACK
        val btnText       = if (isDark) Color.BLACK else Color.WHITE

        findViewById<LinearLayout>(R.id.eci_root).setBackgroundColor(bgColor)

        listOf(R.id.eci_photo_hint, R.id.eci_section_header_data).forEach { id ->
            findViewById<TextView>(id)?.setTextColor(textSecondary)
        }

        listOf(R.id.eci_til_name, R.id.eci_til_position, R.id.eci_til_phone, R.id.eci_til_about).forEach { id ->
            val til = findViewById<TextInputLayout>(id) ?: return@forEach
            til.hintTextColor = ColorStateList.valueOf(textSecondary)
            til.boxStrokeColor = strokeColor
            til.editText?.setTextColor(textPrimary)
            til.editText?.setHintTextColor(ColorStateList.valueOf(textSecondary))
        }

        val saveBtn = findViewById<MaterialButton>(R.id.eci_save_button)
        saveBtn.backgroundTintList = ColorStateList.valueOf(btnBg)
        saveBtn.setTextColor(btnText)
    }
}
