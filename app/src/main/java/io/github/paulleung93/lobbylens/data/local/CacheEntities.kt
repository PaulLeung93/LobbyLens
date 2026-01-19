package io.github.paulleung93.lobbylens.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for caching FEC candidate search results.
 */
@Entity(tableName = "candidate_cache")
data class CachedCandidate(
    @PrimaryKey
    val candidateId: String,
    val name: String,
    val party: String?,
    val state: String?,
    val office: String?,
    val electionYears: String?, // Stored as comma-separated values
    val cachedAt: Long = System.currentTimeMillis()
)

/**
 * Room entity for caching employer contributions.
 */
@Entity(tableName = "contribution_cache", primaryKeys = ["cacheKey", "employer"])
data class CachedContribution(
    val cacheKey: String, // committee_id-cycle format
    val employer: String,
    val total: Double,
    val count: Int,
    val type: String?,
    val mostRecentDate: String?,
    val cachedAt: Long = System.currentTimeMillis()
)

/**
 * Room entity for caching Senate LDA contributions.
 */
@Entity(tableName = "senate_contribution_cache")
data class CachedSenateContribution(
    @PrimaryKey
    val id: String, // Unique identifier from the API
    val normalizedName: String, // The politician name used for lookup
    val registrantName: String?,
    val contributorName: String?,
    val amount: Double?,
    val contributionDate: String?,
    val cachedAt: Long = System.currentTimeMillis()
)
