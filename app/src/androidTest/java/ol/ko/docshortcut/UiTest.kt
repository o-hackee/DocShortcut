package ol.ko.docshortcut

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertNotNull
import org.junit.Before
import java.io.File
import java.io.FileOutputStream
import java.util.regex.Pattern


@RunWith(AndroidJUnit4::class)
open class UiTest {

    protected val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    protected val targetContext: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private val appName = targetContext.getString(R.string.app_name)
    protected val widgetLabel: String = appName // default

    @After
    fun removeAllWidgets() {
        goHome()

        val addedWidgets = device.findObjects(By.desc(widgetLabel))
        addedWidgets?.forEach {
            val widgetTapPoint = Point(it.visibleBounds.centerX(), it.visibleBounds.centerY())
            val trashPoint = Point(device.displayWidth / 2, device.displayHeight / 10)
            val moveSteps = 100
            device.swipe(arrayOf(widgetTapPoint, widgetTapPoint, trashPoint), moveSteps)
            assert(device.wait(Until.hasObject(By.text("Item removed")), DocumentsUiTest.ACTION_TIMEOUT))
        }
    }

    @Test
    fun addingTerminated() {
        startAddingWidget(upToFilePicker = false, pressHome = false)
        startAddingWidget(upToFilePicker = false, pressHome = true)
        startAddingWidget(upToFilePicker = true, pressHome = false)
        startAddingWidget(upToFilePicker = true, pressHome = true)
    }

    private fun startAddingWidget(upToFilePicker: Boolean, pressHome: Boolean) {
        goHome()
        val widgetType = findWidgetInSelector()

        // long press on it and move to the center
        val widgetTapPoint = Point(widgetType.visibleBounds.centerX(), widgetType.visibleBounds.centerY())
        val placementPoint = Point(device.displayWidth / 6, device.displayHeight / 6)
        val moveSteps = 100
        device.swipe(arrayOf(widgetTapPoint, widgetTapPoint, placementPoint), moveSteps)

        // press the button on the configuration activity
        val textPattern = Pattern.compile(targetContext.getString(R.string.select_document), Pattern.CASE_INSENSITIVE)
        val button = device.wait(Until.findObject(By.text(textPattern).clazz(Button::class.java)),
            DocumentsUiTest.ACTION_TIMEOUT
        )
        assertNotNull(button)

        if (upToFilePicker) {
            button.click()
            assert(device.wait(Until.hasObject(By.desc("Show roots").clazz(ImageButton::class.java)),
                DocumentsUiTest.ACTION_TIMEOUT
            ))
        }
        if (pressHome)
            goHome()
        else
            goBackUpToHome()

        // verify there are no widgets
        assert(!device.hasObject(By.desc(widgetLabel)))
    }

    protected fun goHome() {
        device.pressHome()
        assert(device.wait(Until.hasObject(By.pkg(device.launcherPackageName!!).depth(0)),
            DocumentsUiTest.ACTION_TIMEOUT
        ))
    }

    private fun goBackUpToHome() {
        do {
            device.pressBack()
        } while (!device.wait(Until.hasObject(By.pkg(device.launcherPackageName!!).depth(0)),
                DocumentsUiTest.ACTION_TIMEOUT / 2
            ))
    }

    protected fun findWidgetInSelector(): UiObject2 {
        // long press at the center
        val screenCenter = Point(device.displayWidth / 2, device.displayHeight / 2)
        val longPressSteps = 100
        device.swipe(arrayOf(screenCenter, screenCenter), longPressSteps)

        // open widgets selector
        val contextMenuItem = device.wait(Until.findObject(By.text("Widgets")), DocumentsUiTest.ACTION_TIMEOUT)
        assertNotNull(contextMenuItem)
        contextMenuItem.click()
        // don't like this, but need to wait on emulator till widgets list is ready
        device.wait(Until.findObject(By.res("widgets_list_view")), DocumentsUiTest.ACTION_TIMEOUT)

        // swipe to the target widget
        var appSectionHeader = device.findObject(By.desc(appName))
        val startTime = SystemClock.uptimeMillis()
        val swipeSteps = 100
        while (appSectionHeader == null && SystemClock.uptimeMillis() - startTime < 30000) {
            device.swipe(screenCenter.x, screenCenter.y, screenCenter.x, 0, swipeSteps)
            appSectionHeader = device.findObject(By.desc(appName))
        }
        // scroll to get more of a widget on the screen
        device.swipe(screenCenter.x, screenCenter.y, screenCenter.x, 0, swipeSteps)

        val widgetObjects = device.findObjects(By.text(widgetLabel)) // if the widget label is not specified explicitly, app section will be there too
        val widgetType = widgetObjects.maxByOrNull { it.visibleCenter.y }
        assertNotNull(widgetType)
        return widgetType!!
    }
}

