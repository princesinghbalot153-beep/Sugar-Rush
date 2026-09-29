package com.example.game.levels

import com.example.game.model.CandyCollectGoal
import com.example.game.model.CandyType
import com.example.game.model.LevelConfig
import com.example.game.model.LevelGoalType
import com.example.game.model.Position
import com.example.game.model.SpecialCollectGoal
import com.example.game.model.SpecialType

/**
 * 50 hand-tuned levels on a 9x9 board, split in 5 realms of 10 levels.
 * Mechanics are introduced gradually (like the real saga):
 *  L1 score -> L2 collect -> L4 jelly -> L5 specials -> L7 ingredients -> L12 double jelly & licorice locks
 *  -> L21 chocolate -> mixed multi-objective levels from L31.
 *
 * All difficulty knobs (moves, colours, blockers) live here. Star thresholds are derived in [starsFor].
 */
object LevelsCatalog {

    // ---------------------------------------------------------------- shape helpers
    private fun rect(r0: Int, r1: Int, c0: Int, c1: Int): List<Position> = buildList {
        for (r in r0..r1) for (c in c0..c1) add(Position(r, c))
    }

    private fun ring(margin: Int, size: Int = 9): List<Position> = buildList {
        val last = size - 1 - margin
        for (r in margin..last) for (c in margin..last) {
            if (r == margin || r == last || c == margin || c == last) add(Position(r, c))
        }
    }

    private fun checker(parity: Int = 0): List<Position> = buildList {
        for (r in 0..8) for (c in 0..8) if ((r + c) % 2 == parity) add(Position(r, c))
    }

    private fun cross(width: Int = 1): List<Position> = buildList {
        for (i in 0..8) for (w in -width..width) {
            add(Position(4 + w, i)); add(Position(i, 4 + w))
        }
    }.filter { it.row in 0..8 && it.col in 0..8 }.distinct()

    private fun corners(size: Int): List<Position> = buildList {
        for (i in 0 until size) for (j in 0 until size - i) {
            add(Position(i, j)); add(Position(i, 8 - j)); add(Position(8 - i, j)); add(Position(8 - i, 8 - j))
        }
    }.distinct()

    private fun diamond(radius: Int): List<Position> = buildList {
        for (r in 0..8) for (c in 0..8) if (kotlin.math.abs(r - 4) + kotlin.math.abs(c - 4) <= radius) add(Position(r, c))
    }

    private fun holes(vararg p: Pair<Int, Int>): List<Position> = p.map { Position(it.first, it.second) }

    private val four = listOf(CandyType.RED, CandyType.ORANGE, CandyType.YELLOW, CandyType.GREEN)
    private val five = listOf(CandyType.RED, CandyType.ORANGE, CandyType.YELLOW, CandyType.GREEN, CandyType.BLUE)
    private val six = CandyType.entries

    /** (star1, star2, star3) score thresholds, scaled by moves and a difficulty factor. */
    private fun starsFor(moves: Int, tier: Int): Triple<Int, Int, Int> {
        val perMove = 260 + tier * 22
        val s1 = moves * perMove
        return Triple(s1, (s1 * 1.5f).toInt(), (s1 * 2.2f).toInt())
    }

    private fun make(
        n: Int,
        name: String,
        realm: String,
        moves: Int,
        goal: LevelGoalType,
        colors: List<CandyType>,
        story: String,
        scoreTarget: Int = 0,
        jelly: List<Position> = emptyList(),
        doubleJelly: List<Position> = emptyList(),
        blocked: List<Position> = emptyList(),
        locks: List<Position> = emptyList(),
        chocolate: List<Position> = emptyList(),
        candyGoals: List<CandyCollectGoal> = emptyList(),
        specialGoals: List<SpecialCollectGoal> = emptyList(),
        ingredientCols: List<Int> = emptyList(),
        ingredientTarget: Int = 0,
        extra: Set<LevelGoalType> = emptySet()
    ): LevelConfig {
        val (s1, s2, s3) = starsFor(moves, (n - 1) / 2)
        val target = if (goal == LevelGoalType.SCORE) (if (scoreTarget > 0) scoreTarget else s1) else s1
        val dbl = doubleJelly.toSet()
        return LevelConfig(
            levelNumber = n,
            name = name,
            realm = realm,
            maxMoves = moves,
            goalType = goal,
            targetScore = target,
            star1Score = if (goal == LevelGoalType.SCORE) target else s1 / 2,
            star2Score = maxOf(s2, target),
            star3Score = maxOf(s3, target * 2),
            initialJellyMap = jelly.filter { it !in dbl },
            doubleJellyMap = doubleJelly,
            blockedCells = blocked,
            lockMap = locks,
            chocolateMap = chocolate,
            candyGoals = candyGoals,
            specialGoals = specialGoals,
            ingredientStart = ingredientCols.map { Position(0, it) },
            ingredientTarget = ingredientTarget,
            ingredientSpawnCols = ingredientCols,
            availableColors = colors,
            extraGoals = extra,
            storyDescription = story
        )
    }

