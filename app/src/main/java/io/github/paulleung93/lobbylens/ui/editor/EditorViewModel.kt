package io.github.paulleung93.lobbylens.ui.editor

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.paulleung93.lobbylens.data.model.FecCandidate
import io.github.paulleung93.lobbylens.data.model.FecEmployerContribution
import io.github.paulleung93.lobbylens.data.repository.PoliticianRepository
import io.github.paulleung93.lobbylens.util.LogoUtils
import io.github.paulleung93.lobbylens.util.Result
import kotlinx.coroutines.launch

/**
 * ViewModel for the Editor screen, fully refactored for the FEC API.
 * It is responsible for searching for candidates, fetching their top contributing employers,
 * and downloading logos for visualization.
 */
class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PoliticianRepository()
    
    companion object {
        private const val TAG = "EditorViewModel"
    }

    // --- UI State --- //
    val candidates = mutableStateOf<List<FecCandidate>>(emptyList())
    val topOrganizations = mutableStateOf<List<FecEmployerContribution>>(emptyList())
    val organizationLogos = mutableStateOf<Map<String, Bitmap>>(emptyMap())
    val generatedImage = mutableStateOf<Bitmap?>(null) // New state for Vertex AI result
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)
    val selectedCycle = mutableStateOf("2024") // Default to the most recent cycle

    /**
     * Searches for candidates by name using the FEC repository.
     * @param name The name of the politician to search for.
     */
    fun searchCandidatesByName(name: String) {
        Log.d(TAG, "searchCandidatesByName: Searching for '$name'")
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            candidates.value = emptyList()

            when (val result = repository.searchCandidatesByName(name)) {
                is Result.Success -> {
                    Log.i(TAG, "searchCandidatesByName: Success - found ${result.data.results.size} candidates")
                    candidates.value = result.data.results
                }
                is Result.Error -> {
                    Log.e(TAG, "searchCandidatesByName: Error - ${result.exception.message}", result.exception)
                    errorMessage.value = "Failed to fetch candidates: ${result.exception.message}"
                }
                else -> { /* No-op for loading state */ }
            }
            isLoading.value = false
        }
    }

    /**
     * Fetches top contributing organizations (by employer) for a given candidate and cycle.
     * @param cid The candidate's unique FEC campaign ID.
     * @param cycle The election cycle to query.
     */
    fun fetchTopOrganizations(cid: String, cycle: String) {
        Log.d(TAG, "fetchTopOrganizations: Fetching for cid=$cid, cycle=$cycle")
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            selectedCycle.value = cycle

            // Clear previous data
            topOrganizations.value = emptyList()
            organizationLogos.value = emptyMap()

            when (val result = repository.getTopOrganizations(cid, cycle)) {
                is Result.Success -> {
                    val newOrganizations = result.data.results
                    Log.i(TAG, "fetchTopOrganizations: Success - found ${newOrganizations.size} organizations")
                    topOrganizations.value = newOrganizations
                    // Logos are no longer needed for Generative AI, but we might want them for Details screen.
                    // For Generative AI, we pass names.
                    // fetchOrganizationLogos(newOrganizations) 
                }
                is Result.Error -> {
                    Log.e(TAG, "fetchTopOrganizations: Error - ${result.exception.message}", result.exception)
                    errorMessage.value = "Failed to fetch organization data for cycle $cycle: ${result.exception.message}"
                }
                 else -> { /* No-op for loading state */ }
            }
            isLoading.value = false
        }
    }

    /**
     * Fetches logos for a list of contributing employers.
     * @param organizations The list of organizations from the FEC API.
     */
    private fun fetchOrganizationLogos(organizations: List<FecEmployerContribution>) {
        viewModelScope.launch {
            val logos = mutableMapOf<String, Bitmap>()
            organizations.forEach { org ->
                // The employer name is the organization name in this context
                val orgName = org.employer
                val logoBitmap = LogoUtils.fetchLogo(getApplication(), orgName)
                if (logoBitmap != null) {
                    logos[orgName] = logoBitmap
                }
            }
            organizationLogos.value = logos
        }
    }
        /**
     * Identifies a politician from an image using Cloud Vision.
     */
    fun identifyPolitician(bitmap: Bitmap) {
        Log.d(TAG, "identifyPolitician: Starting identification")
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            
            when (val result = repository.identifyPolitician(bitmap)) {
                is Result.Success -> {
                    val candidate = result.data
                    Log.i(TAG, "identifyPolitician: Success - identified ${candidate.name} (${candidate.candidateId})")
                    candidates.value = listOf(candidate)
                    // Auto-fetch details
                    fetchTopOrganizations(candidate.candidateId, selectedCycle.value)
                }
                is Result.Error -> {
                    Log.e(TAG, "identifyPolitician: Error - ${result.exception.message}", result.exception)
                    errorMessage.value = "Identification failed: ${result.exception.message}"
                }
                else -> {}
            }
            isLoading.value = false
        }
    }

    /**
     * Generates the final image using Vertex AI.
     */
    fun generateImage(originalBitmap: Bitmap) {
        Log.d(TAG, "generateImage: Starting image generation")
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            
            val companies = topOrganizations.value.map { it.employer }.take(5) // Limit to top 5
            Log.d(TAG, "generateImage: Using top ${companies.size} companies: $companies")
            if (companies.isEmpty()) {
                errorMessage.value = "No organizations found to display."
                isLoading.value = false
                return@launch
            }

            when (val result = repository.generatePoliticianImage(originalBitmap, companies)) {
                 is Result.Success -> {
                     Log.i(TAG, "generateImage: Success - image generated")
                     generatedImage.value = result.data
                 }
                 is Result.Error -> {
                     Log.e(TAG, "generateImage: Error - ${result.exception.message}", result.exception)
                     errorMessage.value = "Generation failed: ${result.exception.message}"
                 }
                 else -> {}
            }
             isLoading.value = false
        }
    }
}
