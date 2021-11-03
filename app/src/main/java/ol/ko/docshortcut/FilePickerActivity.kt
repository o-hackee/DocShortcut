package ol.ko.docshortcut

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ol.ko.docshortcut.databinding.ActivityFilePickerBinding

class FilePickerActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_DOC_GET = 1
    }

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the result to CANCELED. This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED)

        val binding = ActivityFilePickerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = getString(R.string.select_document)

        // Find the widget id from the intent.
        appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID

        binding.button.setOnClickListener {
            selectDocument()
        }
    }

    private fun selectDocument() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        }
        if (intent.resolveActivity(packageManager) != null) {
            // TODO
            startActivityForResult(Intent.createChooser(intent, "Select a file"), REQUEST_DOC_GET)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != REQUEST_DOC_GET || resultCode != Activity.RESULT_OK)
            return

        val fileUri = data?.data
        fileUri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID)
            return
        val fileUriString = fileUri?.toString()
        fileUriString?.let {
            lifecycleScope.launch {
                FileUrisSettings(this@FilePickerActivity).saveUriPref(appWidgetId, it)
            }
        }

        // It is the responsibility of the configuration activity to update the app widget
        val appWidgetManager = AppWidgetManager.getInstance(this)
        updateAppWidget(this, appWidgetManager, appWidgetId, fileUriString)

        // Make sure we pass back the original appWidgetId
        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        finish()
    }

}