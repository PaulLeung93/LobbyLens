package io.github.paulleung93.lobbylens.ui.details

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.paulleung93.lobbylens.data.model.FecEmployerContribution
import io.github.paulleung93.lobbylens.data.repository.PoliticianRepository
import io.github.paulleung93.lobbylens.util.Result
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

/**
 * ViewModel for the Details screen.
 * It is responsible for fetching top contributor data for a specific politician across multiple cycles,
 * using a Result wrapper for robust and graceful error handling.
 */
class DetailsViewModel : ViewModel() {

    private val repository = PoliticianRepository()

    val historicalOrganizations = mutableStateOf<Map<String, List<FecEmployerContribution>>>(emptyMap())
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

            // Step 1: Launch all API calls in parallel. Each job will return a Pair.
            val deferredResults = cycles.map { cycle ->
                async {
                    when (val result = repository.getTopOrganizations(cid, cycle)) {
                        is Result.Success -> cycle to result.data.results
                        // For any error or other state, treat it as a null result. This makes the 'when' exhaustive.
                        else -> cycle to null
                    }
                }
            }

            // Step 2: CORRECTLY wait for all the parallel jobs to complete BEFORE processing.
            val results = deferredResults.awaitAll()

            // Step 3: Process the now-completed results. Filter out any failed (null) cycles and convert to a map.
            val successfulData = results.mapNotNull { (cycle, contributions) ->
                contributions?.let { cycle to it }
            }.toMap()

            // Step 4: Update the UI state based on the final, processed data.
            if (successfulData.isEmpty() && results.any { it.second == null }) {
                // Show an error only if at least one request failed AND no data was successfully retrieved.
                errorMessage.value = "Failed to fetch historical data. Please check your connection."
            } else {
                historicalOrganizations.value = successfulData
            }

            isLoading.value = false
        }
    }
}
