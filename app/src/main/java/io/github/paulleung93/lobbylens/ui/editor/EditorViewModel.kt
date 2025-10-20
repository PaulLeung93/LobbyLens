package io.github.paulleung93.lobbylens.ui.editor

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.paulleung93.lobbylens.data.model.Legislator
import io.github.paulleung93.lobbylens.data.model.Organization
import io.github.paulleung93.lobbylens.data.repository.PoliticianRepository
import io.github.paulleung93.lobbylens.util.LogoUtils
import io.github.paulleung93.lobbylens.util.Result
import kotlinx.coroutines.launch

/**
 * ViewModel for the Editor screen.
 * It is responsible for fetching legislator and organization data, downloading logos,
 * and managing the UI state using a Result wrapper for robust error handling.
 */
class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PoliticianRepository()

    val legislators = mutableStateOf<List<Legislator>>(emptyList())
    val topOrganizations = mutableStateOf<List<Organization>>(emptyList())
    val organizationLogos = mutableStateOf<Map<String, Bitmap>>(emptyMap())
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)
    val selectedCycle = mutableStateOf("2022") // Add state for the selected cycle

    /**
     * Searches for legislators by name using the Result wrapper.
     * @param name The name of the politician to search for.
     */
    fun searchLegislators(name: String) {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            when (val result = repository.getLegislators(name)) {
                is Result.Success -> {
                    legislators.value = result.data.response.legislators
                }
                is Result.Error -> {
                    errorMessage.value = "Failed to fetch legislators: ${result.exception.message}"
                }
                else -> {}
            }
            isLoading.value = false
        }
    }

    /**
     * Fetches top organizations using the Result wrapper.
     * @param cid The politician's unique campaign ID.
     * @param cycle The election cycle to query.
     */
    fun fetchTopOrganizations(cid: String, cycle: String) {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            selectedCycle.value = cycle

            topOrganizations.value = emptyList()
            organizationLogos.value = emptyMap()

            when (val result = repository.getTopOrganizations(cid, cycle)) {
                is Result.Success -> {
                    val newOrganizations = result.data.response.organizations.organizationList
                    topOrganizations.value = newOrganizations
                    fetchOrganizationLogos(newOrganizations)
                }
                is Result.Error -> {
                    errorMessage.value = "Failed to fetch organization data for cycle $cycle: ${result.exception.message}"
                }
                else -> {}
            }
            isLoading.value = false
        }
    }

    /**
     * Fetches logos for a list of organizations.
     * @param organizations The list of organizations.
     */
    private fun fetchOrganizationLogos(organizations: List<Organization>) {
        viewModelScope.launch {
            val logos = mutableMapOf<String, Bitmap>()
            organizations.forEach { org ->
                val orgName = org.attributes.orgName
                val logoBitmap = LogoUtils.fetchLogo(getApplication(), orgName)
                if (logoBitmap != null) {
                    logos[orgName] = logoBitmap
                }
            }
            organizationLogos.value = logos
        }
    }
}
