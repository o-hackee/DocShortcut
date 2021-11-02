package ol.ko.docshortcut

import android.content.Context
import androidx.core.content.edit

object ShortcutSharedPrefsUtil {
    private const val PREFS_NAME = "fileUris"
    private const val PREF_PREFIX_KEY = "appwidget_"

    private fun sharedPrefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    internal fun saveUriPref(context: Context, appWidgetId: Int, fileUri: String?) {
        sharedPrefs(context).edit {
            putString(PREF_PREFIX_KEY + appWidgetId, fileUri)
        }
    }

    internal fun loadUriPref(context: Context, appWidgetId: Int): String? =
        sharedPrefs(context).getString(PREF_PREFIX_KEY + appWidgetId, null)

    internal fun deleteWidgetLayoutIdPref(context: Context, appWidgetId: Int) {
        sharedPrefs(context).edit {
            remove(PREF_PREFIX_KEY + appWidgetId)
        }
    }
}