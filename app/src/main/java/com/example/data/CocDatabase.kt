package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import com.example.model.BuildingCategory
import com.example.model.PriorityItem
import com.example.model.ResourceType
import com.example.model.UpgradeTask
import kotlinx.coroutines.flow.Flow

class CocTypeConverters {
    @TypeConverter
    fun fromResourceType(value: ResourceType): String = value.name

    @TypeConverter
    fun toResourceType(value: String): ResourceType = try {
        ResourceType.valueOf(value)
    } catch (_: Exception) {
        ResourceType.GOLD
    }

    @TypeConverter
    fun fromBuildingCategory(value: BuildingCategory): String = value.name

    @TypeConverter
    fun toBuildingCategory(value: String): BuildingCategory = try {
        BuildingCategory.valueOf(value)
    } catch (_: Exception) {
        BuildingCategory.DEFENSE
    }
}

@Dao
interface UpgradeDao {
    @Query("SELECT * FROM upgrade_tasks WHERE isCompleted = 0 ORDER BY endTimeMillis ASC")
    fun getActiveUpgrades(): Flow<List<UpgradeTask>>

    @Query("SELECT * FROM upgrade_tasks ORDER BY endTimeMillis ASC")
    fun getAllUpgrades(): Flow<List<UpgradeTask>>

    @Query("SELECT * FROM upgrade_tasks WHERE id = :id LIMIT 1")
    suspend fun getUpgradeById(id: String): UpgradeTask?

    @Query("SELECT * FROM upgrade_tasks WHERE builderIndex = :builderIndex AND isCompleted = 0 LIMIT 1")
    suspend fun getActiveUpgradeForBuilder(builderIndex: Int): UpgradeTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUpgrade(upgrade: UpgradeTask)

    @Update
    suspend fun updateUpgrade(upgrade: UpgradeTask)

    @Query("UPDATE upgrade_tasks SET isCompleted = 1 WHERE id = :id")
    suspend fun markCompleted(id: String)

    @Delete
    suspend fun deleteUpgrade(upgrade: UpgradeTask)

    @Query("DELETE FROM upgrade_tasks WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface PriorityDao {
    @Query("SELECT * FROM priority_items ORDER BY priorityRank ASC")
    fun getAllPriorities(): Flow<List<PriorityItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriority(item: PriorityItem)

    @Update
    suspend fun updatePriority(item: PriorityItem)

    @Delete
    suspend fun deletePriority(item: PriorityItem)

    @Query("DELETE FROM priority_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM priority_items")
    suspend fun clearAll()
}

@Database(
    entities = [UpgradeTask::class, PriorityItem::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(CocTypeConverters::class)
abstract class CocAppDatabase : RoomDatabase() {
    abstract fun upgradeDao(): UpgradeDao
    abstract fun priorityDao(): PriorityDao

    companion object {
        @Volatile
        private var INSTANCE: CocAppDatabase? = null

        fun getDatabase(context: Context): CocAppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CocAppDatabase::class.java,
                    "clash_tracker_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
