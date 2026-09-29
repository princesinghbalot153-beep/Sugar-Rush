package com.example.game.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.LevelProgressEntity
import com.example.data.UserStatsEntity
import com.example.game.levels.LevelsCatalog
import com.example.game.model.BoosterType
import com.example.game.model.LevelConfig
import com.example.ui.theme.CandyBlue
import com.example.ui.theme.CandyGreen
import com.example.ui.theme.CandyOrange
import com.example.ui.theme.CandyPink
import com.example.ui.theme.CandyPurple
import com.example.ui.theme.CandyRed
import com.example.ui.theme.CandyYellow
import com.example.ui.theme.SugarGoldStar

enum class MapTab {
    SAGA,
    BLITZ
}

@Composable
fun SagaMapScreen(
    userStats: UserStatsEntity?,
    progressList: List<LevelProgressEntity>,
    onSelectLevel: (Int) -> Unit,
    onStartBlitz: () -> Unit,
    onBuyBooster: (BoosterType) -> Unit,
    onClaimWheelPrize: (WheelPrize) -> Unit,
    onClaimAchievement: (String, Int) -> Unit,
    onToggleHaptics: () -> Unit,
    onToggleSound: () -> Unit
) {
    var previewLevel by remember { mutableStateOf<LevelConfig?>(null) }
    var showShopDialog by remember { mutableStateOf(false) }
    var showWheelDialog by remember { mutableStateOf(false) }
    var showAchievementsDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(MapTab.SAGA) }

    val listState = rememberLazyListState()
    val levels = remember { LevelsCatalog.levels }

    val highestUnlockedLevel = remember(progressList) {
        val unlockedNums = progressList.filter { it.isUnlocked }.map { it.levelNumber }
        if (unlockedNums.isEmpty()) 1 else unlockedNums.maxOrNull() ?: 1
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF3B1550),
                        Color(0xFF260D38),
                        Color(0xFF140520)
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Deluxe Top HUD Bar
            MapTopBar(
                userStats = userStats,
                onOpenWheel = { showWheelDialog = true },
                onOpenAchievements = { showAchievementsDialog = true },
                onOpenShop = { showShopDialog = true },
                onOpenSettings = { showSettingsDialog = true }
            )

            // Game Mode Segmented Selector Tab
            ModeSegmentedTab(
                selectedTab = selectedTab,
                onSelectTab = { selectedTab = it }
            )

            // Dynamic Content
            Crossfade(
                targetState = selectedTab,
                animationSpec = tween(300),
                label = "mode_tab_crossfade"
            ) { tab ->
                when (tab) {
                    MapTab.SAGA -> {
                        // The Enchanted Saga Road
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(30.dp),
                            reverseLayout = true
                        ) {
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                            }

                            itemsIndexed(levels) { index, level ->
                                val progress = progressList.find { it.levelNumber == level.levelNumber }
                                val isUnlocked = progress?.isUnlocked ?: (level.levelNumber == 1)
                                val stars = progress?.stars ?: 0
                                val isCurrentTop = level.levelNumber == highestUnlockedLevel

                                val xOffset = when (index % 4) {
                                    0 -> (-60).dp
                                    1 -> (-10).dp
                                    2 -> 60.dp
                                    else -> 10.dp
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    LevelMapNode(
                                        level = level,
                                        isUnlocked = isUnlocked,
                                        isCurrentTop = isCurrentTop,
                                        stars = stars,
                                        xOffset = xOffset,
                                        onClick = {
                                            if (isUnlocked) {
                                                previewLevel = level
                                            }
                                        }
                                    )
                                    val realmTitle = LevelsCatalog.realmStarts[level.levelNumber]
                                    if (realmTitle != null) {
                                        Spacer(modifier = Modifier.height(20.dp))
                                        RealmBanner(
                                            title = realmTitle,
                                            color = when (level.levelNumber) {
                                                1 -> CandyGreen
                                                11 -> CandyPink
                                                21 -> CandyOrange
                                                31 -> CandyPurple
                                                else -> SugarGoldStar
                                            }
                                        )
                                    }
                                }
                            }

                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 36.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(CircleShape)
                                            .background(Brush.radialGradient(listOf(SugarGoldStar, CandyOrange)))
                                            .border(4.dp, Color.White, CircleShape)
                                            .shadow(16.dp, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "👑", fontSize = 48.sp)
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "ROYAL SUGAR PALACE",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = SugarGoldStar,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "The Sweetest Kingdom in the Galaxy",
                                        fontSize = 12.sp,
                                        color = Color(0xFFE2E8F0)
                                    )
                                }
                            }
                        }
                    }

                    MapTab.BLITZ -> {
                        // High-Octane Blitz Mode Dashboard
                        BlitzDashboard(
                            highScore = userStats?.blitzHighScore ?: 0,
                            onStartBlitz = onStartBlitz
                        )
                    }
                }
            }
        }

        // Dialogs
        previewLevel?.let { level ->
            val prog = progressList.find { it.levelNumber == level.levelNumber }
            LevelPreviewDialog(
                level = level,
                highScore = prog?.highScore ?: 0,
                stars = prog?.stars ?: 0,
                onDismiss = { previewLevel = null },
                onPlay = {
                    val num = level.levelNumber
                    previewLevel = null
                    onSelectLevel(num)
                }
            )
        }

        if (showWheelDialog) {
            FortuneWheelDialog(
                soundEnabled = userStats?.soundEnabled ?: true,
                onClaimPrize = onClaimWheelPrize,
                onDismiss = { showWheelDialog = false }
            )
        }

        if (showAchievementsDialog && userStats != null) {
            AchievementsDialog(
                userStats = userStats,
                progressList = progressList,
                onClaimAchievement = onClaimAchievement,
                onDismiss = { showAchievementsDialog = false }
            )
        }

        if (showShopDialog && userStats != null) {
            ShopDialog(
                userStats = userStats,
                onBuyBooster = onBuyBooster,
                onDismiss = { showShopDialog = false }
            )
        }

        if (showSettingsDialog && userStats != null) {
            SettingsDialog(
                userStats = userStats,
                onToggleHaptics = onToggleHaptics,
                onToggleSound = onToggleSound,
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}

@Composable
private fun ModeSegmentedTab(
    selectedTab: MapTab,
    onSelectTab: (MapTab) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0x33FFFFFF),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Saga Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .then(
                        if (selectedTab == MapTab.SAGA) {
                            Modifier.background(Brush.horizontalGradient(listOf(CandyPurple, Color(0xFF9333EA))))
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onSelectTab(MapTab.SAGA) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🗺️ SAGA CAMPAIGN",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = if (selectedTab == MapTab.SAGA) Color.White else Color(0xFFCBD5E1)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Blitz Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .then(
                        if (selectedTab == MapTab.BLITZ) {
                            Modifier.background(Brush.horizontalGradient(listOf(CandyOrange, SugarGoldStar)))
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onSelectTab(MapTab.BLITZ) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚡ BLITZ RUSH (2 MIN)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = if (selectedTab == MapTab.BLITZ) Color(0xFF1E0A28) else Color(0xFFCBD5E1)
                )
            }
        }
    }
}

