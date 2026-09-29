package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import kotlinx.coroutines.delay
import com.example.data.GameRepository
import com.example.data.UserStatsEntity
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.game.ui.GameScreen
import com.example.game.ui.GameViewModel
import com.example.game.ui.SagaMapScreen
import com.example.ui.theme.MyApplicationTheme

enum class ScreenState {
    MAP,
    GAME
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SugarRushApp()
                }
            }
        }
    }
}

@Composable
fun SugarRushApp(
    viewModel: GameViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf(ScreenState.MAP) }
    var showNoLives by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allLevels by viewModel.allLevels.collectAsStateWithLifecycle()
    val userStats by viewModel.userStats.collectAsStateWithLifecycle()

    Crossfade(
        targetState = currentScreen,
        animationSpec = tween(350),
        label = "screen_crossfade"
    ) { screen ->
        when (screen) {
            ScreenState.MAP -> {
                SagaMapScreen(
                    userStats = userStats,
                    progressList = allLevels,
                    onSelectLevel = { levelNum ->
                        if (viewModel.hasLives()) {
                            viewModel.startLevel(levelNum)
                            currentScreen = ScreenState.GAME
                        } else {
                            showNoLives = true
                        }
                    },
                    onStartBlitz = {
                        viewModel.startBlitzMode()
                        currentScreen = ScreenState.GAME
                    },
                    onBuyBooster = { booster ->
                        viewModel.buyBooster(booster)
                    },
                    onClaimWheelPrize = { prize ->
                        viewModel.awardWheelPrize(prize)
                    },
                    onClaimAchievement = { id, reward ->
                        viewModel.claimAchievement(id, reward)
                    },
                    onToggleHaptics = {
                        viewModel.toggleHaptics()
                    },
                    onToggleSound = {
                        viewModel.toggleSound()
                    }
                )
            }
            ScreenState.GAME -> {
                GameScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    userStats = userStats,
                    onBackToMap = {
                        currentScreen = ScreenState.MAP
                    }
                )
            }
        }
    }

    if (showNoLives) {
        NoLivesDialog(
            userStats = userStats,
            onBuy = {
                viewModel.buyLives()
                showNoLives = false
            },
            onDismiss = { showNoLives = false }
        )
    }
}

@Composable
private fun NoLivesDialog(
    userStats: UserStatsEntity?,
    onBuy: () -> Unit,
    onDismiss: () -> Unit
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }
    LaunchedEffect(userStats?.lives) {
        if ((userStats?.lives ?: 0) > 0) onDismiss()
    }

    val last = userStats?.lastLifeRegenTimestamp ?: now
    val remainingMs = (GameRepository.LIFE_REGEN_MS - (now - last)).coerceAtLeast(0L)
    val minutes = remainingMs / 60000
    val seconds = (remainingMs % 60000) / 1000
    val canAfford = (userStats?.coins ?: 0) >= GameRepository.LIVES_REFILL_COST

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Out of lives! 💔") },
        text = {
            Text(
                "Next life in %d:%02d.\nOr refill all your lives right now for %d coins."
                    .format(minutes, seconds, GameRepository.LIVES_REFILL_COST)
            )
        },
        confirmButton = {
            TextButton(onClick = onBuy, enabled = canAfford) {
                Text("Refill (${GameRepository.LIVES_REFILL_COST} 🪙)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Wait") }
        }
    )
}
