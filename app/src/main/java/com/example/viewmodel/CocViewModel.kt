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

    // 1-second ticker to update real-time remaining countdowns
    private val _currentTimeMillis = MutableStateFlow(System.currentTimeMillis())
    val currentTimeMillis: StateFlow<Long> = _currentTimeMillis.asStateFlow()

    val activeUpgrades = repository.activeUpgrades.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

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

    val targetPlanSummary: StateFlow<CocDataRepository.TargetPlanSummary> = combine(
        repository.allPriorities,
        playerProfile
    ) { items, profile ->
        repository.calculateTargetSummary(items, profile.totalBuilders)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        repository.calculateTargetSummary(emptyList(), 5)
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

        // Ticker loop
        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _currentTimeMillis.value = System.currentTimeMillis()
            }
        }
    }

    fun setPrioritySort(option: PrioritySortOption) {
        _prioritySort.value = option
    }

    fun setResourceFilter(type: ResourceType?) {
        _resourceFilter.value = type
    }

    fun setCategoryFilter(cat: BuildingCategory?) {
        _categoryFilter.value = cat
    }

    fun selectCalculatorBuilding(buildingId: String, targetLevel: Int) {
        _selectedCalculatorBuildingId.value = buildingId
        _selectedCalculatorTargetLevel.value = targetLevel
    }

    fun startUpgrade(
        building: BuildingInfo,
        fromLevel: Int,
        toLevel: Int,
        builderIndex: Int,
        customDurationSeconds: Long? = null,
        note: String = ""
    ) {
        viewModelScope.launch {
            val levelInfo = building.getLevel(toLevel)
            val durationSecs = customDurationSeconds ?: (levelInfo?.timeSeconds ?: 3600L)
            val cost = levelInfo?.cost ?: 0L
            val now = System.currentTimeMillis()
            val endMillis = now + (durationSecs * 1000L)

            val task = UpgradeTask(
                buildingId = building.id,
                buildingName = building.name,
                category = building.category,
                fromLevel = fromLevel,
                toLevel = toLevel,
                resourceType = building.resourceType,
                cost = cost,
                startTimeMillis = now,
                endTimeMillis = endMillis,
                totalDurationSeconds = durationSecs,
                builderIndex = builderIndex,
                note = note
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
            val item = PriorityItem(
                buildingId = building.id,
                buildingName = building.name,
                category = building.category,
                currentLevel = currentLevel,
                targetLevel = targetLevel,
                resourceType = building.resourceType,
                cost = levelInfo?.cost ?: 1_000_000L,
                durationSeconds = levelInfo?.timeSeconds ?: 86400L,
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
            startUpgrade(
                building = building,
                fromLevel = item.currentLevel,
                toLevel = item.targetLevel,
                builderIndex = builderIndex,
                customDurationSeconds = item.durationSeconds,
                note = item.notes
            )
            // Remove from priority queue once started
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
