package ol.ko.docshortcut.activities

import android.app.Activity
import android.app.Instrumentation
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import ol.ko.docshortcut.GlanceWidgetUtils
import ol.ko.docshortcut.ui.FilePickerActivity
import ol.ko.docshortcut.R
import org.hamcrest.core.AllOf.allOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class FilePickerActivityTest {
    companion object {
        const val APPWIDGET_ID = 42
    }

    private lateinit var context: Context
    private lateinit var scenario: ActivityScenario<FilePickerActivity>

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        Intents.init()
        scenario = launchPickerActivity()
    }

    @After
    fun tearDown() {
        scenario.close()
        Intents.release()
    }

    @Test
    fun testOpenDocumentIntent() {
        Espresso.onView(withId(R.id.button)).perform(click())

        Intents.intended(
            allOf(
                IntentMatchers.hasAction(Intent.ACTION_OPEN_DOCUMENT),
                IntentMatchers.hasType("*/*")
            )
        )
        Intents.assertNoUnverifiedIntents()
    }

    @Test
    fun testDocumentOpenedRefresh() {
        val fileUri = Uri.parse("content://com.android.providers.downloads.documents/document/msf%3A131233")
        Intents.intending(IntentMatchers.hasAction(Intent.ACTION_OPEN_DOCUMENT))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(fileUri)))

        mockkObject(GlanceWidgetUtils)

        Espresso.onView(withId(R.id.button)).perform(click())

        val persistedPermissions = context.contentResolver.persistedUriPermissions
        assertEquals(1, persistedPermissions.size)
        with(persistedPermissions.first()) {
            assertEquals(fileUri, uri)
            assert(isReadPermission)
        }

        coVerify { GlanceWidgetUtils.fillInitialWidgetState(any(), APPWIDGET_ID, fileUri.toString()) }

        with(scenario.result) {
            assertEquals(Activity.RESULT_OK, resultCode)
            assertEquals(
                APPWIDGET_ID,
                resultData.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            )
        }

        unmockkObject(GlanceWidgetUtils)
    }

    @Test
    fun testDocumentOpenCanceled() {
        Intents.intending(IntentMatchers.hasAction(Intent.ACTION_OPEN_DOCUMENT))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null))

        Espresso.onView(withId(R.id.button)).perform(click())

        scenario.moveToState(Lifecycle.State.DESTROYED)
        with(scenario.result) {
            assertEquals(Activity.RESULT_CANCELED, resultCode)
            assertNull(resultData)
        }
    }

    private fun launchPickerActivity(): ActivityScenario<FilePickerActivity> =
        launchActivity<FilePickerActivity>(
            Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                // need to help robolectric, otherwise the activity for the intent's action is not resolved
                .setComponent(ComponentName(context, FilePickerActivity::class.java))
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, APPWIDGET_ID)
        ).also {
            assertEquals(Lifecycle.State.RESUMED, it.state)
        }
}
