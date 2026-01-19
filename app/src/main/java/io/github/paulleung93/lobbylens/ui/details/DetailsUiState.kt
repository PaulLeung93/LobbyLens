package io.github.paulleung93.lobbylens.ui.details

import io.github.paulleung93.lobbylens.data.model.FecCandidate
import io.github.paulleung93.lobbylens.data.model.FecEmployerContribution
import io.github.paulleung93.lobbylens.data.model.SenateContributionReport

/**
 * Sealed interface representing the various UI states of the Details screen.
 */
sealed interface DetailsUiState {
    /**
     * Loading state while fetching data.
     */
    data object Loading : DetailsUiState

    /**
     * Successful data load.
     */
    data class Success(
        val candidate: FecCandidate,
        val committeeId: String?,
        val historicalOrganizations: Map<String, List<FecEmployerContribution>>,
        val senateContributions: List<SenateContributionReport>,
        val isSenateLoading: Boolean = false,
        val senateError: String? = null
    ) : DetailsUiState

    /**
     * Error state with message.
     */
    data class Error(val message: String) : DetailsUiState
}

/**
 * UI state for filter and sort preferences (kept separate as user preferences).
 */
data class DetailsFilterState(
    val selectedYear: String = "All",
    val lobbyistSelectedYear: String = "All",
    val campaignSort: CampaignSortOption = CampaignSortOption.AMOUNT_DESC,
    val lobbyistSort: LobbyistSortOption = LobbyistSortOption.DATE_DESC,
    val campaignSearchQuery: String = "",
    val lobbyistSearchQuery: String = "",
    val selectedView: DetailsViewType = DetailsViewType.LOBBYIST
)
