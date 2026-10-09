package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class ResourceType(val displayName: String, val colorHex: Long) {
    GOLD("Gold", 0xFFFFD700),
    ELIXIR("Elixir", 0xFFE040FB),
    DARK_ELIXIR("Dark Elixir", 0xFF00E5FF),
    ORE("Ores", 0xFF64B5F6),
    GEMS("Gems", 0xFF00E676)
}

enum class BuildingCategory(val displayName: String) {
    TOWN_HALL("Town Hall"),
    DEFENSE("Defenses"),
    ARMY("Army & Camps"),
    HERO("Heroes"),
    RESOURCE("Resources"),
    TRAP("Traps"),
    WALL("Walls")
}

data class BuildingLevelInfo(
    val level: Int = 1,
    val thRequired: Int = 1,
    val cost: Long = 0L,
    val timeSeconds: Long = 0L,
    val hitpoints: Int = 0,
    val dps: Int = 0
)

data class BuildingInfo(
    val id: String = "",
    val name: String = "",
    val category: BuildingCategory = BuildingCategory.DEFENSE,
    val resourceType: ResourceType = ResourceType.GOLD,
    val maxLevel: Int = 1,
    val description: String = "",
    val countAtTh: Map<Int, Int> = emptyMap(), // How many can you build at each TH
    val maxLevelAtTh: Map<Int, Int> = emptyMap(), // Max level allowed at each TH
    val levels: List<BuildingLevelInfo> = emptyList()
) {
    fun getLevel(targetLevel: Int): BuildingLevelInfo? {
        return levels.find { it.level == targetLevel }
            ?: levels.minByOrNull { kotlin.math.abs(it.level - targetLevel) }
    }

    fun getMaxLevelForTh(th: Int): Int {
        if (maxLevelAtTh.isNotEmpty()) {
            val direct = maxLevelAtTh[th]
            if (direct != null) return direct
            // otherwise find highest TH <= player TH
            val valid = maxLevelAtTh.filterKeys { it <= th }
            if (valid.isNotEmpty()) return valid.maxByOrNull { it.key }!!.value
        }
        val allowedLevels = levels.filter { it.thRequired <= th }
        return allowedLevels.maxOfOrNull { it.level } ?: 1
    }
}

data class GameDataWrapper(
    val version: String = "1.0",
    val gameVersion: String = "Clash of Clans",
    val buildings: List<BuildingInfo> = emptyList()
)

@Entity(tableName = "village_structures")
data class VillageStructure(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val buildingId: String = "",
    val name: String = "",
    val category: BuildingCategory = BuildingCategory.DEFENSE,
    val currentLevel: Int = 1,
    val maxLevelForTH: Int = 1,
    val absoluteMaxLevel: Int = 1,
    val structureIndex: Int = 1, // e.g., Cannon #1, Cannon #2
    val resourceType: ResourceType = ResourceType.GOLD,
    val isUpgrading: Boolean = false,
    val activeUpgradeId: String? = null
) {
    val isMaxedForTh: Boolean
        get() = currentLevel >= maxLevelForTH
}

@Entity(tableName = "upgrade_tasks")
data class UpgradeTask(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val structureId: String = "",
    val buildingId: String = "",
    val buildingName: String = "",
    val category: BuildingCategory = BuildingCategory.DEFENSE,
    val fromLevel: Int = 1,
    val toLevel: Int = 2,
    val resourceType: ResourceType = ResourceType.GOLD,
    val originalCost: Long = 0L,
    val cost: Long = 0L, // after season boost
    val originalDurationSeconds: Long = 0L,
    val totalDurationSeconds: Long = 0L, // after season boost
    val startTimeMillis: Long = System.currentTimeMillis(),
    val endTimeMillis: Long = System.currentTimeMillis(),
    val builderIndex: Int = 1, // 1..6, or 7 for Lab, 8 for Pet
    val isCompleted: Boolean = false,
    val notificationScheduled: Boolean = true,
    val note: String = ""
) {
    fun remainingMillis(currentTimeMillis: Long = System.currentTimeMillis()): Long {
        val rem = endTimeMillis - currentTimeMillis
        return if (rem > 0) rem else 0L
    }

    fun isFinished(currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        return currentTimeMillis >= endTimeMillis
    }

    fun progressFraction(currentTimeMillis: Long = System.currentTimeMillis()): Float {
        val totalMillis = endTimeMillis - startTimeMillis
        if (totalMillis <= 0) return 1f
        val elapsed = currentTimeMillis - startTimeMillis
        return (elapsed.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)
    }
}

@Entity(tableName = "priority_items")
data class PriorityItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val structureId: String = "",
    val buildingId: String = "",
    val buildingName: String = "",
    val category: BuildingCategory = BuildingCategory.DEFENSE,
    val currentLevel: Int = 1,
    val targetLevel: Int = 2,
    val resourceType: ResourceType = ResourceType.GOLD,
    val cost: Long = 0L,
    val durationSeconds: Long = 0L,
    val priorityRank: Int = 1,
    val notes: String = ""
)

data class VillageProgressStats(
    val overallPercent: Float = 0f,
    val defensesPercent: Float = 0f,
    val heroesPercent: Float = 0f,
    val armyLabPercent: Float = 0f,
    val resourcesPercent: Float = 0f,
    val totalStructuresCount: Int = 0,
    val maxedStructuresCount: Int = 0,
    val remainingUpgradesCount: Int = 0,
    val remainingBuilderDays: Float = 0f,
    val remainingCalendarDays: Float = 0f,
    val remainingGold: Long = 0L,
    val remainingElixir: Long = 0L,
    val remainingDarkElixir: Long = 0L
)

data class PlayerProfile(
    val townHallLevel: Int = 14,
    val goldPassBoostPercent: Int = 20, // 0%, 10%, 15%, 20% (Clash Ninja feature)
    val currentGold: Long = 8_500_000L,
    val currentElixir: Long = 7_200_000L,
    val currentDarkElixir: Long = 180_000L,
    val maxGoldStorage: Long = 20_000_000L,
    val maxElixirStorage: Long = 20_000_000L,
    val maxDarkElixirStorage: Long = 350_000L,
    val totalBuilders: Int = 6, // including B.O.B
    val avgLootPerRaidGold: Long = 750_000L,
    val avgLootPerRaidElixir: Long = 700_000L,
    val avgLootPerRaidDark: Long = 6_500L,
    val hourlyCollectorGold: Long = 160_000L,
    val hourlyCollectorElixir: Long = 160_000L,
    val hourlyCollectorDark: Long = 1_000L,
    val playerTag: String = "#8P2V8UQG",
    val playerName: String = "Ninja Chief",
    val notifyOnFinish: Boolean = true,
    val notifyBeforeFinishMinutes: Int = 15,
    val vibrationEnabled: Boolean = true
)
