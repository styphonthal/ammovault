package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "range_bag_items",
    foreignKeys = [
        ForeignKey(
            entity = AmmoEntry::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["entryId"]),
        Index(value = ["bagName", "entryId"], unique = true)
    ]
)
data class RangeBagItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val bagName: String = "Primary Range Bag",
    val entryId: Long,
    val packedRounds: Int,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "storage_units")
data class StorageUnit(
    @PrimaryKey
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "range_sessions")
data class RangeSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val sessionDateTime: Long = System.currentTimeMillis(),
    val location: String = "",
    val durationMinutes: Int = 60,
    val rangeBagName: String = "",
    val storageUnitName: String = "",
    val sessionType: String = "Practice",
    val distanceYards: Int? = null,
    val bestGroupMoa: Double? = null,
    val avgVelocityFps: Int? = null,
    val notes: String = "",
    val photoUri: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "range_session_usage",
    foreignKeys = [
        ForeignKey(
            entity = RangeSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AmmoEntry::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["entryId"])
    ]
)
data class RangeSessionAmmoUsage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val sessionId: Long = 0L,
    val entryId: Long? = null,
    val caliber: String,
    val brand: String,
    val grain: Int,
    val bulletType: String,
    val lotNumber: String = "",
    val roundsUsed: Int,
    val costPerRoundSnapshot: Double = 0.0,
    val malfunctionsCount: Int = 0
) {
    val totalAmmoCost: Double
        get() = roundsUsed * costPerRoundSnapshot
}

data class RangeBagItemWithEntry(
    val bagItem: RangeBagItem,
    val entry: AmmoEntry
) {
    val estimatedValue: Double
        get() = bagItem.packedRounds * entry.marketPricePerRound
}

data class RangeSessionWithUsages(
    val session: RangeSession,
    val usages: List<RangeSessionAmmoUsage>
) {
    val totalRoundsFired: Int
        get() = usages.sumOf { it.roundsUsed }

    val totalAmmoCost: Double
        get() = usages.sumOf { it.totalAmmoCost }

    val totalMalfunctions: Int
        get() = usages.sumOf { it.malfunctionsCount }

    val calibersSummary: String
        get() = usages.map { it.caliber }.distinct().joinToString(" · ")
}

data class CaliberBurnForecast(
    val caliber: String,
    val totalRoundsFired: Int,
    val roundsFiredLast30Days: Int,
    val estimatedMonthlyBurnRate: Double,
    val currentRoundsOnHand: Int,
    val estimatedMonthsRemaining: Double?,
    val totalAmmoCostBurned: Double,
    val sessionCount: Int
)

data class LotReliabilityStats(
    val entryId: Long,
    val totalRoundsFired: Int,
    val totalMalfunctions: Int
) {
    val reliabilityPercent: Double
        get() = if (totalRoundsFired > 0) {
            ((totalRoundsFired - totalMalfunctions).coerceAtLeast(0).toDouble() / totalRoundsFired.toDouble()) * 100.0
        } else {
            100.0
        }
}

data class StorageContainerSummary(
    val locationName: String,
    val isUnassigned: Boolean = false,
    val totalRounds: Int,
    val lotCount: Int,
    val calibers: List<String>,
    val totalMarketValue: Double,
    val totalCostBasis: Double,
    val estimatedWeightLbs: Double,
    val lowStockLotCount: Int
)

object RangePresets {
    val commonRangeBags = listOf(
        "Primary Range Bag",
        "Precision Rifle Bag",
        "Carbine Drill Bag",
        "Hunting Pack"
    )

    val commonRangeLocations = listOf(
        "Outdoor 100yd Range",
        "Long Range (300-1000yd)",
        "Indoor Range",
        "Private / BLM Land",
        "Competition Match"
    )

    val commonSessionTypes = listOf(
        "Practice",
        "Zeroing",
        "Competition",
        "Hunting",
        "Load Dev"
    )

    val commonDurationsMinutes = listOf(30, 45, 60, 90, 120, 180)
}
