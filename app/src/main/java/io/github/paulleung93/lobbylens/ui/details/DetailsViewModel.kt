package io.github.paulleung93.lobbylens.ui.details

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.paulleung93.lobbylens.data.model.FecEmployerContribution
import io.github.paulleung93.lobbylens.data.repository.PoliticianRepository
import io.github.paulleung93.lobbylens.util.Result
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DetailsViewType {
    CAMPAIGN, LOBBYIST
}

enum class LobbyistSortOption {
    DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC
}

enum class CampaignSortOption {
    AMOUNT_DESC, AMOUNT_ASC
}

/**
 * ViewModel for the Details screen.
 * Uses StateFlow and sealed UI state for structured state management.
 */
class DetailsViewModel : ViewModel() {

    private val repository = PoliticianRepository()
    
    companion object {
        private const val TAG = "DetailsViewModel"
    }

    // --- Main UI State --- //
    private val _uiState = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    // --- Filter State (user preferences, separate from main UI state) --- //
    private val _filterState = MutableStateFlow(DetailsFilterState())
    val filterState: StateFlow<DetailsFilterState> = _filterState.asStateFlow()

    // --- Filter Actions --- //
    fun updateViewType(viewType: DetailsViewType) {
        _filterState.update { it.copy(selectedView = viewType) }
    }

    fun selectYear(year: String) {
        _filterState.update { it.copy(selectedYear = year) }
    }

    fun selectLobbyistYear(year: String) {
        _filterState.update { it.copy(lobbyistSelectedYear = year) }
    }

    fun updateCampaignSort(option: CampaignSortOption) {
        _filterState.update { it.copy(campaignSort = option) }
    }

    fun updateLobbyistSort(option: LobbyistSortOption) {
        _filterState.update { it.copy(lobbyistSort = option) }
    }

    fun updateCampaignSearchQuery(query: String) {
        _filterState.update { it.copy(campaignSearchQuery = query) }
    }

    fun updateLobbyistSearchQuery(query: String) {
        _filterState.update { it.copy(lobbyistSearchQuery = query) }
    }

