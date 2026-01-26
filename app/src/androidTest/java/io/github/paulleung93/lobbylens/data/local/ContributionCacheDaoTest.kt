package io.github.paulleung93.lobbylens.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Integration tests for ContributionCacheDao.
 * Tests composite key integrity and cache expiry.
 */
@RunWith(AndroidJUnit4::class)
class ContributionCacheDaoTest {

    private lateinit var database: LobbyLensDatabase
    private lateinit var dao: ContributionCacheDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LobbyLensDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.contributionCacheDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    // ========== Composite Key Integrity Tests ==========

    @Test
    fun compositeKeyIntegrity_multipleEmployersForSameCandidate() = runBlocking {
        // Arrange - Two contributions for same candidate (cacheKey) but different employers
        val contribution1 = CachedContribution(
            cacheKey = "C00589093-2024",
            employer = "GOOGLE LLC",
            total = 50000.0,
            count = 25,
            type = "Employer",
            mostRecentDate = "2024-01-15"
        )
        val contribution2 = CachedContribution(
            cacheKey = "C00589093-2024",
            employer = "MICROSOFT CORP",
            total = 30000.0,
            count = 15,
            type = "Employer",
            mostRecentDate = "2024-02-01"
        )

        // Act
        dao.insertContributions(listOf(contribution1, contribution2))
        val results = dao.getContributions("C00589093-2024", 0)

        // Assert - Both should exist (composite key: cacheKey + employer)
        assertEquals(2, results.size)
        assertTrue(results.any { it.employer == "GOOGLE LLC" })
        assertTrue(results.any { it.employer == "MICROSOFT CORP" })
    }

    @Test
    fun compositeKeyIntegrity_sameEmployerDifferentCycles() = runBlocking {
        // Arrange - Same employer for different cycles
        val contribution2024 = CachedContribution(
            cacheKey = "C00589093-2024",
            employer = "GOOGLE LLC",
            total = 50000.0,
            count = 25,
            type = "Employer",
            mostRecentDate = null
        )
        val contribution2022 = CachedContribution(
            cacheKey = "C00589093-2022",
            employer = "GOOGLE LLC",
            total = 40000.0,
            count = 20,
            type = "Employer",
            mostRecentDate = null
        )

        // Act
        dao.insertContributions(listOf(contribution2024, contribution2022))
        
        val results2024 = dao.getContributions("C00589093-2024", 0)
        val results2022 = dao.getContributions("C00589093-2022", 0)

        // Assert - Each cycle has its own entry
        assertEquals(1, results2024.size)
        assertEquals(50000.0, results2024[0].total, 0.01)
        
        assertEquals(1, results2022.size)
        assertEquals(40000.0, results2022[0].total, 0.01)
    }

    // ========== Clear Old Cache Tests ==========

    @Test
    fun clearOldCache_removesExpiredEntriesKeepsNew() = runBlocking {
        // Arrange
        val oldContribution = CachedContribution(
            cacheKey = "C00589093-2024",
            employer = "OLD COMPANY",
            total = 10000.0,
            count = 5,
            type = "Employer",
            mostRecentDate = null,
            cachedAt = System.currentTimeMillis() - (48 * 60 * 60 * 1000) // 48 hours ago
        )
        val newContribution = CachedContribution(
            cacheKey = "C00589093-2024",
            employer = "NEW COMPANY",
            total = 20000.0,
            count = 10,
            type = "Employer",
            mostRecentDate = null,
            cachedAt = System.currentTimeMillis() // Just now
        )
        dao.insertContributions(listOf(oldContribution, newContribution))

        // Act - Clear entries older than 24 hours
        val cutoff = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        dao.clearOldCache(cutoff)

        // Assert
        val results = dao.getContributions("C00589093-2024", 0)
        assertEquals(1, results.size)
        assertEquals("NEW COMPANY", results[0].employer)
    }

    // ========== Sorting Tests ==========

    @Test
    fun getContributions_returnsSortedByTotalDescending() = runBlocking {
        // Arrange
        val contributions = listOf(
            CachedContribution("KEY-2024", "SMALL CORP", 10000.0, 5, "Employer", null),
            CachedContribution("KEY-2024", "BIG CORP", 100000.0, 50, "Employer", null),
            CachedContribution("KEY-2024", "MEDIUM CORP", 50000.0, 25, "Employer", null)
        )
        dao.insertContributions(contributions)

        // Act
        val results = dao.getContributions("KEY-2024", 0)

        // Assert - Should be sorted by total DESC
        assertEquals(3, results.size)
        assertEquals("BIG CORP", results[0].employer)
        assertEquals("MEDIUM CORP", results[1].employer)
        assertEquals("SMALL CORP", results[2].employer)
    }
}
