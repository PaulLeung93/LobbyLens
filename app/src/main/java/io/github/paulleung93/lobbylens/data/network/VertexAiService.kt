package io.github.paulleung93.lobbylens.data.network

import io.github.paulleung93.lobbylens.data.model.VertexAiPredictionRequest
import io.github.paulleung93.lobbylens.data.model.VertexAiPredictionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface VertexAiService {
    @POST("v1/projects/{projectId}/locations/{location}/publishers/google/models/{modelId}:predict")
    suspend fun predict(
        @Path("projectId") projectId: String,
        @Path("location") location: String,
        @Path("modelId") modelId: String,
        @Query("key") apiKey: String,
        @Body request: VertexAiPredictionRequest
    ): Response<VertexAiPredictionResponse>
}
