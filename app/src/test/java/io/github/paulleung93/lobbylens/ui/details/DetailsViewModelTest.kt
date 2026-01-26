package io.github.paulleung93.lobbylens.ui.details

import app.cash.turbine.test
import io.github.paulleung93.lobbylens.data.model.FecCandidate
import io.github.paulleung93.lobbylens.data.model.FecCandidateResponse
import io.github.paulleung93.lobbylens.data.model.FecCommitteeHistory
import io.github.paulleung93.lobbylens.data.model.FecCommitteeHistoryResponse
import io.github.paulleung93.lobbylens.data.model.FecEmployerContribution
import io.github.paulleung93.lobbylens.data.model.FecEmployerContributionResponse
import io.github.paulleung93.lobbylens.data.model.SenateContributionReport
import io.github.paulleung93.lobbylens.data.model.SenateContributionResponse
import io.github.paulleung93.lobbylens.data.model.SenateRegistrant
import io.github.paulleung93.lobbylens.data.repository.ContributionRepository
import io.github.paulleung93.lobbylens.data.repository.FecRepository
import io.github.paulleung93.lobbylens.data.repository.SenateRepository
import io.github.paulleung93.lobbylens.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
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
 * Unit tests for DetailsViewModel.
 * Tests fetching of historical data, filter/sort actions, and error handling.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DetailsViewModelTest {

    // Mocked dependencies
    private lateinit var fecRepository: FecRepository
    private lateinit var contributionRepository: ContributionRepository
    private lateinit var senateRepository: SenateRepository

    // Class under test
    private lateinit var viewModel: DetailsViewModel

    // Test dispatcher for coroutines
    private val testDispatcher = StandardTestDispatcher()

    // Test Data
    private val testCandidateId = "H8CA05035"
    private val testCommitteeId = "C00589093"
    private val testCandidate = FecCandidate(
        candidateId = testCandidateId,
        name = "PELOSI, NANCY",
        officeSought = "H",
        state = "CA",
        party = "DEM"
    )
    private val testCommitteeHistory = FecCommitteeHistory(
        committeeId = testCommitteeId,
        designation = "P",
        designationFull = "Principal campaign committee",
        cycle = 2024,
        name = "NANCY PELOSI FOR CONGRESS"
    )
    private val testContribution = FecEmployerContribution(
        employer = "GOOGLE LLC",
        total = 50000.0,
        count = 25,
        type = "Employer"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        fecRepository = mockk(relaxed = true)
        contributionRepository = mockk(relaxed = true)
        senateRepository = mockk(relaxed = true)

        viewModel = DetailsViewModel(
            fecRepository = fecRepository,
            contributionRepository = contributionRepository,
            senateRepository = senateRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== fetchHistoricalData Tests ==========

    @Test
    fun `fetchHistoricalData emits Loading then Success with Senate data when API returns valid data`() = runTest {
        // Arrange
        val testSenateReport = SenateContributionReport(
            filingUuid = "123",
            filingYear = 2024,
            filingPeriod = "Q1",
            registrant = SenateRegistrant("Test Corp", 1)
        )
        
        coEvery { fecRepository.getCandidateCommitteeHistory(testCandidateId) } returns Result.Success(
            FecCommitteeHistoryResponse(listOf(testCommitteeHistory))
        )
        coEvery { fecRepository.getCandidateDetails(testCandidateId) } returns Result.Success(
            FecCandidateResponse(listOf(testCandidate))
        )
        coEvery { contributionRepository.getTopOrganizations(testCommitteeId, any()) } returns Result.Success(
            FecEmployerContributionResponse(listOf(testContribution))
        )
        coEvery { senateRepository.getContributions(any()) } returns Result.Success(
            SenateContributionResponse(count = 1, next = null, previous = null, results = listOf(testSenateReport))
        )

        // Act & Assert
        viewModel.uiState.test {
            // Initial state
            assertEquals(DetailsUiState.Loading, awaitItem())

            // Trigger fetch
            viewModel.fetchHistoricalData(testCandidateId)
            
            // 1. Loading state (re-emitted) - StateFlow might skip if it's identical, 
            // but in the VM we set it again. However, MutableStateFlow only emits on change.
            // Since initial is Loading, the explicit set to Loading at line 91 might not emit.

            // 2. First Success: Campaign data loaded, isSenateLoading = true
            val campaignSuccess = awaitItem()
            assertTrue(campaignSuccess is DetailsUiState.Success)
            val success1 = campaignSuccess as DetailsUiState.Success
            assertEquals(true, success1.isSenateLoading)
            assertTrue(success1.senateContributions.isEmpty())

            // 3. Second Success: Senate data loaded, isSenateLoading = false
            val finalSuccess = awaitItem()
            assertTrue(finalSuccess is DetailsUiState.Success)
            val success2 = finalSuccess as DetailsUiState.Success
            assertEquals(false, success2.isSenateLoading)
            assertEquals(1, success2.senateContributions.size)
            assertEquals("123", success2.senateContributions[0].filingUuid)
            
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `fetchHistoricalData emits Error when committee history API fails`() = runTest {
        // Arrange
        coEvery { fecRepository.getCandidateCommitteeHistory(testCandidateId) } returns Result.Error(
            Exception("Network error")
        )

        // Act & Assert
        viewModel.uiState.test {
            assertEquals(DetailsUiState.Loading, awaitItem())

            viewModel.fetchHistoricalData(testCandidateId)
            advanceUntilIdle()

            val errorState = awaitItem()
            assertTrue("Expected Error state, got $errorState", errorState is DetailsUiState.Error)
            assertEquals("Failed to load committee history.", (errorState as DetailsUiState.Error).message)
            
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `fetchHistoricalData emits Error when no principal committee found`() = runTest {
        // Arrange - committee history without principal committee (designation != "P")
        val nonPrincipalCommittee = testCommitteeHistory.copy(designation = "A")
        coEvery { fecRepository.getCandidateCommitteeHistory(testCandidateId) } returns Result.Success(
            FecCommitteeHistoryResponse(listOf(nonPrincipalCommittee))
        )

        // Act & Assert
        viewModel.uiState.test {
            assertEquals(DetailsUiState.Loading, awaitItem())

            viewModel.fetchHistoricalData(testCandidateId)
            advanceUntilIdle()

            val errorState = awaitItem()
            assertTrue("Expected Error state", errorState is DetailsUiState.Error)
            assertEquals("No principal campaign committee found.", (errorState as DetailsUiState.Error).message)
            
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `fetchHistoricalData emits Error when candidate details fail`() = runTest {
        // Arrange
        coEvery { fecRepository.getCandidateCommitteeHistory(testCandidateId) } returns Result.Success(
            FecCommitteeHistoryResponse(listOf(testCommitteeHistory))
        )
        coEvery { fecRepository.getCandidateDetails(testCandidateId) } returns Result.Success(
            FecCandidateResponse(emptyList()) // No candidate returned
        )

        // Act & Assert
        viewModel.uiState.test {
            assertEquals(DetailsUiState.Loading, awaitItem())

            viewModel.fetchHistoricalData(testCandidateId)
            advanceUntilIdle()

            val errorState = awaitItem()
            assertTrue("Expected Error state", errorState is DetailsUiState.Error)
            assertEquals("Could not load candidate details.", (errorState as DetailsUiState.Error).message)
            
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `fetchHistoricalData triggers fetchSenateData after campaign data loads`() = runTest {
        // Arrange
        coEvery { fecRepository.getCandidateCommitteeHistory(testCandidateId) } returns Result.Success(
            FecCommitteeHistoryResponse(listOf(testCommitteeHistory))
        )
        coEvery { fecRepository.getCandidateDetails(testCandidateId) } returns Result.Success(
            FecCandidateResponse(listOf(testCandidate))
        )
        coEvery { contributionRepository.getTopOrganizations(testCommitteeId, any()) } returns Result.Success(
            FecEmployerContributionResponse(listOf(testContribution))
        )
        coEvery { senateRepository.getContributions(any()) } returns Result.Success(
            SenateContributionResponse(count = 0, next = null, previous = null, results = emptyList())
        )

        // Act
        viewModel.fetchHistoricalData(testCandidateId)
        advanceUntilIdle()

        // Assert - verify Senate API was called
        coVerify(exactly = 1) { senateRepository.getContributions(any()) }
    }

    // ========== getFilteredOrganizations Tests ==========

    @Test
    fun `getFilteredOrganizations correctly sorts by highest amount`() = runTest {
        // Arrange
        val contribution1 = FecEmployerContribution("Company A", 10000.0, 5, "Employer")
        val contribution2 = FecEmployerContribution("Company B", 50000.0, 20, "Employer")
        val contribution3 = FecEmployerContribution("Company C", 25000.0, 10, "Employer")
        val historicalOrganizations = mapOf(
            "2024" to listOf(contribution1, contribution2, contribution3)
        )

        // Update filter to select 2024 and sort by highest amount
        viewModel.selectYear("2024")
        viewModel.updateCampaignSort(CampaignSortOption.AMOUNT_DESC)
        advanceUntilIdle()

        // Act
        val result = viewModel.getFilteredOrganizations(historicalOrganizations)

        // Assert
        assertEquals(3, result.size)
        assertEquals(50000.0, result[0].total, 0.01)
        assertEquals(25000.0, result[1].total, 0.01)
        assertEquals(10000.0, result[2].total, 0.01)
    }

    @Test
    fun `getFilteredOrganizations correctly filters by search query`() = runTest {
        // Arrange
        val contribution1 = FecEmployerContribution("GOOGLE LLC", 50000.0, 25, "Employer")
        val contribution2 = FecEmployerContribution("APPLE INC", 30000.0, 15, "Employer")
        val contribution3 = FecEmployerContribution("MICROSOFT CORP", 20000.0, 10, "Employer")
        val historicalOrganizations = mapOf(
            "2024" to listOf(contribution1, contribution2, contribution3)
        )

        // Update filter with search query
        viewModel.selectYear("2024")
        viewModel.updateCampaignSearchQuery("GOOGLE")
        advanceUntilIdle()

        // Act
        val result = viewModel.getFilteredOrganizations(historicalOrganizations)

        // Assert
        assertEquals(1, result.size)
        assertEquals("GOOGLE LLC", result[0].employer)
    }

    @Test
    fun `getFilteredOrganizations returns empty list for All year selection`() = runTest {
        // Arrange
        val contribution1 = FecEmployerContribution("Test Company", 10000.0, 5, "Employer")
        val historicalOrganizations = mapOf("2024" to listOf(contribution1))

        viewModel.selectYear("All")
        advanceUntilIdle()

        // Act
        val result = viewModel.getFilteredOrganizations(historicalOrganizations)

        // Assert
        assertTrue(result.isEmpty())
    }
}
