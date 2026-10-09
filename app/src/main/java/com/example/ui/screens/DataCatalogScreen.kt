package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Storage
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BuildingCategory
import com.example.model.BuildingInfo
import com.example.ui.components.AddUpgradeDialog
import com.example.ui.components.CategoryChip
import com.example.ui.components.ResourceBadge
import com.example.ui.components.formatDuration
import com.example.ui.theme.CocGold
import com.example.ui.theme.CocGoldLight
import com.example.viewmodel.CocViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataCatalogScreen(
    viewModel: CocViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val gameData by viewModel.gameData.collectAsState()
    val playerProfile by viewModel.playerProfile.collectAsState()

    var selectedCategory by remember { mutableStateOf<BuildingCategory?>(null) }
    var expandedBuildingId by remember { mutableStateOf<String?>(null) }
    var showJsonSyncSection by remember { mutableStateOf(false) }
    var jsonInputText by remember { mutableStateOf("") }

    var showUpgradeDialogForBuilding by remember { mutableStateOf<BuildingInfo?>(null) }

    val filteredBuildings = if (selectedCategory == null) {
        gameData.buildings
    } else {
        gameData.buildings.filter { it.category == selectedCategory }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("data_catalog_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Database Metadata Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("game_data_header_card"),
                shape = RoundedCornerShape(18.dp),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(CocGold.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DataObject,
                                    contentDescription = null,
                                    tint = CocGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "GAME DATA JSON ENGINE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.2.sp,
                                        color = CocGoldLight
                                    )
                                )
                                Text(
                                    text = "${gameData.gameVersion} (${gameData.buildings.size} structures)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { showJsonSyncSection = !showJsonSyncSection },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("toggle_json_sync_button")
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (showJsonSyncSection) "Hide JSON" else "JSON Sync", fontSize = 11.sp)
                        }
                    }

                    // Expandable JSON Input / Reset section
                    AnimatedVisibility(visible = showJsonSyncSection) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp)
                        ) {
                            Text(
                                text = "Paste updated Clash of Clans JSON schema to sync the latest building costs, upgrade timers, and new Town Hall levels:",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = jsonInputText,
                                onValueChange = { jsonInputText = it },
                                label = { Text("Clash JSON Data") },
                                placeholder = { Text("{\n  \"version\": \"2026.2\",\n  \"buildings\": [...]\n}") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .testTag("custom_json_input"),
                                textStyle = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (jsonInputText.isNotBlank()) {
                                            coroutineScope.launch {
                                                val success = viewModel.importJsonData(jsonInputText)
                                                if (success) {
                                                    Toast.makeText(context, "Game data updated successfully!", Toast.LENGTH_SHORT).show()
                                                    jsonInputText = ""
                                                    showJsonSyncSection = false
                                                } else {
                                                    Toast.makeText(context, "Invalid Clash JSON format", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CocGold),
                                    modifier = Modifier.weight(1f).testTag("import_json_button")
                                ) {
                                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Import JSON", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.resetDefaultJsonData()
                                            Toast.makeText(context, "Reset to default TH16/17 catalog", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f).testTag("reset_json_button")
                                ) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset Official", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Category Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All Categories") }
                    )
                }
                BuildingCategory.values().forEach { cat ->
                    item {
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                            label = { Text(cat.displayName) }
                        )
                    }
                }
            }
        }

        // Buildings Catalog Items
        items(filteredBuildings) { building ->
            val isExpanded = expandedBuildingId == building.id

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("catalog_building_${building.id}"),
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedBuildingId = if (isExpanded) null else building.id
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = building.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryChip(building.category)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Max Level ${building.maxLevel}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = CocGoldLight, fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ResourceBadge(type = building.resourceType, amount = building.levels.firstOrNull()?.cost ?: 0L)
                            IconButton(
                                onClick = {
                                    expandedBuildingId = if (isExpanded) null else building.id
                                }
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Expand"
                                )
                            }
                        }
                    }

                    if (building.description.isNotEmpty()) {
                        Text(
                            text = building.description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    // Level list preview if expanded
                    AnimatedVisibility(visible = isExpanded) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "LEVEL BREAKDOWN & UPGRADE COSTS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CocGold
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            building.levels.forEach { lvl ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Level ${lvl.level} (TH ${lvl.thRequired}+)",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "⏱ ${formatDuration(lvl.timeSeconds)}" + if (lvl.dps > 0) " • ${lvl.dps} DPS" else "",
                                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }

                                    ResourceBadge(type = building.resourceType, amount = lvl.cost)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Quick Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val targetLvl = building.levels.lastOrNull()?.level ?: 2
                                        viewModel.addPriorityItem(
                                            building = building,
                                            currentLevel = (targetLvl - 1).coerceAtLeast(1),
                                            targetLevel = targetLvl,
                                            notes = "Added from Catalog"
                                        )
                                        Toast.makeText(context, "${building.name} added to Priority Backlog!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("+ Add to Priority", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        showUpgradeDialogForBuilding = building
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CocGold),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Upgrade Now", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showUpgradeDialogForBuilding != null) {
        val b = showUpgradeDialogForBuilding!!
        AddUpgradeDialog(
            buildings = listOf(b),
            totalBuilders = playerProfile.totalBuilders,
            initialBuilderIndex = 1,
            onDismiss = { showUpgradeDialogForBuilding = null },
            onStartUpgrade = { selectedB, fromLvl, toLvl, bIdx ->
                viewModel.startCustomUpgrade(selectedB, fromLvl, toLvl, bIdx)
                showUpgradeDialogForBuilding = null
            }
        )
    }
}
