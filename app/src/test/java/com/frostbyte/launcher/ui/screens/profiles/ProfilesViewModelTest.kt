package com.frostbyte.launcher.ui.screens.profiles

import com.frostbyte.launcher.core.common.Loader
import com.frostbyte.launcher.core.storage.repository.FakeProfileDao
import com.frostbyte.launcher.core.storage.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProfilesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ProfilesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ProfilesViewModel(ProfileRepository(FakeProfileDao()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `dialog starts closed`() = runTest(testDispatcher) {
        assertFalse(viewModel.uiState.first().isCreateDialogOpen)
    }

    @Test
    fun `openCreateDialog and dismissCreateDialog toggle dialog state`() = runTest(testDispatcher) {
        viewModel.openCreateDialog()
        assertTrue(viewModel.uiState.first().isCreateDialogOpen)

        viewModel.dismissCreateDialog()
        assertFalse(viewModel.uiState.first().isCreateDialogOpen)
    }

    @Test
    fun `createProfile with valid input adds profile and closes dialog`() = runTest(testDispatcher) {
        viewModel.openCreateDialog()
        viewModel.createProfile("Survival", "1.21.1", Loader.FABRIC, ramGb = 4)
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertFalse(state.isCreateDialogOpen)
        assertEquals(1, state.profiles.size)
        assertEquals("Survival", state.profiles.first().name)
        assertEquals(4096, state.profiles.first().ramAllocationMb)
    }

    @Test
    fun `createProfile with blank name surfaces error and keeps dialog open`() = runTest(testDispatcher) {
        viewModel.openCreateDialog()
        viewModel.createProfile("   ", "1.21.1", Loader.FABRIC, ramGb = 4)
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertTrue(state.isCreateDialogOpen)
        assertNotNull(state.errorMessage)
        assertEquals(0, state.profiles.size)
    }

    @Test
    fun `dismissError clears the error message`() = runTest(testDispatcher) {
        viewModel.createProfile("", "1.21.1", Loader.FABRIC, ramGb = 4)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.first().errorMessage)

        viewModel.dismissError()
        assertEquals(null, viewModel.uiState.first().errorMessage)
    }
}
