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
import java.text.NumberFormat
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Surface


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

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (isLoading) {
                 Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                     androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                 }
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "An unknown error occurred.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                // Create data for the chart by summing totals for each cycle
                val chartData = remember(historicalOrganizations) {
                    historicalOrganizations.mapValues { (_, organizations) ->
                        // CORRECTED: Use the new FecEmployerContribution model
                        organizations.sumOf { it.total }.toFloat()
                    }.filter { it.value > 0f } // Filter out cycles with no data
                }

                if (chartData.isNotEmpty()) {
                    Text(
                        text = "Total Contributions by Cycle",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    // Ensure BarChart handles colors or wrap it properly. 
                    // Assuming BarChart uses simple canvas drawing.
                    BarChart(data = chartData)
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Display the detailed list below the chart
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    historicalOrganizations.forEach { (cycle, organizations) ->
                        item {
                            Text(
                                text = "Top Contributors ($cycle)",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.secondary, // Gold accent for headers
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                        }
                        items(organizations) { organization ->
                            val formattedTotal = remember(organization.total) {
                                NumberFormat.getCurrencyInstance().format(organization.total)
                            }
                            androidx.compose.material3.Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = androidx.compose.material3.CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                androidx.compose.foundation.layout.Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = organization.employer,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = formattedTotal,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
