package io.github.paulleung93.lobbylens.data.repository

import android.graphics.Bitmap
import android.util.Log
import io.github.paulleung93.lobbylens.BuildConfig
import io.github.paulleung93.lobbylens.data.model.AnnotateImageRequest
import io.github.paulleung93.lobbylens.data.model.CloudVisionRequest
import io.github.paulleung93.lobbylens.data.model.FecCandidate
import io.github.paulleung93.lobbylens.data.model.Feature
import io.github.paulleung93.lobbylens.data.model.ImageContent
import io.github.paulleung93.lobbylens.data.network.CloudVisionService
import io.github.paulleung93.lobbylens.util.ImageUtils
import io.github.paulleung93.lobbylens.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Cloud Vision API operations.
 * Handles politician identification from images.
 */
@Singleton
class VisionRepository @Inject constructor(
    private val cloudVisionService: CloudVisionService,
    private val fecRepository: FecRepository
) {
    companion object {
        private const val TAG = "VisionRepository"
        
        // Entities to ignore (generic terms that match too broadly)
        private val IGNORED_ENTITIES = listOf(
            "United States", "Politics", "Government", "Society",
            "Public Speaking", "Event", "Official", "Businessperson",
            "Spokesperson", "Chairperson", "Senator", "Representative",
            "Computer", "Computer Keyboard", "Keyboard", "Mouse", "Computer mouse",
            "Screen", "Monitor", "Laptop", "MacBook", "Tablet", "USB",
            "DisplayLink", "Wireless keyboard"
        )
        
        // Common nickname mappings
        private val NICKNAME_MAP = mapOf(
            "Chuck" to "Charles",
            "Bill" to "William",
            "Bob" to "Robert",
            "Dick" to "Richard",
            "Jim" to "James",
            "Mike" to "Michael",
            "Tom" to "Thomas",
            "Joe" to "Joseph",
            "Tim" to "Timothy",
            "Dan" to "Daniel",
            "Dave" to "David",
            "Ted" to "Edward",
            "Tony" to "Anthony",
            "Bernie" to "Bernard",
            "Beth" to "Elizabeth",
            "Liz" to "Elizabeth",
            "Katie" to "Katherine",
            "Kate" to "Katherine",
            "Chris" to "Christopher",
            "Matt" to "Matthew",
            "Alex" to "Alexander",
            "Andy" to "Andrew",
            "Greg" to "Gregory",
            "Steve" to "Steven",
            "Pat" to "Patricia"
        )
    }

    /**
     * Identifies a politician from an image using Google Cloud Vision API.
     * Returns the FEC Candidate object if a match is found.
     */
    suspend fun identifyPolitician(imageBitmap: Bitmap): Result<FecCandidate> = withContext(Dispatchers.IO) {
        Log.d(TAG, "identifyPolitician: Starting politician identification")
        try {
            val base64Image = ImageUtils.bitmapToBase64(imageBitmap)
            Log.d(TAG, "identifyPolitician: Image encoded to base64, length: ${base64Image.length}")
            
            val request = CloudVisionRequest(
                requests = listOf(
                    AnnotateImageRequest(
                        image = ImageContent(base64Image),
                        features = listOf(
                            Feature("WEB_DETECTION"),
                            Feature("FACE_DETECTION", maxResults = 1)
                        )
                    )
                )
            )

            Log.d(TAG, "identifyPolitician: Calling Cloud Vision API")
            val response = cloudVisionService.annotateImage(BuildConfig.GOOGLE_API_KEY, request)
            Log.d(TAG, "identifyPolitician: Cloud Vision response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val annotationResponse = response.body()!!.responses?.firstOrNull()
                val webAnnotations = annotationResponse?.webDetection
                val faceAnnotations = annotationResponse?.faceAnnotations

                val entities = webAnnotations?.webEntities
                Log.d(TAG, "identifyPolitician: Found ${entities?.size ?: 0} web entities")

                entities?.forEach {
                    Log.d(TAG, "identifyPolitician: Detected entity: ${it.description} (score: ${it.score})")
                }

                if (entities.isNullOrEmpty()) {
                    Log.w(TAG, "identifyPolitician: No entities detected")
                    return@withContext Result.Error(Exception("No entities detected."))
                }

                // Iterate through top 10 entities and try to search FEC
                for (entity in entities.take(10)) {
                    val description = entity.description ?: continue
                    
                    // Skip blocked entities
                    if (IGNORED_ENTITIES.any { description.contains(it, ignoreCase = true) }) {
                        Log.d(TAG, "identifyPolitician: Skipping generic entity: $description")
                        continue
                    }

                    Log.d(TAG, "identifyPolitician: Trying entity: $description (score: ${entity.score})")
                    var searchResult = fecRepository.searchCandidatesByName(description)

                    // Fallback 1: Try first 2 words
                    if (searchResult is Result.Success && searchResult.data.results.isEmpty()) {
                        val words = description.split(" ")
                        if (words.size > 2) {
                            val shortQuery = "${words[0]} ${words[1]}"
                            Log.d(TAG, "identifyPolitician: Retry with shorter query: '$shortQuery'")
                            val retryResult = fecRepository.searchCandidatesByName(shortQuery)
                            if (retryResult is Result.Success && retryResult.data.results.isNotEmpty()) {
                                searchResult = retryResult
                            }
                        }
                    }

                    // Fallback 2: Try name variations
                    if (searchResult is Result.Success && searchResult.data.results.isEmpty()) {
                        val nameToVary = if (description.split(" ").size > 2) {
                            "${description.split(" ")[0]} ${description.split(" ")[1]}"
                        } else {
                            description
                        }

                        val variations = generateNameVariations(nameToVary)
                        for (variation in variations) {
                            if (variation == description || variation == nameToVary) continue
                            Log.d(TAG, "identifyPolitician: Trying variation: '$variation'")
                            val variationResult = fecRepository.searchCandidatesByName(variation)
                            if (variationResult is Result.Success && variationResult.data.results.isNotEmpty()) {
                                Log.i(TAG, "identifyPolitician: Found match using name variation: '$variation'")
                                searchResult = variationResult
                                break
                            }
                        }
                    }

                    if (searchResult is Result.Success && searchResult.data.results.isNotEmpty()) {
                        val candidate = searchResult.data.results.first()
                        Log.i(TAG, "identifyPolitician: Match found: ${candidate.name}")

                        // Attach face vertices if available
                        if (!faceAnnotations.isNullOrEmpty()) {
                            candidate.faceVertices = faceAnnotations[0].boundingPoly?.vertices
                            Log.d(TAG, "identifyPolitician: Attached face vertices: ${candidate.faceVertices}")
                        }

                        return@withContext Result.Success(candidate)
                    }
                }
                
                Log.w(TAG, "identifyPolitician: Could not identify politician in FEC database")
                Result.Error(Exception("Could not identify politician in FEC database."))
            } else {
                Log.e(TAG, "identifyPolitician: Cloud Vision API Error: ${response.message()}")
                Result.Error(Exception("Cloud Vision API Error: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "identifyPolitician: Exception occurred", e)
            Result.Error(e)
        }
    }

    private fun generateNameVariations(name: String): List<String> {
        val variations = mutableListOf(name)
        val words = name.split(" ")
        
        if (words.size >= 2) {
            val firstName = words[0]
            val lastName = words.drop(1).joinToString(" ")

            // If first name is a nickname, add formal version
            NICKNAME_MAP[firstName]?.let { formalName ->
                variations.add("$formalName $lastName")
            }

            // Also try the reverse
            val entry = NICKNAME_MAP.entries.find { it.value == firstName }
            entry?.let {
                variations.add("${it.key} $lastName")
            }
        }

        return variations.distinct()
    }
}
