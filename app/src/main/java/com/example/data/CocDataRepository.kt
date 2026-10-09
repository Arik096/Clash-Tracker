package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.BuildingCategory
import com.example.model.BuildingInfo
import com.example.model.BuildingLevelInfo
import com.example.model.GameDataWrapper
import com.example.model.PlayerProfile
import com.example.model.PriorityItem
import com.example.model.ResourceType
import com.example.model.UpgradeTask
import com.example.notification.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CocDataRepository(private val context: Context) {

    private val db = CocAppDatabase.getDatabase(context)
    private val upgradeDao = db.upgradeDao()
    private val priorityDao = db.priorityDao()
    private val notificationHelper = NotificationHelper(context)
    private val prefs: SharedPreferences = context.getSharedPreferences("coc_player_prefs", Context.MODE_PRIVATE)

    private val _gameData = MutableStateFlow<GameDataWrapper>(GameDataWrapper())
    val gameData = _gameData.asStateFlow()

    private val _playerProfile = MutableStateFlow(loadProfileFromPrefs())
    val playerProfile = _playerProfile.asStateFlow()

    val activeUpgrades: Flow<List<UpgradeTask>> = upgradeDao.getActiveUpgrades()
    val allPriorities: Flow<List<PriorityItem>> = priorityDao.getAllPriorities()

    suspend fun initialize() = withContext(Dispatchers.IO) {
        val savedCustomJson = prefs.getString("custom_game_data_json", null)
        if (!savedCustomJson.isNullOrBlank()) {
            try {
                val parsed = parseGameDataJson(savedCustomJson)
                _gameData.value = parsed
                return@withContext
            } catch (_: Exception) {
                // fallback to default assets
            }
        }
        loadDefaultAssetGameData()
        seedDefaultDataIfEmpty()
    }

    suspend fun loadDefaultAssetGameData() = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.assets.open("coc_game_data.json")
            val jsonString = InputStreamReader(inputStream).readText()
            inputStream.close()
            val parsed = parseGameDataJson(jsonString)
            _gameData.value = parsed
            prefs.edit().remove("custom_game_data_json").apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun importGameDataJson(jsonContent: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val parsed = parseGameDataJson(jsonContent)
            if (parsed.buildings.isNotEmpty()) {
                _gameData.value = parsed
                prefs.edit().putString("custom_game_data_json", jsonContent).apply()
                return@withContext true
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun parseGameDataJson(jsonString: String): GameDataWrapper {
        val root = JSONObject(jsonString)
        val version = root.optString("version", "1.0")
        val gameVersion = root.optString("gameVersion", "Clash of Clans")
        val buildingsArray = root.optJSONArray("buildings") ?: return GameDataWrapper(version, gameVersion, emptyList())

        val buildingsList = mutableListOf<BuildingInfo>()
        for (i in 0 until buildingsArray.length()) {
            val bObj = buildingsArray.getJSONObject(i)
            val id = bObj.optString("id", "")
            val name = bObj.optString("name", "")
            val categoryStr = bObj.optString("category", "DEFENSE")
            val resourceStr = bObj.optString("resourceType", "GOLD")
            val maxLevel = bObj.optInt("maxLevel", 1)
            val description = bObj.optString("description", "")

            val category = try { BuildingCategory.valueOf(categoryStr) } catch (_: Exception) { BuildingCategory.DEFENSE }
            val resourceType = try { ResourceType.valueOf(resourceStr) } catch (_: Exception) { ResourceType.GOLD }

            val levelsArray = bObj.optJSONArray("levels")
            val levelsList = mutableListOf<BuildingLevelInfo>()
            if (levelsArray != null) {
                for (j in 0 until levelsArray.length()) {
                    val lObj = levelsArray.getJSONObject(j)
                    levelsList.add(
                        BuildingLevelInfo(
                            level = lObj.optInt("level", 1),
                            thRequired = lObj.optInt("thRequired", 1),
                            cost = lObj.optLong("cost", 0L),
                            timeSeconds = lObj.optLong("timeSeconds", 0L),
                            hitpoints = lObj.optInt("hitpoints", 0),
                            dps = lObj.optInt("dps", 0)
                        )
                    )
                }
            }

            buildingsList.add(
                BuildingInfo(
                    id = id,
                    name = name,
                    category = category,
                    resourceType = resourceType,
                    maxLevel = maxLevel,
                    description = description,
                    levels = levelsList
                )
            )
        }
        return GameDataWrapper(version, gameVersion, buildingsList)
    }

    private suspend fun seedDefaultDataIfEmpty() {
        // If no priority items exist, insert initial recommended targets
        val currentPriorities = priorityDao.getAllPriorities()
        // We can pre-seed realistic sample upgrades if database is completely fresh
        val count = prefs.getInt("has_seeded_sample_v1", 0)
        if (count == 0) {
            val now = System.currentTimeMillis()
            // Sample active upgrade 1: X-Bow Lv 6 to Lv 7 (14 hours remaining)
            val upgrade1 = UpgradeTask(
                buildingId = "x_bow",
                buildingName = "X-Bow",
                category = BuildingCategory.DEFENSE,
                fromLevel = 6,
                toLevel = 7,
                resourceType = ResourceType.GOLD,
                cost = 9_500_000L,
                startTimeMillis = now - (36 * 3600 * 1000L),
                endTimeMillis = now + (14 * 3600 * 1000L),
                totalDurationSeconds = 50 * 3600L,
                builderIndex = 1,
                note = "Central base ground & air"
            )
            // Sample active upgrade 2: Clan Castle Lv 9 to 10 (2 hours 45 mins remaining)
            val upgrade2 = UpgradeTask(
                buildingId = "clan_castle",
                buildingName = "Clan Castle",
                category = BuildingCategory.ARMY,
                fromLevel = 9,
                toLevel = 10,
                resourceType = ResourceType.ELIXIR,
                cost = 14_000_000L,
                startTimeMillis = now - (70 * 3600 * 1000L),
                endTimeMillis = now + (2 * 3600 * 1000L + 45 * 60 * 1000L),
                totalDurationSeconds = 72 * 3600L,
                builderIndex = 2,
                note = "Unlocks +5 troop space"
            )
            // Sample active upgrade 3: Archer Queen Lv 74 to 75 (1 day 8 hours remaining)
            val upgrade3 = UpgradeTask(
                buildingId = "archer_queen",
                buildingName = "Archer Queen",
                category = BuildingCategory.HERO,
                fromLevel = 74,
                toLevel = 75,
                resourceType = ResourceType.DARK_ELIXIR,
                cost = 190_000L,
                startTimeMillis = now - (20 * 3600 * 1000L),
                endTimeMillis = now + (32 * 3600 * 1000L),
                totalDurationSeconds = 52 * 3600L,
                builderIndex = 3,
                note = "Royal Cloak upgrade milestone"
            )

            upgradeDao.insertUpgrade(upgrade1)
            upgradeDao.insertUpgrade(upgrade2)
            upgradeDao.insertUpgrade(upgrade3)

            notificationHelper.scheduleUpgradeNotification(upgrade1)
            notificationHelper.scheduleUpgradeNotification(upgrade2)
            notificationHelper.scheduleUpgradeNotification(upgrade3)

            // Seed priority queue
            priorityDao.insertPriority(
                PriorityItem(
                    buildingId = "monolith",
                    buildingName = "Monolith",
                    category = BuildingCategory.DEFENSE,
                    currentLevel = 1,
                    targetLevel = 2,
                    resourceType = ResourceType.DARK_ELIXIR,
                    cost = 340_000L,
                    durationSeconds = 1296000L,
                    priorityRank = 1,
                    notes = "Core defense priority against high-HP heroes"
                )
            )
            priorityDao.insertPriority(
                PriorityItem(
                    buildingId = "scattershot",
                    buildingName = "Scattershot",
                    category = BuildingCategory.DEFENSE,
                    currentLevel = 2,
                    targetLevel = 3,
                    resourceType = ResourceType.GOLD,
                    cost = 17_000_000L,
                    durationSeconds = 1123200L,
                    priorityRank = 2,
                    notes = "Splash defense against hybrid & root riders"
                )
            )
            priorityDao.insertPriority(
                PriorityItem(
                    buildingId = "army_camp",
                    buildingName = "Army Camp",
                    category = BuildingCategory.ARMY,
                    currentLevel = 11,
                    targetLevel = 12,
                    resourceType = ResourceType.ELIXIR,
                    cost = 13_000_000L,
                    durationSeconds = 950400L,
                    priorityRank = 3,
                    notes = "+5 army camp capacity"
                )
            )
            priorityDao.insertPriority(
                PriorityItem(
                    buildingId = "eagle_artillery",
                    buildingName = "Eagle Artillery",
                    category = BuildingCategory.DEFENSE,
                    currentLevel = 5,
                    targetLevel = 6,
                    resourceType = ResourceType.GOLD,
                    cost = 20_500_000L,
                    durationSeconds = 1296000L,
                    priorityRank = 4,
                    notes = "Heavy damage map-wide"
                )
            )

            prefs.edit().putInt("has_seeded_sample_v1", 1).apply()
        }
    }

    private fun loadProfileFromPrefs(): PlayerProfile {
        return PlayerProfile(
            townHallLevel = prefs.getInt("town_hall_level", 14),
            currentGold = prefs.getLong("current_gold", 6_200_000L),
            currentElixir = prefs.getLong("current_elixir", 8_400_000L),
            currentDarkElixir = prefs.getLong("current_dark_elixir", 165_000L),
            maxGoldStorage = prefs.getLong("max_gold_storage", 18_000_000L),
            maxElixirStorage = prefs.getLong("max_elixir_storage", 18_000_000L),
            maxDarkElixirStorage = prefs.getLong("max_dark_storage", 340_000L),
            totalBuilders = prefs.getInt("total_builders", 5),
            avgLootPerRaidGold = prefs.getLong("avg_loot_gold", 650_000L),
            avgLootPerRaidElixir = prefs.getLong("avg_loot_elixir", 600_000L),
            avgLootPerRaidDark = prefs.getLong("avg_loot_dark", 6_000L),
            hourlyCollectorGold = prefs.getLong("hourly_gold", 150_000L),
            hourlyCollectorElixir = prefs.getLong("hourly_elixir", 150_000L),
            hourlyCollectorDark = prefs.getLong("hourly_dark", 900L),
            playerTag = prefs.getString("player_tag", "#2P98YCL8") ?: "#2P98YCL8",
            playerName = prefs.getString("player_name", "Chief Warrior") ?: "Chief Warrior",
            notifyOnFinish = prefs.getBoolean("notify_finish", true),
            notifyBeforeFinishMinutes = prefs.getInt("notify_before", 15),
            vibrationEnabled = prefs.getBoolean("notify_vibrate", true)
        )
    }

    fun saveProfile(profile: PlayerProfile) {
        _playerProfile.value = profile
        prefs.edit()
            .putInt("town_hall_level", profile.townHallLevel)
            .putLong("current_gold", profile.currentGold)
            .putLong("current_elixir", profile.currentElixir)
            .putLong("current_dark_elixir", profile.currentDarkElixir)
            .putLong("max_gold_storage", profile.maxGoldStorage)
            .putLong("max_elixir_storage", profile.maxElixirStorage)
            .putLong("max_dark_storage", profile.maxDarkElixirStorage)
            .putInt("total_builders", profile.totalBuilders)
            .putLong("avg_loot_gold", profile.avgLootPerRaidGold)
            .putLong("avg_loot_elixir", profile.avgLootPerRaidElixir)
            .putLong("avg_loot_dark", profile.avgLootPerRaidDark)
            .putLong("hourly_gold", profile.hourlyCollectorGold)
            .putLong("hourly_elixir", profile.hourlyCollectorElixir)
            .putLong("hourly_dark", profile.hourlyCollectorDark)
            .putString("player_tag", profile.playerTag)
            .putString("player_name", profile.playerName)
            .putBoolean("notify_finish", profile.notifyOnFinish)
            .putInt("notify_before", profile.notifyBeforeFinishMinutes)
            .putBoolean("notify_vibrate", profile.vibrationEnabled)
            .apply()
    }

    // Upgrade management
    suspend fun startUpgrade(task: UpgradeTask) = withContext(Dispatchers.IO) {
        upgradeDao.insertUpgrade(task)
        if (task.notificationScheduled) {
            notificationHelper.scheduleUpgradeNotification(task)
        }
    }

    suspend fun completeUpgrade(taskId: String) = withContext(Dispatchers.IO) {
        val task = upgradeDao.getUpgradeById(taskId)
        if (task != null) {
            notificationHelper.cancelUpgradeNotification(task)
            upgradeDao.markCompleted(taskId)
        }
    }

    suspend fun deleteUpgrade(taskId: String) = withContext(Dispatchers.IO) {
        val task = upgradeDao.getUpgradeById(taskId)
        if (task != null) {
            notificationHelper.cancelUpgradeNotification(task)
            upgradeDao.deleteById(taskId)
        }
    }

    suspend fun applyBuilderPotion(taskId: String, boostMultiplier: Int = 10, durationHours: Int = 1) = withContext(Dispatchers.IO) {
        val task = upgradeDao.getUpgradeById(taskId) ?: return@withContext
        val now = System.currentTimeMillis()
        val rem = task.endTimeMillis - now
        if (rem <= 0) return@withContext

        // Boost accelerates time by saving (multiplier - 1) hours of work
        val savedMillis = (boostMultiplier - 1) * durationHours * 3600 * 1000L
        val newEndTime = if (rem <= savedMillis) now + 1000L else task.endTimeMillis - savedMillis

        val updated = task.copy(endTimeMillis = newEndTime)
        upgradeDao.updateUpgrade(updated)
        notificationHelper.cancelUpgradeNotification(task)
        notificationHelper.scheduleUpgradeNotification(updated)
    }

    // Priority queue management
    suspend fun addPriorityItem(item: PriorityItem) = withContext(Dispatchers.IO) {
        priorityDao.insertPriority(item)
    }

    suspend fun removePriorityItem(id: String) = withContext(Dispatchers.IO) {
        priorityDao.deleteById(id)
    }

    suspend fun updatePriority(item: PriorityItem) = withContext(Dispatchers.IO) {
        priorityDao.updatePriority(item)
    }

    // Calculations
    data class LootShortageResult(
        val cost: Long,
        val currentLoot: Long,
        val shortage: Long, // 0 if user has enough
        val percentOwned: Float,
        val isReady: Boolean,
        val raidsNeeded: Int,
        val passiveHoursNeeded: Float,
        val resourceType: ResourceType
    )

    fun calculateShortage(cost: Long, resourceType: ResourceType, profile: PlayerProfile): LootShortageResult {
        val current = when (resourceType) {
            ResourceType.GOLD -> profile.currentGold
            ResourceType.ELIXIR -> profile.currentElixir
            ResourceType.DARK_ELIXIR -> profile.currentDarkElixir
            ResourceType.GEMS -> 0L
        }
        val shortage = if (cost > current) cost - current else 0L
        val percent = if (cost > 0) (current.toFloat() / cost.toFloat()).coerceIn(0f, 1f) else 1f
        val isReady = shortage == 0L

        val avgRaid = when (resourceType) {
            ResourceType.GOLD -> profile.avgLootPerRaidGold
            ResourceType.ELIXIR -> profile.avgLootPerRaidElixir
            ResourceType.DARK_ELIXIR -> profile.avgLootPerRaidDark
            ResourceType.GEMS -> 1L
        }.coerceAtLeast(1L)

        val hourlyPassive = when (resourceType) {
            ResourceType.GOLD -> profile.hourlyCollectorGold
            ResourceType.ELIXIR -> profile.hourlyCollectorElixir
            ResourceType.DARK_ELIXIR -> profile.hourlyCollectorDark
            ResourceType.GEMS -> 1L
        }.coerceAtLeast(1L)

        val raidsNeeded = if (shortage > 0) kotlin.math.ceil(shortage.toDouble() / avgRaid.toDouble()).toInt() else 0
        val passiveHours = if (shortage > 0) (shortage.toFloat() / hourlyPassive.toFloat()) else 0f

        return LootShortageResult(
            cost = cost,
            currentLoot = current,
            shortage = shortage,
            percentOwned = percent,
            isReady = isReady,
            raidsNeeded = raidsNeeded,
            passiveHoursNeeded = passiveHours,
            resourceType = resourceType
        )
    }

    data class LoginAdvice(
        val recommendedTimeMillis: Long,
        val formattedTime: String,
        val timeRemainingFormatted: String,
        val reason: String,
        val builderIndex: Int,
        val buildingName: String,
        val targetLevel: Int,
        val hasActiveUpgrades: Boolean
    )

    fun calculateNextLoginAdvice(activeTasks: List<UpgradeTask>): LoginAdvice {
        val uncompleted = activeTasks.filter { !it.isCompleted }
        if (uncompleted.isEmpty()) {
            return LoginAdvice(
                recommendedTimeMillis = System.currentTimeMillis(),
                formattedTime = "Now",
                timeRemainingFormatted = "All builders are idle",
                reason = "All your builders are currently free! Log into Clash of Clans now to assign them upgrades and keep progress moving.",
                builderIndex = 1,
                buildingName = "Village Builder",
                targetLevel = 1,
                hasActiveUpgrades = false
            )
        }

        val nextTask = uncompleted.minByOrNull { it.endTimeMillis }!!
        val now = System.currentTimeMillis()
        val remMillis = (nextTask.endTimeMillis - now).coerceAtLeast(0L)

        val timeFormatter = SimpleDateFormat("h:mm a, MMM d", Locale.getDefault())
        val formattedTime = timeFormatter.format(Date(nextTask.endTimeMillis))

        val hours = remMillis / (3600 * 1000)
        val minutes = (remMillis % (3600 * 1000)) / (60 * 1000)
        val timeRemString = when {
            remMillis <= 0 -> "Finishing now!"
            hours > 24 -> "${hours / 24}d ${hours % 24}h remaining"
            hours > 0 -> "${hours}h ${minutes}m remaining"
            else -> "${minutes}m remaining"
        }

        val reason = "Builder #${nextTask.builderIndex} completes ${nextTask.buildingName} (Lv ${nextTask.toLevel}). Log in right when this finishes to queue your next upgrade and spend accumulated loot before getting raided!"

        return LoginAdvice(
            recommendedTimeMillis = nextTask.endTimeMillis,
            formattedTime = formattedTime,
            timeRemainingFormatted = timeRemString,
            reason = reason,
            builderIndex = nextTask.builderIndex,
            buildingName = nextTask.buildingName,
            targetLevel = nextTask.toLevel,
            hasActiveUpgrades = true
        )
    }

    data class TargetPlanSummary(
        val totalItems: Int,
        val totalGoldCost: Long,
        val totalElixirCost: Long,
        val totalDarkElixirCost: Long,
        val totalDurationSeconds: Long,
        val builderDaysNeeded: Float,
        val calendarDaysWithBuilders: Float,
        val estimatedCompletionDate: String
    )

    fun calculateTargetSummary(items: List<PriorityItem>, builderCount: Int): TargetPlanSummary {
        var gold = 0L
        var elixir = 0L
        var dark = 0L
        var totalSecs = 0L

        for (item in items) {
            when (item.resourceType) {
                ResourceType.GOLD -> gold += item.cost
                ResourceType.ELIXIR -> elixir += item.cost
                ResourceType.DARK_ELIXIR -> dark += item.cost
                ResourceType.GEMS -> {}
            }
            totalSecs += item.durationSeconds
        }

        val builderDays = (totalSecs.toFloat() / 86400f)
        val effectiveBuilders = builderCount.coerceAtLeast(1)
        val calendarDays = builderDays / effectiveBuilders.toFloat()

        val finishMillis = System.currentTimeMillis() + (calendarDays * 86400 * 1000).toLong()
        val dateFormatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val estimatedDate = if (items.isNotEmpty()) dateFormatter.format(Date(finishMillis)) else "No targets set"

        return TargetPlanSummary(
            totalItems = items.size,
            totalGoldCost = gold,
            totalElixirCost = elixir,
            totalDarkElixirCost = dark,
            totalDurationSeconds = totalSecs,
            builderDaysNeeded = builderDays,
            calendarDaysWithBuilders = calendarDays,
            estimatedCompletionDate = estimatedDate
        )
    }
}
