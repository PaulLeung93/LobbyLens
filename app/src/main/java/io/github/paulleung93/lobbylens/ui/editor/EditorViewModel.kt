package io.github.paulleung93.lobbylens.ui.editor

import android.app.Application
import android.graphics.Bitmap
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

    // --- UI State --- //
    val candidates = mutableStateOf<List<FecCandidate>>(emptyList())
    val topOrganizations = mutableStateOf<List<FecEmployerContribution>>(emptyList())
    val organizationLogos = mutableStateOf<Map<String, Bitmap>>(emptyMap())
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)
    val selectedCycle = mutableStateOf("2024") // Default to the most recent cycle

    /**
     * Searches for candidates by name using the FEC repository.
     * @param name The name of the politician to search for.
     */
    fun searchCandidatesByName(name: String) {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            candidates.value = emptyList()

            when (val result = repository.searchCandidatesByName(name)) {
                is Result.Success -> {
                    candidates.value = result.data.results
                }
                is Result.Error -> {
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
                    topOrganizations.value = newOrganizations
                    // Now, fetch the logos for these new organizations
                    fetchOrganizationLogos(newOrganizations)
                }
                is Result.Error -> {
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
}
