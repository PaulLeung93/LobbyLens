package io.github.paulleung93.lobbylens.data.repository

import io.github.paulleung93.lobbylens.data.api.FecApiService
import io.github.paulleung93.lobbylens.data.api.RetrofitInstance
import io.github.paulleung93.lobbylens.data.model.FecCandidateResponse
import io.github.paulleung93.lobbylens.data.model.FecEmployerContributionResponse
import io.github.paulleung93.lobbylens.util.Result

/**
 * Repository for fetching politician data from the official FEC (Federal Election Commission) API.
 * This class abstracts the data source and provides a clean API for the ViewModels to use,
 * wrapping all network calls in a Result class to handle success and error states gracefully.
 * It also includes an in-memory caching layer to improve performance and reduce API usage.
 */
class PoliticianRepository {

    private val apiService: FecApiService by lazy { RetrofitInstance.api }

    // In-memory cache for API responses.
    private val candidatesCache = mutableMapOf<String, Result<FecCandidateResponse>>()
    private val organizationsCache = mutableMapOf<String, Result<FecEmployerContributionResponse>>()

    /**
     * Searches for candidates by name using the FEC API.
     */
    suspend fun searchCandidatesByName(name: String): Result<FecCandidateResponse> {
        candidatesCache[name]?.let { return it }

        return try {
            val response = apiService.searchCandidates(query = name)
            if (response.isSuccessful && response.body() != null) {
                val result = Result.Success(response.body()!!)
                candidatesCache[name] = result
                result
            } else {
                Result.Error(Exception("API Error: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Fetches top contributing organizations (by employer) for a given candidate and cycle.
     */
    suspend fun getTopOrganizations(cid: String, cycle: String): Result<FecEmployerContributionResponse> {
        val cacheKey = "$cid-$cycle"
        organizationsCache[cacheKey]?.let { return it }

        return try {
            val response = apiService.getTopOrganizationsByEmployer(candidateId = cid, cycle = cycle)
            if (response.isSuccessful && response.body() != null) {
                val result = Result.Success(response.body()!!)
                organizationsCache[cacheKey] = result
                result
            } else {
                Result.Error(Exception("API Error: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    /**
     * Identifies a politician from an image using Google Cloud Vision API.
     * Returns the FEC Candidate object if a match is found.
     */
    suspend fun identifyPolitician(imageBitmap: Bitmap): Result<io.github.paulleung93.lobbylens.data.model.FecCandidate> {
        return try {
            val base64Image = io.github.paulleung93.lobbylens.util.ImageUtils.bitmapToBase64(imageBitmap)
            val request = io.github.paulleung93.lobbylens.data.model.CloudVisionRequest(
                requests = listOf(
                    io.github.paulleung93.lobbylens.data.model.AnnotateImageRequest(
                        image = io.github.paulleung93.lobbylens.data.model.ImageContent(base64Image),
                        features = listOf(io.github.paulleung93.lobbylens.data.model.Feature("WEB_DETECTION"))
                    )
                )
            )

            // Make the Cloud Vision API call
            val response = RetrofitInstance.cloudVisionApi.annotateImage(io.github.paulleung93.lobbylens.BuildConfig.GOOGLE_API_KEY, request)
            
            if (response.isSuccessful && response.body() != null) {
                val annotations = response.body()!!.responses?.firstOrNull()?.webDetection
                // Naive heuristic: Look for the first entity that looks like a person's name or has high score
                // In reality, Cloud Vision returns specific entities. We'll try to find one that matches an FEC candidate.
                val entities = annotations?.webEntities
                if (entities.isNullOrEmpty()) {
                    return Result.Error(Exception("No entities detected."))
                }

                // Iterate through top 3 entites and try to search FEC
                for (entity in entities.take(3)) {
                    if (entity.description != null) {
                        val searchResult = searchCandidatesByName(entity.description)
                        if (searchResult is Result.Success && searchResult.data.results.isNotEmpty()) {
                            // Match found!
                            return Result.Success(searchResult.data.results.first())
                        }
                    }
                }
                Result.Error(Exception("Could not identify politician in FEC database."))
            } else {
                Result.Error(Exception("Cloud Vision API Error: ${response.message()}"))
            }

        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Generates a new image with logos using Vertex AI (Imagen).
     */
    suspend fun generatePoliticianImage(baseBitmap: Bitmap, logos: List<String>): Result<Bitmap> {
        return try {
            val base64Image = io.github.paulleung93.lobbylens.util.ImageUtils.bitmapToBase64(baseBitmap)
            val prompt = "A photo of a politician wearing a suit with ${logos.joinToString(", ")} logo pins on the lapel. Photorealistic, high quality."
            
            val request = io.github.paulleung93.lobbylens.data.model.VertexAiPredictionRequest(
                instances = listOf(
                    io.github.paulleung93.lobbylens.data.model.VertexAiInstance(
                        prompt = prompt,
                        image = io.github.paulleung93.lobbylens.data.model.VertexAiImage(base64Image)
                    )
                ),
                parameters = io.github.paulleung93.lobbylens.data.model.VertexAiParameters(sampleCount = 1)
            )

            val projectId = io.github.paulleung93.lobbylens.BuildConfig.GOOGLE_CLOUD_PROJECT_ID
            val location = io.github.paulleung93.lobbylens.BuildConfig.GOOGLE_CLOUD_LOCATION
            val apiKey = io.github.paulleung93.lobbylens.BuildConfig.GOOGLE_API_KEY
            // Using "image-generation-001" or "imagen-3.0-generate-001". Using a stable model ID.
            val modelId = "imagegeneration@005" // Example model ID, user might need to adjust.

            val response = RetrofitInstance.vertexAiApi.predict(
                projectId = projectId,
                location = location,
                modelId = modelId,
                apiKey = apiKey,
                request = request
            )

            if (response.isSuccessful && response.body() != null) {
                val prediction = response.body()!!.predictions?.firstOrNull()
                if (prediction != null) {
                    val decodedBytes = android.util.Base64.decode(prediction, android.util.Base64.DEFAULT)
                    val generatedBitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    Result.Success(generatedBitmap)
                } else {
                    Result.Error(Exception("No image generated."))
                }
            } else {
                 Result.Error(Exception("Vertex AI API Error: ${response.message()} code ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
