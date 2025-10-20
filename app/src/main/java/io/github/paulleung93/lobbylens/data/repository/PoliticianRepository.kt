package io.github.paulleung93.lobbylens.data.repository

import io.github.paulleung93.lobbylens.data.model.LegislatorResponse
import io.github.paulleung93.lobbylens.data.model.OrganizationResponse
import io.github.paulleung93.lobbylens.data.network.RetrofitInstance
import io.github.paulleung93.lobbylens.util.Result

/**
 * Repository for fetching politician data from the OpenSecrets API.
 * This class abstracts the data source and provides a clean API for the ViewModels to use,
 * wrapping all network calls in a Result class to handle success and error states gracefully.
 * It also includes an in-memory caching layer to improve performance and reduce API usage.
 */
class PoliticianRepository {

    private val apiService = RetrofitInstance.api

    // In-memory cache for API responses
    private val legislatorCache = mutableMapOf<String, Result<LegislatorResponse>>()
    private val organizationCache = mutableMapOf<Pair<String, String>, Result<OrganizationResponse>>()

    /**
     * Fetches a list of legislators based on a name query, using a cache.
     * @param name The name of the politician to search for.
     * @return A Result containing either the LegislatorResponse or an Exception.
     */
    suspend fun getLegislators(name: String): Result<LegislatorResponse> {
        // Check cache first
        legislatorCache[name]?.let { return it }

        // If not in cache, make network call
        return try {
            val response = apiService.getLegislators(id = name)
            val result = Result.Success(response)
            legislatorCache[name] = result // Store in cache
            result
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Fetches the top contributing organizations for a specific legislator, using a cache.
     * @param cid The legislator's unique campaign ID.
     * @param cycle The election cycle to query.
     * @return A Result containing either the OrganizationResponse or an Exception.
     */
    suspend fun getTopOrganizations(cid: String, cycle: String): Result<OrganizationResponse> {
        val cacheKey = cid to cycle
        // Check cache first
        organizationCache[cacheKey]?.let { return it }

        // If not in cache, make network call
        return try {
            val response = apiService.getTopOrganizations(cid = cid, cycle = cycle)
            val result = Result.Success(response)
            organizationCache[cacheKey] = result // Store in cache
            result
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
