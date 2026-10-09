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
import com.example.model.VillageProgressStats
import com.example.model.VillageStructure
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
import java.util.UUID

class CocDataRepository(private val context: Context) {

    private val db = CocAppDatabase.getDatabase(context)
    private val upgradeDao = db.upgradeDao()
    private val priorityDao = db.priorityDao()
    private val villageStructureDao = db.villageStructureDao()
    private val notificationHelper = NotificationHelper(context)
    private val prefs: SharedPreferences = context.getSharedPreferences("coc_player_prefs", Context.MODE_PRIVATE)

    private val _gameData = MutableStateFlow<GameDataWrapper>(GameDataWrapper())
    val gameData = _gameData.asStateFlow()

    private val _playerProfile = MutableStateFlow(loadProfileFromPrefs())
    val playerProfile = _playerProfile.asStateFlow()

    val activeUpgrades: Flow<List<UpgradeTask>> = upgradeDao.getActiveUpgrades()
    val allPriorities: Flow<List<PriorityItem>> = priorityDao.getAllPriorities()
    val villageStructures: Flow<List<VillageStructure>> = villageStructureDao.getAllStructures()

    suspend fun initialize() = withContext(Dispatchers.IO) {
        val savedCustomJson = prefs.getString("custom_game_data_json", null)
        if (!savedCustomJson.isNullOrBlank()) {
            try {
                val parsed = parseGameDataJson(savedCustomJson)
                _gameData.value = parsed
            } catch (_: Exception) {
                loadDefaultAssetGameData()
            }
        } else {
            loadDefaultAssetGameData()
        }

        val hasInitStructures = prefs.getBoolean("has_initialized_village_v2", false)
        if (!hasInitStructures) {
            seedVillageStructuresForTownHall(_playerProfile.value.townHallLevel, setPreviousThMax = false)
            prefs.edit().putBoolean("has_initialized_village_v2", true).apply()
        }
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

    suspend fun seedVillageStructuresForTownHall(th: Int, setPreviousThMax: Boolean) = withContext(Dispatchers.IO) {
        val buildings = _gameData.value.buildings
        val structuresList = mutableListOf<VillageStructure>()

        // Configuration of structure counts at high THs (Clash Ninja village layout)
        val structureConfig = listOf(
            Triple("cannon", 5, 20),
            Triple("archer_tower", 6, 20),
            Triple("eagle_artillery", 1, 6),
            Triple("monolith", 1, 3),
            Triple("scattershot", 2, 4),
            Triple("spell_tower", 2, 3),
            Triple("inferno_tower", 3, 9),
            Triple("x_bow", 4, 10),
            Triple("air_defense", 4, 13),
            Triple("wizard_tower", 5, 15),
            Triple("hidden_tesla", 5, 13),
            Triple("barbarian_king", 1, 85),
            Triple("archer_queen", 1, 85),
            Triple("grand_warden", 1, 60),
            Triple("royal_champion", 1, 35),
            Triple("clan_castle", 1, 11),
            Triple("army_camp", 4, 12),
            Triple("laboratory", 1, 13),
            Triple("pet_house", 1, 7),
            Triple("blacksmith", 1, 7),
            Triple("gold_storage", 4, 16),
            Triple("elixir_storage", 4, 16),
            Triple("dark_elixir_storage", 1, 10),
            Triple("wall_batch", 1, 16)
        )

        for ((bId, count, defaultMax) in structureConfig) {
            val bInfo = buildings.find { it.id == bId } ?: continue
            val maxForThisTh = bInfo.getMaxLevelForTh(th).coerceAtLeast(1)

            for (idx in 1..count) {
                val currentLvl = if (setPreviousThMax) {
                    val prevMax = bInfo.getMaxLevelForTh(th - 1)
                    prevMax.coerceAtLeast(1)
                } else {
                    // Realistic mid-TH progression
                    if (idx % 2 == 0) maxForThisTh else (maxForThisTh - 1).coerceAtLeast(1)
                }

                structuresList.add(
                    VillageStructure(
                        id = "${bId}_$idx",
                        buildingId = bId,
                        name = if (count > 1) "${bInfo.name} #$idx" else bInfo.name,
                        category = bInfo.category,
                        currentLevel = currentLvl,
                        maxLevelForTH = maxForThisTh,
                        absoluteMaxLevel = bInfo.maxLevel,
                        structureIndex = idx,
                        resourceType = bInfo.resourceType,
                        isUpgrading = false
                    )
                )
            }
        }

        villageStructureDao.clearAll()
        villageStructureDao.insertStructures(structuresList)
    }

    suspend fun updateStructureLevel(id: String, newLevel: Int) = withContext(Dispatchers.IO) {
        villageStructureDao.updateLevel(id, newLevel)
    }

    suspend fun setAllToPreviousTownHallMax(th: Int) = withContext(Dispatchers.IO) {
        seedVillageStructuresForTownHall(th, setPreviousThMax = true)
    }

    // Clash Ninja Village Progress Calculations
    fun calculateVillageProgress(structures: List<VillageStructure>, profile: PlayerProfile): VillageProgressStats {
        if (structures.isEmpty()) return VillageProgressStats()

        val buildingsMap = _gameData.value.buildings.associateBy { it.id }
        val boost = profile.goldPassBoostPercent // 0, 10, 15, 20%
        val multiplier = (100 - boost) / 100f

        var totalRequiredLevels = 0
        var totalCompletedLevels = 0

        var defReq = 0
        var defDone = 0
        var heroReq = 0
        var heroDone = 0
        var armyReq = 0
        var armyDone = 0
        var resReq = 0
        var resDone = 0

        var remainingTimeSecs = 0L
        var remainingGold = 0L
        var remainingElixir = 0L
        var remainingDarkElixir = 0L
        var maxedCount = 0
        var remainingUpgrades = 0

        for (s in structures) {
            val maxLvl = s.maxLevelForTH
            val curLvl = s.currentLevel.coerceAtMost(maxLvl)

            totalRequiredLevels += maxLvl
            totalCompletedLevels += curLvl

            when (s.category) {
                BuildingCategory.DEFENSE -> {
                    defReq += maxLvl
                    defDone += curLvl
                }
                BuildingCategory.HERO -> {
                    heroReq += maxLvl
                    heroDone += curLvl
                }
                BuildingCategory.ARMY -> {
                    armyReq += maxLvl
                    armyDone += curLvl
                }
                BuildingCategory.RESOURCE -> {
                    resReq += maxLvl
                    resDone += curLvl
                }
                else -> {}
            }

            if (curLvl >= maxLvl) {
                maxedCount++
            } else {
                remainingUpgrades += (maxLvl - curLvl)
                val bInfo = buildingsMap[s.buildingId]
                if (bInfo != null) {
                    for (lvl in (curLvl + 1)..maxLvl) {
                        val lvlData = bInfo.getLevel(lvl)
                        if (lvlData != null) {
                            val discountedTime = (lvlData.timeSeconds * multiplier).toLong()
                            val discountedCost = (lvlData.cost * multiplier).toLong()

                            remainingTimeSecs += discountedTime
                            when (s.resourceType) {
                                ResourceType.GOLD -> remainingGold += discountedCost
                                ResourceType.ELIXIR -> remainingElixir += discountedCost
                                ResourceType.DARK_ELIXIR -> remainingDarkElixir += discountedCost
                                else -> {}
                            }
                        }
                    }
                }
            }
        }

        val overall = if (totalRequiredLevels > 0) (totalCompletedLevels.toFloat() / totalRequiredLevels.toFloat()) * 100f else 100f
        val defPercent = if (defReq > 0) (defDone.toFloat() / defReq.toFloat()) * 100f else 100f
        val heroPercent = if (heroReq > 0) (heroDone.toFloat() / heroReq.toFloat()) * 100f else 100f
        val armyPercent = if (armyReq > 0) (armyDone.toFloat() / armyReq.toFloat()) * 100f else 100f
        val resPercent = if (resReq > 0) (resDone.toFloat() / resReq.toFloat()) * 100f else 100f

        val totalBuilderDays = remainingTimeSecs.toFloat() / 86400f
        val calendarDays = totalBuilderDays / profile.totalBuilders.coerceAtLeast(1).toFloat()

        return VillageProgressStats(
            overallPercent = overall,
            defensesPercent = defPercent,
            heroesPercent = heroPercent,
            armyLabPercent = armyPercent,
            resourcesPercent = resPercent,
            totalStructuresCount = structures.size,
            maxedStructuresCount = maxedCount,
            remainingUpgradesCount = remainingUpgrades,
            remainingBuilderDays = totalBuilderDays,
            remainingCalendarDays = calendarDays,
            remainingGold = remainingGold,
            remainingElixir = remainingElixir,
            remainingDarkElixir = remainingDarkElixir
        )
    }

    private fun loadProfileFromPrefs(): PlayerProfile {
        return PlayerProfile(
            townHallLevel = prefs.getInt("town_hall_level", 14),
            goldPassBoostPercent = prefs.getInt("gold_pass_boost", 20),
            currentGold = prefs.getLong("current_gold", 8_500_000L),
            currentElixir = prefs.getLong("current_elixir", 7_200_000L),
            currentDarkElixir = prefs.getLong("current_dark_elixir", 180_000L),
            maxGoldStorage = prefs.getLong("max_gold_storage", 20_000_000L),
            maxElixirStorage = prefs.getLong("max_elixir_storage", 20_000_000L),
            maxDarkElixirStorage = prefs.getLong("max_dark_storage", 350_000L),
            totalBuilders = prefs.getInt("total_builders", 6),
            avgLootPerRaidGold = prefs.getLong("avg_loot_gold", 750_000L),
            avgLootPerRaidElixir = prefs.getLong("avg_loot_elixir", 700_000L),
            avgLootPerRaidDark = prefs.getLong("avg_loot_dark", 6_500L),
            hourlyCollectorGold = prefs.getLong("hourly_gold", 160_000L),
            hourlyCollectorElixir = prefs.getLong("hourly_elixir", 160_000L),
            hourlyCollectorDark = prefs.getLong("hourly_dark", 1_000L),
            playerTag = prefs.getString("player_tag", "#8P2V8UQG") ?: "#8P2V8UQG",
            playerName = prefs.getString("player_name", "Ninja Chief") ?: "Ninja Chief",
            notifyOnFinish = prefs.getBoolean("notify_finish", true),
            notifyBeforeFinishMinutes = prefs.getInt("notify_before", 15),
            vibrationEnabled = prefs.getBoolean("notify_vibrate", true)
        )
    }

    fun saveProfile(profile: PlayerProfile) {
        _playerProfile.value = profile
        prefs.edit()
            .putInt("town_hall_level", profile.townHallLevel)
            .putInt("gold_pass_boost", profile.goldPassBoostPercent)
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
        if (task.structureId.isNotEmpty()) {
            villageStructureDao.setUpgrading(task.structureId, true, task.id)
        }
        if (task.notificationScheduled) {
            notificationHelper.scheduleUpgradeNotification(task)
        }
    }

    suspend fun completeUpgrade(taskId: String) = withContext(Dispatchers.IO) {
        val task = upgradeDao.getUpgradeById(taskId)
        if (task != null) {
            notificationHelper.cancelUpgradeNotification(task)
            upgradeDao.markCompleted(taskId)
            if (task.structureId.isNotEmpty()) {
                villageStructureDao.updateLevel(task.structureId, task.toLevel)
                villageStructureDao.setUpgrading(task.structureId, false, null)
            }
        }
    }

    suspend fun deleteUpgrade(taskId: String) = withContext(Dispatchers.IO) {
        val task = upgradeDao.getUpgradeById(taskId)
        if (task != null) {
            notificationHelper.cancelUpgradeNotification(task)
            upgradeDao.deleteById(taskId)
            if (task.structureId.isNotEmpty()) {
                villageStructureDao.setUpgrading(task.structureId, false, null)
            }
        }
    }

    suspend fun applyBuilderPotion(taskId: String, boostMultiplier: Int = 10, durationHours: Int = 1) = withContext(Dispatchers.IO) {
        val task = upgradeDao.getUpgradeById(taskId) ?: return@withContext
        val now = System.currentTimeMillis()
        val rem = task.endTimeMillis - now
        if (rem <= 0) return@withContext

        val savedMillis = (boostMultiplier - 1) * durationHours * 3600 * 1000L
        val newEndTime = if (rem <= savedMillis) now + 1000L else task.endTimeMillis - savedMillis

        val updated = task.copy(endTimeMillis = newEndTime)
        upgradeDao.updateUpgrade(updated)
        notificationHelper.cancelUpgradeNotification(task)
        notificationHelper.scheduleUpgradeNotification(updated)
    }

    // Priority queue
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
        val shortage: Long,
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
            else -> 0L
        }
        val shortage = if (cost > current) cost - current else 0L
        val percent = if (cost > 0) (current.toFloat() / cost.toFloat()).coerceIn(0f, 1f) else 1f
        val isReady = shortage == 0L

        val avgRaid = when (resourceType) {
            ResourceType.GOLD -> profile.avgLootPerRaidGold
            ResourceType.ELIXIR -> profile.avgLootPerRaidElixir
            ResourceType.DARK_ELIXIR -> profile.avgLootPerRaidDark
            else -> 1L
        }.coerceAtLeast(1L)

        val hourlyPassive = when (resourceType) {
            ResourceType.GOLD -> profile.hourlyCollectorGold
            ResourceType.ELIXIR -> profile.hourlyCollectorElixir
            ResourceType.DARK_ELIXIR -> profile.hourlyCollectorDark
            else -> 1L
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
                reason = "All builders are free! Log in to Clash of Clans to start upgrades and prevent idle builders.",
                builderIndex = 1,
                buildingName = "Builder Hut",
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
                else -> {}
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
}
