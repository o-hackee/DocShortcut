package ol.ko.docshortcut

import android.graphics.Point
import android.os.Environment
import android.os.SystemClock
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiCollection
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertNotNull
import org.junit.Before
import java.io.File
import java.io.FileOutputStream
import java.util.regex.Pattern

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    companion object {
        const val ACTION_TIMEOUT: Long = 5000
        const val WIDGET_NAME: String = "Doc Shortcut"
        const val testDataFolderName = "testdata"
    }

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var testsDataFolder: File
    private lateinit var fileNames: Array<String>

    @Before
    fun copyFiles() {
        val testContext = InstrumentationRegistry.getInstrumentation().context
        fileNames = testContext.resources.assets.list(testDataFolderName) ?: arrayOf()
        testsDataFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), testDataFolderName)
        assert(testsDataFolder.mkdir())

        fileNames.forEach {
            val dest = File(testsDataFolder, it)
            assert(dest.createNewFile())
            testContext.assets.open(testDataFolderName + File.separator + it).use { src ->
                src.copyTo(FileOutputStream(dest))
            }
        }
    }

    @After
    fun clear() {
        if (::testsDataFolder.isInitialized) {
            testsDataFolder.listFiles()?.forEach {
                it.delete()
            }
            testsDataFolder.delete()
        }
    }

    @Test
    fun setWidgetOnHome() {
        // go home
        device.pressHome()
        val launcherPackageName = device.launcherPackageName!!
        device.wait(Until.hasObject(By.pkg(launcherPackageName).depth(0)), ACTION_TIMEOUT)

        // long press at the center
        val screenCenter = Point(device.displayWidth / 2, device.displayHeight / 2)
        val longPressSteps = 100
        device.swipe(arrayOf(screenCenter, screenCenter), longPressSteps)

        // open widgets selector
        device.findObject(By.text("Widgets")).click()

        // swipe to the target widget
        var widget = device.findObject(By.text(WIDGET_NAME))
        val startTime = SystemClock.uptimeMillis()
        val swipeSteps = 100
        while (widget == null && SystemClock.uptimeMillis() - startTime < 30000) {
            device.swipe(screenCenter.x, device.displayHeight / 2, screenCenter.x, 0, swipeSteps)
            widget = device.findObject(By.text(WIDGET_NAME))
        }
        assertNotNull(widget)

        // long press on it and move to the center
        val offset = 50
        val widgetTapPoint = Point(widget.visibleBounds.left + offset, widget.visibleBounds.bottom + offset)
        val moveSteps = 100
        device.swipe(arrayOf(widgetTapPoint, widgetTapPoint, screenCenter), moveSteps)

        // press the button on the configuration activity
        val textPattern = Pattern.compile(targetContext.getString(R.string.select_document), Pattern.CASE_INSENSITIVE)
        val button = device.wait(Until.findObject(By.text(textPattern).clazz(Button::class.java)), ACTION_TIMEOUT)
        assertNotNull(button)
        button.click()

        // open downloads
        val rootsButton = device.wait(Until.findObject(By.desc("Show roots").clazz(ImageButton::class.java)), ACTION_TIMEOUT)
        assertNotNull(rootsButton)
        rootsButton.click()
        clickLabel(By.text("Downloads").res("android", "title"))
        clickLabel(By.text(testDataFolderName))
        clickLabel(By.text(fileNames.first()))

        val addedWidget = device.wait(Until.findObject(By.descContains(WIDGET_NAME)), ACTION_TIMEOUT)
        assertNotNull(addedWidget)
        val addedWidgetCollection = UiCollection(UiSelector().description(WIDGET_NAME))
        val widgetFileNameLabel = addedWidgetCollection.getChildByText(UiSelector().className(TextView::class.java), fileNames.first())
        assertNotNull(widgetFileNameLabel)
    }

    private fun clickLabel(bySelector: BySelector) {
        val label = device.wait(Until.findObject(bySelector.clazz(TextView::class.java)), ACTION_TIMEOUT)
        assertNotNull(label)
        label.click()
    }
}
