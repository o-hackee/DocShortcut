package ol.ko.docshortcut

import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import ol.ko.docshortcut.databinding.ActivityMainBinding
import java.io.FileNotFoundException

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "OLKO"

        const val PROXY_REQUEST = 1
        const val EXTRA_PROXY_REQUEST_KEY = "PROXY_REQUEST"
        const val EXTRA_URI_KEY = "URI"

        fun createProxyIntent(context: Context, fileUriString: String, appWidgetId: Int ) =
            Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_PROXY_REQUEST_KEY, true)
            .putExtra(EXTRA_URI_KEY, fileUriString)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate ${intent.action} ${intent.extras?.keySet()?.joinToString { "$it: ${intent.extras?.get(it)}" }}")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent?.getBooleanExtra(EXTRA_PROXY_REQUEST_KEY, false) == true) {
            val fileUriString = intent?.getStringExtra(EXTRA_URI_KEY)
            val appWidgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID
            if (fileUriString != null && appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                viewDocument(fileUriString, appWidgetId)
                close()
            }
        }
        binding.button.setOnClickListener {
            close()
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart ${intent.action} ${intent.extras}")

        // the problem is steadily observed on API 29 emulator and sporadically - on real device
        ShortcutWidgetUtils.requestWidgetsUpdate(this)
    }

    private fun close() {
        setResult(RESULT_OK)
        finish()
    }

    private fun viewDocument(fileUriString: String, appWidgetId: Int) {
        val uri = Uri.parse(fileUriString)
        val mime = contentResolver.getType(uri)
        // ACTION_VIEW is actually very common, e.g. opening a renamed file might end up as trying to open a contact
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        }
        try {
            startActivity(intent)
        } catch (ex: ActivityNotFoundException) {
            Log.e(TAG, "Couldn't start an activity to view a document $fileUriString")
            Toast.makeText(this, "Can't view a document $fileUriString", Toast.LENGTH_LONG).show()
        } catch (ex: FileNotFoundException) {
            Log.e(TAG, fileUriString, ex)
            Toast.makeText(this, "Document $fileUriString not found", Toast.LENGTH_LONG).show()
        } catch (ex: SecurityException) {
            Log.e(TAG, "$fileUriString: security exception", ex)
            Toast.makeText(this, "permissions probably have to be re-granted, please recreate the widget for $fileUriString", Toast.LENGTH_LONG).show()
        } catch (ex: Exception) {
            Log.e(TAG, "$fileUriString: another exception", ex)
            Toast.makeText(this, "An error occurred while trying to open $fileUriString", Toast.LENGTH_LONG).show()
        }
        ShortcutWidgetUtils.requestWidgetsUpdate(this, appWidgetId)
    }
}