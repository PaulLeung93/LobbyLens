package io.github.paulleung93.lobbylens.data.api

import io.github.paulleung93.lobbylens.data.model.SenateContributionResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * Retrofit service interface for the U.S. Senate Lobbying Disclosure API.
 * This API provides data on LD-203 Contribution Reports.
 */
interface SenateLdaApiService {

    /**
     * Searches for contribution reports by honoree name (the politician).
     *
     * @param honoreeName The name of the politician (honoree).
     * @param authorization Optional API key header: "Token <key>".
     * @return A [SenateContributionResponse] containing the matching reports.
     */
    @GET("contributions/")
    suspend fun getContributions(
        @Query("contribution_honoree") honoreeName: String,
        @Header("Authorization") authorization: String? = null,
        @Header("User-Agent") userAgent: String = "LobbyLens/1.0 (https://github.com/paulleung93/lobbylens)"
    ): Response<SenateContributionResponse>
}
