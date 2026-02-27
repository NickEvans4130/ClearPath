package com.clearpath.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.clearpath.geocoding.SearchResult
import com.clearpath.ui.theme.Amber
import com.clearpath.ui.theme.Background
import com.clearpath.ui.theme.OnSurface
import com.clearpath.ui.theme.OnSurfaceMuted
import com.clearpath.ui.theme.Surface
import com.clearpath.ui.theme.SurfaceVariant

@Composable
fun SearchBar(
    searchResults: List<SearchResult>,
    isSearching: Boolean,
    onSearch: (String) -> Unit,
    onResultSelected: (SearchResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Surface)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = OnSurfaceMuted,
                modifier = Modifier.padding(start = 8.dp),
            )
            Spacer(Modifier.width(4.dp))
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search address or place…", color = OnSurfaceMuted) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction    = ImeAction.Search,
                ),
                keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor   = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor        = OnSurface,
                    unfocusedTextColor      = OnSurface,
                    cursorColor             = Amber,
                ),
                modifier = Modifier.weight(1f),
            )
            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp).padding(end = 8.dp),
                    color    = Amber,
                    strokeWidth = 2.dp,
                )
            }
        }

        AnimatedVisibility(searchResults.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                    .background(Surface),
            ) {
                searchResults.forEachIndexed { idx, result ->
                    if (idx > 0) HorizontalDivider(color = SurfaceVariant, thickness = 0.5.dp)
                    Text(
                        text = result.displayName,
                        color = OnSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onResultSelected(result) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        maxLines = 2,
                    )
                }
            }
        }
    }
}
