package com.frostbyte.launcher.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.frostbyte.launcher.FrostByteApplication
import com.frostbyte.launcher.FrostByteContainer

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
inline fun <reified T : ViewModel> frostByteViewModel(noinline factoryFn: (FrostByteContainer) -> T): T {
    val context = LocalContext.current
    val container = (context.applicationContext as FrostByteApplication).container
    val owner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    }
    val factory = remember(container) { FrostByteViewModelFactory(container, factoryFn) }
    val provider = remember(owner, factory) { ViewModelProvider(owner, factory) }
    return provider[T::class.java]
}
