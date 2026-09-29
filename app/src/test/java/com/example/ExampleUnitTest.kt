package com.example

import com.example.game.engine.MatchEngine
import com.example.game.levels.LevelsCatalog
import com.example.game.model.CandyTile
import com.example.game.model.CandyType
import com.example.game.model.Cell
import com.example.game.model.IngredientType
import com.example.game.model.LevelGoalType
import com.example.game.model.Position
import com.example.game.model.SpecialType
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    private val size = 9

    private fun createNonMatchingBoard(): List<MutableList<Cell>> {
        val cycle = listOf(CandyType.BLUE, CandyType.PURPLE, CandyType.ORANGE, CandyType.GREEN)
        return List(size) { r ->
            MutableList(size) { c ->
                val type = cycle[(r * 2 + c) % cycle.size]
                Cell(row = r, col = c, candy = CandyTile(type = type))
            }
        }
    }

    private fun put(board: List<MutableList<Cell>>, r: Int, c: Int, type: CandyType, special: SpecialType = SpecialType.NONE) {
        board[r][c] = board[r][c].copy(candy = CandyTile(type = type, special = special))
    }

    // ------------------------------------------------------------------ catalog

    @Test
    fun catalogHasFiftyLevelsWithNineByNineBoards() {
        assertEquals(50, LevelsCatalog.levels.size)
        LevelsCatalog.levels.forEachIndexed { index, level ->
            assertEquals(index + 1, level.levelNumber)
            assertEquals(9, level.rows)
            assertEquals(9, level.cols)
            assertTrue(level.maxMoves in 15..60)
        }
    }

    @Test
    fun levelDefinitionsAreConsistent() {
        LevelsCatalog.levels.forEach { level ->
            val n = level.levelNumber
            level.candyGoals.forEach {
                assertTrue("L$n goal colour missing from palette", level.availableColors.contains(it.type))
            }
            if (level.goalTypes.contains(LevelGoalType.DROP_INGREDIENTS)) {
                assertTrue("L$n needs ingredient starts", level.ingredientStart.size >= level.ingredientTarget)
                assertTrue(level.ingredientTarget > 0)
            }
            level.ingredientStart.forEach {
                assertFalse("L$n ingredient on wall", level.blockedCells.contains(it))
                assertFalse("L$n ingredient on chocolate", level.chocolateMap.contains(it))
            }
            assertTrue(level.star1Score <= level.star2Score && level.star2Score <= level.star3Score)
        }
    }

    @Test
    fun everyLevelGeneratesPlayableMatchFreeBoard() {
        LevelsCatalog.levels.forEach { level ->
            val board = MatchEngine.generateInitialBoard(level)
            assertEquals(9, board.size)
            assertEquals("L${level.levelNumber} starts with a match", 0, MatchEngine.detectMatches(board).matchedPositions.size)
            assertNotNull("L${level.levelNumber} has no legal move", MatchEngine.findHintMove(board))
            assertEquals(level.ingredientStart.size, MatchEngine.countIngredients(board))
            if (level.goalTypes.contains(LevelGoalType.CLEAR_JELLY)) {
                assertTrue("L${level.levelNumber} has no jelly", board.sumOf { row -> row.sumOf { it.jellyLevel } } > 0)
            }
        }
    }

    // ------------------------------------------------------------------ matching

    @Test
    fun match3Detection() {
        val board = createNonMatchingBoard()
        for (c in 0..2) put(board, 0, c, CandyType.RED)
        val detection = MatchEngine.detectMatches(board)
        assertEquals(3, detection.matchedPositions.size)
    }

    @Test
    fun horizontalFourMakesVerticalStripedAndVerticalFourMakesHorizontalStriped() {
        val h = createNonMatchingBoard()
        for (c in 1..4) put(h, 2, c, CandyType.YELLOW)
        val dh = MatchEngine.detectMatches(h)
        assertEquals(SpecialType.VERTICAL_STRIPED, dh.newSpecialsToSpawn.values.first().second)

        val v = createNonMatchingBoard()
        for (r in 1..4) put(v, r, 6, CandyType.YELLOW)
        val dv = MatchEngine.detectMatches(v)
        assertEquals(SpecialType.HORIZONTAL_STRIPED, dv.newSpecialsToSpawn.values.first().second)
    }

    @Test
    fun fiveInARowMakesColorBomb() {
        val board = createNonMatchingBoard()
        for (c in 0..4) put(board, 3, c, CandyType.RED)
        val detection = MatchEngine.detectMatches(board)
        assertEquals(5, detection.matchedPositions.size)
        assertEquals(SpecialType.COLOR_BOMB, detection.newSpecialsToSpawn.values.first().second)
    }

    @Test
    fun tShapeMakesWrappedCandy() {
        val board = createNonMatchingBoard()
        for (c in 2..4) put(board, 4, c, CandyType.RED)
        put(board, 5, 3, CandyType.RED)
        put(board, 6, 3, CandyType.RED)
        val detection = MatchEngine.detectMatches(board)
        assertEquals(5, detection.matchedPositions.size)
        assertEquals(SpecialType.WRAPPED, detection.newSpecialsToSpawn.values.first().second)
    }

    @Test
    fun ingredientsAndColorBombsNeverMatch() {
        val board = createNonMatchingBoard()
        for (c in 0..2) {
            board[0][c] = board[0][c].copy(candy = CandyTile(type = CandyType.RED, ingredient = IngredientType.CHERRY))
        }
        assertEquals(0, MatchEngine.detectMatches(board).matchedPositions.size)
    }

    // ------------------------------------------------------------------ specials

    @Test
    fun stripedCandyClearsItsLine() {
        val board = createNonMatchingBoard()
        put(board, 4, 4, CandyType.RED, SpecialType.HORIZONTAL_STRIPED)
        val cleared = MatchEngine.expandSpecialDetonations(setOf(Position(4, 4)), board)
        assertEquals(9, cleared.size)
        assertTrue(cleared.all { it.row == 4 })
    }

    @Test
    fun wrappedCandyClearsThreeByThree() {
        val board = createNonMatchingBoard()
        put(board, 4, 4, CandyType.RED, SpecialType.WRAPPED)
        val cleared = MatchEngine.expandSpecialDetonations(setOf(Position(4, 4)), board)
        assertEquals(9, cleared.size)
    }

    @Test
    fun doubleStripedComboClearsCross() {
        val board = createNonMatchingBoard()
        put(board, 4, 4, CandyType.RED, SpecialType.HORIZONTAL_STRIPED)
        put(board, 4, 5, CandyType.RED, SpecialType.VERTICAL_STRIPED)
        val cleared = MatchEngine.handleDirectSpecialCombo(Position(4, 4), Position(4, 5), board)
        assertNotNull(cleared)
        assertEquals(17, cleared!!.size)
    }

    @Test
    fun doubleColorBombClearsEverything() {
        val board = createNonMatchingBoard()
        put(board, 4, 4, CandyType.RED, SpecialType.COLOR_BOMB)
        put(board, 4, 5, CandyType.RED, SpecialType.COLOR_BOMB)
        val cleared = MatchEngine.handleDirectSpecialCombo(Position(4, 4), Position(4, 5), board)
        assertEquals(81, cleared!!.size)
    }

    @Test
    fun colorBombCannotBeSwappedWithIngredient() {
        val board = createNonMatchingBoard()
        put(board, 0, 0, CandyType.RED, SpecialType.COLOR_BOMB)
        board[0][1] = board[0][1].copy(candy = CandyTile(type = CandyType.RED, ingredient = IngredientType.HAZELNUT))
        assertFalse(MatchEngine.isValidSwap(Position(0, 0), Position(0, 1), board))
    }

    // ------------------------------------------------------------------ blockers

    @Test
    fun lockedCandyCannotBeSwappedButCanBeCleared() {
        val board = createNonMatchingBoard()
        board[0][0] = board[0][0].copy(lockLevel = 1)
        assertFalse(MatchEngine.isValidSwap(Position(0, 0), Position(0, 1), board))

        val result = MatchEngine.clearPositions(board, setOf(Position(0, 0)), emptyMap())
        assertEquals(0, result.board[0][0].lockLevel)
        assertEquals(1, result.locksBroken)
        assertNull(result.board[0][0].candy)
    }

    @Test
    fun clearingNextToChocolateEatsIt() {
        val board = createNonMatchingBoard()
        board[3][3] = board[3][3].copy(candy = null, isPlayable = false, isChocolate = true)
        val result = MatchEngine.clearPositions(board, setOf(Position(3, 4)), emptyMap())
        assertEquals(1, result.chocolateDestroyed)
        assertFalse(result.board[3][3].isChocolate)
        assertTrue(result.board[3][3].isPlayable)
    }

    @Test
    fun chocolateSpreadsIntoNeighbouringCandy() {
        val board = createNonMatchingBoard()
        board[4][4] = board[4][4].copy(candy = null, isPlayable = false, isChocolate = true)
        val spread = MatchEngine.spreadChocolate(board, Random(1))
        assertNotNull(spread)
        assertEquals(2, spread!!.sumOf { row -> row.count { it.isChocolate } })
    }

    @Test
    fun jellyLosesOneLayerPerClear() {
        val board = createNonMatchingBoard()
        board[2][2] = board[2][2].copy(jellyLevel = 2)
        val once = MatchEngine.clearPositions(board, setOf(Position(2, 2)), emptyMap()).board
        assertEquals(1, once[2][2].jellyLevel)
        val twice = MatchEngine.clearPositions(once, setOf(Position(2, 2)), emptyMap()).board
        assertEquals(0, twice[2][2].jellyLevel)
    }

    // ------------------------------------------------------------------ gravity & ingredients

    @Test
    fun gravityRefillsEveryPlayableCell() {
        val config = LevelsCatalog.getLevel(1)
        val board = MatchEngine.generateInitialBoard(config)
        val cleared = board.mapIndexed { r, row -> row.map { if (r >= 6) it.copy(candy = null) else it } }
        val refilled = MatchEngine.applyGravityAndRefill(cleared, config.availableColors)
        assertEquals(0, refilled.flatten().count { it.isPlayable && it.candy == null })
    }

    @Test
    fun lockedCandyStaysAndCandiesAboveItStack() {
        val board = createNonMatchingBoard()
        board[5][2] = board[5][2].copy(lockLevel = 1)
        val lockedTile = board[5][2].candy
        // remove the cell just below the lock and the one above it
        board[6][2] = board[6][2].copy(candy = null)
        board[4][2] = board[4][2].copy(candy = null)
        val out = MatchEngine.applyGravityAndRefill(board, listOf(CandyType.RED, CandyType.BLUE, CandyType.GREEN))
        assertEquals(lockedTile?.id, out[5][2].candy?.id)
        assertEquals(0, out.flatten().count { it.isPlayable && it.candy == null })
    }

    @Test
    fun ingredientExitsAtBottomRow() {
        val config = LevelsCatalog.getLevel(7)
        val board = createNonMatchingBoard()
        board[8][2] = board[8][2].copy(candy = CandyTile(type = CandyType.RED, ingredient = IngredientType.CHERRY))
        val (after, exited) = MatchEngine.collectExitedIngredients(board, config)
        assertEquals(1, exited)
        assertNull(after[8][2].candy)
    }

    // ------------------------------------------------------------------ fuzz

    @Test
    fun randomPlayKeepsEveryLevelConsistent() {
        val rng = Random(42)
        LevelsCatalog.levels.forEach { level ->
            var board = MatchEngine.generateInitialBoard(level, rng)
            repeat(25) {
                val hint = MatchEngine.findHintMove(board) ?: run {
                    board = MatchEngine.reshuffleBoard(board, level.availableColors, rng)
                    return@repeat
                }
                val swapped = MatchEngine.swapCells(hint.first, hint.second, board)
                var current = swapped
                val combo = MatchEngine.handleDirectSpecialCombo(hint.first, hint.second, board)
                var guard = 0
                if (combo != null) {
                    current = MatchEngine.clearPositions(current, combo, emptyMap()).board
                    current = MatchEngine.applyGravityAndRefill(current, level.availableColors, rng = rng)
                }
                while (guard < 50) {
                    guard++
                    val detection = MatchEngine.detectMatches(current)
                    if (detection.matchedPositions.isEmpty()) break
                    val full = MatchEngine.expandSpecialDetonations(detection.matchedPositions, current)
                    current = MatchEngine.clearPositions(current, full, detection.newSpecialsToSpawn).board
                    current = MatchEngine.applyGravityAndRefill(current, level.availableColors, rng = rng)
                }
                assertTrue("L${level.levelNumber} cascade never settled", guard < 50)
                assertEquals(
                    "L${level.levelNumber} left empty playable cells",
                    0,
                    current.flatten().count { it.isPlayable && it.candy == null }
                )
                board = current
            }
        }
    }
}
