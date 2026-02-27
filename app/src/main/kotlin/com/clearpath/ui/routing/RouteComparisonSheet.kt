package com.clearpath.ui.routing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.clearpath.data.camera.CameraType
import com.clearpath.data.route.RoutingMode
import com.clearpath.data.route.SavedRoute
import com.clearpath.routing.RouteAlternative
import com.clearpath.routing.RouteOptimisation
import com.clearpath.ui.theme.Amber
import com.clearpath.ui.theme.Background
import com.clearpath.ui.theme.ClearPathTypography
import com.clearpath.ui.theme.ExposureHigh
import com.clearpath.ui.theme.ExposureLow
import com.clearpath.ui.theme.ExposureMed
import com.clearpath.ui.theme.OnSurface
import com.clearpath.ui.theme.OnSurfaceMuted
import com.clearpath.ui.theme.Surface
import com.clearpath.ui.theme.SurfaceVariant
import com.clearpath.util.formatDistance
import com.clearpath.util.formatDuration
import com.clearpath.util.formatExposureScore

@Composable
fun RouteComparisonSheet(
    alternatives: List<RouteAlternative>,
    selectedRoute: SavedRoute?,
    routingMode: RoutingMode,
    optimisation: RouteOptimisation,
    onSelectRoute: (SavedRoute) -> Unit,
    onSaveRoute: () -> Unit,
    onStartNav: () -> Unit,
    onChangeOptimisation: (RouteOptimisation) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text("Route Options", style = ClearPathTypography.headlineMedium, color = OnSurface)
        Spacer(Modifier.height(4.dp))
        Text(
            "Mode: ${routingMode.name.lowercase().replaceFirstChar { it.uppercase() }}",
            style = ClearPathTypography.labelSmall,
            color = OnSurfaceMuted,
        )

        Spacer(Modifier.height(12.dp))

        // Optimisation toggle
        OptimisationToggle(
            current = optimisation,
            onChange = onChangeOptimisation,
        )

        Spacer(Modifier.height(12.dp))

        // Route cards
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(alternatives) { alt ->
                RouteCard(
                    alternative   = alt,
                    isSelected    = alt.route.id == selectedRoute?.id,
                    onSelect      = { onSelectRoute(alt.route) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onSaveRoute,
                colors  = ButtonDefaults.buttonColors(containerColor = SurfaceVariant),
                modifier = Modifier.weight(1f),
            ) {
                Text("Save route", color = OnSurface)
            }
            Button(
                onClick = onStartNav,
                colors  = ButtonDefaults.buttonColors(containerColor = Amber),
                modifier = Modifier.weight(1f),
            ) {
                Text("Navigate", color = Background)
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun RouteCard(
    alternative: RouteAlternative,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    val route       = alternative.route
    val borderColor = if (isSelected) Amber else SurfaceVariant
    val scoreColor  = route.exposureScore.scoreColor()

    Box(
        modifier = Modifier
            .width(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable { onSelect() }
    ) {
        // Left accent stripe
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(120.dp)
                .background(scoreColor),
        )

        Column(modifier = Modifier.padding(start = 12.dp, top = 10.dp, end = 10.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Route ${alternative.label}",
                    style = ClearPathTypography.titleMedium,
                    color = OnSurface,
                )
                if (alternative.isRecommended) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "BEST",
                        style = ClearPathTypography.labelSmall,
                        color = Amber,
                        modifier = Modifier
                            .background(Amber.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            MetricRow("Distance", route.distanceMetres.formatDistance())
            MetricRow("Time", route.durationSeconds.formatDuration())
            MetricRow("Exposure", route.exposureScore.formatExposureScore() + "/100", scoreColor)
            MetricRow("Cameras", route.cameraCount.toString())

            val anprCount = route.cameraEncounters.size  // simplified — would filter by type
            if (anprCount > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "! $anprCount ANPR cameras",
                    style = ClearPathTypography.labelSmall,
                    color = ExposureHigh,
                )
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String, valueColor: Color = OnSurface) {
    Row(modifier = Modifier.padding(vertical = 1.dp)) {
        Text(
            label,
            style    = ClearPathTypography.labelSmall,
            color    = OnSurfaceMuted,
            modifier = Modifier.width(70.dp),
        )
        Text(value, style = ClearPathTypography.labelMedium, color = valueColor)
    }
}

@Composable
private fun OptimisationToggle(current: RouteOptimisation, onChange: (RouteOptimisation) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceVariant),
    ) {
        RouteOptimisation.entries.forEach { opt ->
            val selected = opt == current
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) Amber else Color.Transparent)
                    .clickable { onChange(opt) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text  = opt.label(),
                    style = ClearPathTypography.labelSmall,
                    color = if (selected) Background else OnSurfaceMuted,
                )
            }
        }
    }
}

private fun RouteOptimisation.label() = when (this) {
    RouteOptimisation.FASTEST         -> "Fastest"
    RouteOptimisation.LOWEST_EXPOSURE -> "Stealth"
    RouteOptimisation.BALANCED        -> "Balanced"
}

private fun Float.scoreColor(): Color = when {
    this < 30f -> ExposureLow
    this < 60f -> ExposureMed
    else       -> ExposureHigh
}
