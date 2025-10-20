package io.github.paulleung93.lobbylens.ui.details

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.paulleung93.lobbylens.data.model.Organization
import io.github.paulleung93.lobbylens.data.repository.PoliticianRepository
import io.github.paulleung93.lobbylens.util.Result
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

/**
 * ViewModel for the Details screen.
 * It is responsible for fetching top organization data for a specific politician across multiple cycles,
 * using a Result wrapper for robust and graceful error handling.
 */
class DetailsViewModel : ViewModel() {

    private val repository = PoliticianRepository()

    val historicalOrganizations = mutableStateOf<Map<String, List<Organization>>>(emptyMap())
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)

    /**
     * Fetches historical data concurrently and handles partial failures gracefully.
     * @param cid The campaign ID of the politician.
     */
    fun fetchHistoricalData(cid: String) {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            historicalOrganizations.value = emptyMap()

            val cycles = listOf("2024", "2022", "2020")
            val historicalData = cycles.map { cycle ->
                async {
                    when (val result = repository.getTopOrganizations(cid, cycle)) {
                        is Result.Success -> cycle to result.data.response.organizations.organizationList
                        is Result.Error -> cycle to null // Use null to indicate failure
                        else -> cycle to null
                    }
                }
            }.awaitAll()
             .mapNotNull { it.first to (it.second ?: return@mapNotNull null) }
             .toMap()

            if (historicalData.isEmpty()) {
                errorMessage.value = "Failed to fetch any historical data. Please check your connection."
            } else {
                historicalOrganizations.value = historicalData
            }

            isLoading.value = false
        }
    }
}
