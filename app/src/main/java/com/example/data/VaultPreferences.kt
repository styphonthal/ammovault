package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.vaultDataStore: DataStore<Preferences> by preferencesDataStore(name = "ammovault_prefs")

data class VaultPrivacySettings(
    val hideDollarValues: Boolean = false,
    val biometricLockEnabled: Boolean = false
)

class VaultPreferences(private val context: Context) {

    private object Keys {
        val HIDE_DOLLAR_VALUES = booleanPreferencesKey("hide_dollar_values")
        val BIOMETRIC_LOCK_ENABLED = booleanPreferencesKey("biometric_lock_enabled")
    }

    val privacySettings: Flow<VaultPrivacySettings> = context.vaultDataStore.data.map { prefs ->
        VaultPrivacySettings(
            hideDollarValues = prefs[Keys.HIDE_DOLLAR_VALUES] ?: false,
            biometricLockEnabled = prefs[Keys.BIOMETRIC_LOCK_ENABLED] ?: false
        )
    }

    suspend fun setHideDollarValues(enabled: Boolean) {
        context.vaultDataStore.edit { prefs ->
            prefs[Keys.HIDE_DOLLAR_VALUES] = enabled
        }
    }

    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        context.vaultDataStore.edit { prefs ->
            prefs[Keys.BIOMETRIC_LOCK_ENABLED] = enabled
        }
    }
}
