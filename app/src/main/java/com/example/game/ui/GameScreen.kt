package com.example.game.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserStatsEntity
import com.example.game.model.BoosterType
import com.example.game.model.Cell
import com.example.game.model.FloatingScore
import com.example.game.model.GameParticle
import com.example.game.model.LevelConfig
import com.example.game.model.LevelGoalType
import com.example.game.model.Position
import com.example.ui.theme.BoardCellAlternate
import com.example.ui.theme.BoardCellBg
import com.example.ui.theme.CandyBlue
import com.example.ui.theme.CandyGreen
import com.example.ui.theme.CandyOrange
import com.example.ui.theme.CandyOrangeDark
import com.example.ui.theme.CandyPink
import com.example.ui.theme.CandyPurple
import com.example.ui.theme.CandyRed
import com.example.ui.theme.CandyYellow
import com.example.ui.theme.JellyOverlay
import com.example.ui.theme.SugarGoldStar
import kotlin.math.abs

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    uiState: GameUiState,
    userStats: UserStatsEntity?,
    onBackToMap: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF2A1137),
                        Color(0xFF1B0B26),
                        Color(0xFF100518)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Deluxe Top HUD
            GameTopBar(
                uiState = uiState,
                onBack = onBackToMap
            )

            // Star Progress Bar with 3 Star checkpoints
            ScoreProgressBar(
                score = uiState.score,
                level = uiState.currentLevel,
                stars = uiState.starsEarned
            )

            // Neon Sugar Fever Gauge
            SugarFeverBar(
                progress = uiState.feverProgress,
                isActive = uiState.isFeverActive
            )

            // Active Booster Banner
            if (uiState.activeBooster != null) {
                ActiveBoosterPrompt(
                    booster = uiState.activeBooster,
                    onCancel = { viewModel.activateBooster(uiState.activeBooster) }
                )
            }

            // Sugar Crush Finale Mode Banner
            if (uiState.gameStatus == GameStatus.SUGAR_CRUSH_BONUS) {
                SugarCrushModeBanner(remainingMoves = uiState.sugarCrushMovesRemaining)
            }

            // Interactive Candy Crush Game Board with Dynamic Screen Shake
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .offset(x = uiState.screenShakeX.dp, y = uiState.screenShakeY.dp)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                CandyBoardView(
                    board = uiState.board,
                    rows = uiState.currentLevel.rows,
                    cols = uiState.currentLevel.cols,
                    onCellClicked = { viewModel.onCellClicked(it) }
                )

                // Particle explosion layer
                ParticleCanvas(particles = uiState.particles)

                // Futuristic Quantum Laser Beams
                LaserBeamsCanvas(
                    laserBeams = uiState.laserBeams,
                    rows = uiState.currentLevel.rows,
                    cols = uiState.currentLevel.cols
                )

                // Cosmic Shockwave Rings
                ShockwavesCanvas(shockwaves = uiState.shockwaves)

                // Floating upward scores (+120, +500)
                FloatingScoresOverlay(scores = uiState.floatingScores)

                // Arcade Combo Banner
                ComboBannerOverlay(banner = uiState.comboBanner)

                // Reshuffling Notice
                if (uiState.isReshuffling) {
                    ReshufflingOverlay()
                }
            }

            // Bottom Booster Dock
            BoostersTray(
                userStats = userStats,
                activeBooster = uiState.activeBooster,
                onSelectBooster = { viewModel.activateBooster(it) }
            )
        }

        // End-of-Level Dialogs
        when (uiState.gameStatus) {
            GameStatus.LEVEL_WON -> {
                VictoryDialog(
                    score = uiState.score,
                    stars = uiState.starsEarned,
                    levelNumber = uiState.currentLevel.levelNumber,
                    playTimeSec = uiState.elapsedPlayTimeSec,
                    onNextLevel = { viewModel.startLevel(uiState.currentLevel.levelNumber + 1) },
                    onReplay = { viewModel.startLevel(uiState.currentLevel.levelNumber) },
                    onBackToMap = onBackToMap
                )
            }
            GameStatus.LEVEL_FAILED -> {
                GameOverDialog(
                    score = uiState.score,
                    targetScore = uiState.currentLevel.targetScore,
                    extraMovesAvailable = userStats?.extraMoves ?: 0,
                    playTimeSec = uiState.elapsedPlayTimeSec,
                    onUseExtraMoves = { viewModel.activateBooster(BoosterType.EXTRA_MOVES) },
                    onRetry = { viewModel.startLevel(uiState.currentLevel.levelNumber) },
                    onBackToMap = onBackToMap
                )
            }
            else -> {}
        }
    }
}

