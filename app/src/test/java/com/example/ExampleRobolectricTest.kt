package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.BuildingCategory
import com.example.model.PlayerProfile
import com.example.model.ResourceType
import com.example.model.VillageStructure
import com.example.data.CocDataRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Clash Tracker", appName)
    }

    @Test
    fun `calculate shortage returns correct shortage and raids`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = CocDataRepository(context)

        val profile = PlayerProfile(
            currentGold = 5_000_000L,
            avgLootPerRaidGold = 1_000_000L,
            hourlyCollectorGold = 100_000L
        )

        // Upgrade costs 8,000,000 Gold. Player has 5,000,000 Gold. Shortage = 3,000,000 Gold.
        val shortage = repo.calculateShortage(
            cost = 8_000_000L,
            resourceType = ResourceType.GOLD,
            profile = profile
        )

        assertEquals(3_000_000L, shortage.shortage)
        assertEquals(3, shortage.raidsNeeded) // 3 attacks at 1M each
        assertEquals(30.0f, shortage.passiveHoursNeeded, 0.01f) // 3M / 100k = 30 hours
    }

    @Test
    fun `clash ninja village progress calculates correctly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = CocDataRepository(context)

        val sampleStructures = listOf(
            VillageStructure(
                id = "cannon_1",
                buildingId = "cannon",
                name = "Cannon #1",
                category = BuildingCategory.DEFENSE,
                currentLevel = 20,
                maxLevelForTH = 20,
                resourceType = ResourceType.GOLD
            ),
            VillageStructure(
                id = "cannon_2",
                buildingId = "cannon",
                name = "Cannon #2",
                category = BuildingCategory.DEFENSE,
                currentLevel = 19,
                maxLevelForTH = 20,
                resourceType = ResourceType.GOLD
            )
        )

        val profile = PlayerProfile(
            townHallLevel = 14,
            goldPassBoostPercent = 20,
            totalBuilders = 6
        )

        val stats = repo.calculateVillageProgress(sampleStructures, profile)

        // 39 completed out of 40 total = 97.5%
        assertEquals(97.5f, stats.overallPercent, 0.1f)
        assertEquals(1, stats.maxedStructuresCount)
        assertEquals(2, stats.totalStructuresCount)
    }
}
