package ol.ko.docshortcut

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import ol.ko.docshortcut.utils.ContentResolverUtils

// TODO rename the file in the next commit
object GlanceWidgetUtils {

    private const val TAG = "OLKO"

    suspend fun updateWidget(context: Context, appWidgetId: Int) {
        val glanceIds = GlanceAppWidgetManager(context).getGlanceIds(ShortcutGlanceWidget::class.java)
        val glanceId = glanceIds.find {
            val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, it)
            prefs[ShortcutGlanceWidget.appWidgetIdPreferenceKey] == appWidgetId
        }
        glanceId?.let {
            updateValidity(context, it)
            ShortcutGlanceWidget().update(context, glanceId)
        }
    }

    suspend fun updateWidgets(context: Context, onlyIfValidityChanged: Boolean = true) {
        val glanceIds = GlanceAppWidgetManager(context).getGlanceIds(ShortcutGlanceWidget::class.java)
        glanceIds.forEach { glanceId ->
            val validityChanged = updateValidity(context, glanceId)

            if (!onlyIfValidityChanged || validityChanged) {
                ShortcutGlanceWidget().update(context, glanceId)
            }
        }
    }

    private suspend fun updateValidity(context: Context, glanceId: GlanceId): Boolean {
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
        val fileUriString = prefs[ShortcutGlanceWidget.fileUriPreferenceKey]
        val isCurrentlyValid = ContentResolverUtils.uriFileExists(context.contentResolver, fileUriString)
        val validityChanged = isCurrentlyValid != prefs[ShortcutGlanceWidget.isFileUriValidPreferenceKey]
        Log.d(TAG, "$glanceId: isCurrentlyValid $isCurrentlyValid validityChanged $validityChanged")
        if (validityChanged) {
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefsToUpdate ->
                prefsToUpdate.toMutablePreferences().apply {
                    this[ShortcutGlanceWidget.isFileUriValidPreferenceKey] = isCurrentlyValid
                }
            }
        }
        return validityChanged
    }
}