@Composable
private fun GameTopBar(
    uiState: GameUiState,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Back Button & Live Level Session Timer
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF))
                    .border(1.5.dp, Color(0x44FFFFFF), CircleShape)
                    .testTag("back_to_map_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            val elapsedMins = uiState.elapsedPlayTimeSec / 60
            val elapsedSecs = uiState.elapsedPlayTimeSec % 60
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x33FFFFFF),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "⏱️", fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = String.format(java.util.Locale.getDefault(), "%02d:%02d", elapsedMins, elapsedSecs),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Central Moves or Blitz Timer Badge
        val infiniteTransition = rememberInfiniteTransition(label = "moves_glow")
        val glowScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "moves_pulse"
        )

        val isUrgent = (uiState.isBlitzMode && uiState.blitzTimeRemainingSec <= 10) || (!uiState.isBlitzMode && uiState.movesLeft <= 5)

        Box(
            modifier = Modifier
                .size(74.dp)
                .scale(if (isUrgent) glowScale else 1f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        if (isUrgent) {
                            listOf(CandyRed, Color(0xFF991B1B))
                        } else if (uiState.isBlitzMode) {
                            listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
                        } else {
                            listOf(CandyYellow, CandyOrangeDark)
                        }
                    )
                )
                .border(3.dp, Color.White, CircleShape)
                .shadow(10.dp, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (uiState.isBlitzMode) "${uiState.blitzTimeRemainingSec}s" else "${uiState.movesLeft}",
                    fontSize = if (uiState.isBlitzMode) 22.sp else 26.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = if (uiState.isBlitzMode) "BLITZ" else "MOVES",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFFFBEB),
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Target Objective Chips
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0x33FFFFFF),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
            modifier = Modifier.padding(start = 6.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.isBlitzMode) {
                    Text(text = "⚡ ", fontSize = 16.sp)
                    Column {
                        Text(
                            text = "SCORE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CandyYellow
                        )
                        Text(
                            text = "${uiState.score}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = SugarGoldStar
                        )
                    }
                } else when (uiState.currentLevel.goalType) {
                    LevelGoalType.CLEAR_JELLY -> {
                        Text(text = "🍧 ", fontSize = 16.sp)
                        Column {
                            Text(
                                text = "JELLY",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFBCFE8)
                            )
                            Text(
                                text = "${uiState.jellyRemaining}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = CandyPink
                            )
                        }
                    }
                    LevelGoalType.SCORE -> {
                        Text(text = "🎯 ", fontSize = 16.sp)
                        Column {
                            Text(
                                text = "GOAL",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFEF08A)
                            )
                            Text(
                                text = "${uiState.score}/${uiState.currentLevel.targetScore}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = SugarGoldStar
                            )
                        }
                    }
                    LevelGoalType.COLLECT_CANDIES -> {
                        uiState.candyGoals.forEach { goal ->
                            val remaining = (goal.targetCount - goal.currentCount).coerceAtLeast(0)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(goal.type.mainColor)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "$remaining",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (remaining == 0) CandyGreen else Color.White
                                )
                            }
                        }
                    }
                    LevelGoalType.CREATE_SPECIALS -> {
                        Text(text = "⭐ Specials Goal", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CandyYellow)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreProgressBar(
    score: Int,
    level: LevelConfig,
    stars: Int
) {
    val maxProgressTarget = level.star3Score.toFloat().coerceAtLeast(1f)
    val animatedProgress by animateFloatAsState(
        targetValue = (score.toFloat() / maxProgressTarget).coerceIn(0f, 1f),
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "score_progress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Score: $score",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (i in 1..3) {
                    val isEarned = i <= stars
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Star $i",
                        tint = if (isEarned) SugarGoldStar else Color(0x33FFFFFF),
                        modifier = Modifier
                            .size(20.dp)
                            .scale(if (isEarned) 1.15f else 1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0x33FFFFFF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(CandyOrange, CandyYellow, SugarGoldStar)
                        )
                    )
            )
        }
    }
}

