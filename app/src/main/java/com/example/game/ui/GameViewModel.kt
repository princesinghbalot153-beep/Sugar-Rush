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
import com.example.game.engine.MatchDetectionResult
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

    init {
        viewModelScope.launch {
            repository.initializeDatabaseIfEmpty()
        }
    }

    fun startLevel(levelNumber: Int) {
        hintJob?.cancel()
        blitzJob?.cancel()
        feverJob?.cancel()
        screenShakeJob?.cancel()
        playTimeJob?.cancel()

        val config = LevelsCatalog.getLevel(levelNumber)
        val initialBoard = MatchEngine.generateInitialBoard(config)
        val initialJellyCount = initialBoard.flatten().count { it.hasJelly }

        _uiState.value = GameUiState(
            currentLevel = config,
            board = initialBoard,
            score = 0,
            movesLeft = config.maxMoves,
            selectedPosition = null,
            isBusy = false,
            gameStatus = GameStatus.PLAYING,
            activeBooster = null,
            starsEarned = 0,
            jellyRemaining = initialJellyCount,
            candyGoals = config.candyGoals,
            specialGoals = config.specialGoals,
            floatingScores = emptyList(),
            particles = emptyList(),
            comboBanner = null,
            sugarCrushMovesRemaining = 0,
            isBlitzMode = false,
            blitzTimeRemainingSec = 120,
            blitzMultiplier = 1,
            feverProgress = 0f,
            isFeverActive = false,
            elapsedPlayTimeSec = 0
        )

        playTimeJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_uiState.value.gameStatus == GameStatus.PLAYING) {
                    _uiState.update { it.copy(elapsedPlayTimeSec = it.elapsedPlayTimeSec + 1) }
                }
            }
        }

        scheduleIdleHint()
    }

    fun startBlitzMode() {
        hintJob?.cancel()
        blitzJob?.cancel()
        feverJob?.cancel()
        screenShakeJob?.cancel()
        playTimeJob?.cancel()

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
            score = 0,
            movesLeft = 999,
            selectedPosition = null,
            isBusy = false,
            gameStatus = GameStatus.PLAYING,
            activeBooster = null,
            starsEarned = 0,
            jellyRemaining = 0,
            candyGoals = emptyList(),
            specialGoals = emptyList(),
            floatingScores = emptyList(),
            particles = emptyList(),
            comboBanner = null,
            sugarCrushMovesRemaining = 0,
            isBlitzMode = true,
            blitzTimeRemainingSec = 120,
            blitzMultiplier = 1,
            feverProgress = 0f,
            isFeverActive = false,
            elapsedPlayTimeSec = 0
        )

        playTimeJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_uiState.value.gameStatus == GameStatus.PLAYING) {
                    _uiState.update { it.copy(elapsedPlayTimeSec = it.elapsedPlayTimeSec + 1) }
                }
            }
        }

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

    private fun scheduleIdleHint() {
        hintJob?.cancel()
        hintJob = viewModelScope.launch {
            delay(4000)
            if (_uiState.value.isBusy || _uiState.value.gameStatus != GameStatus.PLAYING) return@launch
            val hint = MatchEngine.findHintMove(_uiState.value.board) ?: return@launch

            _uiState.update { state ->
                val updatedBoard = state.board.map { row ->
                    row.map { cell ->
                        if (cell.position == hint.first || cell.position == hint.second) {
                            cell.copy(candy = cell.candy?.copy(isHinted = true))
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
                    if (cell.candy?.isHinted == true) {
                        cell.copy(candy = cell.candy.copy(isHinted = false))
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

        // Active booster handling
        when (state.activeBooster) {
            BoosterType.LOLLIPOP_HAMMER -> {
                applyLollipopHammer(pos)
                return
            }
            BoosterType.COLOR_BOMB -> {
                applyColorBombBooster(pos)
                return
            }
            BoosterType.FREE_SWITCH -> {
                handleFreeSwitch(pos)
                return
            }
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

    private fun updateCellSelection(pos: Position, selected: Boolean) {
        _uiState.update { state ->
            val updatedBoard = state.board.map { row ->
                row.map { cell ->
                    if (cell.row == pos.row && cell.col == pos.col && cell.candy != null) {
                        cell.copy(candy = cell.candy.copy(isSelected = selected))
                    } else {
                        cell
                    }
                }
            }
            state.copy(board = updatedBoard)
        }
    }

    private fun performSwap(from: Position, to: Position) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }

            val currentBoard = _uiState.value.board
            val candyFrom = currentBoard[from.row][from.col].candy ?: return@launch
            val candyTo = currentBoard[to.row][to.col].candy ?: return@launch

            val specialComboCleared = MatchEngine.handleDirectSpecialCombo(from, to, currentBoard)
            val isValid = specialComboCleared != null || MatchEngine.isValidSwap(from, to, currentBoard)

            // Step 1: Animate Swap
            val swappedBoard = MatchEngine.swapCells(from, to, currentBoard)
            _uiState.update { it.copy(board = swappedBoard) }
            triggerHaptic(HapticType.LIGHT)
            playSound { SoundEffects.playSwap() }
            delay(220)

            if (!isValid) {
                // Return swap back
                _uiState.update { it.copy(board = currentBoard, isBusy = false) }
                triggerHaptic(HapticType.ERROR)
                playSound { SoundEffects.playInvalid() }
                scheduleIdleHint()
                return@launch
            }

            // Valid move: decrement moves
            _uiState.update { it.copy(movesLeft = (it.movesLeft - 1).coerceAtLeast(0)) }

            // Step 2: Resolve cascades or direct combo
            if (specialComboCleared != null) {
                triggerHaptic(HapticType.HEAVY)
                playSound { SoundEffects.playExplosion() }
                resolveClearedPositions(specialComboCleared, swappedBoard, cascadeStreak = 1)
            } else {
                resolveMatchesWithCascades(swappedBoard, lastSwapped = to, cascadeStreak = 1)
            }
        }
    }

    private suspend fun resolveMatchesWithCascades(
        initialBoard: List<List<Cell>>,
        lastSwapped: Position? = null,
        cascadeStreak: Int = 1
    ) {
        var board = initialBoard
        var streak = cascadeStreak

        while (true) {
            val detection = MatchEngine.detectMatches(board, lastSwapped)
            if (detection.matchedPositions.isEmpty()) {
                break
            }

            val fullCleared = MatchEngine.expandSpecialDetonations(detection.matchedPositions, board)
            val baseScore = fullCleared.size * 60
            val streakMultiplier = when (streak) {
                1 -> 1
                2 -> 2
                3 -> 3
                else -> 4
            }
            val feverMultiplier = if (_uiState.value.isFeverActive) 2 else 1
            val blitzMult = if (_uiState.value.isBlitzMode) 2 else 1
            val gainedScore = baseScore * streakMultiplier * feverMultiplier * blitzMult

            val firstCleared = fullCleared.firstOrNull() ?: Position(3, 3)
            addFloatingScore(gainedScore, firstCleared)
            spawnParticlesForPositions(fullCleared, board)

            // Trigger Futuristic Quantum Lasers & Cosmic Shockwaves
            detection.matchedPositions.forEach { pos ->
                val candy = board.getOrNull(pos.row)?.getOrNull(pos.col)?.candy
                if (candy != null) {
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
                            val normX = (pos.col + 0.5f) / board[0].size
                            val normY = (pos.row + 0.5f) / board.size
                            spawnShockwave(normX, normY, SugarGoldStar)
                            playSound { SoundEffects.playCosmicShockwave() }
                            triggerScreenShake()
                        }
                        else -> {}
                    }
                }
            }

            // Blitz Mode bonus seconds
            if (_uiState.value.isBlitzMode) {
                _uiState.update { it.copy(blitzTimeRemainingSec = (it.blitzTimeRemainingSec + 3).coerceAtMost(180)) }
                addFloatingScoreText("+3s Time!", firstCleared, CandyGreen)
            }

            // Fever Gauge Advancement
            if (!_uiState.value.isFeverActive) {
                val newFever = (_uiState.value.feverProgress + (0.12f * streak)).coerceIn(0f, 1f)
                if (newFever >= 1.0f) {
                    triggerFeverOverdrive()
                } else {
                    _uiState.update { it.copy(feverProgress = newFever) }
                }
            }

            // Track stats
            viewModelScope.launch {
                repository.recordMaxCombo(streak)
                if (detection.newSpecialsToSpawn.values.any { it.second == SpecialType.COLOR_BOMB }) {
                    repository.recordColorBombDetonation()
                }
            }

            // Screen shake on heavy combos
            if (streak >= 3) {
                triggerScreenShake()
            }

            // Dynamic Audio & Juice based on combo length
            playSound { SoundEffects.playPop(streak) }

            when (streak) {
                1 -> triggerHaptic(HapticType.MEDIUM)
                2 -> {
                    triggerHaptic(HapticType.HEAVY)
                    showComboBanner("Sweet!", "2x Multiplier!", CandyYellow)
                }
                3 -> {
                    triggerHaptic(HapticType.HEAVY)
                    showComboBanner("Tasty!", "3x Cascade Rush!", CandyOrange)
                }
                4 -> {
                    triggerHaptic(HapticType.HEAVY)
                    showComboBanner("Delicious!", "4x Sugar Frenzy!", CandyPink)
                }
                else -> {
                    triggerHaptic(HapticType.HEAVY)
                    showComboBanner("Sugar Rush!", "Unstoppable Combo!", CandyPurple)
                }
            }

            updateGoalsProgress(fullCleared, detection, board)

            val boardAfterDamage = damageJelliesAndClearCandies(board, fullCleared, detection.newSpecialsToSpawn)
            _uiState.update { state ->
                val newScore = state.score + gainedScore
                val stars = calculateStars(newScore, state.currentLevel)
                val remainingJellies = boardAfterDamage.flatten().count { it.hasJelly }
                state.copy(
                    board = boardAfterDamage,
                    score = newScore,
                    starsEarned = stars,
                    jellyRemaining = remainingJellies
                )
            }
            delay(240)

            val refilledBoard = MatchEngine.applyGravityAndRefill(
                boardAfterDamage,
                _uiState.value.currentLevel.availableColors
            )
            _uiState.update { it.copy(board = refilledBoard) }
            board = refilledBoard
            streak++
            delay(260)
        }

        checkTurnOutcome(board)
    }

    private suspend fun resolveClearedPositions(
        clearedPositions: Set<Position>,
        currentBoard: List<List<Cell>>,
        cascadeStreak: Int
    ) {
        val fullCleared = MatchEngine.expandSpecialDetonations(clearedPositions, currentBoard)
        val gainedScore = fullCleared.size * 120 * cascadeStreak

        val firstCleared = fullCleared.firstOrNull() ?: Position(3, 3)
        addFloatingScore(gainedScore, firstCleared)
        spawnParticlesForPositions(fullCleared, currentBoard)
        showComboBanner("Divine!", "Quantum Blast!", CandyBlue)

        val normX = (firstCleared.col + 0.5f) / currentBoard[0].size
        val normY = (firstCleared.row + 0.5f) / currentBoard.size
        spawnShockwave(normX, normY, CandyBlue)
        triggerScreenShake()
        playSound { SoundEffects.playQuantumLaser() }

        val boardAfterDamage = damageJelliesAndClearCandies(currentBoard, fullCleared, emptyMap())
        _uiState.update { state ->
            val newScore = state.score + gainedScore
            val stars = calculateStars(newScore, state.currentLevel)
            val remainingJellies = boardAfterDamage.flatten().count { it.hasJelly }
            state.copy(
                board = boardAfterDamage,
                score = newScore,
                starsEarned = stars,
                jellyRemaining = remainingJellies
            )
        }
        delay(260)

        val refilledBoard = MatchEngine.applyGravityAndRefill(
            boardAfterDamage,
            _uiState.value.currentLevel.availableColors
        )
        _uiState.update { it.copy(board = refilledBoard) }
        delay(240)

        resolveMatchesWithCascades(refilledBoard, cascadeStreak = cascadeStreak + 1)
    }

    private fun damageJelliesAndClearCandies(
        board: List<List<Cell>>,
        clearedPositions: Set<Position>,
        newSpecials: Map<Position, Pair<CandyType, SpecialType>>
    ): List<List<Cell>> {
        val rows = board.size
        val cols = board[0].size

        return List(rows) { r ->
            List(cols) { c ->
                val current = board[r][c]
                val pos = Position(r, c)
                if (clearedPositions.contains(pos)) {
                    val newJelly = (current.jellyLevel - 1).coerceAtLeast(0)
                    val specialToSpawn = newSpecials[pos]
                    val newCandy = if (specialToSpawn != null) {
                        CandyTile(type = specialToSpawn.first, special = specialToSpawn.second)
                    } else {
                        null
                    }
                    current.copy(candy = newCandy, jellyLevel = newJelly)
                } else {
                    current
                }
            }
        }
    }

    private fun updateGoalsProgress(
        clearedPositions: Set<Position>,
        detection: MatchDetectionResult,
        board: List<List<Cell>>
    ) {
        _uiState.update { state ->
            val clearedColorCounts = mutableMapOf<CandyType, Int>()
            clearedPositions.forEach { pos ->
                val candy = board[pos.row][pos.col].candy
                if (candy != null) {
                    clearedColorCounts[candy.type] = (clearedColorCounts[candy.type] ?: 0) + 1
                }
            }

            val updatedCandyGoals = state.candyGoals.map { goal ->
                val collected = clearedColorCounts[goal.type] ?: 0
                goal.copy(currentCount = goal.currentCount + collected)
            }

            val updatedSpecialGoals = state.specialGoals.map { goal ->
                val createdCount = detection.newSpecialsToSpawn.values.count { it.second == goal.specialType }
                goal.copy(currentCount = goal.currentCount + createdCount)
            }

            state.copy(candyGoals = updatedCandyGoals, specialGoals = updatedSpecialGoals)
        }
    }

    private fun checkTurnOutcome(finalBoard: List<List<Cell>>) {
        val state = _uiState.value
        val level = state.currentLevel

        if (state.isBlitzMode) {
            val hasMoves = MatchEngine.hasPossibleMoves(finalBoard, level.availableColors)
            if (!hasMoves) {
                viewModelScope.launch {
                    _uiState.update { it.copy(isReshuffling = true) }
                    delay(500)
                    val reshuffled = MatchEngine.reshuffleBoard(finalBoard, level.availableColors)
                    _uiState.update { it.copy(board = reshuffled, isReshuffling = false, isBusy = false) }
                    scheduleIdleHint()
                }
            } else {
                _uiState.update { it.copy(isBusy = false) }
                scheduleIdleHint()
            }
            return
        }

        val isWon = when (level.goalType) {
            LevelGoalType.SCORE -> state.score >= level.targetScore
            LevelGoalType.CLEAR_JELLY -> state.jellyRemaining == 0
            LevelGoalType.COLLECT_CANDIES -> state.candyGoals.all { it.isCompleted }
            LevelGoalType.CREATE_SPECIALS -> state.specialGoals.all { it.isCompleted }
        }

        if (isWon) {
            // Initiate Sugar Crush victory finale if remaining moves exist!
            if (state.movesLeft > 0) {
                triggerSugarCrushBonus(finalBoard)
            } else {
                finalizeLevelVictory(state.score)
            }
            return
        }

        if (state.movesLeft <= 0) {
            playTimeJob?.cancel()
            _uiState.update { it.copy(gameStatus = GameStatus.LEVEL_FAILED, isBusy = false) }
            triggerHaptic(HapticType.ERROR)
            playSound { SoundEffects.playInvalid() }
            viewModelScope.launch {
                repository.consumeLife()
            }
            return
        }

        val hasMoves = MatchEngine.hasPossibleMoves(finalBoard, level.availableColors)
        if (!hasMoves) {
            viewModelScope.launch {
                _uiState.update { it.copy(isReshuffling = true) }
                delay(750)
                val reshuffled = MatchEngine.reshuffleBoard(finalBoard, level.availableColors)
                _uiState.update { it.copy(board = reshuffled, isReshuffling = false, isBusy = false) }
                showComboBanner("Reshuffle!", "Fresh Board Mixed In", CandyYellow)
                scheduleIdleHint()
            }
        } else {
            _uiState.update { it.copy(isBusy = false) }
            scheduleIdleHint()
        }
    }

    /**
     * Sugar Crush Victory Finale: detonates remaining moves into explosive bonus points!
     */
    private fun triggerSugarCrushBonus(board: List<List<Cell>>) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    gameStatus = GameStatus.SUGAR_CRUSH_BONUS,
                    isBusy = true,
                    sugarCrushMovesRemaining = it.movesLeft
                )
            }
            showComboBanner("Sugar Crush!", "Bonus Firework Rounds!", SugarGoldStar)
            triggerHaptic(HapticType.VICTORY)
            delay(1000)

            var movesRemaining = _uiState.value.movesLeft
            var currentBoard = board
            var totalScore = _uiState.value.score

            while (movesRemaining > 0) {
                movesRemaining--
                val randomRow = Random.nextInt(0, 8)
                val randomCol = Random.nextInt(0, 8)
                val bonusPts = 500
                totalScore += bonusPts

                val targetPos = Position(randomRow, randomCol)
                addFloatingScore(bonusPts, targetPos)
                spawnParticlesForPositions(setOf(targetPos), currentBoard)
                playSound { SoundEffects.playPop(Random.nextInt(2, 6)) }
                triggerHaptic(HapticType.MEDIUM)

                val damagedBoard = damageJelliesAndClearCandies(currentBoard, setOf(targetPos), emptyMap())
                val refilledBoard = MatchEngine.applyGravityAndRefill(
                    damagedBoard,
                    _uiState.value.currentLevel.availableColors
                )
                currentBoard = refilledBoard

                _uiState.update { state ->
                    state.copy(
                        board = currentBoard,
                        score = totalScore,
                        movesLeft = movesRemaining,
                        sugarCrushMovesRemaining = movesRemaining,
                        starsEarned = calculateStars(totalScore, state.currentLevel).coerceAtLeast(1)
                    )
                }
                delay(220)
            }

            finalizeLevelVictory(totalScore)
        }
    }

    private fun finalizeLevelVictory(finalScore: Int) {
        playTimeJob?.cancel()
        val level = _uiState.value.currentLevel
        val stars = calculateStars(finalScore, level).coerceAtLeast(1)

        _uiState.update {
            it.copy(
                gameStatus = GameStatus.LEVEL_WON,
                isBusy = false,
                starsEarned = stars,
                score = finalScore
            )
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

    // Boosters
    fun activateBooster(booster: BoosterType) {
        val state = _uiState.value
        if (state.isBusy || state.gameStatus != GameStatus.PLAYING) return
        clearHints()

        if (booster == BoosterType.EXTRA_MOVES) {
            viewModelScope.launch {
                val success = repository.useBooster(booster)
                if (success) {
                    _uiState.update { it.copy(movesLeft = it.movesLeft + 5) }
                    showComboBanner("+5 Moves!", "Extra Turns Added!", CandyGreen)
                    triggerHaptic(HapticType.MEDIUM)
                    playSound { SoundEffects.playPop(3) }
                }
            }
            return
        }

        if (state.activeBooster == booster) {
            _uiState.update { it.copy(activeBooster = null) }
        } else {
            _uiState.update { it.copy(activeBooster = booster) }
            triggerHaptic(HapticType.LIGHT)
        }
    }

    private fun applyLollipopHammer(pos: Position) {
        viewModelScope.launch {
            val success = repository.useBooster(BoosterType.LOLLIPOP_HAMMER)
            if (!success) {
                _uiState.update { it.copy(activeBooster = null) }
                return@launch
            }

            _uiState.update { it.copy(activeBooster = null, isBusy = true) }
            triggerHaptic(HapticType.HEAVY)
            playSound { SoundEffects.playExplosion() }

            val board = _uiState.value.board
            val damagedBoard = damageJelliesAndClearCandies(board, setOf(pos), emptyMap())
            addFloatingScore(500, pos)
            spawnParticlesForPositions(setOf(pos), board)

            _uiState.update { state ->
                val newScore = state.score + 500
                val remainingJellies = damagedBoard.flatten().count { it.hasJelly }
                state.copy(
                    board = damagedBoard,
                    score = newScore,
                    jellyRemaining = remainingJellies
                )
            }
            delay(220)

            val refilled = MatchEngine.applyGravityAndRefill(damagedBoard, _uiState.value.currentLevel.availableColors)
            _uiState.update { it.copy(board = refilled) }
            delay(200)

            resolveMatchesWithCascades(refilled, cascadeStreak = 1)
        }
    }

    private fun applyColorBombBooster(pos: Position) {
        viewModelScope.launch {
            val success = repository.useBooster(BoosterType.COLOR_BOMB)
            if (!success) {
                _uiState.update { it.copy(activeBooster = null) }
                return@launch
            }

            _uiState.update { state ->
                val updatedBoard = state.board.map { row ->
                    row.map { cell ->
                        if (cell.row == pos.row && cell.col == pos.col) {
                            cell.copy(candy = CandyTile(type = CandyType.RED, special = SpecialType.COLOR_BOMB))
                        } else {
                            cell
                        }
                    }
                }
                state.copy(board = updatedBoard, activeBooster = null)
            }
            showComboBanner("Color Bomb!", "Rainbow Power!", CandyPurple)
            triggerHaptic(HapticType.MEDIUM)
            playSound { SoundEffects.playExplosion() }
        }
    }

    private fun handleFreeSwitch(pos: Position) {
        val firstPos = _uiState.value.freeSwitchFirstPos
        if (firstPos == null) {
            _uiState.update { it.copy(freeSwitchFirstPos = pos) }
            updateCellSelection(pos, true)
            triggerHaptic(HapticType.LIGHT)
            playSound { SoundEffects.playSwap() }
        } else {
            viewModelScope.launch {
                val success = repository.useBooster(BoosterType.FREE_SWITCH)
                updateCellSelection(firstPos, false)
                _uiState.update { it.copy(freeSwitchFirstPos = null, activeBooster = null) }
                if (success) {
                    val currentBoard = _uiState.value.board
                    val swapped = MatchEngine.swapCells(firstPos, pos, currentBoard)
                    _uiState.update { it.copy(board = swapped, isBusy = true) }
                    triggerHaptic(HapticType.HEAVY)
                    playSound { SoundEffects.playSwap() }
                    delay(220)
                    resolveMatchesWithCascades(swapped, cascadeStreak = 1)
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
