package com.clearpath.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.clearpath.data.camera.CameraNode
import com.clearpath.data.camera.CameraType
import com.clearpath.data.camera.NodeSource
import com.clearpath.ui.theme.Amber
import com.clearpath.ui.theme.CameraANPR
import com.clearpath.ui.theme.CameraFixed
import com.clearpath.ui.theme.CameraPTZ
import com.clearpath.ui.theme.CameraUnknown
import com.clearpath.ui.theme.ClearPathTypography
import com.clearpath.ui.theme.OnSurface
import com.clearpath.ui.theme.OnSurfaceMuted
import com.clearpath.ui.theme.Surface
import com.clearpath.ui.theme.SurfaceVariant

@Composable
fun CameraDetailPopup(
    camera: CameraNode,
    onDismiss: () -> Unit,
    onOpenEducation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typeColor = camera.type.color()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, SurfaceVariant, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Coloured type badge
            Text(
                text     = camera.type.badge(),
                style    = ClearPathTypography.labelSmall,
                color    = typeColor,
                modifier = Modifier
                    .background(typeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
            Spacer(Modifier.width(8.dp))
            // Source tag
            if (camera.source != NodeSource.OSM) {
                Text(
                    text  = if (camera.source == NodeSource.USER_TAGGED) "User-tagged" else "Estimated",
                    style = ClearPathTypography.labelSmall,
                    color = Amber,
                    modifier = Modifier
                        .background(Amber.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onDismiss, modifier = Modifier) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = OnSurfaceMuted)
            }
        }

        Spacer(Modifier.height(10.dp))

        camera.operator?.let {
            InfoRow("Operator", it)
        }
        InfoRow("Coverage radius", "${camera.coverageRadius}m")
        camera.direction?.let {
            InfoRow("Direction", "$it°")
        }
        InfoRow("Confidence", "%.0f%%".format(camera.confidence * 100))
        InfoRow("OSM ID", camera.id.toString())
        camera.notes?.let {
            InfoRow("Notes", it)
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onOpenEducation) {
            Text("Learn about ${camera.type.displayName()} cameras →", color = Amber, style = ClearPathTypography.labelMedium)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(
            text     = label,
            style    = ClearPathTypography.labelSmall,
            color    = OnSurfaceMuted,
            modifier = Modifier.width(120.dp),
        )
        Text(
            text  = value,
            style = ClearPathTypography.bodyMedium,
            color = OnSurface,
        )
    }
}

private fun CameraType.badge() = when (this) {
    CameraType.FIXED   -> "FIXED CCTV"
    CameraType.PTZ     -> "PTZ"
    CameraType.DOME    -> "DOME"
    CameraType.ANPR    -> "ANPR"
    CameraType.UNKNOWN -> "UNKNOWN"
}

private fun CameraType.displayName() = when (this) {
    CameraType.FIXED   -> "fixed CCTV"
    CameraType.PTZ     -> "PTZ"
    CameraType.DOME    -> "dome"
    CameraType.ANPR    -> "ANPR"
    CameraType.UNKNOWN -> "surveillance"
}

private fun CameraType.color(): Color = when (this) {
    CameraType.ANPR    -> CameraANPR
    CameraType.FIXED   -> CameraFixed
    CameraType.PTZ     -> CameraPTZ
    CameraType.DOME    -> CameraPTZ
    CameraType.UNKNOWN -> CameraUnknown
}
