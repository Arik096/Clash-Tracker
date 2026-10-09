package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CocDataRepository
import com.example.model.BuildingCategory
import com.example.model.BuildingInfo
import com.example.model.PlayerProfile
import com.example.model.PriorityItem
import com.example.model.ResourceType
import com.example.model.UpgradeTask
import com.example.model.VillageProgressStats
import com.example.model.VillageStructure
import com.example.notification.NotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class StructureStatusFilter(val title: String) {
    ALL("All Structures"),
    NEEDS_UPGRADE("Needs Upgrade"),
    UPGRADING("Upgrading Now"),
    MAXED("Maxed for TH")
}

enum class PrioritySortOption(val title: String) {
    RANK("Priority Rank"),
    COST_LOW_TO_HIGH("Resource Cost (Low → High)"),
    COST_HIGH_TO_LOW("Resource Cost (High → Low)"),
    TIME_FASTEST("Upgrade Time (Shortest)"),
    TIME_LONGEST("Upgrade Time (Longest)")
}

class CocViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CocDataRepository(application)
    private val notificationHelper = NotificationHelper(application)

    val gameData = repository.gameData
    val playerProfile = repository.playerProfile

    private val _currentTimeMillis = MutableStateFlow(System.currentTimeMillis())
    val currentTimeMillis: StateFlow<Long> = _currentTimeMillis.asStateFlow()

    val activeUpgrades = repository.activeUpgrades.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val villageStructures = repository.villageStructures.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val villageProgress: StateFlow<VillageProgressStats> = combine(
        villageStructures,
        playerProfile
    ) { structures, profile ->
        repository.calculateVillageProgress(structures, profile)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        VillageProgressStats()
    )

    // Structure category & status filtering for Village Checklist
    private val _selectedCategoryFilter = MutableStateFlow<BuildingCategory?>(null)
    val selectedCategoryFilter = _selectedCategoryFilter.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow(StructureStatusFilter.ALL)
    val selectedStatusFilter = _selectedStatusFilter.asStateFlow()

    val filteredStructures: StateFlow<List<VillageStructure>> = combine(
        villageStructures,
        _selectedCategoryFilter,
        _selectedStatusFilter
    ) { list, catFilter, statusFilter ->
        var res = list
        if (catFilter != null) {
            res = res.filter { it.category == catFilter }
        }
        when (statusFilter) {
            StructureStatusFilter.ALL -> res
            StructureStatusFilter.NEEDS_UPGRADE -> res.filter { it.currentLevel < it.maxLevelForTH && !it.isUpgrading }
            StructureStatusFilter.UPGRADING -> res.filter { it.isUpgrading }
            StructureStatusFilter.MAXED -> res.filter { it.currentLevel >= it.maxLevelForTH }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Priority queue
    private val _prioritySort = MutableStateFlow(PrioritySortOption.COST_LOW_TO_HIGH)
    val prioritySort = _prioritySort.asStateFlow()

    private val _resourceFilter = MutableStateFlow<ResourceType?>(null)
    val resourceFilter = _resourceFilter.asStateFlow()

    private val _categoryFilter = MutableStateFlow<BuildingCategory?>(null)
    val categoryFilter = _categoryFilter.asStateFlow()

    val priorities: StateFlow<List<PriorityItem>> = combine(
        repository.allPriorities,
        _prioritySort,
        _resourceFilter,
        _categoryFilter
    ) { list, sort, resFilter, catFilter ->
        var filtered = list
        if (resFilter != null) {
            filtered = filtered.filter { it.resourceType == resFilter }
        }
        if (catFilter != null) {
            filtered = filtered.filter { it.category == catFilter }
        }
        when (sort) {
            PrioritySortOption.RANK -> filtered.sortedBy { it.priorityRank }
            PrioritySortOption.COST_LOW_TO_HIGH -> filtered.sortedBy { it.cost }
            PrioritySortOption.COST_HIGH_TO_LOW -> filtered.sortedByDescending { it.cost }
            PrioritySortOption.TIME_FASTEST -> filtered.sortedBy { it.durationSeconds }
            PrioritySortOption.TIME_LONGEST -> filtered.sortedByDescending { it.durationSeconds }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val targetPlanSummary: StateFlow<CocDataRepository.TargetPlanSummary> = combine(
        repository.allPriorities,
        playerProfile
    ) { items, profile ->
        repository.calculateTargetSummary(items, profile.totalBuilders)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        repository.calculateTargetSummary(emptyList(), 6)
    )

    val loginAdvice: StateFlow<CocDataRepository.LoginAdvice> = combine(
        activeUpgrades,
        _currentTimeMillis
    ) { upgrades, _ ->
        repository.calculateNextLoginAdvice(upgrades)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        repository.calculateNextLoginAdvice(emptyList())
    )

    // For Loot Calculator screen
    private val _selectedCalculatorBuildingId = MutableStateFlow<String?>("eagle_artillery")
    val selectedCalculatorBuildingId = _selectedCalculatorBuildingId.asStateFlow()

    private val _selectedCalculatorTargetLevel = MutableStateFlow<Int>(6)
    val selectedCalculatorTargetLevel = _selectedCalculatorTargetLevel.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initialize()
        }

        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _currentTimeMillis.value = System.currentTimeMillis()
            }
        }
    }

    fun setCategoryFilter(category: BuildingCategory?) {
        _selectedCategoryFilter.value = category
    }

    fun setStatusFilter(filter: StructureStatusFilter) {
        _selectedStatusFilter.value = filter
    }

    fun setPrioritySort(option: PrioritySortOption) {
        _prioritySort.value = option
    }

    fun setResourceFilter(type: ResourceType?) {
        _resourceFilter.value = type
    }

    fun setGoldPassBoost(percent: Int) {
        val updated = playerProfile.value.copy(goldPassBoostPercent = percent)
        repository.saveProfile(updated)
    }

    fun setTownHallLevel(th: Int) {
        val updated = playerProfile.value.copy(townHallLevel = th)
        repository.saveProfile(updated)
        viewModelScope.launch {
            repository.seedVillageStructuresForTownHall(th, setPreviousThMax = false)
        }
    }

    fun setAllToPreviousTownHallMax() {
        viewModelScope.launch {
            repository.setAllToPreviousTownHallMax(playerProfile.value.townHallLevel)
        }
    }

    fun updateStructureLevel(id: String, newLevel: Int) {
        viewModelScope.launch {
            repository.updateStructureLevel(id, newLevel)
        }
    }

    fun selectCalculatorBuilding(buildingId: String, targetLevel: Int) {
        _selectedCalculatorBuildingId.value = buildingId
        _selectedCalculatorTargetLevel.value = targetLevel
    }

    fun startUpgradeFromStructure(structure: VillageStructure, builderIndex: Int) {
        viewModelScope.launch {
            val building = gameData.value.buildings.find { it.id == structure.buildingId } ?: return@launch
            val nextLvl = (structure.currentLevel + 1).coerceAtMost(structure.maxLevelForTH)
            val lvlInfo = building.getLevel(nextLvl)

            val boost = playerProfile.value.goldPassBoostPercent
            val mult = (100 - boost) / 100f

            val origDuration = lvlInfo?.timeSeconds ?: 86400L
            val durationSecs = (origDuration * mult).toLong()
            val origCost = lvlInfo?.cost ?: 1_000_000L
            val cost = (origCost * mult).toLong()

            val now = System.currentTimeMillis()
            val endMillis = now + (durationSecs * 1000L)

            val task = UpgradeTask(
                structureId = structure.id,
                buildingId = building.id,
                buildingName = structure.name,
                category = structure.category,
                fromLevel = structure.currentLevel,
                toLevel = nextLvl,
                resourceType = structure.resourceType,
                originalCost = origCost,
                cost = cost,
                originalDurationSeconds = origDuration,
                totalDurationSeconds = durationSecs,
                startTimeMillis = now,
                endTimeMillis = endMillis,
                builderIndex = builderIndex,
                note = "Clash Ninja Tracker Upgrade"
            )

            repository.startUpgrade(task)
        }
    }

    fun startCustomUpgrade(
        building: BuildingInfo,
        fromLevel: Int,
        toLevel: Int,
        builderIndex: Int
    ) {
        viewModelScope.launch {
            val lvlInfo = building.getLevel(toLevel)
            val boost = playerProfile.value.goldPassBoostPercent
            val mult = (100 - boost) / 100f

            val origDuration = lvlInfo?.timeSeconds ?: 86400L
            val durationSecs = (origDuration * mult).toLong()
            val origCost = lvlInfo?.cost ?: 1_000_000L
            val cost = (origCost * mult).toLong()

            val now = System.currentTimeMillis()
            val endMillis = now + (durationSecs * 1000L)

            val task = UpgradeTask(
                buildingId = building.id,
                buildingName = building.name,
                category = building.category,
                fromLevel = fromLevel,
                toLevel = toLevel,
                resourceType = building.resourceType,
                originalCost = origCost,
                cost = cost,
                originalDurationSeconds = origDuration,
                totalDurationSeconds = durationSecs,
                startTimeMillis = now,
                endTimeMillis = endMillis,
                builderIndex = builderIndex
            )

            repository.startUpgrade(task)
        }
    }

    fun completeUpgrade(taskId: String) {
        viewModelScope.launch {
            repository.completeUpgrade(taskId)
        }
    }

    fun deleteUpgrade(taskId: String) {
        viewModelScope.launch {
            repository.deleteUpgrade(taskId)
        }
    }

    fun boostBuilder(taskId: String) {
        viewModelScope.launch {
            repository.applyBuilderPotion(taskId, boostMultiplier = 10, durationHours = 1)
        }
    }

    fun addPriorityItem(
        building: BuildingInfo,
        currentLevel: Int,
        targetLevel: Int,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val levelInfo = building.getLevel(targetLevel)
            val boost = playerProfile.value.goldPassBoostPercent
            val mult = (100 - boost) / 100f
            val cost = ((levelInfo?.cost ?: 1_000_000L) * mult).toLong()
            val durationSecs = ((levelInfo?.timeSeconds ?: 86400L) * mult).toLong()

            val item = PriorityItem(
                buildingId = building.id,
                buildingName = building.name,
                category = building.category,
                currentLevel = currentLevel,
                targetLevel = targetLevel,
                resourceType = building.resourceType,
                cost = cost,
                durationSeconds = durationSecs,
                priorityRank = priorities.value.size + 1,
                notes = notes
            )
            repository.addPriorityItem(item)
        }
    }

    fun removePriorityItem(id: String) {
        viewModelScope.launch {
            repository.removePriorityItem(id)
        }
    }

    fun assignPriorityToBuilder(item: PriorityItem, builderIndex: Int) {
        viewModelScope.launch {
            val building = gameData.value.buildings.find { it.id == item.buildingId }
                ?: BuildingInfo(
                    id = item.buildingId,
                    name = item.buildingName,
                    category = item.category,
                    resourceType = item.resourceType
                )
            startCustomUpgrade(
                building = building,
                fromLevel = item.currentLevel,
                toLevel = item.targetLevel,
                builderIndex = builderIndex
            )
            repository.removePriorityItem(item.id)
        }
    }

    fun updateProfile(profile: PlayerProfile) {
        repository.saveProfile(profile)
    }

    fun calculateShortage(cost: Long, resourceType: ResourceType): CocDataRepository.LootShortageResult {
        return repository.calculateShortage(cost, resourceType, playerProfile.value)
    }

    suspend fun importJsonData(json: String): Boolean {
        return repository.importGameDataJson(json)
    }

    suspend fun resetDefaultJsonData() {
        repository.loadDefaultAssetGameData()
    }

    fun triggerTestNotification() {
        notificationHelper.showTestNotification()
    }
}
