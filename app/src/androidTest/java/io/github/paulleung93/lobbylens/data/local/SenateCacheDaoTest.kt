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
 * Integration tests for SenateCacheDao.
 * Tests checking for normalized name lookups and TTL expiry.
 */
@RunWith(AndroidJUnit4::class)
class SenateCacheDaoTest {

    private lateinit var database: LobbyLensDatabase
    private lateinit var dao: SenateCacheDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LobbyLensDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.senateCacheDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    // ========== Insert & Retrieve Tests ==========

    @Test
    fun insertAndRetrieveSenateContribution() = runBlocking {
        // Arrange
        val contribution = CachedSenateContribution(
            id = "UUID-123",
            normalizedName = "NANCY PELOSI",
            registrantName = "LOBBYING FIRM INC",
            contributorName = "JOHN DOE",
            amount = 5000.0,
            contributionDate = "2024-01-01"
        )

        // Act
        dao.insertContributions(listOf(contribution))
        val results = dao.getContributions("NANCY PELOSI", 0)

        // Assert
        assertEquals(1, results.size)
        assertEquals("NANCY PELOSI", results[0].normalizedName)
        assertEquals("LOBBYING FIRM INC", results[0].registrantName)
    }

    // ========== TTL Expiry Tests ==========

    @Test
    fun ttlExpiry_oldDataNotReturned() = runBlocking {
        // Arrange
        val oldContribution = CachedSenateContribution(
            id = "OLD-UUID",
            normalizedName = "NANCY PELOSI",
            registrantName = "OLD FIRM",
            contributorName = null,
            amount = 1000.0,
            contributionDate = "2020-01-01",
            cachedAt = System.currentTimeMillis() - (48 * 60 * 60 * 1000) // 48 hours ago
        )
        dao.insertContributions(listOf(oldContribution))

        // Act - Query with 24 hour limit
        val minCacheTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        val results = dao.getContributions("NANCY PELOSI", minCacheTime)

        // Assert
        assertTrue("Expired data should not be returned", results.isEmpty())
    }

    @Test
    fun clearOldCache_removesExpiredEntries() = runBlocking {
        // Arrange
        val oldContribution = CachedSenateContribution(
            id = "OLD-UUID",
            normalizedName = "NANCY PELOSI",
            registrantName = "OLD FIRM",
            contributorName = null,
            amount = 1000.0,
            contributionDate = "2020-01-01",
            cachedAt = System.currentTimeMillis() - (48 * 60 * 60 * 1000)
        )
        val newContribution = CachedSenateContribution(
            id = "NEW-UUID",
            normalizedName = "NANCY PELOSI",
            registrantName = "NEW FIRM",
            contributorName = null,
            amount = 1000.0,
            contributionDate = "2024-01-01",
            cachedAt = System.currentTimeMillis()
        )
        dao.insertContributions(listOf(oldContribution, newContribution))

        // Act
        val cutoff = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        dao.clearOldCache(cutoff)

        // Assert
        val allResults = dao.getContributions("NANCY PELOSI", 0) // Query all times
        assertEquals(1, allResults.size)
        assertEquals("NEW-UUID", allResults[0].id)
    }
}
