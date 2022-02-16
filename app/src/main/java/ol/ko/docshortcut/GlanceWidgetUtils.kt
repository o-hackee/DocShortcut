package ol.ko.docshortcut

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import ol.ko.docshortcut.utils.ContentResolverUtils

object GlanceWidgetUtils {

    private const val TAG = "OLKO"

    var getGlanceAppWidgetManager: (Context) -> GlanceAppWidgetManager
        @VisibleForTesting
        internal set

    init {
        getGlanceAppWidgetManager = { context -> GlanceAppWidgetManager(context) }
    }

    suspend fun fillInitialWidgetState(context: Context, appWidgetId: Int, fileUriString: String) {

        val glanceId = getGlanceAppWidgetManager(context).getGlanceIds(ShortcutGlanceWidget::class.java).lastOrNull()

        glanceId?.let {
            // being paranoid
            fun extractAppWidgetId(glanceId: GlanceId): Int? {
                return with(glanceId.toString()) {
                    val startIndex = indexOf('(')
                    val endIndex = indexOf(')', startIndex + 1)
                    val inParenthesis = substring(startIndex + 1, endIndex)
                    val pairs = inParenthesis.split(',')
                    pairs.associate { pairString ->
                        val (name, value) = pairString.split('=')
                        name to value
                    }
                }["appWidgetId"]?.toIntOrNull()
            }
            val glanceAppWidgetId = extractAppWidgetId(glanceId)
            if (glanceAppWidgetId != appWidgetId) {
                Log.e(TAG, "glance widget id mismatch $glanceAppWidgetId, expected $appWidgetId")
            }

            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { preferences ->
                preferences.toMutablePreferences()
                    .apply {
                        this[ShortcutGlanceWidget.appWidgetIdPreferenceKey] = appWidgetId
                        this[ShortcutGlanceWidget.fileUriPreferenceKey] = fileUriString
                        this[ShortcutGlanceWidget.isFileUriValidPreferenceKey] = true
                    }
            }
            ShortcutGlanceWidget().update(context, glanceId)
        }
    }

    suspend fun updateWidget(context: Context, appWidgetId: Int) {
        val glanceIds = getGlanceAppWidgetManager(context).getGlanceIds(ShortcutGlanceWidget::class.java)
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
        val glanceIds = getGlanceAppWidgetManager(context).getGlanceIds(ShortcutGlanceWidget::class.java)
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