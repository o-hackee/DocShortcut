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
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Assert.assertNotEquals
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
        const val testDataFolderName = "testdata"
    }

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val appName = targetContext.getString(R.string.app_name)
    private val widgetLabel: String = appName // default
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
        removeAllWidgets()

        if (::testsDataFolder.isInitialized) {
            testsDataFolder.listFiles()?.forEach {
                it.delete()
            }
            testsDataFolder.delete()
        }
    }

    @Test
    fun addMultipleWidgets() {
        val widgetViews = fileNames.indices.map { addWidget(it) }

        val viewerApps = listOf("com.google.android.apps.docs", "com.adobe.reader")
        widgetViews.forEach { widgetView ->
            goHome()
            widgetView.click()
            var openedInViewerApp = false
            for (viewAppPackage in viewerApps) {
                openedInViewerApp = device.wait(Until.hasObject(By.pkg(viewAppPackage)), ACTION_TIMEOUT)
                if (openedInViewerApp)
                    break
            }
            assert(openedInViewerApp)
        }
    }

    private fun addWidget(idx: Int): UiObject2 {
        goHome()
        val widgetType = findWidgetInSelector()

        // long press on it and move to the center
        val widgetTapPoint = Point(widgetType.visibleBounds.centerX(), widgetType.visibleBounds.centerY())
        val placementPoint = Point(device.displayWidth / 6, device.displayHeight * (2 * idx + 1) / 4 / fileNames.count())
        val moveSteps = 100
        device.swipe(arrayOf(widgetTapPoint, widgetTapPoint, placementPoint), moveSteps)

        // press the button on the configuration activity
        val textPattern = Pattern.compile(targetContext.getString(R.string.select_document), Pattern.CASE_INSENSITIVE)
        val button = device.wait(Until.findObject(By.text(textPattern).clazz(Button::class.java)), ACTION_TIMEOUT)
        assertNotNull(button)
        button.click()

        selectTestDocument(fileNames[idx])
        val widgetView = getAddedWidget(fileNames[idx])
        assertNotNull(widgetView)
        return widgetView!!
    }

    private fun goHome() {
        device.pressHome()
        val launcherPackageName = device.launcherPackageName!!
        device.wait(Until.hasObject(By.pkg(launcherPackageName).depth(0)), ACTION_TIMEOUT)
    }

    private fun findWidgetInSelector(): UiObject2 {
        // long press at the center
        val screenCenter = Point(device.displayWidth / 2, device.displayHeight / 2)
        val longPressSteps = 100
        device.swipe(arrayOf(screenCenter, screenCenter), longPressSteps)

        // open widgets selector
        val contextMenuItem = device.wait(Until.findObject(By.text("Widgets")), ACTION_TIMEOUT)
        assertNotNull(contextMenuItem)
        contextMenuItem.click()
        // don't like this, but need to wait on emulator till widgets list is ready
        device.wait(Until.findObject(By.res("widgets_list_view")), ACTION_TIMEOUT)

        // swipe to the target widget
        var appSectionHeader = device.findObject(By.desc(appName))
        val startTime = SystemClock.uptimeMillis()
        val swipeSteps = 100
        while (appSectionHeader == null && SystemClock.uptimeMillis() - startTime < 30000) {
            device.swipe(screenCenter.x, device.displayHeight / 2, screenCenter.x, 0, swipeSteps)
            appSectionHeader = device.findObject(By.desc(appName))
        }
        // scroll to get more of a widget on the screen
        device.swipe(screenCenter.x, device.displayHeight / 2, screenCenter.x, 0, swipeSteps)

        val widgetObjects = device.findObjects(By.text(widgetLabel)) // if the widget label is not specified explicitly, app section will be there too
        val widgetType = widgetObjects.maxByOrNull { it.visibleCenter.y }
        assertNotNull(widgetType)
        return widgetType!!
    }

    private fun selectTestDocument(fileName: String) {
        // open downloads
        val rootsButton =
            device.wait(Until.findObject(By.desc("Show roots").clazz(ImageButton::class.java)), ACTION_TIMEOUT)
        assertNotNull(rootsButton)
        rootsButton.click()
        clickLabel(By.text("Downloads").res("android", "title"))
        clickLabel(By.text(testDataFolderName))
        clickLabel(By.text(fileName))
    }

    private fun clickLabel(bySelector: BySelector) {
        val label = device.wait(Until.findObject(bySelector.clazz(TextView::class.java)), ACTION_TIMEOUT)
        assertNotNull(label)
        label.click()
    }

    private fun getAddedWidget(fileName: String): UiObject2? {
        val addedWidgets = device.wait(Until.findObjects(By.desc(widgetLabel)), ACTION_TIMEOUT)
        assertNotEquals(0, addedWidgets.size)
        return addedWidgets.find { it.hasObject(By.clazz(TextView::class.java).text(fileName)) }
    }

    private fun removeAllWidgets() {
        goHome()

        val addedWidgets = device.wait(Until.findObjects(By.desc(widgetLabel)), ACTION_TIMEOUT)
        addedWidgets?.forEach {
            val widgetTapPoint = Point(it.visibleBounds.centerX(), it.visibleBounds.centerY())
            val trashPoint = Point(device.displayWidth / 2, device.displayHeight / 10)
            val moveSteps = 100
            device.swipe(arrayOf(widgetTapPoint, widgetTapPoint, trashPoint), moveSteps)
            assert(device.wait(Until.hasObject(By.text("Item removed")), ACTION_TIMEOUT))
        }
    }
}
