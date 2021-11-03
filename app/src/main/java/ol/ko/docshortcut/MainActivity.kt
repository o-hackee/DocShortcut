package ol.ko.docshortcut

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import android.os.Bundle
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        const val PROXY_REQUEST = 1
        const val PROXY_REQUEST_KEY = "PROXY_REQUEST"
        const val EXTRA_APP_WIDGET_ID = "APP_WIDGET_ID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (intent?.getBooleanExtra(PROXY_REQUEST_KEY, false) == true) {
            val appWidgetId = intent?.getIntExtra(EXTRA_APP_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val fileUriString = FileUrisSettings(this).loadUriPref(appWidgetId)
                lifecycleScope.launch {
                    fileUriString.collect {
                        it?.let {
                            val uri = Uri.parse(it)
                            val mime = contentResolver.getType(uri)
                            val intent = Intent().apply {
                                action = Intent.ACTION_VIEW
                                setDataAndType(uri, mime)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            startActivity(intent)
                        }
                    }
                }
            }
        }
    }
}