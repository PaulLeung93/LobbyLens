package io.github.paulleung93.lobbylens.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.paulleung93.lobbylens.ui.theme.LobbyLensTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for HomeScreen.
 * Verifies the main UI elements are displayed correctly.
 */
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeScreen_displaysMainActionButtons() {
        // Arrange & Act
        composeTestRule.setContent {
            LobbyLensTheme {
                val navController = rememberNavController()
                HomeScreen(navController = navController)
            }
        }

        // Assert - Main buttons are visible
        composeTestRule.onNodeWithText("SCAN CANDIDATE").assertIsDisplayed()
        composeTestRule.onNodeWithText("UPLOAD PHOTO").assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysSearchBar() {
        // Arrange & Act
        composeTestRule.setContent {
            LobbyLensTheme {
                val navController = rememberNavController()
                HomeScreen(navController = navController)
            }
        }

        // Assert - Search bar placeholder text is visible
        composeTestRule.onNodeWithText("Search database manually...").assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysAppBranding() {
        // Arrange & Act
        composeTestRule.setContent {
            LobbyLensTheme {
                val navController = rememberNavController()
                HomeScreen(navController = navController)
            }
        }

        // Assert - App name and tagline are visible
        composeTestRule.onNodeWithText("LOBBYLENS").assertIsDisplayed()
        composeTestRule.onNodeWithText("Transparency in your pocket.").assertIsDisplayed()
    }
}
