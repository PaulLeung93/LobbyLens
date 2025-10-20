package io.github.paulleung93.lobbylens.data.repository

import io.github.paulleung93.lobbylens.data.model.IndustryResponse
import io.github.paulleung93.lobbylens.data.model.LegislatorResponse
import io.github.paulleung93.lobbylens.data.model.OrganizationResponse
import io.github.paulleung93.lobbylens.data.network.RetrofitInstance

/**
 * Repository for fetching politician data from the OpenSecrets API.
 * This class abstracts the data source and provides a clean API for the ViewModels to use.
 */
class PoliticianRepository {

    private val apiService = RetrofitInstance.api

    /**
     * Fetches a list of legislators based on a name query.
     * @param name The name of the politician to search for.
     * @return A LegislatorResponse containing the list of matching legislators.
     */
    suspend fun getLegislators(name: String): LegislatorResponse {
        return apiService.getLegislators(id = name)
    }

    /**
     * Fetches the top contributing industries for a specific legislator.
     * @param cid The legislator's unique campaign ID.
     * @param cycle The election cycle to query.
     * @return An IndustryResponse containing the list of top industries.
     */
    suspend fun getTopIndustries(cid: String, cycle: String): IndustryResponse {
        return apiService.getTopIndustries(cid = cid, cycle = cycle)
    }

    /**
     * Fetches the top contributing organizations for a specific legislator.
     * @param cid The legislator's unique campaign ID.
     * @param cycle The election cycle to query.
     * @return An OrganizationResponse containing the list of top organizations.
     */
    suspend fun getTopOrganizations(cid: String, cycle: String): OrganizationResponse {
        return apiService.getTopOrganizations(cid = cid, cycle = cycle)
    }
}
