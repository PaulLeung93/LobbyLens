package io.github.paulleung93.lobbylens.ui.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun DetailsScreen(navController: NavController, cid: String?, viewModel: DetailsViewModel = viewModel()) {
    // Observe the organizations state from the ViewModel
    val organizations by remember { viewModel.organizations }
    val isLoading by remember { viewModel.isLoading }
    val errorMessage by remember { viewModel.errorMessage }

    // Fetch data when the screen is first composed or when the CID changes
    LaunchedEffect(cid) {
        cid?.let { viewModel.fetchTopOrganizations(it) }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        if (isLoading) {
            Text("Loading...")
        } else if (errorMessage != null) {
            Text(text = errorMessage ?: "An unknown error occurred.")
        } else {
            LazyColumn {
                // Display the list of organizations
                items(organizations) { organization ->
                    // Display organization name and total contribution
                    Text(text = "${organization.attributes.orgName}: ${organization.attributes.total}")
                }
            }
        }
    }
}
