package com.frostbyte.launcher.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.frostbyte.launcher.ui.space.SpaceRenderer
import com.frostbyte.launcher.ui.theme.TextSecondary

/**
 * Shared scaffold for sections whose real functionality lands in a later
 * development phase (see Section 36 of the PRD). Keeps the cosmic background
 * and navigation fully working end-to-end in Phase 1, while being explicit
 * that the feature body itself isn't built yet - deliberately NOT faking
 * data here, per Section 37 ("No fake core features remain").
 */
@Composable
fun PlaceholderScreen(title: String, plannedInPhase: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        SpaceRenderer(modifier = Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            GlassCard {
                Text(
                    text = "Coming in $plannedInPhase",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}
