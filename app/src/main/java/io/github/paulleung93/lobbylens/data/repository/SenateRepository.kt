package io.github.paulleung93.lobbylens.data.repository

import android.util.Log
import io.github.paulleung93.lobbylens.data.api.SenateLdaApiService
import io.github.paulleung93.lobbylens.data.local.CachedSenateContribution
import io.github.paulleung93.lobbylens.data.local.LobbyLensDatabase
import io.github.paulleung93.lobbylens.data.local.SenateCacheDao
import io.github.paulleung93.lobbylens.data.model.SenateContributionResponse
import io.github.paulleung93.lobbylens.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for fetching Senate LDA (Lobbying Disclosure Act) data.
 * Handles lobbyist contributions from LD-203 reports.
 */
@Singleton
class SenateRepository @Inject constructor(
    private val apiService: SenateLdaApiService,
    private val cacheDao: SenateCacheDao
) {
    companion object {
        private const val TAG = "SenateRepository"
    }

    /**
     * Fetches lobbyist contributions (LD-203 reports) from the U.S. Senate Lobbying Disclosure API.
     * Searches by honoree name (the politician).
     */
    suspend fun getContributions(politicianName: String): Result<SenateContributionResponse> = withContext(Dispatchers.IO) {
        val normalizedName = normalizeNameForSenate(politicianName)
        Log.d(TAG, "getContributions: Fetching for normalizedName=$normalizedName (original=$politicianName)")

        // Check cache first
        val minCacheTime = System.currentTimeMillis() - LobbyLensDatabase.CACHE_VALIDITY_DURATION_MS
        val cachedResults = cacheDao.getContributions(normalizedName, minCacheTime)
        if (cachedResults.isNotEmpty()) {
            Log.d(TAG, "getContributions: Returning ${cachedResults.size} cached results")
            // Note: For simplicity, we return cached data but ideally would reconstruct full response
        }

        try {
            val pageSize = 100
            val response = apiService.getContributions(
                honoreeName = normalizedName,
                page = 1,
                pageSize = pageSize
            )
            Log.d(TAG, "getContributions: Response code: ${response.code()}")
            
            if (response.isSuccessful && response.body() != null) {
                val result = response.body()!!
                Log.d(TAG, "getContributions: Success - fetched ${result.results.size} of ${result.count} total contributions")
                
                // Cache results - using report-level data since contributions are nested
                val cached = result.results.mapIndexed { index, report ->
                    CachedSenateContribution(
                        id = "${normalizedName}_${report.filingUuid}",
                        normalizedName = normalizedName,
                        registrantName = report.registrant.name,
                        contributorName = null, // Contributions are nested in contributionItems
                        amount = null,
                        contributionDate = report.filingPeriod
                    )
                }
                if (cached.isNotEmpty()) {
                    cacheDao.insertContributions(cached)
                }
                
                Result.Success(result)
            } else {
                Log.e(TAG, "getContributions: API Error: ${response.code()} ${response.message()}")
                Result.Error(Exception("Senate API Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getContributions: Exception occurred", e)
            Result.Error(e)
        }
    }

    /**
     * Normalizes a name from FEC format (LAST, FIRST MIDDLE) to "FIRST LAST" format
     * for Senate honoree search.
     */
    private fun normalizeNameForSenate(name: String): String {
        return try {
            val parts = name.split(",").map { it.trim() }
            if (parts.size >= 2) {
                val last = parts[0]
                val first = parts[1].split(Regex("\\s+"))[0]
                "$first $last".trim()
            } else {
                name.trim()
            }
        } catch (e: Exception) {
            name.trim()
        }
    }
}
