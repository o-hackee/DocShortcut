package ol.ko.docshortcut

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
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
        const val FILE_COUNT = 1 // TODO
    }

    @Before
    fun setUp() {
        mockkObject(ContentResolverUtils)
        every { ContentResolverUtils.uriFileExists(any(), match { it.startsWith(fileUriStringBase) } ) } returns true
    }

    @After
    fun tearDown() = unmockkObject(ContentResolverUtils)

    @Test
    fun testWorker() = runBlocking {
        val appWidgetIds = IntArray(FILE_COUNT) { i -> APPWIDGET_ID + i }
        val fileUrisRepository = FileUrisRepository(testDataStore)
        for (i in 0 until FILE_COUNT) {
            fileUrisRepository.saveUriPref(appWidgetIds[i], "$fileUriStringBase$i")
        }

        val context = ApplicationProvider.getApplicationContext<Context>()
        val worker = TestListenableWorkerBuilder<FileCheckWorker>(context).build()
        val result = worker.doWork()
        assertEquals(Result.success(), result)
    }
}
