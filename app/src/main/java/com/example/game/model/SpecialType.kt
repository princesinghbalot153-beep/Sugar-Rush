package com.example.game.model

enum class SpecialType(val description: String) {
    NONE("Normal"),
    HORIZONTAL_STRIPED("Horizontal Striped (Clears Row)"),
    VERTICAL_STRIPED("Vertical Striped (Clears Column)"),
    WRAPPED("Wrapped Candy (3x3 Blast)"),
    COLOR_BOMB("Color Bomb (Clears All of One Color)");

    val isStriped: Boolean
        get() = this == HORIZONTAL_STRIPED || this == VERTICAL_STRIPED

    val isSpecial: Boolean
        get() = this != NONE
}
