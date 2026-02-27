package com.clearpath.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clearpath.data.camera.CameraType
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

@Composable
fun StatsScreen(
    onBack: () -> Unit,
    vm: StatsViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = OnSurface)
            }
            Text("Surveillance Stats", style = ClearPathTypography.headlineMedium, color = OnSurface)
        }

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Summary cards ──────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    label = "Total routes",
                    value = state.totalRoutes.toString(),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = "Time under surveillance",
                    value = "${state.totalExposureSeconds / 60}m",
                    valueColor = ExposureHigh,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    label = "Total cameras in dataset",
                    value = state.totalCameras.toString(),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = "ANPR cameras",
                    value = state.anprCount.toString(),
                    valueColor = ExposureHigh,
                    modifier = Modifier.weight(1f),
                )
            }

            // ── Most surveilled route ──────────────────────────────────────
            state.mostSurveilledRoute?.let { route ->
                SectionHeader("Most surveilled route")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface)
                        .padding(14.dp)
                ) {
                    Column {
                        Text(route.name, style = ClearPathTypography.titleMedium, color = OnSurface)
                        Text(
                            "Exposure score: %.0f/100".format(route.exposureScore),
                            style = ClearPathTypography.bodyMedium,
                            color = ExposureHigh,
                        )
                        Text(
                            "${route.cameraCount} cameras encountered",
                            style = ClearPathTypography.labelSmall,
                            color = OnSurfaceMuted,
                        )
                    }
                }
            }

            // ── Camera type breakdown ──────────────────────────────────────
            SectionHeader("Camera type breakdown")
            CameraTypeBreakdown(state.typeBreakdown)

            // ── Educational callout ────────────────────────────────────────
            if (state.anprCount > 0) {
                EducationalCallout(
                    "You've passed ${state.anprCount} ANPR cameras in your recorded journeys. " +
                    "ANPR systems log your vehicle's plate, timestamp, and location to the Police National Computer. " +
                    "Reads can be retained for up to 2 years under current UK Home Office guidance."
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = OnSurface,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .padding(14.dp),
    ) {
        Text(label, style = ClearPathTypography.labelSmall, color = OnSurfaceMuted)
        Spacer(Modifier.height(4.dp))
        Text(value, style = ClearPathTypography.displayLarge, color = valueColor)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = ClearPathTypography.titleMedium, color = OnSurfaceMuted, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun CameraTypeBreakdown(counts: Map<CameraType, Int>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val total = counts.values.sum().toFloat().coerceAtLeast(1f)
        counts.entries.sortedByDescending { it.value }.forEach { (type, count) ->
            val fraction = count / total
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    type.label(),
                    style    = ClearPathTypography.labelSmall,
                    color    = OnSurfaceMuted,
                    modifier = Modifier.weight(0.35f),
                )
                Box(
                    modifier = Modifier
                        .weight(0.55f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(8.dp)
                            .background(type.barColor())
                    )
                }
                Text(
                    count.toString(),
                    style    = ClearPathTypography.labelMedium,
                    color    = OnSurface,
                    modifier = Modifier.weight(0.1f).padding(start = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun EducationalCallout(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ExposureHigh.copy(alpha = 0.1f))
            .padding(14.dp),
    ) {
        Text(text, style = ClearPathTypography.bodyMedium, color = ExposureHigh)
    }
}

private fun CameraType.label() = when (this) {
    CameraType.FIXED   -> "Fixed"
    CameraType.PTZ     -> "PTZ"
    CameraType.DOME    -> "Dome"
    CameraType.ANPR    -> "ANPR"
    CameraType.UNKNOWN -> "Unknown"
}

private fun CameraType.barColor() = when (this) {
    CameraType.ANPR    -> ExposureHigh
    CameraType.PTZ     -> ExposureMed
    CameraType.DOME    -> ExposureMed
    CameraType.FIXED   -> Amber
    CameraType.UNKNOWN -> com.clearpath.ui.theme.CameraUnknown
}