@Composable
private fun SugarFeverBar(
    progress: Float,
    isActive: Boolean
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300),
        label = "fever_progress"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) Color(0x66FF1493) else Color(0x33FFFFFF),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isActive) SugarGoldStar else Color(0x33FFFFFF)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isActive) "🔥 HYPER FRENZY (2X SCORE ACTIVE)!" else "🍬 SUGAR FRENZY GAUGE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isActive) SugarGoldStar else Color.White
                )
                Text(
                    text = if (isActive) "OVERDRIVE!" else "${(progress * 100).toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) CandyYellow else Color(0xFFE2E8F0)
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0x33000000))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (isActive) {
                                Brush.horizontalGradient(listOf(CandyRed, CandyYellow, SugarGoldStar, CandyPink))
                            } else {
                                Brush.horizontalGradient(listOf(CandyPurple, CandyPink, CandyYellow))
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun ActiveBoosterPrompt(
    booster: BoosterType,
    onCancel: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF6B21A8),
        border = androidx.compose.foundation.BorderStroke(2.dp, CandyYellow),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = booster.iconEmoji, fontSize = 22.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (booster) {
                        BoosterType.LOLLIPOP_HAMMER -> "Tap any candy or jelly to smash it!"
                        BoosterType.FREE_SWITCH -> "Tap 2 adjacent candies to swap freely!"
                        BoosterType.COLOR_BOMB -> "Tap any cell to place a Color Bomb!"
                        else -> "Booster Active"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Text(
                text = "CANCEL",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = CandyYellow,
                modifier = Modifier.clickable { onCancel() }
            )
        }
    }
}

@Composable
private fun SugarCrushModeBanner(remainingMoves: Int) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFBE185D),
        border = androidx.compose.foundation.BorderStroke(2.dp, SugarGoldStar),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "🎆 ", fontSize = 20.sp)
            Text(
                text = "SUGAR CRUSH! $remainingMoves moves converting to bonus points!",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

@Composable
private fun CandyBoardView(
    board: List<List<Cell>>,
    rows: Int,
    cols: Int,
    onCellClicked: (Position) -> Unit
) {
    if (board.isEmpty()) return

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x88170624)),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .border(3.5.dp, Brush.verticalGradient(listOf(Color(0xFF9333EA), Color(0xFF4C1D95))), RoundedCornerShape(22.dp))
            .shadow(16.dp, RoundedCornerShape(22.dp))
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (r in 0 until rows) {
                    Row(modifier = Modifier.weight(1f)) {
                        for (c in 0 until cols) {
                            val cell = board.getOrNull(r)?.getOrNull(c)
                            val isPlayable = cell?.isPlayable ?: false
                            val isAlt = (r + c) % 2 == 0

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .padding(1.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isPlayable) {
                                            if (isAlt) BoardCellBg else BoardCellAlternate
                                        } else {
                                            Color.Transparent
                                        }
                                    )
                                    .then(
                                        if (isPlayable) {
                                            Modifier.pointerInput(cell?.position) {
                                                var totalDx = 0f
                                                var totalDy = 0f
                                                var hasTriggeredSwipe = false

                                                detectDragGestures(
                                                    onDragStart = {
                                                        totalDx = 0f
                                                        totalDy = 0f
                                                        hasTriggeredSwipe = false
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        if (hasTriggeredSwipe) return@detectDragGestures
                                                        totalDx += dragAmount.x
                                                        totalDy += dragAmount.y

                                                        val threshold = 32f
                                                        if (abs(totalDx) > threshold || abs(totalDy) > threshold) {
                                                            hasTriggeredSwipe = true
                                                            val targetPos = if (abs(totalDx) > abs(totalDy)) {
                                                                if (totalDx > 0) Position(r, c + 1) else Position(r, c - 1)
                                                            } else {
                                                                if (totalDy > 0) Position(r + 1, c) else Position(r - 1, c)
                                                            }
                                                            if (targetPos.row in 0 until rows && targetPos.col in 0 until cols) {
                                                                onCellClicked(Position(r, c))
                                                                onCellClicked(targetPos)
                                                            }
                                                        }
                                                    },
                                                    onDragEnd = {
                                                        if (!hasTriggeredSwipe) {
                                                            onCellClicked(Position(r, c))
                                                        }
                                                    }
                                                )
                                            }
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                // Draw Jelly layer if present
                                if (cell != null && cell.hasJelly) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (cell.jellyLevel >= 2) Color(0xAAFF1493) else JellyOverlay
                                            )
                                            .border(
                                                1.dp,
                                                if (cell.jellyLevel >= 2) Color(0xFFFF69B4) else Color(0x77FF69B4),
                                                RoundedCornerShape(8.dp)
                                            )
                                    )
                                }

                                // Draw Candy with glossy rendering
                                if (cell?.candy != null) {
                                    CandyView(tile = cell.candy)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticleCanvas(particles: List<GameParticle>) {
    if (particles.isEmpty()) return

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            drawCircle(
                color = p.color.copy(alpha = p.alpha),
                radius = p.size,
                center = Offset(p.x * size.width, p.y * size.height)
            )
        }
    }
}

