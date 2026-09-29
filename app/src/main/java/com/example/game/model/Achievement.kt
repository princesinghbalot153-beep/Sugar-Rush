package com.example.game.model

data class AchievementItem(
    val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val rewardCoins: Int,
    val currentProgress: Int,
    val maxProgress: Int,
    val isCompleted: Boolean,
    val isClaimed: Boolean
)
