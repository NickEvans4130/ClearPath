package com.clearpath.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clearpath.ui.theme.Amber
import com.clearpath.ui.theme.Background
import com.clearpath.ui.theme.FabBackground
import com.clearpath.ui.theme.FabIcon

@Composable
fun MapFabs(
    modifier: Modifier = Modifier,
    onSearch: () -> Unit,
    onMyLocation: () -> Unit,
    onToggleLayers: () -> Unit,
    onTagCamera: () -> Unit,
    onPlanRoute: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenDownload: () -> Unit,
) {
    Column(
        modifier    = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.End,
    ) {
        SmallFab(Icons.Default.Download, "Download tiles", onOpenDownload)
        SmallFab(Icons.Default.BarChart, "Statistics", onOpenStats)
        SmallFab(Icons.Default.Add, "Tag camera", onTagCamera)
        SmallFab(Icons.Default.Layers, "Toggle layers", onToggleLayers)
        SmallFab(Icons.Default.MyLocation, "My location", onMyLocation)

        // Primary FAB — plan route
        FloatingActionButton(
            onClick          = onPlanRoute,
            containerColor   = Amber,
            contentColor     = Background,
            modifier         = Modifier.size(56.dp),
        ) {
            Icon(Icons.Default.Route, contentDescription = "Plan route")
        }
    }
}

@Composable
private fun SmallFab(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    SmallFloatingActionButton(
        onClick          = onClick,
        containerColor   = FabBackground,
        contentColor     = FabIcon,
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}
