package ol.ko.docshortcut

import android.app.Application
import android.appwidget.AppWidgetManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.matcher.IntentMatchers
import org.hamcrest.MatcherAssert
import org.hamcrest.core.AllOf
import org.junit.Assert
import org.robolectric.Shadows

object BroadcastIntentUtil {
    internal fun checkBroadcastIntents(appWidgetIds: IntArray) {
        val intents = Shadows.shadowOf(ApplicationProvider.getApplicationContext<Application>()).broadcastIntents
        Assert.assertEquals(1, intents.size)
        val intent = intents.first()
        MatcherAssert.assertThat(
            intent, AllOf.allOf(
                IntentMatchers.hasAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE),
                IntentMatchers.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            )
        )
    }
}