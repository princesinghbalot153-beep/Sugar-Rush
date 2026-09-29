package com.example.game.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.UserStatsEntity
import com.example.game.model.BoosterType
import com.example.game.model.LevelConfig
import com.example.game.model.LevelGoalType
import com.example.ui.theme.CandyBlue
import com.example.ui.theme.CandyGreen
import com.example.ui.theme.CandyOrange
import com.example.ui.theme.CandyPink
import com.example.ui.theme.CandyPurple
import com.example.ui.theme.CandyRed
import com.example.ui.theme.CandyYellow
import com.example.ui.theme.SugarGoldStar
import kotlinx.coroutines.delay

@Composable
fun LevelPreviewDialog(
    level: LevelConfig,
    highScore: Int,
    stars: Int,
    onDismiss: () -> Unit,
    onPlay: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF261238)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(3.dp, Brush.verticalGradient(listOf(CandyPink, CandyPurple)), RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header badge
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(CandyYellow, CandyOrange))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${level.levelNumber}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = level.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))
                // Existing stars
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 1..3) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Star $i",
                            tint = if (i <= stars) SugarGoldStar else Color(0x44FFFFFF),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                if (highScore > 0) {
                    Text(
                        text = "High Score: $highScore",
                        fontSize = 14.sp,
                        color = SugarGoldStar,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Level Objective Details Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x33FFFFFF),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TARGET GOAL",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CandyYellow,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = level.goalTypes.joinToString("\n") { goal ->
                                when (goal) {
                                    LevelGoalType.SCORE -> "Reach ${level.targetScore} Points"
                                    LevelGoalType.CLEAR_JELLY -> "Clear ${level.totalJellies} Jelly Layers"
                                    LevelGoalType.COLLECT_CANDIES -> "Collect target coloured candies"
                                    LevelGoalType.CREATE_SPECIALS -> "Create special candies"
                                    LevelGoalType.DROP_INGREDIENTS -> "Bring ${level.ingredientTarget} ingredients down 🍒"
                                }
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Moves Allowed: ${level.maxMoves}",
                            fontSize = 14.sp,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = level.storyDescription,
                    fontSize = 13.sp,
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Play Button
                Button(
                    onClick = onPlay,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("play_level_button"),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CandyGreen)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "PLAY", fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun VictoryDialog(
    score: Int,
    stars: Int,
    levelNumber: Int,
    playTimeSec: Int = 0,
    onNextLevel: () -> Unit,
    onReplay: () -> Unit,
    onBackToMap: () -> Unit
) {
    var animatedStars by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        for (i in 1..stars) {
            delay(350)
            animatedStars = i
        }
    }

    Dialog(onDismissRequest = onBackToMap) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF231033)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(4.dp, Brush.verticalGradient(listOf(SugarGoldStar, CandyOrange)), RoundedCornerShape(32.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SWEET VICTORY!",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = SugarGoldStar,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Level $levelNumber Completed",
                    fontSize = 14.sp,
                    color = Color(0xFFCBD5E1)
                )

                if (playTimeSec > 0) {
                    val m = playTimeSec / 60
                    val s = playTimeSec % 60
                    Text(
                        text = "⏱️ Play Time: ${m}m ${s}s",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Animated Stars
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..3) {
                        val isLit = i <= animatedStars
                        val starScale by animateFloatAsState(
                            targetValue = if (isLit) 1.25f else 0.85f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "star_anim_$i"
                        )
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Star $i",
                            tint = if (isLit) SugarGoldStar else Color(0x33FFFFFF),
                            modifier = Modifier
                                .size(44.dp)
                                .scale(starScale)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Score card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x22FFFFFF),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "FINAL SCORE", fontSize = 12.sp, color = CandyPink, fontWeight = FontWeight.Bold)
                        Text(
                            text = "$score",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "+${50 + stars * 25} Coins Earned!",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SugarGoldStar
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Button(
                    onClick = onNextLevel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("next_level_button"),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CandyGreen)
                ) {
                    Text(text = "NEXT LEVEL", fontSize = 16.sp, fontWeight = FontWeight.Black)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onReplay,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("replay_button"),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Replay")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Replay", color = Color.White)
                    }
                    Button(
                        onClick = onBackToMap,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("map_button"),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CandyPurple)
                    ) {
                        Text("Map", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun GameOverDialog(
    score: Int,
    targetScore: Int,
    extraMovesAvailable: Int,
    playTimeSec: Int = 0,
    onUseExtraMoves: () -> Unit,
    onRetry: () -> Unit,
    onBackToMap: () -> Unit
) {
    Dialog(onDismissRequest = onBackToMap) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF28112B)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(3.dp, CandyRed, RoundedCornerShape(32.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "OUT OF MOVES!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = CandyRed,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Don't give up! Sweet victory is close.",
                    fontSize = 13.sp,
                    color = Color(0xFFE2E8F0),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                if (playTimeSec > 0) {
                    val m = playTimeSec / 60
                    val s = playTimeSec % 60
                    Text(
                        text = "⏱️ Play Time: ${m}m ${s}s",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x22FFFFFF),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Score Achieved", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        Text(
                            text = "$score / $targetScore",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (extraMovesAvailable > 0) {
                    Button(
                        onClick = onUseExtraMoves,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("use_extra_moves_button"),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CandyGreen)
                    ) {
                        Text(text = "USE +5 MOVES ($extraMovesAvailable left)", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("retry_level_button"),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CandyOrange)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "TRY AGAIN", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onBackToMap,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("return_map_button"),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Text(text = "Return to Map", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ShopDialog(
    userStats: UserStatsEntity,
    onBuyBooster: (BoosterType) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF201131)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(2.dp, CandyPurple, RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CANDY BOOSTER SHOP",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = SugarGoldStar
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Coins balance banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x33FFD700),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "🪙 Current Coins: ", fontSize = 16.sp, color = Color.White)
                        Text(
                            text = "${userStats.coins}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = SugarGoldStar
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Boosters catalog
                BoosterType.entries.forEach { booster ->
                    ShopItemRow(
                        booster = booster,
                        userCoins = userStats.coins,
                        onBuy = { onBuyBooster(booster) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun ShopItemRow(
    booster: BoosterType,
    userCoins: Int,
    onBuy: () -> Unit
) {
    val canAfford = userCoins >= booster.coinCost

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0x22FFFFFF),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(text = booster.iconEmoji, fontSize = 28.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = booster.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = booster.description,
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1),
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onBuy,
                enabled = canAfford,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CandyGreen),
                modifier = Modifier.testTag("buy_${booster.name.lowercase()}")
            ) {
                Text(text = "🪙 ${booster.coinCost}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DailyRewardDialog(
    onClaim: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF261238)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(3.dp, SugarGoldStar, RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "🎁", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "DAILY SUGAR TREAT!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = SugarGoldStar
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Claim your free daily gift basket packed with coins and replenished heart lives!",
                    fontSize = 13.sp,
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x33FFFFFF),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "+150 Free Coins 🪙", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SugarGoldStar)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Full Heart Lives ❤️❤️❤️❤️❤️", fontSize = 14.sp, color = CandyRed)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        onClaim()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("claim_daily_gift_button"),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CandyGreen)
                ) {
                    Text(text = "CLAIM REWARD", fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
