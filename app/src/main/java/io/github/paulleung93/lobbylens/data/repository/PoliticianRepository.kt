package io.github.paulleung93.lobbylens.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.ai.ai
import io.github.paulleung93.lobbylens.data.api.FecApiService
import io.github.paulleung93.lobbylens.data.api.RetrofitInstance
import io.github.paulleung93.lobbylens.data.model.FecCandidateResponse
import io.github.paulleung93.lobbylens.data.model.FecEmployerContributionResponse
import io.github.paulleung93.lobbylens.util.Result
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

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
     * Fetches current members of congress (Incumbents) for a specific cycle.
     * Fetches all pages to ensure the list is complete.
     */
    suspend fun getCongressMembers(cycle: String = "2024"): Result<FecCandidateResponse> {
        Log.d(TAG, "getCongressMembers: Fetching incumbents for cycle $cycle")
        
        val allCandidates = mutableListOf<io.github.paulleung93.lobbylens.data.model.FecCandidate>()
        var page = 1
        var hasMorePages = true
        // Safety limit to prevent infinite loops if API behaves unexpectedly
        val maxPages = 20 

        return try {
            while (hasMorePages && page <= maxPages) {
                Log.d(TAG, "getCongressMembers: Fetching page $page")
                val response = apiService.getCandidates(
                    cycle = cycle, 
                    incumbentChallenge = "I",
                    sort = "name",
                    perPage = 100, // Maximize per page
                    page = page
                )

                if (response.isSuccessful && response.body() != null) {
                    val batch = response.body()!!.results
                    if (batch.isEmpty()) {
                        hasMorePages = false
                    } else {
                        allCandidates.addAll(batch)
                        // If we got fewer than requested, we are at the end
                        if (batch.size < 100) {
                            hasMorePages = false
                        } else {
                            page++
                        }
                    }
                } else {
                    Log.e(TAG, "getCongressMembers: API Error on page $page: ${response.message()}")
                    // If we have some data, return what we have? Or fail?
                    // Let's return error to be safe, or break and show partial.
                    // Returning error is safer for now.
                    return Result.Error(Exception("API Error on page $page: ${response.message()}"))
                }
            }
            
            Log.d(TAG, "getCongressMembers: Finished fetching. Total found: ${allCandidates.size}")
            // Wrap in a dummy response object since we expect FecCandidateResponse
            val combinedResponse = FecCandidateResponse(
                results = allCandidates
            )
            Result.Success(combinedResponse)

        } catch (e: Exception) {
            Log.e(TAG, "getCongressMembers: Exception occurred", e)
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
    /**
     * Fetches top contributing organizations (by employer) AND top committee/PAC contributors for a given committee and cycle.
     * Merges the results into a single list of FecEmployerContribution.
     */
    suspend fun getTopOrganizations(committeeId: String, cycle: String): Result<FecEmployerContributionResponse> {
        Log.d(TAG, "getTopOrganizations: Fetching for committeeId=$committeeId, cycle=$cycle")
        val cacheKey = "$committeeId-$cycle"
        organizationsCache[cacheKey]?.let {
            Log.d(TAG, "getTopOrganizations: Returning cached result")
            return it
        }

        return try {
            // Launch parallel requests using kotlinx.coroutines.async if within a coroutine scope,
            // but here we are suspend, so we can just call them sequentially or use coroutineScope { async ... }
            // For simplicity and safety without adding more dependencies right now, we'll do sequential.
            // Ideally use coroutineScope { awaitAll(...) } for parallelism.

            kotlinx.coroutines.coroutineScope {
                val employerDeferred = async {
                    apiService.getTopOrganizationsByEmployer(committeeId = committeeId, cycle = cycle)
                }
                val contributorDeferred = async {
                    apiService.getPacContributions(committeeId = committeeId, cycle = cycle)
                }

                val employerResponse = employerDeferred.await()
                val contributorResponse = contributorDeferred.await()

                val employers = if (employerResponse.isSuccessful && employerResponse.body() != null) {
                    employerResponse.body()!!.results.map {
                        it.apply { type = "Employer" }
                    }
                } else {
                    Log.e(TAG, "getTopOrganizations: Employer API Error: ${employerResponse.message()}")
                    emptyList<io.github.paulleung93.lobbylens.data.model.FecEmployerContribution>()
                }

                val contributors = if (contributorResponse.isSuccessful && contributorResponse.body() != null) {
                    val rawList = contributorResponse.body()!!.results
                    // Manually aggregate by contributor name
                    rawList.groupBy { it.contributorName }
                        .map { (name, items) ->
                            io.github.paulleung93.lobbylens.data.model.FecEmployerContribution(
                                employer = name,
                                total = items.sumOf { it.amount },
                                count = items.size,
                                type = "PAC"
                            )
                        }
                } else {
                    Log.e(TAG, "getTopOrganizations: Contributor API Error: ${contributorResponse.message()}")
                    emptyList<io.github.paulleung93.lobbylens.data.model.FecEmployerContribution>()
                }

                val mergedList = (employers + contributors).sortedByDescending { it.total }
                
                if (mergedList.isNotEmpty()) {
                    val result = Result.Success(FecEmployerContributionResponse(mergedList))
                    organizationsCache[cacheKey] = result
                    result
                } else {
                    Result.Error(Exception("Failed to fetch contribution data"))
                }
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
            Log.d(TAG, "identifyPolitician: RetrofitInstance headers - Package: ${RetrofitInstance.getHeaderInfo()}")
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
     * Fetches lobbyist contributions (LD-203 reports) from the U.S. Senate Lobbying Disclosure API.
     * Searches by honoree name (the politician).
     */
    suspend fun getSenateContributions(politicianName: String): Result<io.github.paulleung93.lobbylens.data.model.SenateContributionResponse> {
        val normalizedName = normalizeNameForSenate(politicianName)
        Log.d(TAG, "getSenateContributions: Fetching for normalizedName=$normalizedName (original=$politicianName)")
        
        return try {
            val response = RetrofitInstance.senateLdaApi.getContributions(honoreeName = normalizedName)
            Log.d(TAG, "getSenateContributions: Response code: ${response.code()}")
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Log.e(TAG, "getSenateContributions: API Error: ${response.code()} ${response.message()}")
                Result.Error(Exception("Senate API Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getSenateContributions: Exception occurred", e)
            Result.Error(e)
        }
    }

    /**
     * Normalizes a name from FEC format (LAST, FIRST MIDDLE) to a looser "FIRST LAST" format
     * that works better for the Senate honoree search.
     */
    private fun normalizeNameForSenate(name: String): String {
        return try {
            val parts = name.split(",").map { it.trim() }
            if (parts.size >= 2) {
                val last = parts[0]
                // Take only the first word of the second part (the first name)
                // FEC names are often "FIRST MIDDLE SUFFIX"
                val first = parts[1].split(Regex("\\s+"))[0]
                "$first $last".trim()
            } else {
                name.trim() // Fallback to original
            }
        } catch (e: Exception) {
            name.trim()
        }
    }

    /**
     * Generates a new image with logos using Firebase AI Imagen.
     */
    suspend fun generatePoliticianImage(baseBitmap: Bitmap, logos: List<String>): Result<Bitmap> {
        Log.d(TAG, "generatePoliticianImage: Starting image generation with logos: $logos")
        return try {
            val prompt = "A photo of a politician wearing a suit with ${logos.joinToString(", ")} logo pins on the lapel. Photorealistic, high quality."
            Log.d(TAG, "generatePoliticianImage: Prompt: $prompt")

            // Initialize Firebase Imagen model
            val firebaseAI = com.google.firebase.Firebase.ai(backend = com.google.firebase.ai.type.GenerativeBackend.vertexAI())
            val imagenModel = firebaseAI.imagenModel("imagen-3.0-generate-002")

            Log.d(TAG, "generatePoliticianImage: Calling Firebase Imagen API")
            val imageResponse = imagenModel.generateImages(prompt)
            
            val image = imageResponse.images.firstOrNull()
            if (image != null) {
                val generatedBitmap = image.asBitmap()
                Log.i(TAG, "generatePoliticianImage: Successfully generated image")
                Result.Success(generatedBitmap)
            } else {
                Log.e(TAG, "generatePoliticianImage: No image generated")
                Result.Error(Exception("No image generated."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "generatePoliticianImage: Exception occurred", e)
            Result.Error(e)
        }
    }
}
