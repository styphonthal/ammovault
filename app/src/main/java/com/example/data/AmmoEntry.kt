package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ammo_entries")
data class AmmoEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val caliber: String,
    val brand: String,
    val productLine: String = "",
    val grain: Int,
    val bulletType: String,
    val currentCount: Int,
    val boxSize: Int = 20,
    val lowStockThreshold: Int = 60,
    val marketPricePerRound: Double,
    val costBasis: Double = 0.0,
    val targetBuyPricePerRound: Double? = null,
    val casingMaterial: String = "Brass",
    val lotNumber: String = "",
    val upcCode: String = "",
    val vendor: String = "",
    val storageLocation: String = "",
    val photoUri: String = "",
    val purchaseDate: Long? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val totalMarketValue: Double
        get() = currentCount * marketPricePerRound

    val effectivePurchaseCpr: Double
        get() = if (costBasis > 0.0) costBasis else marketPricePerRound

    val totalCostBasis: Double
        get() = currentCount * effectivePurchaseCpr

    val unrealizedGainLoss: Double
        get() = totalMarketValue - totalCostBasis

    val unrealizedGainLossPercent: Double?
        get() = if (totalCostBasis > 0.005) (unrealizedGainLoss / totalCostBasis) * 100.0 else null

    val hasExplicitCostBasis: Boolean
        get() = costBasis > 0.0

    val stockStatus: StockStatus
        get() = when {
            currentCount <= 0 -> StockStatus.CRITICAL
            currentCount <= lowStockThreshold -> StockStatus.LOW
            else -> StockStatus.HEALTHY
        }
}

enum class StockStatus {
    HEALTHY,
    LOW,
    CRITICAL
}

data class CaliberInventorySummary(
    val caliber: String,
    val totalRounds: Int,
    val targetRounds: Int,
    val lotCount: Int,
    val totalMarketValue: Double,
    val totalCostBasis: Double = totalMarketValue,
    val weightedAvgPricePerRound: Double,
    val minPricePerRound: Double,
    val maxPricePerRound: Double,
    val lowStockLotCount: Int,
    val grainWeights: List<Int>,
    val targetBuyPricePerRound: Double? = null,
    val minThresholdRounds: Int = 0
) {
    val progressFraction: Float
        get() = if (targetRounds <= 0) 1f else (totalRounds.toFloat() / targetRounds.toFloat()).coerceIn(0f, 1f)

    val minThresholdFraction: Float?
        get() = if (minThresholdRounds > 0 && targetRounds > 0) {
            (minThresholdRounds.toFloat() / targetRounds.toFloat()).coerceIn(0.02f, 1f)
        } else {
            null
        }

    val isBelowMinThreshold: Boolean
        get() = minThresholdRounds > 0 && totalRounds < minThresholdRounds

    val roundsBelowMinThreshold: Int
        get() = if (minThresholdRounds > 0) (minThresholdRounds - totalRounds).coerceAtLeast(0) else 0

    val roundsToTarget: Int
        get() = (targetRounds - totalRounds).coerceAtLeast(0)

    val estimatedRestockCost: Double
        get() = roundsToTarget * weightedAvgPricePerRound

    val status: StockStatus
        get() = when {
            totalRounds <= 0 -> StockStatus.CRITICAL
            isBelowMinThreshold -> StockStatus.LOW
            targetRounds > 0 && totalRounds < (targetRounds * 0.25f) -> StockStatus.LOW
            lowStockLotCount > 0 && totalRounds < targetRounds -> StockStatus.LOW
            else -> StockStatus.HEALTHY
        }
}

