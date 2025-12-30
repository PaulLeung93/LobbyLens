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
import androidx.compose.runtime.State

enum class DetailsViewType {
    CAMPAIGN, LOBBYIST
}

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
    val senateContributions = mutableStateOf<List<io.github.paulleung93.lobbylens.data.model.SenateContributionReport>>(emptyList())
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)
    val senateErrorMessage = mutableStateOf<String?>(null)
    
    private val _selectedView = mutableStateOf(DetailsViewType.CAMPAIGN)
    val selectedView: State<DetailsViewType> = _selectedView

    fun updateViewType(viewType: DetailsViewType) {
        _selectedView.value = viewType
    }

    /**
     * Fetches historical data concurrently and handles partial failures gracefully.
     * @param cid The campaign ID of the politician.
     */
    fun fetchHistoricalData(cid: String) {

        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            senateErrorMessage.value = null
            historicalOrganizations.value = emptyMap()
            senateContributions.value = emptyList()

            // Step 1: Fetch Candidate Committee History to get a valid Principal Committee ID
            var committeeId: String? = null
            // We need to resolve the committee ID first. We want the Principal Campaign Committee ("P").
            when (val historyResult = repository.getCandidateCommitteeHistory(cid)) {
                is Result.Success -> {
                    // Sort descending by cycle to get the latest
                    val historySorted = historyResult.data.results.sortedByDescending { it.cycle }
                    
                    // Find the first Principal Campaign Committee (designation = "P")
                    // If no principal committee is found, fallback to any committee or handle error.
                    val principalCommittee = historySorted.firstOrNull { 
                        it.designation == "P" 
                    }

                    if (principalCommittee != null) {
                        committeeId = principalCommittee.committeeId
                    } else {
                        Log.w(TAG, "fetchHistoricalData: No principal committee (designation='P') found for candidate $cid")
                        // Fallback: Try to find any committee if P is missing, or strictly require P.
                        // Sticking to P for accuracy as per plan.
                        errorMessage.value = "No principal campaign committee found."
                    }
                }
                is Result.Error -> {
                    Log.e(TAG, "fetchHistoricalData: Failed to fetch candidate committee history: ${historyResult.exception.message}")
                    errorMessage.value = "Failed to load committee history."
                }
                else -> {
                    Log.d(TAG, "fetchHistoricalData: Candidate history is loading or in an unknown state.")
                }
            }

            if (committeeId == null) {
                isLoading.value = false
                return@launch
            }

            // Step 2: Fetch Contribution Data using the resolved Committee ID
            val cycles = listOf("2024", "2022", "2020")
            Log.d(TAG, "fetchHistoricalData: Fetching data for committeeId=$committeeId, cycles=$cycles")

            // Launch all API calls in parallel. Each job will return a Pair.
            val deferredResults = cycles.map { cycle ->
                async {
                    Log.d(TAG, "fetchHistoricalData: Launching fetch for cycle $cycle")
                    when (val result = repository.getTopOrganizations(committeeId, cycle)) {
                        is Result.Success -> {
                            Log.d(TAG, "fetchHistoricalData: Success for cycle $cycle - ${result.data.results.size} organizations")
                            cycle to result.data.results
                        }
                        is Result.Error -> {
                            Log.e(TAG, "fetchHistoricalData: Error for cycle $cycle - ${result.exception.message}", result.exception)
                            cycle to null
                        }
                        // For any error or other state, treat it as a null result.
                        else -> {
                            Log.w(TAG, "fetchHistoricalData: Unknown result for cycle $cycle")
                            cycle to null
                        }
                    }
                }
            }

            // Wait for all the parallel jobs to complete
            val results = deferredResults.awaitAll()
            Log.d(TAG, "fetchHistoricalData: All requests completed. Processing results...")

            // Process results
            val successfulData = results.mapNotNull { (cycle, contributions) ->
                contributions?.let { 
                    cycle to it.sortedByDescending { item -> item.total }
                }
            }.toMap()
            Log.d(TAG, "fetchHistoricalData: Successful cycles: ${successfulData.keys}, total organizations: ${successfulData.values.sumOf { it.size }}")

            // Update UI state
            if (successfulData.isEmpty() && results.any { it.second == null }) {
                Log.e(TAG, "fetchHistoricalData: All requests failed or returned no data")
                errorMessage.value = "Failed to fetch historical data. Please check your connection."
            } else {
                Log.i(TAG, "fetchHistoricalData: Successfully loaded data for ${successfulData.size} cycles")
                historicalOrganizations.value = successfulData
            }

            isLoading.value = false
            
            // Step 3: Fetch Senate Data (LD-203)
            // We need the candidate's name for this. Let's fetch it if we don't have it.
            when (val candidateResult = repository.getCandidateDetails(cid)) {
                is Result.Success -> {
                    val name = candidateResult.data.results.firstOrNull()?.name
                    if (name != null) {
                        fetchSenateData(name)
                    }
                }
                is Result.Error -> Log.e(TAG, "fetchHistoricalData: Failed to get candidate name for Senate search")
                else -> {}
            }
        }
    }

    /**
     * Fetches lobbyist contributions from the Senate API.
     */
    private suspend fun fetchSenateData(name: String) {
        Log.d(TAG, "fetchSenateData: Fetching for $name")
        senateErrorMessage.value = null
        when (val result = repository.getSenateContributions(name)) {
            is Result.Success -> {
                Log.d(TAG, "fetchSenateData: Success, found ${result.data.results.size} reports")
                senateContributions.value = result.data.results
            }
            is Result.Error -> {
                Log.e(TAG, "fetchSenateData: Error: ${result.exception.message}")
                senateErrorMessage.value = "Failed to load lobbyist disclosures: ${result.exception.message}"
            }
            else -> {}
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
