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
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun DetailsScreen(navController: NavController, cid: String?, viewModel: DetailsViewModel = viewModel()) {
    // Observe state
    val historicalOrganizations by remember { viewModel.historicalOrganizations }
    val isLoading by remember { viewModel.isLoading }
    val errorMessage by remember { viewModel.errorMessage }
    val selectedYear by remember { viewModel.selectedYear }

    // Fetch data
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
                // Chart Data (Top 5 Contributors)
                val chartData = remember(historicalOrganizations, selectedYear) {
                    if (selectedYear == "All") {
                        // Aggregate all years to find top contributors of all time
                        historicalOrganizations.values.flatten()
                            .groupBy { it.employer }
                            .mapValues { (_, contributions) ->
                                contributions.sumOf { it.total }.toFloat()
                            }
                            .toList()
                            .sortedByDescending { it.second }
                            .take(5)
                    } else {
                        // Top contributors for the selected year
                        historicalOrganizations[selectedYear] // This list is already sorted by VM, but sort again to be safe/explicit or if logic changes
                            ?.sortedByDescending { it.total }
                            ?.take(5)
                            ?.map { it.employer to it.total.toFloat() }
                            ?: emptyList()
                    }
                }

                // Header & Filter Chips
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    // "All" Chip
                    androidx.compose.material3.FilterChip(
                        selected = selectedYear == "All",
                        onClick = { viewModel.selectYear("All") },
                        label = { Text("All Years") }
                    )
                    // Dynamic Chips for available years, sorted
                    historicalOrganizations.keys.sortedDescending().forEach { year ->
                        androidx.compose.material3.FilterChip(
                            selected = selectedYear == year,
                            onClick = { viewModel.selectYear(year) },
                            label = { Text(year) }
                        )
                    }
                }

                if (chartData.isNotEmpty()) {
                     Text(
                        text = if (selectedYear == "All") "Top Contributors (All Time)" else "Top Contributors ($selectedYear)",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    BarChart(
                        data = chartData,
                        valueFormatter = { value ->
                             NumberFormat.getCurrencyInstance().apply {
                                 maximumFractionDigits = 0
                             }.format(value)
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // List of Contributors
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (selectedYear == "All") {
                        // Show all sections
                        historicalOrganizations.forEach { (cycle, organizations) ->
                             item {
                                Text(
                                    text = "Top Contributors ($cycle)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                )
                            }
                            items(organizations) { organization ->
                                ContributorItem(organization)
                            }
                        }
                    } else {
                        // Show only selected year
                        val organizations = viewModel.filteredOrganizations
                        if (organizations.isNotEmpty()) {
                            // Header removed as per user request when filtering by specific year
                            items(organizations) { organization ->
                                ContributorItem(organization)
                            }
                        } else {
                            item {
                                Text("No data for this year.", modifier = Modifier.padding(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContributorItem(organization: io.github.paulleung93.lobbylens.data.model.FecEmployerContribution) {
    val formattedTotal = remember(organization.total) {
        NumberFormat.getCurrencyInstance().format(organization.total)
    }
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant // slightly distinct background
        )
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            Text(
                text = organization.employer,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = formattedTotal,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface, // Improved contrast
                fontWeight = FontWeight.Bold
            )
        }
    }
}
