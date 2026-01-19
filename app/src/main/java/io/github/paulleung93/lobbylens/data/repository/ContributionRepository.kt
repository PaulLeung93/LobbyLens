package io.github.paulleung93.lobbylens.data.repository

import android.util.Log
import io.github.paulleung93.lobbylens.data.api.FecApiService
import io.github.paulleung93.lobbylens.data.local.CachedContribution
import io.github.paulleung93.lobbylens.data.local.ContributionCacheDao
import io.github.paulleung93.lobbylens.data.local.LobbyLensDatabase
import io.github.paulleung93.lobbylens.data.model.FecEmployerContribution
import io.github.paulleung93.lobbylens.data.model.FecEmployerContributionResponse
import io.github.paulleung93.lobbylens.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for fetching campaign contribution data.
 * Handles employer contributions and PAC data.
 */
@Singleton
class ContributionRepository @Inject constructor(
    private val apiService: FecApiService,
    private val cacheDao: ContributionCacheDao
) {
    companion object {
        private const val TAG = "ContributionRepository"
    }

    /**
     * Fetches top contributing organizations (by employer) AND top committee/PAC contributors
     * for a given committee and cycle. Merges the results into a single list.
     */
    suspend fun getTopOrganizations(committeeId: String, cycle: String): Result<FecEmployerContributionResponse> = withContext(Dispatchers.IO) {
        Log.d(TAG, "getTopOrganizations: Fetching for committeeId=$committeeId, cycle=$cycle")
        val cacheKey = "$committeeId-$cycle"
        
        // Check cache first
        val minCacheTime = System.currentTimeMillis() - LobbyLensDatabase.CACHE_VALIDITY_DURATION_MS
        val cachedResults = cacheDao.getContributions(cacheKey, minCacheTime)
        if (cachedResults.isNotEmpty()) {
            Log.d(TAG, "getTopOrganizations: Returning ${cachedResults.size} cached results")
            return@withContext Result.Success(FecEmployerContributionResponse(
                results = cachedResults.map { it.toFecEmployerContribution() }
            ))
        }

        try {
            coroutineScope {
                val employerDeferred = async {
                    apiService.getTopOrganizationsByEmployer(committeeId = committeeId, cycle = cycle)
                }
                val contributorDeferred = async {
                    apiService.getPacContributions(committeeId = committeeId, cycle = cycle)
                }

                val employerResponse = employerDeferred.await()
                val contributorResponse = contributorDeferred.await()

                val employers = if (employerResponse.isSuccessful && employerResponse.body() != null) {
                    employerResponse.body()!!.results.map {
                        it.apply { type = "Employer" }
                    }
                } else {
                    Log.e(TAG, "getTopOrganizations: Employer API Error: ${employerResponse.message()}")
                    emptyList()
                }

                val contributors = if (contributorResponse.isSuccessful && contributorResponse.body() != null) {
                    val rawList = contributorResponse.body()!!.results
                    // Aggregate by contributor name
                    rawList.filter { it.contributorName != null }
                        .groupBy { it.contributorName ?: "Unknown PAC" }
                        .map { (name, items) ->
                            val latestDate = items.mapNotNull { it.contributionReceiptDate }.maxOrNull()
                            FecEmployerContribution(
                                employer = name,
                                total = items.sumOf { it.amount },
                                count = items.size,
                                type = "PAC",
                                mostRecentDate = latestDate
                            )
                        }
                } else {
                    Log.e(TAG, "getTopOrganizations: Contributor API Error: ${contributorResponse.message()}")
                    emptyList()
                }

                val mergedList = (employers + contributors).sortedByDescending { it.total }

                if (mergedList.isNotEmpty()) {
                    // Cache results
                    cacheDao.insertContributions(mergedList.map { it.toCachedContribution(cacheKey) })
                    Result.Success(FecEmployerContributionResponse(mergedList))
                } else {
                    Result.Error(Exception("Failed to fetch contribution data"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getTopOrganizations: Exception occurred", e)
            Result.Error(e)
        }
    }

    // Extension functions for entity conversion
    private fun CachedContribution.toFecEmployerContribution(): FecEmployerContribution {
        return FecEmployerContribution(
            employer = employer,
            total = total,
            count = count,
            type = type ?: "Employer",
            mostRecentDate = mostRecentDate
        )
    }

    private fun FecEmployerContribution.toCachedContribution(cacheKey: String): CachedContribution {
        return CachedContribution(
            cacheKey = cacheKey,
            employer = employer,
            total = total,
            count = count,
            type = type,
            mostRecentDate = mostRecentDate
        )
    }
}
