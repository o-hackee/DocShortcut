package ol.ko.docshortcut.activities

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.launchActivity
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.unmockkObject
import ol.ko.docshortcut.GlanceWidgetUtils
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

    @Before
    fun setUp() = mockkObject(GlanceWidgetUtils)

    @After
    fun tearDown() = unmockkObject(GlanceWidgetUtils)

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
        coVerify { GlanceWidgetUtils.updateWidget(any(), APPWIDGET_ID) }
        scenario.close()
    }

    @Test
    fun testRefreshIntent() {
        val scenario = launchActivity<MainActivity>()
        assertEquals(Lifecycle.State.RESUMED, scenario.state)

        coVerify { GlanceWidgetUtils.updateWidgets(any(), false) }
        scenario.close()
    }
}
