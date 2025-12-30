package io.github.paulleung93.lobbylens.data.repository

import android.graphics.Bitmap
import android.util.Log
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
    
    companion object {
        private const val TAG = "PoliticianRepository"
    }

    // In-memory cache for API responses.
    private val candidatesCache = mutableMapOf<String, Result<FecCandidateResponse>>()
    private val organizationsCache = mutableMapOf<String, Result<FecEmployerContributionResponse>>()

    /**
     * Searches for candidates by name using the FEC API.
     */
    suspend fun searchCandidatesByName(name: String): Result<FecCandidateResponse> {
        Log.d(TAG, "searchCandidatesByName: Searching for candidate: $name")
        candidatesCache[name]?.let {
            Log.d(TAG, "searchCandidatesByName: Returning cached result for $name")
            return it
        }

        return try {
            val response = apiService.searchCandidates(query = name)
            Log.d(TAG, "searchCandidatesByName: Response code: ${response.code()}")
            if (response.isSuccessful && response.body() != null) {
                val result = Result.Success(response.body()!!)
                Log.d(TAG, "searchCandidatesByName: Found ${result.data.results.size} candidates")
                candidatesCache[name] = result
                result
            } else {
                Log.e(TAG, "searchCandidatesByName: API Error: ${response.message()}")
                Result.Error(Exception("API Error: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "searchCandidatesByName: Exception occurred", e)
            Result.Error(e)
        }
    }

    /**
     * Fetches detailed information for a candidate, including principal committees.
     */
    /**
     * Fetches detailed information for a candidate, including principal committees.
     */
    suspend fun getCandidateDetails(candidateId: String): Result<FecCandidateResponse> {
        return try {
            val response = apiService.getCandidateDetails(candidateId = candidateId)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(Exception("API Error: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Fetches historical data, which is better for finding principal committees.
     */
    suspend fun getCandidateHistory(candidateId: String): Result<io.github.paulleung93.lobbylens.data.model.FecCandidateHistoryResponse> {
        Log.d(TAG, "getCandidateHistory: Fetching history for $candidateId")
        return try {
            val response = apiService.getCandidateHistory(candidateId = candidateId)
            Log.d(TAG, "getCandidateHistory: Response code: ${response.code()}")
            if (response.isSuccessful && response.body() != null) {
                val result = Result.Success(response.body()!!)
                Log.d(TAG, "getCandidateHistory: Success, found ${result.data.results.size} history records")
                result
            } else {
                Log.e(TAG, "getCandidateHistory: API Error: ${response.message()}")
                Result.Error(Exception("API Error: ${response.message()}"))
            }
        } catch (e: Exception) {
             Log.e(TAG, "getCandidateHistory: Exception occurred", e)
             Result.Error(e)
        }
    }

    /**
     * Fetches committee history for a candidate, which explicitly links committee assignments to cycles.
     */
    suspend fun getCandidateCommitteeHistory(candidateId: String): Result<io.github.paulleung93.lobbylens.data.model.FecCommitteeHistoryResponse> {
        Log.d(TAG, "getCandidateCommitteeHistory: Fetching committee history for $candidateId")
        return try {
            val response = apiService.getCandidateCommitteeHistory(candidateId = candidateId)
            Log.d(TAG, "getCandidateCommitteeHistory: Response code: ${response.code()}")
            if (response.isSuccessful && response.body() != null) {
                val result = Result.Success(response.body()!!)
                Log.d(TAG, "getCandidateCommitteeHistory: Success, found ${result.data.results.size} committee records")
                result
            } else {
                Log.e(TAG, "getCandidateCommitteeHistory: API Error: ${response.message()}")
                Result.Error(Exception("API Error: ${response.message()}"))
            }
        } catch (e: Exception) {
             Log.e(TAG, "getCandidateCommitteeHistory: Exception occurred", e)
             Result.Error(e)
        }
    }

    /**
     * Fetches top contributing organizations (by employer) for a given committee (principal campaign committee) and cycle.
     */
    suspend fun getTopOrganizations(committeeId: String, cycle: String): Result<FecEmployerContributionResponse> {
        Log.d(TAG, "getTopOrganizations: Fetching for committeeId=$committeeId, cycle=$cycle")
        val cacheKey = "$committeeId-$cycle"
        organizationsCache[cacheKey]?.let {
            Log.d(TAG, "getTopOrganizations: Returning cached result")
            return it
        }

        return try {
            val response = apiService.getTopOrganizationsByEmployer(committeeId = committeeId, cycle = cycle)
            Log.d(TAG, "getTopOrganizations: Response code: ${response.code()}")
            if (response.isSuccessful && response.body() != null) {
                val result = Result.Success(response.body()!!)
                Log.d(TAG, "getTopOrganizations: Found ${result.data.results.size} organizations")
                organizationsCache[cacheKey] = result
                result
            } else {
                Log.e(TAG, "getTopOrganizations: API Error: ${response.message()}")
                Result.Error(Exception("API Error: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getTopOrganizations: Exception occurred", e)
            Result.Error(e)
        }
    }
    /**
     * Identifies a politician from an image using Google Cloud Vision API.
     * Returns the FEC Candidate object if a match is found.
     */
    suspend fun identifyPolitician(imageBitmap: Bitmap): Result<io.github.paulleung93.lobbylens.data.model.FecCandidate> {
        Log.d(TAG, "identifyPolitician: Starting politician identification")
        return try {
            val base64Image = io.github.paulleung93.lobbylens.util.ImageUtils.bitmapToBase64(imageBitmap)
            Log.d(TAG, "identifyPolitician: Image encoded to base64, length: ${base64Image.length}")
            val request = io.github.paulleung93.lobbylens.data.model.CloudVisionRequest(
                requests = listOf(
                    io.github.paulleung93.lobbylens.data.model.AnnotateImageRequest(
                        image = io.github.paulleung93.lobbylens.data.model.ImageContent(base64Image),
                        features = listOf(io.github.paulleung93.lobbylens.data.model.Feature("WEB_DETECTION"))
                    )
                )
            )

            // Make the Cloud Vision API call
            Log.d(TAG, "identifyPolitician: Calling Cloud Vision API")
            val response = RetrofitInstance.cloudVisionApi.annotateImage(io.github.paulleung93.lobbylens.BuildConfig.GOOGLE_API_KEY, request)
            Log.d(TAG, "identifyPolitician: Cloud Vision response code: ${response.code()}")
            
            if (response.isSuccessful && response.body() != null) {
                val annotations = response.body()!!.responses?.firstOrNull()?.webDetection
                // Naive heuristic: Look for the first entity that looks like a person's name or has high score
                // In reality, Cloud Vision returns specific entities. We'll try to find one that matches an FEC candidate.
                val entities = annotations?.webEntities
                Log.d(TAG, "identifyPolitician: Found ${entities?.size ?: 0} web entities")
                if (entities.isNullOrEmpty()) {
                    Log.w(TAG, "identifyPolitician: No entities detected")
                    return Result.Error(Exception("No entities detected."))
                }

                // Iterate through top 3 entites and try to search FEC
                for (entity in entities.take(3)) {
                    if (entity.description != null) {
                        Log.d(TAG, "identifyPolitician: Trying entity: ${entity.description} (score: ${entity.score})")
                        val searchResult = searchCandidatesByName(entity.description)
                        if (searchResult is Result.Success && searchResult.data.results.isNotEmpty()) {
                            // Match found!
                            Log.i(TAG, "identifyPolitician: Match found: ${searchResult.data.results.first().name}")
                            return Result.Success(searchResult.data.results.first())
                        }
                    }
                }
                Log.w(TAG, "identifyPolitician: Could not identify politician in FEC database")
                Result.Error(Exception("Could not identify politician in FEC database."))
            } else {
                Log.e(TAG, "identifyPolitician: Cloud Vision API Error: ${response.message()}, body: ${response.errorBody()?.string()}")
                Result.Error(Exception("Cloud Vision API Error: ${response.message()}"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "identifyPolitician: Exception occurred", e)
            Result.Error(e)
        }
    }

    /**
     * Generates a new image with logos using Vertex AI (Imagen).
     */
    suspend fun generatePoliticianImage(baseBitmap: Bitmap, logos: List<String>): Result<Bitmap> {
        Log.d(TAG, "generatePoliticianImage: Starting image generation with logos: $logos")
        return try {
            val base64Image = io.github.paulleung93.lobbylens.util.ImageUtils.bitmapToBase64(baseBitmap)
            val prompt = "A photo of a politician wearing a suit with ${logos.joinToString(", ")} logo pins on the lapel. Photorealistic, high quality."
            Log.d(TAG, "generatePoliticianImage: Prompt: $prompt")
            
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
            Log.d(TAG, "generatePoliticianImage: Using projectId=$projectId, location=$location, modelId=$modelId")

            Log.d(TAG, "generatePoliticianImage: Calling Vertex AI API")
            val response = RetrofitInstance.vertexAiApi.predict(
                projectId = projectId,
                location = location,
                modelId = modelId,
                apiKey = apiKey,
                request = request
            )
            Log.d(TAG, "generatePoliticianImage: Vertex AI response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val prediction = response.body()!!.predictions?.firstOrNull()
                Log.d(TAG, "generatePoliticianImage: Received ${response.body()!!.predictions?.size ?: 0} predictions")
                if (prediction != null) {
                    val decodedBytes = android.util.Base64.decode(prediction, android.util.Base64.DEFAULT)
                    val generatedBitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    Log.i(TAG, "generatePoliticianImage: Successfully generated image")
                    Result.Success(generatedBitmap)
                } else {
                    Log.e(TAG, "generatePoliticianImage: No image in prediction")
                    Result.Error(Exception("No image generated."))
                }
            } else {
                Log.e(TAG, "generatePoliticianImage: Vertex AI API Error: ${response.message()}, code: ${response.code()}, body: ${response.errorBody()?.string()}")
                 Result.Error(Exception("Vertex AI API Error: ${response.message()} code ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "generatePoliticianImage: Exception occurred", e)
            Result.Error(e)
        }
    }
}
