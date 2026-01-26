package io.github.paulleung93.lobbylens.data.repository

import io.github.paulleung93.lobbylens.data.api.FecApiService
import io.github.paulleung93.lobbylens.data.local.CachedCandidate
import io.github.paulleung93.lobbylens.data.local.CandidateCacheDao
import io.github.paulleung93.lobbylens.data.model.FecCandidate
import io.github.paulleung93.lobbylens.data.model.FecCandidateResponse
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
 * Unit tests for FecRepository.
 * Tests cache-first strategy, API interactions, and error handling.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FecRepositoryTest {

    // Mocked dependencies
    private lateinit var apiService: FecApiService
    private lateinit var cacheDao: CandidateCacheDao

    // Class under test
    private lateinit var repository: FecRepository

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
    private val cachedCandidate = CachedCandidate(
        candidateId = "H8CA05035",
        name = "PELOSI, NANCY",
        party = "DEM",
        state = "CA",
        office = "H",
        electionYears = "2020,2022,2024",
        cachedAt = System.currentTimeMillis()
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        apiService = mockk(relaxed = true)
        cacheDao = mockk(relaxed = true)

        repository = FecRepository(
            apiService = apiService,
            cacheDao = cacheDao
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== searchCandidatesByName Tests ==========

    @Test
    fun `searchCandidatesByName returns cached data and does not call API when cache hit`() = runTest {
        // Arrange - Cache has data
        coEvery { cacheDao.searchCandidates(any(), any()) } returns listOf(cachedCandidate)

        // Act
        val result = repository.searchCandidatesByName("PELOSI")

        // Assert
        assertTrue("Expected Success result", result is Result.Success)
        val success = result as Result.Success
        assertEquals(1, success.data.results.size)
        assertEquals("PELOSI, NANCY", success.data.results[0].name)
        
        // Verify API was NOT called
        coVerify(exactly = 0) { apiService.searchCandidates(any(), any(), any()) }
    }

    @Test
    fun `searchCandidatesByName calls API and caches result when cache miss`() = runTest {
        // Arrange - Cache is empty, API returns data
        coEvery { cacheDao.searchCandidates(any(), any()) } returns emptyList()
        coEvery { apiService.searchCandidates(any(), any(), any()) } returns Response.success(
            FecCandidateResponse(listOf(testCandidate))
        )

        // Act
        val result = repository.searchCandidatesByName("PELOSI")

        // Assert
        assertTrue("Expected Success result", result is Result.Success)
        val success = result as Result.Success
        assertEquals(1, success.data.results.size)
        
        // Verify API was called
        coVerify(atLeast = 1) { apiService.searchCandidates(any(), any(), any()) }
        
        // Verify data was cached
        coVerify { cacheDao.insertCandidates(any()) }
    }

    @Test
    fun `searchCandidatesByName returns Error when API fails and cache is empty`() = runTest {
        // Arrange
        coEvery { cacheDao.searchCandidates(any(), any()) } returns emptyList()
        coEvery { apiService.searchCandidates(any(), any(), any()) } throws Exception("Network error")

        // Act
        val result = repository.searchCandidatesByName("PELOSI")

        // Assert
        assertTrue("Expected Error result", result is Result.Error)
    }

    @Test
    fun `searchCandidatesByName handles multiple pages correctly`() = runTest {
        // Arrange - Cache is empty, API returns full page then partial
        val fullPage = (1..100).map { 
            testCandidate.copy(candidateId = "ID$it", name = "CANDIDATE $it") 
        }
        val lastPage = (1..50).map { 
            testCandidate.copy(candidateId = "ID${100 + it}", name = "CANDIDATE ${100 + it}") 
        }
        
        coEvery { cacheDao.searchCandidates(any(), any()) } returns emptyList()
        coEvery { apiService.searchCandidates(any(), any(), page = 1) } returns Response.success(
            FecCandidateResponse(fullPage)
        )
        coEvery { apiService.searchCandidates(any(), any(), page = 2) } returns Response.success(
            FecCandidateResponse(lastPage)
        )

        // Act
        val result = repository.searchCandidatesByName("CANDIDATE")

        // Assert
        assertTrue("Expected Success result", result is Result.Success)
        val success = result as Result.Success
        assertEquals(150, success.data.results.size) // 100 + 50
    }

    // ========== getCandidateDetails Tests ==========

    @Test
    fun `getCandidateDetails returns Success when API succeeds`() = runTest {
        // Arrange
        coEvery { apiService.getCandidateDetails(any()) } returns Response.success(
            FecCandidateResponse(listOf(testCandidate))
        )

        // Act
        val result = repository.getCandidateDetails("H8CA05035")

        // Assert
        assertTrue("Expected Success result", result is Result.Success)
        val success = result as Result.Success
        assertEquals("PELOSI, NANCY", success.data.results[0].name)
    }

    @Test
    fun `getCandidateDetails returns Error when API fails`() = runTest {
        // Arrange
        coEvery { apiService.getCandidateDetails(any()) } throws Exception("Not found")

        // Act
        val result = repository.getCandidateDetails("INVALID")

        // Assert
        assertTrue("Expected Error result", result is Result.Error)
    }
}
