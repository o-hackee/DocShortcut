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
    private lateinit var appWidgetsInitialIds: List<Int>
    private lateinit var fileUrisRepository: FileUrisRepository

    private fun buildContentUri(appWidgetIdx: Int) = "$fileUriStringBase${appWidgetIdx - APPWIDGET_ID}"

    @Before
    fun setUp() {
        appWidgetsInitialIds = List(FILE_COUNT) { APPWIDGET_ID + it }
        fileUrisRepository = FileUrisRepository(testDataStore)
        runBlocking {
            appWidgetsInitialIds.forEach { appWidgetIdx ->
                fileUrisRepository.saveUriPref(appWidgetIdx, buildContentUri(appWidgetIdx))
            }
        }
    }

    @Test
    fun load() = runBlocking {
        checkLoad(appWidgetsInitialIds)
    }

    @Test
    fun save() = runBlocking {
        val appWidgetIdx = appWidgetsInitialIds.last() + 1
        fileUrisRepository.saveUriPref(appWidgetIdx, buildContentUri(appWidgetIdx))
        checkLoad(appWidgetsInitialIds + appWidgetIdx)
    }

    @Test
    fun mark() = runBlocking {
        // mark nonexistent widget record
        fileUrisRepository.markUriPref(appWidgetsInitialIds.last() + 1, false)
        checkLoad(appWidgetsInitialIds)

        // mark as invalid
        fileUrisRepository.markUriPref(appWidgetsInitialIds[1], false)
        checkLoad(appWidgetsInitialIds, appWidgetsInitialIds.map { true }.toMutableList().also { validity -> validity[1] = false })
    }

    @Test
    fun delete() = runBlocking {
        // delete nonexistent widget record
        fileUrisRepository.deleteUriPref(appWidgetsInitialIds.last() + 1)
        checkLoad(appWidgetsInitialIds)

        // delete record
        val appWidgetIdx = appWidgetsInitialIds[1]
        fileUrisRepository.deleteUriPref(appWidgetIdx)
        checkLoad(appWidgetsInitialIds - appWidgetIdx)

        // delete all
        appWidgetsInitialIds.forEach {
            fileUrisRepository.deleteUriPref(it)
        }
        checkLoad(listOf())
    }

    private suspend fun checkLoad(indices: List<Int>, validity: List<Boolean> = indices.map { true }) {
        val allPrefs = fileUrisRepository.allUriPrefs()
        assertEquals(indices.size, allPrefs.size)

        indices.forEachIndexed { i, appWidgetIdx ->
            val fromAll = allPrefs.find { it.first == appWidgetIdx }?.second
            with(fromAll) {
                assertNotNull(this)
                assertEquals(buildContentUri(appWidgetIdx), this!!.uriString)
                assertEquals(validity[i], lastIsValid)
            }
            assertEquals(fromAll, fileUrisRepository.loadUriPref(appWidgetIdx).first())
        }
    }
}