package io.github.paulleung93.lobbylens.data.repository

import io.github.paulleung93.lobbylens.data.model.LegislatorResponse
import io.github.paulleung93.lobbylens.data.model.OrganizationResponse
import io.github.paulleung93.lobbylens.data.network.RetrofitInstance
import io.github.paulleung93.lobbylens.util.Result

/**
 * Repository for fetching politician data from the OpenSecrets API.
 * This class abstracts the data source and provides a clean API for the ViewModels to use,
 * wrapping all network calls in a Result class to handle success and error states gracefully.
 */
class PoliticianRepository {

    private val apiService = RetrofitInstance.api

    /**
     * Fetches a list of legislators based on a name query.
     * @param name The name of the politician to search for.
     * @return A Result containing either the LegislatorResponse or an Exception.
     */
    suspend fun getLegislators(name: String): Result<LegislatorResponse> {
        return try {
            val response = apiService.getLegislators(id = name)
            Result.Success(response)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Fetches the top contributing organizations for a specific legislator.
     * @param cid The legislator's unique campaign ID.
     * @param cycle The election cycle to query.
     * @return A Result containing either the OrganizationResponse or an Exception.
     */
    suspend fun getTopOrganizations(cid: String, cycle: String): Result<OrganizationResponse> {
        return try {
            val response = apiService.getTopOrganizations(cid = cid, cycle = cycle)
            Result.Success(response)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
