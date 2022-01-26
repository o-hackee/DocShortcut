package ol.ko.docshortcut

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import ol.ko.docshortcut.utils.ContentResolverUtils
import ol.ko.docshortcut.utils.FileUrisRepository
import ol.ko.docshortcut.work.FileCheckWorker
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class FileCheckWorkerTest: DataStoreBaseTest() {

    companion object {
        const val APPWIDGET_ID = 42
        const val fileUriStringBase = "content://com.android.providers.downloads.documents/document/msf%3A13123"
        const val WIDGET_COUNT = 2
    }

    private fun buildContentUri(appWidgetIdx: Int) = "$fileUriStringBase${appWidgetIdx - APPWIDGET_ID}"

    @Before
    fun setUp() {
        val fileUrisRepository = FileUrisRepository(testDataStore)
        runBlocking {
            for (i in 0 until WIDGET_COUNT) {
                fileUrisRepository.saveUriPref(APPWIDGET_ID + i, buildContentUri(APPWIDGET_ID + i))
            }
        }

        mockkObject(ContentResolverUtils)
    }

    @After
    fun tearDown() = unmockkObject(ContentResolverUtils)

    @Test
    fun workerChecks() {
        every { ContentResolverUtils.uriFileExists(any(), match { it.startsWith(fileUriStringBase) } ) } returns true

        assertEquals(Result.success(), doWork())
    }

    @Test
    fun becomesInvalid() {
        every { ContentResolverUtils.uriFileExists(any(), eq(buildContentUri(APPWIDGET_ID))) } returns true
        every { ContentResolverUtils.uriFileExists(any(), eq(buildContentUri(APPWIDGET_ID + WIDGET_COUNT - 1))) } returns false

        assertEquals(Result.success(), doWork())

        BroadcastIntentUtil.checkBroadcastIntents(intArrayOf(APPWIDGET_ID + WIDGET_COUNT - 1))
    }

    @Test
    // rare case and actually works for the path (not link as e.g. fileUriStringBase) format of content uri
    fun becomesValid() {
        becomesInvalid()

        every { ContentResolverUtils.uriFileExists(any(), eq(buildContentUri(APPWIDGET_ID + WIDGET_COUNT - 1))) } returns true

        assertEquals(Result.success(), doWork())

        BroadcastIntentUtil.checkBroadcastIntents(intArrayOf(APPWIDGET_ID + WIDGET_COUNT - 1))
    }

    private fun doWork(): Result = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val worker = TestListenableWorkerBuilder<FileCheckWorker>(context).build()
        val result = worker.doWork()
        for (i in 0 until WIDGET_COUNT) {
            verify { ContentResolverUtils.uriFileExists(any(), eq(buildContentUri(APPWIDGET_ID + i))) }
        }
        return@runBlocking result
    }
}
