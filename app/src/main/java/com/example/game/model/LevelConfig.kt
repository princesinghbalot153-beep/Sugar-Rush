package com.example.game.model

enum class LevelGoalType {
    SCORE,
    CLEAR_JELLY,
    COLLECT_CANDIES,
    CREATE_SPECIALS
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
    val rows: Int = 8,
    val cols: Int = 8,
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
    val storyDescription: String = ""
) {
    val totalJellies: Int = initialJellyMap.size + (doubleJellyMap.size * 2)
}
