package com.example.game.levels

import com.example.game.model.CandyCollectGoal
import com.example.game.model.CandyType
import com.example.game.model.LevelConfig
import com.example.game.model.LevelGoalType
import com.example.game.model.Position
import com.example.game.model.SpecialCollectGoal
import com.example.game.model.SpecialType

object LevelsCatalog {
    val levels: List<LevelConfig> = listOf(
        // Level 1: Peppermint Meadow (2-3 min play: Collect 35 Red + 35 Yellow)
        LevelConfig(
            levelNumber = 1,
            name = "Peppermint Meadow",
            rows = 8,
            cols = 8,
            maxMoves = 28,
            goalType = LevelGoalType.COLLECT_CANDIES,
            targetScore = 20000,
            star1Score = 20000,
            star2Score = 32000,
            star3Score = 48000,
            candyGoals = listOf(
                CandyCollectGoal(CandyType.RED, targetCount = 35),
                CandyCollectGoal(CandyType.YELLOW, targetCount = 35)
            ),
            availableColors = listOf(CandyType.RED, CandyType.ORANGE, CandyType.YELLOW, CandyType.GREEN),
            storyDescription = "Harvest 35 Cherry Hearts and 35 Lemon Drops across 28 moves to fuel the candy festival!"
        ),

        // Level 2: Soda Sprinkles (2-3 min play: Forge 4 Striped + 2 Wrapped)
        LevelConfig(
            levelNumber = 2,
            name = "Soda Sprinkles",
            rows = 8,
            cols = 8,
            maxMoves = 30,
            goalType = LevelGoalType.CREATE_SPECIALS,
            targetScore = 25000,
            star1Score = 25000,
            star2Score = 40000,
            star3Score = 60000,
            specialGoals = listOf(
                SpecialCollectGoal(SpecialType.HORIZONTAL_STRIPED, targetCount = 4),
                SpecialCollectGoal(SpecialType.WRAPPED, targetCount = 2)
            ),
            availableColors = listOf(CandyType.RED, CandyType.ORANGE, CandyType.YELLOW, CandyType.GREEN, CandyType.BLUE),
            storyDescription = "Forge 4 Striped Candies and 2 explosive Wrapped Candies to fill the bubbling soda fountain!"
        ),

        // Level 3: Jelly Junction (2-3 min play: 28 single + 6 double jellies)
        LevelConfig(
            levelNumber = 3,
            name = "Jelly Junction",
            rows = 8,
            cols = 8,
            maxMoves = 32,
            goalType = LevelGoalType.CLEAR_JELLY,
            targetScore = 28000,
            star1Score = 28000,
            star2Score = 45000,
            star3Score = 68000,
            initialJellyMap = buildList {
                for (r in 1..6) {
                    for (c in 1..6) {
                        if (r == 1 || r == 6 || c == 1 || c == 6) add(Position(r, c))
                    }
                }
                for (r in 3..4) {
                    for (c in 3..4) {
                        add(Position(r, c))
                    }
                }
            },
            doubleJellyMap = listOf(
                Position(2, 2), Position(2, 5), Position(5, 2), Position(5, 5),
                Position(3, 3), Position(4, 4)
            ),
            availableColors = listOf(CandyType.RED, CandyType.ORANGE, CandyType.YELLOW, CandyType.GREEN, CandyType.BLUE),
            storyDescription = "Clear all the stubborn jelly layers trapped beneath the junction across 32 thoughtful moves!"
        ),

        // Level 4: Cherry Orchard (2-3 min play: 130 candies to harvest)
        LevelConfig(
            levelNumber = 4,
            name = "Cherry Orchard",
            rows = 8,
            cols = 8,
            maxMoves = 32,
            goalType = LevelGoalType.COLLECT_CANDIES,
            targetScore = 32000,
            star1Score = 32000,
            star2Score = 52000,
            star3Score = 75000,
            candyGoals = listOf(
                CandyCollectGoal(CandyType.RED, targetCount = 45),
                CandyCollectGoal(CandyType.ORANGE, targetCount = 45),
                CandyCollectGoal(CandyType.YELLOW, targetCount = 40)
            ),
            availableColors = listOf(CandyType.RED, CandyType.ORANGE, CandyType.YELLOW, CandyType.GREEN, CandyType.PURPLE),
            storyDescription = "Gather 45 Cherry Hearts, 45 Orange Lozenges, and 40 Lemon Drops across a full orchard harvest!"
        ),

        // Level 5: Caramel Canyon (2-3 min play: 4 Wrapped + 4 Striped)
        LevelConfig(
            levelNumber = 5,
            name = "Caramel Canyon",
            rows = 8,
            cols = 8,
            maxMoves = 32,
            goalType = LevelGoalType.CREATE_SPECIALS,
            targetScore = 35000,
            star1Score = 35000,
            star2Score = 55000,
            star3Score = 80000,
            specialGoals = listOf(
                SpecialCollectGoal(SpecialType.WRAPPED, targetCount = 4),
                SpecialCollectGoal(SpecialType.HORIZONTAL_STRIPED, targetCount = 4)
            ),
            availableColors = listOf(CandyType.RED, CandyType.ORANGE, CandyType.YELLOW, CandyType.BLUE, CandyType.PURPLE),
            storyDescription = "Match in T, L, and 4-in-a-row lines to forge 4 Wrapped and 4 Striped power Candies!"
        ),

        // Level 6: Gummy Glade (2-3 min play: 24 single + 16 double jellies)
        LevelConfig(
            levelNumber = 6,
            name = "Gummy Glade",
            rows = 8,
            cols = 8,
            maxMoves = 34,
            goalType = LevelGoalType.CLEAR_JELLY,
            targetScore = 38000,
            star1Score = 38000,
            star2Score = 60000,
            star3Score = 90000,
            initialJellyMap = buildList {
                for (r in 0..7) {
                    add(Position(r, 0))
                    add(Position(r, 7))
                }
                for (c in 1..6) {
                    add(Position(0, c))
                    add(Position(7, c))
                }
            },
            doubleJellyMap = buildList {
                for (r in 2..5) {
                    for (c in 2..5) {
                        add(Position(r, c))
                    }
                }
            },
            availableColors = CandyType.entries,
            storyDescription = "Double jellies require two matches to clear! Blast through the outer ring and central gummy vault."
        ),

        // Level 7: Disco Sugar Lagoon (2-3 min play: 2 Color Bombs + 4 Striped)
        LevelConfig(
            levelNumber = 7,
            name = "Disco Sugar Lagoon",
            rows = 8,
            cols = 8,
            maxMoves = 34,
            goalType = LevelGoalType.CREATE_SPECIALS,
            targetScore = 40000,
            star1Score = 40000,
            star2Score = 65000,
            star3Score = 95000,
            specialGoals = listOf(
                SpecialCollectGoal(SpecialType.COLOR_BOMB, targetCount = 2),
                SpecialCollectGoal(SpecialType.HORIZONTAL_STRIPED, targetCount = 4)
            ),
            availableColors = listOf(CandyType.RED, CandyType.ORANGE, CandyType.YELLOW, CandyType.GREEN, CandyType.BLUE),
            storyDescription = "Match 5 in a straight line to craft 2 legendary Disco Color Bombs and 4 Striped Candies!"
        ),

        // Level 8: Blueberry Bay (2-3 min play: 145 candies)
        LevelConfig(
            levelNumber = 8,
            name = "Blueberry Bay",
            rows = 8,
            cols = 8,
            maxMoves = 35,
            goalType = LevelGoalType.COLLECT_CANDIES,
            targetScore = 42000,
            star1Score = 42000,
            star2Score = 68000,
            star3Score = 100000,
            candyGoals = listOf(
                CandyCollectGoal(CandyType.BLUE, targetCount = 50),
                CandyCollectGoal(CandyType.GREEN, targetCount = 50),
                CandyCollectGoal(CandyType.PURPLE, targetCount = 45)
            ),
            availableColors = CandyType.entries,
            storyDescription = "Harvest 50 Blue Spheres, 50 Green Apples, and 45 Purple Grapes to brew galactic juice!"
        ),

        // Level 9: Chocolate Fortress (2-3 min play: 28 double frosted perimeter)
        LevelConfig(
            levelNumber = 9,
            name = "Chocolate Fortress",
            rows = 8,
            cols = 8,
            maxMoves = 36,
            goalType = LevelGoalType.CLEAR_JELLY,
            targetScore = 45000,
            star1Score = 45000,
            star2Score = 72000,
            star3Score = 105000,
            doubleJellyMap = buildList {
                for (r in 0..7) {
                    add(Position(r, 0))
                    add(Position(r, 7))
                }
                for (c in 1..6) {
                    add(Position(0, c))
                    add(Position(7, c))
                }
            },
            availableColors = CandyType.entries,
            storyDescription = "A double-frosted perimeter surrounds the chocolate fortress. Strategize laser blasts to shatter it!"
        ),

        // Level 10: Royal Sugar Palace (2-3 min play: 32 checkered + 12 double throne)
        LevelConfig(
            levelNumber = 10,
            name = "Royal Sugar Palace",
            rows = 8,
            cols = 8,
            maxMoves = 36,
            goalType = LevelGoalType.CLEAR_JELLY,
            targetScore = 50000,
            star1Score = 50000,
            star2Score = 80000,
            star3Score = 120000,
            initialJellyMap = buildList {
                for (r in 0..7) {
                    for (c in 0..7) {
                        if ((r + c) % 2 == 0) add(Position(r, c))
                    }
                }
            },
            doubleJellyMap = listOf(
                Position(2, 3), Position(2, 4), Position(3, 2), Position(3, 5),
                Position(4, 2), Position(4, 5), Position(5, 3), Position(5, 4),
                Position(3, 3), Position(3, 4), Position(4, 3), Position(4, 4)
            ),
            availableColors = CandyType.entries,
            storyDescription = "The Royal Palace checkered floor requires 36 deliberate moves to claim the King's Golden Crown!"
        ),

        // Level 11: Prism Falls (2-3 min play: 60,000 points score summit)
        LevelConfig(
            levelNumber = 11,
            name = "Prism Falls",
            rows = 8,
            cols = 8,
            maxMoves = 35,
            goalType = LevelGoalType.SCORE,
            targetScore = 60000,
            star1Score = 60000,
            star2Score = 95000,
            star3Score = 140000,
            availableColors = listOf(CandyType.RED, CandyType.ORANGE, CandyType.YELLOW, CandyType.GREEN),
            storyDescription = "Cascading paradise! Chain long combo streaks and Hyper Fever over 35 moves to reach 60,000 pts!"
        ),

        // Level 12: Cotton Candy Cloud (2-3 min play: 165 candies)
        LevelConfig(
            levelNumber = 12,
            name = "Cotton Candy Cloud",
            rows = 8,
            cols = 8,
            maxMoves = 36,
            goalType = LevelGoalType.COLLECT_CANDIES,
            targetScore = 48000,
            star1Score = 48000,
            star2Score = 75000,
            star3Score = 110000,
            candyGoals = listOf(
                CandyCollectGoal(CandyType.PURPLE, targetCount = 55),
                CandyCollectGoal(CandyType.ORANGE, targetCount = 55),
                CandyCollectGoal(CandyType.RED, targetCount = 55)
            ),
            availableColors = CandyType.entries,
            storyDescription = "Gather 55 Purple, 55 Orange, and 55 Red candies across 36 moves to fill the cloud machine!"
        ),

        // Level 13: Taffy Crossroads (2-3 min play: 32 double frosted cross)
        LevelConfig(
            levelNumber = 13,
            name = "Taffy Crossroads",
            rows = 8,
            cols = 8,
            maxMoves = 36,
            goalType = LevelGoalType.CLEAR_JELLY,
            targetScore = 52000,
            star1Score = 52000,
            star2Score = 82000,
            star3Score = 120000,
            doubleJellyMap = buildList {
                for (i in 0..7) {
                    add(Position(3, i))
                    add(Position(4, i))
                    add(Position(i, 3))
                    add(Position(i, 4))
                }
            },
            availableColors = CandyType.entries,
            storyDescription = "Dense double jelly forms a massive cross across the board. Laser blasts will be your best weapon!"
        ),

        // Level 14: Licorice Labyrinth (2-3 min play: 5 Striped + 4 Wrapped + 2 Color Bombs)
        LevelConfig(
            levelNumber = 14,
            name = "Licorice Labyrinth",
            rows = 8,
            cols = 8,
            maxMoves = 38,
            goalType = LevelGoalType.CREATE_SPECIALS,
            targetScore = 55000,
            star1Score = 55000,
            star2Score = 88000,
            star3Score = 130000,
            specialGoals = listOf(
                SpecialCollectGoal(SpecialType.HORIZONTAL_STRIPED, targetCount = 5),
                SpecialCollectGoal(SpecialType.WRAPPED, targetCount = 4),
                SpecialCollectGoal(SpecialType.COLOR_BOMB, targetCount = 2)
            ),
            availableColors = CandyType.entries,
            storyDescription = "Master all 3 types of special candies: forge 5 Striped, 4 Wrapped, and 2 Color Bombs!"
        ),

        // Level 15: Sugar Supreme Galaxy (3-4 min grand finale: 64 board jellies with 24 double core)
        LevelConfig(
            levelNumber = 15,
            name = "Sugar Supreme Galaxy",
            rows = 8,
            cols = 8,
            maxMoves = 42,
            goalType = LevelGoalType.CLEAR_JELLY,
            targetScore = 70000,
            star1Score = 70000,
            star2Score = 110000,
            star3Score = 160000,
            initialJellyMap = buildList {
                for (r in 0..7) {
                    for (c in 0..7) {
                        add(Position(r, c))
                    }
                }
            },
            doubleJellyMap = buildList {
                for (r in 2..5) {
                    for (c in 2..5) {
                        add(Position(r, c))
                    }
                }
                add(Position(0, 0))
                add(Position(0, 7))
                add(Position(7, 0))
                add(Position(7, 7))
                add(Position(1, 1))
                add(Position(1, 6))
                add(Position(6, 1))
                add(Position(6, 6))
            },
            availableColors = CandyType.entries,
            storyDescription = "The supreme grand finale! Clear all 64 galaxy tiles across 42 strategic moves for ultimate glory!"
        )
    )

    fun getLevel(levelNumber: Int): LevelConfig {
        return levels.find { it.levelNumber == levelNumber } ?: levels.first()
    }
}
