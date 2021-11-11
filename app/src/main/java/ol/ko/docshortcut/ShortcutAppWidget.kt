package ol.ko.docshortcut

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Implementation of App Widget functionality.
 */
class ShortcutAppWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {

            val fileUriString = FileUrisSettings(context).loadUriPref(appWidgetId)
            CoroutineScope(Dispatchers.Main).launch {
                fileUriString.collect {
                    updateAppWidget(context, appWidgetManager, appWidgetId, it)
                }
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            CoroutineScope(Dispatchers.Main).launch {
                FileUrisSettings(context).deleteWidgetLayoutIdPref(appWidgetId)
            }
        }
    }
}

internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, fileUriString: String?) {
    val fileName = fileUriString?.getFilename(context) ?: context.getString(R.string.data_not_found)
    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.shortcut_app_widget).apply {
        setTextViewText(R.id.file_uri, fileName)
        fileUriString?.let {
            setOnClickPendingIntent(
                R.id.container, PendingIntent.getActivity(
                    context,
                    MainActivity.PROXY_REQUEST,
                    Intent(context, MainActivity::class.java)
                        .putExtra(MainActivity.EXTRA_PROXY_REQUEST_KEY, true)
                        .putExtra(MainActivity.EXTRA_URI_KEY, it),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }
    }

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}

private fun String.getFilename(context: Context): String {
    Uri.parse(this)?.let { returnUri ->
        context.contentResolver.query(returnUri, null, null, null, null)
    }?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        cursor.moveToFirst()
        return cursor.getString(nameIndex)
    }
    return context.getString(R.string.unknown_document)
}
