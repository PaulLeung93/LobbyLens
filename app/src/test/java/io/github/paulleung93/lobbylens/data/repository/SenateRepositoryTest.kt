package io.github.paulleung93.lobbylens.data.repository

import io.github.paulleung93.lobbylens.data.api.SenateLdaApiService
import io.github.paulleung93.lobbylens.data.local.SenateCacheDao
import io.github.paulleung93.lobbylens.data.model.SenateContributionReport
import io.github.paulleung93.lobbylens.data.model.SenateContributionResponse
import io.github.paulleung93.lobbylens.data.model.SenateRegistrant
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
import java.net.SocketTimeoutException

/**
 * Unit tests for SenateRepository.
 * Tests Senate LDA API interactions, caching, and name normalization.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SenateRepositoryTest {

    // Mocked dependencies
    private lateinit var apiService: SenateLdaApiService
    private lateinit var cacheDao: SenateCacheDao

    // Class under test
    private lateinit var repository: SenateRepository

    // Test dispatcher
    private val testDispatcher = StandardTestDispatcher()

    // Test Data
    private val testRegistrant = SenateRegistrant(
        name = "LOBBYING FIRM INC",
        registrantId = 12345
    )
    private val testReport = SenateContributionReport(
        filingUuid = "uuid-123",
        filingYear = 2024,
        filingPeriod = "2024Q1",
        registrant = testRegistrant,
        contributionItems = emptyList()
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        apiService = mockk(relaxed = true)
        cacheDao = mockk(relaxed = true)

        repository = SenateRepository(
            apiService = apiService,
            cacheDao = cacheDao
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== getContributions Tests ==========

    @Test
    fun `getContributions returns Success when API returns data`() = runTest {
        // Arrange
        coEvery { cacheDao.getContributions(any(), any()) } returns emptyList()
        coEvery { apiService.getContributions(any(), any(), any()) } returns Response.success(
            SenateContributionResponse(count = 1, next = null, previous = null, results = listOf(testReport))
        )

        // Act
        val result = repository.getContributions("PELOSI, NANCY")

        // Assert
        assertTrue("Expected Success result", result is Result.Success)
        val success = result as Result.Success
        assertEquals(1, success.data.results.size)
        assertEquals("LOBBYING FIRM INC", success.data.results[0].registrant.name)
    }

    @Test
    fun `getContributions caches results after fetching`() = runTest {
        // Arrange
        coEvery { cacheDao.getContributions(any(), any()) } returns emptyList()
        coEvery { apiService.getContributions(any(), any(), any()) } returns Response.success(
            SenateContributionResponse(count = 1, next = null, previous = null, results = listOf(testReport))
        )

        // Act
        repository.getContributions("PELOSI, NANCY")

        // Assert - verify caching
        coVerify { cacheDao.insertContributions(any()) }
    }

    @Test
    fun `getContributions handles API timeout gracefully`() = runTest {
        // Arrange
        coEvery { cacheDao.getContributions(any(), any()) } returns emptyList()
        coEvery { apiService.getContributions(any(), any(), any()) } throws SocketTimeoutException("Timeout")

        // Act
        val result = repository.getContributions("PELOSI, NANCY")

        // Assert
        assertTrue("Expected Error result", result is Result.Error)
        assertTrue((result as Result.Error).exception is SocketTimeoutException)
    }

    @Test
    fun `getContributions normalizes name correctly from FEC format`() = runTest {
        // Arrange
        coEvery { cacheDao.getContributions(any(), any()) } returns emptyList()
        coEvery { apiService.getContributions(honoreeName = "NANCY PELOSI", any(), any()) } returns Response.success(
            SenateContributionResponse(count = 1, next = null, previous = null, results = listOf(testReport))
        )

        // Act - Pass FEC format name
        repository.getContributions("PELOSI, NANCY")

        // Assert - Verify API was called with normalized name
        coVerify { apiService.getContributions(honoreeName = "NANCY PELOSI", any(), any()) }
    }

    @Test
    fun `getContributions handles empty results`() = runTest {
        // Arrange
        coEvery { cacheDao.getContributions(any(), any()) } returns emptyList()
        coEvery { apiService.getContributions(any(), any(), any()) } returns Response.success(
            SenateContributionResponse(count = 0, next = null, previous = null, results = emptyList())
        )

        // Act
        val result = repository.getContributions("UNKNOWN, PERSON")

        // Assert
        assertTrue("Expected Success result", result is Result.Success)
        val success = result as Result.Success
        assertEquals(0, success.data.results.size)
    }
}