@RunWith(AndroidJUnit4::class)
class DocumentsUiTest : UiTest() {

    companion object {
        const val ACTION_TIMEOUT: Long = 5000
        const val testDataFolderName = "testdata"
        val viewerApps = listOf("com.google.android.apps.docs", "com.adobe.reader")
    }

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
    fun addMultipleWidgets() {
        val widgetViews = fileNames.indices.map { addWidget(it) }
        widgetViews.forEachIndexed { widgetIdx, widgetView -> viewDocument(widgetView, fileNames[widgetIdx]) }
    }

    @Test
    fun fullyQualifiedRenamed() {
        // test initial display as well

    }

    @Test
    fun providerLinkRenamed() {
        // test initial display as well
        val widgetView = addWidget(fileNames.lastIndex)
        val fileName = fileNames.last()
        viewDocument(widgetView, fileName)

        goHome()
        val filesAppName = "Files"
        val appDrawer = UiScrollable(UiSelector().scrollable(true))
        if (appDrawer.exists()) {
            appDrawer.scrollForward()
            appDrawer.scrollTextIntoView(filesAppName)
        } else {
            device.swipe(device.displayWidth / 2, device.displayHeight / 2, device.displayWidth / 2, 0, 100)
        }
        val filesAppShortcut = device.wait(Until.findObject(By.text(filesAppName)), ACTION_TIMEOUT)
        assertNotNull(filesAppShortcut)
        // alternatively use testContext.packageManager.getLaunchIntentForPackage() ("com.android.documentsui" or "com.google.android.apps.nbu.files")
        // and testContext.startActivity() - and for "com.google.android.apps.nbu.files" that would be better cause old task might be brought up

        filesAppShortcut.click()
        val (fileLabel, trickyApp) = filesAppNavigateToTestDocumentViaProviderLink(fileName)
        if (trickyApp) {
            val longPressSteps = 100
            device.swipe(arrayOf(fileLabel.visibleCenter, fileLabel.visibleCenter), longPressSteps)
        }
        else
            fileLabel.longClick()

        val image = device.wait(Until.findObject(By.clazz(ImageView::class.java).desc("More options")), ACTION_TIMEOUT)
        assertNotNull(image)
        image.click()
        clickLabel(By.text("Rename"))
        val editText = device.wait(Until.findObject(By.clazz(EditText::class.java)), ACTION_TIMEOUT)
        assertNotNull(editText)
        val newName = "renamed_$fileName"
        editText.text = newName
        val textPattern = Pattern.compile(targetContext.getString(android.R.string.ok), Pattern.CASE_INSENSITIVE)
        val button = device.wait(Until.findObject(By.text(textPattern).clazz(Button::class.java)), ACTION_TIMEOUT)
        assertNotNull(button)
        button.click()
        device.wait(Until.hasObject(By.text("File renamed")), ACTION_TIMEOUT)

        // close both "Files" (if is trickyApp) & viewer
        device.pressRecentApps()
        val clearAllSelector = By.text(Pattern.compile("Clear All", Pattern.CASE_INSENSITIVE))
        var clearAll = device.findObject(clearAllSelector)
        val startTime = SystemClock.uptimeMillis()
        val swipeSteps = 100
        while (clearAll == null && SystemClock.uptimeMillis() - startTime < 30000) {
            device.swipe(0, device.displayHeight / 2, device.displayWidth, device.displayHeight / 2, swipeSteps)
            clearAll = device.findObject(clearAllSelector)
        }
        assertNotNull(clearAll)
        clearAll.click()

        goHome()
        val shouldBeHandled = !isEmulator() // actually on emulator it might differ from launch to launch
        val widgetFileLabel = widgetView.findObject(By.clazz(TextView::class.java))
        if (shouldBeHandled) {
            viewDocument(widgetView, newName)
            goHome()
            // TODO do we guarantee it's updated right now actually? do recheck
//            assertEquals(newName, widgetFileLabel.text)
            assertEquals(targetContext.getString(R.string.appwidget_text), widgetFileLabel.contentDescription)
        } else {
            widgetView.click()
//            assert(device.wait(Until.hasObject(By.clazz(Toast::class.java)), ACTION_TIMEOUT))
            verifyUnableToViewDocument()
            assertEquals(targetContext.getString(R.string.appwidget_invalid_text), widgetFileLabel?.contentDescription)
        }
    }

