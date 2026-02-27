package com.clearpath.ui.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clearpath.data.camera.CameraType
import com.clearpath.map.LayerSettings
import com.clearpath.ui.theme.Amber
import com.clearpath.ui.theme.ClearPathTypography
import com.clearpath.ui.theme.OnSurface
import com.clearpath.ui.theme.OnSurfaceMuted
import com.clearpath.ui.theme.SurfaceVariant

@Composable
fun LayerToggleSheet(
    layers: LayerSettings,
    onChange: (LayerSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            "Map Layers",
            style    = ClearPathTypography.headlineMedium,
            color    = OnSurface,
        )
        Spacer(Modifier.height(16.dp))

        SwitchRow("Show cameras", layers.showCameras) {
            onChange(layers.copy(showCameras = it))
        }
        SwitchRow("Heatmap mode", layers.showHeatmap) {
            onChange(layers.copy(showHeatmap = it))
        }
        SwitchRow("Show routes", layers.showRoute) {
            onChange(layers.copy(showRoute = it))
        }
        SwitchRow("Coverage circles", layers.showCoverage) {
            onChange(layers.copy(showCoverage = it))
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "Camera types",
            style = ClearPathTypography.titleMedium,
            color = OnSurfaceMuted,
        )
        Spacer(Modifier.height(8.dp))

        CameraType.entries.forEach { type ->
            val isVisible = type in layers.visibleTypes
            SwitchRow(type.displayName(), isVisible) { checked ->
                val updated = if (checked) layers.visibleTypes + type
                              else layers.visibleTypes - type
                onChange(layers.copy(visibleTypes = updated))
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = OnSurface, style = ClearPathTypography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor  = Amber,
                checkedTrackColor  = Amber.copy(alpha = 0.4f),
                uncheckedTrackColor = SurfaceVariant,
            ),
        )
    }
}

private fun CameraType.displayName() = when (this) {
    CameraType.FIXED   -> "Fixed CCTV"
    CameraType.PTZ     -> "PTZ (Pan-Tilt-Zoom)"
    CameraType.DOME    -> "Dome"
    CameraType.ANPR    -> "ANPR (Plate Recognition)"
    CameraType.UNKNOWN -> "Unknown"
}
