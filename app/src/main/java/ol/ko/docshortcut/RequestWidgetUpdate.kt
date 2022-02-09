package ol.ko.docshortcut

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

object WidgetUtils {
    internal fun requestWidgetsUpdate(context: Context, appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID)
            appWidgetManager.getAppWidgetIds(ComponentName(context, ShortcutAppWidget::class.java))
        else
            arrayOf(appWidgetId).toIntArray()
        if (appWidgetIds.isNotEmpty()) {
            context.sendBroadcast(
                Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).putExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_IDS,
                    appWidgetIds
                )
            )
        }
    }
}