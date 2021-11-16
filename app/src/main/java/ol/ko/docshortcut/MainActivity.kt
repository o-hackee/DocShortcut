package ol.ko.docshortcut

import android.content.ActivityNotFoundException
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
        Log.d(TAG, "onCreate ${intent.action} ${intent.extras?.keySet()?.joinToString()}")

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
        Log.d(TAG, "onStart ${intent.action} ${intent.extras}")

        // the problem is steadily observed on API 29 emulator and sporadically - on real device
        ShortcutWidgetUtils.requestWidgetsUpdate(this)
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
}