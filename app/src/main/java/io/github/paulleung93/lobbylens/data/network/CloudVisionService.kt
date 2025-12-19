package io.github.paulleung93.lobbylens.data.network

import io.github.paulleung93.lobbylens.data.model.CloudVisionRequest
import io.github.paulleung93.lobbylens.data.model.CloudVisionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface CloudVisionService {
    @POST("v1/images:annotate")
    suspend fun annotateImage(
        @Query("key") apiKey: String,
        @Body request: CloudVisionRequest
    ): Response<CloudVisionResponse>
}
