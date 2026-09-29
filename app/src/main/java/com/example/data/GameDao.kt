package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM level_progress ORDER BY levelNumber ASC")
    fun getAllLevelProgress(): Flow<List<LevelProgressEntity>>

    @Query("SELECT * FROM level_progress WHERE levelNumber = :levelNumber")
    suspend fun getLevelProgress(levelNumber: Int): LevelProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLevelProgress(progress: LevelProgressEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInitialLevels(levels: List<LevelProgressEntity>)

    @Query("SELECT * FROM user_stats WHERE id = 1")
    fun getUserStatsFlow(): Flow<UserStatsEntity?>

    @Query("SELECT * FROM user_stats WHERE id = 1")
    suspend fun getUserStats(): UserStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUserStats(stats: UserStatsEntity)

    @Update
    suspend fun updateUserStats(stats: UserStatsEntity)
}
