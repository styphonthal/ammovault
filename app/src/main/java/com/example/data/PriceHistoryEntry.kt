package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "price_history",
    foreignKeys = [
        ForeignKey(
            entity = AmmoEntry::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["entryId", "recordedAt"])]
)
data class PriceHistoryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val entryId: Long,
    val pricePerRound: Double,
    val vendor: String = "",
    val source: String = "Manual",
    val recordedAt: Long = System.currentTimeMillis()
)

data class CaliberPricePoint(
    val snapshotId: Long,
    val entryId: Long,
    val caliber: String,
    val lotLabel: String,
    val pricePerRound: Double,
    val vendor: String,
    val source: String,
    val recordedAt: Long
)

data class LotPriceLine(
    val entryId: Long,
    val label: String,
    val points: List<CaliberPricePoint>
)

data class CaliberPriceSeries(
    val caliber: String,
    val points: List<CaliberPricePoint>,
    val lotLines: List<LotPriceLine>,
    val comparisonPoints: List<CaliberPricePoint>,
    val latestPricePerRound: Double,
    val earliestPricePerRound: Double,
    val minPricePerRound: Double,
    val maxPricePerRound: Double,
    val avgPricePerRound: Double,
    val targetBuyPricePerRound: Double? = null
) {
    val snapshotCount: Int
        get() = points.size

    val priceDelta: Double
        get() = if (comparisonPoints.size >= 2) latestPricePerRound - earliestPricePerRound else 0.0

    val priceDeltaPercent: Double?
        get() = if (comparisonPoints.size >= 2 && earliestPricePerRound > 0.0001) {
            ((latestPricePerRound - earliestPricePerRound) / earliestPricePerRound) * 100.0
        } else {
            null
        }
}

