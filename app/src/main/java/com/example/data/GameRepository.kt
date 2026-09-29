package com.example.data

import com.example.game.levels.LevelsCatalog
import com.example.game.model.BoosterType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class GameRepository(private val gameDao: GameDao) {

    val allLevels: Flow<List<LevelProgressEntity>> = gameDao.getAllLevelProgress()
    val userStats: Flow<UserStatsEntity?> = gameDao.getUserStatsFlow()

    suspend fun initializeDatabaseIfEmpty() = withContext(Dispatchers.IO) {
        val stats = gameDao.getUserStats()
        if (stats == null) {
            gameDao.insertOrUpdateUserStats(UserStatsEntity())
        }

        val initialLevels = LevelsCatalog.levels.map { config ->
            LevelProgressEntity(
                levelNumber = config.levelNumber,
                stars = 0,
                highScore = 0,
                isUnlocked = config.levelNumber == 1, // First level is unlocked by default
                isCompleted = false
            )
        }
        gameDao.insertInitialLevels(initialLevels)
    }

    suspend fun saveLevelCompletion(
        levelNumber: Int,
        score: Int,
        starsEarned: Int
    ) = withContext(Dispatchers.IO) {
        val currentProgress = gameDao.getLevelProgress(levelNumber)
        val oldHighScore = currentProgress?.highScore ?: 0
        val oldStars = currentProgress?.stars ?: 0

        val newHighScore = maxOf(oldHighScore, score)
        val newStars = maxOf(oldStars, starsEarned)

        gameDao.insertOrUpdateLevelProgress(
            LevelProgressEntity(
                levelNumber = levelNumber,
                stars = newStars,
                highScore = newHighScore,
                isUnlocked = true,
                isCompleted = true,
                completedAt = System.currentTimeMillis()
            )
        )

        // Unlock the next level if exists
        val nextLevelNumber = levelNumber + 1
        val nextLevel = gameDao.getLevelProgress(nextLevelNumber)
        if (nextLevel != null && !nextLevel.isUnlocked) {
            gameDao.insertOrUpdateLevelProgress(
                nextLevel.copy(isUnlocked = true)
            )
        }

        // Award reward coins: 50 base + 25 per star
        val rewardCoins = 50 + (starsEarned * 25)
        addCoins(rewardCoins)
    }

    suspend fun addCoins(amount: Int) = withContext(Dispatchers.IO) {
        val current = gameDao.getUserStats() ?: UserStatsEntity()
        gameDao.insertOrUpdateUserStats(
            current.copy(coins = current.coins + amount)
        )
    }

    suspend fun useBooster(booster: BoosterType): Boolean = withContext(Dispatchers.IO) {
        val stats = gameDao.getUserStats() ?: return@withContext false
        val updated = when (booster) {
            BoosterType.LOLLIPOP_HAMMER -> {
                if (stats.lollipopHammers > 0) stats.copy(lollipopHammers = stats.lollipopHammers - 1) else null
            }
            BoosterType.FREE_SWITCH -> {
                if (stats.freeSwitches > 0) stats.copy(freeSwitches = stats.freeSwitches - 1) else null
            }
            BoosterType.COLOR_BOMB -> {
                if (stats.colorBombs > 0) stats.copy(colorBombs = stats.colorBombs - 1) else null
            }
            BoosterType.EXTRA_MOVES -> {
                if (stats.extraMoves > 0) stats.copy(extraMoves = stats.extraMoves - 1) else null
            }
        }

        if (updated != null) {
            gameDao.insertOrUpdateUserStats(updated)
            true
        } else {
            false
        }
    }

    suspend fun buyBooster(booster: BoosterType): Boolean = withContext(Dispatchers.IO) {
        val stats = gameDao.getUserStats() ?: return@withContext false
        if (stats.coins < booster.coinCost) return@withContext false

        val updated = when (booster) {
            BoosterType.LOLLIPOP_HAMMER -> stats.copy(
                coins = stats.coins - booster.coinCost,
                lollipopHammers = stats.lollipopHammers + 3
            )
            BoosterType.FREE_SWITCH -> stats.copy(
                coins = stats.coins - booster.coinCost,
                freeSwitches = stats.freeSwitches + 3
            )
            BoosterType.COLOR_BOMB -> stats.copy(
                coins = stats.coins - booster.coinCost,
                colorBombs = stats.colorBombs + 2
            )
            BoosterType.EXTRA_MOVES -> stats.copy(
                coins = stats.coins - booster.coinCost,
                extraMoves = stats.extraMoves + 3
            )
        }
        gameDao.insertOrUpdateUserStats(updated)
        true
    }

    suspend fun consumeLife(): Boolean = withContext(Dispatchers.IO) {
        val stats = gameDao.getUserStats() ?: return@withContext true
        if (stats.lives > 0) {
            gameDao.insertOrUpdateUserStats(
                stats.copy(lives = stats.lives - 1)
            )
            true
        } else {
            false
        }
    }

    suspend fun refillLives() = withContext(Dispatchers.IO) {
        val stats = gameDao.getUserStats() ?: return@withContext
        gameDao.insertOrUpdateUserStats(
            stats.copy(lives = stats.maxLives)
        )
    }

    suspend fun toggleHaptics() = withContext(Dispatchers.IO) {
        val stats = gameDao.getUserStats() ?: return@withContext
        gameDao.insertOrUpdateUserStats(
            stats.copy(hapticsEnabled = !stats.hapticsEnabled)
        )
    }

    suspend fun toggleSound() = withContext(Dispatchers.IO) {
        val stats = gameDao.getUserStats() ?: return@withContext
        gameDao.insertOrUpdateUserStats(
            stats.copy(soundEnabled = !stats.soundEnabled)
        )
    }

    suspend fun saveBlitzScore(score: Int): Boolean = withContext(Dispatchers.IO) {
        val stats = gameDao.getUserStats() ?: return@withContext false
        if (score > stats.blitzHighScore) {
            gameDao.insertOrUpdateUserStats(stats.copy(blitzHighScore = score))
            true
        } else {
            false
        }
    }

    suspend fun recordColorBombDetonation() = withContext(Dispatchers.IO) {
        val stats = gameDao.getUserStats() ?: return@withContext
        gameDao.insertOrUpdateUserStats(stats.copy(colorBombsDetonated = stats.colorBombsDetonated + 1))
    }

    suspend fun recordMaxCombo(combo: Int) = withContext(Dispatchers.IO) {
        val stats = gameDao.getUserStats() ?: return@withContext
        if (combo > stats.maxComboChain) {
            gameDao.insertOrUpdateUserStats(stats.copy(maxComboChain = combo))
        }
    }

    suspend fun claimAchievement(achievementId: String, rewardCoins: Int) = withContext(Dispatchers.IO) {
        val stats = gameDao.getUserStats() ?: return@withContext
        val currentClaimed = stats.claimedAchievements.split(",").filter { it.isNotBlank() }.toMutableSet()
        if (!currentClaimed.contains(achievementId)) {
            currentClaimed.add(achievementId)
            gameDao.insertOrUpdateUserStats(
                stats.copy(
                    coins = stats.coins + rewardCoins,
                    claimedAchievements = currentClaimed.joinToString(",")
                )
            )
        }
    }

    suspend fun awardWheelReward(
        coins: Int = 0,
        hammers: Int = 0,
        switches: Int = 0,
        bombs: Int = 0,
        fullLives: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val stats = gameDao.getUserStats() ?: return@withContext
        gameDao.insertOrUpdateUserStats(
            stats.copy(
                coins = stats.coins + coins,
                lollipopHammers = stats.lollipopHammers + hammers,
                freeSwitches = stats.freeSwitches + switches,
                colorBombs = stats.colorBombs + bombs,
                lives = if (fullLives) stats.maxLives else stats.lives
            )
        )
    }
}
