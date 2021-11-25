package ol.ko.docshortcut

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.launchActivity
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.core.AllOf.allOf
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
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
    @Test
    fun testViewIntent() {
        Intents.init()

        val fileUriString = "content://com.android.providers.downloads.documents/document/msf%3A131233"
        val scenario = launchActivity<MainActivity>(MainActivity.createProxyIntent(
            ApplicationProvider.getApplicationContext(),
            fileUriString,
            42
        ))
        assertEquals(Lifecycle.State.DESTROYED, scenario.state)
        assertEquals(Activity.RESULT_OK, scenario.result.resultCode)

        Intents.intended(allOf(
            IntentMatchers.hasAction(Intent.ACTION_VIEW),
            IntentMatchers.hasData(fileUriString),
            IntentMatchers.hasFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        ))

        Intents.release()
    }
}