    /**
     * Fetches historical data concurrently and handles partial failures gracefully.
     * @param cid The campaign ID of the politician.
     */
    fun fetchHistoricalData(cid: String) {
        viewModelScope.launch {
            _uiState.value = DetailsUiState.Loading

            // Step 1: Fetch Candidate Committee History to get a valid Principal Committee ID
            var committeeId: String? = null
            var candidateName: String? = null
            var candidateObj: io.github.paulleung93.lobbylens.data.model.FecCandidate? = null

            when (val historyResult = repository.getCandidateCommitteeHistory(cid)) {
                is Result.Success -> {
                    val historySorted = historyResult.data.results.sortedByDescending { it.cycle }
                    val principalCommittee = historySorted.firstOrNull { it.designation == "P" }

                    if (principalCommittee != null) {
                        committeeId = principalCommittee.committeeId
                    } else {
                        Log.w(TAG, "fetchHistoricalData: No principal committee found for candidate $cid")
                        _uiState.value = DetailsUiState.Error("No principal campaign committee found.")
                        return@launch
                    }
                }
                is Result.Error -> {
                    Log.e(TAG, "fetchHistoricalData: Failed to fetch committee history: ${historyResult.exception.message}")
                    _uiState.value = DetailsUiState.Error("Failed to load committee history.")
                    return@launch
                }
                else -> {
                    Log.d(TAG, "fetchHistoricalData: Candidate history is loading")
                    return@launch
                }
            }

            // Get candidate details for name
            when (val candidateResult = repository.getCandidateDetails(cid)) {
                is Result.Success -> {
                    candidateObj = candidateResult.data.results.firstOrNull()
                    candidateName = candidateObj?.name
                }
                is Result.Error -> Log.e(TAG, "fetchHistoricalData: Failed to get candidate name")
                else -> {}
            }

            if (candidateObj == null) {
                _uiState.value = DetailsUiState.Error("Could not load candidate details.")
                return@launch
            }

            // Step 2: Fetch Contribution Data using the resolved Committee ID
            val cycles = listOf("2024", "2022", "2020")
            Log.d(TAG, "fetchHistoricalData: Fetching data for committeeId=$committeeId, cycles=$cycles")

            val deferredResults = cycles.map { cycle ->
                async {
                    Log.d(TAG, "fetchHistoricalData: Launching fetch for cycle $cycle")
                    when (val result = repository.getTopOrganizations(committeeId, cycle)) {
                        is Result.Success -> {
                            Log.d(TAG, "fetchHistoricalData: Success for cycle $cycle - ${result.data.results.size} organizations")
                            cycle to result.data.results
                        }
                        is Result.Error -> {
                            Log.e(TAG, "fetchHistoricalData: Error for cycle $cycle - ${result.exception.message}")
                            cycle to null
                        }
                        else -> cycle to null
                    }
                }
            }

            val results = deferredResults.awaitAll()
            Log.d(TAG, "fetchHistoricalData: All requests completed. Processing results...")

            val successfulData = results.mapNotNull { (cycle, contributions) ->
                contributions?.let { 
                    cycle to it.sortedByDescending { item -> item.total }
                }
            }.toMap()
            Log.d(TAG, "fetchHistoricalData: Successful cycles: ${successfulData.keys}")

            if (successfulData.isEmpty() && results.any { it.second == null }) {
                Log.e(TAG, "fetchHistoricalData: All requests failed or returned no data")
                _uiState.value = DetailsUiState.Error("Failed to fetch historical data. Please check your connection.")
                return@launch
            }

            // Initial success state (without Senate data yet)
            _uiState.value = DetailsUiState.Success(
                candidate = candidateObj,
                committeeId = committeeId,
                historicalOrganizations = successfulData,
                senateContributions = emptyList(),
                isSenateLoading = true
            )

            // Step 3: Fetch Senate Data (LD-203) if we have a name
            if (candidateName != null) {
                fetchSenateData(candidateName)
            }
        }
    }

    /**
     * Fetches lobbyist contributions from the Senate API.
     */
    private suspend fun fetchSenateData(name: String) {
        Log.d(TAG, "fetchSenateData: Fetching for $name")
        
        when (val result = repository.getSenateContributions(name)) {
            is Result.Success -> {
                Log.d(TAG, "fetchSenateData: Success, found ${result.data.results.size} reports")
                val currentState = _uiState.value
                if (currentState is DetailsUiState.Success) {
                    _uiState.value = currentState.copy(
                        senateContributions = result.data.results,
                        isSenateLoading = false
                    )
                }
            }
            is Result.Error -> {
                Log.e(TAG, "fetchSenateData: Error: ${result.exception.message}")
                val currentState = _uiState.value
                if (currentState is DetailsUiState.Success) {
                    _uiState.value = currentState.copy(
                        isSenateLoading = false,
                        senateError = "Failed to load lobbyist disclosures: ${result.exception.message}"
                    )
                }
            }
            else -> {}
        }
    }

    /**
     * Returns filtered organizations based on current filter state.
     */
    fun getFilteredOrganizations(historicalOrganizations: Map<String, List<FecEmployerContribution>>): List<FecEmployerContribution> {
        val filter = _filterState.value
        val selectedYear = filter.selectedYear
        
        if (selectedYear == "All") {
            return emptyList() // UI handles "All" view manually
        }
        
        var data = historicalOrganizations[selectedYear] ?: emptyList()
        
        // Apply Search
        if (filter.campaignSearchQuery.isNotEmpty()) {
            val query = filter.campaignSearchQuery.lowercase()
            data = data.filter { it.employer.lowercase().contains(query) }
        }

        // Apply Sort
        return when (filter.campaignSort) {
            CampaignSortOption.AMOUNT_DESC -> data.sortedByDescending { it.total }
            CampaignSortOption.AMOUNT_ASC -> data.sortedBy { it.total }
        }
    }
}
