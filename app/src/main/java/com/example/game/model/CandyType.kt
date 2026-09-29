package com.example.game.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CandyBlue
import com.example.ui.theme.CandyBlueDark
import com.example.ui.theme.CandyGreen
import com.example.ui.theme.CandyGreenDark
import com.example.ui.theme.CandyOrange
import com.example.ui.theme.CandyOrangeDark
import com.example.ui.theme.CandyPurple
import com.example.ui.theme.CandyPurpleDark
import com.example.ui.theme.CandyRed
import com.example.ui.theme.CandyRedDark
import com.example.ui.theme.CandyYellow
import com.example.ui.theme.CandyYellowDark

enum class CandyType(
    val displayName: String,
    val mainColor: Color,
    val darkColor: Color,
    val shapeName: String
) {
    RED("Cherry Heart", CandyRed, CandyRedDark, "heart"),
    ORANGE("Orange Lozenge", CandyOrange, CandyOrangeDark, "lozenge"),
    YELLOW("Lemon Drop", CandyYellow, CandyYellowDark, "star"),
    GREEN("Apple Square", CandyGreen, CandyGreenDark, "square"),
    BLUE("Berry Sphere", CandyBlue, CandyBlueDark, "circle"),
    PURPLE("Plum Triangle", CandyPurple, CandyPurpleDark, "triangle");

    companion object {
        fun random(available: List<CandyType> = entries): CandyType {
            return available.random()
        }
    }
}
