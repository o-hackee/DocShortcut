package ol.ko.docshortcut.activities

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.launchActivity
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import ol.ko.docshortcut.BroadcastIntentUtil
import ol.ko.docshortcut.ui.MainActivity
import org.hamcrest.core.AllOf.allOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    companion object {
        const val APPWIDGET_ID = 42
    }

    private lateinit var appWidgetManagerMock : AppWidgetManager

    @Before
    fun setUp() {
         appWidgetManagerMock = mockk {
            every { updateAppWidget(more(APPWIDGET_ID, andEquals = true), any()) } just Runs
        }
        mockkStatic(AppWidgetManager::class)
        every { AppWidgetManager.getInstance(any()) } returns appWidgetManagerMock
    }

    @After
    fun tearDown() = unmockkStatic(AppWidgetManager::class)

    @Test
    fun testViewIntent() {
        Intents.init()

        val fileUriString = "content://com.android.providers.downloads.documents/document/msf%3A131233"
        val scenario = launchActivity<MainActivity>(
            MainActivity.createProxyIntent(
                ApplicationProvider.getApplicationContext(),
                fileUriString,
                APPWIDGET_ID
            )
        )
        assertEquals(Lifecycle.State.DESTROYED, scenario.state)
        assertEquals(Activity.RESULT_OK, scenario.result.resultCode)

        Intents.intended(
            allOf(
                IntentMatchers.hasAction(Intent.ACTION_VIEW),
                IntentMatchers.hasData(fileUriString),
                IntentMatchers.hasFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
            )
        )
        Intents.assertNoUnverifiedIntents()
        Intents.release()

        // also make sure the widget is being refreshed (e.g. if the URI is for some reason invalid, the user would
        // expect to the the error indication in the widget UI as well after unsuccessful document view)
        BroadcastIntentUtil.checkBroadcastIntents(IntArray(1) { APPWIDGET_ID })
        scenario.close()
    }

    @Test
    fun testRefreshIntent() {
        val appWidgetIds = IntArray(3) { i -> APPWIDGET_ID + i }
        every { appWidgetManagerMock.getAppWidgetIds(any()) } returns appWidgetIds

        val scenario = launchActivity<MainActivity>()
        assertEquals(Lifecycle.State.RESUMED, scenario.state)

        BroadcastIntentUtil.checkBroadcastIntents(appWidgetIds)
        scenario.close()
    }
}
