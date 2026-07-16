package com.skofqq.domainmanager.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PrefsStore(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val uiPrefs: SharedPreferences =
        context.getSharedPreferences(UI_PREFS_NAME, Context.MODE_PRIVATE)

    var routerHost: String
        get() = prefs.getString(KEY_HOST, "192.168.1.1") ?: "192.168.1.1"
        set(value) { prefs.edit().putString(KEY_HOST, value).apply() }

    var routerPort: Int
        get() = prefs.getInt(KEY_PORT, 80)
        set(value) { prefs.edit().putInt(KEY_PORT, value).apply() }

    var token: String
        get() = prefs.getString(KEY_TOKEN, "") ?: ""
        set(value) { prefs.edit().putString(KEY_TOKEN, value).apply() }

    var defaultTarget: String
        get() = prefs.getString(KEY_TARGET, "both") ?: "both"
        set(value) { prefs.edit().putString(KEY_TARGET, value).apply() }

    var useDynamicColor: Boolean
        get() = uiPrefs.getBoolean(KEY_DYNAMIC_COLOR, true)
        set(value) { uiPrefs.edit().putBoolean(KEY_DYNAMIC_COLOR, value).apply() }

    companion object {
        private const val PREFS_NAME = "domain_manager_secure_prefs"
        private const val UI_PREFS_NAME = "domain_manager_ui_prefs"
        private const val KEY_HOST = "router_host"
        private const val KEY_PORT = "router_port"
        private const val KEY_TOKEN = "token"
        private const val KEY_TARGET = "default_target"
        private const val KEY_DYNAMIC_COLOR = "use_dynamic_color"
    }
}
