package com.example.game.model

enum class BoosterType(
    val title: String,
    val description: String,
    val iconEmoji: String,
    val coinCost: Int
) {
    LOLLIPOP_HAMMER(
        title = "Lollipop Hammer",
        description = "Smash any single candy or jelly tile on the board!",
        iconEmoji = "🍭",
        coinCost = 100
    ),
    FREE_SWITCH(
        title = "Free Switch",
        description = "Swap any two candies without spending a move!",
        iconEmoji = "🔄",
        coinCost = 120
    ),
    COLOR_BOMB(
        title = "Color Bomb",
        description = "Place a Rainbow Color Bomb on any chosen cell!",
        iconEmoji = "💣",
        coinCost = 150
    ),
    EXTRA_MOVES(
        title = "+5 Moves",
        description = "Add 5 extra moves to keep playing!",
        iconEmoji = "➕",
        coinCost = 80
    )
}
