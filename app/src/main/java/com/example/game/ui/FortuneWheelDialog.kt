package com.example.game.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.game.audio.SoundEffects
import com.example.ui.theme.CandyBlue
import com.example.ui.theme.CandyGreen
import com.example.ui.theme.CandyOrange
import com.example.ui.theme.CandyPink
import com.example.ui.theme.CandyPurple
import com.example.ui.theme.CandyRed
import com.example.ui.theme.CandyYellow
import com.example.ui.theme.SugarGoldStar
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class WheelPrize(
    val name: String,
    val iconEmoji: String,
    val color: Color,
    val coins: Int = 0,
    val hammers: Int = 0,
    val switches: Int = 0,
    val bombs: Int = 0,
    val fullLives: Boolean = false
)

val WHEEL_PRIZES = listOf(
    WheelPrize("100 Coins", "🪙", CandyYellow, coins = 100),
    WheelPrize("Hammer", "🍭", CandyPink, hammers = 1),
    WheelPrize("250 Coins", "🪙", CandyOrange, coins = 250),
    WheelPrize("Switch", "🔄", CandyBlue, switches = 1),
    WheelPrize("500 JACKPOT", "👑", SugarGoldStar, coins = 500),
    WheelPrize("Color Bomb", "💣", CandyPurple, bombs = 1),
    WheelPrize("150 Coins", "🪙", CandyGreen, coins = 150),
    WheelPrize("Full Lives", "❤️", CandyRed, fullLives = true)
)

@Composable
fun FortuneWheelDialog(
    soundEnabled: Boolean,
    onClaimPrize: (WheelPrize) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val rotationAnim = remember { Animatable(0f) }
    var isSpinning by remember { mutableStateOf(false) }
    var wonPrize by remember { mutableStateOf<WheelPrize?>(null) }

    Dialog(onDismissRequest = { if (!isSpinning) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF220D35)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .border(3.5.dp, Brush.verticalGradient(listOf(SugarGoldStar, CandyPurple)), RoundedCornerShape(32.dp))
                .shadow(24.dp, RoundedCornerShape(32.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "LUCKY SUGAR WHEEL",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = SugarGoldStar,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Spin daily for sweet bonuses!",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1)
                        )
                    }

                    if (!isSpinning) {
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // The Wheel Stage
                Box(
                    modifier = Modifier
                        .size(270.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer Golden Ring Frame
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(listOf(SugarGoldStar, CandyOrange)),
                            radius = size.width / 2f
                        )
                        drawCircle(
                            color = Color(0xFF160624),
                            radius = size.width / 2f - 7.dp.toPx()
                        )
                    }

                    // Rotating Canvas Wheel
                    Canvas(
                        modifier = Modifier
                            .size(250.dp)
                            .rotate(rotationAnim.value)
                    ) {
                        val radius = size.width / 2f
                        val center = Offset(radius, radius)
                        val anglePerSegment = 360f / WHEEL_PRIZES.size

                        WHEEL_PRIZES.forEachIndexed { i, prize ->
                            val startAngle = i * anglePerSegment - 90f - (anglePerSegment / 2f)

                            // Segment wedge
                            drawArc(
                                brush = Brush.radialGradient(
                                    colors = listOf(prize.color, prize.color.copy(alpha = 0.8f)),
                                    center = center,
                                    radius = radius
                                ),
                                startAngle = startAngle,
                                sweepAngle = anglePerSegment,
                                useCenter = true
                            )

                            // Segment boundary line
                            val rad = Math.toRadians((startAngle + anglePerSegment).toDouble())
                            drawLine(
                                color = Color.White.copy(alpha = 0.6f),
                                start = center,
                                end = Offset(
                                    (center.x + cos(rad) * radius).toFloat(),
                                    (center.y + sin(rad) * radius).toFloat()
                                ),
                                strokeWidth = 2.dp.toPx()
                            )

                            // Prize text/icon painted
                            val midAngle = Math.toRadians((startAngle + anglePerSegment / 2f).toDouble())
                            val textDistance = radius * 0.62f
                            val textX = (center.x + cos(midAngle) * textDistance).toFloat()
                            val textY = (center.y + sin(midAngle) * textDistance).toFloat()

                            drawContext.canvas.nativeCanvas.apply {
                                val paint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.WHITE
                                    textSize = 34f
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isFakeBoldText = true
                                    setShadowLayer(4f, 0f, 2f, android.graphics.Color.BLACK)
                                }
                                drawText(prize.iconEmoji, textX, textY + 12f, paint)
                            }
                        }

                        // Wheel border rim
                        drawCircle(
                            color = Color.White.copy(alpha = 0.8f),
                            radius = radius,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }

                    // Center Hub Button / Jewel
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(Color.White, SugarGoldStar, CandyOrange)))
                            .border(3.dp, Color.White, CircleShape)
                            .shadow(8.dp, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "⭐", fontSize = 24.sp)
                    }

                    // Top Wheel Pointer Pin
                    Canvas(
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.TopCenter)
                            .shadow(6.dp)
                    ) {
                        val path = Path().apply {
                            moveTo(size.width / 2f, size.height)
                            lineTo(0f, 0f)
                            lineTo(size.width, 0f)
                            close()
                        }
                        drawPath(path = path, color = Color.White)
                        drawPath(path = path, color = CandyRed, style = Stroke(width = 3.dp.toPx()))
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Result Card when settled
                AnimatedVisibility(
                    visible = wonPrize != null,
                    enter = scaleIn() + fadeIn()
                ) {
                    wonPrize?.let { prize ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0x44FFFFFF),
                            border = androidx.compose.foundation.BorderStroke(2.dp, prize.color),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(text = prize.iconEmoji, fontSize = 32.sp)
                                Spacer(modifier = Modifier.size(10.dp))
                                Column {
                                    Text(
                                        text = "YOU WON!",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = SugarGoldStar
                                    )
                                    Text(
                                        text = prize.name,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Spin / Collect Button
                if (wonPrize == null) {
                    Button(
                        onClick = {
                            if (isSpinning) return@Button
                            isSpinning = true
                            wonPrize = null

                            scope.launch {
                                val targetPrizeIndex = Random.nextInt(WHEEL_PRIZES.size)
                                val segmentAngle = 360f / WHEEL_PRIZES.size

                                // Calculate rotation angle to align the winning wedge at the top pointer (0 deg)
                                val baseSpins = 360f * 6
                                val targetAngle = baseSpins + (360f - (targetPrizeIndex * segmentAngle))
                                var lastTickDegrees = 0f

                                rotationAnim.snapTo(0f)
                                rotationAnim.animateTo(
                                    targetValue = targetAngle,
                                    animationSpec = tween(3800, easing = FastOutSlowInEasing)
                                ) {
                                    if (value - lastTickDegrees >= segmentAngle) {
                                        lastTickDegrees = value
                                        if (soundEnabled) SoundEffects.playWheelTick()
                                    }
                                }

                                val prize = WHEEL_PRIZES[targetPrizeIndex]
                                wonPrize = prize
                                isSpinning = false
                                if (soundEnabled) SoundEffects.playChime()
                            }
                        },
                        enabled = !isSpinning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("spin_wheel_button"),
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CandyGreen)
                    ) {
                        Text(
                            text = if (isSpinning) "SPINNING..." else "SPIN THE WHEEL! 🎡",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            wonPrize?.let { onClaimPrize(it) }
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("claim_wheel_reward_button"),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CandyOrange)
                    ) {
                        Text(
                            text = "CLAIM REWARD & ENJOY",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}
