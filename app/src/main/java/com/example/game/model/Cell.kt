package com.example.game.model

data class Position(val row: Int, val col: Int) {
    fun isAdjacentTo(other: Position): Boolean {
        val dRow = kotlin.math.abs(row - other.row)
        val dCol = kotlin.math.abs(col - other.col)
        return (dRow == 1 && dCol == 0) || (dRow == 0 && dCol == 1)
    }
}

data class Cell(
    val row: Int,
    val col: Int,
    val candy: CandyTile?,
    val jellyLevel: Int = 0, // 0: no jelly, 1: single jelly, 2: double jelly
    val isPlayable: Boolean = true
) {
    val position: Position get() = Position(row, col)
    val hasJelly: Boolean get() = jellyLevel > 0
}
