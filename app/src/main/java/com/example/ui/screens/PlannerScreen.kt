package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BuildingCategory
import com.example.model.PriorityItem
import com.example.model.ResourceType
import com.example.ui.components.AddPriorityDialog
import com.example.ui.components.CategoryChip
import com.example.ui.components.ResourceBadge
import com.example.ui.components.formatDuration
import com.example.ui.components.formatResourceAmount
import com.example.ui.theme.CocDarkElixir
import com.example.ui.theme.CocElixir
import com.example.ui.theme.CocGold
import com.example.ui.theme.CocGoldLight
import com.example.viewmodel.CocViewModel
import com.example.viewmodel.PrioritySortOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    viewModel: CocViewModel,
    onNavigateToLootCalc: (buildingId: String, targetLevel: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val priorities by viewModel.priorities.collectAsState()
    val targetSummary by viewModel.targetPlanSummary.collectAsState()
    val playerProfile by viewModel.playerProfile.collectAsState()
    val gameData by viewModel.gameData.collectAsState()
    val currentSort by viewModel.prioritySort.collectAsState()
    val resourceFilter by viewModel.resourceFilter.collectAsState()
    val categoryFilter by viewModel.categoryFilter.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = CocGold,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_priority_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Target")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Target", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("priority_planner_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Target Completion Forecast Summary Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("target_summary_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(CocGold.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = CocGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "TARGET COMPLETION FORECAST",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 1.2.sp,
                                            color = CocGoldLight
                                        )
                                    )
                                    Text(
                                        text = "Target Date: ${targetSummary.estimatedCompletionDate}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stats Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Queue Size", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${targetSummary.totalItems} Buildings", fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Builder Work", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    String.format(java.util.Locale.US, "%.1f Days", targetSummary.builderDaysNeeded),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text("With ${playerProfile.totalBuilders} Builders", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    String.format(java.util.Locale.US, "%.1f Days", targetSummary.calendarDaysWithBuilders),
                                    fontWeight = FontWeight.Bold,
                                    color = CocGold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Total Resource Needs Badges
                        Text(
                            text = "Total Resource Budget Required:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (targetSummary.totalGoldCost > 0) {
                                ResourceBadge(
                                    type = ResourceType.GOLD,
                                    amount = targetSummary.totalGoldCost,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (targetSummary.totalElixirCost > 0) {
                                ResourceBadge(
                                    type = ResourceType.ELIXIR,
                                    amount = targetSummary.totalElixirCost,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (targetSummary.totalDarkElixirCost > 0) {
                                ResourceBadge(
                                    type = ResourceType.DARK_ELIXIR,
                                    amount = targetSummary.totalDarkElixirCost,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Controls: Sort & Resource Filters
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PRIORITY BACKLOG",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp,
                                color = CocGold
                            )
                        )

                        // Sort dropdown trigger
                        Box {
                            OutlinedButton(
                                onClick = { showSortMenu = true },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("sort_priority_button")
                            ) {
                                Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when (currentSort) {
                                        PrioritySortOption.COST_LOW_TO_HIGH -> "Cost: Low to High"
                                        PrioritySortOption.COST_HIGH_TO_LOW -> "Cost: High to Low"
                                        PrioritySortOption.TIME_FASTEST -> "Fastest Time"
                                        PrioritySortOption.TIME_LONGEST -> "Longest Time"
                                        PrioritySortOption.RANK -> "Rank"
                                    },
                                    fontSize = 12.sp
                                )
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                PrioritySortOption.values().forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.title) },
                                        onClick = {
                                            viewModel.setPrioritySort(option)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Resource Filter Chips Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = resourceFilter == null,
                                onClick = { viewModel.setResourceFilter(null) },
                                label = { Text("All Resources") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = resourceFilter == ResourceType.GOLD,
                                onClick = {
                                    viewModel.setResourceFilter(if (resourceFilter == ResourceType.GOLD) null else ResourceType.GOLD)
                                },
                                label = { Text("Gold Only") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CocGold.copy(alpha = 0.25f)
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = resourceFilter == ResourceType.ELIXIR,
                                onClick = {
                                    viewModel.setResourceFilter(if (resourceFilter == ResourceType.ELIXIR) null else ResourceType.ELIXIR)
                                },
                                label = { Text("Elixir Only") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CocElixir.copy(alpha = 0.25f)
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = resourceFilter == ResourceType.DARK_ELIXIR,
                                onClick = {
                                    viewModel.setResourceFilter(if (resourceFilter == ResourceType.DARK_ELIXIR) null else ResourceType.DARK_ELIXIR)
                                },
                                label = { Text("Dark Elixir") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CocDarkElixir.copy(alpha = 0.25f)
                                )
                            )
                        }
                    }
                }
            }

            // Priority Items List
            if (priorities.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No targets matching filter",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap '+ Add Target' below to queue buildings prioritized by resource cost or upgrade duration.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(priorities) { item ->
                    val shortageResult = viewModel.calculateShortage(item.cost, item.resourceType)

                    PriorityItemCard(
                        item = item,
                        shortage = shortageResult,
                        onAssign = {
                            viewModel.assignPriorityToBuilder(item, builderIndex = 1)
                        },
                        onDelete = { viewModel.removePriorityItem(item.id) },
                        onInspectLoot = {
                            onNavigateToLootCalc(item.buildingId, item.targetLevel)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    if (showAddDialog) {
        AddPriorityDialog(
            buildings = gameData.buildings,
            onDismiss = { showAddDialog = false },
            onAddPriority = { building, curLvl, tgtLvl, notes ->
                viewModel.addPriorityItem(building, curLvl, tgtLvl, notes)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun PriorityItemCard(
    item: PriorityItem,
    shortage: com.example.data.CocDataRepository.LootShortageResult,
    onAssign: () -> Unit,
    onDelete: () -> Unit,
    onInspectLoot: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("priority_item_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = item.buildingName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryChip(item.category)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Lv ${item.currentLevel} → Lv ${item.targetLevel}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = CocGoldLight
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "⏱ ${formatDuration(item.durationSeconds)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                ResourceBadge(type = item.resourceType, amount = item.cost)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Shortage indicator row
            if (shortage.isReady) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x2200E676), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ready to start! You have full loot required.",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E676)
                        )
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x22FF9800), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Need +${formatResourceAmount(shortage.shortage)} extra ${shortage.resourceType.displayName}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF9800)
                                )
                            )
                        }
                        Text(
                            text = "~${shortage.raidsNeeded} attacks needed",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = CocGoldLight
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { shortage.percentOwned },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFFFF9800),
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                }
            }

            if (item.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Goal Note: ${item.notes}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onInspectLoot,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Loot Calculator", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onAssign,
                    colors = ButtonDefaults.buttonColors(containerColor = CocGold),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("assign_priority_${item.id}_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Assign Builder",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
