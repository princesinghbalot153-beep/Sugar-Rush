package com.example.game.model

import androidx.compose.ui.graphics.Color
import java.util.UUID

data class FloatingScore(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val color: Color,
    val row: Int,
    val col: Int,
    val createdAt: Long = System.currentTimeMillis()
)

data class GameParticle(
    val id: String = UUID.randomUUID().toString(),
    val x: Float, // normalized 0..1 or pixel relative to board
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    val alpha: Float = 1f,
    val rotation: Float = 0f,
    val vr: Float = 0f
)

data class ComboBanner(
    val text: String,
    val subText: String = "",
    val color: Color,
    val timestamp: Long = System.currentTimeMillis()
)

data class LaserBeamEffect(
    val id: String = UUID.randomUUID().toString(),
    val isHorizontal: Boolean,
    val index: Int, // row or col index
    val color: Color,
    val timestamp: Long = System.currentTimeMillis()
)

data class ShockwaveRingEffect(
    val id: String = UUID.randomUUID().toString(),
    val centerX: Float, // normalized 0..1
    val centerY: Float,
    val color: Color,
    val timestamp: Long = System.currentTimeMillis()
)
