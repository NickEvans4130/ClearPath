package com.clearpath.ui.routing

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.clearpath.routing.NavigationState
import com.clearpath.routing.ZoneStatus
import com.clearpath.ui.theme.Amber
import com.clearpath.ui.theme.Background
import com.clearpath.ui.theme.ClearPathTypography
import com.clearpath.ui.theme.ExposureHigh
import com.clearpath.ui.theme.ExposureLow
import com.clearpath.ui.theme.OnSurface
import com.clearpath.ui.theme.Surface
import com.clearpath.ui.theme.SurfaceVariant
import com.clearpath.util.formatDistance
import com.clearpath.util.formatDuration

@Composable
fun NavigationHUD(
    navState: NavigationState,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zoneColor = when (navState.zoneStatus) {
        ZoneStatus.IN_ZONE  -> ExposureHigh
        ZoneStatus.ENTERING -> Amber
        ZoneStatus.CLEAR    -> ExposureLow
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface.copy(alpha = 0.95f))
            .border(1.5.dp, zoneColor, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier              = Modifier.fillMaxWidth(),
        ) {
            // Zone status
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (navState.zoneStatus != ZoneStatus.CLEAR) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = zoneColor)
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = when (navState.zoneStatus) {
                        ZoneStatus.CLEAR    -> "Clear zone"
                        ZoneStatus.ENTERING -> "Entering surveillance zone"
                        ZoneStatus.IN_ZONE  -> "In surveillance zone"
                    },
                    style = ClearPathTypography.titleMedium,
                    color = zoneColor,
                )
            }
            IconButton(onClick = onStop) {
                Icon(Icons.Default.Close, contentDescription = "Stop navigation", tint = OnSurface)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            HudStat(
                label = "Remaining",
                value = navState.distanceRemainingMetres.formatDistance(),
            )
            HudStat(
                label = "ETA",
                value = navState.durationRemainingSeconds.formatDuration(),
            )
            navState.nextCameraDistanceMetres?.let { dist ->
                HudStat(
                    label = "Next camera",
                    value = dist.toInt().formatDistance(),
                    valueColor = Amber,
                )
            }
            HudStat(
                label = "In coverage",
                value = "${navState.totalExposureSeconds}s",
                valueColor = if (navState.totalExposureSeconds > 30) ExposureHigh else OnSurface,
            )
        }
    }
}

@Composable
private fun HudStat(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = OnSurface,
) {
    Column {
        Text(label, style = ClearPathTypography.labelSmall, color = com.clearpath.ui.theme.OnSurfaceMuted)
        Text(value, style = ClearPathTypography.labelMedium, color = valueColor)
    }
}
