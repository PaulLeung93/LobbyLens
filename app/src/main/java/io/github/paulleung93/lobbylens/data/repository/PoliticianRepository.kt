package io.github.paulleung93.lobbylens.data.repository

import io.github.paulleung93.lobbylens.data.api.FecApiService
import io.github.paulleung93.lobbylens.data.api.RetrofitInstance
import io.github.paulleung93.lobbylens.data.model.FecCandidateResponse
import io.github.paulleung93.lobbylens.data.model.FecEmployerContributionResponse
import io.github.paulleung93.lobbylens.util.Result

/**
 * Repository for fetching politician data from the official FEC (Federal Election Commission) API.
 * This class abstracts the data source and provides a clean API for the ViewModels to use,
 * wrapping all network calls in a Result class to handle success and error states gracefully.
 * It also includes an in-memory caching layer to improve performance and reduce API usage.
 */
class PoliticianRepository {

    private val apiService: FecApiService by lazy { RetrofitInstance.api }

    // In-memory cache for API responses.
    private val candidatesCache = mutableMapOf<String, Result<FecCandidateResponse>>()
    private val organizationsCache = mutableMapOf<String, Result<FecEmployerContributionResponse>>()

    /**
     * Searches for candidates by name using the FEC API.
     */
    suspend fun searchCandidatesByName(name: String): Result<FecCandidateResponse> {
        candidatesCache[name]?.let { return it }

        return try {
            val response = apiService.searchCandidates(query = name)
            if (response.isSuccessful && response.body() != null) {
                val result = Result.Success(response.body()!!)
                candidatesCache[name] = result
                result
            } else {
                Result.Error(Exception("API Error: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Fetches top contributing organizations (by employer) for a given candidate and cycle.
     */
    suspend fun getTopOrganizations(cid: String, cycle: String): Result<FecEmployerContributionResponse> {
        val cacheKey = "$cid-$cycle"
        organizationsCache[cacheKey]?.let { return it }

        return try {
            val response = apiService.getTopOrganizationsByEmployer(candidateId = cid, cycle = cycle)
            if (response.isSuccessful && response.body() != null) {
                val result = Result.Success(response.body()!!)
                organizationsCache[cacheKey] = result
                result
            } else {
                Result.Error(Exception("API Error: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
