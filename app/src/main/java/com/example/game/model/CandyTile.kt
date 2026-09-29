package com.example.game.model

import java.util.concurrent.atomic.AtomicLong

private val idGenerator = AtomicLong(1L)

/** Drop-down goal items (like cherries / hazelnuts in Candy Crush). They never match. */
enum class IngredientType(val displayName: String, val emoji: String) {
    CHERRY("Cherry", "🍒"),
    HAZELNUT("Hazelnut", "🌰")
}

data class CandyTile(
    val id: Long = idGenerator.incrementAndGet(),
    val type: CandyType,
    val special: SpecialType = SpecialType.NONE,
    val ingredient: IngredientType? = null,
    val isClearing: Boolean = false,
    val isSelected: Boolean = false,
    val isHinted: Boolean = false
) {
    val isIngredient: Boolean get() = ingredient != null

    fun copyWithoutState(): CandyTile {
        return copy(isClearing = false, isSelected = false, isHinted = false)
    }
}
