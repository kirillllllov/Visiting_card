package com.example.visiting_card.ui

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.visiting_card.R
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_SOCIAL_NETWORKS
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_THEME_DARK
import com.example.visiting_card.ui.EditDataActivity.Companion.PREFS_NAME
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class AddSocialNetworksActivity : ComponentActivity() {

    private val networks = mutableListOf<SocialNetwork>()
    private lateinit var networksContainer: LinearLayout
    private lateinit var emptyHint: TextView
    private lateinit var rootLayout: LinearLayout
    private lateinit var addBtn: MaterialButton
    private lateinit var saveBtn: MaterialButton
    private var isDark = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_social_networks)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isDark = prefs.getBoolean(KEY_THEME_DARK, false)

        rootLayout = findViewById(R.id.social_root_layout)
        val toolbar = findViewById<MaterialToolbar>(R.id.social_toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        networksContainer = findViewById(R.id.networks_container)
        emptyHint = findViewById(R.id.empty_hint)
        addBtn = findViewById(R.id.add_network_button)
        saveBtn = findViewById(R.id.save_social_button)

        networks.addAll(SocialNetworkUtils.loadNetworks(prefs, KEY_SOCIAL_NETWORKS))
        applyTheme()
        refreshList()

        addBtn.setOnClickListener { showAddDialog() }

        saveBtn.setOnClickListener {
            SocialNetworkUtils.saveNetworks(prefs, KEY_SOCIAL_NETWORKS, networks)
            finish()
        }
    }

    private fun applyTheme() {
        val bgColor    = if (isDark) Color.parseColor("#121212") else Color.WHITE
        val textColor  = if (isDark) Color.WHITE else Color.parseColor("#212121")
        val hintColor  = if (isDark) Color.parseColor("#AAAAAA") else Color.parseColor("#999999")
        val btnBg      = if (isDark) Color.WHITE else Color.BLACK
        val btnText    = if (isDark) Color.BLACK else Color.WHITE
        val outlineBtn = if (isDark) Color.parseColor("#DDDDDD") else Color.BLACK
        val outlineBg  = if (isDark) Color.parseColor("#1E1E1E") else Color.WHITE

        rootLayout.setBackgroundColor(bgColor)
        emptyHint.setTextColor(hintColor)

        val btnColorList     = android.content.res.ColorStateList.valueOf(btnBg)
        val outlineBtnColor  = android.content.res.ColorStateList.valueOf(outlineBtn)
        val outlineBgColor   = android.content.res.ColorStateList.valueOf(outlineBg)

        saveBtn.backgroundTintList = btnColorList
        saveBtn.setTextColor(btnText)

        addBtn.backgroundTintList = outlineBgColor
        addBtn.strokeColor = outlineBtnColor
        addBtn.setTextColor(outlineBtn)
    }

    private fun refreshList() {
        networksContainer.removeAllViews()
        emptyHint.visibility = if (networks.isEmpty()) View.VISIBLE else View.GONE

        val textColor  = if (isDark) Color.WHITE else Color.parseColor("#212121")

        networks.forEachIndexed { index, network ->
            val row = layoutInflater.inflate(R.layout.item_social_network, networksContainer, false)
            row.setBackgroundColor(Color.TRANSPARENT)
            row.findViewById<TextView>(R.id.network_name).apply {
                text = "${network.type}: ${network.username}"
                setTextColor(textColor)
            }
            row.findViewById<ImageButton>(R.id.delete_network).also { btn ->
                btn.setColorFilter(textColor)
                btn.setOnClickListener {
                    networks.removeAt(index)
                    refreshList()
                }
            }
            networksContainer.addView(row)

            if (index < networks.size - 1) {
                networksContainer.addView(View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1
                    )
                    setBackgroundColor(if (isDark) Color.parseColor("#333333") else Color.parseColor("#EEEEEE"))
                })
            }
        }
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_social_network, null)
        val spinner = dialogView.findViewById<Spinner>(R.id.network_type_spinner)
        val usernameInput = dialogView.findViewById<TextInputEditText>(R.id.network_username_input)

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            SocialNetworkUtils.NETWORK_TYPES
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        AlertDialog.Builder(this)
            .setTitle("Добавить социальную сеть")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                val type = spinner.selectedItem.toString()
                var username = usernameInput.text.toString().trim()
                if (username.isNotEmpty()) {
                    if (!username.startsWith("@")) username = "@$username"
                    networks.add(SocialNetwork(type, username))
                    refreshList()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}
