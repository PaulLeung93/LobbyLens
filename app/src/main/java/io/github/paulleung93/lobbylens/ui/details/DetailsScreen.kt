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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import io.github.paulleung93.lobbylens.ui.components.BarChart
import java.text.NumberFormat
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Surface
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew


@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun DetailsScreen(navController: NavController, cid: String?, viewModel: DetailsViewModel = viewModel()) {
    // Observe state
    val historicalOrganizations by remember { viewModel.historicalOrganizations }
    val senateContributions by remember { viewModel.senateContributions }
    val isLoading by remember { viewModel.isLoading }
    val isSenateLoading by remember { viewModel.isSenateLoading }
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
                // View Selector
                androidx.compose.material3.TabRow(
                    selectedTabIndex = if (viewModel.selectedView.value == DetailsViewType.LOBBYIST) 0 else 1,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp),
                    indicator = { tabPositions ->
                        androidx.compose.material3.TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[if (viewModel.selectedView.value == DetailsViewType.LOBBYIST) 0 else 1]),
                            color = MaterialTheme.colorScheme.primary,
                            height = 3.dp
                        )
                    }
                ) {
                    androidx.compose.material3.Tab(
                        selected = viewModel.selectedView.value == DetailsViewType.LOBBYIST,
                        onClick = { viewModel.updateViewType(DetailsViewType.LOBBYIST) },
                        text = { 
                            Text(
                                "Lobbyist Disclosures",
                                fontWeight = if (viewModel.selectedView.value == DetailsViewType.LOBBYIST) FontWeight.Bold else FontWeight.Normal,
                                color = if (viewModel.selectedView.value == DetailsViewType.LOBBYIST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            ) 
                        }
                    )
                    androidx.compose.material3.Tab(
                        selected = viewModel.selectedView.value == DetailsViewType.CAMPAIGN,
                        onClick = { viewModel.updateViewType(DetailsViewType.CAMPAIGN) },
                        text = { 
                            Text(
                                "Campaign Contributions",
                                fontWeight = if (viewModel.selectedView.value == DetailsViewType.CAMPAIGN) FontWeight.Bold else FontWeight.Normal,
                                color = if (viewModel.selectedView.value == DetailsViewType.CAMPAIGN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            ) 
                        }
                    )
                }

                if (viewModel.selectedView.value == DetailsViewType.CAMPAIGN) {
                    // --- CAMPAIGN VIEW ---
                    
                    // Chart Data (Top 5 Contributors)
                    val chartData = remember(historicalOrganizations, selectedYear) {
                        if (selectedYear == "All") {
                            historicalOrganizations.values.flatten()
                                .groupBy { it.employer }
                                .mapValues { (_, contributions) ->
                                    contributions.sumOf { it.total }.toFloat()
                                }
                                .toList()
                                .sortedByDescending { it.second }
                                .take(5)
                        } else {
                            historicalOrganizations[selectedYear]
                                ?.sortedByDescending { it.total }
                                ?.take(5)
                                ?.map { it.employer to it.total.toFloat() }
                                ?: emptyList()
                        }
                    }

                    // Header & Filter Chips
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.FilterChip(
                            selected = selectedYear == "All",
                            onClick = { viewModel.selectYear("All") },
                            label = { Text("All Years") },
                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                        historicalOrganizations.keys.sortedDescending().forEach { year ->
                            androidx.compose.material3.FilterChip(
                                selected = selectedYear == year,
                                onClick = { viewModel.selectYear(year) },
                                label = { Text(year) },
                                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
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

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if (selectedYear == "All") {
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
                            val organizations = viewModel.filteredOrganizations
                            if (organizations.isNotEmpty()) {
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
                } else {
                    // --- LOBBYIST VIEW ---
                    val flattenedContributions = remember(senateContributions, viewModel.candidateName.value) {
                        val name = viewModel.candidateName.value
                        val lastName = name?.split(",")?.firstOrNull()?.trim()?.lowercase() ?: ""
                        
                        senateContributions.flatMap { report ->
                            report.contributionItems?.map { contribution ->
                                report to contribution
                            } ?: emptyList()
                        }.filter { (_, contribution) ->
                            // Filter logic: The honoree name should contain the last name of our candidate
                            // or the candidate name should contain the honoree name's parts.
                            // This prevents showing other people from the same report.
                            val honoree = contribution.honoreeName.lowercase()
                            if (lastName.isEmpty()) true 
                            else honoree.contains(lastName)
                        }.sortedByDescending { it.second.date }
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Text(
                                text = "Lobbyist Disclosures (LD-203)",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                            )
                            Text(
                                text = "Non-campaign contributions to charities, events, or inaugural committees by registered lobbying firms.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                        
                        if (isSenateLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = androidx.compose.ui.Alignment.Center
                                ) {
                                    androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        } else if (viewModel.senateErrorMessage.value != null) {
                            item {
                                Text(
                                    text = viewModel.senateErrorMessage.value!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        } else if (flattenedContributions.isEmpty()) {
                            item {
                                Text(
                                    text = "No lobbyist disclosures found for this politician.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            }
                        } else {
                            items(flattenedContributions) { (report, contribution) ->
                                LobbyistContributionCard(report, contribution)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LobbyistContributionCard(
    report: io.github.paulleung93.lobbylens.data.model.SenateContributionReport,
    contribution: io.github.paulleung93.lobbylens.data.model.SenateContribution
) {
    val formattedAmount = remember(contribution.amount) {
        try {
            val amountDouble = contribution.amount.toDouble()
            NumberFormat.getCurrencyInstance().format(amountDouble)
        } catch (e: Exception) {
            contribution.amount // Fallback to raw string if parsing fails
        }
    }

    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                Text(
                    text = contribution.payeeName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formattedAmount,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "From: ${report.registrant.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Type: ${contribution.type.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Text(
                    text = contribution.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val context = androidx.compose.ui.platform.LocalContext.current
            androidx.compose.material3.OutlinedButton(
                onClick = {
                    val url = "https://lda.senate.gov/filings/public/contribution/${report.filingUuid}/print/"
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verify with Official Senate Report", style = MaterialTheme.typography.labelMedium)
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
