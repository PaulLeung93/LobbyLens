package io.github.paulleung93.lobbylens.data.network

import io.github.paulleung93.lobbylens.data.model.IndustryResponse
import io.github.paulleung93.lobbylens.data.model.LegislatorResponse
import io.github.paulleung93.lobbylens.data.model.OrganizationResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service interface for the OpenSecrets API.
 * Defines the endpoints for fetching legislator and industry data.
 */
interface OpenSecretsApiService {

    /**
     * Fetches a list of legislators based on a partial name or ID.
     *
     * @param id The search parameter (e.g., a legislator's name).
     * @param apikey Your personal API key for OpenSecrets.
     * @return A [LegislatorResponse] containing a list of matching legislators.
     */
    @GET("getLegislators?output=json")
    suspend fun getLegislators(
        @Query("id") id: String,
        @Query("apikey") apikey: String = "YOUR_API_KEY_HERE" // TODO: Replace with your API key
    ): LegislatorResponse

    /**
     * Fetches the top contributing industries for a specific legislator and election cycle.
     *
     * @param cid The legislator's unique campaign ID.
     * @param cycle The election cycle to query (e.g., "2022").
     * @param apikey Your personal API key for OpenSecrets.
     * @return An [IndustryResponse] containing a list of top industries.
     */
    @GET("candIndustry?output=json")
    suspend fun getTopIndustries(
        @Query("cid") cid: String,
        @Query("cycle") cycle: String,
        @Query("apikey") apikey: String = "YOUR_API_KEY_HERE" // TODO: Replace with your API key
    ): IndustryResponse

    /**
     * Fetches the top contributing organizations for a specific legislator.
     *
     * @param cid The legislator's unique campaign ID.
     * @param cycle The election cycle to query (e.g., "2022").
     * @param apikey Your personal API key for OpenSecrets.
     * @return An [OrganizationResponse] containing a list of top organizations.
     */
    @GET("candContrib?output=json")
    suspend fun getTopOrganizations(
        @Query("cid") cid: String,
        @Query("cycle") cycle: String,
        @Query("apikey") apikey: String = "YOUR_API_KEY_HERE" // TODO: Replace with your API key
    ): OrganizationResponse
}
