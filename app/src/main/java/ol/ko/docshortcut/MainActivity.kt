package ol.ko.docshortcut

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {

    companion object {
        const val PROXY_REQUEST = 1
        const val EXTRA_PROXY_REQUEST_KEY = "PROXY_REQUEST"
        const val EXTRA_URI_KEY = "URI"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (intent?.getBooleanExtra(EXTRA_PROXY_REQUEST_KEY, false) == true) {
            val fileUriString = intent?.getStringExtra(EXTRA_URI_KEY)
            fileUriString?.let {
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