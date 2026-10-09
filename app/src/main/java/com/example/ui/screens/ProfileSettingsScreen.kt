package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PlayerProfile
import com.example.ui.theme.CocGold
import com.example.ui.theme.CocGoldLight
import com.example.viewmodel.CocViewModel

@Composable
fun ProfileSettingsScreen(
    viewModel: CocViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profile by viewModel.playerProfile.collectAsState()

    var playerName by remember(profile) { mutableStateOf(profile.playerName) }
    var playerTag by remember(profile) { mutableStateOf(profile.playerTag) }
    var townHall by remember(profile) { mutableIntStateOf(profile.townHallLevel) }
    var builders by remember(profile) { mutableIntStateOf(profile.totalBuilders) }

    var avgGoldRaid by remember(profile) { mutableStateOf(profile.avgLootPerRaidGold.toString()) }
    var avgElixirRaid by remember(profile) { mutableStateOf(profile.avgLootPerRaidElixir.toString()) }
    var avgDarkRaid by remember(profile) { mutableStateOf(profile.avgLootPerRaidDark.toString()) }

    var hourlyGold by remember(profile) { mutableStateOf(profile.hourlyCollectorGold.toString()) }
    var hourlyElixir by remember(profile) { mutableStateOf(profile.hourlyCollectorElixir.toString()) }
    var hourlyDark by remember(profile) { mutableStateOf(profile.hourlyCollectorDark.toString()) }

    var notifyFinish by remember(profile) { mutableStateOf(profile.notifyOnFinish) }
    var vibration by remember(profile) { mutableStateOf(profile.vibrationEnabled) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("profile_settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Village Profile Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("village_identity_card"),
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
                            Icon(Icons.Default.Person, contentDescription = null, tint = CocGold, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "VILLAGE IDENTITY & BUILDERS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp,
                                color = CocGoldLight
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = playerName,
                        onValueChange = { playerName = it },
                        label = { Text("Chief Name") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = playerTag,
                        onValueChange = { playerTag = it },
                        label = { Text("Clash Player Tag (#)") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_tag_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Town Hall Slider
                    Text(
                        text = "Town Hall Level: TH $townHall",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Slider(
                        value = townHall.toFloat(),
                        onValueChange = { townHall = it.toInt() },
                        valueRange = 1f..17f,
                        steps = 15,
                        modifier = Modifier.fillMaxWidth().testTag("th_level_slider")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Builder Count Slider
                    Text(
                        text = "Total Village Builders: $builders ${if (builders == 6) "(Includes B.O.B)" else ""}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Slider(
                        value = builders.toFloat(),
                        onValueChange = { builders = it.toInt() },
                        valueRange = 2f..6f,
                        steps = 3,
                        modifier = Modifier.fillMaxWidth().testTag("builders_count_slider")
                    )
                }
            }
        }

        // Notification Preferences Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notifications_settings_card"),
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
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = CocGold, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "PUSH NOTIFICATION ALERTS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp,
                                color = CocGoldLight
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Upgrade Finish Push Alerts", fontWeight = FontWeight.Bold)
                            Text(
                                "Notify immediately when a builder finishes an upgrade",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        Switch(
                            checked = notifyFinish,
                            onCheckedChange = { notifyFinish = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CocGold, checkedTrackColor = CocGold.copy(alpha = 0.5f))
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Vibration", fontWeight = FontWeight.Bold)
                            Text(
                                "Vibrate phone when upgrade completes",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        Switch(
                            checked = vibration,
                            onCheckedChange = { vibration = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CocGold, checkedTrackColor = CocGold.copy(alpha = 0.5f))
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Test notification button
                    OutlinedButton(
                        onClick = {
                            viewModel.triggerTestNotification()
                            Toast.makeText(context, "Test push notification sent!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().testTag("send_test_notification_button")
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send Test Push Notification")
                    }
                }
            }
        }

        // Farming & Production Rates
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("farming_rates_card"),
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
                            Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = CocGold, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "AVERAGE LOOT PER RAID (CALCULATOR)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp,
                                color = CocGoldLight
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = avgGoldRaid,
                            onValueChange = { avgGoldRaid = it },
                            label = { Text("Gold / Raid") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = avgElixirRaid,
                            onValueChange = { avgElixirRaid = it },
                            label = { Text("Elixir / Raid") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = avgDarkRaid,
                        onValueChange = { avgDarkRaid = it },
                        label = { Text("Dark Elixir / Raid") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        // Save Button
        item {
            Button(
                onClick = {
                    val updated = profile.copy(
                        playerName = playerName,
                        playerTag = playerTag,
                        townHallLevel = townHall,
                        totalBuilders = builders,
                        notifyOnFinish = notifyFinish,
                        vibrationEnabled = vibration,
                        avgLootPerRaidGold = avgGoldRaid.toLongOrNull() ?: profile.avgLootPerRaidGold,
                        avgLootPerRaidElixir = avgElixirRaid.toLongOrNull() ?: profile.avgLootPerRaidElixir,
                        avgLootPerRaidDark = avgDarkRaid.toLongOrNull() ?: profile.avgLootPerRaidDark
                    )
                    viewModel.updateProfile(updated)
                    Toast.makeText(context, "Village profile saved!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CocGold),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_profile_button")
            ) {
                Text(
                    text = "Save Profile Settings",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
