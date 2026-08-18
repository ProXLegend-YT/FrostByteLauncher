package com.frostbyte.launcher.ui.screens.shaders

import androidx.compose.runtime.Composable
import com.frostbyte.launcher.core.content.ContentType
import com.frostbyte.launcher.ui.common.frostByteViewModel
import com.frostbyte.launcher.ui.content.ContentBrowserScreen
import com.frostbyte.launcher.ui.content.ContentBrowserViewModel

@Composable
fun ShadersScreen(
    viewModel: ContentBrowserViewModel = frostByteViewModel { container ->
        ContentBrowserViewModel(
            contentType = ContentType.SHADER,
            repository = container.modrinthContentRepository,
            downloadRepository = container.downloadRepository,
            downloadScheduler = container.downloadScheduler,
            fileManager = container.fileManager
        )
    }
) {
    ContentBrowserScreen(title = "Shaders", viewModel = viewModel)
}
