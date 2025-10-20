package io.github.paulleung93.lobbylens.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A simple bar chart composable to visualize data.
 *
 * @param data A map of labels to values to be represented as bars.
 */
@Composable
fun BarChart(data: Map<String, Float>) {
    val maxValue = data.values.maxOrNull() ?: 0f

    Column(modifier = Modifier.padding(16.dp)) {
        data.forEach { (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = label, modifier = Modifier.weight(1f))
                Canvas(modifier = Modifier
                    .weight(3f)
                    .height(20.dp)) {
                    val barWidth = (value / maxValue) * size.width
                    drawRect(
                        color = MaterialTheme.colorScheme.primary,
                        topLeft = Offset(x = 0f, y = 0f),
                        size = Size(width = barWidth, height = size.height)
                    )
                }
            }
        }
    }
}
