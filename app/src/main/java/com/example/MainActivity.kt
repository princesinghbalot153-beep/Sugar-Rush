package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
                        viewModel.startLevel(levelNum)
                        currentScreen = ScreenState.GAME
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
                BackHandler {
                    currentScreen = ScreenState.MAP
                }

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
}
