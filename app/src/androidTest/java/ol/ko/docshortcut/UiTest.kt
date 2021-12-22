package ol.ko.docshortcut

import android.content.Context
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.DocumentsContract
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.test.core.app.launchActivity
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
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.regex.Pattern

open class WidgetsBaseTest {
    companion object {
        const val ACTION_TIMEOUT: Long = 5000
    }

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
            assert(device.wait(Until.hasObject(By.text("Item removed")), ACTION_TIMEOUT))
        }
    }

    protected fun goHome() {
        device.pressHome()
        assert(device.wait(Until.hasObject(By.pkg(device.launcherPackageName!!).depth(0)), ACTION_TIMEOUT))
    }

    protected fun findWidgetInSelector(): UiObject2 {
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
        var appSectionHeader = device.findObject(By.text(appName))
        val startTime = SystemClock.uptimeMillis()
        val swipeSteps = 100
        while (appSectionHeader == null && SystemClock.uptimeMillis() - startTime < 30000) {
            device.swipe(screenCenter.x, screenCenter.y, screenCenter.x, 0, swipeSteps)
            appSectionHeader = device.findObject(By.text(appName))
        }
        assertNotNull(appSectionHeader)
        // scroll to get more of a widget on the screen
        device.swipe(screenCenter.x, screenCenter.y, screenCenter.x, 0, swipeSteps)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            appSectionHeader.click()
        }

        val widgetObjects = device.findObjects(By.text(widgetLabel)) // if the widget label is not specified explicitly, app section will be there too
        val widgetType = widgetObjects.maxByOrNull { it.visibleCenter.y }
        assertNotNull(widgetType)
        return widgetType!!
    }
}

@RunWith(AndroidJUnit4::class)
class UiTest: WidgetsBaseTest() {

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
        val button = device.wait(Until.findObject(By.text(textPattern).clazz(Button::class.java)), ACTION_TIMEOUT)
        assertNotNull(button)

        if (upToFilePicker) {
            button.click()
            assert(device.wait(Until.hasObject(By.desc("Show roots").clazz(ImageButton::class.java)), ACTION_TIMEOUT
            ))
        }
        if (pressHome)
            goHome()
        else
            goBackUpToHome()

        // verify there are no widgets
        assert(!device.hasObject(By.desc(widgetLabel)))
    }

    private fun goBackUpToHome() {
        do {
            device.pressBack()
        } while (!device.wait(Until.hasObject(By.pkg(device.launcherPackageName!!).depth(0)), ACTION_TIMEOUT / 2))
    }
}

open class DocumentsBaseTest: WidgetsBaseTest() {

    companion object {
        const val testDataFolderName = "testdata"
        val viewerApps = listOf("com.google.android.apps.docs", "com.adobe.reader")
    }

    // TODO built-in
    protected fun createFile(fileName: String, onRemovableStorage: Boolean): Uri {
        val scenario = launchActivity<TestActivity>()
        var createdFileUri: Uri? = null
        scenario.onActivity {
            it.launchDocumentCreator(fileName) { obtainedUri -> createdFileUri = obtainedUri }
        }
        val rootsButton =
            device.wait(Until.findObject(By.desc("Show roots").clazz(ImageButton::class.java)), ACTION_TIMEOUT)
        assertNotNull(rootsButton)
        rootsButton.click()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            addDelay()
        if (onRemovableStorage) {
            clickLabel(By.textContains(sdCardLabelName()).res("android", "title"))
            clickLabel(By.text("Download"))
        } else {
            clickLabel(By.text("Downloads").res("android", "title"))
        }
        val saveButton =
            device.wait(Until.findObject(By.text(Pattern.compile("Save", Pattern.CASE_INSENSITIVE))), ACTION_TIMEOUT)
        assertNotNull(saveButton)
        saveButton.click()

        device.wait(Until.findObject(By.text("non-existing object")), ACTION_TIMEOUT)
        assertNotNull(createdFileUri)
        try {
            targetContext.contentResolver.openFileDescriptor(createdFileUri!!, "w")?.use { it ->
                FileOutputStream(it.fileDescriptor).use { destStream ->
                    val testContext = InstrumentationRegistry.getInstrumentation().context
                    testContext.assets.open(testDataFolderName + File.separator + fileName).use { src ->
                        src.copyTo(destStream)
                    }
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            assert(false) { "file create failed" }
        } catch (e: IOException) {
            e.printStackTrace()
            assert(false) { "file create failed" }
        }
        return createdFileUri!!
    }

    protected fun addWidget(fileName: String, useProviderLink: Boolean, fromRemovableStorage: Boolean, placementPoint: Point): UiObject2 {
        goHome()
        val widgetType = findWidgetInSelector()

        // long press on it and move to the center
        val widgetTapPoint = Point(widgetType.visibleBounds.centerX(), widgetType.visibleBounds.centerY())
        val moveSteps = 100
        device.swipe(arrayOf(widgetTapPoint, widgetTapPoint, placementPoint), moveSteps)

        // press the button on the configuration activity
        val textPattern = Pattern.compile(targetContext.getString(R.string.select_document), Pattern.CASE_INSENSITIVE)
        val button = device.wait(Until.findObject(By.text(textPattern).clazz(Button::class.java)), ACTION_TIMEOUT)
        assertNotNull(button)
        button.click()

        filePickerNavigateToTestDocument(fileName, useProviderLink, fromRemovableStorage).click()
        val widgetView = getAddedWidget(fileName)
        assertNotNull(widgetView)
        return widgetView!!
    }

    private fun getAddedWidget(fileName: String, withDescription: Boolean = true): UiObject2? {
        val addedWidgets = device.wait(Until.findObjects(By.desc(widgetLabel)), ACTION_TIMEOUT)
        assertNotEquals(0, addedWidgets.size)
        return addedWidgets.find {
            val selector = By.clazz(TextView::class.java).text(fileName)
            if (withDescription) {
                selector.desc(targetContext.getString(R.string.appwidget_text))
            }
            it.hasObject(selector)
        }
    }

    protected fun viewDocument(widgetView: UiObject2, fileName: String) {
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
                clickMoreOptions()
                val fileNameWithoutExtension = File(fileName).nameWithoutExtension
                assert(device.wait(Until.hasObject(By.text(fileNameWithoutExtension)), ACTION_TIMEOUT))
            }
        }
    }

