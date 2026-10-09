package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BuildingCategory
import com.example.model.VillageStructure
import com.example.ui.components.CategoryChip
import com.example.ui.components.ResourceBadge
import com.example.ui.components.formatDuration
import com.example.ui.theme.CocDarkElixir
import com.example.ui.theme.CocElixir
import com.example.ui.theme.CocGold
import com.example.ui.theme.CocGoldLight
import com.example.viewmodel.CocViewModel
import com.example.viewmodel.StructureStatusFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StructuresChecklistScreen(
    viewModel: CocViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val structures by viewModel.filteredStructures.collectAsState()
    val allStructures by viewModel.villageStructures.collectAsState()
    val playerProfile by viewModel.playerProfile.collectAsState()
    val gameData by viewModel.gameData.collectAsState()
    val activeUpgrades by viewModel.activeUpgrades.collectAsState()
    val selectedCat by viewModel.selectedCategoryFilter.collectAsState()
    val selectedStatus by viewModel.selectedStatusFilter.collectAsState()

    val boost = playerProfile.goldPassBoostPercent
    val multiplier = (100 - boost) / 100f
    val buildingsMap = gameData.buildings.associateBy { it.id }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("structures_checklist_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("structures_header_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "VILLAGE STRUCTURES CHECKLIST",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp,
                                color = CocGoldLight
                            )
                        )
                        Text(
                            text = "Showing ${structures.size} Structures",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CocGold.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Gold Pass: $boost% Boost",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = CocGold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Category Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCat == null,
                        onClick = { viewModel.setCategoryFilter(null) },
                        label = { Text("All Categories") }
                    )
                }
                listOf(
                    BuildingCategory.DEFENSE,
                    BuildingCategory.HERO,
                    BuildingCategory.ARMY,
                    BuildingCategory.RESOURCE,
                    BuildingCategory.WALL
                ).forEach { cat ->
                    item {
                        FilterChip(
                            selected = selectedCat == cat,
                            onClick = { viewModel.setCategoryFilter(if (selectedCat == cat) null else cat) },
                            label = { Text(cat.displayName) }
                        )
                    }
                }
            }
        }

        // Status Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StructureStatusFilter.values().forEach { st ->
                    item {
                        FilterChip(
                            selected = selectedStatus == st,
                            onClick = { viewModel.setStatusFilter(st) },
                            label = { Text(st.title) }
                        )
                    }
                }
            }
        }

        // Structure Cards
        if (structures.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No buildings matching current filters", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            items(structures) { item ->
                val bInfo = buildingsMap[item.buildingId]
                val nextLvl = (item.currentLevel + 1).coerceAtMost(item.maxLevelForTH)
                val nextLvlInfo = bInfo?.getLevel(nextLvl)

                val discountedCost = if (nextLvlInfo != null) (nextLvlInfo.cost * multiplier).toLong() else 0L
                val discountedTime = if (nextLvlInfo != null) (nextLvlInfo.timeSeconds * multiplier).toLong() else 0L

                val isMaxed = item.currentLevel >= item.maxLevelForTH

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("structure_item_${item.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.isUpgrading) Color(0x2200E5FF) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CategoryChip(item.category)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Lv ${item.currentLevel} / ${item.maxLevelForTH} (TH${playerProfile.townHallLevel} Max)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isMaxed) Color(0xFF00E676) else CocGoldLight,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            // Quick [-] and [+] level adjuster
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.size(32.dp).clickable {
                                        if (item.currentLevel > 1) {
                                            viewModel.updateStructureLevel(item.id, item.currentLevel - 1)
                                        }
                                    }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease Level", modifier = Modifier.size(16.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.size(32.dp).clickable {
                                        if (item.currentLevel < item.absoluteMaxLevel) {
                                            viewModel.updateStructureLevel(item.id, item.currentLevel + 1)
                                        }
                                    }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase Level", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Status & Upgrade Details
                        when {
                            item.isUpgrading -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0x2200E5FF), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.HourglassTop, contentDescription = null, tint = CocDarkElixir, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "UPGRADING TO LV $nextLvl (Builder active)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = CocDarkElixir)
                                    )
                                }
                            }
                            isMaxed -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0x2200E676), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "MAXED FOR TOWN HALL ${playerProfile.townHallLevel}!",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
                                    )
                                }
                            }
                            else -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Next: Lv $nextLvl", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            ResourceBadge(type = item.resourceType, amount = discountedCost)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "⏱ ${formatDuration(discountedTime)}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = CocGold)
                                            )
                                        }
                                    }

                                    Row {
                                        OutlinedButton(
                                            onClick = {
                                                if (bInfo != null) {
                                                    viewModel.addPriorityItem(
                                                        building = bInfo,
                                                        currentLevel = item.currentLevel,
                                                        targetLevel = nextLvl,
                                                        notes = "Planned for ${item.name}"
                                                    )
                                                    Toast.makeText(context, "Added to Planner Backlog", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("+ Plan", fontSize = 11.sp)
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        Button(
                                            onClick = {
                                                val freeBuilder = (1..playerProfile.totalBuilders).firstOrNull { idx ->
                                                    activeUpgrades.none { it.builderIndex == idx && !it.isCompleted }
                                                } ?: 1

                                                viewModel.startUpgradeFromStructure(item, builderIndex = freeBuilder)
                                                Toast.makeText(context, "Upgrade started on Builder #$freeBuilder!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = CocGold),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.testTag("start_upgrade_${item.id}")
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Upgrade", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
