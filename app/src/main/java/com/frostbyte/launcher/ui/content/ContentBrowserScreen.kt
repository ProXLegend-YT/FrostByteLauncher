package com.frostbyte.launcher.ui.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column {
                    Text(text = title, style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        text = "Search and install $title from Modrinth",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::onQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search $title...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary) },
                    trailingIcon = {
                        IconButton(onClick = viewModel::search) {
                            if (uiState.isSearching) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = IceBlue, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.Search, contentDescription = "Search", tint = IceBlue)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IceBlue,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f)
                    )
                )
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
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        GlassCard {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Extension, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Search to discover $title from Modrinth",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Placeholder glyph avatar - item.iconUrl is real data from Modrinth
            // but this build doesn't have an image-loading library wired up
            // yet, so rather than add a new dependency inside a UI restyle
            // (real risk of a broken build on a CI-only workflow), this shows
            // a consistent icon instead of nothing. Swapping in the real icon
            // via Coil/AsyncImage is a clean, self-contained follow-up.
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color = IceBlue.copy(alpha = 0.15f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Extension, contentDescription = null, tint = IceBlue, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${formatDownloadCount(item.downloadCount)} downloads",
                    style = MaterialTheme.typography.labelSmall,
                    color = IceBlue
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

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

/** "12,483" -> "12.5k", ">=1,000,000 -> "1.2M". Real count, just formatted for a small screen. */
private fun formatDownloadCount(count: Int): String = when {
    count >= 1_000_000 -> "%.1fM".format(count / 1_000_000f)
    count >= 1_000 -> "%.1fk".format(count / 1_000f)
    else -> count.toString()
}
