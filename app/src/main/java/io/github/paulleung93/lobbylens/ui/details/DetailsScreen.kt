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
    val industries by remember { viewModel.industries }
    val isLoading by remember { viewModel.isLoading }

    LaunchedEffect(cid) {
        cid?.let { viewModel.fetchTopIndustries(it) }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        if (isLoading) {
            Text("Loading...")
        } else {
            LazyColumn {
                items(industries) { industry ->
                    Text(text = "${industry.attributes.industryName}: ${industry.attributes.total}")
                }
            }
        }
    }
}