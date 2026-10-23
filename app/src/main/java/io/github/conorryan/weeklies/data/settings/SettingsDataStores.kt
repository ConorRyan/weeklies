package io.github.conorryan.weeklies.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * The process-wide DataStores backing the settings stores: one DataStore per
 * file, owned by the delegates below. The stores themselves receive their
 * DataStore through the constructor, so tests can substitute their own
 * file-backed store instead of the process-wide one.
 */
internal object SettingsDataStores {

    private val Context.weekliesSettings: DataStore<Preferences> by preferencesDataStore("weeklies.settings")
    private val Context.weekliesSecrets: DataStore<Preferences> by preferencesDataStore("weeklies.secrets")

    fun settings(context: Context): DataStore<Preferences> = context.applicationContext.weekliesSettings
    fun secrets(context: Context): DataStore<Preferences> = context.applicationContext.weekliesSecrets
}
