package com.clearpath.ui.education

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.clearpath.data.camera.CameraType
import com.clearpath.ui.theme.Amber
import com.clearpath.ui.theme.CameraANPR
import com.clearpath.ui.theme.CameraFixed
import com.clearpath.ui.theme.CameraPTZ
import com.clearpath.ui.theme.CameraUnknown
import com.clearpath.ui.theme.ClearPathTypography
import com.clearpath.ui.theme.ExposureHigh
import com.clearpath.ui.theme.OnSurface
import com.clearpath.ui.theme.OnSurfaceMuted
import com.clearpath.ui.theme.Surface

data class CameraEducationContent(
    val type: CameraType,
    val headline: String,
    val coverage: String,
    val retention: String,
    val access: String,
    val legislation: String,
    val tips: List<String>,
)

val educationContent = listOf(
    CameraEducationContent(
        type        = CameraType.FIXED,
        headline    = "Fixed CCTV Camera",
        coverage    = "Covers a fixed field of view, typically 20–40m range. No pan or tilt.",
        retention   = "28 days typical (Protection of Freedoms Act 2012 Code of Practice). Some operators keep footage shorter.",
        access      = "Local authority, building management, police on request. Subject access requests possible under UK GDPR.",
        legislation = "Regulated by the Surveillance Camera Code of Practice (2013). ICO Data Protection Act 2018 applies.",
        tips        = listOf(
            "Fixed cameras have blind spots — their coverage is limited to the direction they face.",
            "Footage is usually only reviewed after an incident is reported.",
            "You can request footage of yourself under UK GDPR Article 15 (Subject Access Request).",
        ),
    ),
    CameraEducationContent(
        type        = CameraType.PTZ,
        headline    = "PTZ Camera (Pan-Tilt-Zoom)",
        coverage    = "Can pan 360°, tilt ±90°, and optionally zoom. Effective range 100m+ at max zoom.",
        retention   = "28–90 days. Often monitored live in CCTV control rooms.",
        access      = "Same as fixed, but more likely to have an active operator. Remote access via VPN common.",
        legislation = "Higher capability requires explicit justification under the Surveillance Camera Code. Must be necessary and proportionate.",
        tips        = listOf(
            "PTZ cameras may be actively tracking movement — movement triggers automated PTZ steering.",
            "Their direction is unpredictable; they can rapidly switch coverage.",
            "Look for the 'dome-and-stick' mount with visible motor housing.",
        ),
    ),
    CameraEducationContent(
        type        = CameraType.ANPR,
        headline    = "ANPR Camera (Automatic Number Plate Recognition)",
        coverage    = "Captures vehicle plates. Fixed to road infrastructure. Effective at lane-level range.",
        retention   = "Non-hits (no alert): 90 days (NPCC guidance). Hits: 12 months or as required by investigation.",
        access      = "Police National Computer (PNC) and regional ANPR databases. Counter-terrorism and serious crime units.",
        legislation = "Regulated by National ANPR Standards for Policing. Data Protection Act 2018 / UK GDPR applies. Freedom of Information requests may reveal camera locations.",
        tips        = listOf(
            "Pedestrians are not directly captured by ANPR — these cameras target vehicle plates.",
            "ANPR builds movement profiles over time, correlating journeys with times and locations.",
            "Rental vehicles and hire cars are fully ANPR-visible, contrary to common belief.",
            "ANPR data is shared widely across forces under 'overt' and 'covert' ANPR networks.",
        ),
    ),
    CameraEducationContent(
        type        = CameraType.DOME,
        headline    = "Dome Camera",
        coverage    = "Dome housing obscures the lens direction — effective 360° deterrent even if fixed internally.",
        retention   = "Typically 28 days. Same as fixed CCTV in most deployments.",
        access      = "Building security, retail loss prevention, police request. Operator identity may not be marked.",
        legislation = "Same as fixed CCTV. Dome design does not grant additional legal authority.",
        tips        = listOf(
            "You cannot determine which direction a dome camera is facing from ground level.",
            "Assume full circular coverage when a dome camera is present.",
            "Common in retail, transport hubs, and public buildings.",
        ),
    ),
)

@Composable
fun CameraTypeCard(
    content: CameraEducationContent,
    modifier: Modifier = Modifier,
) {
    val accentColor = content.type.accentColor()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .padding(16.dp),
    ) {
        // Type badge
        Text(
            content.headline,
            style    = ClearPathTypography.headlineMedium,
            color    = accentColor,
        )

        Spacer(Modifier.height(12.dp))

        Section("Coverage",    content.coverage)
        Section("Retention",   content.retention)
        Section("Who can access", content.access)
        Section("Legislation", content.legislation)

        if (content.tips.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Practical notes", style = ClearPathTypography.titleMedium, color = OnSurfaceMuted)
            Spacer(Modifier.height(4.dp))
            content.tips.forEach { tip ->
                Text(
                    "• $tip",
                    style    = ClearPathTypography.bodyMedium,
                    color    = OnSurface,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun Section(label: String, body: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = ClearPathTypography.labelSmall, color = OnSurfaceMuted)
        Text(body,  style = ClearPathTypography.bodyMedium, color = OnSurface)
    }
}

private fun CameraType.accentColor(): Color = when (this) {
    CameraType.ANPR    -> CameraANPR
    CameraType.FIXED   -> CameraFixed
    CameraType.PTZ     -> CameraPTZ
    CameraType.DOME    -> CameraPTZ
    CameraType.UNKNOWN -> CameraUnknown
}
