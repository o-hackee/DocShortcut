package ol.ko.docshortcut

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ol.ko.docshortcut.FileUrisSettings.Companion.PREFS_NAME
import ol.ko.docshortcut.FileUrisDataStore.prefsDataStore
import ol.ko.docshortcut.FileUrisSettings.Companion.INVALID_MARK

data class SavedUriString(
    val uriString: String,
    val lastIsValid: Boolean
)

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
        const val INVALID_MARK = "INVALID"
    }

    private fun keyFileUri(appWidgetId: Int) = stringSetPreferencesKey(PREF_PREFIX_KEY + appWidgetId)

    internal suspend fun saveUriPref(appWidgetId: Int, fileUri: String) = withContext(ioDispatcher) {
        context.prefsDataStore.edit { prefs ->
            prefs[keyFileUri(appWidgetId)] = setOf(fileUri)
        }
    }

    internal suspend fun markUriPref(appWidgetId: Int, asValid: Boolean) = withContext(ioDispatcher) {
        context.prefsDataStore.edit { prefs ->
            val stringsValue = prefs[keyFileUri(appWidgetId)]
            stringsValue.fileStringUri()?.let {
                prefs[keyFileUri(appWidgetId)] = if (asValid) setOf(it) else setOf(it, INVALID_MARK)
            }
        }
    }

    internal fun loadUriPref(appWidgetId: Int): Flow<SavedUriString?> = context.prefsDataStore.data.map { prefs ->
        val stringsValue = prefs[keyFileUri(appWidgetId)]
        stringsValue.toSavedUriString()
    }

    internal suspend fun deleteUriPref(appWidgetId: Int) = withContext(ioDispatcher) {
        context.prefsDataStore.edit { prefs ->
            prefs.minusAssign(keyFileUri(appWidgetId))
        }
    }

    @Suppress("UNCHECKED_CAST")
    internal fun allUriPrefs(): Flow<List<Pair<Int?, SavedUriString?>>> =
        context.prefsDataStore.data.map { prefs ->
            prefs.asMap().mapNotNull { entry ->
                if (entry.key.name.startsWith(PREF_PREFIX_KEY)) {
                    entry.key.name.substring(PREF_PREFIX_KEY.length).toIntOrNull() to (entry.value as? Set<String>).toSavedUriString()
                } else
                    null
            }
        }
}

fun Set<String>?.fileStringUri(): String? {
    return this?.let { strings ->
        if (strings.count() == 1) strings.firstOrNull() else {
            strings.firstOrNull { it != INVALID_MARK }
        }
    }
}

fun Set<String>?.toSavedUriString(): SavedUriString? {
    return this?.let { strings ->
        val fileStringUri = fileStringUri()
        fileStringUri?.let {
            SavedUriString(fileStringUri, strings.count() == 1)
        }
    }
}
