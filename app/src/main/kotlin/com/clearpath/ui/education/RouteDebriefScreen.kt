package com.clearpath.ui.education

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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.clearpath.data.camera.CameraType
import com.clearpath.data.route.SavedRoute
import com.clearpath.ui.routing.ExposureTimelineChart
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

@Composable
fun RouteDebriefScreen(
    route: SavedRoute,
    onBack: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scoreColor = when {
        route.exposureScore < 30f -> ExposureLow
        route.exposureScore < 60f -> ExposureMed
        else                      -> ExposureHigh
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding(),
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = OnSurface)
            }
            Text("Journey Debrief", style = ClearPathTypography.headlineMedium, color = OnSurface, modifier = Modifier.weight(1f))
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, "Share", tint = OnSurface)
            }
        }

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Exposure score ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(scoreColor.copy(alpha = 0.1f))
                    .padding(16.dp),
            ) {
                Column {
                    Text("Exposure Score", style = ClearPathTypography.labelSmall, color = OnSurfaceMuted)
                    Text(
                        "%.0f / 100".format(route.exposureScore),
                        style = ClearPathTypography.displayLarge,
                        color = scoreColor,
                    )
                    Text(
                        when {
                            route.exposureScore < 30f -> "Low exposure — effective low-profile route."
                            route.exposureScore < 60f -> "Moderate exposure — some surveillance contact."
                            else                      -> "High exposure — significant surveillance contact."
                        },
                        style = ClearPathTypography.bodyMedium,
                        color = OnSurface,
                    )
                }
            }

            // ── Journey summary ───────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("Distance", route.distanceMetres.formatDistance(), Modifier.weight(1f))
                SummaryCard("Duration", route.durationSeconds.formatDuration(), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("Cameras", route.cameraCount.toString(), Modifier.weight(1f))
                val totalSecs = route.cameraEncounters.sumOf { it.estimatedExposureSeconds }
                SummaryCard("Time in coverage", "${totalSecs}s", Modifier.weight(1f))
            }

            // ── What was logged ───────────────────────────────────────────
            DebriefSection(
                title = "What was likely logged",
                body  = buildWasLoggedText(route),
                color = ExposureHigh,
            )

            // ── What wasn't logged ────────────────────────────────────────
            DebriefSection(
                title = "What wasn't logged",
                body  = buildWasNotLoggedText(route),
                color = ExposureLow,
            )

            // ── Exposure timeline ─────────────────────────────────────────
            ExposureTimelineChart(
                route = route,
                onSpikeTap = {},
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Surface)
            .padding(12.dp),
    ) {
        Text(label, style = ClearPathTypography.labelSmall, color = OnSurfaceMuted)
        Text(value, style = ClearPathTypography.titleMedium, color = OnSurface)
    }
}

@Composable
private fun DebriefSection(title: String, body: String, color: androidx.compose.ui.graphics.Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(14.dp),
    ) {
        Text(title, style = ClearPathTypography.titleMedium, color = color)
        Spacer(Modifier.height(6.dp))
        Text(body, style = ClearPathTypography.bodyMedium, color = OnSurface)
    }
}

private fun buildWasLoggedText(route: SavedRoute): String {
    val parts = mutableListOf<String>()
    val anpr = route.cameraEncounters.size  // simplified
    if (anpr > 0) {
        parts.add("Vehicle plate reads by ANPR cameras — timestamped and location-tagged entries on the Police National Computer.")
    }
    if (route.cameraCount > 0) {
        parts.add("CCTV footage from fixed/dome cameras covering your route corridor — retained for up to 28 days.")
    }
    if (parts.isEmpty()) parts.add("No significant surveillance contact recorded.")
    return parts.joinToString("\n\n")
}

private fun buildWasNotLoggedText(route: SavedRoute): String {
    val parts = mutableListOf<String>()
    if (route.exposureScore < 50f) {
        parts.add("Large segments of this route were outside all known camera coverage zones.")
    }
    parts.add("Audio is not recorded by standard CCTV deployments under UK law.")
    parts.add("Communications and digital activity are unaffected by physical route choice.")
    return parts.joinToString("\n\n")
}
