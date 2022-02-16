package ol.ko.docshortcut

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import ol.ko.docshortcut.work.FileCheckWorker
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class FileCheckWorkerTest {

    @Before
    fun setUp() = mockkObject(GlanceWidgetUtils)

    @After
    fun tearDown() = unmockkObject(GlanceWidgetUtils)

    @Test
    fun workerChecks() {
        assertEquals(Result.success(), doWork())
        coVerify { GlanceWidgetUtils.updateWidgets(any(), true ) }
    }

    private fun doWork(): Result = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val worker = TestListenableWorkerBuilder<FileCheckWorker>(context).build()
        return@runBlocking worker.doWork()
    }
}