@Composable
private fun BlitzDashboard(
    highScore: Int,
    onStartBlitz: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // High Score Trophy Banner
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF26103A)),
            modifier = Modifier
                .fillMaxWidth()
                .border(3.dp, Brush.horizontalGradient(listOf(CandyYellow, CandyOrange)), RoundedCornerShape(28.dp))
                .shadow(16.dp, RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "⚡", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "SUGAR BLITZ RUSH",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = SugarGoldStar,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "High-Speed 2-3 Minute Arcade Rush",
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x33000000),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0x44FFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "ALL-TIME HIGH SCORE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CandyYellow)
                        Text(
                            text = if (highScore > 0) "$highScore PTS" else "NO RECORD YET",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Blitz Rules Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0x22FFFFFF),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "⚡ HOW TO PLAY BLITZ:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SugarGoldStar)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "• Start with 120 seconds (2 mins) on the clock", fontSize = 12.sp, color = Color.White)
                Text(text = "• Each match adds +3.0 seconds bonus time", fontSize = 12.sp, color = Color.White)
                Text(text = "• High cascades trigger 2X HYPER FEVER overdrive", fontSize = 12.sp, color = Color.White)
                Text(text = "• Infinite moves — clear board as fast as you can!", fontSize = 12.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Big Play Button
        Button(
            onClick = onStartBlitz,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .testTag("start_blitz_button"),
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CandyGreen)
        ) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play Blitz", modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "START 2-MIN BLITZ RUSH!", fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun RealmBanner(title: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0x33000000),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, color.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = color,
                letterSpacing = 1.5.sp
            )
        }
    }
}

