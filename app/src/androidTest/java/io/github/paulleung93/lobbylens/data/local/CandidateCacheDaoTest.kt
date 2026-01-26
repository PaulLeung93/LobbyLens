package io.github.paulleung93.lobbylens.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Integration tests for CandidateCacheDao.
 * Tests database operations using an in-memory Room database.
 */
@RunWith(AndroidJUnit4::class)
class CandidateCacheDaoTest {

    private lateinit var database: LobbyLensDatabase
    private lateinit var dao: CandidateCacheDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LobbyLensDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.candidateCacheDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    // ========== Insert & Retrieve Tests ==========

    @Test
    fun insertAndRetrieveCandidate() = runBlocking {
        // Arrange
        val candidate = CachedCandidate(
            candidateId = "H8CA05035",
            name = "PELOSI, NANCY",
            party = "DEM",
            state = "CA",
            office = "H",
            electionYears = "2020,2022,2024",
            cachedAt = System.currentTimeMillis()
        )

        // Act
        dao.insertCandidates(listOf(candidate))
        
        // Query with a very old min time to ensure we get results
        val result = dao.searchCandidates("PELOSI", 0)

        // Assert
        assertEquals(1, result.size)
        assertEquals("PELOSI, NANCY", result[0].name)
        assertEquals("CA", result[0].state)
    }

    @Test
    fun getCandidateById_returnsCorrectCandidate() = runBlocking {
        // Arrange
        val candidate1 = CachedCandidate(
            candidateId = "H8CA05035",
            name = "PELOSI, NANCY",
            party = "DEM",
            state = "CA",
            office = "H",
            electionYears = "2024"
        )
        val candidate2 = CachedCandidate(
            candidateId = "S2CA00285",
            name = "FEINSTEIN, DIANNE",
            party = "DEM",
            state = "CA",
            office = "S",
            electionYears = "2024"
        )
        dao.insertCandidates(listOf(candidate1, candidate2))

        // Act
        val result = dao.getCandidateById("S2CA00285", 0)

        // Assert
        assertNotNull(result)
        assertEquals("FEINSTEIN, DIANNE", result?.name)
    }

    // ========== TTL Expiry Tests ==========

    @Test
    fun ttlExpiry_oldRecordNotReturned() = runBlocking {
        // Arrange - Insert an "old" record with timestamp from yesterday
        val oldTimestamp = System.currentTimeMillis() - (25 * 60 * 60 * 1000) // 25 hours ago
        val oldCandidate = CachedCandidate(
            candidateId = "OLD001",
            name = "OLD CANDIDATE",
            party = "REP",
            state = "TX",
            office = "H",
            electionYears = "2020",
            cachedAt = oldTimestamp
        )
        dao.insertCandidates(listOf(oldCandidate))

        // Act - Query with a strict time limit (24 hours)
        val minCacheTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // 24 hours ago
        val result = dao.searchCandidates("OLD CANDIDATE", minCacheTime)

        // Assert - Old record should NOT be returned
        assertTrue("Old record should not be returned due to TTL", result.isEmpty())
    }

    @Test
    fun ttlExpiry_newRecordReturned() = runBlocking {
        // Arrange - Insert a fresh record
        val freshCandidate = CachedCandidate(
            candidateId = "FRESH001",
            name = "FRESH CANDIDATE",
            party = "DEM",
            state = "NY",
            office = "S",
            electionYears = "2024",
            cachedAt = System.currentTimeMillis() // Just now
        )
        dao.insertCandidates(listOf(freshCandidate))

        // Act - Query with 24 hour limit
        val minCacheTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        val result = dao.searchCandidates("FRESH", minCacheTime)

        // Assert
        assertEquals(1, result.size)
        assertEquals("FRESH CANDIDATE", result[0].name)
    }

    // ========== Search Filtering Tests ==========

    @Test
    fun searchFiltering_onlyMatchingNameReturned() = runBlocking {
        // Arrange
        val biden = CachedCandidate(
            candidateId = "P80001571",
            name = "BIDEN, JOSEPH R",
            party = "DEM",
            state = "DE",
            office = "P",
            electionYears = "2020,2024"
        )
        val trump = CachedCandidate(
            candidateId = "P80001591",
            name = "TRUMP, DONALD J",
            party = "REP",
            state = "FL",
            office = "P",
            electionYears = "2016,2020,2024"
        )
        dao.insertCandidates(listOf(biden, trump))

        // Act - Search for "BI" (should match BIDEN only)
        val result = dao.searchCandidates("BI", 0)

        // Assert
        assertEquals(1, result.size)
        assertEquals("BIDEN, JOSEPH R", result[0].name)
    }

    // ========== Clear Cache Tests ==========

    @Test
    fun clearOldCache_removesExpiredEntries() = runBlocking {
        // Arrange
        val oldCandidate = CachedCandidate(
            candidateId = "OLD001",
            name = "OLD CANDIDATE",
            party = "REP",
            state = "TX",
            office = "H",
            electionYears = "2020",
            cachedAt = System.currentTimeMillis() - (48 * 60 * 60 * 1000) // 48 hours ago
        )
        val freshCandidate = CachedCandidate(
            candidateId = "FRESH001",
            name = "FRESH CANDIDATE",
            party = "DEM",
            state = "NY",
            office = "S",
            electionYears = "2024",
            cachedAt = System.currentTimeMillis()
        )
        dao.insertCandidates(listOf(oldCandidate, freshCandidate))

        // Act - Clear entries older than 24 hours
        val cutoff = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        dao.clearOldCache(cutoff)

        // Assert
        val allResults = dao.searchCandidates("CANDIDATE", 0)
        assertEquals(1, allResults.size)
        assertEquals("FRESH CANDIDATE", allResults[0].name)
    }

    @Test
    fun getCacheSize_returnsCorrectCount() = runBlocking {
        // Arrange
        val candidates = listOf(
            CachedCandidate("ID1", "NAME1", "DEM", "CA", "H", null),
            CachedCandidate("ID2", "NAME2", "REP", "TX", "S", null),
            CachedCandidate("ID3", "NAME3", "DEM", "NY", "P", null)
        )
        dao.insertCandidates(candidates)

        // Act
        val size = dao.getCacheSize()

        // Assert
        assertEquals(3, size)
    }
}
