package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "level_progress")
data class LevelProgressEntity(
    @PrimaryKey val levelNumber: Int,
    val stars: Int = 0, // 0 to 3
    val highScore: Int = 0,
    val isUnlocked: Boolean = false,
    val isCompleted: Boolean = false,
    val completedAt: Long = 0L
)

@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey val id: Int = 1,
    val coins: Int = 300,
    val lives: Int = 5,
    val maxLives: Int = 5,
    val lastLifeRegenTimestamp: Long = System.currentTimeMillis(),
    val lollipopHammers: Int = 3,
    val freeSwitches: Int = 3,
    val colorBombs: Int = 2,
    val extraMoves: Int = 3,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val blitzHighScore: Int = 0,
    val colorBombsDetonated: Int = 0,
    val maxComboChain: Int = 0,
    val claimedAchievements: String = ""
)
