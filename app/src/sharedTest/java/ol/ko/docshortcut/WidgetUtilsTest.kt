package ol.ko.docshortcut

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Bundle
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import ol.ko.docshortcut.utils.ContentResolverUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

open class WidgetUtilsBaseTest {

    companion object {
        const val APPWIDGET_ID_BASE = 42
        const val APPWIDGET_ID_FIRST = APPWIDGET_ID_BASE + 0
        const val APPWIDGET_ID_SECOND = APPWIDGET_ID_BASE + 1
        const val fileUriStringBase = "content://com.android.providers.downloads.documents/document/msf%3A13123"
    }

    protected fun buildContentUri(appWidgetId: Int) = "$fileUriStringBase${appWidgetId - APPWIDGET_ID_BASE}"

    protected val context: Context = ApplicationProvider.getApplicationContext()
    protected lateinit var glanceAppWidgetIds: List<GlanceId>
    protected lateinit var glanceAppWidgetManagerMock: GlanceAppWidgetManager
    protected lateinit var appWidgetManagerMock: AppWidgetManager

    @Before
    fun setUpBase() {
        // DEPENDS ON GLANCE IMPLEMENTATION TOO MUCH
        val appWidgetIdConstructor = Class.forName("androidx.glance.appwidget.AppWidgetId").getConstructor(Int::class.java)
        glanceAppWidgetIds = listOf(APPWIDGET_ID_FIRST, APPWIDGET_ID_SECOND).map { appWidgetId ->
            appWidgetIdConstructor.newInstance(appWidgetId) as GlanceId
        }
        glanceAppWidgetManagerMock = mockk()
        GlanceWidgetUtils.getGlanceAppWidgetManager = { glanceAppWidgetManagerMock }

        appWidgetManagerMock = mockk {
            every { updateAppWidget(more(APPWIDGET_ID_BASE, andEquals = true), any()) } just Runs
            every { getAppWidgetOptions(more(APPWIDGET_ID_BASE, andEquals = true)) } returns Bundle()
            every { getAppWidgetInfo(more(APPWIDGET_ID_BASE, andEquals = true)) } returns AppWidgetProviderInfo()
        }
        mockkStatic(AppWidgetManager::class)
        every { AppWidgetManager.getInstance(any()) } returns appWidgetManagerMock

        mockkObject(ContentResolverUtils)
        every { ContentResolverUtils.getFilenameFromUri(any(), any(), any()) } returns ""
    }

    @After
    fun tearDownBase() {
        // can't remove the preferences, but at least clean them
        runBlocking {
            glanceAppWidgetIds.forEach { glanceId ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) {
                    it.toMutablePreferences().apply { clear() }
                }
            }
        }

        unmockkStatic(AppWidgetManager::class)
        unmockkObject(ContentResolverUtils)
    }

    protected fun verifyUpdateAppWidgetCalls(appWidgetIds: List<Int>) {
        val appWidgetIdsSlotList = mutableListOf<Int>()
        verify(exactly = appWidgetIds.size) { appWidgetManagerMock.updateAppWidget(capture(appWidgetIdsSlotList), any()) }
        assertEquals(appWidgetIds, appWidgetIdsSlotList)
    }
}

@RunWith(AndroidJUnit4::class)
class GlanceAppWidgetStateTest : WidgetUtilsBaseTest() {

    @Test
    fun verifyInitialFill() {
        for (addedWidgets in (1..glanceAppWidgetIds.size)) {

            val appWidgetId = APPWIDGET_ID_BASE + addedWidgets - 1
            coEvery { glanceAppWidgetManagerMock.getGlanceIds<ShortcutGlanceWidget>(any()) } returns glanceAppWidgetIds.take(addedWidgets)
            runBlocking {
                GlanceWidgetUtils.fillInitialWidgetState(context, appWidgetId, buildContentUri(appWidgetId))
                val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceAppWidgetIds[addedWidgets - 1])
                assertEquals(appWidgetId, prefs[ShortcutGlanceWidget.appWidgetIdPreferenceKey])
                assertEquals(buildContentUri(appWidgetId), prefs[ShortcutGlanceWidget.fileUriPreferenceKey])
                assertEquals(true, prefs[ShortcutGlanceWidget.isFileUriValidPreferenceKey])
            }

            verifyUpdateAppWidgetCalls(listOf(appWidgetId))
            clearMocks(appWidgetManagerMock, answers = false, recordedCalls = true)
        }
    }
}

