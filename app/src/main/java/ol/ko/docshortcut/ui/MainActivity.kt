package ol.ko.docshortcut.ui

import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ol.ko.docshortcut.GlanceWidgetUtils
import ol.ko.docshortcut.R
import ol.ko.docshortcut.databinding.ActivityMainBinding
import java.io.FileNotFoundException

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "OLKO"

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

        // the problem with stale UI ("Tap to update", i.e. appwidget_text)
        // is steadily observed on API 29 emulator and sporadically - on real device
        lifecycleScope.launch {
            // alternatively start one-time work request
            GlanceWidgetUtils.updateWidgets(this@MainActivity, onlyIfValidityChanged = false)
        }
    }

    private fun close() {
        setResult(RESULT_OK)
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
            Toast.makeText(this, "Document $fileUriString might have been altered, please recreate the widget", Toast.LENGTH_LONG).show()
        } catch (ex: Exception) {
            Log.e(TAG, "$fileUriString: another exception", ex)
            Toast.makeText(this, "An error occurred while trying to open $fileUriString", Toast.LENGTH_LONG).show()
        }

        // alternatively start one-time work request
        lifecycleScope.launch {
            GlanceWidgetUtils.updateWidget(this@MainActivity, appWidgetId)
        }
    }
}