@Composable
private fun LaserBeamsCanvas(
    laserBeams: List<com.example.game.model.LaserBeamEffect>,
    rows: Int,
    cols: Int
) {
    if (laserBeams.isEmpty()) return

    laserBeams.forEach { beam ->
        val progress = remember(beam.id) { Animatable(0f) }
        LaunchedEffect(beam.id) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(280, easing = FastOutSlowInEasing)
            )
        }

        val animVal = progress.value
        val alpha = (1f - animVal).coerceIn(0f, 1f)

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (beam.isHorizontal) {
                val y = (beam.index + 0.5f) / rows * size.height
                val span = size.width * (animVal * 2f).coerceAtMost(1f)
                val startX = (size.width / 2f) - (span / 2f)
                val endX = (size.width / 2f) + (span / 2f)

                // Outer beam glow
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, beam.color.copy(alpha = 0.5f * alpha), Color.Transparent),
                        startX = startX,
                        endX = endX
                    ),
                    start = Offset(startX, y),
                    end = Offset(endX, y),
                    strokeWidth = (22.dp.toPx() * (1f - animVal * 0.4f))
                )
                // Core brilliant beam
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = alpha), Color.Transparent),
                        startX = startX,
                        endX = endX
                    ),
                    start = Offset(startX, y),
                    end = Offset(endX, y),
                    strokeWidth = (9.dp.toPx() * (1f - animVal * 0.4f))
                )
            } else {
                val x = (beam.index + 0.5f) / cols * size.width
                val span = size.height * (animVal * 2f).coerceAtMost(1f)
                val startY = (size.height / 2f) - (span / 2f)
                val endY = (size.height / 2f) + (span / 2f)

                // Outer beam glow
                drawLine(
                    brush = Brush.verticalGradient(
                        listOf(Color.Transparent, beam.color.copy(alpha = 0.5f * alpha), Color.Transparent),
                        startY = startY,
                        endY = endY
                    ),
                    start = Offset(x, startY),
                    end = Offset(x, endY),
                    strokeWidth = (22.dp.toPx() * (1f - animVal * 0.4f))
                )
                // Core brilliant beam
                drawLine(
                    brush = Brush.verticalGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = alpha), Color.Transparent),
                        startY = startY,
                        endY = endY
                    ),
                    start = Offset(x, startY),
                    end = Offset(x, endY),
                    strokeWidth = (9.dp.toPx() * (1f - animVal * 0.4f))
                )
            }
        }
    }
}

@Composable
private fun ShockwavesCanvas(shockwaves: List<com.example.game.model.ShockwaveRingEffect>) {
    if (shockwaves.isEmpty()) return

    shockwaves.forEach { wave ->
        val progress = remember(wave.id) { Animatable(0f) }
        LaunchedEffect(wave.id) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(340, easing = FastOutSlowInEasing)
            )
        }

        val animVal = progress.value
        val alpha = (1f - animVal).coerceIn(0f, 1f)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(wave.centerX * size.width, wave.centerY * size.height)
            val currentRadius = size.width * 0.75f * animVal

            // Outer chromatic aura ring
            drawCircle(
                color = wave.color.copy(alpha = 0.6f * alpha),
                radius = currentRadius,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = (12.dp.toPx() * (1f - animVal * 0.5f)))
            )

            // Inner intense white ring
            drawCircle(
                color = Color.White.copy(alpha = 0.9f * alpha),
                radius = currentRadius * 0.75f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = (5.dp.toPx() * (1f - animVal * 0.5f)))
            )
        }
    }
}