@RunWith(AndroidJUnit4::class)
class UpdateWidgetsTest : WidgetUtilsBaseTest() {

    @Before
    fun setUp() {
        coEvery { glanceAppWidgetManagerMock.getGlanceIds<ShortcutGlanceWidget>(any()) } returns glanceAppWidgetIds

        runBlocking {
            glanceAppWidgetIds.forEachIndexed { idx, glanceAppWidgetId ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceAppWidgetId) { preferences ->
                    preferences.toMutablePreferences()
                        .apply {
                            val appWidgetId = APPWIDGET_ID_BASE + idx
                            this[ShortcutGlanceWidget.appWidgetIdPreferenceKey] = appWidgetId
                            this[ShortcutGlanceWidget.fileUriPreferenceKey] = buildContentUri(appWidgetId)
                            this[ShortcutGlanceWidget.isFileUriValidPreferenceKey] = true
                        }
                }
            }
        }

        listOf(APPWIDGET_ID_FIRST, APPWIDGET_ID_SECOND).forEach { appWidgetId ->
            every { ContentResolverUtils.uriFileExists(any(), eq(buildContentUri(appWidgetId))) } returns true
        }
    }

    @Test
    fun becomesInvalid() {
        every { ContentResolverUtils.uriFileExists(any(), eq(buildContentUri(APPWIDGET_ID_SECOND))) } returns false

        runBlocking {
            GlanceWidgetUtils.updateWidgets(context)
            val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceAppWidgetIds.last())
            assertEquals(false, prefs[ShortcutGlanceWidget.isFileUriValidPreferenceKey])
        }

        verifyUpdateAppWidgetCalls(listOf(APPWIDGET_ID_SECOND))
    }

    @Test
    // rare case and actually works for the path (not link as e.g. fileUriStringBase) format of content uri
    fun becomesValid() {
        becomesInvalid()
        clearMocks(appWidgetManagerMock, answers = false, recordedCalls = true)

        every { ContentResolverUtils.uriFileExists(any(), eq(buildContentUri(APPWIDGET_ID_SECOND))) } returns true

        runBlocking {
            GlanceWidgetUtils.updateWidgets(context)
            val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceAppWidgetIds.last())
            assertEquals(true, prefs[ShortcutGlanceWidget.isFileUriValidPreferenceKey])
        }

        verifyUpdateAppWidgetCalls(listOf(APPWIDGET_ID_SECOND))
    }

    @Test
    fun updateUnconditionally() {
        every { ContentResolverUtils.uriFileExists(any(), eq(buildContentUri(APPWIDGET_ID_SECOND))) } returns false

        runBlocking {
            GlanceWidgetUtils.updateWidgets(context, onlyIfValidityChanged = false)
            val prefsUnchanged = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceAppWidgetIds.first())
            assertEquals(true, prefsUnchanged[ShortcutGlanceWidget.isFileUriValidPreferenceKey])
            val prefsChanged = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceAppWidgetIds.last())
            assertEquals(false, prefsChanged[ShortcutGlanceWidget.isFileUriValidPreferenceKey])
        }

        verifyUpdateAppWidgetCalls(listOf(APPWIDGET_ID_FIRST, APPWIDGET_ID_SECOND))
    }

    @Test
    fun updateOneUnchanged() {
        runBlocking {
            GlanceWidgetUtils.updateWidget(context, APPWIDGET_ID_FIRST)
            val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceAppWidgetIds.first())
            assertEquals(true, prefs[ShortcutGlanceWidget.isFileUriValidPreferenceKey])
        }
        verifyUpdateAppWidgetCalls(listOf(APPWIDGET_ID_FIRST))
    }

    @Test
    fun updateOneChanged() {
        every { ContentResolverUtils.uriFileExists(any(), eq(buildContentUri(APPWIDGET_ID_FIRST))) } returns false

        runBlocking {
            GlanceWidgetUtils.updateWidget(context, APPWIDGET_ID_FIRST)
            val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceAppWidgetIds.first())
            assertEquals(false, prefs[ShortcutGlanceWidget.isFileUriValidPreferenceKey])
        }

        verifyUpdateAppWidgetCalls(listOf(APPWIDGET_ID_FIRST))
    }
}