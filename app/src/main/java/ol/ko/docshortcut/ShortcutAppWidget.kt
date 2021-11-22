package ol.ko.docshortcut

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import ol.ko.docshortcut.ShortcutWidgetUtils.uriFileExists

/**
 * Implementation of App Widget functionality.
 */
class ShortcutAppWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {

            val fileUrisSettings = FileUrisSettings(context)
            val fileUriStringFlow = fileUrisSettings.loadUriPref(appWidgetId)
            CoroutineScope(Dispatchers.IO).launch {
                fileUriStringFlow.collect { savedUriString ->
                    val isCurrentlyValid = savedUriString?.uriString.uriFileExists(context)
                    CoroutineScope(Dispatchers.Main).launch {
                        ShortcutWidgetUtils.updateAppWidget(context, appWidgetManager, appWidgetId, savedUriString?.uriString, isCurrentlyValid)
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
                FileUrisSettings(context).deleteUriPref(appWidgetId)
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
