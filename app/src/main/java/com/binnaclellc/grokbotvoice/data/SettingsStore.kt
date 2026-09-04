package com.binnaclellc.grokbotvoice.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SettingsStore(context: Context) {
    private val prefs: SharedPreferences = runCatching {
        val master = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "grok_bot_voice_secure",
            master,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }.getOrElse {
        context.getSharedPreferences("grok_bot_voice", Context.MODE_PRIVATE)
    }

    var serverUrl: String
        get() = prefs.getString(KEY_URL, DEFAULT_URL)?.trim()?.trimEnd('/') ?: DEFAULT_URL
        set(value) {
            prefs.edit().putString(KEY_URL, value.trim().trimEnd('/')).apply()
        }

    var deviceToken: String
        get() = prefs.getString(KEY_TOKEN, "")?.trim().orEmpty()
        set(value) {
            prefs.edit().putString(KEY_TOKEN, value.trim()).apply()
        }

    companion object {
        const val DEFAULT_URL = "https://mcp.binnaclellc.com"
        private const val KEY_URL = "server_url"
        private const val KEY_TOKEN = "device_token"
    }
}
