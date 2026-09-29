package com.example.game.engine

import com.example.game.model.CandyTile
import com.example.game.model.CandyType
import com.example.game.model.Cell
import com.example.game.model.LevelConfig
import com.example.game.model.Position
import com.example.game.model.SpecialType

data class MatchGroup(
    val positions: Set<Position>,
    val candyType: CandyType,
    val resultingSpecial: SpecialType = SpecialType.NONE,
    val specialSpawnPos: Position? = null
)

data class MatchDetectionResult(
    val matchedPositions: Set<Position>,
    val matchGroups: List<MatchGroup>,
    val newSpecialsToSpawn: Map<Position, Pair<CandyType, SpecialType>>
)

data class BoardCascadeStep(
    val clearedPositions: Set<Position>,
    val clearedCandiesCountByType: Map<CandyType, Int>,
    val jelliesDamaged: List<Position>,
    val scoreGained: Int,
    val comboMultiplier: Int,
    val updatedBoard: List<List<Cell>>
)

object MatchEngine {

    fun generateInitialBoard(config: LevelConfig): List<List<Cell>> {
        var attempts = 0
        var board: List<List<Cell>>
        do {
            board = createRawBoard(config)
            attempts++
        } while ((hasAnyInitialMatch(board) || !hasPossibleMoves(board, config.availableColors)) && attempts < 100)

        // If loop finished and still has match, surgically fix matches
        if (hasAnyInitialMatch(board)) {
            board = removeInitialMatches(board, config)
        }
        return board
    }

    private fun createRawBoard(config: LevelConfig): List<List<Cell>> {
        val rows = config.rows
        val cols = config.cols
        val blocked = config.blockedCells.toSet()
        val singleJellies = config.initialJellyMap.toSet()
        val doubleJellies = config.doubleJellyMap.toSet()

        return List(rows) { r ->
            List(cols) { c ->
                val pos = Position(r, c)
                if (blocked.contains(pos)) {
                    Cell(row = r, col = c, candy = null, isPlayable = false)
                } else {
                    val jelly = when {
                        doubleJellies.contains(pos) -> 2
                        singleJellies.contains(pos) -> 1
                        else -> 0
                    }
                    val type = config.availableColors.random()
                    Cell(
                        row = r,
                        col = c,
                        candy = CandyTile(type = type),
                        jellyLevel = jelly,
                        isPlayable = true
                    )
                }
            }
        }
    }

    private fun hasAnyInitialMatch(board: List<List<Cell>>): Boolean {
        val rows = board.size
        val cols = board[0].size

        // Check horizontal
        for (r in 0 until rows) {
            for (c in 0 until cols - 2) {
                val t1 = board[r][c].candy?.type ?: continue
                val t2 = board[r][c + 1].candy?.type ?: continue
                val t3 = board[r][c + 2].candy?.type ?: continue
                if (t1 == t2 && t2 == t3) return true
            }
        }

        // Check vertical
        for (c in 0 until cols) {
            for (r in 0 until rows - 2) {
                val t1 = board[r][c].candy?.type ?: continue
                val t2 = board[r + 1][c].candy?.type ?: continue
                val t3 = board[r + 2][c].candy?.type ?: continue
                if (t1 == t2 && t2 == t3) return true
            }
        }
        return false
    }