@Composable
private fun FloatingScoresOverlay(scores: List<FloatingScore>) {
    Box(modifier = Modifier.fillMaxSize()) {
        scores.forEach { score ->
            var animY by remember(score.id) { mutableFloatStateOf(0f) }
            var alpha by remember(score.id) { mutableFloatStateOf(1f) }

            LaunchedEffect(score.id) {
                val anim = Animatable(0f)
                anim.animateTo(1f, animationSpec = tween(900, easing = LinearEasing)) {
                    animY = value * -60f
                    alpha = (1f - (value * 0.9f)).coerceIn(0f, 1f)
                }
            }

            Box(
                modifier = Modifier
                    .offset(
                        x = ((score.col + 0.5f) / 8f * 320f).dp,
                        y = ((score.row + 0.5f) / 8f * 320f + animY).dp
                    )
            ) {
                Text(
                    text = score.text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = score.color.copy(alpha = alpha),
                    modifier = Modifier.shadow(4.dp)
                )
            }
        }
    }
}

@Composable
private fun ComboBannerOverlay(banner: com.example.game.model.ComboBanner?) {
    AnimatedVisibility(
        visible = banner != null,
        enter = scaleIn(spring(dampingRatio = 0.6f)) + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        if (banner != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = Color(0xF21C0A2E),
                    border = androidx.compose.foundation.BorderStroke(3.5.dp, banner.color),
                    modifier = Modifier.shadow(20.dp, RoundedCornerShape(26.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = banner.text,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black,
                            color = banner.color,
                            letterSpacing = 1.sp
                        )
                        if (banner.subText.isNotEmpty()) {
                            Text(
                                text = banner.subText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReshufflingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88000000)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF2E1045),
            border = androidx.compose.foundation.BorderStroke(2.dp, CandyYellow)
        ) {
            Text(
                text = "Reshuffling Candies...",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SugarGoldStar,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)
            )
        }
    }
}

@Composable
private fun BoostersTray(
    userStats: UserStatsEntity?,
    activeBooster: BoosterType?,
    onSelectBooster: (BoosterType) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0x33FFFFFF),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BoosterButton(
                booster = BoosterType.LOLLIPOP_HAMMER,
                count = userStats?.lollipopHammers ?: 0,
                isActive = activeBooster == BoosterType.LOLLIPOP_HAMMER,
                onClick = { onSelectBooster(BoosterType.LOLLIPOP_HAMMER) }
            )

            BoosterButton(
                booster = BoosterType.FREE_SWITCH,
                count = userStats?.freeSwitches ?: 0,
                isActive = activeBooster == BoosterType.FREE_SWITCH,
                onClick = { onSelectBooster(BoosterType.FREE_SWITCH) }
            )

            BoosterButton(
                booster = BoosterType.COLOR_BOMB,
                count = userStats?.colorBombs ?: 0,
                isActive = activeBooster == BoosterType.COLOR_BOMB,
                onClick = { onSelectBooster(BoosterType.COLOR_BOMB) }
            )

            BoosterButton(
                booster = BoosterType.EXTRA_MOVES,
                count = userStats?.extraMoves ?: 0,
                isActive = false,
                onClick = { onSelectBooster(BoosterType.EXTRA_MOVES) }
            )
        }
    }
}

@Composable
private fun BoosterButton(
    booster: BoosterType,
    count: Int,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
            .testTag("booster_${booster.name.lowercase()}")
    ) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = if (count > 0) CandyGreen else Color.Gray,
                    contentColor = Color.White
                ) {
                    Text(text = "$count", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) {
                            Brush.radialGradient(listOf(CandyPink, CandyPurple))
                        } else {
                            Brush.radialGradient(listOf(Color(0x55FFFFFF), Color(0x22FFFFFF)))
                        }
                    )
                    .border(
                        2.5.dp,
                        if (isActive) SugarGoldStar else Color(0x33FFFFFF),
                        CircleShape
                    )
                    .shadow(if (isActive) 8.dp else 2.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = booster.iconEmoji, fontSize = 23.sp)
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = booster.title.take(8),
            fontSize = 10.sp,
            color = if (isActive) SugarGoldStar else Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}
