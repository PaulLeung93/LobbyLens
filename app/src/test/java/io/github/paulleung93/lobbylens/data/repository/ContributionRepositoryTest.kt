package io.github.paulleung93.lobbylens.data.repository

import io.github.paulleung93.lobbylens.data.api.FecApiService
import io.github.paulleung93.lobbylens.data.local.CachedContribution
import io.github.paulleung93.lobbylens.data.local.ContributionCacheDao
import io.github.paulleung93.lobbylens.data.model.FecEmployerContribution
import io.github.paulleung93.lobbylens.data.model.FecEmployerContributionResponse
import io.github.paulleung93.lobbylens.data.model.FecScheduleA
import io.github.paulleung93.lobbylens.data.model.FecScheduleAResponse
import io.github.paulleung93.lobbylens.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * Unit tests for ContributionRepository.
 * Tests data merging, sorting, and caching behavior.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ContributionRepositoryTest {

    // Mocked dependencies
    private lateinit var apiService: FecApiService
    private lateinit var cacheDao: ContributionCacheDao

    // Class under test
    private lateinit var repository: ContributionRepository

    // Test dispatcher
    private val testDispatcher = StandardTestDispatcher()

    // Test Data
    private val employerContribution1 = FecEmployerContribution(
        employer = "GOOGLE LLC",
        total = 50000.0,
        count = 25,
        type = "Employer"
    )
    private val employerContribution2 = FecEmployerContribution(
        employer = "MICROSOFT CORP",
        total = 30000.0,
        count = 15,
        type = "Employer"
    )
    
    // PAC data comes as Schedule A
    private val pacContribution = FecScheduleA(
        contributorName = "DEMOCRACY PAC",
        amount = 40000.0,
        entityType = "PAC",
        contributionReceiptDate = "2024-01-01"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        apiService = mockk(relaxed = true)
        cacheDao = mockk(relaxed = true)

        repository = ContributionRepository(
            apiService = apiService,
            cacheDao = cacheDao
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== getTopOrganizations Tests ==========

    @Test
    fun `getTopOrganizations merges employer and PAC lists correctly`() = runTest {
        // Arrange - Both employer and PAC APIs return data
        coEvery { cacheDao.getContributions(any(), any()) } returns emptyList()
        
        // Mock API calls with named arguments matching Repository calls
        coEvery { 
            apiService.getTopOrganizationsByEmployer(committeeId = any(), cycle = any()) 
        } returns Response.success(
            FecEmployerContributionResponse(listOf(employerContribution1, employerContribution2))
        )
        
        coEvery { 
            apiService.getPacContributions(committeeId = any(), cycle = any()) 
        } returns Response.success(
            FecScheduleAResponse(listOf(pacContribution))
        )

        // Act
        val result = repository.getTopOrganizations("C00589093", "2024")

        // Assert
        assertTrue("Expected Success result", result is Result.Success)
        val success = result as Result.Success
        // 2 employers + 1 aggregated PAC = 3
        assertEquals(3, success.data.results.size) 
        
        val pacResult = success.data.results.find { it.employer == "DEMOCRACY PAC" }
        assertTrue("PAC should be present", pacResult != null)
        assertEquals("PAC", pacResult?.type)
        assertEquals(40000.0, pacResult?.total ?: 0.0, 0.01)
    }

    @Test
    fun `getTopOrganizations sorts merged list by amount descending`() = runTest {
        // Arrange
        coEvery { cacheDao.getContributions(any(), any()) } returns emptyList()
        coEvery { 
            apiService.getTopOrganizationsByEmployer(committeeId = any(), cycle = any()) 
        } returns Response.success(
            FecEmployerContributionResponse(listOf(employerContribution1, employerContribution2))
        )
        coEvery { 
            apiService.getPacContributions(committeeId = any(), cycle = any()) 
        } returns Response.success(
            FecScheduleAResponse(listOf(pacContribution))
        )

        // Act
        val result = repository.getTopOrganizations("C00589093", "2024")

        // Assert
        assertTrue("Expected Success result", result is Result.Success)
        val success = result as Result.Success
        val sorted = success.data.results
        
        // Output order should be: Google (50k), PAC (40k), Microsoft (30k)
        assertEquals(50000.0, sorted[0].total, 0.01)
        assertEquals("GOOGLE LLC", sorted[0].employer)
        
        assertEquals(40000.0, sorted[1].total, 0.01)
        assertEquals("DEMOCRACY PAC", sorted[1].employer)
        
        assertEquals(30000.0, sorted[2].total, 0.01)
        assertEquals("MICROSOFT CORP", sorted[2].employer)
    }

    @Test
    fun `getTopOrganizations returns cached data when available`() = runTest {
        // Arrange - Cache has data
        val cachedContributions = listOf(
            CachedContribution(
                cacheKey = "C00589093-2024",
                employer = "CACHED COMPANY",
                total = 100000.0,
                count = 50,
                type = "Employer",
                mostRecentDate = null
            )
        )
        coEvery { cacheDao.getContributions(any(), any()) } returns cachedContributions

        // Act
        val result = repository.getTopOrganizations("C00589093", "2024")

        // Assert
        assertTrue("Expected Success result", result is Result.Success)
        val success = result as Result.Success
        assertEquals(1, success.data.results.size)
        assertEquals("CACHED COMPANY", success.data.results[0].employer)
        
        // Verify API was NOT called
        coVerify(exactly = 0) { apiService.getTopOrganizationsByEmployer(any(), any(), any()) }
        coVerify(exactly = 0) { apiService.getPacContributions(any(), any(), any()) }
    }

    @Test
    fun `getTopOrganizations returns Error when both APIs fail`() = runTest {
        // Arrange
        coEvery { cacheDao.getContributions(any(), any()) } returns emptyList()
        coEvery { 
            apiService.getTopOrganizationsByEmployer(committeeId = any(), cycle = any()) 
        } throws Exception("Network error")
        coEvery { 
            apiService.getPacContributions(committeeId = any(), cycle = any()) 
        } throws Exception("Network error")

        // Act
        val result = repository.getTopOrganizations("C00589093", "2024")

        // Assert
        assertTrue("Expected Error result", result is Result.Error)
    }
}
