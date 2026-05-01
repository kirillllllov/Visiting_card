package com.example.visiting_card.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
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
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_SELECTED_SOCIAL_INDEX
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_SHOW_LOGO
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_SHOW_PHONE
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_SHOW_POSITION
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_SHOW_SOCIAL
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_SOCIAL_NETWORKS
import com.example.visiting_card.ui.EditDataActivity.Companion.PREFS_NAME
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class EditCardInfoActivity : ComponentActivity() {

    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_card_info)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val toolbar = findViewById<MaterialToolbar>(R.id.edit_card_toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        // Data inputs
        val fullNameInput = findViewById<TextInputEditText>(R.id.eci_full_name_input)
        val positionInput = findViewById<TextInputEditText>(R.id.eci_position_input)
        val phoneInput    = findViewById<TextInputEditText>(R.id.eci_phone_input)
        val aboutInput    = findViewById<TextInputEditText>(R.id.eci_about_input)
        val photoView     = findViewById<ImageView>(R.id.eci_photo)

        // Visibility checkboxes
        val cbPosition = findViewById<CheckBox>(R.id.cb_show_position)
        val cbPhone    = findViewById<CheckBox>(R.id.cb_show_phone)
        val cbLogo     = findViewById<CheckBox>(R.id.cb_show_logo)
        val cbSocial   = findViewById<CheckBox>(R.id.cb_show_social)

        // Social selection
        val socialContainer = findViewById<LinearLayout>(R.id.social_selection_container)
        val socialHint      = findViewById<TextView>(R.id.social_no_networks_hint)
        val socialGroup     = findViewById<RadioGroup>(R.id.social_radio_group)

        // ── Populate data ──────────────────────────────────────────────────
        fullNameInput.setText(prefs.getString(KEY_FULL_NAME, ""))
        positionInput.setText(prefs.getString(KEY_POSITION, ""))
        phoneInput.setText(prefs.getString(KEY_PHONE, ""))
        aboutInput.setText(prefs.getString(KEY_ABOUT, ""))

        prefs.getString(KEY_PROFILE_IMAGE_URI, null)?.let {
            selectedImageUri = Uri.parse(it)
            photoView.setImageURI(selectedImageUri)
        }

        // ── Visibility prefs ───────────────────────────────────────────────
        cbPosition.isChecked = prefs.getBoolean(KEY_SHOW_POSITION, true)
        cbPhone.isChecked    = prefs.getBoolean(KEY_SHOW_PHONE, true)
        cbLogo.isChecked     = prefs.getBoolean(KEY_SHOW_LOGO, true)
        cbSocial.isChecked   = prefs.getBoolean(KEY_SHOW_SOCIAL, false)

        // ── Social radio group ─────────────────────────────────────────────
        val networks = SocialNetworkUtils.loadNetworks(prefs, KEY_SOCIAL_NETWORKS)
        val savedIdx = prefs.getInt(KEY_SELECTED_SOCIAL_INDEX, -1)

        fun buildSocialContainer() {
            val visible = cbSocial.isChecked
            socialContainer.visibility = if (visible) View.VISIBLE else View.GONE
            if (!visible) return

            if (networks.isEmpty()) {
                socialHint.visibility  = View.VISIBLE
                socialGroup.visibility = View.GONE
            } else {
                socialHint.visibility  = View.GONE
                socialGroup.visibility = View.VISIBLE
                socialGroup.removeAllViews()
                networks.forEachIndexed { idx, network ->
                    val rb = RadioButton(this).apply {
                        id   = View.generateViewId()
                        text = "${network.type}: ${network.username}"
                        tag  = idx
                        textSize = 15f
                    }
                    socialGroup.addView(rb)
                    if (idx == savedIdx) socialGroup.check(rb.id)
                }
                // Default to first if nothing selected
                if (socialGroup.checkedRadioButtonId == -1 && networks.isNotEmpty()) {
                    (socialGroup.getChildAt(0) as? RadioButton)?.let { socialGroup.check(it.id) }
                }
            }
        }

        buildSocialContainer()
        cbSocial.setOnCheckedChangeListener { _, _ -> buildSocialContainer() }

        // ── Photo picker ───────────────────────────────────────────────────
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

        // ── Save ───────────────────────────────────────────────────────────
        findViewById<MaterialButton>(R.id.eci_save_button).setOnClickListener {
            val selectedSocialIdx = if (cbSocial.isChecked && networks.isNotEmpty()) {
                val rb = socialGroup.findViewById<RadioButton>(socialGroup.checkedRadioButtonId)
                (rb?.tag as? Int) ?: 0
            } else -1

            with(prefs.edit()) {
                putString(KEY_FULL_NAME, fullNameInput.text.toString().trim())
                putString(KEY_POSITION,  positionInput.text.toString().trim())
                putString(KEY_PHONE,     phoneInput.text.toString().trim())
                putString(KEY_ABOUT,     aboutInput.text.toString().trim())
                putBoolean(KEY_SHOW_POSITION, cbPosition.isChecked)
                putBoolean(KEY_SHOW_PHONE,    cbPhone.isChecked)
                putBoolean(KEY_SHOW_LOGO,     cbLogo.isChecked)
                putBoolean(KEY_SHOW_SOCIAL,   cbSocial.isChecked)
                putInt(KEY_SELECTED_SOCIAL_INDEX, selectedSocialIdx)
                selectedImageUri?.let { putString(KEY_PROFILE_IMAGE_URI, it.toString()) }
                apply()
            }

            setResult(RESULT_OK)
            finish()
        }
    }
}
