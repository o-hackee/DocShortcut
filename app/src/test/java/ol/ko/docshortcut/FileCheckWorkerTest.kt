package ol.ko.docshortcut

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
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
    private lateinit var appWidgetManagerMock : AppWidgetManager

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

        appWidgetManagerMock = mockk {
            every { updateAppWidget(more(APPWIDGET_ID, andEquals = true), any()) } just Runs
        }
        mockkStatic(AppWidgetManager::class)
        every { AppWidgetManager.getInstance(any()) } returns appWidgetManagerMock
    }

    @After
    fun tearDown() {
        unmockkObject(ContentResolverUtils)

        unmockkStatic(AppWidgetManager::class)
    }

    @Test
    fun workerChecks() {
        every { ContentResolverUtils.uriFileExists(any(), match { it.startsWith(fileUriStringBase) } ) } returns true

        assertEquals(Result.success(), doWork())
    }

    @Test
    fun becomesInvalid() {
        every { ContentResolverUtils.uriFileExists(any(), eq(buildContentUri(APPWIDGET_ID))) } returns true
        every { ContentResolverUtils.uriFileExists(any(), eq(buildContentUri(APPWIDGET_ID + 1))) } returns false

        assertEquals(Result.success(), doWork())

        verify { appWidgetManagerMock.updateAppWidget(APPWIDGET_ID + 1, any()) }
    }

    @Test
    // rare case and actually works for the path (not link as e.g. fileUriStringBase) format of content uri
    fun becomesValid() {
        becomesInvalid()

        every { ContentResolverUtils.uriFileExists(any(), eq(buildContentUri(APPWIDGET_ID + 1))) } returns true

        assertEquals(Result.success(), doWork())

        verify { appWidgetManagerMock.updateAppWidget(APPWIDGET_ID + 1, any()) }
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
