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

    // TODO: Add endpoints for fetching financial data (e.g., top donors)
}
