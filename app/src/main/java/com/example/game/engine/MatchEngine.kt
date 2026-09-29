package com.example.game.engine

import com.example.game.model.CandyTile
import com.example.game.model.CandyType
import com.example.game.model.Cell
import com.example.game.model.IngredientType
import com.example.game.model.LevelConfig
import com.example.game.model.Position
import com.example.game.model.SpecialType
import kotlin.random.Random

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

/** Result of clearing a set of cells. */
data class ClearResult(
    val board: List<List<Cell>>,
    val chocolateDestroyed: Int,
    val locksBroken: Int
)

/** Pure game rules. No Android / Compose dependencies except colours inside CandyType. */
object MatchEngine {

    // ------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------

    /** The colour a cell contributes to matching, or null when it cannot be matched. */
    private fun matchKey(cell: Cell?): CandyType? {
        if (cell == null || !cell.isPlayable) return null
        val candy = cell.candy ?: return null
        if (candy.ingredient != null) return null
        if (candy.special == SpecialType.COLOR_BOMB) return null
        return candy.type
    }

    private fun inBounds(board: List<List<Cell>>, r: Int, c: Int): Boolean {
        return r >= 0 && r < board.size && c >= 0 && c < board[0].size
    }

    private fun isFreeForGravity(cell: Cell): Boolean {
        return cell.isPlayable && !(cell.lockLevel > 0 && cell.candy != null)
    }

    // ------------------------------------------------------------------------------------------
    // Board generation
    // ------------------------------------------------------------------------------------------

    fun generateInitialBoard(config: LevelConfig, rng: Random = Random.Default): List<List<Cell>> {
        var attempts = 0
        var board: List<List<Cell>>
        do {
            board = createRawBoard(config, rng)
            attempts++
        } while ((hasAnyInitialMatch(board) || !hasPossibleMoves(board, config.availableColors)) && attempts < 200)

        if (hasAnyInitialMatch(board)) {
            board = removeInitialMatches(board, config, rng)
        }
        return board
    }

    private fun createRawBoard(config: LevelConfig, rng: Random): List<List<Cell>> {
        val rows = config.rows
        val cols = config.cols
        val blocked = config.blockedCells.toSet()
        val chocolate = config.chocolateMap.toSet()
        val locks = config.lockMap.toSet()
        val singleJellies = config.initialJellyMap.toSet()
        val doubleJellies = config.doubleJellyMap.toSet()
        val ingredientStarts = config.ingredientStart

        return List(rows) { r ->
            List(cols) { c ->
                val pos = Position(r, c)
                when {
                    blocked.contains(pos) -> Cell(row = r, col = c, candy = null, isPlayable = false)
                    chocolate.contains(pos) -> Cell(row = r, col = c, candy = null, isPlayable = false, isChocolate = true)
                    else -> {
                        val jelly = when {
                            doubleJellies.contains(pos) -> 2
                            singleJellies.contains(pos) -> 1
                            else -> 0
                        }
                        val ingredientIdx = ingredientStarts.indexOf(pos)
                        val candy = if (ingredientIdx >= 0) {
                            val kind = IngredientType.entries[ingredientIdx % IngredientType.entries.size]
                            CandyTile(type = config.availableColors.first(), ingredient = kind)
                        } else {
                            CandyTile(type = config.availableColors.random(rng))
                        }
                        Cell(
                            row = r,
                            col = c,
                            candy = candy,
                            jellyLevel = jelly,
                            isPlayable = true,
                            lockLevel = if (locks.contains(pos) && ingredientIdx < 0) 1 else 0
                        )
                    }
                }
            }
        }
    }

    private fun hasAnyInitialMatch(board: List<List<Cell>>): Boolean {
        return detectMatches(board).matchedPositions.isNotEmpty()
    }

