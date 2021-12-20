package ol.ko.docshortcut

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class TestActivity : AppCompatActivity() {

    private lateinit var setFileUri: (Uri?) -> Unit

    private val createDocument = registerForActivityResult(object : ActivityResultContracts.CreateDocument() {

        override fun createIntent(context: Context, input: String): Intent {
            return super.createIntent(context, input).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/pdf"
            }
        }
    }, ::savingFile)

    private fun savingFile(uri: Uri) = setFileUri(uri)

    fun launchDocumentCreator(fileName: String, obtainFileUri: (Uri?) -> Unit) {
        setFileUri = obtainFileUri
        createDocument.launch(fileName)
    }
}