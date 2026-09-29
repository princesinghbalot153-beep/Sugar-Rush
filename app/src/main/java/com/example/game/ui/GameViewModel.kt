package com.example.game.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.GameRepository
import com.example.data.LevelProgressEntity
import com.example.data.UserStatsEntity
import com.example.game.audio.SoundEffects
import com.example.game.engine.MatchEngine
import com.example.game.levels.LevelsCatalog
import com.example.game.model.BoosterType
import com.example.game.model.CandyCollectGoal
import com.example.game.model.CandyTile
import com.example.game.model.CandyType
import com.example.game.model.Cell
import com.example.game.model.ComboBanner
import com.example.game.model.FloatingScore
import com.example.game.model.GameParticle
import com.example.game.model.LevelConfig
import com.example.game.model.LevelGoalType
import com.example.game.model.Position
import com.example.game.model.SpecialCollectGoal
import com.example.game.model.SpecialType
import com.example.ui.theme.CandyBlue
import com.example.ui.theme.CandyGreen
import com.example.ui.theme.CandyOrange
import com.example.ui.theme.CandyPink
import com.example.ui.theme.CandyPurple
import com.example.ui.theme.CandyRed
import com.example.ui.theme.CandyYellow
import com.example.ui.theme.SugarGoldStar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class GameStatus {
    PLAYING,
    SUGAR_CRUSH_BONUS,
    LEVEL_WON,
    LEVEL_FAILED,
    PAUSED
}

