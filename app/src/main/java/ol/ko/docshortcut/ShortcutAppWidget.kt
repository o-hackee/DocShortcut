package ol.ko.docshortcut

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.RemoteViews

// TODO if not only for private use: go through API 30 & 31
// TODO protestirovat' app update, reboot, file got removed, ...
// some automated tests too?

// TODO ne cleanup-s'a zhe zapisi v shared_prefs pri udalenii itema!

/**
 * Implementation of App Widget functionality.
 */
class ShortcutAppWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {

            val fileUriString = ShortcutSharedPrefsUtil.loadUriPref(context, appWidgetId)
            Log.i("OLKO", "appWidgetId $appWidgetId loaded uri $fileUriString")

            updateAppWidget(context, appWidgetManager, appWidgetId, fileUriString)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, fileUriString: String?) {
    val fileName = fileUriString?.getFilename(context) ?: context.getString(R.string.data_not_found)
    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.shortcut_app_widget).apply {
        setTextViewText(R.id.file_uri, fileName)
        setOnClickPendingIntent(R.id.container, PendingIntent.getActivity(
                context,
                MainActivity.PROXY_REQUEST,
                Intent(context, MainActivity::class.java)
                    .putExtra(MainActivity.PROXY_REQUEST_KEY, true)
                    .putExtra(MainActivity.EXTRA_APP_WIDGET_ID, appWidgetId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
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
