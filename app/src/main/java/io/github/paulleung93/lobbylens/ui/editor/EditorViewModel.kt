package io.github.paulleung93.lobbylens.ui.editor

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.paulleung93.lobbylens.data.model.FecCandidate
import io.github.paulleung93.lobbylens.data.model.FecEmployerContribution
import io.github.paulleung93.lobbylens.data.repository.PoliticianRepository
import io.github.paulleung93.lobbylens.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * ViewModel for the Editor screen, fully refactored for the FEC API.
 * Uses StateFlow and sealed UI state for structured state management.
 */
class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PoliticianRepository()
    
    companion object {
        private const val TAG = "EditorViewModel"
    }

    // --- UI State --- //
    private val _uiState = MutableStateFlow<EditorUiState>(EditorUiState.Initial)
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    // Search query for the manual search mode
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Selected cycle for filtering
    private val _selectedCycle = MutableStateFlow("2024")
    val selectedCycle: StateFlow<String> = _selectedCycle.asStateFlow()

    // Internal storage for intermediate processing data
    private var currentCandidate: FecCandidate? = null
    private var currentBitmap: Bitmap? = null
    private var currentOrganizations: List<FecEmployerContribution> = emptyList()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Loads current members of congress (incumbents) into the candidates list.
     */
    fun loadCongressMembers() {
        Log.d(TAG, "loadCongressMembers: Loading incumbents...")
        viewModelScope.launch {
            _uiState.value = EditorUiState.SearchResults(candidates = emptyList(), isLoading = true)

            when (val result = repository.getCongressMembers(_selectedCycle.value)) {
                is Result.Success -> {
                    Log.i(TAG, "loadCongressMembers: Success - found ${result.data.results.size} members")
                    _uiState.value = EditorUiState.SearchResults(candidates = result.data.results)
                }
                is Result.Error -> {
                    Log.e(TAG, "loadCongressMembers: Error - ${result.exception.message}", result.exception)
                    _uiState.value = EditorUiState.Error("Failed to fetch members: ${result.exception.message}")
                }
                else -> { /* No-op */ }
            }
        }
    }

    /**
     * Searches for candidates by name using the FEC repository.
     * @param name The name of the politician to search for.
     */
    fun searchCandidatesByName(name: String) {
        Log.d(TAG, "searchCandidatesByName: Searching for '$name'")
        viewModelScope.launch {
            _uiState.value = EditorUiState.SearchResults(candidates = emptyList(), isLoading = true)

            when (val result = repository.searchCandidatesByName(name)) {
                is Result.Success -> {
                    Log.i(TAG, "searchCandidatesByName: Success - found ${result.data.results.size} candidates")
                    _uiState.value = EditorUiState.SearchResults(candidates = result.data.results)
                }
                is Result.Error -> {
                    Log.e(TAG, "searchCandidatesByName: Error - ${result.exception.message}", result.exception)
                    _uiState.value = EditorUiState.Error("Failed to fetch candidates: ${result.exception.message}")
                }
                else -> { /* No-op for loading state */ }
            }
        }
    }

    /**
     * Processes the image URI on a background thread to decode the bitmap.
     */
    fun processImage(uri: String, context: Context) {
        Log.d(TAG, "processImage: Starting image processing for URI")
        viewModelScope.launch {
            _uiState.value = EditorUiState.LoadingImage
            
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    val decodedUri = URLDecoder.decode(uri, StandardCharsets.UTF_8.toString())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, Uri.parse(decodedUri)))
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, Uri.parse(decodedUri))
                    }.copy(Bitmap.Config.ARGB_8888, true)
                }
                
                currentBitmap = bitmap
                Log.d(TAG, "processImage: Bitmap decoded, size: ${bitmap.width}x${bitmap.height}")
                
                // Trigger Identification immediately after loading
                identifyPolitician(bitmap)
                
            } catch (e: Exception) {
                Log.e(TAG, "processImage: Failed to decode image", e)
                _uiState.value = EditorUiState.Error("Failed to load image: ${e.message}")
            }
        }
    }

    /**
     * Identifies a politician from an image using Cloud Vision.
     */
    private suspend fun identifyPolitician(bitmap: Bitmap) {
        Log.d(TAG, "identifyPolitician: Starting identification")
        _uiState.value = EditorUiState.Identifying
        
        when (val result = repository.identifyPolitician(bitmap)) {
            is Result.Success -> {
                val candidate = result.data
                Log.i(TAG, "identifyPolitician: Success - identified ${candidate.name} (${candidate.candidateId})")
                currentCandidate = candidate
                // Auto-fetch details
                fetchTopOrganizations(candidate.candidateId, _selectedCycle.value)
            }
            is Result.Error -> {
                Log.e(TAG, "identifyPolitician: Error - ${result.exception.message}", result.exception)
                _uiState.value = EditorUiState.Error("Identification failed: ${result.exception.message}")
            }
            else -> {}
        }
    }

    /**
     * Fetches top contributing organizations (by employer) for a given candidate and cycle.
     */
    private suspend fun fetchTopOrganizations(cid: String, cycle: String) {
        Log.d(TAG, "fetchTopOrganizations: Fetching for cid=$cid, cycle=$cycle")

        // Step 1: Fetch Candidate Committee History to get a valid Principal Committee ID
        var committeeId: String? = null
        when (val historyResult = repository.getCandidateCommitteeHistory(cid)) {
            is Result.Success -> {
                val historySorted = historyResult.data.results.sortedByDescending { it.cycle }
                val principalCommittee = historySorted.firstOrNull { it.designation == "P" }

                if (principalCommittee != null) {
                    committeeId = principalCommittee.committeeId
                } else {
                    Log.w(TAG, "fetchTopOrganizations: No principal committee found for candidate $cid")
                    _uiState.value = EditorUiState.Error("No principal campaign committee found.")
                    return
                }
            }
            is Result.Error -> {
                Log.e(TAG, "fetchTopOrganizations: Failed to fetch committee history: ${historyResult.exception.message}")
                _uiState.value = EditorUiState.Error("Failed to load committee history.")
                return
            }
            else -> {
                Log.d(TAG, "fetchTopOrganizations: Candidate history is loading")
                return
            }
        }

        // Step 2: Fetch Contribution Data using the resolved Committee ID
        when (val result = repository.getTopOrganizations(committeeId, cycle)) {
            is Result.Success -> {
                val organizations = result.data.results
                Log.i(TAG, "fetchTopOrganizations: Success - found ${organizations.size} organizations")
                currentOrganizations = organizations
                
                // Trigger image generation if we have organizations
                if (organizations.isNotEmpty() && currentBitmap != null) {
                    generateImage(currentBitmap!!)
                } else {
                    // No organizations, show success with no generated image
                    _uiState.value = EditorUiState.ImageProcessingSuccess(
                        candidate = currentCandidate!!,
                        originalBitmap = currentBitmap!!,
                        generatedImage = null,
                        organizations = organizations
                    )
                }
            }
            is Result.Error -> {
                Log.e(TAG, "fetchTopOrganizations: Error - ${result.exception.message}", result.exception)
                _uiState.value = EditorUiState.Error("Failed to fetch organization data: ${result.exception.message}")
            }
            else -> { /* No-op */ }
        }
    }

    /**
     * Generates the final image using Vertex AI.
     */
    private suspend fun generateImage(originalBitmap: Bitmap) {
        Log.d(TAG, "generateImage: Starting image generation")
        _uiState.value = EditorUiState.GeneratingVisualization
        
        val companies = currentOrganizations.map { it.employer }.take(5)
        Log.d(TAG, "generateImage: Using top ${companies.size} companies: $companies")
        
        if (companies.isEmpty()) {
            _uiState.value = EditorUiState.ImageProcessingSuccess(
                candidate = currentCandidate!!,
                originalBitmap = originalBitmap,
                generatedImage = null,
                organizations = currentOrganizations
            )
            return
        }

        when (val result = repository.generatePoliticianImage(originalBitmap, companies)) {
            is Result.Success -> {
                Log.i(TAG, "generateImage: Success - image generated")
                _uiState.value = EditorUiState.ImageProcessingSuccess(
                    candidate = currentCandidate!!,
                    originalBitmap = originalBitmap,
                    generatedImage = result.data,
                    organizations = currentOrganizations
                )
            }
            is Result.Error -> {
                Log.e(TAG, "generateImage: Error - ${result.exception.message}", result.exception)
                // Even on generation error, show success with original image
                _uiState.value = EditorUiState.ImageProcessingSuccess(
                    candidate = currentCandidate!!,
                    originalBitmap = originalBitmap,
                    generatedImage = null,
                    organizations = currentOrganizations
                )
            }
            else -> {}
        }
    }

    /**
     * Resets the UI state to initial.
     */
    fun resetState() {
        _uiState.value = EditorUiState.Initial
        currentCandidate = null
        currentBitmap = null
        currentOrganizations = emptyList()
    }
}
