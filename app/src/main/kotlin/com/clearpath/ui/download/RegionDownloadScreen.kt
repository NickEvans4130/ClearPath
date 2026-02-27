package com.clearpath.ui.download

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clearpath.data.tiles.Region
import com.clearpath.data.tiles.RegionCatalogue
import com.clearpath.ui.theme.Amber
import com.clearpath.ui.theme.Background
import com.clearpath.ui.theme.ClearPathTypography
import com.clearpath.ui.theme.ExposureLow
import com.clearpath.ui.theme.OnSurface
import com.clearpath.ui.theme.OnSurfaceMuted
import com.clearpath.ui.theme.Surface
import com.clearpath.ui.theme.SurfaceVariant

@Composable
fun RegionDownloadScreen(
    onBack: () -> Unit,
    vm: DownloadViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = OnSurface)
            }
            Text("Download Map Region", style = ClearPathTypography.headlineMedium, color = OnSurface)
        }

        Text(
            "Select a region to download tiles for offline use (~350MB for London).",
            style    = ClearPathTypography.bodyMedium,
            color    = OnSurfaceMuted,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(8.dp))

        // Import custom .mbtiles
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Surface)
                .clickable { vm.importCustomFile() }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Amber)
            Column {
                Text("Import custom .mbtiles file", style = ClearPathTypography.titleMedium, color = OnSurface)
                Text("Browse and import any .mbtiles file from device storage", style = ClearPathTypography.labelSmall, color = OnSurfaceMuted)
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(RegionCatalogue.regions) { region ->
                RegionCard(
                    region    = region,
                    isActive  = state.activeRegionId == region.id,
                    progress  = if (state.downloadingRegionId == region.id) state.progress else null,
                    onDownload = { vm.download(region) },
                    onActivate = { vm.activate(region) },
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun RegionCard(
    region: Region,
    isActive: Boolean,
    progress: Int?,
    onDownload: () -> Unit,
    onActivate: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(region.displayName, style = ClearPathTypography.titleMedium, color = OnSurface)
                if (isActive) {
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "ACTIVE",
                        style = ClearPathTypography.labelSmall,
                        color = ExposureLow,
                        modifier = Modifier
                            .background(ExposureLow.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Text(
                "~${region.estimatedSizeMb} MB",
                style = ClearPathTypography.labelSmall,
                color = OnSurfaceMuted,
            )
            if (progress != null) {
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress          = { progress / 100f },
                    modifier          = Modifier.fillMaxWidth(),
                    color             = Amber,
                    trackColor        = SurfaceVariant,
                )
                Text("$progress%", style = ClearPathTypography.labelSmall, color = Amber)
            }
        }

        if (progress != null) {
            CircularProgressIndicator(
                modifier    = Modifier.size(24.dp),
                color       = Amber,
                strokeWidth = 2.dp,
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isActive) {
                    Button(
                        onClick = onActivate,
                        colors  = ButtonDefaults.buttonColors(containerColor = SurfaceVariant),
                    ) {
                        Text("Use", color = OnSurface)
                    }
                }
                Button(
                    onClick = onDownload,
                    colors  = ButtonDefaults.buttonColors(containerColor = Amber),
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = Background, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
