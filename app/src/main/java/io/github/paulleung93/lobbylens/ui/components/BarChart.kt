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
import java.util.SortedMap

/**
 * A simple bar chart composable to visualize data.
 *
 * @param data A map of labels to values to be represented as bars.
 */
@Composable
fun BarChart(
    data: Map<String, Float>,
    valueFormatter: (Float) -> String = { "%.0f".format(it) }
) {
    val maxValue = data.values.maxOrNull() ?: 0f
    val barColor = MaterialTheme.colorScheme.secondary
    val labelColor = MaterialTheme.colorScheme.onSurface
    val valueLabelColor = MaterialTheme.colorScheme.onSurfaceVariant

    val sortedData = data.toSortedMap()

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
            sortedData.forEach { (label, value) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val heightFraction = if (maxValue > 0) value / maxValue else 0f
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Value Label
                        Text(
                            text = valueFormatter(value),
                            style = MaterialTheme.typography.labelSmall,
                            color = valueLabelColor,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        // Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
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
            sortedData.forEach { (label, _) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = labelColor
                )
            }
        }
    }
}
