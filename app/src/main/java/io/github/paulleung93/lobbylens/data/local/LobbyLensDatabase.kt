package io.github.paulleung93.lobbylens.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room database for local caching of API responses.
 * Uses cache eviction to prevent unbounded memory growth.
 */
@Database(
    entities = [
        CachedCandidate::class,
        CachedContribution::class,
        CachedSenateContribution::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LobbyLensDatabase : RoomDatabase() {
    abstract fun candidateCacheDao(): CandidateCacheDao
    abstract fun contributionCacheDao(): ContributionCacheDao
    abstract fun senateCacheDao(): SenateCacheDao
    
    companion object {
        const val DATABASE_NAME = "lobbylens_cache.db"
        
        // Cache validity duration: 24 hours
        const val CACHE_VALIDITY_DURATION_MS = 24 * 60 * 60 * 1000L
    }
}
