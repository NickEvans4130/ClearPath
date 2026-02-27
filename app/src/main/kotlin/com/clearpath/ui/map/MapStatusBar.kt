package com.clearpath.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.clearpath.ui.theme.Amber
import com.clearpath.ui.theme.ClearPathTypography
import com.clearpath.ui.theme.OnSurfaceMuted
import com.clearpath.ui.theme.Surface
import com.clearpath.util.toRelativeDate

@Composable
fun MapStatusBar(
    cameraCount: Int,
    freshnessMs: Long,
    isSyncing: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Surface.copy(alpha = 0.92f))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(
            text  = "$cameraCount cameras",
            style = ClearPathTypography.labelSmall,
            color = OnSurfaceMuted,
        )
        Text("·", color = OnSurfaceMuted, style = ClearPathTypography.labelSmall)
        Text(
            text  = "Data: ${freshnessMs.toRelativeDate()}",
            style = ClearPathTypography.labelSmall,
            color = OnSurfaceMuted,
        )
        if (isSyncing) {
            CircularProgressIndicator(
                modifier    = Modifier.padding(start = 4.dp),
                color       = Amber,
                strokeWidth = 1.5.dp,
            )
        }
    }
}
