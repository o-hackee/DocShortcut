package ol.ko.docshortcut

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.util.Log
import android.widget.RemoteViews

// TODO if not only for private use: go through API 30 & 31

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
    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.shortcut_app_widget)
    views.setTextViewText(R.id.file_uri, fileUriString?: context.getString(R.string.data_not_found))

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}