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
import com.example.model.VillageStructure
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
interface VillageStructureDao {
    @Query("SELECT * FROM village_structures ORDER BY category ASC, name ASC, structureIndex ASC")
    fun getAllStructures(): Flow<List<VillageStructure>>

    @Query("SELECT * FROM village_structures WHERE category = :category ORDER BY name ASC, structureIndex ASC")
    fun getStructuresByCategory(category: BuildingCategory): Flow<List<VillageStructure>>

    @Query("SELECT * FROM village_structures WHERE id = :id LIMIT 1")
    suspend fun getStructureById(id: String): VillageStructure?

    @Query("SELECT * FROM village_structures WHERE buildingId = :buildingId ORDER BY structureIndex ASC")
    suspend fun getStructuresForBuilding(buildingId: String): List<VillageStructure>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStructures(structures: List<VillageStructure>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStructure(structure: VillageStructure)

    @Update
    suspend fun updateStructure(structure: VillageStructure)

    @Query("UPDATE village_structures SET currentLevel = :newLevel WHERE id = :id")
    suspend fun updateLevel(id: String, newLevel: Int)

    @Query("UPDATE village_structures SET isUpgrading = :isUpgrading, activeUpgradeId = :upgradeId WHERE id = :id")
    suspend fun setUpgrading(id: String, isUpgrading: Boolean, upgradeId: String?)

    @Query("UPDATE village_structures SET currentLevel = :level WHERE buildingId = :buildingId")
    suspend fun setBuildingTypeLevel(buildingId: String, level: Int)

    @Query("DELETE FROM village_structures")
    suspend fun clearAll()
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
    entities = [UpgradeTask::class, PriorityItem::class, VillageStructure::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(CocTypeConverters::class)
abstract class CocAppDatabase : RoomDatabase() {
    abstract fun upgradeDao(): UpgradeDao
    abstract fun priorityDao(): PriorityDao
    abstract fun villageStructureDao(): VillageStructureDao

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
