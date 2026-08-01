package com.example.wavepass.wavepass

import android.content.Context
import java.util.UUID

// Generates and persists a random anonymous identifier for this device,
// used only to recognize repeat encounters — never tied to any real identity.
object DeviceIdentity {

    private const val PREFS_NAME = "wavepass_device_identity"
    private const val KEY_ANONYMOUS_ID = "anonymous_id"

    fun getOrCreateAnonymousId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_ANONYMOUS_ID, null)
        if (existing != null) return existing

        val newId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_ANONYMOUS_ID, newId).apply()
        return newId
    }
}