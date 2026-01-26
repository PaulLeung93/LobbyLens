package io.github.paulleung93.lobbylens.ui.editor

import android.app.Application
import app.cash.turbine.test
import io.github.paulleung93.lobbylens.data.model.FecCandidate
import io.github.paulleung93.lobbylens.data.model.FecCandidateResponse
import io.github.paulleung93.lobbylens.data.repository.ContributionRepository
import io.github.paulleung93.lobbylens.data.repository.FecRepository
import io.github.paulleung93.lobbylens.data.repository.ImageGenerationRepository
import io.github.paulleung93.lobbylens.data.repository.VisionRepository
import io.github.paulleung93.lobbylens.util.Result
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for EditorViewModel.
 * Tests candidate search, state transitions, and error handling.
 *
 * Note: Tests that involve Android Context (processImage, generateImage) are limited
 * in unit tests due to AndroidViewModel dependency. Full testing requires instrumented tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {

    // Mocked dependencies
    private lateinit var application: Application
    private lateinit var fecRepository: FecRepository
    private lateinit var visionRepository: VisionRepository
    private lateinit var imageGenRepository: ImageGenerationRepository
    private lateinit var contributionRepository: ContributionRepository

    // Class under test
    private lateinit var viewModel: EditorViewModel

    // Test dispatcher for coroutines
    private val testDispatcher = StandardTestDispatcher()

    // Test Data
    private val testCandidate = FecCandidate(
        candidateId = "H8CA05035",
        name = "PELOSI, NANCY",
        officeSought = "H",
        state = "CA",
        party = "DEM"
    )
    private val testCandidate2 = FecCandidate(
        candidateId = "S2CA00285",
        name = "FEINSTEIN, DIANNE",
        officeSought = "S",
        state = "CA",
        party = "DEM"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        application = mockk(relaxed = true)
        fecRepository = mockk(relaxed = true)
        visionRepository = mockk(relaxed = true)
        imageGenRepository = mockk(relaxed = true)
        contributionRepository = mockk(relaxed = true)

        viewModel = EditorViewModel(
            application = application,
            fecRepository = fecRepository,
            visionRepository = visionRepository,
            imageGenRepository = imageGenRepository,
            contributionRepository = contributionRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== searchCandidatesByName Tests ==========

    @Test
    fun `searchCandidatesByName updates state to SearchResults with candidates`() = runTest {
        // Arrange
        val candidates = listOf(testCandidate, testCandidate2)
        coEvery { fecRepository.searchCandidatesByName("PELOSI") } returns Result.Success(
            FecCandidateResponse(candidates)
        )

        // Act & Assert
        viewModel.uiState.test {
            // Initial state
            assertEquals(EditorUiState.Initial, awaitItem())

            // Trigger search
            viewModel.searchCandidatesByName("PELOSI")
            
            // Loading state with empty candidates
            val loadingState = awaitItem()
            assertTrue("Expected SearchResults with isLoading=true", 
                loadingState is EditorUiState.SearchResults && loadingState.isLoading)
            
            advanceUntilIdle()
            
            // Success state with candidates
            val successState = awaitItem()
            assertTrue("Expected SearchResults with candidates", successState is EditorUiState.SearchResults)
            val results = successState as EditorUiState.SearchResults
            assertEquals(2, results.candidates.size)
            assertEquals(false, results.isLoading)
            
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `searchCandidatesByName handles empty results gracefully`() = runTest {
        // Arrange
        coEvery { fecRepository.searchCandidatesByName("NONEXISTENT") } returns Result.Success(
            FecCandidateResponse(emptyList())
        )

        // Act & Assert
        viewModel.uiState.test {
            assertEquals(EditorUiState.Initial, awaitItem())

            viewModel.searchCandidatesByName("NONEXISTENT")
            
            // Loading state
            val loadingState = awaitItem()
            assertTrue(loadingState is EditorUiState.SearchResults && loadingState.isLoading)
            
            advanceUntilIdle()
            
            // Success with empty list
            val resultState = awaitItem()
            assertTrue(resultState is EditorUiState.SearchResults)
            assertEquals(0, (resultState as EditorUiState.SearchResults).candidates.size)
            
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `searchCandidatesByName emits Error when blank query provided`() = runTest {
        // Act & Assert
        viewModel.uiState.test {
            assertEquals(EditorUiState.Initial, awaitItem())

            // Search with blank query
            viewModel.searchCandidatesByName("")
            
            val errorState = awaitItem()
            assertTrue("Expected Error state", errorState is EditorUiState.Error)
            assertEquals("Please enter a candidate name to search.", (errorState as EditorUiState.Error).message)
            
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `searchCandidatesByName emits Error when API fails`() = runTest {
        // Arrange
        coEvery { fecRepository.searchCandidatesByName(any()) } returns Result.Error(
            Exception("Network timeout")
        )

        // Act & Assert
        viewModel.uiState.test {
            assertEquals(EditorUiState.Initial, awaitItem())

            viewModel.searchCandidatesByName("PELOSI")
            
            // Loading state
            awaitItem()
            advanceUntilIdle()
            
            val errorState = awaitItem()
            assertTrue("Expected Error state", errorState is EditorUiState.Error)
            assertTrue((errorState as EditorUiState.Error).message.contains("Failed to fetch candidates"))
            
            cancelAndConsumeRemainingEvents()
        }
    }

    // ========== loadCongressMembers Tests ==========

    @Test
    fun `loadCongressMembers updates state with list of incumbents`() = runTest {
        // Arrange
        val members = listOf(testCandidate, testCandidate2)
        coEvery { fecRepository.getCongressMembers(any()) } returns Result.Success(
            FecCandidateResponse(members)
        )

        // Act & Assert
        viewModel.uiState.test {
            assertEquals(EditorUiState.Initial, awaitItem())

            viewModel.loadCongressMembers()
            
            // Loading state
            val loadingState = awaitItem()
            assertTrue(loadingState is EditorUiState.SearchResults && loadingState.isLoading)
            
            advanceUntilIdle()
            
            // Success with members
            val resultState = awaitItem()
            assertTrue(resultState is EditorUiState.SearchResults)
            assertEquals(2, (resultState as EditorUiState.SearchResults).candidates.size)
            
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `loadCongressMembers emits Error when API fails`() = runTest {
        // Arrange
        coEvery { fecRepository.getCongressMembers(any()) } returns Result.Error(
            Exception("Server error")
        )

        // Act & Assert
        viewModel.uiState.test {
            assertEquals(EditorUiState.Initial, awaitItem())

            viewModel.loadCongressMembers()
            awaitItem() // Loading
            advanceUntilIdle()
            
            val errorState = awaitItem()
            assertTrue("Expected Error state", errorState is EditorUiState.Error)
            
            cancelAndConsumeRemainingEvents()
        }
    }

    // ========== resetState Tests ==========

    @Test
    fun `resetState returns UI to Initial state`() = runTest {
        // Arrange - First put ViewModel in a different state
        coEvery { fecRepository.searchCandidatesByName(any()) } returns Result.Success(
            FecCandidateResponse(listOf(testCandidate))
        )
        
        viewModel.searchCandidatesByName("TEST")
        advanceUntilIdle()

        // Act
        viewModel.resetState()

        // Assert
        assertEquals(EditorUiState.Initial, viewModel.uiState.value)
    }

    // ========== updateSearchQuery Tests ==========

    @Test
    fun `updateSearchQuery updates searchQuery state`() = runTest {
        // Act
        viewModel.updateSearchQuery("Biden")
        
        // Assert
        assertEquals("Biden", viewModel.searchQuery.value)
    }
}
