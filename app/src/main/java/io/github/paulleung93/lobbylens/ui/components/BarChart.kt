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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
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
    val highlightColor = MaterialTheme.colorScheme.primary

    // State for interactive tooltip
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { offset ->
                                val barWidth = size.width / data.size
                                val index = (offset.x / barWidth).toInt().coerceIn(0, data.lastIndex)
                                selectedIndex = index
                                tryAwaitRelease()
                                selectedIndex = null
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val barWidth = size.width / data.size
                                val index = (offset.x / barWidth).toInt().coerceIn(0, data.lastIndex)
                                selectedIndex = index
                            },
                            onDrag = { change, _ ->
                                val barWidth = size.width / data.size
                                val index = (change.position.x / barWidth).toInt().coerceIn(0, data.lastIndex)
                                selectedIndex = index
                            },
                            onDragEnd = {
                                selectedIndex = null
                            },
                            onDragCancel = {
                                selectedIndex = null
                            }
                        )
                    },
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
            ) {
                data.forEachIndexed { index, (label, value) ->
                    val isSelected = index == selectedIndex
                    
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
                                color = if (isSelected) highlightColor else valueLabelColor,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
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
                                        color = if (isSelected) highlightColor else barColor,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                                            topStart = 4.dp,
                                            topEnd = 4.dp
                                        )
                                    )
                            )
                        }
                    }
                }
            }
            
            // Tooltip Overlay
            selectedIndex?.let { index ->
                val item = data.getOrNull(index)
                if (item != null) {
                    androidx.compose.material3.Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shadowElevation = 4.dp,
                        tonalElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = item.first,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = valueFormatter(item.second),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
        ) {
            data.forEachIndexed { index, (label, _) ->
                val isSelected = index == selectedIndex
                // Truncate long labels
                val displayLabel = if (label.length > 10) label.take(8) + ".." else label

                Text(
                    text = displayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) highlightColor else labelColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Visible, // Allow some overflow if needed, or Clip
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
