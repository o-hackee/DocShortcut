package ol.ko.docshortcut

import android.app.Activity
import android.app.Application
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
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.Runs
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.AllOf.allOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
}

@RunWith(AndroidJUnit4::class)
class MainActivityTests {
    companion object {
        const val APPWIDGET_ID = 42
    }

    @Test
    fun testViewIntent() {
        Intents.init()

        val fileUriString = "content://com.android.providers.downloads.documents/document/msf%3A131233"
        val scenario = launchActivity<MainActivity>(MainActivity.createProxyIntent(
            ApplicationProvider.getApplicationContext(),
            fileUriString,
            APPWIDGET_ID
        ))
        assertEquals(Lifecycle.State.DESTROYED, scenario.state)
        assertEquals(Activity.RESULT_OK, scenario.result.resultCode)

        Intents.intended(allOf(
            IntentMatchers.hasAction(Intent.ACTION_VIEW),
            IntentMatchers.hasData(fileUriString),
            IntentMatchers.hasFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        ))
        Intents.assertNoUnverifiedIntents()
        Intents.release()

        // also make sure the widget is being refreshed (e.g. if the URI is for some reason invalid, the user would
        // expect to the the error indication in the widget UI as well after unsuccessful document view)
        checkBroadcastIntents(IntArray(1) { APPWIDGET_ID })
    }

    @Test
    fun testRefreshIntent() {
        val appWidgetIds = IntArray(3) { i -> APPWIDGET_ID + i }

        mockkStatic(AppWidgetManager::class)
        every { AppWidgetManager.getInstance(any()) } returns mockk(relaxed = true) {
            every { getAppWidgetIds(any()) } returns appWidgetIds
        }

        val scenario = launchActivity<MainActivity>()
        assertEquals(Lifecycle.State.RESUMED, scenario.state)

        checkBroadcastIntents(appWidgetIds)
    }

    private fun checkBroadcastIntents(appWidgetIds: IntArray) {
        val intents = shadowOf(ApplicationProvider.getApplicationContext<Application>()).broadcastIntents
        assertEquals(1, intents.size)
        val intent = intents.first()
        assertThat(
            intent, allOf(
                IntentMatchers.hasAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE),
                IntentMatchers.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            )
        )
    }
}

@RunWith(AndroidJUnit4::class)
class FilePickerActivityTests {
    companion object {
        const val APPWIDGET_ID = 42
    }

    @Before
    fun setUp() = Intents.init()

    @After
    fun tearDown() = Intents.release()

    @Test
    fun testOpenDocumentIntent() {
        launchPickerActivity()

        onView(withId(R.id.button)).perform(click())

        Intents.intended(allOf(
            IntentMatchers.hasAction(Intent.ACTION_OPEN_DOCUMENT),
            IntentMatchers.hasType("*/*")
        ))
        Intents.assertNoUnverifiedIntents()
    }

    @Test
    fun testDocumentOpenedRefresh() {
        val scenario = launchPickerActivity()

        val fileUri = Uri.parse("content://com.android.providers.downloads.documents/document/msf%3A131233")
        Intents.intending(IntentMatchers.hasAction(Intent.ACTION_OPEN_DOCUMENT))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(fileUri)))

        mockkStatic(AppWidgetManager::class)
        every { AppWidgetManager.getInstance(any()) } returns mockk {
            every { updateAppWidget(APPWIDGET_ID, any()) } just Runs
        }

        onView(withId(R.id.button)).perform(click())

        val persistedPermissions = ApplicationProvider.getApplicationContext<Context>().contentResolver.persistedUriPermissions
        assertEquals(1, persistedPermissions.size)
        with(persistedPermissions.first()) {
            assertEquals(fileUri, uri)
            assert(isReadPermission)
        }
        with(scenario.result) {
            assertEquals(Activity.RESULT_OK, resultCode)
            assertEquals(APPWIDGET_ID, resultData.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID))
        }
    }

    @Test
    fun testDocumentOpenCanceled() {
        val scenario = launchPickerActivity()

        Intents.intending(IntentMatchers.hasAction(Intent.ACTION_OPEN_DOCUMENT))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null))

        onView(withId(R.id.button)).perform(click())

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
                .setComponent(ComponentName(ApplicationProvider.getApplicationContext(), FilePickerActivity::class.java))
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, APPWIDGET_ID)
        ).also {
            assertEquals(Lifecycle.State.RESUMED, it.state)
        }
}