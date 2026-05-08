package com.example.visiting_card.ui

import android.content.SharedPreferences
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_ABOUT
import com.example.visiting_card.ui.EditDataActivity.Companion.KEY_EMAIL
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
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object ProfileManager {

    private const val KEY_PROFILES  = "mp_profiles"
    private const val KEY_ACTIVE_ID = "mp_active_id"
    const val DEFAULT_LABEL         = "Основной"

    // ── Bootstrap: call once in onCreate ──────────────────────────────────
    // Creates the first profile from existing main prefs if this is the first launch
    // with multi-profile support. Returns the active profile ID.
    fun initIfNeeded(prefs: SharedPreferences): String {
        val existingId = prefs.getString(KEY_ACTIVE_ID, null)
        if (existingId != null) return existingId

        // First time: snapshot existing data into a default profile
        val id      = UUID.randomUUID().toString()
        val profile = snapshotFromMainPrefs(prefs, id, DEFAULT_LABEL)
        saveProfiles(prefs, listOf(profile))
        prefs.edit().putString(KEY_ACTIVE_ID, id).apply()
        return id
    }

    // ── Load / save all profiles ──────────────────────────────────────────
    fun getAllProfiles(prefs: SharedPreferences): List<ProfileData> {
        val json = prefs.getString(KEY_PROFILES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i -> fromJson(arr.getJSONObject(i)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveProfiles(prefs: SharedPreferences, profiles: List<ProfileData>) {
        val arr = JSONArray()
        profiles.forEach { arr.put(toJson(it)) }
        prefs.edit().putString(KEY_PROFILES, arr.toString()).apply()
    }

    fun getActiveId(prefs: SharedPreferences): String =
        prefs.getString(KEY_ACTIVE_ID, null) ?: initIfNeeded(prefs)

    // ── Sync the active profile's stored entry from current main prefs ────
    // Call this whenever the user edits card info, so the stored copy is up-to-date.
    fun syncActiveFromMainPrefs(prefs: SharedPreferences, activeId: String, activeLabel: String) {
        val profiles    = getAllProfiles(prefs).toMutableList()
        val snapshot    = snapshotFromMainPrefs(prefs, activeId, activeLabel)
        val idx         = profiles.indexOfFirst { it.id == activeId }
        if (idx >= 0) profiles[idx] = snapshot else profiles.add(snapshot)
        saveProfiles(prefs, profiles)
    }

    // ── Switch to an existing profile ─────────────────────────────────────
    // Saves current state, loads the target, returns it.
    fun switchTo(
        prefs: SharedPreferences,
        currentActiveId: String,
        currentLabel: String,
        targetId: String
    ): ProfileData? {
        // First, save current main prefs into the profiles list
        syncActiveFromMainPrefs(prefs, currentActiveId, currentLabel)

        val profiles = getAllProfiles(prefs)
        val target   = profiles.firstOrNull { it.id == targetId } ?: return null

        applyToMainPrefs(prefs, target)
        prefs.edit().putString(KEY_ACTIVE_ID, targetId).apply()
        return target
    }

    // ── Create a brand-new empty profile and switch to it ─────────────────
    fun createAndSwitchTo(
        prefs: SharedPreferences,
        currentActiveId: String,
        currentLabel: String,
        newLabel: String
    ): ProfileData {
        // Save current state
        syncActiveFromMainPrefs(prefs, currentActiveId, currentLabel)

        // Create empty profile
        val newProfile = ProfileData(id = UUID.randomUUID().toString(), label = newLabel)
        val profiles   = getAllProfiles(prefs).toMutableList()
        profiles.add(newProfile)
        saveProfiles(prefs, profiles)

        // Activate it (wipes main prefs to empty)
        applyToMainPrefs(prefs, newProfile)
        prefs.edit().putString(KEY_ACTIVE_ID, newProfile.id).apply()
        return newProfile
    }

    // ── Delete a profile (only if it's not the last one) ──────────────────
    // If deleting the active profile, switches to the first remaining one.
    // Returns the new active profile (same as before if non-active was deleted).
    fun deleteProfile(
        prefs: SharedPreferences,
        idToDelete: String,
        currentActiveId: String,
        currentLabel: String
    ): ProfileData? {
        syncActiveFromMainPrefs(prefs, currentActiveId, currentLabel)
        val profiles = getAllProfiles(prefs).toMutableList()
        if (profiles.size <= 1) return null   // refuse to delete the last one

        profiles.removeAll { it.id == idToDelete }
        saveProfiles(prefs, profiles)

        // If we deleted the active profile, activate the first remaining
        return if (idToDelete == currentActiveId) {
            val next = profiles.first()
            applyToMainPrefs(prefs, next)
            prefs.edit().putString(KEY_ACTIVE_ID, next.id).apply()
            next
        } else {
            profiles.firstOrNull { it.id == currentActiveId }
        }
    }

    // ── Rename a profile ──────────────────────────────────────────────────
    fun renameProfile(prefs: SharedPreferences, id: String, newLabel: String) {
        val profiles = getAllProfiles(prefs).toMutableList()
        val idx      = profiles.indexOfFirst { it.id == id }
        if (idx >= 0) {
            profiles[idx] = profiles[idx].copy(label = newLabel)
            saveProfiles(prefs, profiles)
        }
    }

    // ── Snapshot current main prefs → ProfileData ─────────────────────────
    fun snapshotFromMainPrefs(prefs: SharedPreferences, id: String, label: String) = ProfileData(
        id                  = id,
        label               = label,
        fullName            = prefs.getString(KEY_FULL_NAME, "") ?: "",
        position            = prefs.getString(KEY_POSITION, "") ?: "",
        phone               = prefs.getString(KEY_PHONE, "") ?: "",
        email               = prefs.getString(KEY_EMAIL, "") ?: "",
        about               = prefs.getString(KEY_ABOUT, "") ?: "",
        profileImageUri     = prefs.getString(KEY_PROFILE_IMAGE_URI, null),
        showPosition        = prefs.getBoolean(KEY_SHOW_POSITION, true),
        showPhone           = prefs.getBoolean(KEY_SHOW_PHONE, true),
        showLogo            = prefs.getBoolean(KEY_SHOW_LOGO, true),
        showSocial          = prefs.getBoolean(KEY_SHOW_SOCIAL, false),
        selectedSocialIndex = prefs.getInt(KEY_SELECTED_SOCIAL_INDEX, -1),
        socialNetworks      = prefs.getString(KEY_SOCIAL_NETWORKS, "[]") ?: "[]"
    )

    // ── Apply a ProfileData → main prefs ──────────────────────────────────
    fun applyToMainPrefs(prefs: SharedPreferences, profile: ProfileData) {
        prefs.edit()
            .putString(KEY_FULL_NAME, profile.fullName)
            .putString(KEY_POSITION, profile.position)
            .putString(KEY_PHONE, profile.phone)
            .putString(KEY_EMAIL, profile.email)
            .putString(KEY_ABOUT, profile.about)
            .putString(KEY_PROFILE_IMAGE_URI, profile.profileImageUri)
            .putBoolean(KEY_SHOW_POSITION, profile.showPosition)
            .putBoolean(KEY_SHOW_PHONE, profile.showPhone)
            .putBoolean(KEY_SHOW_LOGO, profile.showLogo)
            .putBoolean(KEY_SHOW_SOCIAL, profile.showSocial)
            .putInt(KEY_SELECTED_SOCIAL_INDEX, profile.selectedSocialIndex)
            .putString(KEY_SOCIAL_NETWORKS, profile.socialNetworks)
            .apply()
    }

    // ── JSON helpers ──────────────────────────────────────────────────────
    private fun toJson(p: ProfileData) = JSONObject().apply {
        put("id", p.id)
        put("label", p.label)
        put("fullName", p.fullName)
        put("position", p.position)
        put("phone", p.phone)
        put("email", p.email)
        put("about", p.about)
        put("profileImageUri", p.profileImageUri ?: "")
        put("showPosition", p.showPosition)
        put("showPhone", p.showPhone)
        put("showLogo", p.showLogo)
        put("showSocial", p.showSocial)
        put("selectedSocialIndex", p.selectedSocialIndex)
        put("socialNetworks", p.socialNetworks)
    }

    private fun fromJson(o: JSONObject) = ProfileData(
        id                  = o.getString("id"),
        label               = o.optString("label", DEFAULT_LABEL),
        fullName            = o.optString("fullName", ""),
        position            = o.optString("position", ""),
        phone               = o.optString("phone", ""),
        email               = o.optString("email", ""),
        about               = o.optString("about", ""),
        profileImageUri     = o.optString("profileImageUri").ifEmpty { null },
        showPosition        = o.optBoolean("showPosition", true),
        showPhone           = o.optBoolean("showPhone", true),
        showLogo            = o.optBoolean("showLogo", true),
        showSocial          = o.optBoolean("showSocial", false),
        selectedSocialIndex = o.optInt("selectedSocialIndex", -1),
        socialNetworks      = o.optString("socialNetworks", "[]")
    )
}
