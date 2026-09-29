package com.example.game.model

enum class LevelGoalType {
    SCORE,
    CLEAR_JELLY,
    COLLECT_CANDIES,
    CREATE_SPECIALS,
    DROP_INGREDIENTS
}

data class CandyCollectGoal(
    val type: CandyType,
    val targetCount: Int,
    val currentCount: Int = 0
) {
    val isCompleted: Boolean get() = currentCount >= targetCount
}

data class SpecialCollectGoal(
    val specialType: SpecialType,
    val targetCount: Int,
    val currentCount: Int = 0
) {
    val isCompleted: Boolean get() = currentCount >= targetCount
}

data class LevelConfig(
    val levelNumber: Int,
    val name: String,
    val rows: Int = 9,
    val cols: Int = 9,
    val maxMoves: Int,
    val goalType: LevelGoalType,
    val targetScore: Int,
    val star1Score: Int,
    val star2Score: Int,
    val star3Score: Int,
    val initialJellyMap: List<Position> = emptyList(),
    val doubleJellyMap: List<Position> = emptyList(),
    val blockedCells: List<Position> = emptyList(),
    val candyGoals: List<CandyCollectGoal> = emptyList(),
    val specialGoals: List<SpecialCollectGoal> = emptyList(),
    val availableColors: List<CandyType> = CandyType.entries,
    val storyDescription: String = "",
    // ---- Candy-Crush style blockers / objectives ----
    val lockMap: List<Position> = emptyList(),
    val chocolateMap: List<Position> = emptyList(),
    val ingredientStart: List<Position> = emptyList(),
    val ingredientTarget: Int = 0,
    val ingredientSpawnCols: List<Int> = emptyList(),
    val extraGoals: Set<LevelGoalType> = emptySet(),
    val realm: String = ""
) {
    /** Every objective that must be satisfied to win (primary goal + extras). */
    val goalTypes: Set<LevelGoalType> get() = extraGoals + goalType

    val totalJellies: Int = run {
        val doubles = doubleJellyMap.toSet()
        val singles = initialJellyMap.toSet() - doubles
        singles.size + doubles.size * 2
    }

    /** Bottom-most row of a column that is part of the board (ingredients exit there). -1 if none. */
    fun exitRow(col: Int): Int {
        val blocked = blockedCells.toSet()
        for (r in rows - 1 downTo 0) {
            if (!blocked.contains(Position(r, col))) return r
        }
        return -1
    }
}