@Composable
private fun MapTopBar(
    userStats: UserStatsEntity?,
    onOpenWheel: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        color = Color(0xE61E092D),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp),
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Hearts Lives Counter
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0x33FFFFFF))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(18.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(text = "❤️", fontSize = 15.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${userStats?.lives ?: 5}/${userStats?.maxLives ?: 5}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            // Golden Coins Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0x44FFD700), Color(0x22FFD700))))
                    .border(1.5.dp, SugarGoldStar, RoundedCornerShape(18.dp))
                    .clickable(onClick = onOpenShop)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("coins_shop_button")
            ) {
                Text(text = "🪙", fontSize = 15.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${userStats?.coins ?: 300}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = SugarGoldStar
                )
            }

            // Quick Actions: Wheel, Trophies, Shop, Settings
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Fortune Wheel
                IconButton(
                    onClick = onOpenWheel,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .testTag("fortune_wheel_button")
                ) {
                    Text(text = "🎡", fontSize = 18.sp)
                }

                // Trophies / Achievements
                IconButton(
                    onClick = onOpenAchievements,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .testTag("achievements_button")
                ) {
                    Text(text = "🏆", fontSize = 18.sp)
                }

                // Booster Shop
                IconButton(
                    onClick = onOpenShop,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .testTag("shop_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Shop",
                        tint = SugarGoldStar,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Settings
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelMapNode(
    level: LevelConfig,
    isUnlocked: Boolean,
    isCurrentTop: Boolean,
    stars: Int,
    xOffset: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "node_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.09f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val avatarBob by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatar_bob"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = xOffset),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isCurrentTop) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CandyPink,
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                modifier = Modifier
                    .offset(y = avatarBob.dp)
                    .shadow(8.dp, RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "👑 YOU", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
        }

        Box(
            modifier = Modifier
                .size(72.dp)
                .scale(if (isCurrentTop) pulseScale else 1f)
                .clip(CircleShape)
                .background(
                    when {
                        !isUnlocked -> Brush.radialGradient(listOf(Color(0xFF64748B), Color(0xFF334155)))
                        stars > 0 -> Brush.radialGradient(listOf(CandyYellow, CandyOrange))
                        else -> Brush.radialGradient(listOf(CandyPink, CandyPurple))
                    }
                )
                .border(
                    3.5.dp,
                    if (isCurrentTop) SugarGoldStar else if (isUnlocked) Color.White else Color(0x55FFFFFF),
                    CircleShape
                )
                .shadow(10.dp, CircleShape)
                .clickable(enabled = isUnlocked, onClick = onClick)
                .testTag("level_node_${level.levelNumber}"),
            contentAlignment = Alignment.Center
        ) {
            if (!isUnlocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color(0xFFCBD5E1),
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Text(
                    text = "${level.levelNumber}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }

        if (isUnlocked) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (i in 1..3) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Star $i",
                        tint = if (i <= stars) SugarGoldStar else Color(0x33FFFFFF),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = level.name.take(13),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE2E8F0)
            )
        }
    }
}

@Composable
private fun SettingsDialog(
    userStats: UserStatsEntity,
    onToggleHaptics: () -> Unit,
    onToggleSound: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF241036)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(2.5.dp, CandyPurple, RoundedCornerShape(26.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Text(
                    text = "GAME SETTINGS",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Haptic Vibrations 📳", fontSize = 16.sp, color = Color.White)
                    Switch(
                        checked = userStats.hapticsEnabled,
                        onCheckedChange = { onToggleHaptics() },
                        colors = SwitchDefaults.colors(checkedThumbColor = CandyGreen)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Sound Effects 🎵", fontSize = 16.sp, color = Color.White)
                    Switch(
                        checked = userStats.soundEnabled,
                        onCheckedChange = { onToggleSound() },
                        colors = SwitchDefaults.colors(checkedThumbColor = CandyGreen)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0x22FFFFFF),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "Sugar Rush v3.0 (Pro Master)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SugarGoldStar)
                        Text(text = "Blitz Time-Attack • Fortune Wheel • Hyper Fever", fontSize = 11.sp, color = Color(0xFFCBD5E1))
                    }
                }
            }
        }
    }
}