    protected fun filePickerNavigateToTestDocument(fileName: String, useProviderLink: Boolean, fromRemovableStorage: Boolean): UiObject2 {
        // open downloads
        val rootsButtonCondition = Until.findObject(By.desc("Show roots").clazz(ImageButton::class.java))
        val rootsButton = device.wait(rootsButtonCondition, ACTION_TIMEOUT)
        assertNotNull(rootsButton)
        rootsButton.click()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            addDelay()
        if (useProviderLink) {
            clickLabel(By.text("Downloads").res("android", "title"))
        } else {
            var storageLabel: UiObject2?
            if (fromRemovableStorage) {
                storageLabel = device.wait(Until.findObject(By.textContains(sdCardLabelName()).res("android", "title")), ACTION_TIMEOUT)
            } else {
                storageLabel = device.wait(Until.findObject(By.text(Build.MODEL).res("android", "title")), ACTION_TIMEOUT)
                if (isEmulator() && storageLabel == null) {
                    clickMoreOptions()
                    clickMoreOptions()
                    clickLabel(By.text("Show internal storage"))
                    device.wait(rootsButtonCondition, ACTION_TIMEOUT)
                    rootsButton.click()
                    storageLabel = device.wait(Until.findObject(By.text(Build.MODEL).res("android", "title")), ACTION_TIMEOUT)
                }
            }
            assertNotNull(storageLabel)
            storageLabel.click()

            val downloadFolder = scrollDownTo(By.text("Download"))
            assertNotNull(downloadFolder)
            downloadFolder!!.click()
        }
        val fileLabel = scrollDownTo(By.text(fileName).clazz(TextView::class.java))
        assertNotNull(fileLabel)
        return fileLabel!!
    }

    protected fun clickLabel(bySelector: BySelector) {
        val label = device.wait(Until.findObject(bySelector.clazz(TextView::class.java)), ACTION_TIMEOUT)
        assertNotNull(label)
        label.click()
    }

    protected fun scrollDownTo(bySelector: BySelector): UiObject2? {
        var obj = device.findObject(bySelector)
        val startTime = SystemClock.uptimeMillis()
        val swipeSteps = 100
        while (obj == null && SystemClock.uptimeMillis() - startTime < 30000) {
            device.swipe(device.displayWidth / 2, 3 * device.displayHeight / 4, device.displayWidth / 2, 0, swipeSteps)
            obj = device.findObject(bySelector)
        }
        return obj
    }

    protected fun clickMoreOptions() {
        val image = device.wait(Until.findObject(By.clazz(ImageView::class.java).desc("More options")), ACTION_TIMEOUT)
        assertNotNull(image)
        image.click()
    }

    private fun addDelay() {
        device.wait(Until.findObject(By.text("non-existing object")), ACTION_TIMEOUT / 2)
    }

    protected fun sdCardLabelName() = if (isEmulator()) "SDCARD" else "SD card"

