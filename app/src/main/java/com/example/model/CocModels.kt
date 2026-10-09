package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class ResourceType(val displayName: String, val colorHex: Long) {
    GOLD("Gold", 0xFFFFD700),
    ELIXIR("Elixir", 0xFFE040FB),
    DARK_ELIXIR("Dark Elixir", 0xFF00E5FF),
    GEMS("Gems", 0xFF00E676)
}

enum class BuildingCategory(val displayName: String) {
    TOWN_HALL("Town Hall"),
    DEFENSE("Defenses"),
    ARMY("Army & Heroes"),
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
    val levels: List<BuildingLevelInfo> = emptyList()
) {
    fun getLevel(targetLevel: Int): BuildingLevelInfo? {
        return levels.find { it.level == targetLevel }
            ?: levels.minByOrNull { kotlin.math.abs(it.level - targetLevel) }
    }
}

data class GameDataWrapper(
    val version: String = "1.0",
    val gameVersion: String = "Clash of Clans",
    val buildings: List<BuildingInfo> = emptyList()
)

@Entity(tableName = "upgrade_tasks")
data class UpgradeTask(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val buildingId: String = "",
    val buildingName: String = "",
    val category: BuildingCategory = BuildingCategory.DEFENSE,
    val fromLevel: Int = 1,
    val toLevel: Int = 2,
    val resourceType: ResourceType = ResourceType.GOLD,
    val cost: Long = 0L,
    val startTimeMillis: Long = System.currentTimeMillis(),
    val endTimeMillis: Long = System.currentTimeMillis(),
    val totalDurationSeconds: Long = 0L,
    val builderIndex: Int = 1, // 1..6
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

data class PlayerProfile(
    val townHallLevel: Int = 13,
    val currentGold: Long = 5_000_000L,
    val currentElixir: Long = 4_500_000L,
    val currentDarkElixir: Long = 120_000L,
    val maxGoldStorage: Long = 16_000_000L,
    val maxElixirStorage: Long = 16_000_000L,
    val maxDarkElixirStorage: Long = 320_000L,
    val totalBuilders: Int = 5,
    val avgLootPerRaidGold: Long = 650_000L,
    val avgLootPerRaidElixir: Long = 600_000L,
    val avgLootPerRaidDark: Long = 5_500L,
    val hourlyCollectorGold: Long = 140_000L,
    val hourlyCollectorElixir: Long = 140_000L,
    val hourlyCollectorDark: Long = 800L,
    val playerTag: String = "#9Y8Q2V8R",
    val playerName: String = "Chief ClashMaster",
    val notifyOnFinish: Boolean = true,
    val notifyBeforeFinishMinutes: Int = 15,
    val vibrationEnabled: Boolean = true
)
