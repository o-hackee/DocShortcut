package ol.ko.docshortcut

import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import ol.ko.docshortcut.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "OLKO"

        const val PROXY_REQUEST = 1
        const val EXTRA_PROXY_REQUEST_KEY = "PROXY_REQUEST"
        const val EXTRA_URI_KEY = "URI"
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate $intent")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent?.getBooleanExtra(EXTRA_PROXY_REQUEST_KEY, false) == true) {
            val fileUriString = intent?.getStringExtra(EXTRA_URI_KEY)
            fileUriString?.let {
                viewDocument(it)
                close()
            }
        }
        binding.button.setOnClickListener {
            close()
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart $intent")

        // the problem is only observed on API 29 emulator so far, however it would be nice to have an update feature built-in
        updateAllWidgets()
    }

    private fun close() {
        setResult(RESULT_OK)
        finish()
    }

    private fun viewDocument(fileUriString: String) {
        val uri = Uri.parse(fileUriString)
        val mime = contentResolver.getType(uri)
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (ex: ActivityNotFoundException) {
            Log.e(TAG, "Couldn't start an activity to view a document $fileUriString")
            Toast.makeText(this, "Can't view a document $fileUriString", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateAllWidgets() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(this, ShortcutAppWidget::class.java))
        if (appWidgetIds.isNotEmpty()) {
            sendBroadcast(
                Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).putExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_IDS,
                    appWidgetIds
                )
            )
            Toast.makeText(this, R.string.updating, Toast.LENGTH_SHORT).show()
        }
    }
}