    private const val R1 = "🍭 PEPPERMINT MEADOW"
    private const val R2 = "🍮 GUMMY GLADE"
    private const val R3 = "🍫 CHOCOLATE FORTRESS"
    private const val R4 = "🌀 LICORICE LABYRINTH"
    private const val R5 = "🏰 ROYAL SUGAR SUMMIT"

    private fun collect(vararg g: Pair<CandyType, Int>) = g.map { CandyCollectGoal(it.first, it.second) }
    private fun specials(vararg g: Pair<SpecialType, Int>) = g.map { SpecialCollectGoal(it.first, it.second) }

    val levels: List<LevelConfig> = listOf(
        // ======================================================= REALM 1 : basics
        make(1, "Sprinkle Start", R1, 22, LevelGoalType.SCORE, four,
            "Match three or more candies to reach the score!", scoreTarget = 4500),
        make(2, "Cherry Picking", R1, 24, LevelGoalType.COLLECT_CANDIES, four,
            "Collect 30 Cherry Hearts and 25 Lemon Drops.",
            candyGoals = collect(CandyType.RED to 30, CandyType.YELLOW to 25)),
        make(3, "Minty Corners", R1, 24, LevelGoalType.SCORE, four,
            "Sweet score chase on a cosy board.", scoreTarget = 7000, blocked = corners(1)),
        make(4, "First Jelly", R1, 26, LevelGoalType.CLEAR_JELLY, four,
            "Match candies on top of jelly to clear it.", jelly = rect(3, 5, 2, 6)),
        make(5, "Striped Surprise", R1, 26, LevelGoalType.CREATE_SPECIALS, five,
            "Match 4 in a row to make striped candies!",
            specialGoals = specials(SpecialType.HORIZONTAL_STRIPED to 4)),
        make(6, "Orange Grove", R1, 26, LevelGoalType.COLLECT_CANDIES, five,
            "Gather oranges and greens before moves run out.",
            candyGoals = collect(CandyType.ORANGE to 35, CandyType.GREEN to 35), blocked = corners(2)),
        make(7, "Cherry Drop", R1, 28, LevelGoalType.DROP_INGREDIENTS, four,
            "Bring the cherries and hazelnuts to the bottom!",
            ingredientCols = listOf(2, 6), ingredientTarget = 2),
        make(8, "Jelly Ring", R1, 28, LevelGoalType.CLEAR_JELLY, five,
            "Clear the whole jelly ring.", jelly = ring(1)),
        make(9, "Wrapped Wonders", R1, 28, LevelGoalType.CREATE_SPECIALS, five,
            "Make L or T shapes for wrapped candies!",
            specialGoals = specials(SpecialType.WRAPPED to 3, SpecialType.HORIZONTAL_STRIPED to 2),
            blocked = holes(4 to 0, 4 to 8)),
        make(10, "Meadow Finale", R1, 26, LevelGoalType.SCORE, five,
            "Big score, small board. Use your specials!", scoreTarget = 11000, blocked = corners(2)),

        // ======================================================= REALM 2 : jelly & ingredients
        make(11, "Gummy Gate", R2, 30, LevelGoalType.CLEAR_JELLY, five,
            "A gummy checkerboard blocks the gate.", jelly = checker(0)),
        make(12, "Double Trouble", R2, 30, LevelGoalType.CLEAR_JELLY, five,
            "Double jelly needs two matches on the same tile!", doubleJelly = rect(3, 5, 3, 5),
            jelly = rect(2, 6, 2, 6)),
        make(13, "Licorice Lock", R2, 28, LevelGoalType.SCORE, five,
            "Locked candies can't be swapped. Match them to free them!", scoreTarget = 12000,
            locks = holes(2 to 2, 2 to 6, 4 to 4, 6 to 2, 6 to 6)),
        make(14, "Nutty Path", R2, 32, LevelGoalType.DROP_INGREDIENTS, five,
            "Drop 3 ingredients past the walls.", ingredientCols = listOf(1, 4, 7), ingredientTarget = 3,
            blocked = holes(3 to 3, 3 to 5, 5 to 4, 6 to 2, 6 to 6)),
        make(15, "Berry Bay", R2, 28, LevelGoalType.COLLECT_CANDIES, six,
            "Six flavours! Collect blue, purple and green.",
            candyGoals = collect(CandyType.BLUE to 30, CandyType.PURPLE to 30, CandyType.GREEN to 30)),
        make(16, "Cross Jelly", R2, 30, LevelGoalType.CLEAR_JELLY, five,
            "Clear the jelly cross.", jelly = cross(1), doubleJelly = holes(4 to 4)),
        make(17, "Lock & Load", R2, 30, LevelGoalType.CLEAR_JELLY, five,
            "Jelly guarded by licorice locks.", jelly = rect(3, 5, 1, 7),
            locks = holes(3 to 1, 3 to 7, 5 to 1, 5 to 7, 4 to 4)),
        make(18, "Bomb Squad", R2, 28, LevelGoalType.CREATE_SPECIALS, five,
            "Match 5 in a line for a Colour Bomb!",
            specialGoals = specials(SpecialType.COLOR_BOMB to 2, SpecialType.WRAPPED to 2)),
        make(19, "Golden Harvest", R2, 34, LevelGoalType.DROP_INGREDIENTS, six,
            "Four ingredients to bring home.", ingredientCols = listOf(0, 3, 5, 8), ingredientTarget = 4,
            jelly = rect(6, 8, 0, 8), blocked = holes(4 to 4)),
        make(20, "Glade Finale", R2, 32, LevelGoalType.CLEAR_JELLY, six,
            "Everything at once!", jelly = ring(0) + ring(2), doubleJelly = rect(3, 5, 3, 5),
            locks = holes(1 to 1, 1 to 7, 7 to 1, 7 to 7)),

        // ======================================================= REALM 3 : chocolate
        make(21, "Choco Chunk", R3, 28, LevelGoalType.SCORE, five,
            "Chocolate grows if you don't eat it. Match next to it!", scoreTarget = 14000,
            chocolate = holes(4 to 4, 4 to 5)),
        make(22, "Fudge Fields", R3, 30, LevelGoalType.COLLECT_CANDIES, five,
            "Collect while chocolate creeps in.",
            candyGoals = collect(CandyType.RED to 30, CandyType.BLUE to 30, CandyType.GREEN to 25),
            chocolate = holes(2 to 4, 6 to 4)),
        make(23, "Cocoa Jelly", R3, 32, LevelGoalType.CLEAR_JELLY, five,
            "Jelly beneath the chocolate.", jelly = rect(3, 5, 3, 5) + rect(0, 0, 3, 5),
            chocolate = holes(4 to 3, 4 to 5)),
        make(24, "Bitter Drops", R3, 34, LevelGoalType.DROP_INGREDIENTS, six,
            "Chocolate blocks the paths!", ingredientCols = listOf(2, 6), ingredientTarget = 2,
            chocolate = holes(4 to 2, 4 to 6, 5 to 4)),
        make(25, "Choco Vault", R3, 30, LevelGoalType.CLEAR_JELLY, six,
            "Crack the double-jelly vault.", doubleJelly = rect(3, 5, 3, 5), jelly = ring(2),
            chocolate = holes(2 to 2, 2 to 6, 6 to 2, 6 to 6)),
        make(26, "Sweet Storm", R3, 28, LevelGoalType.SCORE, six,
            "Storm the score! Chain cascades.", scoreTarget = 17000, blocked = diamond(4).let { d ->
                (0..8).flatMap { r -> (0..8).map { c -> Position(r, c) } }.filter { it !in d }
            }),
        make(27, "Toffee Twist", R3, 32, LevelGoalType.CREATE_SPECIALS, six,
            "Craft a full arsenal.",
            specialGoals = specials(SpecialType.HORIZONTAL_STRIPED to 3, SpecialType.WRAPPED to 3, SpecialType.COLOR_BOMB to 1),
            chocolate = holes(3 to 4, 5 to 4)),
        make(28, "Locked Cocoa", R3, 32, LevelGoalType.CLEAR_JELLY, six,
            "Locks AND chocolate.", jelly = checker(0), locks = holes(1 to 4, 4 to 1, 4 to 7, 7 to 4),
            chocolate = holes(4 to 4)),
        make(29, "Fortress Gate", R3, 34, LevelGoalType.COLLECT_CANDIES, six,
            "A big harvest.", candyGoals = collect(CandyType.ORANGE to 40, CandyType.PURPLE to 40, CandyType.YELLOW to 35),
            chocolate = holes(2 to 3, 2 to 5, 6 to 3, 6 to 5)),
        make(30, "Fortress Finale", R3, 34, LevelGoalType.CLEAR_JELLY, six,
            "Storm the fortress!", doubleJelly = ring(0), jelly = ring(2) + rect(4, 4, 3, 5),
            chocolate = holes(1 to 1, 1 to 7, 7 to 1, 7 to 7)),

        // ======================================================= REALM 4 : multi-objective
        make(31, "Twin Peaks", R4, 34, LevelGoalType.CLEAR_JELLY, six,
            "Clear the jelly AND drop the ingredients!", jelly = rect(5, 8, 0, 8),
            ingredientCols = listOf(2, 6), ingredientTarget = 2, extra = setOf(LevelGoalType.DROP_INGREDIENTS)),
        make(32, "Dizzy Diamond", R4, 32, LevelGoalType.CLEAR_JELLY, six,
            "A jelly diamond.", jelly = diamond(3), doubleJelly = holes(4 to 4),
            blocked = corners(2)),
        make(33, "Nut Maze", R4, 36, LevelGoalType.DROP_INGREDIENTS, six,
            "A maze of walls.", ingredientCols = listOf(1, 4, 7), ingredientTarget = 3,
            blocked = holes(2 to 2, 2 to 6, 3 to 4, 4 to 1, 4 to 7, 5 to 4, 6 to 2, 6 to 6)),
        make(34, "Lock Lane", R4, 32, LevelGoalType.CLEAR_JELLY, six,
            "Licorice on every jelly.", jelly = checker(0), locks = checker(0).filter { it.row in 2..6 && it.col in 2..6 }),
        make(35, "Spectrum Rush", R4, 30, LevelGoalType.SCORE, six,
            "A pure score sprint.", scoreTarget = 20000),
        make(36, "Striped Storm", R4, 32, LevelGoalType.CREATE_SPECIALS, six,
            "Lots of stripes plus a bomb.",
            specialGoals = specials(SpecialType.HORIZONTAL_STRIPED to 7, SpecialType.COLOR_BOMB to 1),
            blocked = corners(1)),
        make(37, "Double Jelly Dash", R4, 34, LevelGoalType.CLEAR_JELLY, six,
            "Every tile is double jelly!", doubleJelly = rect(2, 6, 2, 6), blocked = corners(2)),
        make(38, "Choco Cascade", R4, 34, LevelGoalType.CLEAR_JELLY, six,
            "Chocolate on the jelly line.", jelly = rect(4, 4, 0, 8) + rect(2, 2, 1, 7) + rect(6, 6, 1, 7),
            chocolate = holes(3 to 2, 3 to 6, 5 to 2, 5 to 6), extra = emptySet()),
        make(39, "Gem Drop", R4, 38, LevelGoalType.DROP_INGREDIENTS, six,
            "Four ingredients, chocolate, locks.", ingredientCols = listOf(1, 3, 5, 7), ingredientTarget = 4,
            chocolate = holes(4 to 4, 3 to 2, 3 to 6), locks = holes(6 to 1, 6 to 7)),
        make(40, "Labyrinth Finale", R4, 36, LevelGoalType.CLEAR_JELLY, six,
            "Jelly + ingredients + locks.", jelly = ring(1) + rect(4, 4, 2, 6),
            ingredientCols = listOf(2, 6), ingredientTarget = 2, extra = setOf(LevelGoalType.DROP_INGREDIENTS),
            locks = holes(1 to 1, 1 to 7, 7 to 1, 7 to 7)),

        // ======================================================= REALM 5 : the summit
        make(41, "Royal Welcome", R5, 30, LevelGoalType.SCORE, six,
            "Impress the court!", scoreTarget = 24000, chocolate = holes(4 to 4)),
        make(42, "Crown Jewels", R5, 34, LevelGoalType.COLLECT_CANDIES, six,
            "Collect the royal colours.", candyGoals = collect(CandyType.RED to 45, CandyType.BLUE to 45, CandyType.YELLOW to 40),
            locks = holes(3 to 3, 3 to 5, 5 to 3, 5 to 5)),
        make(43, "Throne Room", R5, 36, LevelGoalType.CLEAR_JELLY, six,
            "Double jelly throne.", doubleJelly = rect(3, 5, 2, 6), jelly = ring(1),
            chocolate = holes(4 to 4)),
        make(44, "Sugar Bridge", R5, 38, LevelGoalType.DROP_INGREDIENTS, six,
            "A long way down.", ingredientCols = listOf(0, 2, 6, 8), ingredientTarget = 4,
            blocked = holes(4 to 3, 4 to 4, 4 to 5), chocolate = holes(6 to 1, 6 to 7)),
        make(45, "Master Chef", R5, 34, LevelGoalType.CREATE_SPECIALS, six,
            "Forge every special.",
            specialGoals = specials(SpecialType.WRAPPED to 4, SpecialType.HORIZONTAL_STRIPED to 3, SpecialType.COLOR_BOMB to 2)),
        make(46, "Jelly Palace", R5, 36, LevelGoalType.CLEAR_JELLY, six,
            "The whole palace floor.", jelly = checker(0), doubleJelly = rect(3, 5, 3, 5),
            locks = holes(1 to 1, 1 to 7, 7 to 1, 7 to 7)),
        make(47, "Storm the Castle", R5, 32, LevelGoalType.SCORE, six,
            "The score of a lifetime!", scoreTarget = 30000, chocolate = holes(2 to 2, 2 to 6, 6 to 2, 6 to 6)),
        make(48, "Golden Cascade", R5, 40, LevelGoalType.DROP_INGREDIENTS, six,
            "Everything, everywhere.", ingredientCols = listOf(1, 4, 7), ingredientTarget = 3,
            jelly = rect(6, 8, 0, 8), extra = setOf(LevelGoalType.CLEAR_JELLY),
            chocolate = holes(4 to 2, 4 to 6), locks = holes(5 to 4)),
        make(49, "Crown Guard", R5, 38, LevelGoalType.CLEAR_JELLY, six,
            "Nearly there!", doubleJelly = ring(0) + ring(2), jelly = ring(4),
            chocolate = holes(1 to 4, 7 to 4, 4 to 1, 4 to 7)),
        make(50, "Sugar Supreme", R5, 42, LevelGoalType.CLEAR_JELLY, six,
            "The grand finale: jelly, locks, chocolate and ingredients!",
            jelly = (0..8).flatMap { r -> (0..8).map { c -> Position(r, c) } },
            doubleJelly = rect(3, 5, 3, 5), ingredientCols = listOf(2, 6), ingredientTarget = 2,
            extra = setOf(LevelGoalType.DROP_INGREDIENTS),
            chocolate = holes(1 to 4, 7 to 4), locks = holes(1 to 1, 1 to 7, 7 to 1, 7 to 7))
    )

    fun getLevel(levelNumber: Int): LevelConfig {
        return levels.find { it.levelNumber == levelNumber } ?: levels.first()
    }

    /** First level of each realm and its banner title. */
    val realmStarts: Map<Int, String> = mapOf(1 to R1, 11 to R2, 21 to R3, 31 to R4, 41 to R5)
}
