package com.clearpath.ui.tagging

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clearpath.data.camera.CameraNode
import com.clearpath.data.camera.CameraType
import com.clearpath.data.camera.NodeSource
import com.clearpath.data.route.LatLon
import com.clearpath.ui.theme.Amber
import com.clearpath.ui.theme.Background
import com.clearpath.ui.theme.ClearPathTypography
import com.clearpath.ui.theme.OnSurface
import com.clearpath.ui.theme.OnSurfaceMuted
import com.clearpath.ui.theme.Surface
import com.clearpath.ui.theme.SurfaceVariant
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagCameraSheet(
    position: LatLon,
    onSave: (CameraNode) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedType by remember { mutableStateOf(CameraType.UNKNOWN) }
    var direction    by remember { mutableStateOf<Int?>(null) }
    var confidence   by remember { mutableFloatStateOf(0.7f) }
    var notes        by remember { mutableStateOf("") }
    var expanded     by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text("Tag a Camera", style = ClearPathTypography.headlineMedium, color = OnSurface)
        Text(
            "%.5f, %.5f".format(position.lat, position.lon),
            style = ClearPathTypography.labelSmall,
            color = OnSurfaceMuted,
        )

        Spacer(Modifier.height(16.dp))

        // Camera type dropdown
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value         = selectedType.displayName(),
                onValueChange = {},
                readOnly      = true,
                label         = { Text("Camera type", color = OnSurfaceMuted) },
                trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                colors        = fieldColors(),
                modifier      = Modifier.menuAnchor().fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded         = expanded,
                onDismissRequest = { expanded = false },
            ) {
                CameraType.entries.forEach { type ->
                    DropdownMenuItem(
                        text    = { Text(type.displayName(), color = OnSurface) },
                        onClick = { selectedType = type; expanded = false },
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Direction (optional)
        OutlinedTextField(
            value         = direction?.toString() ?: "",
            onValueChange = { direction = it.toIntOrNull()?.coerceIn(0, 359) },
            label         = { Text("Direction (0–359°, optional)", color = OnSurfaceMuted) },
            placeholder   = { Text("Leave blank for omnidirectional", color = OnSurfaceMuted) },
            colors        = fieldColors(),
            modifier      = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        // Confidence slider
        Text(
            "Confidence: ${"%.0f".format(confidence * 100)}%",
            style = ClearPathTypography.bodyMedium,
            color = OnSurface,
        )
        Slider(
            value         = confidence,
            onValueChange = { confidence = it },
            valueRange    = 0.1f..1.0f,
            colors        = SliderDefaults.colors(thumbColor = Amber, activeTrackColor = Amber),
        )

        Spacer(Modifier.height(12.dp))

        // Notes
        OutlinedTextField(
            value         = notes,
            onValueChange = { notes = it },
            label         = { Text("Notes (optional)", color = OnSurfaceMuted) },
            minLines      = 2,
            colors        = fieldColors(),
            modifier      = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onDismiss,
                colors  = ButtonDefaults.buttonColors(containerColor = SurfaceVariant),
                modifier = Modifier.weight(1f),
            ) {
                Text("Cancel", color = OnSurface)
            }
            Button(
                onClick = {
                    val node = CameraNode(
                        id         = System.currentTimeMillis(),
                        lat        = position.lat,
                        lon        = position.lon,
                        type       = selectedType,
                        direction  = direction,
                        confidence = confidence,
                        source     = NodeSource.USER_TAGGED,
                        notes      = notes.takeIf { it.isNotBlank() },
                    )
                    onSave(node)
                },
                colors   = ButtonDefaults.buttonColors(containerColor = Amber),
                modifier = Modifier.weight(1f),
            ) {
                Text("Save", color = Background)
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = Amber,
    unfocusedBorderColor = SurfaceVariant,
    focusedTextColor     = OnSurface,
    unfocusedTextColor   = OnSurface,
    cursorColor          = Amber,
)

private fun CameraType.displayName() = when (this) {
    CameraType.FIXED   -> "Fixed CCTV"
    CameraType.PTZ     -> "PTZ (Pan-Tilt-Zoom)"
    CameraType.DOME    -> "Dome"
    CameraType.ANPR    -> "ANPR"
    CameraType.UNKNOWN -> "Unknown"
}