data class CaliberThresholdConfigItem(
    val caliber: String,
    val currentRounds: Int,
    val minThresholdRounds: Int,
    val targetRounds: Int,
    val lotCount: Int,
    val weightedAvgPricePerRound: Double
) {
    val isAlertEnabled: Boolean
        get() = minThresholdRounds > 0

    val isBelowMinThreshold: Boolean
        get() = minThresholdRounds > 0 && currentRounds < minThresholdRounds

    val roundsDeficit: Int
        get() = if (minThresholdRounds > 0) (minThresholdRounds - currentRounds).coerceAtLeast(0) else 0

    val fillRatioToThreshold: Float
        get() = if (minThresholdRounds <= 0) {
            1f
        } else {
            (currentRounds.toFloat() / minThresholdRounds.toFloat()).coerceIn(0f, 1.5f)
        }
}

data class RestockLotItem(
    val entry: AmmoEntry,
    val effectiveBuyTargetCpr: Double?,
    val roundsNeededToThreshold: Int,
    val boxesSuggested: Int,
    val estimatedRestockCost: Double
) {
    val isAtOrBelowBuyTarget: Boolean
        get() = effectiveBuyTargetCpr != null && entry.marketPricePerRound <= effectiveBuyTargetCpr + 0.0001
}

enum class AmmoSortOption(val label: String) {
    CALIBER("Caliber (A–Z)"),
    ROUNDS_DESC("Rounds (High–Low)"),
    ROUNDS_ASC("Lowest Stock First"),
    PRICE_ASC("Price/Rd (Low–High)"),
    PRICE_DESC("Price/Rd (High–Low)"),
    GRAIN_DESC("Grain Weight (High–Low)")
}

object StarterRiflePresets {
    val commonCalibers = listOf(
        "5.56 NATO",
        ".223 Rem",
        ".308 Win",
        "6.5 Creedmoor",
        ".300 BLK",
        "7.62x39mm",
        ".30-06 Springfield",
        ".270 Win",
        ".243 Win",
        ".300 Win Mag",
        "6mm Creedmoor",
        ".22 LR"
    )

    val commonBrands = listOf(
        "Federal Premium",
        "Hornady",
        "PMC",
        "Winchester",
        "Lapua",
        "Nosler",
        "Norma",
        "Fiocchi",
        "Sierra",
        "Barnes",
        "AAC",
        "PPU"
    )

    val commonBulletTypes = listOf(
        "FMJ",
        "FMJ-BT",
        "BTHP",
        "OTM",
        "ELD-M",
        "ELD-X",
        "Soft Point (SP)",
        "V-MAX",
        "SST",
        "Bonded",
        "Solid Copper",
        "Subsonic"
    )

    val commonCasingMaterials = listOf(
        "Brass",
        "Nickel-Plated Brass",
        "Steel",
        "Polymer"
    )

    val commonVendors = listOf(
        "MidwayUSA",
        "SGAmmo",
        "Palmetto State",
        "Brownells",
        "Local Gun Shop"
    )

    val commonStorageLocations = listOf(
        "Safe Top Shelf",
        "Ammo Can #1",
        "Ammo Can #2",
        "Ammo Can #3",
        "Range Bag",
        "Closet Lockbox"
    )

    val commonBoxSizes = listOf(20, 50, 100, 150, 200, 500)

    fun defaultTargetForCaliber(caliber: String): Int = when {
        caliber.contains("5.56", ignoreCase = true) ||
            caliber.contains(".223", ignoreCase = true) ||
            caliber.contains("7.62x39", ignoreCase = true) -> 1000
        caliber.contains(".22 LR", ignoreCase = true) -> 2000
        caliber.contains("300 BLK", ignoreCase = true) -> 500
        else -> 200
    }

    fun defaultMinThresholdForCaliber(caliber: String): Int = when {
        caliber.contains("5.56", ignoreCase = true) ||
            caliber.contains(".223", ignoreCase = true) ||
            caliber.contains("7.62x39", ignoreCase = true) -> 250
        caliber.contains(".22 LR", ignoreCase = true) -> 500
        caliber.contains("300 BLK", ignoreCase = true) -> 100
        else -> 60
    }

