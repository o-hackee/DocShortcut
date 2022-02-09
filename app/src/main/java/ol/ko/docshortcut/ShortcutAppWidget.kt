package ol.ko.docshortcut

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Build
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import ol.ko.docshortcut.ui.MainActivity
import ol.ko.docshortcut.utils.ContentResolverUtils
import ol.ko.docshortcut.utils.FileUrisDataStore
import ol.ko.docshortcut.utils.FileUrisRepository
import ol.ko.docshortcut.work.FileCheckWorker

/**
 * Implementation of App Widget functionality.
 */
class ShortcutAppWidget : AppWidgetProvider() {

    companion object {

        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            fileUriString: String?,
            isValid: Boolean = true
        ) {
            val fileName = fileUriString?.let {
                ContentResolverUtils.getFilenameFromUri(
                    context.contentResolver,
                    it,
                    context.getString(R.string.unknown_document)
                )
            } ?: context.getString(R.string.data_not_found)
            // Construct the RemoteViews object
            val views = RemoteViews(context.packageName, R.layout.shortcut_app_widget).apply {
                with (R.id.file_uri) {
                    setTextViewText(this, fileName)

                    val color = if (isValid) {
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
                    }
                    setTextColor(this, color)

                    if (isValid) {
                        setContentDescription(this, context.getString(R.string.appwidget_text))
                    } else {
                        setContentDescription(this, context.getString(R.string.appwidget_invalid_text))
                    }
                }
                fileUriString?.let {
                    setOnClickPendingIntent(
                        R.id.container, PendingIntent.getActivity(
                            context,
                            MainActivity.PROXY_REQUEST + appWidgetId,
                            MainActivity.createProxyIntent(context, it, appWidgetId),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                }
            }

//            println("updating")
            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {

            val fileUrisSettings = FileUrisRepository(FileUrisDataStore.getInstance(context))
            val fileUriStringFlow = fileUrisSettings.loadUriPref(appWidgetId)
            CoroutineScope(Dispatchers.IO).launch {
                fileUriStringFlow.collect { savedUriString ->
                    val isCurrentlyValid = ContentResolverUtils.uriFileExists(context.contentResolver, savedUriString?.uriString)
                    CoroutineScope(Dispatchers.Main).launch {
                        updateAppWidget(context, appWidgetManager, appWidgetId, savedUriString?.uriString, isCurrentlyValid)
                    }
                    savedUriString?.let {
                        if (isCurrentlyValid != it.lastIsValid) {
                            fileUrisSettings.markUriPref(appWidgetId, isCurrentlyValid)
                        }
                    }
                }
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            CoroutineScope(Dispatchers.Main).launch {
                FileUrisRepository(FileUrisDataStore.getInstance(context)).deleteUriPref(appWidgetId)
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        FileCheckWorker.start(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        FileCheckWorker.stop(context)
    }
}