    protected fun isEmulator(): Boolean = Build.MODEL.equals("Android SDK built for x86") or Build.MODEL.equals("sdk_gphone_x86")
}

@RunWith(AndroidJUnit4::class)
class DocumentsUiTest: DocumentsBaseTest() {

    private lateinit var fileNames: Array<String>
    private val fileUris = mutableListOf<Uri>()

    @Before
    fun prepareFiles() {
        fileNames = InstrumentationRegistry.getInstrumentation().context.resources.assets.list(testDataFolderName) ?: arrayOf()

        fileNames.forEach { fileName ->
            fileUris.add(createFile(fileName, onRemovableStorage = false))
        }
    }

    @After
    fun clearFiles() {
        fileUris.forEach {
            DocumentsContract.deleteDocument(targetContext.contentResolver, it)
        }
    }

    @Test
    fun addMultipleWidgets() {
        val widgetViews = fileNames.mapIndexed { idx, fileName ->
            val placementPoint = Point(device.displayWidth / 6, device.displayHeight * (2 * idx + 1) / 4 / fileNames.count())
            addWidget(fileName, useProviderLink = true, fromRemovableStorage = false, placementPoint)
        }
        widgetViews.forEachIndexed { idx, widgetView -> viewDocument(widgetView, fileNames[idx]) }
    }
}

open class DocumentsAlteredBaseTest: DocumentsBaseTest() {

    protected fun getPlacementPoint() = Point(device.displayWidth / 6, device.displayHeight / 6)

    protected fun handleRenaming(fromFileName: String, toFileName:String, useProviderLink: Boolean, fromRemovableStorage: Boolean, widgetView: UiObject2, shouldBeHandled: Boolean) {
        openFilesApp()
        renameFile(fromFileName, toFileName, useProviderLink, fromRemovableStorage)
        // close both "Files" (crucial if is trickyApp) & viewer
        closeRecentApps()
        goHome()
        tryToViewDocument(toFileName, widgetView, shouldBeHandled)
    }

    protected fun handleRemoving(fileName: String, useProviderLink: Boolean, fromRemovableStorage: Boolean, widgetView: UiObject2) {
        openFilesApp()
        removeFile(fileName, useProviderLink, fromRemovableStorage)
        closeRecentApps()
        goHome()
        tryToViewDocument(fileName, widgetView, false)
    }

    protected fun handleRestoring(fileName: String, widgetView: UiObject2, shouldBeHandled: Boolean, fileUris: MutableList<Uri>) {
        fileUris.add(createFile(fileName, onRemovableStorage = false))
        goHome()
        tryToViewDocument(fileName, widgetView, shouldBeHandled)
    }

