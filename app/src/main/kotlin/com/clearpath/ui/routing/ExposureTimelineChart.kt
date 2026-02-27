package com.clearpath.ui.routing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clearpath.data.route.CameraEncounter
import com.clearpath.data.route.SavedRoute
import com.clearpath.ui.theme.Amber
import com.clearpath.ui.theme.ClearPathTypography
import com.clearpath.ui.theme.OnSurface
import com.clearpath.ui.theme.OnSurfaceMuted
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.component.text.textComponent
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf

/**
 * Bar chart showing surveillance intensity along a route.
 * X axis = distance metres, Y axis = seconds under coverage at each encounter.
 */
@Composable
fun ExposureTimelineChart(
    route: SavedRoute,
    onSpikeTap: (CameraEncounter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val encounters = route.cameraEncounters
    val totalDist  = route.distanceMetres

    val entries = remember(encounters) {
        encounters.map { enc ->
            FloatEntry(
                x = enc.atRoutePosition * totalDist,
                y = (enc.estimatedExposureSeconds.toFloat() / 60f).coerceAtLeast(0.1f),
            )
        }
    }

    val model = remember(entries) { entryModelOf(entries) }

    Column(modifier = modifier) {
        Text("Exposure Timeline", style = ClearPathTypography.titleMedium, color = OnSurface)
        Text(
            "Bars show seconds under surveillance at each point",
            style = ClearPathTypography.labelSmall,
            color = OnSurfaceMuted,
        )
        Spacer(Modifier.height(8.dp))

        if (entries.isEmpty()) {
            Text(
                "No camera encounters on this route.",
                style = ClearPathTypography.bodyMedium,
                color = OnSurfaceMuted,
            )
        } else {
            Chart(
                chart = columnChart(
                    columns = listOf(
                        LineComponent(
                            color        = android.graphics.Color.parseColor("#EF4444"),
                            thicknessDp  = 8f,
                            shape        = Shapes.roundedCornerShape(
                                topLeftPercent  = 30,
                                topRightPercent = 30,
                            ),
                        )
                    ),
                ),
                model      = model,
                startAxis  = rememberStartAxis(
                    label = textComponent {
                        color      = android.graphics.Color.parseColor("#8B949E")
                        textSizeSp = 10f
                    }
                ),
                bottomAxis = rememberBottomAxis(
                    label = textComponent {
                        color      = android.graphics.Color.parseColor("#8B949E")
                        textSizeSp = 10f
                    },
                    valueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
                        if (value >= 1000) "%.1fkm".format(value / 1000) else "${value.toInt()}m"
                    },
                ),
                modifier   = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
            )
        }

        Spacer(Modifier.height(4.dp))
        Text(
            "Total in coverage: ${encounters.sumOf { it.estimatedExposureSeconds }}s",
            style = ClearPathTypography.labelSmall,
            color = Amber,
        )
    }
}
