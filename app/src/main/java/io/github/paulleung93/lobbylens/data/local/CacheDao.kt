package io.github.paulleung93.lobbylens.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Data Access Object for cached candidates.
 */
@Dao
interface CandidateCacheDao {
    @Query("SELECT * FROM candidate_cache WHERE name LIKE '%' || :query || '%' AND cachedAt > :minCacheTime")
    suspend fun searchCandidates(query: String, minCacheTime: Long): List<CachedCandidate>
    
    @Query("SELECT * FROM candidate_cache WHERE candidateId = :id AND cachedAt > :minCacheTime")
    suspend fun getCandidateById(id: String, minCacheTime: Long): CachedCandidate?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCandidates(candidates: List<CachedCandidate>)
    
    @Query("DELETE FROM candidate_cache WHERE cachedAt < :maxAge")
    suspend fun clearOldCache(maxAge: Long)
    
    @Query("DELETE FROM candidate_cache")
    suspend fun clearAll()
    
    @Query("SELECT COUNT(*) FROM candidate_cache")
    suspend fun getCacheSize(): Int
}

/**
 * Data Access Object for cached contributions.
 */
@Dao
interface ContributionCacheDao {
    @Query("SELECT * FROM contribution_cache WHERE cacheKey = :cacheKey AND cachedAt > :minCacheTime ORDER BY total DESC")
    suspend fun getContributions(cacheKey: String, minCacheTime: Long): List<CachedContribution>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContributions(contributions: List<CachedContribution>)
    
    @Query("DELETE FROM contribution_cache WHERE cachedAt < :maxAge")
    suspend fun clearOldCache(maxAge: Long)
    
    @Query("DELETE FROM contribution_cache")
    suspend fun clearAll()
}

/**
 * Data Access Object for cached Senate contributions.
 */
@Dao
interface SenateCacheDao {
    @Query("SELECT * FROM senate_contribution_cache WHERE normalizedName = :name AND cachedAt > :minCacheTime")
    suspend fun getContributions(name: String, minCacheTime: Long): List<CachedSenateContribution>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContributions(contributions: List<CachedSenateContribution>)
    
    @Query("DELETE FROM senate_contribution_cache WHERE cachedAt < :maxAge")
    suspend fun clearOldCache(maxAge: Long)
    
    @Query("DELETE FROM senate_contribution_cache")
    suspend fun clearAll()
}
