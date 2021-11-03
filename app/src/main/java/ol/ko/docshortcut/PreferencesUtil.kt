package ol.ko.docshortcut

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ol.ko.docshortcut.FileUrisSettings.Companion.PREFS_NAME
import ol.ko.docshortcut.FileUrisDataStore.prefsDataStore


object FileUrisDataStore {
    val Context.prefsDataStore: DataStore<Preferences> by preferencesDataStore(PREFS_NAME)
}

class FileUrisSettings(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        const val PREFS_NAME = "file_uris"
        const val PREF_PREFIX_KEY = "appwidget_"
    }

    private fun keyFileUri(appWidgetId: Int) = stringPreferencesKey(PREF_PREFIX_KEY + appWidgetId)

    internal suspend fun saveUriPref(appWidgetId: Int, fileUri: String) = withContext(ioDispatcher) {
        context.prefsDataStore.edit { prefs ->
            prefs[keyFileUri(appWidgetId)] = fileUri
        }
    }

    internal fun loadUriPref(appWidgetId: Int): Flow<String?> = context.prefsDataStore.data.map { prefs ->
        prefs[keyFileUri(appWidgetId)]
    }

    internal suspend fun deleteWidgetLayoutIdPref(appWidgetId: Int) = withContext(ioDispatcher) {
        context.prefsDataStore.edit { prefs ->
            prefs.minusAssign(keyFileUri(appWidgetId))
        }
    }
}