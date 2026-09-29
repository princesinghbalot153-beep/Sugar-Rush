package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.LevelProgressEntity
import com.example.data.UserStatsEntity
import com.example.game.model.AchievementItem
import com.example.ui.theme.CandyBlue
import com.example.ui.theme.CandyGreen
import com.example.ui.theme.CandyOrange
import com.example.ui.theme.CandyPurple
import com.example.ui.theme.CandyYellow
import com.example.ui.theme.SugarGoldStar

@Composable
fun AchievementsDialog(
    userStats: UserStatsEntity,
    progressList: List<LevelProgressEntity>,
    onClaimAchievement: (achievementId: String, reward: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val totalStars = progressList.sumOf { it.stars }
    val completedLevels = progressList.count { it.isCompleted }
    val claimedSet = userStats.claimedAchievements.split(",").filter { it.isNotBlank() }.toSet()

    val achievements = listOf(
        AchievementItem(
            id = "sugar_pioneer",
            title = "Sugar Pioneer",
            description = "Clear Level 1 in the Saga Campaign",
            iconEmoji = "🍭",
            rewardCoins = 100,
            currentProgress = completedLevels.coerceAtMost(1),
            maxProgress = 1,
            isCompleted = completedLevels >= 1,
            isClaimed = claimedSet.contains("sugar_pioneer")
        ),
        AchievementItem(
            id = "star_hunter",
            title = "Master of Stars",
            description = "Earn 15 Golden Stars across all levels",
            iconEmoji = "⭐",
            rewardCoins = 250,
            currentProgress = totalStars.coerceAtMost(15),
            maxProgress = 15,
            isCompleted = totalStars >= 15,
            isClaimed = claimedSet.contains("star_hunter")
        ),
        AchievementItem(
            id = "bomb_specialist",
            title = "Rainbow Alchemist",
            description = "Detonate 5 Color Bombs in matches",
            iconEmoji = "💣",
            rewardCoins = 200,
            currentProgress = userStats.colorBombsDetonated.coerceAtMost(5),
            maxProgress = 5,
            isCompleted = userStats.colorBombsDetonated >= 5,
            isClaimed = claimedSet.contains("bomb_specialist")
        ),
        AchievementItem(
            id = "cascade_virtuoso",
            title = "Cascade Virtuoso",
            description = "Trigger a 4x or higher Sugar Rush Combo",
            iconEmoji = "⚡",
            rewardCoins = 300,
            currentProgress = userStats.maxComboChain.coerceAtMost(4),
            maxProgress = 4,
            isCompleted = userStats.maxComboChain >= 4,
            isClaimed = claimedSet.contains("cascade_virtuoso")
        ),
        AchievementItem(
            id = "blitz_racer",
            title = "Blitz Speed Demon",
            description = "Score over 15,000 points in Blitz Rush mode",
            iconEmoji = "⏱️",
            rewardCoins = 350,
            currentProgress = userStats.blitzHighScore.coerceAtMost(15000),
            maxProgress = 15000,
            isCompleted = userStats.blitzHighScore >= 15000,
            isClaimed = claimedSet.contains("blitz_racer")
        ),
        AchievementItem(
            id = "grand_sugar_king",
            title = "Sugar Kingdom Sovereign",
            description = "Complete all 15 campaign levels",
            iconEmoji = "👑",
            rewardCoins = 1000,
            currentProgress = completedLevels.coerceAtMost(15),
            maxProgress = 15,
            isCompleted = completedLevels >= 15,
            isClaimed = claimedSet.contains("grand_sugar_king")
        )
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF220D35)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .border(3.dp, Brush.verticalGradient(listOf(CandyPurple, SugarGoldStar)), RoundedCornerShape(30.dp))
                .shadow(24.dp, RoundedCornerShape(30.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🏆", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "TROPHIES & REWARDS",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = SugarGoldStar
                            )
                            Text(
                                text = "${achievements.count { it.isCompleted }} of ${achievements.size} Unlocked",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(achievements) { item ->
                        AchievementRow(
                            item = item,
                            onClaim = { onClaimAchievement(item.id, item.rewardCoins) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementRow(
    item: AchievementItem,
    onClaim: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (item.isCompleted) Color(0x336B21A8) else Color(0x22FFFFFF),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (item.isCompleted && !item.isClaimed) SugarGoldStar else Color(0x22FFFFFF)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (item.isCompleted) {
                            Brush.radialGradient(listOf(CandyYellow, CandyOrange))
                        } else {
                            Brush.radialGradient(listOf(Color(0xFF475569), Color(0xFF1E293B)))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = item.iconEmoji, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details & Progress
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = item.description,
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1),
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (item.currentProgress.toFloat() / item.maxProgress).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (item.isCompleted) CandyGreen else CandyBlue,
                    trackColor = Color(0x44FFFFFF)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Claim / Status Button
            when {
                item.isClaimed -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x4410B981)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Claimed", tint = CandyGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "CLAIMED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CandyGreen)
                        }
                    }
                }
                item.isCompleted -> {
                    Button(
                        onClick = onClaim,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SugarGoldStar),
                        modifier = Modifier.testTag("claim_${item.id}")
                    ) {
                        Text(text = "CLAIM 🪙${item.rewardCoins}", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF1E0A28))
                    }
                }
                else -> {
                    Text(
                        text = "🪙 ${item.rewardCoins}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SugarGoldStar
                    )
                }
            }
        }
    }
}
