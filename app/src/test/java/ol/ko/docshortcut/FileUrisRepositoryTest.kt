package ol.ko.docshortcut

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class FileUrisRepositoryTest: DataStoreBaseTest() {

    companion object {
        const val APPWIDGET_ID = 42
        const val fileUriStringBase = "content://com.android.providers.downloads.documents/document/msf%3A13123"
        const val FILE_COUNT = 3
    }
    private lateinit var fileUrisRepository: FileUrisRepository

    private fun buildContentUri(idx: Int) = "$fileUriStringBase$idx"

    @Before
    fun setUp() {
        val appWidgetIds = IntArray(FILE_COUNT) { i -> APPWIDGET_ID + i }
        fileUrisRepository = FileUrisRepository(testDataStore)
        runBlocking {
            for (i in 0 until FILE_COUNT) {
                fileUrisRepository.saveUriPref(appWidgetIds[i], buildContentUri(i))
            }
        }
    }

    @Test
    fun load() = runBlocking{
        val allPrefs = fileUrisRepository.allUriPrefs()
        assertEquals(FILE_COUNT, allPrefs.size)

        for (i in 0 until FILE_COUNT) {
            val fromAll = allPrefs.find { it.first == APPWIDGET_ID + i }?.second
            with (fromAll) {
                assertNotNull(this)
                assertEquals(buildContentUri(i), this!!.uriString)
                assert(lastIsValid)
            }
            assertEquals(fromAll, fileUrisRepository.loadUriPref(APPWIDGET_ID + i).first())
        }
    }

    @Test
    fun mark() {
    }

    @Test
    fun delete() {
    }
}