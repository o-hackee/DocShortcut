package ol.ko.docshortcut.ui

import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ol.ko.docshortcut.GlanceWidgetUtils
import ol.ko.docshortcut.R
import ol.ko.docshortcut.databinding.ActivityFilePickerBinding

class FilePickerActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "OLKO"
    }

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private val pickDocument = registerForActivityResult(object : ActivityResultContracts.OpenDocument() {

        override fun createIntent(context: Context, input: Array<out String>): Intent {
            return super.createIntent(context, input).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            }
        }
    }, ::fileSelected)

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
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID)
            return

        binding.button.setOnClickListener {
            try {
                pickDocument.launch(arrayOf("*/*"))
            } catch (ex: ActivityNotFoundException) {
                Log.e(TAG, "Couldn't start an activity to pick a document")
            }
        }
    }

    private fun fileSelected(fileUri: Uri?) {
        if (fileUri == null || appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID)
            return

        contentResolver.takePersistableUriPermission(fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val fileUriString = fileUri.toString()

        // It is the responsibility of the configuration activity to update the app widget
        CoroutineScope(Dispatchers.Main).launch { // lifecycleScope would not work: the job is likely to be canceled when the activity is finished i.e. very soon
            GlanceWidgetUtils.fillInitialWidgetState(this@FilePickerActivity, appWidgetId, fileUriString)
        }

        // Make sure we pass back the original appWidgetId
        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        finish()
    }
}