    @Test
    fun fullyQualifiedRemoved() {
        // test initial display as well

    }

    @Test
    fun providerLinkRemoved() {
        // test initial display as well

    }

    @Test
    fun fullyQualifiedRemovableStorageRenamed() {
        // test initial display as well

    }

    @Test
    fun providerLinkRemovableStorageRenamed() {
        // test initial display as well

    }

    @Test
    fun fullyQualifiedRemovableStorageRemoved() {
        // test initial display as well

    }

    @Test
    fun providerLinkRemovableStorageRemoved() {
        // test initial display as well

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

        filePickerNavigateToTestDocumentViaProviderLink(fileNames[idx]).click()
        val widgetView = getAddedWidget(fileNames[idx])
        assertNotNull(widgetView)
        return widgetView!!
    }

    private fun filesAppNavigateToTestDocumentViaProviderLink(fileName: String): Pair<UiObject2, Boolean> {
        if (device.wait(Until.hasObject(By.desc("Show roots").clazz(ImageButton::class.java)), ACTION_TIMEOUT)) {
            return filePickerNavigateToTestDocumentViaProviderLink(fileName) to false
        }

        clickLabel(By.text("Browse"))
        clickLabel(By.text("Downloads"))
        clickLabel(By.text(testDataFolderName))
        val fileLabel = device.wait(Until.findObject(By.text(fileName).clazz(TextView::class.java)), ACTION_TIMEOUT)
        assertNotNull(fileLabel)
        return fileLabel to true
    }

    private fun filePickerNavigateToTestDocumentViaProviderLink(fileName: String): UiObject2 {
        // open downloads
        val rootsButton =
            device.wait(Until.findObject(By.desc("Show roots").clazz(ImageButton::class.java)), ACTION_TIMEOUT)
        assertNotNull(rootsButton)
        rootsButton.click()
        clickLabel(By.text("Downloads").res("android", "title"))
        clickLabel(By.text(testDataFolderName))
        val fileLabel = device.wait(Until.findObject(By.text(fileName).clazz(TextView::class.java)), ACTION_TIMEOUT)
        assertNotNull(fileLabel)
        return fileLabel
    }

    private fun clickLabel(bySelector: BySelector) {
        val label = device.wait(Until.findObject(bySelector.clazz(TextView::class.java)), ACTION_TIMEOUT)
        assertNotNull(label)
        label.click()
    }

    private fun getAddedWidget(fileName: String, withDescription: Boolean = true): UiObject2? {
        val addedWidgets = device.wait(Until.findObjects(By.desc(widgetLabel)), ACTION_TIMEOUT)
        assertNotEquals(0, addedWidgets.size)
        return addedWidgets.find {
            var selector = By.clazz(TextView::class.java).text(fileName)
            if (withDescription) {
                selector = selector.desc(targetContext.getString(R.string.appwidget_text))
            }
            it.hasObject(selector)
        }
    }

    private fun viewDocument(widgetView: UiObject2, fileName: String) {
        goHome()
        widgetView.click()
        var viewerApp = ""
        for (app in viewerApps) {
            if (device.wait(Until.hasObject(By.pkg(app)), ACTION_TIMEOUT)) {
                viewerApp = app
                break
            }
        }
        assertNotEquals("", viewerApp)
        when (viewerApp) {
            viewerApps[0] -> assert(device.hasObject(By.text(fileName)))
            viewerApps[1] -> {
                val image = device.wait(Until.findObject(By.clazz(ImageView::class.java).desc("More options")), ACTION_TIMEOUT)
                assertNotNull(image)
                image.click()
                val fileNameWithoutExtension = File(fileName).nameWithoutExtension
                assert(device.wait(Until.hasObject(By.text(fileNameWithoutExtension)), ACTION_TIMEOUT))
            }
        }
    }

    private fun verifyUnableToViewDocument() {
        var viewerApp = ""
        for (app in viewerApps) {
            if (device.wait(Until.hasObject(By.pkg(app)), ACTION_TIMEOUT)) {
                viewerApp = app
                break
            }
        }
        assertEquals("", viewerApp)
    }

    private fun isEmulator(): Boolean = Build.MODEL.contains("Android SDK built for x86")
}

@RunWith(AndroidJUnit4::class)
class MediaTest {
    @Test
    fun fullyQualifiedMedia() {
        // photo

        // music

    }

    @Test
    fun providerLinkMedia() {
        // photo

        // music

    }
}
