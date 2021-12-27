package ol.ko.docshortcut.utils

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log

object ContentResolverUtils {

    private const val TAG = "OLKO"

    fun getFilenameFromUri(contentResolver: ContentResolver, uriString: String, defaultString: String = ""): String {
        try {
            Uri.parse(uriString)?.let { uri ->
                contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
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
            val looksLikeFileNameIndex = uriString.lastIndexOf("%2F")
            if (looksLikeFileNameIndex != -1) {
                return uriString.substring(looksLikeFileNameIndex + 3)
            }
        } catch (ex: Exception) {
            // if the file can't be found by uri, the cursor will be just empty, but catch the exception anyway
            Log.e(TAG, "get filename failed", ex)
        }
        return defaultString
    }

    fun uriFileExists(contentResolver: ContentResolver, uriString: String?): Boolean {
        if (uriString.isNullOrEmpty())
            return false
        try {
            Uri.parse(uriString)?.let { uri ->
                contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
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