package com.frostbyte.launcher.ui.screens.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.frostbyte.launcher.core.storage.db.DownloadStatus
import com.frostbyte.launcher.core.storage.repository.DownloadItem
import com.frostbyte.launcher.ui.common.frostByteViewModel
import com.frostbyte.launcher.ui.components.GlassCard
import com.frostbyte.launcher.ui.space.SpaceRenderer
import com.frostbyte.launcher.ui.theme.IceBlue
import com.frostbyte.launcher.ui.theme.StatusGreen
import com.frostbyte.launcher.ui.theme.StatusRed
import com.frostbyte.launcher.ui.theme.TextSecondary

@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel = frostByteViewModel { container ->
        DownloadsViewModel(container.downloadRepository, container.downloadScheduler)
    }
) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        SpaceRenderer(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Downloads",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (downloads.any { it.status == DownloadStatus.COMPLETED }) {
                        TextButton(onClick = viewModel::clearCompleted) {
                            Text("Clear completed")
                        }
                    }
                }
            }

            if (downloads.isEmpty()) {
                item { EmptyDownloadsState() }
            } else {
                items(downloads, key = { it.id }) { item ->
                    DownloadRow(
                        item = item,
                        onRetry = { viewModel.retry(item) },
                        onCancel = { viewModel.cancel(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDownloadsState() {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Nothing queued. Downloads triggered from Versions, Mods, or Shaders will appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
private fun DownloadRow(item: DownloadItem, onRetry: () -> Unit, onCancel: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = statusLabel(item), style = MaterialTheme.typography.bodyMedium, color = statusColor(item.status))
                }
                DownloadActions(item = item, onRetry = onRetry, onCancel = onCancel)
            }
            if (item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.VERIFYING) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { item.progressFraction },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = IceBlue,
                    trackColor = TextSecondary.copy(alpha = 0.25f)
                )
            }
        }
    }
}

@Composable
private fun DownloadActions(item: DownloadItem, onRetry: () -> Unit, onCancel: () -> Unit) {
    when (item.status) {
        DownloadStatus.COMPLETED -> Icon(Icons.Filled.CheckCircle, contentDescription = "Completed", tint = StatusGreen)
        DownloadStatus.FAILED -> IconButton(onClick = onRetry) {
            Icon(Icons.Filled.Refresh, contentDescription = "Retry", tint = IceBlue)
        }
        DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING, DownloadStatus.VERIFYING, DownloadStatus.PAUSED ->
            IconButton(onClick = onCancel) {
                Icon(Icons.Filled.Cancel, contentDescription = "Cancel", tint = StatusRed)
            }
        DownloadStatus.CANCELLED -> Icon(Icons.Filled.Error, contentDescription = "Cancelled", tint = TextSecondary)
    }
}

private fun statusLabel(item: DownloadItem): String = when (item.status) {
    DownloadStatus.QUEUED -> "Queued"
    DownloadStatus.DOWNLOADING -> "Downloading · ${(item.progressFraction * 100).toInt()}%"
    DownloadStatus.VERIFYING -> "Verifying…"
    DownloadStatus.COMPLETED -> "Completed"
    DownloadStatus.FAILED -> item.errorMessage ?: "Failed"
    DownloadStatus.PAUSED -> "Paused"
    DownloadStatus.CANCELLED -> "Cancelled"
}

private fun statusColor(status: DownloadStatus) = when (status) {
    DownloadStatus.COMPLETED -> StatusGreen
    DownloadStatus.FAILED -> StatusRed
    else -> TextSecondary
}
