package io.github.paulleung93.lobbylens.ui.details

import android.util.Log
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
    
    companion object {
        private const val TAG = "DetailsViewModel"
    }

    val historicalOrganizations = mutableStateOf<Map<String, List<FecEmployerContribution>>>(emptyMap())
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)

    /**
     * Fetches historical data concurrently and handles partial failures gracefully.
     * @param cid The campaign ID of the politician.
     */
    fun fetchHistoricalData(cid: String) {
        Log.d(TAG, "fetchHistoricalData: Starting fetch for cid=$cid")
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            historicalOrganizations.value = emptyMap()

            val cycles = listOf("2024", "2022", "2020")
            Log.d(TAG, "fetchHistoricalData: Fetching data for cycles: $cycles")

            // Step 1: Launch all API calls in parallel. Each job will return a Pair.
            val deferredResults = cycles.map { cycle ->
                async {
                    Log.d(TAG, "fetchHistoricalData: Launching fetch for cycle $cycle")
                    when (val result = repository.getTopOrganizations(cid, cycle)) {
                        is Result.Success -> {
                            Log.d(TAG, "fetchHistoricalData: Success for cycle $cycle - ${result.data.results.size} organizations")
                            cycle to result.data.results
                        }
                        is Result.Error -> {
                            Log.e(TAG, "fetchHistoricalData: Error for cycle $cycle - ${result.exception.message}", result.exception)
                            cycle to null
                        }
                        // For any error or other state, treat it as a null result. This makes the 'when' exhaustive.
                        else -> {
                            Log.w(TAG, "fetchHistoricalData: Unknown result for cycle $cycle")
                            cycle to null
                        }
                    }
                }
            }

            // Step 2: CORRECTLY wait for all the parallel jobs to complete BEFORE processing.
            val results = deferredResults.awaitAll()
            Log.d(TAG, "fetchHistoricalData: All requests completed. Processing results...")

            // Step 3: Process the now-completed results. Filter out any failed (null) cycles and convert to a map.
            val successfulData = results.mapNotNull { (cycle, contributions) ->
                // Sort contributions by total amount descending
                contributions?.let { 
                    cycle to it.sortedByDescending { item -> item.total }
                }
            }.toMap()
            Log.d(TAG, "fetchHistoricalData: Successful cycles: ${successfulData.keys}, total organizations: ${successfulData.values.sumOf { it.size }}")

            // Step 4: Update the UI state based on the final, processed data.
            if (successfulData.isEmpty() && results.any { it.second == null }) {
                // Show an error only if at least one request failed AND no data was successfully retrieved.
                Log.e(TAG, "fetchHistoricalData: All requests failed or returned no data")
                errorMessage.value = "Failed to fetch historical data. Please check your connection."
            } else {
                Log.i(TAG, "fetchHistoricalData: Successfully loaded data for ${successfulData.size} cycles")
                historicalOrganizations.value = successfulData
            }

            isLoading.value = false
        }
    }
    // State for the currently selected year. "All" means no filter.
    val selectedYear = mutableStateOf("All")

    /**
     * Updates the selected year filter.
     */
    fun selectYear(year: String) {
        selectedYear.value = year
    }

    /**
     * Returns the list of organizations filtered by the selected year.
     * If "All" is selected, it effectively flattens the list (or we could show all cycles).
     * For the "All" view in the list, we might want to just show everything or group them.
     * Based on the requirement "list of years... act as a filter", 
     * when a year is selected, we show that year's data. 
     * When "All" is selected, we show all data (existing behavior).
     */
    val filteredOrganizations: List<FecEmployerContribution>
        get() {
            val allData = historicalOrganizations.value
            return if (selectedYear.value == "All") {
                // If "All", we might want to return everything, but the UI expects a list of contributions.
                // The current UI iterates over the map. 
                // To keep it simple for the UI, let's let the UI decide how to render "All" (map iteration)
                // vs "Single Year" (list iteration).
                // Or better, let's expose the map, and if a year is selected, this map only contains that year.
                emptyList() // Not used when "All" is active, refer to historicalOrganizations
            } else {
                allData[selectedYear.value] ?: emptyList()
            }
        }
}
