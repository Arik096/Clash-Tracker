package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.BuildingInfo
import com.example.ui.theme.CocGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUpgradeDialog(
    buildings: List<BuildingInfo>,
    totalBuilders: Int,
    initialBuilderIndex: Int = 1,
    onDismiss: () -> Unit,
    onStartUpgrade: (building: BuildingInfo, fromLevel: Int, toLevel: Int, builderIndex: Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedBuilding by remember {
        mutableStateOf(buildings.firstOrNull() ?: BuildingInfo(name = "Cannon"))
    }
    var selectedBuilderIndex by remember { mutableIntStateOf(initialBuilderIndex) }
    var targetLevel by remember {
        mutableIntStateOf(
            selectedBuilding.levels.firstOrNull()?.level ?: 1
        )
    }

    val currentLevelInfo = selectedBuilding.getLevel(targetLevel)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("add_upgrade_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Start Builder Upgrade 🔨",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = CocGold
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Builder Selection Row
                Text(
                    text = "Assign to Builder:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (i in 1..totalBuilders) {
                        val isSelected = selectedBuilderIndex == i
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) CocGold else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedBuilderIndex = i }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "B#$i",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search building
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search building / defense...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_building_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Building selection list
                val filteredBuildings = buildings.filter {
                    it.name.contains(searchQuery, ignoreCase = true)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                ) {
                    items(filteredBuildings) { building ->
                        val isSelected = building.id == selectedBuilding.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    selectedBuilding = building
                                    targetLevel = (building.levels.firstOrNull()?.level ?: 1)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) CocGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = building.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    CategoryChip(building.category)
                                }
                                ResourceBadge(
                                    type = building.resourceType,
                                    amount = building.levels.firstOrNull()?.cost ?: 0L
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Level Selector
                Text(
                    text = "Target Level: Lv $targetLevel",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )

                val availableLevels = selectedBuilding.levels.map { it.level }
                if (availableLevels.size > 1) {
                    val minLevel = availableLevels.minOrNull() ?: 1
                    val maxLevel = availableLevels.maxOrNull() ?: 16
                    Slider(
                        value = targetLevel.toFloat(),
                        onValueChange = { targetLevel = it.toInt() },
                        valueRange = minLevel.toFloat()..maxLevel.toFloat(),
                        steps = (maxLevel - minLevel - 1).coerceAtLeast(0),
                        modifier = Modifier.fillMaxWidth().testTag("level_slider")
                    )
                }

                // Cost and Duration details
                if (currentLevelInfo != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Upgrade Cost", style = MaterialTheme.typography.labelSmall)
                                ResourceBadge(
                                    type = selectedBuilding.resourceType,
                                    amount = currentLevelInfo.cost,
                                    useShortFormat = false
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Time Required", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = formatDuration(currentLevelInfo.timeSeconds),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = CocGold
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val fromLevel = (targetLevel - 1).coerceAtLeast(1)
                            onStartUpgrade(selectedBuilding, fromLevel, targetLevel, selectedBuilderIndex)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CocGold),
                        modifier = Modifier.testTag("confirm_start_upgrade_button")
                    ) {
                        Text(
                            text = "Start Upgrade",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
