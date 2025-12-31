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

    @GET("contributions/")
    suspend fun getContributions(
        @Query("contribution_honoree") honoreeName: String
    ): Response<SenateContributionResponse>
}
