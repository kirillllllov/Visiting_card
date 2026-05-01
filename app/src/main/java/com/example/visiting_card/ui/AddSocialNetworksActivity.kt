package com.example.visiting_card.ui

import android.app.AlertDialog
import android.content.Context
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
import com.example.visiting_card.ui.EditDataActivity.Companion.PREFS_NAME
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class AddSocialNetworksActivity : ComponentActivity() {

    private val networks = mutableListOf<SocialNetwork>()
    private lateinit var networksContainer: LinearLayout
    private lateinit var emptyHint: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_social_networks)

        val toolbar = findViewById<MaterialToolbar>(R.id.social_toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        networksContainer = findViewById(R.id.networks_container)
        emptyHint = findViewById(R.id.empty_hint)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        networks.addAll(SocialNetworkUtils.loadNetworks(prefs, KEY_SOCIAL_NETWORKS))
        refreshList()

        findViewById<MaterialButton>(R.id.add_network_button).setOnClickListener {
            showAddDialog()
        }

        findViewById<MaterialButton>(R.id.save_social_button).setOnClickListener {
            SocialNetworkUtils.saveNetworks(prefs, KEY_SOCIAL_NETWORKS, networks)
            finish()
        }
    }

    private fun refreshList() {
        networksContainer.removeAllViews()
        emptyHint.visibility = if (networks.isEmpty()) View.VISIBLE else View.GONE

        networks.forEachIndexed { index, network ->
            val row = layoutInflater.inflate(R.layout.item_social_network, networksContainer, false)
            row.findViewById<TextView>(R.id.network_name).text = "${network.type}: ${network.username}"
            row.findViewById<ImageButton>(R.id.delete_network).setOnClickListener {
                networks.removeAt(index)
                refreshList()
            }
            networksContainer.addView(row)

            // Divider line between items
            if (index < networks.size - 1) {
                val divider = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1
                    ).also { it.setMargins(0, 0, 0, 0) }
                    setBackgroundColor(0xFFEEEEEE.toInt())
                }
                networksContainer.addView(divider)
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
