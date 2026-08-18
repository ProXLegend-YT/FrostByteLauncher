package com.frostbyte.launcher

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Application entry point. Holds the manual service locator (FrostByteContainer).
 *
 * Crash reporting (Phase 9) is installed/uninstalled live as the user
 * toggles it in Settings, per Section 27 (OFF by default, explicit opt-in
 * required) - it is never force-installed regardless of the setting.
 */
class FrostByteApplication : Application() {

    lateinit var container: FrostByteContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = FrostByteContainer(this)

        applicationScope.launch {
            container.settingsRepository.settings.collectLatest { settings ->
                if (settings.crashReportsEnabled) {
                    container.crashReporter.install()
                } else {
                    container.crashReporter.uninstall()
                }
            }
        }
    }
}
