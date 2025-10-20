package io.github.paulleung93.lobbylens.ui.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.paulleung93.lobbylens.ui.components.BarChart

@Composable
fun DetailsScreen(navController: NavController, cid: String?, viewModel: DetailsViewModel = viewModel()) {
    // Observe the historicalOrganizations state from the ViewModel
    val historicalOrganizations by remember { viewModel.historicalOrganizations }
    val isLoading by remember { viewModel.isLoading }
    val errorMessage by remember { viewModel.errorMessage }

    // Fetch data when the screen is first composed or when the CID changes
    LaunchedEffect(cid) {
        cid?.let { viewModel.fetchHistoricalData(it) }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        if (isLoading) {
            Text("Loading...")
        } else if (errorMessage != null) {
            Text(text = errorMessage ?: "An unknown error occurred.")
        } else {
            // Create data for the chart by summing totals for each cycle
            val chartData = remember(historicalOrganizations) {
                historicalOrganizations.mapValues { (_, organizations) ->
                    organizations.sumOf { it.attributes.total.toDoubleOrNull() ?: 0.0 }.toFloat()
                }.filter { it.value > 0f } // Filter out cycles with no data
            }

            if (chartData.isNotEmpty()) {
                Text(
                    text = "Total Contributions by Cycle",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                BarChart(data = chartData)
            }

            // Display the detailed list below the chart
            LazyColumn {
                historicalOrganizations.forEach { (cycle, organizations) ->
                    item {
                        Text(
                            text = "Top Contributors for $cycle",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(organizations) { organization ->
                        // Display organization name and total contribution
                        Text(text = "${organization.attributes.orgName}: ${organization.attributes.total}")
                    }
                }
            }
        }
    }
}
