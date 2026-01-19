package io.github.paulleung93.lobbylens.data.repository

import android.util.Log
import io.github.paulleung93.lobbylens.data.api.FecApiService
import io.github.paulleung93.lobbylens.data.local.CachedCandidate
import io.github.paulleung93.lobbylens.data.local.CandidateCacheDao
import io.github.paulleung93.lobbylens.data.local.LobbyLensDatabase
import io.github.paulleung93.lobbylens.data.model.FecCandidate
import io.github.paulleung93.lobbylens.data.model.FecCandidateHistoryResponse
import io.github.paulleung93.lobbylens.data.model.FecCandidateResponse
import io.github.paulleung93.lobbylens.data.model.FecCommitteeHistoryResponse
import io.github.paulleung93.lobbylens.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for fetching FEC candidate data.
 * Handles candidate search, details, and committee history.
 */
@Singleton
class FecRepository @Inject constructor(
    private val apiService: FecApiService,
    private val cacheDao: CandidateCacheDao
) {
    companion object {
        private const val TAG = "FecRepository"
    }

    /**
     * Searches for candidates by name using the FEC API.
     * Fetches all pages to ensure the complete list is returned.
     */
    suspend fun searchCandidatesByName(name: String): Result<FecCandidateResponse> = withContext(Dispatchers.IO) {
        Log.d(TAG, "searchCandidatesByName: Searching for candidate: $name")
        
        // Check cache first
        val minCacheTime = System.currentTimeMillis() - LobbyLensDatabase.CACHE_VALIDITY_DURATION_MS
        val cachedResults = cacheDao.searchCandidates(name, minCacheTime)
        if (cachedResults.isNotEmpty()) {
            Log.d(TAG, "searchCandidatesByName: Returning ${cachedResults.size} cached results for $name")
            return@withContext Result.Success(FecCandidateResponse(
                results = cachedResults.map { it.toFecCandidate() }
            ))
        }

        val allCandidates = mutableListOf<FecCandidate>()
        var page = 1
        var hasMorePages = true
        val maxPages = 10

        try {
            while (hasMorePages && page <= maxPages) {
                Log.d(TAG, "searchCandidatesByName: Fetching page $page for query '$name'")
                val response = apiService.searchCandidates(
                    query = name,
                    perPage = 100,
                    page = page
                )

                if (response.isSuccessful && response.body() != null) {
                    val batch = response.body()!!.results
                    if (batch.isEmpty()) {
                        hasMorePages = false
                    } else {
                        allCandidates.addAll(batch)
                        if (batch.size < 100) {
                            hasMorePages = false
                        } else {
                            page++
                        }
                    }
                } else {
                    Log.e(TAG, "searchCandidatesByName: API Error on page $page: ${response.message()}")
                    if (allCandidates.isNotEmpty()) {
                        break
                    } else {
                        return@withContext Result.Error(Exception("API Error: ${response.message()}"))
                    }
                }
            }

            Log.d(TAG, "searchCandidatesByName: Finished fetching. Total found: ${allCandidates.size} candidates")
            
            // Cache results
            if (allCandidates.isNotEmpty()) {
                cacheDao.insertCandidates(allCandidates.map { it.toCachedCandidate() })
            }
            
            Result.Success(FecCandidateResponse(results = allCandidates))
        } catch (e: Exception) {
            Log.e(TAG, "searchCandidatesByName: Exception occurred", e)
            Result.Error(e)
        }
    }

    /**
     * Fetches current members of congress (Incumbents) for a specific cycle.
     */
    suspend fun getCongressMembers(cycle: String = "2024"): Result<FecCandidateResponse> = withContext(Dispatchers.IO) {
        Log.d(TAG, "getCongressMembers: Fetching incumbents for cycle $cycle")
        
        val allCandidates = mutableListOf<FecCandidate>()
        var page = 1
        var hasMorePages = true
        val maxPages = 20

        try {
            while (hasMorePages && page <= maxPages) {
                Log.d(TAG, "getCongressMembers: Fetching page $page")
                val response = apiService.getCandidates(
                    cycle = cycle,
                    incumbentChallenge = "I",
                    sort = "name",
                    perPage = 100,
                    page = page
                )

                if (response.isSuccessful && response.body() != null) {
                    val batch = response.body()!!.results
                    if (batch.isEmpty()) {
                        hasMorePages = false
                    } else {
                        allCandidates.addAll(batch)
                        if (batch.size < 100) {
                            hasMorePages = false
                        } else {
                            page++
                        }
                    }
                } else {
                    Log.e(TAG, "getCongressMembers: API Error on page $page: ${response.message()}")
                    return@withContext Result.Error(Exception("API Error on page $page: ${response.message()}"))
                }
            }

            Log.d(TAG, "getCongressMembers: Finished fetching. Total found: ${allCandidates.size}")
            Result.Success(FecCandidateResponse(results = allCandidates))
        } catch (e: Exception) {
            Log.e(TAG, "getCongressMembers: Exception occurred", e)
            Result.Error(e)
        }
    }

    /**
     * Fetches detailed information for a candidate.
     */
    suspend fun getCandidateDetails(candidateId: String): Result<FecCandidateResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCandidateDetails(candidateId = candidateId)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(Exception("API Error: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Fetches historical data for a candidate.
     */
    suspend fun getCandidateHistory(candidateId: String): Result<FecCandidateHistoryResponse> = withContext(Dispatchers.IO) {
        Log.d(TAG, "getCandidateHistory: Fetching history for $candidateId")
        try {
            val response = apiService.getCandidateHistory(candidateId = candidateId)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Log.e(TAG, "getCandidateHistory: API Error: ${response.message()}")
                Result.Error(Exception("API Error: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getCandidateHistory: Exception occurred", e)
            Result.Error(e)
        }
    }

    /**
     * Fetches committee history for a candidate.
     */
    suspend fun getCandidateCommitteeHistory(candidateId: String): Result<FecCommitteeHistoryResponse> = withContext(Dispatchers.IO) {
        Log.d(TAG, "getCandidateCommitteeHistory: Fetching committee history for $candidateId")
        try {
            val response = apiService.getCandidateCommitteeHistory(candidateId = candidateId)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Log.e(TAG, "getCandidateCommitteeHistory: API Error: ${response.message()}")
                Result.Error(Exception("API Error: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getCandidateCommitteeHistory: Exception occurred", e)
            Result.Error(e)
        }
    }

    // Extension functions for entity conversion
    private fun CachedCandidate.toFecCandidate(): FecCandidate {
        return FecCandidate(
            candidateId = candidateId,
            name = name,
            party = party,
            state = state,
            officeSought = office
        )
    }

    private fun FecCandidate.toCachedCandidate(): CachedCandidate {
        return CachedCandidate(
            candidateId = candidateId,
            name = name,
            party = party,
            state = state,
            office = officeSought,
            electionYears = null
        )
    }
}