data class GameUiState(
    val currentLevel: LevelConfig = LevelsCatalog.levels.first(),
    val board: List<List<Cell>> = emptyList(),
    val score: Int = 0,
    val movesLeft: Int = 20,
    val selectedPosition: Position? = null,
    val isBusy: Boolean = false,
    val gameStatus: GameStatus = GameStatus.PLAYING,
    val activeBooster: BoosterType? = null,
    val starsEarned: Int = 0,
    val jellyRemaining: Int = 0,
    val candyGoals: List<CandyCollectGoal> = emptyList(),
    val specialGoals: List<SpecialCollectGoal> = emptyList(),
    val ingredientsDropped: Int = 0,
    val floatingScores: List<FloatingScore> = emptyList(),
    val particles: List<GameParticle> = emptyList(),
    val comboBanner: ComboBanner? = null,
    val freeSwitchFirstPos: Position? = null,
    val isReshuffling: Boolean = false,
    val sugarCrushMovesRemaining: Int = 0,
    val isBlitzMode: Boolean = false,
    val blitzTimeRemainingSec: Int = 60,
    val blitzMultiplier: Int = 1,
    val feverProgress: Float = 0f,
    val isFeverActive: Boolean = false,
    val screenShakeX: Float = 0f,
    val screenShakeY: Float = 0f,
    val laserBeams: List<com.example.game.model.LaserBeamEffect> = emptyList(),
    val shockwaves: List<com.example.game.model.ShockwaveRingEffect> = emptyList(),
    val elapsedPlayTimeSec: Int = 0
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRepository = GameRepository(
        AppDatabase.getDatabase(application).gameDao()
    )

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    val allLevels: StateFlow<List<LevelProgressEntity>> = repository.allLevels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userStats: StateFlow<UserStatsEntity?> = repository.userStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var hintJob: Job? = null
    private var blitzJob: Job? = null
    private var feverJob: Job? = null
    private var screenShakeJob: Job? = null
    private var playTimeJob: Job? = null

    /** True while the Sugar Crush finale runs: animations are sped up. */
    private var fastMode = false

    /** Whether the life for the current attempt has already been paid. */
    private var lifeSpent = false

    /** Chocolate eaten during the current player turn (if 0 at end of turn, chocolate spreads). */
    private var chocolateEatenThisTurn = 0

    init {
        viewModelScope.launch {
            repository.initializeDatabaseIfEmpty()
            repository.regenLives()
            while (true) {
                delay(30_000)
                repository.regenLives()
            }
        }
    }

    private fun pace(ms: Long): Long = if (fastMode) (ms / 2).coerceAtLeast(60L) else ms

    // ------------------------------------------------------------------------------------------
    // Level lifecycle
    // ------------------------------------------------------------------------------------------

    fun buyLives() {
        viewModelScope.launch { repository.buyLives(GameRepository.LIVES_REFILL_COST) }
    }

    fun hasLives(): Boolean = (userStats.value?.lives ?: 1) > 0

    private fun cancelJobs() {
        hintJob?.cancel()
        blitzJob?.cancel()
        feverJob?.cancel()
        screenShakeJob?.cancel()
        playTimeJob?.cancel()
    }

    private fun startPlayTimer() {
        playTimeJob?.cancel()
        playTimeJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_uiState.value.gameStatus == GameStatus.PLAYING) {
                    _uiState.update { it.copy(elapsedPlayTimeSec = it.elapsedPlayTimeSec + 1) }
                }
            }
        }
    }

    private fun countJellyLayers(board: List<List<Cell>>): Int = board.sumOf { row -> row.sumOf { it.jellyLevel } }

    fun startLevel(levelNumber: Int) {
        cancelJobs()
        fastMode = false
        lifeSpent = false
        chocolateEatenThisTurn = 0

        val config = LevelsCatalog.getLevel(levelNumber)
        val initialBoard = MatchEngine.generateInitialBoard(config)

        _uiState.value = GameUiState(
            currentLevel = config,
            board = initialBoard,
            score = 0,
            movesLeft = config.maxMoves,
            jellyRemaining = countJellyLayers(initialBoard),
            candyGoals = config.candyGoals,
            specialGoals = config.specialGoals,
            ingredientsDropped = 0
        )

        startPlayTimer()
        scheduleIdleHint()
    }

    fun startBlitzMode() {
        cancelJobs()
        fastMode = false
        lifeSpent = true
        chocolateEatenThisTurn = 0

        val blitzConfig = LevelConfig(
            levelNumber = 999,
            name = "Blitz Frenzy",
            storyDescription = "2-Minute lightning rush! Match rapidly to add bonus seconds and trigger Hyper Fever!",
            targetScore = 40000,
            star1Score = 15000,
            star2Score = 28000,
            star3Score = 40000,
            maxMoves = 999,
            goalType = LevelGoalType.SCORE,
            availableColors = listOf(CandyType.RED, CandyType.YELLOW, CandyType.GREEN, CandyType.BLUE, CandyType.PURPLE)
        )

        val initialBoard = MatchEngine.generateInitialBoard(blitzConfig)

        _uiState.value = GameUiState(
            currentLevel = blitzConfig,
            board = initialBoard,
            movesLeft = 999,
            isBlitzMode = true,
            blitzTimeRemainingSec = 120
        )

        startPlayTimer()

        blitzJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_uiState.value.gameStatus != GameStatus.PLAYING) break
                val currentSec = _uiState.value.blitzTimeRemainingSec
                if (currentSec <= 1) {
                    _uiState.update { it.copy(blitzTimeRemainingSec = 0) }
                    finalizeBlitzGameOver()
                    break
                } else {
                    if (currentSec <= 6) {
                        playSound { SoundEffects.playBlitzTick() }
                        triggerHaptic(HapticType.LIGHT)
                    }
                    _uiState.update { it.copy(blitzTimeRemainingSec = currentSec - 1) }
                }
            }
        }

        scheduleIdleHint()
    }

    private fun finalizeBlitzGameOver() {
        playTimeJob?.cancel()
        val finalScore = _uiState.value.score
        viewModelScope.launch {
            val isNewRecord = repository.saveBlitzScore(finalScore)
            if (isNewRecord) {
                showComboBanner("NEW RECORD!", "Blitz High Score: $finalScore", SugarGoldStar)
            }
            _uiState.update {
                it.copy(
                    gameStatus = GameStatus.LEVEL_WON,
                    isBusy = false,
                    starsEarned = if (finalScore >= 15000) 3 else if (finalScore >= 8000) 2 else 1
                )
            }
            triggerHaptic(HapticType.VICTORY)
            playSound { SoundEffects.playVictory() }
        }
    }

    /** Called when the player leaves a level mid-game or dismisses the fail screen: a life is lost. */
    fun abandonLevel() {
        val state = _uiState.value
        if (state.isBlitzMode) {
            cancelJobs()
            return
        }
        if (state.gameStatus == GameStatus.PLAYING || state.gameStatus == GameStatus.LEVEL_FAILED) {
            spendLifeOnce()
        }
        cancelJobs()
    }

    /** Called from the game-over dialog (retry / back to map). */
    fun confirmFail() {
        spendLifeOnce()
    }

    private fun spendLifeOnce() {
        if (lifeSpent) return
        lifeSpent = true
        viewModelScope.launch { repository.consumeLife() }
    }

    private fun triggerScreenShake() {
        screenShakeJob?.cancel()
        screenShakeJob = viewModelScope.launch {
            for (i in 0..4) {
                val magnitude = (4 - i) * 2.5f
                val dx = (Random.nextFloat() * 2f - 1f) * magnitude
                val dy = (Random.nextFloat() * 2f - 1f) * magnitude
                _uiState.update { it.copy(screenShakeX = dx, screenShakeY = dy) }
                delay(30)
            }
            _uiState.update { it.copy(screenShakeX = 0f, screenShakeY = 0f) }
        }
    }

    private fun triggerFeverOverdrive() {
        feverJob?.cancel()
        feverJob = viewModelScope.launch {
            _uiState.update { it.copy(isFeverActive = true, feverProgress = 1f) }
            playSound { SoundEffects.playFeverActive() }
            triggerHaptic(HapticType.VICTORY)
            triggerScreenShake()
            showComboBanner("🔥 HYPER FRENZY!", "2X MULTIPLIER ACTIVE!", SugarGoldStar)

            for (sec in 12 downTo 1) {
                delay(1000)
                _uiState.update { it.copy(feverProgress = sec / 12f) }
            }

            _uiState.update { it.copy(isFeverActive = false, feverProgress = 0f) }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Hints & selection
    // ------------------------------------------------------------------------------------------

    private fun scheduleIdleHint() {
        hintJob?.cancel()
        hintJob = viewModelScope.launch {
            delay(4000)
            if (_uiState.value.isBusy || _uiState.value.gameStatus != GameStatus.PLAYING) return@launch
            val hint = MatchEngine.findHintMove(_uiState.value.board) ?: return@launch

            _uiState.update { state ->
                val updatedBoard = state.board.map { row ->
                    row.map { cell ->
                        val candy = cell.candy
                        if (candy != null && (cell.position == hint.first || cell.position == hint.second)) {
                            cell.copy(candy = candy.copy(isHinted = true))
                        } else {
                            cell
                        }
                    }
                }
                state.copy(board = updatedBoard)
            }
        }
    }

    private fun clearHints() {
        hintJob?.cancel()
        _uiState.update { state ->
            val updatedBoard = state.board.map { row ->
                row.map { cell ->
                    val candy = cell.candy
                    if (candy != null && candy.isHinted) {
                        cell.copy(candy = candy.copy(isHinted = false))
                    } else {
                        cell
                    }
                }
            }
            state.copy(board = updatedBoard)
        }
    }

    private fun updateCellSelection(pos: Position, selected: Boolean) {
        _uiState.update { state ->
            val updatedBoard = state.board.map { row ->
                row.map { cell ->
                    val candy = cell.candy
                    if (cell.row == pos.row && cell.col == pos.col && candy != null) {
                        cell.copy(candy = candy.copy(isSelected = selected))
                    } else {
                        cell
                    }
                }
            }
            state.copy(board = updatedBoard)
        }
    }

    fun onCellClicked(pos: Position) {
        val state = _uiState.value
        if (state.isBusy || state.gameStatus != GameStatus.PLAYING) return
        val cell = state.board.getOrNull(pos.row)?.getOrNull(pos.col) ?: return
        if (!cell.isPlayable) return

        clearHints()

        when (state.activeBooster) {
            BoosterType.LOLLIPOP_HAMMER -> { applyLollipopHammer(pos); return }
            BoosterType.COLOR_BOMB -> { applyColorBombBooster(pos); return }
            BoosterType.FREE_SWITCH -> { handleFreeSwitch(pos); return }
            else -> {}
        }

        val currentSelected = state.selectedPosition
        if (currentSelected == null) {
            updateCellSelection(pos, true)
            _uiState.update { it.copy(selectedPosition = pos) }
            triggerHaptic(HapticType.LIGHT)
            playSound { SoundEffects.playSwap() }
        } else if (currentSelected == pos) {
            updateCellSelection(pos, false)
            _uiState.update { it.copy(selectedPosition = null) }
        } else if (currentSelected.isAdjacentTo(pos)) {
            updateCellSelection(currentSelected, false)
            _uiState.update { it.copy(selectedPosition = null) }
            performSwap(currentSelected, pos)
        } else {
            updateCellSelection(currentSelected, false)
            updateCellSelection(pos, true)
            _uiState.update { it.copy(selectedPosition = pos) }
            triggerHaptic(HapticType.LIGHT)
            playSound { SoundEffects.playSwap() }
        }
    }

    /** Swipe gesture: swap `from` with its neighbour `to` directly (like the real game). */
    fun onSwipe(from: Position, to: Position) {
        val state = _uiState.value
        if (state.isBusy || state.gameStatus != GameStatus.PLAYING) return
        if (state.activeBooster != null) {
            onCellClicked(from)
            return
        }
        if (!from.isAdjacentTo(to)) return
        val cellFrom = state.board.getOrNull(from.row)?.getOrNull(from.col) ?: return
        val cellTo = state.board.getOrNull(to.row)?.getOrNull(to.col) ?: return
        if (!cellFrom.isPlayable || !cellTo.isPlayable) return

        clearHints()
        state.selectedPosition?.let { updateCellSelection(it, false) }
        _uiState.update { it.copy(selectedPosition = null) }
        performSwap(from, to)
    }

    // ------------------------------------------------------------------------------------------
    // Turn resolution
    // ------------------------------------------------------------------------------------------

    private fun performSwap(from: Position, to: Position) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }

            val currentBoard = _uiState.value.board
            val candyFrom = currentBoard[from.row][from.col].candy
            val candyTo = currentBoard[to.row][to.col].candy
            if (candyFrom == null || candyTo == null) {
                _uiState.update { it.copy(isBusy = false) }
                return@launch
            }

            val specialComboCleared = MatchEngine.handleDirectSpecialCombo(from, to, currentBoard)
            val isValid = MatchEngine.isValidSwap(from, to, currentBoard)

            // Animate the swap
            val swappedBoard = MatchEngine.swapCells(from, to, currentBoard)
            _uiState.update { it.copy(board = swappedBoard) }
            triggerHaptic(HapticType.LIGHT)
            playSound { SoundEffects.playSwap() }
            delay(230)

            if (!isValid) {
                _uiState.update { it.copy(board = currentBoard, isBusy = false) }
                triggerHaptic(HapticType.ERROR)
                playSound { SoundEffects.playInvalid() }
                delay(200)
                scheduleIdleHint()
                return@launch
            }

            _uiState.update { it.copy(movesLeft = (it.movesLeft - 1).coerceAtLeast(0)) }
            chocolateEatenThisTurn = 0

            var board: List<List<Cell>> = swappedBoard
            if (specialComboCleared != null) {
                triggerHaptic(HapticType.HEAVY)
                playSound { SoundEffects.playExplosion() }
                showComboBanner("Divine!", "Special Combo!", CandyBlue)
                board = clearStep(board, specialComboCleared, emptyMap(), streak = 1, alreadyExpanded = true, isComboBlast = true)
                board = cascadeLoop(board, null, null, startStreak = 2)
            } else {
                board = cascadeLoop(board, to, from, startStreak = 1)
            }

            endOfTurn(board)
        }
    }

    /** Repeats detect -> clear -> fall until the board is stable. Returns the settled board. */
    private suspend fun cascadeLoop(
        initialBoard: List<List<Cell>>,
        swapA: Position?,
        swapB: Position?,
        startStreak: Int
    ): List<List<Cell>> {
        var board = initialBoard
        var streak = startStreak
        var first = true
        while (true) {
            val detection = MatchEngine.detectMatches(
                board,
                if (first) swapA else null,
                if (first) swapB else null
            )
            first = false
            if (detection.matchedPositions.isEmpty()) break
            board = clearStep(board, detection.matchedPositions, detection.newSpecialsToSpawn, streak, alreadyExpanded = false)
            streak++
        }
        return board
    }

    /**
     * One clear phase: score, effects, pop animation, remove, gravity (+ ingredient exits).
     * Returns the board after everything has fallen.
     */
    private suspend fun clearStep(
        boardIn: List<List<Cell>>,
        matchedOrCleared: Set<Position>,
        newSpecials: Map<Position, Pair<CandyType, SpecialType>>,
        streak: Int,
        alreadyExpanded: Boolean,
        isComboBlast: Boolean = false
    ): List<List<Cell>> {
        val level = _uiState.value.currentLevel
        val fullCleared = if (alreadyExpanded) matchedOrCleared else MatchEngine.expandSpecialDetonations(matchedOrCleared, boardIn)

        // ---- score
        val streakMultiplier = when (streak) { 1 -> 1; 2 -> 2; 3 -> 3; else -> 4 }
        val feverMultiplier = if (_uiState.value.isFeverActive) 2 else 1
        val blitzMult = if (_uiState.value.isBlitzMode) 2 else 1
        val perCandy = if (isComboBlast) 90 else 60
        val specialBonus = newSpecials.size * 300
        val gainedScore = (fullCleared.size * perCandy + specialBonus) * streakMultiplier * feverMultiplier * blitzMult

        val anchor = fullCleared.firstOrNull() ?: Position(level.rows / 2, level.cols / 2)
        addFloatingScore(gainedScore, anchor)
        spawnParticlesForPositions(fullCleared, boardIn)

        // ---- special effects (lasers / shockwaves) for special candies that go off
        fullCleared.forEach { pos ->
            val candy = boardIn[pos.row][pos.col].candy ?: return@forEach
            if (newSpecials.containsKey(pos)) return@forEach
            when (candy.special) {
                SpecialType.HORIZONTAL_STRIPED -> {
                    spawnLaserBeam(isHorizontal = true, index = pos.row, color = candy.type.mainColor)
                    playSound { SoundEffects.playQuantumLaser() }
                }
                SpecialType.VERTICAL_STRIPED -> {
                    spawnLaserBeam(isHorizontal = false, index = pos.col, color = candy.type.mainColor)
                    playSound { SoundEffects.playQuantumLaser() }
                }
                SpecialType.COLOR_BOMB, SpecialType.WRAPPED -> {
                    spawnShockwave((pos.col + 0.5f) / level.cols, (pos.row + 0.5f) / level.rows, SugarGoldStar)
                    playSound { SoundEffects.playCosmicShockwave() }
                    triggerScreenShake()
                }
                else -> {}
            }
        }

        if (_uiState.value.isBlitzMode) {
            _uiState.update { it.copy(blitzTimeRemainingSec = (it.blitzTimeRemainingSec + 3).coerceAtMost(180)) }
            addFloatingScoreText("+3s", anchor, CandyGreen)
        }

        // ---- fever gauge
        if (!_uiState.value.isFeverActive && !fastMode) {
            val newFever = (_uiState.value.feverProgress + (0.12f * streak)).coerceIn(0f, 1f)
            if (newFever >= 1.0f) triggerFeverOverdrive() else _uiState.update { it.copy(feverProgress = newFever) }
        }

        viewModelScope.launch {
            repository.recordMaxCombo(streak)
            if (newSpecials.values.any { it.second == SpecialType.COLOR_BOMB }) repository.recordColorBombDetonation()
        }

        if (streak >= 3) triggerScreenShake()
        playSound { SoundEffects.playPop(streak) }
        if (!isComboBlast) {
            when (streak) {
                1 -> triggerHaptic(HapticType.MEDIUM)
                2 -> { triggerHaptic(HapticType.HEAVY); showComboBanner("Sweet!", "2x Multiplier!", CandyYellow) }
                3 -> { triggerHaptic(HapticType.HEAVY); showComboBanner("Tasty!", "3x Cascade Rush!", CandyOrange) }
                4 -> { triggerHaptic(HapticType.HEAVY); showComboBanner("Delicious!", "4x Sugar Frenzy!", CandyPink) }
                else -> { triggerHaptic(HapticType.HEAVY); showComboBanner("Sugar Rush!", "Unstoppable Combo!", CandyPurple) }
            }
        }

        updateGoalsProgress(fullCleared - newSpecials.keys, newSpecials, boardIn)

        // ---- pop animation
        val popping = MatchEngine.markClearing(boardIn, fullCleared, keep = newSpecials.keys)
        _uiState.update { it.copy(board = popping) }
        delay(pace(200))

        // ---- actually remove
        val cleared = MatchEngine.clearPositions(popping, fullCleared, newSpecials)
        chocolateEatenThisTurn += cleared.chocolateDestroyed
        _uiState.update { state ->
            val newScore = state.score + gainedScore
            state.copy(
                board = cleared.board,
                score = newScore,
                starsEarned = calculateStars(newScore, state.currentLevel),
                jellyRemaining = countJellyLayers(cleared.board)
            )
        }
        delay(pace(120))

        // ---- gravity, ingredient exits
        val settled = settleGravity(cleared.board)
        delay(pace(320))
        return settled
    }

    private suspend fun settleGravity(boardIn: List<List<Cell>>): List<List<Cell>> {
        val level = _uiState.value.currentLevel
        var board = boardIn
        var guard = 0
        while (guard < 6) {
            guard++
            val onBoard = MatchEngine.countIngredients(board)
            val dropped = _uiState.value.ingredientsDropped
            val budget = (level.ingredientTarget - dropped - onBoard).coerceAtLeast(0)
            board = MatchEngine.applyGravityAndRefill(
                board,
                level.availableColors,
                ingredientBudget = if (level.goalTypes.contains(LevelGoalType.DROP_INGREDIENTS)) budget else 0,
                ingredientSpawnCols = level.ingredientSpawnCols
            )
            _uiState.update { it.copy(board = board) }

            val (afterExit, exited) = MatchEngine.collectExitedIngredients(board, level)
            if (exited == 0) break
            delay(pace(260))
            addFloatingScoreText("🍒 +$exited", Position(level.rows - 1, level.cols / 2), CandyGreen)
            playSound { SoundEffects.playChime() }
            triggerHaptic(HapticType.MEDIUM)
            board = afterExit
            _uiState.update { it.copy(board = board, ingredientsDropped = it.ingredientsDropped + exited, score = it.score + 500 * exited) }
            delay(pace(120))
        }
        return board
    }

    private fun updateGoalsProgress(
        collected: Set<Position>,
        newSpecials: Map<Position, Pair<CandyType, SpecialType>>,
        board: List<List<Cell>>
    ) {
        _uiState.update { state ->
            val colorCounts = mutableMapOf<CandyType, Int>()
            collected.forEach { pos ->
                val candy = board[pos.row][pos.col].candy
                if (candy != null && candy.ingredient == null) {
                    colorCounts[candy.type] = (colorCounts[candy.type] ?: 0) + 1
                }
            }
            val updatedCandyGoals = state.candyGoals.map { goal ->
                goal.copy(currentCount = goal.currentCount + (colorCounts[goal.type] ?: 0))
            }
            val updatedSpecialGoals = state.specialGoals.map { goal ->
                val created = newSpecials.values.count { made ->
                    made.second == goal.specialType || (goal.specialType.isStriped && made.second.isStriped)
                }
                goal.copy(currentCount = goal.currentCount + created)
            }
            state.copy(candyGoals = updatedCandyGoals, specialGoals = updatedSpecialGoals)
        }
    }

    private fun isLevelWon(state: GameUiState): Boolean {
        val level = state.currentLevel
        return level.goalTypes.all { goal ->
            when (goal) {
                LevelGoalType.SCORE -> state.score >= level.targetScore
                LevelGoalType.CLEAR_JELLY -> state.jellyRemaining == 0
                LevelGoalType.COLLECT_CANDIES -> state.candyGoals.all { it.isCompleted }
                LevelGoalType.CREATE_SPECIALS -> state.specialGoals.all { it.isCompleted }
                LevelGoalType.DROP_INGREDIENTS -> state.ingredientsDropped >= level.ingredientTarget
            }
        }
    }

    /** Runs after every player action: spread chocolate, check win / lose, reshuffle if stuck. */
    private suspend fun endOfTurn(boardIn: List<List<Cell>>) {
        var board = boardIn
        val level = _uiState.value.currentLevel

        // Chocolate spreads when the player didn't eat any this turn.
        if (!_uiState.value.isBlitzMode && chocolateEatenThisTurn == 0 &&
            board.any { row -> row.any { it.isChocolate } }
        ) {
            val spread = MatchEngine.spreadChocolate(board)
            if (spread != null) {
                board = spread
                _uiState.update { it.copy(board = board) }
                playSound { SoundEffects.playInvalid() }
                delay(250)
                board = settleGravity(board)
                board = cascadeLoop(board, null, null, startStreak = 2)
            }
        }
        chocolateEatenThisTurn = 0

        val state = _uiState.value

        if (state.isBlitzMode) {
            if (!MatchEngine.hasPossibleMoves(board, level.availableColors)) {
                reshuffle(board, level.availableColors, banner = false)
            } else {
                _uiState.update { it.copy(isBusy = false) }
                scheduleIdleHint()
            }
            return
        }

        if (isLevelWon(state)) {
            if (state.movesLeft > 0) triggerSugarCrushBonus(board) else finalizeLevelVictory(state.score)
            return
        }

        if (state.movesLeft <= 0) {
            playTimeJob?.cancel()
            _uiState.update { it.copy(gameStatus = GameStatus.LEVEL_FAILED, isBusy = false) }
            triggerHaptic(HapticType.ERROR)
            playSound { SoundEffects.playInvalid() }
            return
        }

        if (!MatchEngine.hasPossibleMoves(board, level.availableColors)) {
            reshuffle(board, level.availableColors, banner = true)
        } else {
            _uiState.update { it.copy(isBusy = false) }
            scheduleIdleHint()
        }
    }

    private suspend fun reshuffle(board: List<List<Cell>>, colors: List<CandyType>, banner: Boolean) {
        _uiState.update { it.copy(isReshuffling = true) }
        delay(750)
        val reshuffled = MatchEngine.reshuffleBoard(board, colors)
        _uiState.update { it.copy(board = reshuffled, isReshuffling = false, isBusy = false) }
        if (banner) showComboBanner("Reshuffle!", "No moves left - fresh board", CandyYellow)
        scheduleIdleHint()
    }

    /**
     * Sugar Crush: every leftover move becomes a striped candy that fires, with cascades.
     */
    private fun triggerSugarCrushBonus(startBoard: List<List<Cell>>) {
        viewModelScope.launch {
            fastMode = true
            _uiState.update {
                it.copy(gameStatus = GameStatus.SUGAR_CRUSH_BONUS, isBusy = true, sugarCrushMovesRemaining = it.movesLeft)
            }
            showComboBanner("Sugar Crush!", "Bonus Firework Rounds!", SugarGoldStar)
            triggerHaptic(HapticType.VICTORY)
            delay(900)

            var board = startBoard
            var movesRemaining = _uiState.value.movesLeft

            while (movesRemaining > 0) {
                movesRemaining--
                val candidates = mutableListOf<Position>()
                for (r in board.indices) for (c in board[0].indices) {
                    val cell = board[r][c]
                    val candy = cell.candy
                    if (cell.isPlayable && candy != null && candy.ingredient == null && cell.lockLevel == 0 &&
                        candy.special == SpecialType.NONE
                    ) candidates.add(Position(r, c))
                }
                if (candidates.isNotEmpty()) {
                    val target = candidates.random()
                    val horizontal = Random.nextBoolean()
                    board = board.mapIndexed { r, row ->
                        row.mapIndexed { c, cell ->
                            val candy = cell.candy
                            if (r == target.row && c == target.col && candy != null) {
                                cell.copy(candy = candy.copy(
                                    special = if (horizontal) SpecialType.HORIZONTAL_STRIPED else SpecialType.VERTICAL_STRIPED
                                ))
                            } else cell
                        }
                    }
                    _uiState.update { it.copy(board = board) }
                    delay(pace(260))
                    board = clearStep(board, setOf(target), emptyMap(), streak = 1, alreadyExpanded = false)
                    board = cascadeLoop(board, null, null, startStreak = 2)
                }
                _uiState.update { state ->
                    state.copy(
                        movesLeft = movesRemaining,
                        sugarCrushMovesRemaining = movesRemaining,
                        score = state.score + 1000,
                        starsEarned = calculateStars(state.score + 1000, state.currentLevel)
                    )
                }
                playSound { SoundEffects.playPop(Random.nextInt(2, 6)) }
                delay(pace(150))
            }

            fastMode = false
            finalizeLevelVictory(_uiState.value.score)
        }
    }

    private fun finalizeLevelVictory(finalScore: Int) {
        playTimeJob?.cancel()
        hintJob?.cancel()
        val level = _uiState.value.currentLevel
        val stars = calculateStars(finalScore, level).coerceAtLeast(1)

        _uiState.update {
            it.copy(gameStatus = GameStatus.LEVEL_WON, isBusy = false, starsEarned = stars, score = finalScore)
        }
        triggerHaptic(HapticType.VICTORY)
        playSound { SoundEffects.playVictory() }

        viewModelScope.launch {
            repository.saveLevelCompletion(level.levelNumber, finalScore, stars)
        }
    }

    private fun calculateStars(score: Int, config: LevelConfig): Int {
        return when {
            score >= config.star3Score -> 3
            score >= config.star2Score -> 2
            score >= config.star1Score -> 1
            else -> 0
        }
    }

    // ------------------------------------------------------------------------------------------
    // Boosters
    // ------------------------------------------------------------------------------------------

    fun activateBooster(booster: BoosterType) {
        val state = _uiState.value

        if (booster == BoosterType.EXTRA_MOVES) {
            val canUse = state.gameStatus == GameStatus.PLAYING && !state.isBusy ||
                state.gameStatus == GameStatus.LEVEL_FAILED
            if (!canUse || state.isBlitzMode) return
            viewModelScope.launch {
                val success = repository.useBooster(booster)
                if (success) {
                    val wasFailed = _uiState.value.gameStatus == GameStatus.LEVEL_FAILED
                    _uiState.update {
                        it.copy(movesLeft = it.movesLeft + 5, gameStatus = GameStatus.PLAYING, isBusy = false)
                    }
                    if (wasFailed) startPlayTimer()
                    showComboBanner("+5 Moves!", "Extra Turns Added!", CandyGreen)
                    triggerHaptic(HapticType.MEDIUM)
                    playSound { SoundEffects.playPop(3) }
                    scheduleIdleHint()
                }
            }
            return
        }

        if (state.isBusy || state.gameStatus != GameStatus.PLAYING) return
        clearHints()

        if (state.activeBooster == booster) {
            state.freeSwitchFirstPos?.let { updateCellSelection(it, false) }
            _uiState.update { it.copy(activeBooster = null, freeSwitchFirstPos = null) }
        } else {
            _uiState.update { it.copy(activeBooster = booster, freeSwitchFirstPos = null) }
            triggerHaptic(HapticType.LIGHT)
        }
    }

    private fun applyLollipopHammer(pos: Position) {
        val board0 = _uiState.value.board
        val cell = board0[pos.row][pos.col]
        if (cell.candy == null || cell.candy.ingredient != null) return

        viewModelScope.launch {
            val success = repository.useBooster(BoosterType.LOLLIPOP_HAMMER)
            if (!success) {
                _uiState.update { it.copy(activeBooster = null) }
                return@launch
            }

            _uiState.update { it.copy(activeBooster = null, isBusy = true) }
            triggerHaptic(HapticType.HEAVY)
            playSound { SoundEffects.playExplosion() }
            chocolateEatenThisTurn = 0

            var board = clearStep(board0, setOf(pos), emptyMap(), streak = 1, alreadyExpanded = false)
            board = cascadeLoop(board, null, null, startStreak = 2)
            // Boosters never spend a move, so only check win/fail state.
            chocolateEatenThisTurn = 1
            endOfTurn(board)
        }
    }

    private fun applyColorBombBooster(pos: Position) {
        val cell = _uiState.value.board[pos.row][pos.col]
        val candy = cell.candy ?: return
        if (candy.ingredient != null) return

        viewModelScope.launch {
            val success = repository.useBooster(BoosterType.COLOR_BOMB)
            if (!success) {
                _uiState.update { it.copy(activeBooster = null) }
                return@launch
            }

            _uiState.update { state ->
                val updatedBoard = state.board.map { row ->
                    row.map { c ->
                        val t = c.candy
                        if (c.row == pos.row && c.col == pos.col && t != null) {
                            c.copy(candy = t.copy(special = SpecialType.COLOR_BOMB))
                        } else c
                    }
                }
                state.copy(board = updatedBoard, activeBooster = null)
            }
            showComboBanner("Color Bomb!", "Rainbow Power!", CandyPurple)
            triggerHaptic(HapticType.MEDIUM)
            playSound { SoundEffects.playExplosion() }
            scheduleIdleHint()
        }
    }

    private fun handleFreeSwitch(pos: Position) {
        val firstPos = _uiState.value.freeSwitchFirstPos
        if (firstPos == null) {
            _uiState.update { it.copy(freeSwitchFirstPos = pos) }
            updateCellSelection(pos, true)
            triggerHaptic(HapticType.LIGHT)
            playSound { SoundEffects.playSwap() }
        } else if (firstPos == pos) {
            updateCellSelection(pos, false)
            _uiState.update { it.copy(freeSwitchFirstPos = null) }
        } else {
            viewModelScope.launch {
                val success = repository.useBooster(BoosterType.FREE_SWITCH)
                updateCellSelection(firstPos, false)
                _uiState.update { it.copy(freeSwitchFirstPos = null, activeBooster = null) }
                if (success) {
                    _uiState.update { it.copy(isBusy = true) }
                    val swapped = MatchEngine.swapCells(firstPos, pos, _uiState.value.board)
                    _uiState.update { it.copy(board = swapped) }
                    triggerHaptic(HapticType.HEAVY)
                    playSound { SoundEffects.playSwap() }
                    delay(230)
                    chocolateEatenThisTurn = 0
                    val combo = MatchEngine.handleDirectSpecialCombo(firstPos, pos, swapped)
                    var board: List<List<Cell>> = swapped
                    if (combo != null) {
                        board = clearStep(board, combo, emptyMap(), streak = 1, alreadyExpanded = true, isComboBlast = true)
                        board = cascadeLoop(board, null, null, startStreak = 2)
                    } else {
                        board = cascadeLoop(board, pos, firstPos, startStreak = 1)
                    }
                    chocolateEatenThisTurn = 1
                    endOfTurn(board)
                }
            }
        }
    }

    fun buyBooster(booster: BoosterType) {
        viewModelScope.launch {
            val ok = repository.buyBooster(booster)
            if (ok) {
                triggerHaptic(HapticType.MEDIUM)
                playSound { SoundEffects.playPop(4) }
            } else {
                triggerHaptic(HapticType.ERROR)
                playSound { SoundEffects.playInvalid() }
            }
        }
    }

    fun claimDailyReward() {
        viewModelScope.launch {
            repository.addCoins(150)
            repository.refillLives()
            triggerHaptic(HapticType.VICTORY)
            playSound { SoundEffects.playVictory() }
            showComboBanner("Sugar Gift!", "+150 Coins & Full Hearts!", SugarGoldStar)
        }
    }

    fun toggleHaptics() = viewModelScope.launch { repository.toggleHaptics() }
    fun toggleSound() = viewModelScope.launch { repository.toggleSound() }

    private fun playSound(action: () -> Unit) {
        if (userStats.value?.soundEnabled == true) {
            action()
        }
    }

    fun awardWheelPrize(prize: WheelPrize) {
        viewModelScope.launch {
            repository.awardWheelReward(
                coins = prize.coins,
                hammers = prize.hammers,
                switches = prize.switches,
                bombs = prize.bombs,
                fullLives = prize.fullLives
            )
            triggerHaptic(HapticType.VICTORY)
            playSound { SoundEffects.playVictory() }
            showComboBanner("Lucky Spin!", "Claimed: ${prize.name}", SugarGoldStar)
        }
    }

    fun claimAchievement(id: String, reward: Int) {
        viewModelScope.launch {
            repository.claimAchievement(id, reward)
            triggerHaptic(HapticType.VICTORY)
            playSound { SoundEffects.playChime() }
            showComboBanner("Trophy Claimed!", "+$reward Coins Added", SugarGoldStar)
        }
    }

    private fun addFloatingScoreText(text: String, pos: Position, color: androidx.compose.ui.graphics.Color) {
        val scoreItem = FloatingScore(text = text, color = color, row = pos.row, col = pos.col)
        _uiState.update { state ->
            state.copy(floatingScores = (state.floatingScores + scoreItem).takeLast(10))
        }
        viewModelScope.launch {
            delay(1100)
            _uiState.update { state ->
                state.copy(floatingScores = state.floatingScores.filter { it.id != scoreItem.id })
            }
        }
    }

    private fun addFloatingScore(points: Int, pos: Position) {
        val text = "+$points"
        val color = when {
            points >= 1000 -> SugarGoldStar
            points >= 500 -> CandyPink
            points >= 200 -> CandyYellow
            else -> CandyBlue
        }
        val scoreItem = FloatingScore(text = text, color = color, row = pos.row, col = pos.col)
        _uiState.update { state ->
            state.copy(floatingScores = (state.floatingScores + scoreItem).takeLast(10))
        }
        viewModelScope.launch {
            delay(1100)
            _uiState.update { state ->
                state.copy(floatingScores = state.floatingScores.filter { it.id != scoreItem.id })
            }
        }
    }

    private fun spawnParticlesForPositions(positions: Set<Position>, board: List<List<Cell>>) {
        val rows = board.size
        val cols = board[0].size
        val newParticles = mutableListOf<GameParticle>()

        positions.take(8).forEach { pos ->
            val candy = board.getOrNull(pos.row)?.getOrNull(pos.col)?.candy
            val baseColor = candy?.type?.mainColor ?: CandyPink
            val normX = (pos.col + 0.5f) / cols
            val normY = (pos.row + 0.5f) / rows

            for (i in 0 until 6) {
                val angle = Random.nextFloat() * 6.28f
                val speed = Random.nextFloat() * 0.09f + 0.03f
                newParticles.add(
                    GameParticle(
                        x = normX,
                        y = normY,
                        vx = kotlin.math.cos(angle) * speed,
                        vy = kotlin.math.sin(angle) * speed,
                        color = baseColor,
                        size = Random.nextFloat() * 14f + 6f
                    )
                )
            }
        }

        _uiState.update { state ->
            state.copy(particles = (state.particles + newParticles).takeLast(50))
        }

        viewModelScope.launch {
            delay(750)
            _uiState.update { state -> state.copy(particles = emptyList()) }
        }
    }

    private fun spawnLaserBeam(isHorizontal: Boolean, index: Int, color: androidx.compose.ui.graphics.Color) {
        val beam = com.example.game.model.LaserBeamEffect(
            isHorizontal = isHorizontal,
            index = index,
            color = color
        )
        _uiState.update { it.copy(laserBeams = (it.laserBeams + beam).takeLast(6)) }
        viewModelScope.launch {
            delay(280)
            _uiState.update { state -> state.copy(laserBeams = state.laserBeams.filter { it.id != beam.id }) }
        }
    }

    private fun spawnShockwave(centerX: Float, centerY: Float, color: androidx.compose.ui.graphics.Color) {
        val wave = com.example.game.model.ShockwaveRingEffect(
            centerX = centerX,
            centerY = centerY,
            color = color
        )
        _uiState.update { it.copy(shockwaves = (it.shockwaves + wave).takeLast(4)) }
        viewModelScope.launch {
            delay(350)
            _uiState.update { state -> state.copy(shockwaves = state.shockwaves.filter { it.id != wave.id }) }
        }
    }

    private fun showComboBanner(title: String, subtitle: String, color: androidx.compose.ui.graphics.Color) {
        val banner = ComboBanner(title, subtitle, color)
        _uiState.update { it.copy(comboBanner = banner) }
        viewModelScope.launch {
            delay(1200)
            _uiState.update { if (it.comboBanner?.timestamp == banner.timestamp) it.copy(comboBanner = null) else it }
        }
    }

    private enum class HapticType { LIGHT, MEDIUM, HEAVY, VICTORY, ERROR }

    private fun triggerHaptic(type: HapticType) {
        val stats = userStats.value ?: return
        if (!stats.hapticsEnabled) return
        val vib = vibrator ?: return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                when (type) {
                    HapticType.LIGHT -> vib.vibrate(VibrationEffect.createOneShot(20, 70))
                    HapticType.MEDIUM -> vib.vibrate(VibrationEffect.createOneShot(45, 150))
                    HapticType.HEAVY -> vib.vibrate(VibrationEffect.createOneShot(90, 255))
                    HapticType.VICTORY -> {
                        val timings = longArrayOf(0, 50, 50, 80, 70, 180)
                        val amplitudes = intArrayOf(0, 110, 0, 170, 0, 255)
                        vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    }
                    HapticType.ERROR -> {
                        val timings = longArrayOf(0, 35, 35, 35)
                        val amplitudes = intArrayOf(0, 170, 0, 170)
                        vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                when (type) {
                    HapticType.LIGHT -> vib.vibrate(20)
                    HapticType.MEDIUM -> vib.vibrate(45)
                    HapticType.HEAVY, HapticType.VICTORY -> vib.vibrate(90)
                    HapticType.ERROR -> vib.vibrate(50)
                }
            }
        } catch (_: Exception) {}
    }
}
