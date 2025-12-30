package io.github.paulleung93.lobbylens.data.api

import io.github.paulleung93.lobbylens.BuildConfig
import io.github.paulleung93.lobbylens.data.model.FecCandidateResponse
import io.github.paulleung93.lobbylens.data.model.FecEmployerContributionResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service interface for the official U.S. Federal Election Commission (FEC) API.
 */
interface FecApiService {

    /**
     * Searches for candidates by name.
     *
     * @param apiKey Your personal API key for the FEC API.
     * @param query The name to search for.
     * @return A [FecCandidateResponse] containing a list of matching candidates.
     */
    @GET("candidates/search")
    suspend fun searchCandidates(
        @Query("api_key") apiKey: String = BuildConfig.FEC_API_KEY,
        @Query("q") query: String
    ): Response<FecCandidateResponse>

    /**
     * Fetches campaign contributions for a given candidate, aggregated by employer.
     * This serves as the source for "top organizations" data.
     *
     * @param apiKey Your personal API key for the FEC API.
     * @param candidateId The FEC candidate ID.
     * @param cycle The election cycle.
     * @return A [FecEmployerContributionResponse] containing the aggregated contributions.
     */
    @GET("schedules/schedule_a/by_employer/")
    suspend fun getTopOrganizationsByEmployer(
        @Query("api_key") apiKey: String = BuildConfig.FEC_API_KEY,
        @Query("candidate_id") candidateId: String,
        @Query("cycle") cycle: String,
        @Query("sort_hide_null") sortHideNull: Boolean = false,
        @Query("sort_nulls_last") sortNullsLast: Boolean = false,
        @Query("per_page") perPage: Int = 20
    ): Response<FecEmployerContributionResponse>
}
