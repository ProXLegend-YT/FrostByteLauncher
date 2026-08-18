package com.frostbyte.launcher.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.frostbyte.launcher.FrostByteApplication
import com.frostbyte.launcher.FrostByteContainer

/**
 * Generic factory that constructs a ViewModel from the app-wide
 * FrostByteContainer. Keeps screens from needing to know how to build a
 * ProfileRepository/SettingsRepository themselves.
 *
 * The constructor lambda is named `factoryFn` rather than `create` - naming
 * it `create` would collide with the overridden `create(Class<VM>)` member
 * below and resolve to the wrong (recursive) overload at the call site.
 *
 * Usage: `viewModel(factory = frostByteViewModelFactory { container -> MyViewModel(container.profileRepository) })`
 */
class FrostByteViewModelFactory<T : ViewModel>(
    private val container: FrostByteContainer,
    private val factoryFn: (FrostByteContainer) -> T
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM {
        return factoryFn(container) as VM
    }
}

@Composable
fun <T : ViewModel> frostByteViewModel(factoryFn: (FrostByteContainer) -> T): T {
    val context = LocalContext.current
    val container = (context.applicationContext as FrostByteApplication).container
    return viewModel(factory = FrostByteViewModelFactory(container, factoryFn))
}
