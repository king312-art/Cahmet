package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JarvisDao {
  @Query("SELECT * FROM price_packages ORDER BY sortOrder ASC, id ASC")
  fun observePricePackages(): Flow<List<PricePackageEntity>>

  @Query("SELECT * FROM price_packages ORDER BY sortOrder ASC, id ASC")
  suspend fun getPricePackagesSnapshot(): List<PricePackageEntity>

  @Query("SELECT COUNT(*) FROM price_packages")
  suspend fun countPricePackages(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPricePackage(pkg: PricePackageEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAllPricePackages(packages: List<PricePackageEntity>)

  @Update
  suspend fun updatePricePackage(pkg: PricePackageEntity)

  @Query("DELETE FROM price_packages WHERE id = :id")
  suspend fun deletePricePackageById(id: Int)

  @Query("DELETE FROM price_packages")
  suspend fun deleteAllPricePackages()

  @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 30")
  fun observeRecentLogs(): Flow<List<ActivityLogEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertLog(log: ActivityLogEntity)

  @Query("DELETE FROM activity_logs")
  suspend fun clearAllLogs()
}
