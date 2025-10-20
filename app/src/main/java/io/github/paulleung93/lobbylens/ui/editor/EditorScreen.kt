package io.github.paulleung93.lobbylens.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun EditorScreen(navController: NavController, viewModel: EditorViewModel = viewModel()) {
    var text by remember { mutableStateOf("") }
    val legislators by remember { viewModel.legislators }
    val isLoading by remember { viewModel.isLoading }

    Column(modifier = Modifier.padding(16.dp)) {
        TextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Enter Politician Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { viewModel.searchLegislators(text) },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Search")
        }

        if (isLoading) {
            Text("Loading...")
        }

        LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
            items(legislators) { legislator ->
                Text(
                    text = legislator.attributes.firstLast,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("details/${legislator.attributes.cid}") }
                        .padding(8.dp)
                )
            }
        }
    }
}