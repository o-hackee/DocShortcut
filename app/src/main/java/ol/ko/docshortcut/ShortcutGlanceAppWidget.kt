package ol.ko.docshortcut

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.util.TypedValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.currentState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import ol.ko.docshortcut.utils.ContentResolverUtils
import ol.ko.docshortcut.work.FileCheckWorker

class ShortcutGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    companion object {
        private const val TAG = "OLKO"
    }

    override val glanceAppWidget: GlanceAppWidget = ShortcutGlanceWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Log.d(TAG, "ShortcutGlanceWidgetReceiver onUpdate()")
        // TODO where belongs former markUriPref() functionality?
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
//        FileCheckWorker.start(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
//        FileCheckWorker.stop(context)
    }
}

class ShortcutGlanceWidget: GlanceAppWidget() {

    companion object {
        val fileUriPreferenceKey = stringPreferencesKey("fileuri-key")
        val isFileUriValidPreferenceKey = booleanPreferencesKey("isvalid-key")

        const val DEFAULT_VALID = true
    }

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    @Composable
    override fun Content() {
        val prefs = currentState<Preferences>()
        val fileUriString = prefs[fileUriPreferenceKey]
        val isValid = prefs[isFileUriValidPreferenceKey] ?: DEFAULT_VALID

        val context = LocalContext.current
        val fileName = fileUriString?.let {
            ContentResolverUtils.getFilenameFromUri(
                context.contentResolver,
                it,
                context.getString(R.string.unknown_document)
            )
        } ?: context.getString(R.string.data_not_found)
        val color = Color(if (isValid) {
            // couldn't obtain the color from the theme
            ContextCompat.getColor(context, R.color.on_primary)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val typedValue = TypedValue()
                context.theme.resolveAttribute(android.R.attr.colorError, typedValue, true)
                typedValue.data
            } else {
                ContextCompat.getColor(context, R.color.error)
            }
        })
        // TODO
        val contentDescription = context.getString(if (isValid) R.string.appwidget_text else R.string.appwidget_invalid_text)

        Text(
            text = fileName,
            style = TextStyle( // TODO smth more specific; AppWidget.InnerView style; at least background
                fontSize = 12.sp,
                color = ColorProvider(color)
            )
        )

        // TODO onClick
    }
}