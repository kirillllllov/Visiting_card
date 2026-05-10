package com.example.visiting_card.ui

data class ProfileData(
    val id: String,
    var label: String,
    var fullName: String = "",
    var position: String = "",
    var phone: String = "",
    var email: String = "",
    var about: String = "",
    var profileImageUri: String? = null,
    var showPosition: Boolean = true,
    var showPhone: Boolean = true,
    var showLogo: Boolean = true,
    var showSocial: Boolean = false,
    var selectedSocialIndex: Int = -1,
    var socialNetworks: String = "[]",
    var cardBgColor1: String = "",
    var cardBgColor2: String = "",
    var cardTextColor: String = "",
    var cardTemplate: Int = 0,
    var logoImageUri: String? = null
)
