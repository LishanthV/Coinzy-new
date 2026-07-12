package com.example.data

import android.content.Context
import android.content.SharedPreferences

class AuthManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("coinzy_auth_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_REGISTERED = "is_registered"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
        private const val KEY_PASSWORD_HASH = "password_hash"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_AVATAR_INDEX = "avatar_index"
        private const val KEY_CURRENCY = "currency"
    }

    val isRegistered: Boolean
        get() = prefs.getBoolean(KEY_IS_REGISTERED, false)

    val username: String
        get() = prefs.getString(KEY_USERNAME, "") ?: ""

    val email: String
        get() = prefs.getString(KEY_EMAIL, "") ?: ""

    val isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    val avatarIndex: Int
        get() = prefs.getInt(KEY_AVATAR_INDEX, 0)

    val currency: String
        get() = prefs.getString(KEY_CURRENCY, "$") ?: "$"

    fun register(username: String, email: String, passwordHash: String, avatarIndex: Int, currency: String): Boolean {
        return prefs.edit()
            .putBoolean(KEY_IS_REGISTERED, true)
            .putString(KEY_USERNAME, username)
            .putString(KEY_EMAIL, email)
            .putString(KEY_PASSWORD_HASH, passwordHash)
            .putInt(KEY_AVATAR_INDEX, avatarIndex)
            .putString(KEY_CURRENCY, currency)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .commit()
    }

    fun login(passwordHash: String): Boolean {
        val storedHash = prefs.getString(KEY_PASSWORD_HASH, "") ?: ""
        if (storedHash == passwordHash) {
            prefs.edit().putBoolean(KEY_IS_LOGGED_IN, true).apply()
            return true
        }
        return false
    }

    fun logout() {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, false).apply()
    }

    fun updateProfile(username: String, email: String, avatarIndex: Int, currency: String) {
        prefs.edit()
            .putString(KEY_USERNAME, username)
            .putString(KEY_EMAIL, email)
            .putInt(KEY_AVATAR_INDEX, avatarIndex)
            .putString(KEY_CURRENCY, currency)
            .apply()
    }
}
