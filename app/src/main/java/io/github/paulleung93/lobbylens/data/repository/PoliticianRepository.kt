package io.github.paulleung93.lobbylens.data.repository

import io.github.paulleung93.lobbylens.data.model.LegislatorResponse
import io.github.paulleung93.lobbylens.data.network.RetrofitInstance
import io.github.paulleung93.lobbylens.data.model.IndustryResponse

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
        return apiService.getLegislators(name = name)
    }

    /**
     * Fetches the top contributing industries for a specific legislator.
     * @param cid The legislator's unique campaign ID.
     * @return An IndustryResponse containing the list of top industries.
     */
    suspend fun getTopIndustries(cid: String): IndustryResponse {
        return apiService.getTopIndustries(cid = cid)
    }
}