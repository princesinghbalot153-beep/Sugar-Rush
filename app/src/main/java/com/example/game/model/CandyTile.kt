package com.example.game.model

import java.util.concurrent.atomic.AtomicLong

private val idGenerator = AtomicLong(1L)

data class CandyTile(
    val id: Long = idGenerator.incrementAndGet(),
    val type: CandyType,
    val special: SpecialType = SpecialType.NONE,
    val isClearing: Boolean = false,
    val isSelected: Boolean = false,
    val isHinted: Boolean = false
) {
    fun copyWithoutState(): CandyTile {
        return copy(isClearing = false, isSelected = false, isHinted = false)
    }
}
