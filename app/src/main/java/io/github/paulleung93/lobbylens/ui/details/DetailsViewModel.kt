package io.github.paulleung93.lobbylens.ui.details

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.paulleung93.lobbylens.data.model.Organization
import io.github.paulleung93.lobbylens.data.repository.PoliticianRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for the Details screen.
 * It is responsible for fetching top organization data for a specific politician.
 */
class DetailsViewModel : ViewModel() {

    private val repository = PoliticianRepository()

    val organizations = mutableStateOf<List<Organization>>(emptyList())
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)

    /**
     * Fetches the top contributing organizations for a given politician's campaign ID (cid).
     * @param cid The campaign ID of the politician.
     * @param cycle The election cycle.
     */
    fun fetchTopOrganizations(cid: String, cycle: String = "2022") {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            try {
                val response = repository.getTopOrganizations(cid = cid, cycle = cycle)
                organizations.value = response.response.organizations.organizationList
            } catch (e: Exception) {
                errorMessage.value = "Failed to fetch data: ${e.message}"
            }
            isLoading.value = false
        }
    }
}
