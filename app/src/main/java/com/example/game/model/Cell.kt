package com.example.game.model

data class Position(val row: Int, val col: Int) {
    fun isAdjacentTo(other: Position): Boolean {
        val dRow = kotlin.math.abs(row - other.row)
        val dCol = kotlin.math.abs(col - other.col)
        return (dRow == 1 && dCol == 0) || (dRow == 0 && dCol == 1)
    }
}

/**
 * One board slot.
 *  - [isPlayable] false  => wall (blocked) OR chocolate ([isChocolate] true).
 *  - [lockLevel] > 0     => licorice lock: the candy cannot be swapped or fall, but it can be matched.
 */
data class Cell(
    val row: Int,
    val col: Int,
    val candy: CandyTile?,
    val jellyLevel: Int = 0, // 0: no jelly, 1: single jelly, 2: double jelly
    val isPlayable: Boolean = true,
    val lockLevel: Int = 0,
    val isChocolate: Boolean = false
) {
    val position: Position get() = Position(row, col)
    val hasJelly: Boolean get() = jellyLevel > 0
    val isLocked: Boolean get() = lockLevel > 0 && candy != null
}