    private fun removeInitialMatches(board: List<List<Cell>>, config: LevelConfig): List<List<Cell>> {
        val mutable = board.map { row -> row.toMutableList() }.toMutableList()
        val rows = board.size
        val cols = board[0].size

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (!mutable[r][c].isPlayable || mutable[r][c].candy == null) continue
                var currentType = mutable[r][c].candy!!.type
                var attempts = 0
                while (attempts < 10) {
                    val matchH = (c >= 2 && mutable[r][c - 1].candy?.type == currentType && mutable[r][c - 2].candy?.type == currentType) ||
                            (c <= cols - 3 && mutable[r][c + 1].candy?.type == currentType && mutable[r][c + 2].candy?.type == currentType) ||
                            (c in 1 until cols - 1 && mutable[r][c - 1].candy?.type == currentType && mutable[r][c + 1].candy?.type == currentType)

                    val matchV = (r >= 2 && mutable[r - 1][c].candy?.type == currentType && mutable[r - 2][c].candy?.type == currentType) ||
                            (r <= rows - 3 && mutable[r + 1][c].candy?.type == currentType && mutable[r + 2][c].candy?.type == currentType) ||
                            (r in 1 until rows - 1 && mutable[r - 1][c].candy?.type == currentType && mutable[r + 1][c].candy?.type == currentType)

                    if (matchH || matchV) {
                        val otherTypes = config.availableColors.filter { it != currentType }
                        currentType = otherTypes.random()
                        mutable[r][c] = mutable[r][c].copy(candy = CandyTile(type = currentType))
                        attempts++
                    } else {
                        break
                    }
                }
            }
        }
        return mutable.map { it.toList() }
    }

    fun isValidSwap(pos1: Position, pos2: Position, board: List<List<Cell>>): Boolean {
        if (!pos1.isAdjacentTo(pos2)) return false
        val cell1 = board.getOrNull(pos1.row)?.getOrNull(pos1.col) ?: return false
        val cell2 = board.getOrNull(pos2.row)?.getOrNull(pos2.col) ?: return false
        if (!cell1.isPlayable || !cell2.isPlayable) return false
        val candy1 = cell1.candy ?: return false
        val candy2 = cell2.candy ?: return false

        // Color bomb swapped with anything is ALWAYS valid!
        if (candy1.special == SpecialType.COLOR_BOMB || candy2.special == SpecialType.COLOR_BOMB) {
            return true
        }

        // Two special candies swapped together is ALWAYS valid!
        if (candy1.special.isSpecial && candy2.special.isSpecial) {
            return true
        }

        // Otherwise simulate swap and check for standard match-3+
        val swappedBoard = swapCells(pos1, pos2, board)
        val matches = detectMatches(swappedBoard)
        return matches.matchedPositions.isNotEmpty()
    }

    fun swapCells(pos1: Position, pos2: Position, board: List<List<Cell>>): List<List<Cell>> {
        val rows = board.size
        val cols = board[0].size
        val candy1 = board[pos1.row][pos1.col].candy
        val candy2 = board[pos2.row][pos2.col].candy

        return List(rows) { r ->
            List(cols) { c ->
                val current = board[r][c]
                when {
                    r == pos1.row && c == pos1.col -> current.copy(candy = candy2)
                    r == pos2.row && c == pos2.col -> current.copy(candy = candy1)
                    else -> current
                }
            }
        }
    }

    /**
     * Detects 3, 4, and 5 candy matches across rows and columns.
     * Identifies T/L shapes for Wrapped candy and straight lines for Striped/Color Bombs.
     */
    fun detectMatches(
        board: List<List<Cell>>,
        lastSwappedPosition: Position? = null
    ): MatchDetectionResult {
        val rows = board.size
        val cols = board[0].size
        val horizontalMatches = mutableListOf<List<Position>>()
        val verticalMatches = mutableListOf<List<Position>>()

        // Scan Horizontal
        for (r in 0 until rows) {
            var c = 0
            while (c < cols) {
                val cell = board[r][c]
                val candy = cell.candy
                if (!cell.isPlayable || candy == null || candy.special == SpecialType.COLOR_BOMB) {
                    c++
                    continue
                }
                val match = mutableListOf(Position(r, c))
                var nextC = c + 1
                while (nextC < cols) {
                    val nextCell = board[r][nextC]
                    val nextCandy = nextCell.candy
                    if (nextCell.isPlayable && nextCandy != null &&
                        nextCandy.type == candy.type && nextCandy.special != SpecialType.COLOR_BOMB
                    ) {
                        match.add(Position(r, nextC))
                        nextC++
                    } else {
                        break
                    }
                }
                if (match.size >= 3) {
                    horizontalMatches.add(match)
                }
                c = nextC
            }
        }

        // Scan Vertical
        for (c in 0 until cols) {
            var r = 0
            while (r < rows) {
                val cell = board[r][c]
                val candy = cell.candy
                if (!cell.isPlayable || candy == null || candy.special == SpecialType.COLOR_BOMB) {
                    r++
                    continue
                }
                val match = mutableListOf(Position(r, c))
                var nextR = r + 1
                while (nextR < rows) {
                    val nextCell = board[nextR][c]
                    val nextCandy = nextCell.candy
                    if (nextCell.isPlayable && nextCandy != null &&
                        nextCandy.type == candy.type && nextCandy.special != SpecialType.COLOR_BOMB
                    ) {
                        match.add(Position(nextR, c))
                        nextR++
                    } else {
                        break
                    }
                }
                if (match.size >= 3) {
                    verticalMatches.add(match)
                }
                r = nextR
            }
        }

        val allMatchedPositions = mutableSetOf<Position>()
        val matchGroups = mutableListOf<MatchGroup>()
        val newSpecials = mutableMapOf<Position, Pair<CandyType, SpecialType>>()

        // Check for T or L intersections (Wrapped candy: 5+ items crossing)
        val usedHorizontals = mutableSetOf<Int>()
        val usedVerticals = mutableSetOf<Int>()

        for (hIdx in horizontalMatches.indices) {
            val hMatch = horizontalMatches[hIdx]
            for (vIdx in verticalMatches.indices) {
                val vMatch = verticalMatches[vIdx]
                val intersection = hMatch.intersect(vMatch.toSet())
                if (intersection.isNotEmpty()) {
                    val crossPoint = intersection.first()
                    val combinedPositions = (hMatch + vMatch).toSet()
                    val candyType = board[crossPoint.row][crossPoint.col].candy!!.type
                    val spawnPos = if (lastSwappedPosition != null && combinedPositions.contains(lastSwappedPosition)) {
                        lastSwappedPosition
                    } else {
                        crossPoint
                    }
                    newSpecials[spawnPos] = candyType to SpecialType.WRAPPED
                    matchGroups.add(MatchGroup(combinedPositions, candyType, SpecialType.WRAPPED, spawnPos))
                    allMatchedPositions.addAll(combinedPositions)
                    usedHorizontals.add(hIdx)
                    usedVerticals.add(vIdx)
                }
            }
        }

        // Process remaining Horizontal Matches
        for (hIdx in horizontalMatches.indices) {
            if (usedHorizontals.contains(hIdx)) continue
            val hMatch = horizontalMatches[hIdx]
            val candyType = board[hMatch.first().row][hMatch.first().col].candy!!.type
            val matchSize = hMatch.size

            val spawnPos = if (lastSwappedPosition != null && hMatch.contains(lastSwappedPosition)) {
                lastSwappedPosition
            } else {
                hMatch[matchSize / 2]
            }

            when {
                matchSize >= 5 -> {
                    newSpecials[spawnPos] = candyType to SpecialType.COLOR_BOMB
                    matchGroups.add(MatchGroup(hMatch.toSet(), candyType, SpecialType.COLOR_BOMB, spawnPos))
                }
                matchSize == 4 -> {
                    newSpecials[spawnPos] = candyType to SpecialType.HORIZONTAL_STRIPED
                    matchGroups.add(MatchGroup(hMatch.toSet(), candyType, SpecialType.HORIZONTAL_STRIPED, spawnPos))
                }
                else -> {
                    matchGroups.add(MatchGroup(hMatch.toSet(), candyType, SpecialType.NONE, null))
                }
            }
            allMatchedPositions.addAll(hMatch)
        }

        // Process remaining Vertical Matches
        for (vIdx in verticalMatches.indices) {
            if (usedVerticals.contains(vIdx)) continue
            val vMatch = verticalMatches[vIdx]
            val candyType = board[vMatch.first().row][vMatch.first().col].candy!!.type
            val matchSize = vMatch.size

            val spawnPos = if (lastSwappedPosition != null && vMatch.contains(lastSwappedPosition)) {
                lastSwappedPosition
            } else {
                vMatch[matchSize / 2]
            }

            when {
                matchSize >= 5 -> {
                    newSpecials[spawnPos] = candyType to SpecialType.COLOR_BOMB
                    matchGroups.add(MatchGroup(vMatch.toSet(), candyType, SpecialType.COLOR_BOMB, spawnPos))
                }
                matchSize == 4 -> {
                    newSpecials[spawnPos] = candyType to SpecialType.VERTICAL_STRIPED
                    matchGroups.add(MatchGroup(vMatch.toSet(), candyType, SpecialType.VERTICAL_STRIPED, spawnPos))
                }
                else -> {
                    matchGroups.add(MatchGroup(vMatch.toSet(), candyType, SpecialType.NONE, null))
                }
            }
            allMatchedPositions.addAll(vMatch)
        }

        return MatchDetectionResult(
            matchedPositions = allMatchedPositions,
            matchGroups = matchGroups,
            newSpecialsToSpawn = newSpecials
        )
    }

    /**
     * Handles detonating special candies recursively (e.g. striped row/col, wrapped 3x3 blast).
     */
    fun expandSpecialDetonations(
        startingPositions: Set<Position>,
        board: List<List<Cell>>
    ): Set<Position> {
        val totalCleared = mutableSetOf<Position>()
        val queue = ArrayDeque<Position>()
        queue.addAll(startingPositions)

        val rows = board.size
        val cols = board[0].size

        while (queue.isNotEmpty()) {
            val pos = queue.removeFirst()
            if (!totalCleared.add(pos)) continue

            val cell = board.getOrNull(pos.row)?.getOrNull(pos.col) ?: continue
            val candy = cell.candy ?: continue

            when (candy.special) {
                SpecialType.HORIZONTAL_STRIPED -> {
                    for (c in 0 until cols) {
                        val p = Position(pos.row, c)
                        if (!totalCleared.contains(p)) queue.add(p)
                    }
                }
                SpecialType.VERTICAL_STRIPED -> {
                    for (r in 0 until rows) {
                        val p = Position(r, pos.col)
                        if (!totalCleared.contains(p)) queue.add(p)
                    }
                }
                SpecialType.WRAPPED -> {
                    for (dr in -1..1) {
                        for (dc in -1..1) {
                            val r = pos.row + dr
                            val c = pos.col + dc
                            if (r in 0 until rows && c in 0 until cols) {
                                val p = Position(r, c)
                                if (!totalCleared.contains(p)) queue.add(p)
                            }
                        }
                    }
                }
                SpecialType.COLOR_BOMB -> {
                    // Explodes candies of a random or most prevalent color
                    val targetType = candy.type
                    for (r in 0 until rows) {
                        for (c in 0 until cols) {
                            val other = board[r][c].candy
                            if (other != null && other.type == targetType) {
                                val p = Position(r, c)
                                if (!totalCleared.contains(p)) queue.add(p)
                            }
                        }
                    }
                }
                SpecialType.NONE -> {
                    // Regular candy, no extra detonation
                }
            }
        }
        return totalCleared
    }

    /**
     * Handles when two special candies or a color bomb are directly swapped together!
     */
    fun handleDirectSpecialCombo(
        pos1: Position,
        pos2: Position,
        board: List<List<Cell>>
    ): Set<Position>? {
        val candy1 = board[pos1.row][pos1.col].candy ?: return null
        val candy2 = board[pos2.row][pos2.col].candy ?: return null

        val rows = board.size
        val cols = board[0].size

        // 1. Color Bomb + Color Bomb -> Clears the entire board!
        if (candy1.special == SpecialType.COLOR_BOMB && candy2.special == SpecialType.COLOR_BOMB) {
            val all = mutableSetOf<Position>()
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    if (board[r][c].isPlayable) all.add(Position(r, c))
                }
            }
            return all
        }

        // 2. Color Bomb + Regular Candy -> Clears all candies of that regular candy's color
        if (candy1.special == SpecialType.COLOR_BOMB || candy2.special == SpecialType.COLOR_BOMB) {
            val colorBombPos = if (candy1.special == SpecialType.COLOR_BOMB) pos1 else pos2
            val otherCandy = if (candy1.special == SpecialType.COLOR_BOMB) candy2 else candy1
            val targetColor = otherCandy.type

            val cleared = mutableSetOf(pos1, pos2)
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val other = board[r][c].candy
                    if (other != null && other.type == targetColor) {
                        cleared.add(Position(r, c))
                    }
                }
            }
            return expandSpecialDetonations(cleared, board)
        }

        // 3. Striped + Striped -> Clears both the row and column in a giant cross!
        if (candy1.special.isStriped && candy2.special.isStriped) {
            val cleared = mutableSetOf<Position>()
            for (c in 0 until cols) cleared.add(Position(pos2.row, c))
            for (r in 0 until rows) cleared.add(Position(r, pos2.col))
            return expandSpecialDetonations(cleared, board)
        }

        // 4. Striped + Wrapped -> Giant 3-row and 3-column sweeping blast!
        if ((candy1.special.isStriped && candy2.special == SpecialType.WRAPPED) ||
            (candy2.special.isStriped && candy1.special == SpecialType.WRAPPED)
        ) {
            val cleared = mutableSetOf<Position>()
            for (dr in -1..1) {
                val r = pos2.row + dr
                if (r in 0 until rows) {
                    for (c in 0 until cols) cleared.add(Position(r, c))
                }
            }
            for (dc in -1..1) {
                val c = pos2.col + dc
                if (c in 0 until cols) {
                    for (r in 0 until rows) cleared.add(Position(r, c))
                }
            }
            return expandSpecialDetonations(cleared, board)
        }

        // 5. Wrapped + Wrapped -> 5x5 massive explosion!
        if (candy1.special == SpecialType.WRAPPED && candy2.special == SpecialType.WRAPPED) {
            val cleared = mutableSetOf<Position>()
            for (dr in -2..2) {
                for (dc in -2..2) {
                    val r = pos2.row + dr
                    val c = pos2.col + dc
                    if (r in 0 until rows && c in 0 until cols) {
                        cleared.add(Position(r, c))
                    }
                }
            }
            return expandSpecialDetonations(cleared, board)
        }

        return null
    }

    /**
     * Applies gravity downward in each column and refills empty spaces with new random candies.
     */
    fun applyGravityAndRefill(
        board: List<List<Cell>>,
        availableColors: List<CandyType>
    ): List<List<Cell>> {
        val rows = board.size
        val cols = board[0].size
        val mutable = board.map { row -> row.toMutableList() }.toMutableList()

        for (c in 0 until cols) {
            var emptyRow = rows - 1
            // Shift down existing candies
            for (r in rows - 1 downTo 0) {
                if (!mutable[r][c].isPlayable) continue
                if (mutable[r][c].candy != null) {
                    if (emptyRow != r) {
                        mutable[emptyRow][c] = mutable[emptyRow][c].copy(candy = mutable[r][c].candy)
                        mutable[r][c] = mutable[r][c].copy(candy = null)
                    }
                    emptyRow--
                    while (emptyRow >= 0 && !mutable[emptyRow][c].isPlayable) {
                        emptyRow--
                    }
                }
            }

            // Fill top empty spaces with fresh candies
            for (r in emptyRow downTo 0) {
                if (mutable[r][c].isPlayable && mutable[r][c].candy == null) {
                    val newType = availableColors.random()
                    mutable[r][c] = mutable[r][c].copy(candy = CandyTile(type = newType))
                }
            }
        }
        return mutable.map { it.toList() }
    }

    /**
     * Checks if at least one valid move exists on the board.
     */
    fun hasPossibleMoves(board: List<List<Cell>>, availableColors: List<CandyType>): Boolean {
        return findHintMove(board) != null
    }

    /**
     * Finds a valid move to suggest as a hint to the player.
     */
    fun findHintMove(board: List<List<Cell>>): Pair<Position, Position>? {
        val rows = board.size
        val cols = board[0].size

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val current = Position(r, c)
                // Check right swap
                if (c < cols - 1) {
                    val right = Position(r, c + 1)
                    if (isValidSwap(current, right, board)) return current to right
                }
                // Check down swap
                if (r < rows - 1) {
                    val down = Position(r + 1, c)
                    if (isValidSwap(current, down, board)) return current to down
                }
            }
        }
        return null
    }

    /**
     * Reshuffles candies when no moves are possible.
     */
    fun reshuffleBoard(board: List<List<Cell>>, availableColors: List<CandyType>): List<List<Cell>> {
        val candies = board.flatMap { row -> row.mapNotNull { it.candy } }.shuffled()
        var candyIdx = 0

        val rows = board.size
        val cols = board[0].size
        var attempts = 0
        var newBoard: List<List<Cell>>

        do {
            candyIdx = 0
            val shuffledCandies = candies.shuffled()
            newBoard = List(rows) { r ->
                List(cols) { c ->
                    val cell = board[r][c]
                    if (cell.isPlayable) {
                        cell.copy(candy = shuffledCandies[candyIdx++])
                    } else {
                        cell
                    }
                }
            }
            attempts++
        } while (attempts < 20 && (!hasPossibleMoves(newBoard, availableColors) || hasAnyInitialMatch(newBoard)))

        return newBoard
    }
}