    fun starterEntries(now: Long = System.currentTimeMillis()): List<AmmoEntry> = listOf(
        AmmoEntry(
            caliber = "5.56 NATO",
            brand = "PMC",
            productLine = "X-TAC M193",
            grain = 55,
            bulletType = "FMJ-BT",
            currentCount = 640,
            boxSize = 20,
            lowStockThreshold = 200,
            marketPricePerRound = 0.48,
            costBasis = 0.43,
            targetBuyPricePerRound = 0.45,
            casingMaterial = "Brass",
            lotNumber = "PMC-26A",
            upcCode = "741569010115",
            vendor = "SGAmmo",
            storageLocation = "Ammo Can #1",
            purchaseDate = now - 86400000L * 24,
            notes = "Range & carbine drills",
            createdAt = now - 86400000L * 10,
            updatedAt = now - 86400000L * 2
        ),
        AmmoEntry(
            caliber = "5.56 NATO",
            brand = "Federal Premium",
            productLine = "Gold Medal Match",
            grain = 77,
            bulletType = "BTHP",
            currentCount = 140,
            boxSize = 20,
            lowStockThreshold = 60,
            marketPricePerRound = 1.38,
            costBasis = 1.26,
            targetBuyPricePerRound = 1.30,
            casingMaterial = "Brass",
            lotNumber = "FGMM-77SMK",
            upcCode = "029465094034",
            vendor = "MidwayUSA",
            storageLocation = "Safe Top Shelf",
            purchaseDate = now - 86400000L * 18,
            notes = "18\" SPR precision load",
            createdAt = now - 86400000L * 8,
            updatedAt = now - 86400000L
        ),
        AmmoEntry(
            caliber = ".308 Win",
            brand = "Federal Premium",
            productLine = "Gold Medal Sierra",
            grain = 168,
            bulletType = "BTHP",
            currentCount = 180,
            boxSize = 20,
            lowStockThreshold = 60,
            marketPricePerRound = 1.62,
            costBasis = 1.48,
            targetBuyPricePerRound = 1.50,
            casingMaterial = "Brass",
            lotNumber = "F308GM1-112",
            upcCode = "029465089986",
            vendor = "Brownells",
            storageLocation = "Safe Top Shelf",
            purchaseDate = now - 86400000L * 45,
            notes = "100 yd zero confirmed @ 0.6 MOA",
            createdAt = now - 86400000L * 12,
            updatedAt = now - 86400000L * 3
        ),
        AmmoEntry(
            caliber = "6.5 Creedmoor",
            brand = "Hornady",
            productLine = "Match",
            grain = 140,
            bulletType = "ELD-M",
            currentCount = 120,
            boxSize = 20,
            lowStockThreshold = 60,
            marketPricePerRound = 1.75,
            costBasis = 1.82,
            targetBuyPricePerRound = 1.65,
            casingMaterial = "Brass",
            lotNumber = "HDY-65140-88",
            upcCode = "090255815009",
            vendor = "MidwayUSA",
            storageLocation = "Ammo Can #2",
            purchaseDate = now - 86400000L * 14,
            notes = "Long range steel & PRS practice",
            createdAt = now - 86400000L * 6,
            updatedAt = now - 86400000L * 4
        ),
        AmmoEntry(
            caliber = ".300 BLK",
            brand = "Hornady",
            productLine = "Sub-X",
            grain = 190,
            bulletType = "Subsonic",
            currentCount = 40,
            boxSize = 20,
            lowStockThreshold = 60,
            marketPricePerRound = 1.45,
            costBasis = 1.32,
            targetBuyPricePerRound = 1.50,
            casingMaterial = "Brass",
            lotNumber = "HDY-300SUB",
            upcCode = "090255808773",
            vendor = "Local Gun Shop",
            storageLocation = "Range Bag",
            purchaseDate = now - 86400000L * 7,
            notes = "Suppressed subsonic load — restock soon",
            createdAt = now - 86400000L * 5,
            updatedAt = now - 86400000L
        )
    )
}
