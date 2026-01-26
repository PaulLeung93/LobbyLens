package io.github.paulleung93.lobbylens.ui.details
 
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavController
import io.github.paulleung93.lobbylens.data.model.FecCandidate
import io.github.paulleung93.lobbylens.data.model.FecEmployerContribution
import io.github.paulleung93.lobbylens.data.model.SenateContribution
import io.github.paulleung93.lobbylens.data.model.SenateContributionReport
import io.github.paulleung93.lobbylens.data.model.SenateRegistrant
import io.github.paulleung93.lobbylens.ui.theme.LobbyLensTheme
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
 
/**
 * UI tests for DetailsScreen using direct Composable testing.
 * Verifies that the screen correctly honors the DetailsUiState.
 */
class DetailsScreenTest {
 
    @get:Rule
    val composeTestRule = createComposeRule()
 
    private lateinit var mockNavController: NavController
    private lateinit var mockViewModel: DetailsViewModel
 
    private val uiStateFlow = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    private val filterStateFlow = MutableStateFlow(DetailsFilterState())
 
    // Test Data
    private val testCandidate = FecCandidate(
        candidateId = "H8CA05035",
        name = "PELOSI, NANCY",
        officeSought = "H",
        state = "CA",
        party = "DEM"
    )
    
    private val testContribution = FecEmployerContribution(
        employer = "GOOGLE LLC",
        total = 50000.0,
        count = 25,
        type = "Employer"
    )

    private val testSenateContribution = SenateContribution(
        type = "FEA",
        contributorName = "Self",
        payeeName = "Charity X",
        honoreeName = "Nancy Pelosi",
        amount = "1000.00",
        date = "2024-01-01"
    )

    private val testSenateReport = SenateContributionReport(
        filingUuid = "123",
        filingYear = 2024,
        filingPeriod = "Q1",
        registrant = SenateRegistrant("Test Corp", 1),
        contributionItems = listOf(testSenateContribution)
    )
 
    @Before
    fun setUp() {
        mockNavController = mockk(relaxed = true)
        mockViewModel = mockk(relaxed = true)
        
        every { mockViewModel.uiState } returns uiStateFlow
        every { mockViewModel.filterState } returns filterStateFlow
        // Mock getFilteredOrganizations which is used in the screen
        every { mockViewModel.getFilteredOrganizations(any()) } returns listOf(testContribution)
    }
 
    @Test
    fun detailsScreen_loadingState_showsCircularProgressIndicator() {
        // Arrange
        uiStateFlow.value = DetailsUiState.Loading
 
        // Act
        composeTestRule.setContent {
            LobbyLensTheme {
                DetailsScreen(navController = mockNavController, cid = "H8CA05035", viewModel = mockViewModel)
            }
        }
 
        // Assert - The progress indicator should be shown
        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
    }
 
    @Test
    fun detailsScreen_errorState_showsErrorMessage() {
        // Arrange
        val errorMsg = "Failed to load data"
        uiStateFlow.value = DetailsUiState.Error(errorMsg)
 
        // Act
        composeTestRule.setContent {
            LobbyLensTheme {
                DetailsScreen(navController = mockNavController, cid = "H8CA05035", viewModel = mockViewModel)
            }
        }
 
        // Assert
        composeTestRule.onNodeWithText(errorMsg).assertIsDisplayed()
    }
 
    @Test
    fun detailsScreen_successState_showsCandidateNameAndTabs() {
        // Arrange
        uiStateFlow.value = DetailsUiState.Success(
            candidate = testCandidate,
            committeeId = "C001",
            historicalOrganizations = mapOf("2024" to listOf(testContribution)),
            senateContributions = listOf(testSenateReport),
            isSenateLoading = false
        )
        // Default view logic in screen might vary, but we can search for the tab text
        filterStateFlow.value = DetailsFilterState(selectedView = DetailsViewType.LOBBYIST)
 
        // Act
        composeTestRule.setContent {
            LobbyLensTheme {
                DetailsScreen(navController = mockNavController, cid = "H8CA05035", viewModel = mockViewModel)
            }
        }
 
        // Assert
        // The screen normalizes PELOSI, NANCY to "Nancy Pelosi"
        composeTestRule.onNodeWithText("Nancy Pelosi (DEM-CA)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lobbyist Disclosures").assertIsDisplayed()
        composeTestRule.onNodeWithText("Campaign Contributions").assertIsDisplayed()
        
        // Verify Senate data is shown (Look for the registrant name)
        composeTestRule.onNodeWithText("Lobbyist Disclosures (LD-203)").assertIsDisplayed()
        composeTestRule.onNodeWithText("From: Test Corp").assertIsDisplayed()
    }

    @Test
    fun detailsScreen_campaignTab_showsContributions() {
        // Arrange
        uiStateFlow.value = DetailsUiState.Success(
            candidate = testCandidate,
            committeeId = "C001",
            historicalOrganizations = mapOf("2024" to listOf(testContribution)),
            senateContributions = emptyList(),
            isSenateLoading = false
        )
        // Switch to Campaign view
        filterStateFlow.value = DetailsFilterState(
            selectedView = DetailsViewType.CAMPAIGN,
            selectedYear = "2024"
        )
 
        // Act
        composeTestRule.setContent {
            LobbyLensTheme {
                DetailsScreen(navController = mockNavController, cid = "H8CA05035", viewModel = mockViewModel)
            }
        }
 
        // Assert
        // Use testTag to disambiguate from Chart labels
        composeTestRule.onNode(
            hasTestTag("ContributorCard") and hasAnyDescendant(hasText("GOOGLE LLC"))
        ).assertIsDisplayed()
        
        // Format check depends on locale, but $50,000.00 is common for US
        composeTestRule.onNodeWithText("$50,000.00", substring = true).assertIsDisplayed()
    }
}
