package com.example.xbible.viewmodel

import android.app.Application
import com.example.xbible.data.BibleRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EngineViewModelTest {

    private val application: Application = mockk()
    private val repository: BibleRepository = mockk()
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun when_initialization_is_successful_isReady_should_be_true() {
        runTest {
            // Given
            coEvery { repository.initialize() } returns Result.success(Unit)
            
            // When
            val viewModel = EngineViewModel(application, repository)
            
            // Then
            assertTrue(viewModel.isReady.value)
            assertEquals(null, viewModel.errorMessage.value)
        }
    }

    @Test
    fun when_initialization_fails_errorMessage_should_be_set() {
        runTest {
            // Given
            val errorMsg = "Path error"
            coEvery { repository.initialize() } returns Result.failure(Throwable(errorMsg))
            
            // When
            val viewModel = EngineViewModel(application, repository)
            
            // Then
            assertEquals(false, viewModel.isReady.value)
            assertTrue(viewModel.errorMessage.value?.contains(errorMsg) == true)
        }
    }
}
