package com.frostbyte.launcher.core.controls

import android.content.Context
import android.hardware.input.InputManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A CI/emulator environment typically has zero real gamepads attached, so
 * this test intentionally does NOT assert on the exact list contents (that
 * would require physical/virtual controller hardware) - it verifies the
 * class runs against the REAL Android InputManager without crashing and
 * returns a well-formed (possibly empty) result, which is what's actually
 * verifiable in this environment.
 */
@RunWith(AndroidJUnit4::class)
class GamepadDetectorTest {

    @Test
    fun listConnectedGamepads_runsAgainstRealInputManagerWithoutCrashing() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager
        val detector = GamepadDetector(inputManager)

        val gamepads = detector.listConnectedGamepads()

        assertNotNull(gamepads) // real call, real (possibly empty) result - no exception, no fake data
    }
}
