package ol.ko.docshortcut

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import ol.ko.docshortcut.utils.FileUrisDataStore
import org.junit.After
import org.junit.Before
import java.io.File

@ExperimentalCoroutinesApi
open class DataStoreBaseTest(protected val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()) {

    private lateinit var preferencesDataStoreFile: File
    private lateinit var storeScope: CoroutineScope
    protected lateinit var testDataStore: DataStore<Preferences>

    @Before
    fun setUpDataStore() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        preferencesDataStoreFile = context.preferencesDataStoreFile("test-preferences-file")
        storeScope = CoroutineScope(testDispatcher + SupervisorJob())
        testDataStore = PreferenceDataStoreFactory.create(scope = storeScope) {
            preferencesDataStoreFile
        }
        mockkObject(FileUrisDataStore)
        every { FileUrisDataStore.getInstance(any()) } returns testDataStore
    }

    @After
    fun tearDownDataStore() {
        unmockkObject(FileUrisDataStore)
        storeScope.cancel()
        preferencesDataStoreFile.delete()
    }
}
