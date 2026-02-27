package com.clearpath.ui.routing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clearpath.data.route.LatLon
import com.clearpath.data.route.RoutingMode
import com.clearpath.ui.theme.Amber
import com.clearpath.ui.theme.Background
import com.clearpath.ui.theme.ClearPathTypography
import com.clearpath.ui.theme.ExposureHigh
import com.clearpath.ui.theme.OnSurface
import com.clearpath.ui.theme.OnSurfaceMuted
import com.clearpath.ui.theme.SurfaceVariant

@Composable
fun RoutePlanningSheet(
    origin: LatLon?,
    destination: LatLon?,
    routingMode: RoutingMode,
    isRouting: Boolean,
    error: String?,
    onSetRoutingMode: (RoutingMode) -> Unit,
    onClearOrigin: () -> Unit,
    onClearDestination: () -> Unit,
    onPlanRoute: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text("Plan Route", style = ClearPathTypography.headlineMedium, color = OnSurface)
        Spacer(Modifier.height(4.dp))
        Text(
            "Long-press the map to set origin and destination.",
            style = ClearPathTypography.bodySmall,
            color = OnSurfaceMuted,
        )

        Spacer(Modifier.height(16.dp))

        WaypointRow(
            label   = "From",
            latLon  = origin,
            hint    = "Long-press the map to set",
            onClear = onClearOrigin,
        )

        Spacer(Modifier.height(8.dp))

        WaypointRow(
            label   = "To",
            latLon  = destination,
            hint    = "Long-press the map to set",
            onClear = onClearDestination,
        )

        Spacer(Modifier.height(16.dp))

        Text("Travel mode", style = ClearPathTypography.labelMedium, color = OnSurfaceMuted)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoutingMode.entries.forEach { mode ->
                FilterChip(
                    selected = routingMode == mode,
                    onClick  = { onSetRoutingMode(mode) },
                    label    = {
                        Text(
                            mode.label(),
                            color = if (routingMode == mode) Background else OnSurface,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Amber,
                        containerColor         = SurfaceVariant,
                    ),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (error != null) {
            Text(error, style = ClearPathTypography.bodySmall, color = ExposureHigh)
            Spacer(Modifier.height(8.dp))
        }

        val canPlan = origin != null && destination != null && !isRouting
        Button(
            onClick  = onPlanRoute,
            enabled  = canPlan,
            colors   = ButtonDefaults.buttonColors(
                containerColor         = Amber,
                disabledContainerColor = SurfaceVariant,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isRouting) {
                CircularProgressIndicator(
                    color       = Background,
                    modifier    = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text  = if (isRouting) "Planning..." else "Plan Route",
                color = if (canPlan) Background else OnSurfaceMuted,
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun WaypointRow(
    label: String,
    latLon: LatLon?,
    hint: String,
    onClear: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceVariant, shape = RoundedCornerShape(8.dp))
            .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = ClearPathTypography.labelSmall, color = OnSurfaceMuted)
            if (latLon != null) {
                Text(
                    "%.5f, %.5f".format(latLon.lat, latLon.lon),
                    style = ClearPathTypography.bodyMedium,
                    color = OnSurface,
                )
            } else {
                Text(hint, style = ClearPathTypography.bodyMedium, color = OnSurfaceMuted)
            }
        }
        if (latLon != null) {
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = OnSurfaceMuted)
            }
        }
    }
}

private fun RoutingMode.label() = when (this) {
    RoutingMode.FOOT    -> "Walk"
    RoutingMode.BICYCLE -> "Cycle"
    RoutingMode.CAR     -> "Drive"
}
