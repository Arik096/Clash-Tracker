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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ResourceType
import com.example.model.UpgradeTask
import com.example.ui.components.AddUpgradeDialog
import com.example.ui.components.CategoryChip
import com.example.ui.components.CategoryProgressBar
import com.example.ui.components.ClashNinjaProgressRing
import com.example.ui.components.LoginAdvisorCard
import com.example.ui.components.ResourceBadge
import com.example.ui.components.formatRemainingMillis
import com.example.ui.components.formatResourceAmount
import com.example.ui.theme.CocDarkElixir
import com.example.ui.theme.CocElixir
import com.example.ui.theme.CocGold
import com.example.ui.theme.CocGoldLight
import com.example.viewmodel.CocViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradesScreen(
    viewModel: CocViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeUpgrades by viewModel.activeUpgrades.collectAsState()
    val playerProfile by viewModel.playerProfile.collectAsState()
    val villageProgress by viewModel.villageProgress.collectAsState()
    val gameData by viewModel.gameData.collectAsState()
    val loginAdvice by viewModel.loginAdvice.collectAsState()
    val currentTimeMillis by viewModel.currentTimeMillis.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var dialogInitialBuilderIndex by remember { mutableIntStateOf(1) }
    var showThMenu by remember { mutableStateOf(false) }

    val totalBuilders = playerProfile.totalBuilders

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val firstFree = (1..totalBuilders).firstOrNull { idx ->
                        activeUpgrades.none { it.builderIndex == idx && !it.isCompleted }
                    } ?: 1
                    dialogInitialBuilderIndex = firstFree
                    showAddDialog = true
                },
                containerColor = CocGold,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_upgrade_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Start Upgrade")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start Upgrade", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("upgrades_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Clash Ninja Top Control Bar: Town Hall + Gold Pass Season Boost (0, 10, 15, 20%)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("clash_ninja_top_bar"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Town Hall Selector Dropdown
                            Box {
                                OutlinedButton(
                                    onClick = { showThMenu = true },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("th_selector_button")
                                ) {
                                    Icon(Icons.Default.Shield, contentDescription = null, tint = CocGold, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Town Hall ${playerProfile.townHallLevel}", fontWeight = FontWeight.Bold)
                                }

                                DropdownMenu(
                                    expanded = showThMenu,
                                    onDismissRequest = { showThMenu = false }
                                ) {
                                    (1..17).reversed().forEach { th ->
                                        DropdownMenuItem(
                                            text = { Text("Town Hall $th", fontWeight = if (th == playerProfile.townHallLevel) FontWeight.Bold else FontWeight.Normal) },
                                            onClick = {
                                                viewModel.setTownHallLevel(th)
                                                showThMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Season Boost Buttons (0%, 10%, 15%, 20%)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Pass Boost: ",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                listOf(0, 10, 15, 20).forEach { boost ->
                                    val isSelected = playerProfile.goldPassBoostPercent == boost
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 2.dp)
                                            .background(
                                                if (isSelected) CocGold else MaterialTheme.colorScheme.surface,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable { viewModel.setGoldPassBoost(boost) }
                                            .padding(horizontal = 6.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$boost%",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick 1-click action: Set All to Previous TH Max (Clash Ninja signature feature)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Upgrading to TH${playerProfile.townHallLevel}?",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            OutlinedButton(
                                onClick = {
                                    viewModel.setAllToPreviousTownHallMax()
                                    Toast.makeText(context, "All buildings set to max for TH${playerProfile.townHallLevel - 1}!", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Set All to Previous TH Max", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Clash Ninja Signature Village Progress Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("clash_ninja_progress_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TH${playerProfile.townHallLevel} COMPLETION STATUS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.2.sp,
                                    color = CocGoldLight
                                )
                            )
                            Text(
                                text = "${villageProgress.maxedStructuresCount}/${villageProgress.totalStructuresCount} Maxed",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CocGold
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Circular Progress Ring & Category Breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ClashNinjaProgressRing(
                                percentage = villageProgress.overallPercent,
                                size = 96.dp
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                CategoryProgressBar(
                                    title = "Defenses & Traps",
                                    percentage = villageProgress.defensesPercent,
                                    color = Color(0xFFF44336)
                                )
                                CategoryProgressBar(
                                    title = "Heroes",
                                    percentage = villageProgress.heroesPercent,
                                    color = CocDarkElixir
                                )
                                CategoryProgressBar(
                                    title = "Army & Lab",
                                    percentage = villageProgress.armyLabPercent,
                                    color = CocElixir
                                )
                                CategoryProgressBar(
                                    title = "Resource Buildings",
                                    percentage = villageProgress.resourcesPercent,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Time & Costs to Max TH
                        Text(
                            text = "TIME & LOOT REMAINING TO MAX TH${playerProfile.townHallLevel}:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Builder Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    String.format(Locale.US, "%.1f Builder Days", villageProgress.remainingBuilderDays),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("With ${playerProfile.totalBuilders} Builders", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    String.format(Locale.US, "%.1f Calendar Days", villageProgress.remainingCalendarDays),
                                    fontWeight = FontWeight.Bold,
                                    color = CocGold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Resource totals to max
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ResourceBadge(
                                type = ResourceType.GOLD,
                                amount = villageProgress.remainingGold,
                                modifier = Modifier.weight(1f)
                            )
                            ResourceBadge(
                                type = ResourceType.ELIXIR,
                                amount = villageProgress.remainingElixir,
                                modifier = Modifier.weight(1f)
                            )
                            ResourceBadge(
                                type = ResourceType.DARK_ELIXIR,
                                amount = villageProgress.remainingDarkElixir,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Login Advisor Card
            item {
                LoginAdvisorCard(advice = loginAdvice)
            }

            // Section Header: Active Builder Slots
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BUILDER & LAB TRACKER",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp,
                            color = CocGold
                        )
                    )
                    Text(
                        text = "${activeUpgrades.filter { !it.isCompleted }.size}/$totalBuilders Busy",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (activeUpgrades.filter { !it.isCompleted }.size == totalBuilders) Color(0xFFFF5722) else Color(0xFF00E676)
                        )
                    )
                }
            }

            // Cards for Builders 1 .. totalBuilders
            items((1..totalBuilders).toList()) { builderIdx ->
                val task = activeUpgrades.find { it.builderIndex == builderIdx && !it.isCompleted }

                if (task != null) {
                    ActiveBuilderCard(
                        builderIndex = builderIdx,
                        task = task,
                        currentTimeMillis = currentTimeMillis,
                        onBoost = { viewModel.boostBuilder(task.id) },
                        onComplete = { viewModel.completeUpgrade(task.id) },
                        onCancel = { viewModel.deleteUpgrade(task.id) }
                    )
                } else {
                    IdleBuilderCard(
                        builderIndex = builderIdx,
                        onAssign = {
                            dialogInitialBuilderIndex = builderIdx
                            showAddDialog = true
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }

    if (showAddDialog) {
        AddUpgradeDialog(
            buildings = gameData.buildings,
            totalBuilders = totalBuilders,
            initialBuilderIndex = dialogInitialBuilderIndex,
            onDismiss = { showAddDialog = false },
            onStartUpgrade = { building, fromLvl, toLvl, bIdx ->
                viewModel.startCustomUpgrade(building, fromLvl, toLvl, bIdx)
                showAddDialog = false
            }
        )
    }
}