    private fun tryToViewDocument(fileName: String, widgetView: UiObject2, shouldBeHandled: Boolean) {
        val widgetFileLabel = widgetView.findObject(By.clazz(TextView::class.java))
        if (shouldBeHandled) {
            viewDocument(widgetView, fileName)
            goHome()
            device.waitForIdle()
            assertEquals(fileName, widgetFileLabel.text)
            assertEquals(targetContext.getString(R.string.appwidget_text), widgetFileLabel.contentDescription)
        } else {
            widgetView.click()
            // unable to catch Toast, just make sure document is not opened
            // also can be tried to view in another app (e.g. Contacts), also needs back or home then
            verifyUnableToViewDocument()
            goHome()
            assertEquals(targetContext.getString(R.string.appwidget_invalid_text), widgetFileLabel.contentDescription)
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

    private fun filesAppNavigateToTestDocument(fileName: String, useProviderLink: Boolean, fromRemovableStorage: Boolean): Pair<UiObject2, Boolean> {
        if (device.wait(Until.hasObject(By.desc("Show roots").clazz(ImageButton::class.java)), ACTION_TIMEOUT)) {
            return filePickerNavigateToTestDocument(fileName, useProviderLink, fromRemovableStorage) to false
        }

        clickLabel(By.text("Browse"))
        if (useProviderLink) {
            clickLabel(By.text("Downloads"))
        } else {
            val storageLabel = scrollDownTo(By.text(if (fromRemovableStorage) sdCardLabelName() else "Internal storage"))
            assertNotNull(storageLabel)
            storageLabel!!.click()
            clickLabel(By.text("Download"))
        }
        val fileLabel = device.wait(Until.findObject(By.text(fileName).clazz(TextView::class.java)), ACTION_TIMEOUT)
        assertNotNull(fileLabel)
        return fileLabel to true
    }

    private fun openFilesApp() {
        goHome()
        val filesAppName = "Files"
        val appDrawer = UiScrollable(UiSelector().scrollable(true))
        if (appDrawer.exists()) {
            appDrawer.scrollForward()
            try {
                appDrawer.scrollTextIntoView(filesAppName)
            } catch (_: Exception) {}
        } else {
            device.swipe(device.displayWidth / 2, device.displayHeight / 2, device.displayWidth / 2, 0, 100)
        }
        val filesAppShortcut = device.wait(Until.findObject(By.text(filesAppName)), ACTION_TIMEOUT)
        assertNotNull(filesAppShortcut)
        // alternatively use testContext.packageManager.getLaunchIntentForPackage() ("com.android.documentsui" or "com.google.android.apps.nbu.files")
        // and testContext.startActivity() - and for "com.google.android.apps.nbu.files" that would be better cause old task might be brought up

        filesAppShortcut.click()
    }

    private fun renameFile(fileName: String, newName: String, useProviderLink: Boolean, fromRemovableStorage: Boolean) {
        val (fileLabel, trickyApp) = filesAppNavigateToTestDocument(fileName, useProviderLink, fromRemovableStorage)
        if (trickyApp) {
            val longPressSteps = 100
            device.swipe(arrayOf(fileLabel.visibleCenter, fileLabel.visibleCenter), longPressSteps)
        } else
            fileLabel.longClick()

        clickMoreOptions()
        clickLabel(By.text("Rename"))
        val editText = device.wait(Until.findObject(By.clazz(EditText::class.java)), ACTION_TIMEOUT)
        assertNotNull(editText)
        editText.text = newName
        val textPattern = Pattern.compile(targetContext.getString(android.R.string.ok), Pattern.CASE_INSENSITIVE)
        val button = device.wait(Until.findObject(By.text(textPattern).clazz(Button::class.java)), ACTION_TIMEOUT)
        assertNotNull(button)
        button.click()
        assert(device.wait(Until.hasObject(By.text(newName)), ACTION_TIMEOUT))
    }

    private fun removeFile(fileName: String, useProviderLink: Boolean, fromRemovableStorage: Boolean) {
        val (fileLabel, trickyApp) = filesAppNavigateToTestDocument(fileName, useProviderLink, fromRemovableStorage)
        if (trickyApp) {
            val longPressSteps = 100
            device.swipe(arrayOf(fileLabel.visibleCenter, fileLabel.visibleCenter), longPressSteps)
        } else
            fileLabel.longClick()

        clickLabel(By.descContains("Delete"))
        val textPattern = Pattern.compile(targetContext.getString(android.R.string.ok), Pattern.CASE_INSENSITIVE)
        val button = device.wait(Until.findObject(By.text(textPattern).clazz(Button::class.java)), ACTION_TIMEOUT) ?:
        device.wait(Until.findObject(By.text("Delete").clazz(Button::class.java)), ACTION_TIMEOUT)
        assertNotNull(button)
        button.click()
        device.wait(Until.hasObject(By.textContains("delet")), ACTION_TIMEOUT)
    }

    private fun closeRecentApps() {
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
    }
}

@RunWith(AndroidJUnit4::class)
class DocumentsAlteredTest: DocumentsAlteredBaseTest() {
    private lateinit var fileName: String
    private val fileUris = mutableListOf<Uri>()

    @Before
    fun prepareFile() {
        fileName = InstrumentationRegistry.getInstrumentation().context.resources.assets.list(testDataFolderName)?.first() ?: ""
        fileUris.add(createFile(fileName, onRemovableStorage = false))
    }

    @After
    fun clearFiles() {
        fileUris.forEach {
            try {
                DocumentsContract.deleteDocument(targetContext.contentResolver, it)
            } catch (_: Exception) {
                // is ok, e.g. uri of the deleted file
            }
        }
    }

    // emulator 30
    // fullyQualifiedRenamed: generally ok, but the file is not cleaned at the end

    @Test
    fun fullyQualifiedRenamed() {
        val widgetView = addWidget(fileName, useProviderLink = false, fromRemovableStorage = false, getPlacementPoint())
        viewDocument(widgetView, fileName)

        // actually it doesn't matter how we navigate to the file to rename it, it only matters how we obtain the uri while creating,
        // but keeping the same way
        val newName = "renamed_$fileName"
        handleRenaming(fileName, newName, useProviderLink = false, fromRemovableStorage = false, widgetView, shouldBeHandled = false)
        handleRenaming(newName, fileName, useProviderLink = false, fromRemovableStorage = false, widgetView, shouldBeHandled = !isEmulator())
    }

    @Test
    fun providerLinkRenamed() {
        val widgetView = addWidget(fileName, useProviderLink = true, fromRemovableStorage = false, getPlacementPoint())
        viewDocument(widgetView, fileName)

        val newName = "renamed_$fileName"
        handleRenaming(fileName, newName, useProviderLink = true, fromRemovableStorage = false, widgetView, shouldBeHandled = isEmulator())
        handleRenaming(newName, fileName, useProviderLink = true, fromRemovableStorage = false, widgetView, shouldBeHandled = true)
    }

    @Test
    fun fullyQualifiedRemoved() {
        val widgetView = addWidget(fileName, useProviderLink = false, fromRemovableStorage = false, getPlacementPoint())
        viewDocument(widgetView, fileName)

        handleRemoving(fileName, useProviderLink = false, fromRemovableStorage = false, widgetView)
        handleRestoring(fileName, widgetView, shouldBeHandled = !isEmulator(), fileUris)
    }

    @Test
    fun providerLinkRemoved() {
        val widgetView = addWidget(fileName, useProviderLink = true, fromRemovableStorage = false, getPlacementPoint())
        viewDocument(widgetView, fileName)

        handleRemoving(fileName, useProviderLink = true, fromRemovableStorage = false, widgetView)
        handleRestoring(fileName, widgetView, shouldBeHandled = !isEmulator(), fileUris)
    }
}

@RunWith(AndroidJUnit4::class)
class RemovableStorageTest: DocumentsAlteredBaseTest() {
    private lateinit var fileName: String
    private val fileUris = mutableListOf<Uri>()

    @Before
    fun prepareFile() {
        fileName = InstrumentationRegistry.getInstrumentation().context.resources.assets.list(testDataFolderName)?.first() ?: ""
        fileUris.add(createFile(fileName, onRemovableStorage = true))
    }

    @After
    fun clearFiles() {
        fileUris.forEach {
            try {
                DocumentsContract.deleteDocument(targetContext.contentResolver, it)
            } catch (_: Exception) {
                // is ok, e.g. uri of the deleted file
            }
        }
    }

    // emulator 30
    // fullyQualifiedRenamed: generally ok, but the file is not cleaned at the end
    // providerLinkRenamed: "failed to rename document" - actually renamed, hence not removed at the end, and still occurs by the old name in "Downloads" provider
    // emulator 29
    // fullyQualifiedRenamed: generally ok, but the file is not cleaned at the end
    // providerLinkRenamed: can't even view the document initially
    // providerLinkRemoved: can't even view the document initially

    // phone
    // fullyQualifiedRenamed: generally ok, but the file is not cleaned at the end TOO
    // providerLinkRenamed: generally ok, but the file is not cleaned at the end

    @Test
    fun fullyQualifiedRenamed() {
        val widgetView = addWidget(fileName, useProviderLink = false, fromRemovableStorage = true, getPlacementPoint())
        viewDocument(widgetView, fileName)

        // actually it doesn't matter how we navigate to the file to rename it, it only matters how we obtain the uri while creating,
        // but keeping the same way
        val newName = "renamed_$fileName"
        handleRenaming(fileName, newName, useProviderLink = false, fromRemovableStorage = true, widgetView, shouldBeHandled = false)
        handleRenaming(newName, fileName, useProviderLink = false, fromRemovableStorage = true, widgetView, shouldBeHandled = false)
    }

    @Test
    fun providerLinkRenamed() {
        val widgetView = addWidget(fileName, useProviderLink = true, fromRemovableStorage = true, getPlacementPoint())
        viewDocument(widgetView, fileName)

        val newName = "renamed_$fileName"
        handleRenaming(fileName, newName, useProviderLink = true, fromRemovableStorage = true, widgetView, shouldBeHandled = false)
        handleRenaming(newName, fileName, useProviderLink = true, fromRemovableStorage = true, widgetView, shouldBeHandled = isEmulator())
    }

    @Test
    fun fullyQualifiedRemoved() {
        val widgetView = addWidget(fileName, useProviderLink = false, fromRemovableStorage = true, getPlacementPoint())
        viewDocument(widgetView, fileName)

        handleRemoving(fileName, useProviderLink = false, fromRemovableStorage = true, widgetView)
        handleRestoring(fileName, widgetView, shouldBeHandled = true, fileUris)
    }

    @Test
    fun providerLinkRemoved() {
        val widgetView = addWidget(fileName, useProviderLink = true, fromRemovableStorage = true, getPlacementPoint())
        viewDocument(widgetView, fileName)

        handleRemoving(fileName, useProviderLink = true, fromRemovableStorage = true, widgetView)
        handleRestoring(fileName, widgetView, shouldBeHandled = false, fileUris)
    }
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
