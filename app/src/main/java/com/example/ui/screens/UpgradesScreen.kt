package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.UpgradeTask
import com.example.ui.components.AddUpgradeDialog
import com.example.ui.components.CategoryChip
import com.example.ui.components.LoginAdvisorCard
import com.example.ui.components.ResourceBadge
import com.example.ui.components.formatRemainingMillis
import com.example.ui.theme.CocGold
import com.example.ui.theme.CocGoldLight
import com.example.viewmodel.CocViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UpgradesScreen(
    viewModel: CocViewModel,
    modifier: Modifier = Modifier
) {
    val activeUpgrades by viewModel.activeUpgrades.collectAsState()
    val playerProfile by viewModel.playerProfile.collectAsState()
    val gameData by viewModel.gameData.collectAsState()
    val loginAdvice by viewModel.loginAdvice.collectAsState()
    val currentTimeMillis by viewModel.currentTimeMillis.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var dialogInitialBuilderIndex by remember { mutableIntStateOf(1) }

    val totalBuilders = playerProfile.totalBuilders
    val busyBuildersCount = activeUpgrades.filter { !it.isCompleted }.size

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
                    Icon(Icons.Default.Add, contentDescription = "New Upgrade")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Village Banner Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hero_village_banner"),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.clash_hero_banner),
                            contentDescription = "Clash of Clans Village",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color(0xCC0F1218),
                                            Color(0xF00F1218)
                                        )
                                    )
                                )
                        )
                        // Content overlay
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = CocGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "TOWN HALL ${playerProfile.townHallLevel}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = CocGoldLight
                                        )
                                    )
                                }
                                Text(
                                    text = playerProfile.playerName,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                )
                            }

                            // Builders Status Badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (busyBuildersCount == totalBuilders) Color(0x33FF5722) else Color(0x3300E676),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (busyBuildersCount == totalBuilders) Color(0xFFFF5722) else Color(0xFF00E676)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Build,
                                        contentDescription = null,
                                        tint = if (busyBuildersCount == totalBuilders) Color(0xFFFF5722) else Color(0xFF00E676),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$busyBuildersCount/$totalBuilders Busy",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (busyBuildersCount == totalBuilders) Color(0xFFFF5722) else Color(0xFF00E676)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Login Advisor Banner
            item {
                LoginAdvisorCard(advice = loginAdvice)
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BUILDER HUTS & TIMERS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp,
                            color = CocGold
                        )
                    )
                    Text(
                        text = "Total: $totalBuilders Builders",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            // Cards for Builder 1 .. totalBuilders
            items((1..totalBuilders).toList()) { builderIdx ->
                val task = activeUpgrades.find { it.builderIndex == builderIdx && !it.isCompleted }

                if (task != null) {
                    // Active Upgrade Card
                    ActiveBuilderCard(
                        builderIndex = builderIdx,
                        task = task,
                        currentTimeMillis = currentTimeMillis,
                        onBoost = { viewModel.boostBuilder(task.id) },
                        onComplete = { viewModel.completeUpgrade(task.id) },
                        onCancel = { viewModel.deleteUpgrade(task.id) }
                    )
                } else {
                    // Idle Builder Card
                    IdleBuilderCard(
                        builderIndex = builderIdx,
                        onAssign = {
                            dialogInitialBuilderIndex = builderIdx
                            showAddDialog = true
                        }
                    )
                }
            }

            // Bottom padding for FAB
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
                viewModel.startUpgrade(building, fromLvl, toLvl, bIdx)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ActiveBuilderCard(
    builderIndex: Int,
    task: UpgradeTask,
    currentTimeMillis: Long,
    onBoost: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val remainingMillis = task.remainingMillis(currentTimeMillis)
    val progress = task.progressFraction(currentTimeMillis)
    val isFinished = task.isFinished(currentTimeMillis)

    val timeFormatter = SimpleDateFormat("h:mm a, MMM d", Locale.getDefault())
    val finishDateString = timeFormatter.format(Date(task.endTimeMillis))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("active_builder_card_$builderIndex"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
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
                            .size(32.dp)
                            .background(CocGold.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#$builderIndex",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = CocGold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = task.buildingName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CategoryChip(task.category)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Lv ${task.fromLevel} → Lv ${task.toLevel}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CocGoldLight
                                )
                            )
                        }
                    }
                }

                ResourceBadge(type = task.resourceType, amount = task.cost)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Timer display & Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = if (isFinished) Color(0xFF00E676) else CocGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatRemainingMillis(remainingMillis),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isFinished) Color(0xFF00E676) else CocGold
                        )
                    )
                }

                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isFinished) Color(0xFF00E676) else CocGold,
                trackColor = MaterialTheme.colorScheme.surface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Finishes at: $finishDateString",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            if (task.note.isNotEmpty()) {
                Text(
                    text = "Note: ${task.note}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    ),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onBoost,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Builder Potion",
                        tint = CocGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("10x Potion", fontSize = 12.sp)
                }

                Button(
                    onClick = onComplete,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("complete_builder_${builderIndex}_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFinished) Color(0xFF00C853) else MaterialTheme.colorScheme.primaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Complete",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isFinished) "Claim!" else "Finish", fontSize = 12.sp)
                }

                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Cancel Upgrade",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun IdleBuilderCard(
    builderIndex: Int,
    onAssign: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("idle_builder_card_$builderIndex"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0x2200E676), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Builder #$builderIndex is Idle",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E676)
                        )
                    )
                    Text(
                        text = "Ready for immediate assignment",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Button(
                onClick = onAssign,
                colors = ButtonDefaults.buttonColors(containerColor = CocGold),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.testTag("assign_builder_${builderIndex}_button")
            ) {
                Text(
                    text = "Assign",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
