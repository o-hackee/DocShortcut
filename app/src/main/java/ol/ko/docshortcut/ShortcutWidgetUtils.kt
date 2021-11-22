package ol.ko.docshortcut

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.RemoteViews
import androidx.core.content.ContextCompat

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
                // couldn't obtain the colors from the theme
                val color = ContextCompat.getColor(context, if (isValid) R.color.on_primary else R.color.error_workaround)
                setTextColor(this, color)
            }
            fileUriString?.let {
                setOnClickPendingIntent(
                    R.id.container, PendingIntent.getActivity(
                        context,
                        MainActivity.PROXY_REQUEST + appWidgetId,
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

    internal fun requestWidgetsUpdate(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, ShortcutAppWidget::class.java))
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