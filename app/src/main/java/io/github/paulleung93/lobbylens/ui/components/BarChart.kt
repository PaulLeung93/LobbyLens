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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

/**
 * A simple bar chart composable to visualize data.
 *
 * @param data A list of pairs (Label -> Value) to be represented as bars. Order is preserved.
 */
@Composable
fun BarChart(
    data: List<Pair<String, Float>>,
    valueFormatter: (Float) -> String = { "%.0f".format(it) }
) {
    // If no data, just return
    if (data.isEmpty()) return

    val maxValue = data.maxOfOrNull { it.second } ?: 0f
    val barColor = MaterialTheme.colorScheme.secondary
    val labelColor = MaterialTheme.colorScheme.onSurface
    val valueLabelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp), 
            verticalAlignment = Alignment.Bottom, 
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
        ) {
            data.forEach { (label, value) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val heightFraction = if (maxValue > 0) value / maxValue else 0f
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Value Label
                        Text(
                            text = valueFormatter(value),
                            style = MaterialTheme.typography.labelSmall,
                            color = valueLabelColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        // Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f) // Make bars slightly thinner
                                .fillMaxHeight(heightFraction)
                                .background(
                                    color = barColor,
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                )
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Axis Line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        
        // Labels row (X-Axis)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
        ) {
            data.forEach { (label, _) ->
                // Truncate long labels
                val displayLabel = if (label.length > 10) label.take(8) + ".." else label
                
                Text(
                    text = displayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Visible, // Allow some overflow if needed, or Clip
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