    private fun removeInitialMatches(
        board: List<List<Cell>>,
        config: LevelConfig,
        rng: Random
    ): List<List<Cell>> {
        val mutable = board.map { row -> row.toMutableList() }.toMutableList()
        val rows = board.size
        val cols = board[0].size

        fun keyAt(r: Int, c: Int): CandyType? = if (inBounds(mutable, r, c)) matchKey(mutable[r][c]) else null

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val cell = mutable[r][c]
                val candy = cell.candy ?: continue
                if (!cell.isPlayable || candy.ingredient != null) continue
                var currentType = candy.type
                var attempts = 0
                while (attempts < 12) {
                    val matchH = (keyAt(r, c - 1) == currentType && keyAt(r, c - 2) == currentType) ||
                        (keyAt(r, c + 1) == currentType && keyAt(r, c + 2) == currentType) ||
                        (keyAt(r, c - 1) == currentType && keyAt(r, c + 1) == currentType)
                    val matchV = (keyAt(r - 1, c) == currentType && keyAt(r - 2, c) == currentType) ||
                        (keyAt(r + 1, c) == currentType && keyAt(r + 2, c) == currentType) ||
                        (keyAt(r - 1, c) == currentType && keyAt(r + 1, c) == currentType)
                    if (matchH || matchV) {
                        val others = config.availableColors.filter { it != currentType }
                        if (others.isEmpty()) break
                        currentType = others.random(rng)
                        mutable[r][c] = mutable[r][c].copy(candy = candy.copy(type = currentType))
                        attempts++
                    } else {
                        break
                    }
                }
            }
        }
        return mutable.map { it.toList() }
    }

    // ------------------------------------------------------------------------------------------
    // Swapping
    // ------------------------------------------------------------------------------------------

    fun isValidSwap(pos1: Position, pos2: Position, board: List<List<Cell>>): Boolean {
        if (!pos1.isAdjacentTo(pos2)) return false
        val cell1 = board.getOrNull(pos1.row)?.getOrNull(pos1.col) ?: return false
        val cell2 = board.getOrNull(pos2.row)?.getOrNull(pos2.col) ?: return false
        if (!cell1.isPlayable || !cell2.isPlayable) return false
        if (cell1.lockLevel > 0 || cell2.lockLevel > 0) return false
        val candy1 = cell1.candy ?: return false
        val candy2 = cell2.candy ?: return false

        val bomb1 = candy1.special == SpecialType.COLOR_BOMB
        val bomb2 = candy2.special == SpecialType.COLOR_BOMB

        // A colour bomb can't be combined with a drop-down ingredient.
        if ((bomb1 && candy2.ingredient != null) || (bomb2 && candy1.ingredient != null)) return false
        if (bomb1 || bomb2) return true

        // Two special candies swapped together is always valid.
        if (candy1.special.isSpecial && candy2.special.isSpecial) return true

        val swappedBoard = swapCells(pos1, pos2, board)
        return detectMatches(swappedBoard).matchedPositions.isNotEmpty()
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

    // ------------------------------------------------------------------------------------------
    // Match detection
    // ------------------------------------------------------------------------------------------

    /**
     * Detects 3/4/5+ runs and T / L shapes.
     *  - 4 in a row  -> striped candy (stripes run perpendicular to the match, like Candy Crush)
     *  - T / L shape -> wrapped candy
     *  - 5 in a row  -> colour bomb
     */
    fun detectMatches(
        board: List<List<Cell>>,
        lastSwappedPosition: Position? = null,
        secondSwappedPosition: Position? = null
    ): MatchDetectionResult {
        val rows = board.size
        val cols = board[0].size
        val horizontalMatches = mutableListOf<List<Position>>()
        val verticalMatches = mutableListOf<List<Position>>()

        for (r in 0 until rows) {
            var c = 0
            while (c < cols) {
                val key = matchKey(board[r][c])
                if (key == null) {
                    c++
                    continue
                }
                var end = c + 1
                while (end < cols && matchKey(board[r][end]) == key) end++
                if (end - c >= 3) {
                    horizontalMatches.add((c until end).map { Position(r, it) })
                }
                c = end
            }
        }

        for (c in 0 until cols) {
            var r = 0
            while (r < rows) {
                val key = matchKey(board[r][c])
                if (key == null) {
                    r++
                    continue
                }
                var end = r + 1
                while (end < rows && matchKey(board[end][c]) == key) end++
                if (end - r >= 3) {
                    verticalMatches.add((r until end).map { Position(it, c) })
                }
                r = end
            }
        }

        val preferred = listOfNotNull(lastSwappedPosition, secondSwappedPosition)
        fun pickSpawn(group: Collection<Position>, fallback: Position): Position {
            return preferred.firstOrNull { group.contains(it) } ?: fallback
        }

        val allMatched = mutableSetOf<Position>()
        val groups = mutableListOf<MatchGroup>()
        val newSpecials = mutableMapOf<Position, Pair<CandyType, SpecialType>>()

        val usedH = mutableSetOf<Int>()
        val usedV = mutableSetOf<Int>()

        // T / L intersections -> wrapped candy
        for (hIdx in horizontalMatches.indices) {
            val hMatch = horizontalMatches[hIdx]
            for (vIdx in verticalMatches.indices) {
                val vMatch = verticalMatches[vIdx]
                val crossing = hMatch.intersect(vMatch.toSet())
                if (crossing.isEmpty()) continue
                val crossPoint = crossing.first()
                val type = matchKey(board[crossPoint.row][crossPoint.col]) ?: continue
                val combined = (hMatch + vMatch).toSet()
                val spawn = pickSpawn(combined, crossPoint)
                if (!newSpecials.containsKey(spawn)) {
                    newSpecials[spawn] = type to SpecialType.WRAPPED
                    groups.add(MatchGroup(combined, type, SpecialType.WRAPPED, spawn))
                }
                allMatched.addAll(combined)
                usedH.add(hIdx)
                usedV.add(vIdx)
            }
        }

        for (hIdx in horizontalMatches.indices) {
            if (usedH.contains(hIdx)) continue
            val match = horizontalMatches[hIdx]
            val type = matchKey(board[match.first().row][match.first().col]) ?: continue
            val spawn = pickSpawn(match, match[match.size / 2])
            when {
                match.size >= 5 -> {
                    newSpecials[spawn] = type to SpecialType.COLOR_BOMB
                    groups.add(MatchGroup(match.toSet(), type, SpecialType.COLOR_BOMB, spawn))
                }
                match.size == 4 -> {
                    // horizontal line of 4 -> vertical stripes (clears a column)
                    newSpecials[spawn] = type to SpecialType.VERTICAL_STRIPED
                    groups.add(MatchGroup(match.toSet(), type, SpecialType.VERTICAL_STRIPED, spawn))
                }
                else -> groups.add(MatchGroup(match.toSet(), type))
            }
            allMatched.addAll(match)
        }

        for (vIdx in verticalMatches.indices) {
            if (usedV.contains(vIdx)) continue
            val match = verticalMatches[vIdx]
            val type = matchKey(board[match.first().row][match.first().col]) ?: continue
            val spawn = pickSpawn(match, match[match.size / 2])
            when {
                match.size >= 5 -> {
                    newSpecials[spawn] = type to SpecialType.COLOR_BOMB
                    groups.add(MatchGroup(match.toSet(), type, SpecialType.COLOR_BOMB, spawn))
                }
                match.size == 4 -> {
                    // vertical line of 4 -> horizontal stripes (clears a row)
                    newSpecials[spawn] = type to SpecialType.HORIZONTAL_STRIPED
                    groups.add(MatchGroup(match.toSet(), type, SpecialType.HORIZONTAL_STRIPED, spawn))
                }
                else -> groups.add(MatchGroup(match.toSet(), type))
            }
            allMatched.addAll(match)
        }

        return MatchDetectionResult(
            matchedPositions = allMatched,
            matchGroups = groups,
            newSpecialsToSpawn = newSpecials
        )
    }

    // ------------------------------------------------------------------------------------------
    // Special candy detonation
    // ------------------------------------------------------------------------------------------

    fun mostCommonColor(board: List<List<Cell>>): CandyType? {
        val counts = mutableMapOf<CandyType, Int>()
        board.forEach { row ->
            row.forEach { cell ->
                val key = matchKey(cell)
                if (key != null) counts[key] = (counts[key] ?: 0) + 1
            }
        }
        return counts.maxByOrNull { it.value }?.key
    }

    /**
     * Expands a set of cleared cells by chain-reacting every special candy inside it.
     * Positions in [alreadyDetonated] are cleared but do NOT fire again (used for direct combos).
     * Ingredients, walls and empty cells are never included in the result.
     */
    fun expandSpecialDetonations(
        startingPositions: Set<Position>,
        board: List<List<Cell>>,
        alreadyDetonated: Set<Position> = emptySet()
    ): Set<Position> {
        val cleared = mutableSetOf<Position>()
        val visited = mutableSetOf<Position>()
        val queue = ArrayDeque<Position>()
        queue.addAll(startingPositions)

        val rows = board.size
        val cols = board[0].size

        while (queue.isNotEmpty()) {
            val pos = queue.removeFirst()
            if (!visited.add(pos)) continue
            if (!inBounds(board, pos.row, pos.col)) continue

            val cell = board[pos.row][pos.col]
            val candy = cell.candy
            if (!cell.isPlayable || candy == null || candy.ingredient != null) continue
            cleared.add(pos)

            if (alreadyDetonated.contains(pos)) continue

            when (candy.special) {
                SpecialType.HORIZONTAL_STRIPED -> {
                    for (c in 0 until cols) queue.add(Position(pos.row, c))
                }
                SpecialType.VERTICAL_STRIPED -> {
                    for (r in 0 until rows) queue.add(Position(r, pos.col))
                }
                SpecialType.WRAPPED -> {
                    for (dr in -1..1) for (dc in -1..1) queue.add(Position(pos.row + dr, pos.col + dc))
                }
                SpecialType.COLOR_BOMB -> {
                    val target = mostCommonColor(board)
                    if (target != null) {
                        for (r in 0 until rows) for (c in 0 until cols) {
                            if (matchKey(board[r][c]) == target) queue.add(Position(r, c))
                        }
                    }
                }
                SpecialType.NONE -> {}
            }
        }
        return cleared
    }

    private fun positionsOfColor(board: List<List<Cell>>, color: CandyType): List<Position> {
        val result = mutableListOf<Position>()
        for (r in board.indices) for (c in board[0].indices) {
            if (matchKey(board[r][c]) == color) result.add(Position(r, c))
        }
        return result
    }

    /**
     * Handles two special candies (or a colour bomb) swapped directly together.
     * Returns the set of cells to clear, or null if this swap isn't a special combo.
     */
    fun handleDirectSpecialCombo(
        pos1: Position,
        pos2: Position,
        board: List<List<Cell>>
    ): Set<Position>? {
        val candy1 = board[pos1.row][pos1.col].candy ?: return null
        val candy2 = board[pos2.row][pos2.col].candy ?: return null
        if (candy1.ingredient != null || candy2.ingredient != null) return null

        val rows = board.size
        val cols = board[0].size
        val bomb1 = candy1.special == SpecialType.COLOR_BOMB
        val bomb2 = candy2.special == SpecialType.COLOR_BOMB

        // 1. Colour bomb + colour bomb -> whole board
        if (bomb1 && bomb2) {
            val all = mutableSetOf<Position>()
            for (r in 0 until rows) for (c in 0 until cols) {
                val cell = board[r][c]
                val candy = cell.candy
                if (cell.isPlayable && candy != null && candy.ingredient == null) all.add(Position(r, c))
            }
            return all
        }

        // 2. Colour bomb + something
        if (bomb1 || bomb2) {
            val bombPos = if (bomb1) pos1 else pos2
            val other = if (bomb1) candy2 else candy1
            val targets = positionsOfColor(board, other.type)
            val cleared = mutableSetOf<Position>()
            cleared.add(bombPos)

            when {
                // 2a. bomb + striped -> every candy of that colour becomes striped and fires
                other.special.isStriped -> {
                    targets.forEachIndexed { index, p ->
                        cleared.add(p)
                        if (index % 2 == 0) {
                            for (c in 0 until cols) cleared.add(Position(p.row, c))
                        } else {
                            for (r in 0 until rows) cleared.add(Position(r, p.col))
                        }
                    }
                }
                // 2b. bomb + wrapped -> every candy of that colour explodes 3x3
                other.special == SpecialType.WRAPPED -> {
                    targets.forEach { p ->
                        for (dr in -1..1) for (dc in -1..1) cleared.add(Position(p.row + dr, p.col + dc))
                    }
                }
                // 2c. bomb + plain candy -> every candy of that colour
                else -> cleared.addAll(targets)
            }
            return expandSpecialDetonations(cleared, board, alreadyDetonated = setOf(bombPos))
        }

        val s1 = candy1.special
        val s2 = candy2.special

        // 3. Striped + striped -> big cross
        if (s1.isStriped && s2.isStriped) {
            val cleared = mutableSetOf<Position>()
            for (c in 0 until cols) cleared.add(Position(pos2.row, c))
            for (r in 0 until rows) cleared.add(Position(r, pos2.col))
            return expandSpecialDetonations(cleared, board, alreadyDetonated = setOf(pos1, pos2))
        }

        // 4. Striped + wrapped -> 3-wide row and column
        if ((s1.isStriped && s2 == SpecialType.WRAPPED) || (s2.isStriped && s1 == SpecialType.WRAPPED)) {
            val cleared = mutableSetOf<Position>()
            for (d in -1..1) {
                val r = pos2.row + d
                if (r in 0 until rows) for (c in 0 until cols) cleared.add(Position(r, c))
                val c2 = pos2.col + d
                if (c2 in 0 until cols) for (r2 in 0 until rows) cleared.add(Position(r2, c2))
            }
            return expandSpecialDetonations(cleared, board, alreadyDetonated = setOf(pos1, pos2))
        }

        // 5. Wrapped + wrapped -> 5x5 blast
        if (s1 == SpecialType.WRAPPED && s2 == SpecialType.WRAPPED) {
            val cleared = mutableSetOf<Position>()
            for (dr in -2..2) for (dc in -2..2) cleared.add(Position(pos2.row + dr, pos2.col + dc))
            return expandSpecialDetonations(cleared, board, alreadyDetonated = setOf(pos1, pos2))
        }

        return null
    }

    // ------------------------------------------------------------------------------------------
    // Clearing, blockers
    // ------------------------------------------------------------------------------------------

    /** Sets isClearing on cells about to be removed so the UI can play the pop animation. */
    fun markClearing(
        board: List<List<Cell>>,
        cleared: Set<Position>,
        keep: Set<Position> = emptySet()
    ): List<List<Cell>> {
        return board.mapIndexed { r, row ->
            row.mapIndexed { c, cell ->
                val pos = Position(r, c)
                val candy = cell.candy
                if (candy != null && cleared.contains(pos) && !keep.contains(pos)) {
                    cell.copy(candy = candy.copy(isClearing = true, isSelected = false, isHinted = false))
                } else {
                    cell
                }
            }
        }
    }

    /**
     * Removes candies, damages jelly, breaks locks, and destroys chocolate that touches a cleared cell.
     * A candy at a position in [newSpecials] is transformed into the special instead of being removed
     * (keeping its id so the UI doesn't treat it as a brand new falling tile).
     */
    fun clearPositions(
        board: List<List<Cell>>,
        cleared: Set<Position>,
        newSpecials: Map<Position, Pair<CandyType, SpecialType>>
    ): ClearResult {
        val rows = board.size
        val cols = board[0].size
        var locksBroken = 0

        val afterClear: List<List<Cell>> = List(rows) { r ->
            List(cols) { c ->
                val current = board[r][c]
                val pos = Position(r, c)
                if (!cleared.contains(pos) || !current.isPlayable) {
                    current
                } else {
                    val newJelly = (current.jellyLevel - 1).coerceAtLeast(0)
                    if (current.lockLevel > 0) locksBroken++
                    val special = newSpecials[pos]
                    val existing = current.candy
                    val newCandy = if (special != null && existing != null) {
                        existing.copy(
                            type = special.first,
                            special = special.second,
                            isClearing = false,
                            isSelected = false,
                            isHinted = false
                        )
                    } else {
                        null
                    }
                    current.copy(candy = newCandy, jellyLevel = newJelly, lockLevel = 0)
                }
            }
        }

        // Chocolate touching any cleared cell is eaten.
        val hitChocolate = mutableSetOf<Position>()
        cleared.forEach { p ->
            val neighbours = listOf(
                Position(p.row - 1, p.col), Position(p.row + 1, p.col),
                Position(p.row, p.col - 1), Position(p.row, p.col + 1)
            )
            neighbours.forEach { n ->
                if (inBounds(afterClear, n.row, n.col) && afterClear[n.row][n.col].isChocolate) hitChocolate.add(n)
            }
        }
        if (hitChocolate.isEmpty()) {
            return ClearResult(afterClear, 0, locksBroken)
        }
        val finalBoard = afterClear.mapIndexed { r, row ->
            row.mapIndexed { c, cell ->
                if (hitChocolate.contains(Position(r, c))) {
                    cell.copy(isChocolate = false, isPlayable = true, candy = null)
                } else {
                    cell
                }
            }
        }
        return ClearResult(finalBoard, hitChocolate.size, locksBroken)
    }

    /**
     * Chocolate spreads if none was eaten this turn: a random chocolate infects a neighbouring candy.
     * Returns null when nothing can spread.
     */
    fun spreadChocolate(board: List<List<Cell>>, rng: Random = Random.Default): List<List<Cell>>? {
        val targets = mutableListOf<Position>()
        for (r in board.indices) for (c in board[0].indices) {
            if (!board[r][c].isChocolate) continue
            val neighbours = listOf(Position(r - 1, c), Position(r + 1, c), Position(r, c - 1), Position(r, c + 1))
            neighbours.forEach { n ->
                if (inBounds(board, n.row, n.col)) {
                    val cell = board[n.row][n.col]
                    val candy = cell.candy
                    if (cell.isPlayable && candy != null && candy.ingredient == null && cell.lockLevel == 0) {
                        targets.add(n)
                    }
                }
            }
        }
        if (targets.isEmpty()) return null
        val chosen = targets.random(rng)
        return board.mapIndexed { r, row ->
            row.mapIndexed { c, cell ->
                if (r == chosen.row && c == chosen.col) {
                    cell.copy(candy = null, isPlayable = false, isChocolate = true)
                } else {
                    cell
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Gravity / refill / ingredients
    // ------------------------------------------------------------------------------------------

    /**
     * Candies fall inside their own column segment (walls, chocolate and locked candies split segments).
     * Empty slots are refilled at the top of every segment. At most one new ingredient is dropped per call
     * when [ingredientBudget] > 0, from the top row of one of [ingredientSpawnCols].
     */
    fun applyGravityAndRefill(
        board: List<List<Cell>>,
        availableColors: List<CandyType>,
        ingredientBudget: Int = 0,
        ingredientSpawnCols: List<Int> = emptyList(),
        rng: Random = Random.Default
    ): List<List<Cell>> {
        val rows = board.size
        val cols = board[0].size
        val out = board.map { row -> row.toMutableList() }.toMutableList()
        var budget = if (ingredientBudget > 0) 1 else 0
        val chosenSpawnCol = ingredientSpawnCols.shuffled(rng).firstOrNull()

        for (c in 0 until cols) {
            var r = rows - 1
            while (r >= 0) {
                if (!isFreeForGravity(out[r][c])) {
                    r--
                    continue
                }
                val bottom = r
                var top = r
                while (top - 1 >= 0 && isFreeForGravity(out[top - 1][c])) top--

                val tiles = (bottom downTo top).mapNotNull { out[it][c].candy }
                for (rr in top..bottom) out[rr][c] = out[rr][c].copy(candy = null)
                var write = bottom
                for (tile in tiles) {
                    out[write][c] = out[write][c].copy(candy = tile)
                    write--
                }
                // rows top..write are empty -> refill
                for (rr in write downTo top) {
                    val makeIngredient = budget > 0 && rr == 0 && chosenSpawnCol == c && rng.nextFloat() < 0.7f
                    val tile = if (makeIngredient) {
                        budget--
                        val kind = IngredientType.entries[rng.nextInt(IngredientType.entries.size)]
                        CandyTile(type = availableColors.first(), ingredient = kind)
                    } else {
                        CandyTile(type = availableColors.random(rng))
                    }
                    out[rr][c] = out[rr][c].copy(candy = tile)
                }
                r = top - 1
            }
        }
        return out.map { it.toList() }
    }

    fun countIngredients(board: List<List<Cell>>): Int {
        return board.sumOf { row -> row.count { it.candy?.ingredient != null } }
    }

    /** Removes ingredients standing on their exit cell. Returns the new board and how many left. */
    fun collectExitedIngredients(
        board: List<List<Cell>>,
        config: LevelConfig
    ): Pair<List<List<Cell>>, Int> {
        var count = 0
        val exitRows = IntArray(board[0].size) { config.exitRow(it) }
        val newBoard = board.mapIndexed { r, row ->
            row.mapIndexed { c, cell ->
                if (cell.candy?.ingredient != null && exitRows[c] == r) {
                    count++
                    cell.copy(candy = null)
                } else {
                    cell
                }
            }
        }
        return newBoard to count
    }

    // ------------------------------------------------------------------------------------------
    // Hints & shuffle
    // ------------------------------------------------------------------------------------------

    fun hasPossibleMoves(board: List<List<Cell>>, availableColors: List<CandyType>): Boolean {
        return findHintMove(board) != null
    }

    fun findHintMove(board: List<List<Cell>>): Pair<Position, Position>? {
        val rows = board.size
        val cols = board[0].size
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val current = Position(r, c)
                if (c < cols - 1) {
                    val right = Position(r, c + 1)
                    if (isValidSwap(current, right, board)) return current to right
                }
                if (r < rows - 1) {
                    val down = Position(r + 1, c)
                    if (isValidSwap(current, down, board)) return current to down
                }
            }
        }
        return null
    }

    /** Shuffles only free candies (ingredients and locked candies stay where they are). */
    fun reshuffleBoard(
        board: List<List<Cell>>,
        availableColors: List<CandyType>,
        rng: Random = Random.Default
    ): List<List<Cell>> {
        val rows = board.size
        val cols = board[0].size

        val slots = mutableListOf<Position>()
        val candies = mutableListOf<CandyTile>()
        for (r in 0 until rows) for (c in 0 until cols) {
            val cell = board[r][c]
            val candy = cell.candy
            if (cell.isPlayable && cell.lockLevel == 0 && candy != null && candy.ingredient == null) {
                slots.add(Position(r, c))
                candies.add(candy)
            }
        }
        if (slots.size < 2) return board

        fun build(order: List<CandyTile>): List<List<Cell>> {
            val map = HashMap<Position, CandyTile>()
            slots.forEachIndexed { i, p -> map[p] = order[i] }
            return List(rows) { r ->
                List(cols) { c ->
                    val tile = map[Position(r, c)]
                    if (tile != null) board[r][c].copy(candy = tile) else board[r][c]
                }
            }
        }

        var attempt = 0
        var result = build(candies.shuffled(rng))
        while (attempt < 40 && (hasAnyInitialMatch(result) || !hasPossibleMoves(result, availableColors))) {
            result = build(candies.shuffled(rng))
            attempt++
        }

        // Last resort: re-roll colours until a playable, match-free board appears.
        var rerolls = 0
        while (rerolls < 60 && (hasAnyInitialMatch(result) || !hasPossibleMoves(result, availableColors))) {
            val recoloured = candies.map { it.copy(type = availableColors.random(rng), special = SpecialType.NONE) }
            result = build(recoloured)
            rerolls++
        }
        return result
    }
}
