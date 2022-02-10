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
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalGlanceId
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import ol.ko.docshortcut.ShortcutGlanceWidgetReceiver.Companion.extractAppWidgetId
import ol.ko.docshortcut.ui.MainActivity
import ol.ko.docshortcut.utils.ContentResolverUtils
import ol.ko.docshortcut.work.FileCheckWorker

class ShortcutGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    companion object {
        private const val TAG = "OLKO"

        // TODO creating own stable ID and store that ID in the state attached to the App Widget (if an ID is still needed)
        fun extractAppWidgetId(glanceId: GlanceId): Int? {
            val parseComponents: (String) -> Map<String, String> = {
                val startIndex = it.indexOf('(')
                val endIndex = it.indexOf(')', startIndex + 1)
                val inParenthesis = it.substring(startIndex + 1, endIndex)
                val pairs = inParenthesis.split(',')
                pairs.associate { pairString ->
                    val (name, value) = pairString.split('=')
                    name to value
                }
            }
            return parseComponents(glanceId.toString())["appWidgetId"]?.toIntOrNull()
        }
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
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.app_widget_res_background))
                .appWidgetBackground()
                .appWidgetBackgroundRadius()
                .padding(R.dimen.app_widget_padding)
        ) {
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
            // only supported by images, have to adapt the test
            val contentDescription = context.getString(if (isValid) R.string.appwidget_text else R.string.appwidget_invalid_text)

            var modifier = GlanceModifier
                .background(ImageProvider(R.drawable.app_widget_inner_view_res_background))
                .fillMaxSize()
                .padding(R.dimen.app_widget_padding)
            fileUriString?.let {
               extractAppWidgetId(LocalGlanceId.current)?.let { appWidgetId ->
                   // might use ActionParameters and TODO remove dependency on appWidgetId
                   val intent = MainActivity.createProxyIntent(context, it, appWidgetId)
                   modifier = modifier.clickable(onClick = actionStartActivity(intent))
               }
            }
            Text(
                text = fileName,
                modifier = modifier,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = ColorProvider(color)
                )
            )
        }
    }

    @Composable
    fun GlanceModifier.appWidgetBackgroundRadius(): GlanceModifier {
        // copied from https://github.com/android/user-interface-samples/tree/glance/AppWidget/glance-widget/src/main/java/com/example/android/glancewidget,
        // but WTH cornerRadius only works on Android S+, else-branch won't have any effect anyway;
        // and if applying system_app_widget_background_radius, don't we need a system_app_widget_inner_radius as well?
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            this.cornerRadius(android.R.dimen.system_app_widget_background_radius)
        } else {
            this.cornerRadius(R.dimen.app_widget_radius)
        }
    }
}