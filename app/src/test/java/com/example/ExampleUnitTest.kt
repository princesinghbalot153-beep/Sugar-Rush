package com.example

import com.example.game.engine.MatchEngine
import com.example.game.levels.LevelsCatalog
import com.example.game.model.CandyTile
import com.example.game.model.CandyType
import com.example.game.model.Cell
import com.example.game.model.Position
import com.example.game.model.SpecialType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    private fun createNonMatchingBoard(): List<MutableList<Cell>> {
        val nonMatchingCycle = listOf(CandyType.BLUE, CandyType.PURPLE, CandyType.ORANGE, CandyType.GREEN)
        return List(8) { r ->
            MutableList(8) { c ->
                val type = nonMatchingCycle[(r * 2 + c) % nonMatchingCycle.size]
                Cell(row = r, col = c, candy = CandyTile(type = type))
            }
        }
    }

    @Test
    fun testLevelsCatalogHasFifteenLevels() {
        assertEquals(15, LevelsCatalog.levels.size)
        val level1 = LevelsCatalog.getLevel(1)
        assertEquals(1, level1.levelNumber)
        assertTrue(level1.maxMoves > 0)
    }

    @Test
    fun testInitialBoardGenerationHasNoImmediateMatches() {
        val config = LevelsCatalog.getLevel(1)
        val board = MatchEngine.generateInitialBoard(config)
        assertEquals(8, board.size)
        assertEquals(8, board[0].size)

        val matches = MatchEngine.detectMatches(board)
        assertEquals("Initial board should start without pending matches", 0, matches.matchedPositions.size)
    }

    @Test
    fun testMatch3Detection() {
        val board = createNonMatchingBoard()
        // Inject exactly 3 Red candies horizontally at (0, 0), (0, 1), (0, 2)
        board[0][0] = board[0][0].copy(candy = CandyTile(type = CandyType.RED))
        board[0][1] = board[0][1].copy(candy = CandyTile(type = CandyType.RED))
        board[0][2] = board[0][2].copy(candy = CandyTile(type = CandyType.RED))

        val detection = MatchEngine.detectMatches(board)
        assertTrue(detection.matchedPositions.contains(Position(0, 0)))
        assertTrue(detection.matchedPositions.contains(Position(0, 1)))
        assertTrue(detection.matchedPositions.contains(Position(0, 2)))
        assertEquals(3, detection.matchedPositions.size)
    }

    @Test
    fun testMatch4CreatesStripedCandy() {
        val board = createNonMatchingBoard()
        // Inject exactly 4 Yellow candies horizontally at (2, 1..4)
        for (c in 1..4) {
            board[2][c] = board[2][c].copy(candy = CandyTile(type = CandyType.YELLOW))
        }

        val detection = MatchEngine.detectMatches(board)
        assertEquals(4, detection.matchedPositions.size)
        assertEquals(1, detection.newSpecialsToSpawn.size)
        val special = detection.newSpecialsToSpawn.values.first()
        assertEquals(CandyType.YELLOW, special.first)
        assertEquals(SpecialType.HORIZONTAL_STRIPED, special.second)
    }

    @Test
    fun testMatch5CreatesColorBomb() {
        val board = createNonMatchingBoard()
        // Inject exactly 5 Red candies horizontally at (3, 0..4)
        for (c in 0..4) {
            board[3][c] = board[3][c].copy(candy = CandyTile(type = CandyType.RED))
        }

        val detection = MatchEngine.detectMatches(board)
        assertEquals(5, detection.matchedPositions.size)
        val special = detection.newSpecialsToSpawn.values.first()
        assertEquals(SpecialType.COLOR_BOMB, special.second)
    }

    @Test
    fun testGravityAndRefillLeavesNoEmptyCells() {
        val config = LevelsCatalog.getLevel(1)
        val board = MatchEngine.generateInitialBoard(config)

        // Clear row 7
        val clearedBoard = board.mapIndexed { r, row ->
            row.map { cell ->
                if (r == 7) cell.copy(candy = null) else cell
            }
        }

        val refilledBoard = MatchEngine.applyGravityAndRefill(clearedBoard, config.availableColors)
        val emptyCount = refilledBoard.flatten().count { it.candy == null }
        assertEquals("Gravity refill should fill all empty playable spaces", 0, emptyCount)
    }

    @Test
    fun testLevelPacingAndMovesEnsureExtendedGameplay() {
        // Every level should have >= 28 moves to guarantee 2-3 minutes of play
        LevelsCatalog.levels.forEach { level ->
            assertTrue("Level ${level.levelNumber} moves should be >= 28", level.maxMoves >= 28)
            assertTrue("Level ${level.levelNumber} targetScore should be >= 20000", level.targetScore >= 20000)
        }
    }
}
