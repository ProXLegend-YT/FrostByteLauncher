package com.frostbyte.launcher.ui.screens.resourcepacks

import androidx.compose.runtime.Composable
import com.frostbyte.launcher.core.content.ContentType
import com.frostbyte.launcher.ui.common.frostByteViewModel
import com.frostbyte.launcher.ui.content.ContentBrowserScreen
import com.frostbyte.launcher.ui.content.ContentBrowserViewModel

@Composable
fun ResourcePacksScreen(
    viewModel: ContentBrowserViewModel = frostByteViewModel { container ->
        ContentBrowserViewModel(
            contentType = ContentType.RESOURCE_PACK,
            repository = container.modrinthContentRepository,
            downloadRepository = container.downloadRepository,
            downloadScheduler = container.downloadScheduler,
            fileManager = container.fileManager
        )
    }
) {
    ContentBrowserScreen(title = "Resource Packs", viewModel = viewModel)
}
