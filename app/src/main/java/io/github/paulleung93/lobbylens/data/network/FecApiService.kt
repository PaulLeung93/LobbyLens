package io.github.paulleung93.lobbylens.data.network

import io.github.paulleung93.lobbylens.data.model.FecCandidateResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service interface for the official FEC (Federal Election Commission) API.
 * Defines the endpoints for fetching candidate and campaign finance data.
 */
interface FecApiService {

    /**
     * Searches for candidates by name.
     *
     * @param query The name to search for.
     * @return A [FecCandidateResponse] containing a list of matching candidates.
     */
    @GET("candidates/search")
    suspend fun searchCandidatesByName(
        @Query("q") query: String
    ): FecCandidateResponse

    @GET("candidate/{candidate_id}")
    suspend fun getCandidateDetails(
        @retrofit2.http.Path("candidate_id") candidateId: String
    ): retrofit2.Response<io.github.paulleung93.lobbylens.data.model.FecCandidate>

    // Fetch top contributions by employer
    @GET("schedules/schedule_a/by_employer/")
    suspend fun getTopOrganizationsByEmployer(
        @Query("committee_id") committeeId: String,
        @Query("cycle") cycle: String,
        @Query("sort_hide_null") sortHideNull: Boolean = false,
        @Query("sort") sort: String = "-total",
        @Query("per_page") perPage: Int = 10
    ): retrofit2.Response<io.github.paulleung93.lobbylens.data.model.FecEmployerContributionResponse>
}
