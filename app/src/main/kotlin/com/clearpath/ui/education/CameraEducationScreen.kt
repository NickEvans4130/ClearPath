package com.clearpath.ui.education

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clearpath.ui.theme.Background
import com.clearpath.ui.theme.ClearPathTypography
import com.clearpath.ui.theme.OnSurface
import com.clearpath.ui.theme.OnSurfaceMuted

@Composable
fun CameraEducationScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = OnSurface)
            }
            Text(
                "Surveillance Cameras",
                style = ClearPathTypography.headlineMedium,
                color = OnSurface,
            )
        }

        Text(
            "Understanding what each camera type records, retains, and who can access it.",
            style    = ClearPathTypography.bodyMedium,
            color    = OnSurfaceMuted,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            educationContent.forEach { content ->
                CameraTypeCard(content = content)
            }

            // Private vs public section
            PrivateVsPublicCard()

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PrivateVsPublicCard() {
    CameraTypeCard(
        content = CameraEducationContent(
            type        = com.clearpath.data.camera.CameraType.UNKNOWN,
            headline    = "Private vs Public Cameras",
            coverage    = "Private cameras (shops, homes) are regulated by UK GDPR/DPA 2018. Public authority cameras (council, police) also fall under the Surveillance Camera Code of Practice.",
            retention   = "Private: operator sets policy, often 30 days. Public: 28–90 days as standard, longer if crime-linked.",
            access      = "Private: operator; law enforcement via court order or urgent data request. Public: inter-agency sharing common under CCTV sharing protocols.",
            legislation = "ICO guidance on domestic CCTV. UK GDPR Article 15 enables subject access requests. Human Rights Act Article 8 (right to privacy) applies.",
            tips        = listOf(
                "A camera pointing beyond the owner's property boundary onto public space must comply with UK GDPR.",
                "Registered operators must be contactable for subject access requests — look for mandatory CCTV signs.",
                "Unregistered private cameras are a grey area — ICO can issue enforcement notices.",
            ),
        )
    )
}
