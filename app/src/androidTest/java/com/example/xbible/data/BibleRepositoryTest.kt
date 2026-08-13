package com.example.xbible.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BibleRepositoryTest {

    private lateinit var repository: BibleRepository

    @Before
    fun setup() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        repository = BibleRepository(appContext as android.app.Application)
    }

    @Test
    fun testEngineInitialization() = runBlocking {
        // When
        val result = repository.initialize()
        
        // Then
        assertTrue("Engine should initialize successfully: ${result.exceptionOrNull()?.message}", result.isSuccess)
        assertTrue("Repository should be reported as initialized", repository.isInitialized())
    }

    @Test
    fun testGetBibleModules() = runBlocking {
        // Given
        repository.initialize()
        
        // When
        val modules = repository.getBibleModules()
        
        // Then
        assertNotNull("Modules list should not be null", modules)
        // Note: might be empty if no modules are installed, which is expected on first run
    }
}
