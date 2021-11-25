package ol.ko.docshortcut

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import android.util.TypedValue

object ShortcutWidgetUtils {

    private const val TAG = "OLKO"

    internal fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        fileUriString: String?,
        isValid: Boolean = true
    ) {
        val fileName = fileUriString?.getFilenameFromUri(context) ?: context.getString(R.string.data_not_found)
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

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

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
//            Toast.makeText(context, R.string.updating, Toast.LENGTH_SHORT).show()
        }
    }

    private fun String.getFilenameFromUri(context: Context): String {
        try {
            Uri.parse(this)?.let { uri ->
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            }?.use { cursor ->
                if (cursor.count > 0) {
                    cursor.moveToFirst()
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME) // should be zero, but JIC
                    return cursor.getString(nameIndex)
                }
            }
        } catch (ex: SecurityException) {
            // maybe missing permissions after renaming back and forth the full external storage path
            Log.e(TAG, "get filename failed with security exception", ex)
            val looksLikeFileNameIndex = lastIndexOf("%2F")
            if (looksLikeFileNameIndex != -1) {
                return substring(looksLikeFileNameIndex + 3)
            }
        } catch (ex: Exception) {
            // if the file can't be found by uri, the cursor will be just empty, but catch the exception anyway
            Log.e(TAG, "get filename failed", ex)
        }
        return context.getString(R.string.unknown_document)
    }

    fun String?.uriFileExists(context: Context): Boolean {
        if (isNullOrEmpty())
            return false
        try {
            Uri.parse(this)?.let { uri ->
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            }?.use { cursor ->
                return cursor.count > 0
            }
        } catch (ex: Exception) {
            // can't do much in case of missing permissions after renaming back and forth the full external storage path, just one common catch
            Log.e(TAG, "file check failed", ex)
        }
        return false
    }
}