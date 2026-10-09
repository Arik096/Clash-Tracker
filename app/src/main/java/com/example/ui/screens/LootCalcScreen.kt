package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BuildingInfo
import com.example.model.PlayerProfile
import com.example.model.ResourceType
import com.example.ui.components.CategoryChip
import com.example.ui.components.ResourceBadge
import com.example.ui.components.formatDuration
import com.example.ui.components.formatFullResourceAmount
import com.example.ui.components.formatResourceAmount
import com.example.ui.theme.CocDarkElixir
import com.example.ui.theme.CocElixir
import com.example.ui.theme.CocGold
import com.example.ui.theme.CocGoldLight
import com.example.viewmodel.CocViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LootCalcScreen(
    viewModel: CocViewModel,
    modifier: Modifier = Modifier
) {
    val playerProfile by viewModel.playerProfile.collectAsState()
    val gameData by viewModel.gameData.collectAsState()
    val selectedBuildingId by viewModel.selectedCalculatorBuildingId.collectAsState()
    val selectedTargetLevel by viewModel.selectedCalculatorTargetLevel.collectAsState()

    var showBalanceEditor by remember { mutableStateOf(false) }

    // Find current selected building
    val selectedBuilding = gameData.buildings.find { it.id == selectedBuildingId }
        ?: gameData.buildings.firstOrNull()
        ?: BuildingInfo(name = "Eagle Artillery")

    val levelInfo = selectedBuilding.getLevel(selectedTargetLevel)
    val cost = levelInfo?.cost ?: 0L
    val resourceType = selectedBuilding.resourceType

    val shortageResult = viewModel.calculateShortage(cost, resourceType)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("loot_calc_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Player Current Resources Summary Card (with Edit trigger)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("player_loot_balances_card"),
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
                                    imageVector = Icons.Default.Savings,
                                    contentDescription = null,
                                    tint = CocGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "CURRENT VILLAGE LOOT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.2.sp,
                                    color = CocGoldLight
                                )
                            )
                        }

                        OutlinedButton(
                            onClick = { showBalanceEditor = !showBalanceEditor },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("toggle_balance_editor_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (showBalanceEditor) "Done" else "Update Balances", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!showBalanceEditor) {
                        // Display balances row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            LootBalanceTile(
                                label = "Gold",
                                current = playerProfile.currentGold,
                                max = playerProfile.maxGoldStorage,
                                type = ResourceType.GOLD,
                                modifier = Modifier.weight(1f)
                            )
                            LootBalanceTile(
                                label = "Elixir",
                                current = playerProfile.currentElixir,
                                max = playerProfile.maxElixirStorage,
                                type = ResourceType.ELIXIR,
                                modifier = Modifier.weight(1f)
                            )
                            LootBalanceTile(
                                label = "Dark Elixir",
                                current = playerProfile.currentDarkElixir,
                                max = playerProfile.maxDarkElixirStorage,
                                type = ResourceType.DARK_ELIXIR,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        // Inline editor
                        BalanceEditor(
                            profile = playerProfile,
                            onSave = { updated ->
                                viewModel.updateProfile(updated)
                                showBalanceEditor = false
                            }
                        )
                    }
                }
            }
        }

        // Target Upgrade Selector
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upgrade_target_selector_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "SELECT UPGRADE TO CALCULATE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp,
                            color = CocGold
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    var expandedDropdown by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = !expandedDropdown },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = "${selectedBuilding.name} (Lv $selectedTargetLevel)",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("calculator_building_dropdown")
                        )

                        ExposedDropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            gameData.buildings.forEach { b ->
                                val defaultLvl = b.levels.lastOrNull()?.level ?: 1
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(b.name, fontWeight = FontWeight.SemiBold)
                                            Text("Max Lv ${b.maxLevel}", color = CocGold)
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectCalculatorBuilding(b.id, defaultLvl)
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Selected Building Summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryChip(selectedBuilding.category)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Target Level: Lv $selectedTargetLevel",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            if (levelInfo != null) {
                                Text(
                                    text = "Upgrade Time: ${formatDuration(levelInfo.timeSeconds)}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }

                        ResourceBadge(type = resourceType, amount = cost, useShortFormat = false)
                    }
                }
            }
        }

        // Shortage Calculation & Progress Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("shortage_result_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (shortageResult.isReady) Color(0x2200E676) else Color(0x22FF9800)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (shortageResult.isReady) "READY FOR UPGRADE! ⚔️" else "EXTRA LOOT NEEDED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.2.sp,
                                    color = if (shortageResult.isReady) Color(0xFF00E676) else Color(0xFFFF9800)
                                )
                            )
                            Text(
                                text = if (shortageResult.isReady) {
                                    "0 Extra Loot Required"
                                } else {
                                    "+${formatFullResourceAmount(shortageResult.shortage)} ${resourceType.displayName}"
                                },
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (shortageResult.isReady) Color(0xFF00E676) else Color(0xFFFF9800)
                                )
                            )
                        }

                        // Percentage Circle / Text
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${(shortageResult.percentOwned * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CocGold
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { shortageResult.percentOwned },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = if (shortageResult.isReady) Color(0xFF00E676) else Color(0xFFFF9800),
                        trackColor = MaterialTheme.colorScheme.surface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Current: ${formatFullResourceAmount(shortageResult.currentLoot)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Required: ${formatFullResourceAmount(cost)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Time to Reach Target Calculator
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("time_to_reach_target_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(CocGold.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = null,
                                tint = CocGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "TIME TO REACH TARGET",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp,
                                color = CocGoldLight
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Strategy 1: Active Multiplayer Raiding
                    StrategyRow(
                        icon = Icons.Default.MilitaryTech,
                        iconTint = CocGold,
                        title = "Active Raiding Route",
                        subtitle = if (shortageResult.isReady) {
                            "You are already ready! No attacks needed."
                        } else {
                            "~${shortageResult.raidsNeeded} Multiplayer Attacks (at ~${formatResourceAmount(playerProfile.avgLootPerRaidGold)} per raid)\nEstimated Time: ~${(shortageResult.raidsNeeded * 6)} minutes of gameplay"
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Strategy 2: Passive Collector Generation
                    val passiveHours = shortageResult.passiveHoursNeeded
                    val passiveFormatted = when {
                        shortageResult.isReady -> "Immediate"
                        passiveHours > 24 -> String.format(java.util.Locale.US, "%.1f Days (%.0f Hours)", passiveHours / 24, passiveHours)
                        else -> String.format(java.util.Locale.US, "%.1f Hours", passiveHours)
                    }

                    StrategyRow(
                        icon = Icons.Default.HourglassBottom,
                        iconTint = CocDarkElixir,
                        title = "Passive Mines & Drills Route",
                        subtitle = if (shortageResult.isReady) {
                            "No passive farming needed."
                        } else {
                            "$passiveFormatted of passive collector production without attacking\nRate: ~${formatResourceAmount(playerProfile.hourlyCollectorGold)}/hr from village mines"
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Strategy 3: Hybrid Strategy
                    StrategyRow(
                        icon = Icons.Default.Speed,
                        iconTint = CocElixir,
                        title = "Recommended Fast Route",
                        subtitle = if (shortageResult.isReady) {
                            "Assign a free builder now to start upgrading immediately!"
                        } else {
                            val hybridAttacks = (shortageResult.raidsNeeded / 2).coerceAtLeast(1)
                            "Do $hybridAttacks quick attacks today + collect overnight to hit your target by tomorrow morning!"
                        }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun LootBalanceTile(
    label: String,
    current: Long,
    max: Long,
    type: ResourceType,
    modifier: Modifier = Modifier
) {
    val color = when (type) {
        ResourceType.GOLD -> CocGold
        ResourceType.ELIXIR -> CocElixir
        ResourceType.DARK_ELIXIR -> CocDarkElixir
        ResourceType.ORE -> Color(0xFF64B5F6)
        ResourceType.GEMS -> Color(0xFF00E676)
    }

    val fraction = if (max > 0) (current.toFloat() / max.toFloat()).coerceIn(0f, 1f) else 0f

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = color)
        )
        Text(
            text = formatResourceAmount(current),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
        )
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Text(
            text = "Cap: ${formatResourceAmount(max)}",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun BalanceEditor(
    profile: PlayerProfile,
    onSave: (PlayerProfile) -> Unit
) {
    var goldText by remember { mutableStateOf(profile.currentGold.toString()) }
    var elixirText by remember { mutableStateOf(profile.currentElixir.toString()) }
    var darkText by remember { mutableStateOf(profile.currentDarkElixir.toString()) }

    Column {
        OutlinedTextField(
            value = goldText,
            onValueChange = { goldText = it },
            label = { Text("Current Gold") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag("edit_gold_input"),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = elixirText,
            onValueChange = { elixirText = it },
            label = { Text("Current Elixir") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag("edit_elixir_input"),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = darkText,
            onValueChange = { darkText = it },
            label = { Text("Current Dark Elixir") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag("edit_dark_input"),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = {
                val updated = profile.copy(
                    currentGold = goldText.toLongOrNull() ?: profile.currentGold,
                    currentElixir = elixirText.toLongOrNull() ?: profile.currentElixir,
                    currentDarkElixir = darkText.toLongOrNull() ?: profile.currentDarkElixir
                )
                onSave(updated)
            },
            colors = ButtonDefaults.buttonColors(containerColor = CocGold),
            modifier = Modifier.fillMaxWidth().testTag("save_balances_button")
        ) {
            Text("Save Balances", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StrategyRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(iconTint.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            )
        }
    }
}
