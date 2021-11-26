package ol.ko.docshortcut

import android.app.Activity
import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.launchActivity
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.AllOf.allOf
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config


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
@Config(sdk = [30]) // alternatively create a robolectric.properties file inside the app/src/test/resources directory
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
