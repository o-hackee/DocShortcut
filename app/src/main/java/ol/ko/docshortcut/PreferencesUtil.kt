package ol.ko.docshortcut

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ol.ko.docshortcut.FileUrisRepository.Companion.INVALID_MARK

data class SavedUriString(
    val uriString: String,
    val lastIsValid: Boolean
)

object FileUrisDataStore {
    private const val PREFS_NAME = "file_uris"
    private val Context.prefsDataStore: DataStore<Preferences> by preferencesDataStore(PREFS_NAME)

    fun getInstance(context: Context) = context.prefsDataStore
}

class FileUrisRepository(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        const val PREF_PREFIX_KEY = "appwidget_"
        const val INVALID_MARK = "INVALID"
    }

    private fun keyFileUri(appWidgetId: Int) = stringSetPreferencesKey(PREF_PREFIX_KEY + appWidgetId)

    internal suspend fun saveUriPref(appWidgetId: Int, fileUri: String) {
        dataStore.edit { prefs ->
            prefs[keyFileUri(appWidgetId)] = setOf(fileUri)
//            println("saved $appWidgetId $fileUri")
        }
    }

    internal suspend fun markUriPref(appWidgetId: Int, asValid: Boolean) {
        dataStore.edit { prefs ->
            val stringsValue = prefs[keyFileUri(appWidgetId)]
            stringsValue.fileStringUri()?.let {
                prefs[keyFileUri(appWidgetId)] = if (asValid) setOf(it) else setOf(it, INVALID_MARK)
//                println("marked $appWidgetId $asValid")
            }
        }
    }

    internal fun loadUriPref(appWidgetId: Int): Flow<SavedUriString?> = dataStore.data.map { prefs ->
        val stringsValue = prefs[keyFileUri(appWidgetId)]
//        println("loaded $appWidgetId $stringsValue")
        stringsValue.toSavedUriString()
    }

    internal suspend fun deleteUriPref(appWidgetId: Int) {
        dataStore.edit { prefs ->
            prefs.minusAssign(keyFileUri(appWidgetId))
        }
    }

    @Suppress("UNCHECKED_CAST")
    internal suspend fun allUriPrefs(): List<Pair<Int?, SavedUriString?>> {
        val prefs = try {
            dataStore.data.first().asMap()
        } catch (_: NoSuchElementException) {
            return emptyList()
        }
        return prefs.mapNotNull { entry ->
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
