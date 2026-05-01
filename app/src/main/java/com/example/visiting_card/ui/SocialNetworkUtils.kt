package com.example.visiting_card.ui

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class SocialNetwork(val type: String, val username: String)

object SocialNetworkUtils {

    val NETWORK_TYPES = listOf(
        "Telegram", "Instagram", "Facebook",
        "Twitter/X", "LinkedIn", "YouTube", "TikTok", "VK"
    )

    fun getNetworkUrl(type: String, username: String): String {
        val handle = username.removePrefix("@").trim()
        return when (type) {
            "Telegram"  -> "https://t.me/$handle"
            "Instagram" -> "https://instagram.com/$handle"
            "Facebook"  -> "https://facebook.com/$handle"
            "Twitter/X" -> "https://x.com/$handle"
            "LinkedIn"  -> "https://linkedin.com/in/$handle"
            "YouTube"   -> "https://youtube.com/@$handle"
            "TikTok"    -> "https://tiktok.com/@$handle"
            "VK"        -> "https://vk.com/$handle"
            else        -> "https://t.me/$handle"
        }
    }

    fun loadNetworks(prefs: SharedPreferences, key: String): List<SocialNetwork> {
        val json = prefs.getString(key, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                SocialNetwork(obj.getString("type"), obj.getString("username"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveNetworks(prefs: SharedPreferences, key: String, networks: List<SocialNetwork>) {
        val arr = JSONArray()
        networks.forEach { network ->
            arr.put(JSONObject().apply {
                put("type", network.type)
                put("username", network.username)
            })
        }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    fun buildAllNetworksQrText(name: String, networks: List<SocialNetwork>): String {
        val lines = networks.joinToString("\n") { "${it.type}: ${it.username}" }
        return "BEGIN:VCARD\n" +
            "VERSION:3.0\n" +
            "FN:$name\n" +
            "NOTE:$lines\n" +
            "END:VCARD"
    }
}
