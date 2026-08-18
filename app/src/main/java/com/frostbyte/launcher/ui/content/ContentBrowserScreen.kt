package com.frostbyte.launcher.ui.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.frostbyte.launcher.core.content.ContentSearchResult
import com.frostbyte.launcher.ui.components.GlassCard
import com.frostbyte.launcher.ui.space.SpaceRenderer
import com.frostbyte.launcher.ui.theme.IceBlue
import com.frostbyte.launcher.ui.theme.StatusRed
import com.frostbyte.launcher.ui.theme.TextSecondary

@Composable
fun ContentBrowserScreen(title: String, viewModel: ContentBrowserViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        SpaceRenderer(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(text = title, style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onBackground)
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = viewModel::onQueryChanged,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search $title") },
                        singleLine = true
                    )
                    IconButton(onClick = viewModel::search) {
                        if (uiState.isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = IceBlue, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Search, contentDescription = "Search", tint = IceBlue)
                        }
                    }
                }
            }

            if (uiState.errorMessage != null) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
                        Text(uiState.errorMessage!!, color = StatusRed, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (uiState.results.isEmpty() && !uiState.isSearching) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Search to discover $title from Modrinth.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                items(uiState.results, key = { it.id }) { item ->
                    ContentResultRow(
                        item = item,
                        isDownloading = uiState.downloadingProjectId == item.id,
                        onDownloadClick = { viewModel.downloadLatest(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ContentResultRow(item: ContentSearchResult, isDownloading: Boolean, onDownloadClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(item.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, maxLines = 2)
                Text("${item.downloadCount} downloads", style = MaterialTheme.typography.labelSmall, color = IceBlue)
            }
            if (isDownloading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = IceBlue, strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onDownloadClick) {
                    Icon(Icons.Filled.Download, contentDescription = "Download ${item.title}", tint = IceBlue)
                }
            }
        }
    }
}
