package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AmmoEntry
import com.example.data.AmmoRepository
import com.example.data.AmmoSortOption
import com.example.data.CaliberBurnForecast
import com.example.data.CaliberInventorySummary
import com.example.data.CaliberPricePoint
import com.example.data.CaliberPriceSeries
import com.example.data.CaliberTarget
import com.example.data.CaliberThresholdConfigItem
import com.example.data.LotPriceLine
import com.example.data.LotReliabilityStats
import com.example.data.PriceHistoryEntry
import com.example.data.RangeBagItem
import com.example.data.RangeBagItemWithEntry
import com.example.data.RangePresets
import com.example.data.RangeSession
import com.example.data.RangeSessionAmmoUsage
import com.example.data.RangeSessionWithUsages
import com.example.data.RestockLotItem
import com.example.data.StarterRiflePresets
import com.example.data.StockStatus
import com.example.data.StorageContainerSummary
import com.example.data.StorageQrCodec
import com.example.data.StorageUnit
import com.example.data.VaultBackupManager
import com.example.data.VaultBackupPayload
import java.io.OutputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.ceil

data class AmmoVaultUiState(
    val isLoading: Boolean = true,
    val allEntries: List<AmmoEntry> = emptyList(),
    val filteredEntries: List<AmmoEntry> = emptyList(),
    val caliberSummaries: List<CaliberInventorySummary> = emptyList(),
    val caliberTargets: List<CaliberTarget> = emptyList(),
    val activeCalibers: List<String> = emptyList(),
    val activeStorageLocations: List<String> = emptyList(),
    val allStorageUnitNames: List<String> = emptyList(),
    val selectedCaliberFilter: String? = null,
    val selectedLocationFilter: String? = null,
    val showLowStockOnly: Boolean = false,
    val searchQuery: String = "",
    val sortOption: AmmoSortOption = AmmoSortOption.CALIBER,
    val totalRounds: Int = 0,
    val totalMarketValue: Double = 0.0,
    val totalCostBasis: Double = 0.0,
    val totalUnrealizedGainLoss: Double = 0.0,
    val weightedAvgPricePerRound: Double = 0.0,
    val lowStockLotCount: Int = 0,
    val restockLots: List<RestockLotItem> = emptyList(),
    val underParCalibers: List<CaliberInventorySummary> = emptyList(),
    val estimatedRestockCost: Double = 0.0,
    val allPriceHistory: List<PriceHistoryEntry> = emptyList(),
    val priceHistoryByEntryId: Map<Long, List<PriceHistoryEntry>> = emptyMap(),
    val caliberPriceSeries: List<CaliberPriceSeries> = emptyList(),
    // Range Bag, Range Logs & Burn-Rate Analytics
    val rangeBagNames: List<String> = RangePresets.commonRangeBags,
    val selectedRangeBagName: String = RangePresets.commonRangeBags.first(),
    val rawRangeBagItems: List<RangeBagItem> = emptyList(),
    val allRangeBagItems: List<RangeBagItemWithEntry> = emptyList(),
    val selectedRangeBagItems: List<RangeBagItemWithEntry> = emptyList(),
    val rawRangeSessions: List<RangeSession> = emptyList(),
    val rawRangeSessionUsages: List<RangeSessionAmmoUsage> = emptyList(),
    val rangeSessions: List<RangeSessionWithUsages> = emptyList(),
    val caliberBurnForecasts: List<CaliberBurnForecast> = emptyList(),
    val burnForecastByCaliber: Map<String, CaliberBurnForecast> = emptyMap(),
    val burnRateRestockAlerts: List<CaliberBurnForecast> = emptyList(),
    val totalRoundsFiredAllTime: Int = 0,
    val totalRoundsFiredLast30Days: Int = 0,
    val totalRangeMinutesAllTime: Int = 0,
    val totalAmmoCostBurnedAllTime: Double = 0.0,
    // Storage Container Dashboard, FIFO Rotation & Lot Reliability
    val storageSummaries: List<StorageContainerSummary> = emptyList(),
    val fifoOldestLotIds: Set<Long> = emptySet(),
    val lotReliabilityByEntryId: Map<Long, LotReliabilityStats> = emptyMap(),
    // Minimum Threshold Configuration & Visual Alerts
    val thresholdConfigItems: List<CaliberThresholdConfigItem> = emptyList(),
    val belowMinThresholdAlerts: List<CaliberThresholdConfigItem> = emptyList()
)

class AmmoViewModel(
    private val repository: AmmoRepository
) : ViewModel() {

    private val searchQueryFlow = MutableStateFlow("")
    private val selectedCaliberFlow = MutableStateFlow<String?>(null)
    private val selectedLocationFlow = MutableStateFlow<String?>(null)
    private val lowStockOnlyFlow = MutableStateFlow(false)
    private val sortOptionFlow = MutableStateFlow(AmmoSortOption.CALIBER)
    private val selectedRangeBagFlow = MutableStateFlow(RangePresets.commonRangeBags.first())

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 6)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private data class FilterParams(
        val query: String,
        val caliber: String?,
        val location: String?,
        val lowStockOnly: Boolean,
        val sortOption: AmmoSortOption
    )

    private data class CoreCatalogBundle(
        val entries: List<AmmoEntry>,
        val targets: List<CaliberTarget>,
        val priceHistory: List<PriceHistoryEntry>,
        val storageUnits: List<StorageUnit>
    )

    private data class RangeDataBundle(
        val bagItems: List<RangeBagItem>,
        val sessions: List<RangeSession>,
        val usages: List<RangeSessionAmmoUsage>,
        val selectedBag: String
    )

    private val coreCatalogFlow = combine(
        repository.allEntries,
        repository.allCaliberTargets,
        repository.allPriceHistory,
        repository.allStorageUnits
    ) { entries, targets, priceHistory, storageUnits ->
        CoreCatalogBundle(entries, targets, priceHistory, storageUnits)
    }

    private val filterFlow = combine(
        searchQueryFlow,
        selectedCaliberFlow,
        selectedLocationFlow,
        lowStockOnlyFlow,
        sortOptionFlow
    ) { query, caliber, location, lowStockOnly, sort ->
        FilterParams(query, caliber, location, lowStockOnly, sort)
    }

    private val rangeDataFlow = combine(
        repository.allRangeBagItems,
        repository.allRangeSessions,
        repository.allRangeSessionUsages,
        selectedRangeBagFlow
    ) { bagItems, sessions, usages, selectedBag ->
        RangeDataBundle(bagItems, sessions, usages, selectedBag)
    }

    val uiState: StateFlow<AmmoVaultUiState> = combine(
        coreCatalogFlow,
        filterFlow,
        rangeDataFlow
    ) { core, filters, rangeData ->
        val entries = core.entries
        val targets = core.targets
        val priceHistory = core.priceHistory
        val savedStorageUnits = core.storageUnits
        val targetMap = targets.associateBy { it.caliber }
        val entriesById = entries.associateBy { it.id }

        val summaries = entries.groupBy { it.caliber }
            .map { (caliber, caliberEntries) ->
                val totalRounds = caliberEntries.sumOf { it.currentCount }
                val totalVal = caliberEntries.sumOf { it.totalMarketValue }
                val totalCost = caliberEntries.sumOf { it.totalCostBasis }
                val weightedCpr = if (totalRounds > 0) {
                    totalVal / totalRounds.toDouble()
                } else {
                    caliberEntries.map { it.marketPricePerRound }.average().takeIf { !it.isNaN() } ?: 0.0
                }
                val minCpr = caliberEntries.minOfOrNull { it.marketPricePerRound } ?: 0.0
                val maxCpr = caliberEntries.maxOfOrNull { it.marketPricePerRound } ?: 0.0
                val targetObj = targetMap[caliber]
                val targetRounds = targetObj?.targetRounds ?: StarterRiflePresets.defaultTargetForCaliber(caliber)
                val lowLots = caliberEntries.count { it.stockStatus != StockStatus.HEALTHY }
                val grains = caliberEntries.map { it.grain }.distinct().sorted()

                CaliberInventorySummary(
                    caliber = caliber,
                    totalRounds = totalRounds,
                    targetRounds = targetRounds,
                    lotCount = caliberEntries.size,
                    totalMarketValue = totalVal,
                    totalCostBasis = totalCost,
                    weightedAvgPricePerRound = weightedCpr,
                    minPricePerRound = minCpr,
                    maxPricePerRound = maxCpr,
                    lowStockLotCount = lowLots,
                    grainWeights = grains,
                    targetBuyPricePerRound = targetObj?.targetBuyPricePerRound,
                    minThresholdRounds = targetObj?.minThresholdRounds ?: 0
                )
            }
            .sortedBy { it.caliber.lowercase() }

        val summaryByCaliber = summaries.associateBy { it.caliber }
        val allThresholdCalibers = (summaries.map { it.caliber } + targets.map { it.caliber })
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sortedBy { it.lowercase() }

        val thresholdConfigItems = allThresholdCalibers.map { caliber ->
            val summary = summaryByCaliber[caliber]
            val targetObj = targetMap[caliber]
            CaliberThresholdConfigItem(
                caliber = caliber,
                currentRounds = summary?.totalRounds ?: 0,
                minThresholdRounds = targetObj?.minThresholdRounds ?: 0,
                targetRounds = targetObj?.targetRounds ?: StarterRiflePresets.defaultTargetForCaliber(caliber),
                lotCount = summary?.lotCount ?: 0,
                weightedAvgPricePerRound = summary?.weightedAvgPricePerRound ?: 0.0
            )
        }

        val belowMinThresholdAlerts = thresholdConfigItems
            .filter { it.isBelowMinThreshold }
            .sortedWith(
                compareByDescending<CaliberThresholdConfigItem> { it.roundsDeficit }
                    .thenBy { it.caliber.lowercase() }
            )

        val activeCalibers = summaries.map { it.caliber }
        val activeLocations = entries
            .map { it.storageLocation.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sortedBy { it.lowercase() }

        val allStorageUnitNames = (savedStorageUnits.map { it.name.trim() } + activeLocations)
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }

        val effectiveCaliberFilter = filters.caliber?.takeIf { it in activeCalibers }
        val effectiveLocationFilter = filters.location?.takeIf { loc ->
            allStorageUnitNames.any { it.equals(loc, ignoreCase = true) }
        }

        val queryTrimmed = filters.query.trim()
        val filtered = entries
            .filter { entry ->
                val matchesCaliber = effectiveCaliberFilter == null || entry.caliber == effectiveCaliberFilter
                val matchesLocation = effectiveLocationFilter == null ||
                    entry.storageLocation.trim().equals(effectiveLocationFilter, ignoreCase = true)
                val matchesLowStock = !filters.lowStockOnly || entry.stockStatus != StockStatus.HEALTHY
                val matchesQuery = queryTrimmed.isBlank() ||
                    entry.caliber.contains(queryTrimmed, ignoreCase = true) ||
                    entry.brand.contains(queryTrimmed, ignoreCase = true) ||
                    entry.productLine.contains(queryTrimmed, ignoreCase = true) ||
                    entry.bulletType.contains(queryTrimmed, ignoreCase = true) ||
                    entry.grain.toString().contains(queryTrimmed, ignoreCase = true) ||
                    entry.lotNumber.contains(queryTrimmed, ignoreCase = true) ||
                    entry.upcCode.contains(queryTrimmed, ignoreCase = true) ||
                    entry.vendor.contains(queryTrimmed, ignoreCase = true) ||
                    entry.storageLocation.contains(queryTrimmed, ignoreCase = true)
                matchesCaliber && matchesLocation && matchesLowStock && matchesQuery
            }
            .let { list ->
                when (filters.sortOption) {
                    AmmoSortOption.CALIBER -> list.sortedWith(compareBy({ it.caliber.lowercase() }, { it.grain }))
                    AmmoSortOption.ROUNDS_DESC -> list.sortedByDescending { it.currentCount }
                    AmmoSortOption.ROUNDS_ASC -> list.sortedBy { it.currentCount }
                    AmmoSortOption.PRICE_ASC -> list.sortedBy { it.marketPricePerRound }
                    AmmoSortOption.PRICE_DESC -> list.sortedByDescending { it.marketPricePerRound }
                    AmmoSortOption.GRAIN_DESC -> list.sortedByDescending { it.grain }
                }
            }

        val restockLots = entries
            .filter { it.stockStatus != StockStatus.HEALTHY }
            .sortedWith(compareBy<AmmoEntry>({ it.currentCount }, { it.caliber.lowercase() }))
            .map { entry ->
                val boxSize = entry.boxSize.coerceAtLeast(1)
                val deficit = (entry.lowStockThreshold - entry.currentCount + boxSize).coerceAtLeast(boxSize)
                val boxes = ceil(deficit.toDouble() / boxSize.toDouble()).toInt().coerceAtLeast(1)
                val effectiveBuyTarget = entry.targetBuyPricePerRound ?: targetMap[entry.caliber]?.targetBuyPricePerRound
                val estCost = (boxes * boxSize) * entry.marketPricePerRound
                RestockLotItem(
                    entry = entry,
                    effectiveBuyTargetCpr = effectiveBuyTarget,
                    roundsNeededToThreshold = (entry.lowStockThreshold - entry.currentCount).coerceAtLeast(0),
                    boxesSuggested = boxes,
                    estimatedRestockCost = estCost
                )
            }

        val underParCalibers = summaries
            .filter { it.roundsToTarget > 0 }
            .sortedBy { it.progressFraction }

        // Range Bag projections
        val allBagItemsWithEntry = rangeData.bagItems.mapNotNull { bagItem ->
            val linked = entriesById[bagItem.entryId] ?: return@mapNotNull null
            RangeBagItemWithEntry(bagItem = bagItem, entry = linked)
        }
        val dynamicBagNames = (RangePresets.commonRangeBags +
            rangeData.bagItems.map { it.bagName } +
            rangeData.sessions.map { it.rangeBagName }.filter { it.isNotBlank() } +
            listOf(rangeData.selectedBag))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        val selectedBagItems = allBagItemsWithEntry.filter {
            it.bagItem.bagName.equals(rangeData.selectedBag, ignoreCase = true)
        }

        // Range Sessions & Burn-Rate Forecasts
        val usagesBySession = rangeData.usages.groupBy { it.sessionId }
        val sessionsWithUsages = rangeData.sessions
            .sortedByDescending { it.sessionDateTime }
            .map { session ->
                RangeSessionWithUsages(
                    session = session,
                    usages = usagesBySession[session.id].orEmpty()
                )
            }

        val now = System.currentTimeMillis()
        val thirtyDaysMs = 30L * 24L * 60L * 60L * 1000L
        val ninetyDaysMs = 90L * 24L * 60L * 60L * 1000L
        val sessionDateMap = rangeData.sessions.associate { it.id to it.sessionDateTime }
        val roundsOnHandByCaliber = summaries.associate { it.caliber to it.totalRounds }

        val totalFiredAllTime = rangeData.usages.sumOf { it.roundsUsed }
        val totalFiredLast30 = rangeData.usages
            .filter { usage ->
                val ts = sessionDateMap[usage.sessionId] ?: 0L
                ts >= now - thirtyDaysMs
            }
            .sumOf { it.roundsUsed }
        val totalRangeMinutes = rangeData.sessions.sumOf { it.durationMinutes }
        val totalCostBurned = rangeData.usages.sumOf { it.totalAmmoCost }

        val caliberBurnForecasts = rangeData.usages
            .groupBy { it.caliber }
            .map { (caliber, calUsages) ->
                val totalFired = calUsages.sumOf { it.roundsUsed }
                val fired30 = calUsages
                    .filter { (sessionDateMap[it.sessionId] ?: 0L) >= now - thirtyDaysMs }
                    .sumOf { it.roundsUsed }
                val fired90 = calUsages
                    .filter { (sessionDateMap[it.sessionId] ?: 0L) >= now - ninetyDaysMs }
                    .sumOf { it.roundsUsed }
                val sessionIds = calUsages.map { it.sessionId }.distinct()
                val earliestTs = sessionIds.mapNotNull { sessionDateMap[it] }.minOrNull() ?: now
                val spanMonths = ((now - earliestTs).coerceAtLeast(0L).toDouble() / thirtyDaysMs.toDouble())
                    .coerceAtLeast(1.0)
                val monthlyRate = when {
                    fired30 > 0 && spanMonths <= 1.5 -> fired30.toDouble()
                    fired90 > 0 -> fired90.toDouble() / spanMonths.coerceAtMost(3.0)
                    else -> totalFired.toDouble() / spanMonths
                }
                val onHand = roundsOnHandByCaliber[caliber] ?: 0
                val monthsRemaining = if (monthlyRate > 0.01) {
                    onHand.toDouble() / monthlyRate
                } else {
                    null
                }
                CaliberBurnForecast(
                    caliber = caliber,
                    totalRoundsFired = totalFired,
                    roundsFiredLast30Days = fired30,
                    estimatedMonthlyBurnRate = monthlyRate,
                    currentRoundsOnHand = onHand,
                    estimatedMonthsRemaining = monthsRemaining,
                    totalAmmoCostBurned = calUsages.sumOf { it.totalAmmoCost },
                    sessionCount = sessionIds.size
                )
            }
            .sortedByDescending { it.totalRoundsFired }

        val burnForecastMap = caliberBurnForecasts.associateBy { it.caliber }
        val burnRateAlerts = caliberBurnForecasts
            .filter { forecast ->
                val months = forecast.estimatedMonthsRemaining
                months != null && months < 2.5
            }
            .sortedBy { it.estimatedMonthsRemaining ?: Double.MAX_VALUE }

        // FIFO: highlight oldest in-stock lot per caliber when >= 2 in-stock lots exist
        val fifoOldestIds = entries
            .filter { it.currentCount > 0 }
            .groupBy { it.caliber }
            .mapNotNull { (_, group) ->
                if (group.size < 2) null
                else group.minWithOrNull(
                    compareBy<AmmoEntry>({ it.purchaseDate ?: it.createdAt }, { it.id })
                )?.id
            }
            .toSet()

        // Lot Reliability / Malfunction tracking from Range Logs
        val reliabilityByEntry = rangeData.usages
            .filter { it.entryId != null }
            .groupBy { it.entryId!! }
            .mapValues { (entryId, usages) ->
                LotReliabilityStats(
                    entryId = entryId,
                    totalRoundsFired = usages.sumOf { it.roundsUsed },
                    totalMalfunctions = usages.sumOf { it.malfunctionsCount }
                )
            }

        // Storage Container / Ammo Can Dashboard Summaries (includes empty saved units)
        val entriesByLocationLower = entries.groupBy { it.storageLocation.trim().lowercase() }
        val namedStorageSummaries = allStorageUnitNames.map { unitName ->
            val containerEntries = entriesByLocationLower[unitName.lowercase()].orEmpty()
            val estWeight = containerEntries.sumOf { entry ->
                StorageQrCodec.estimateAmmoWeightLbs(
                    caliber = entry.caliber,
                    bulletGrain = entry.grain,
                    rounds = entry.currentCount
                )
            }
            StorageContainerSummary(
                locationName = unitName,
                isUnassigned = false,
                totalRounds = containerEntries.sumOf { it.currentCount },
                lotCount = containerEntries.size,
                calibers = containerEntries.map { it.caliber }.distinct().sorted(),
                totalMarketValue = containerEntries.sumOf { it.totalMarketValue },
                totalCostBasis = containerEntries.sumOf { it.totalCostBasis },
                estimatedWeightLbs = estWeight,
                lowStockLotCount = containerEntries.count { it.stockStatus != StockStatus.HEALTHY }
            )
        }
        val unassignedEntries = entriesByLocationLower[""].orEmpty()
        val unassignedSummary = if (unassignedEntries.isNotEmpty()) {
            listOf(
                StorageContainerSummary(
                    locationName = "Unassigned Storage",
                    isUnassigned = true,
                    totalRounds = unassignedEntries.sumOf { it.currentCount },
                    lotCount = unassignedEntries.size,
                    calibers = unassignedEntries.map { it.caliber }.distinct().sorted(),
                    totalMarketValue = unassignedEntries.sumOf { it.totalMarketValue },
                    totalCostBasis = unassignedEntries.sumOf { it.totalCostBasis },
                    estimatedWeightLbs = unassignedEntries.sumOf { entry ->
                        StorageQrCodec.estimateAmmoWeightLbs(
                            caliber = entry.caliber,
                            bulletGrain = entry.grain,
                            rounds = entry.currentCount
                        )
                    },
                    lowStockLotCount = unassignedEntries.count { it.stockStatus != StockStatus.HEALTHY }
                )
            )
        } else {
            emptyList()
        }
        val storageSummaries = namedStorageSummaries + unassignedSummary

        val totalRounds = entries.sumOf { it.currentCount }
        val totalVal = entries.sumOf { it.totalMarketValue }
        val totalCost = entries.sumOf { it.totalCostBasis }
        val weightedAvgCpr = if (totalRounds > 0) totalVal / totalRounds.toDouble() else 0.0
        val lowCount = entries.count { it.stockStatus != StockStatus.HEALTHY }
        val historyByEntry = priceHistory.groupBy { it.entryId }
        val restockTotalCost = restockLots.sumOf { it.estimatedRestockCost }

        val caliberPriceSeries = summaries.mapNotNull { summary ->
            val caliberEntries = entries.filter { it.caliber == summary.caliber }
            if (caliberEntries.isEmpty()) return@mapNotNull null

            val rawPoints = caliberEntries.flatMap { entry ->
                val lotLabel = "${entry.brand} ${entry.grain}gr".trim()
                val entryHistory = historyByEntry[entry.id].orEmpty()
                if (entryHistory.isNotEmpty()) {
                    entryHistory.map { snap ->
                        CaliberPricePoint(
                            snapshotId = snap.id,
                            entryId = entry.id,
                            caliber = entry.caliber,
                            lotLabel = lotLabel,
                            pricePerRound = snap.pricePerRound,
                            vendor = snap.vendor.ifBlank { entry.vendor },
                            source = snap.source,
                            recordedAt = snap.recordedAt
                        )
                    }
                } else {
                    listOf(
                        CaliberPricePoint(
                            snapshotId = -entry.id,
                            entryId = entry.id,
                            caliber = entry.caliber,
                            lotLabel = lotLabel,
                            pricePerRound = entry.marketPricePerRound,
                            vendor = entry.vendor,
                            source = "Current Market",
                            recordedAt = entry.updatedAt
                        )
                    )
                }
            }.sortedWith(compareBy<CaliberPricePoint>({ it.recordedAt }, { it.snapshotId }))

            if (rawPoints.isEmpty()) return@mapNotNull null

            val lotLines = rawPoints
                .groupBy { it.entryId }
                .map { (entryId, pts) ->
                    val sortedPts = pts.sortedWith(compareBy<CaliberPricePoint>({ it.recordedAt }, { it.snapshotId }))
                    LotPriceLine(
                        entryId = entryId,
                        label = sortedPts.first().lotLabel,
                        points = sortedPts
                    )
                }
                .sortedWith(compareByDescending<LotPriceLine> { it.points.size }.thenBy { it.label.lowercase() })

            // Running average per caliber across active lots at each timestamp (prevents sawtooth jumps in All-Calibers view)
            val comparisonPoints = if (lotLines.size <= 1) {
                rawPoints
            } else {
                val runningByLot = mutableMapOf<Long, Double>()
                rawPoints.map { pt ->
                    runningByLot[pt.entryId] = pt.pricePerRound
                    val avgAtTimestamp = runningByLot.values.average()
                    pt.copy(pricePerRound = avgAtTimestamp)
                }
            }

            val allPrices = rawPoints.map { it.pricePerRound }
            val latestCpr = comparisonPoints.last().pricePerRound
            val earliestCpr = comparisonPoints.first().pricePerRound

            CaliberPriceSeries(
                caliber = summary.caliber,
                points = rawPoints,
                lotLines = lotLines,
                comparisonPoints = comparisonPoints,
                latestPricePerRound = latestCpr,
                earliestPricePerRound = earliestCpr,
                minPricePerRound = allPrices.minOrNull() ?: latestCpr,
                maxPricePerRound = allPrices.maxOrNull() ?: latestCpr,
                avgPricePerRound = allPrices.average().takeIf { !it.isNaN() } ?: latestCpr,
                targetBuyPricePerRound = summary.targetBuyPricePerRound
            )
        }

        AmmoVaultUiState(
            isLoading = false,
            allEntries = entries,
            filteredEntries = filtered,
            caliberSummaries = summaries,
            caliberTargets = targets,
            activeCalibers = activeCalibers,
            activeStorageLocations = activeLocations,
            allStorageUnitNames = allStorageUnitNames,
            selectedCaliberFilter = effectiveCaliberFilter,
            selectedLocationFilter = effectiveLocationFilter,
            showLowStockOnly = filters.lowStockOnly,
            searchQuery = filters.query,
            sortOption = filters.sortOption,
            totalRounds = totalRounds,
            totalMarketValue = totalVal,
            totalCostBasis = totalCost,
            totalUnrealizedGainLoss = totalVal - totalCost,
            weightedAvgPricePerRound = weightedAvgCpr,
            lowStockLotCount = lowCount,
            restockLots = restockLots,
            underParCalibers = underParCalibers,
            estimatedRestockCost = restockTotalCost,
            allPriceHistory = priceHistory,
            priceHistoryByEntryId = historyByEntry,
            caliberPriceSeries = caliberPriceSeries,
            rangeBagNames = dynamicBagNames,
            selectedRangeBagName = rangeData.selectedBag,
            rawRangeBagItems = rangeData.bagItems,
            allRangeBagItems = allBagItemsWithEntry,
            selectedRangeBagItems = selectedBagItems,
            rawRangeSessions = rangeData.sessions,
            rawRangeSessionUsages = rangeData.usages,
            rangeSessions = sessionsWithUsages,
            caliberBurnForecasts = caliberBurnForecasts,
            burnForecastByCaliber = burnForecastMap,
            burnRateRestockAlerts = burnRateAlerts,
            totalRoundsFiredAllTime = totalFiredAllTime,
            totalRoundsFiredLast30Days = totalFiredLast30,
            totalRangeMinutesAllTime = totalRangeMinutes,
            totalAmmoCostBurnedAllTime = totalCostBurned,
            storageSummaries = storageSummaries,
            fifoOldestLotIds = fifoOldestIds,
            lotReliabilityByEntryId = reliabilityByEntry,
            thresholdConfigItems = thresholdConfigItems,
            belowMinThresholdAlerts = belowMinThresholdAlerts
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = AmmoVaultUiState()
    )

    fun observeEntry(entryId: Long): Flow<AmmoEntry?> = repository.observeEntry(entryId)

    fun observePriceHistory(entryId: Long): Flow<List<PriceHistoryEntry>> =
        repository.observePriceHistory(entryId)

    suspend fun getEntryById(entryId: Long): AmmoEntry? = repository.getEntry(entryId)

    fun updateSearchQuery(query: String) {
        searchQueryFlow.value = query
    }

    fun selectCaliberFilter(caliber: String?) {
        selectedCaliberFlow.value = caliber
    }

    fun selectLocationFilter(location: String?) {
        selectedLocationFlow.value = location
    }

    fun toggleLowStockOnly() {
        lowStockOnlyFlow.value = !lowStockOnlyFlow.value
    }

    fun setSortOption(option: AmmoSortOption) {
        sortOptionFlow.value = option
    }

    fun selectRangeBag(bagName: String) {
        val trimmed = bagName.trim()
        if (trimmed.isNotEmpty()) {
            selectedRangeBagFlow.value = trimmed
        }
    }

    fun packLotInRangeBag(bagName: String, entry: AmmoEntry, packedRounds: Int) {
        val trimmedBag = bagName.trim().ifEmpty { RangePresets.commonRangeBags.first() }
        selectedRangeBagFlow.value = trimmedBag
        viewModelScope.launch {
            repository.upsertRangeBagItem(trimmedBag, entry.id, packedRounds)
            if (packedRounds > 0) {
                _events.tryEmit("Packed $packedRounds rds of ${entry.brand} ${entry.caliber} in $trimmedBag")
            } else {
                _events.tryEmit("Removed ${entry.brand} ${entry.caliber} from $trimmedBag")
            }
        }
    }

    fun removeRangeBagItem(itemId: Long) {
        viewModelScope.launch {
            repository.deleteRangeBagItem(itemId)
            _events.tryEmit("Removed lot from range bag")
        }
    }

    fun clearRangeBag(bagName: String) {
        viewModelScope.launch {
            repository.clearRangeBag(bagName)
            _events.tryEmit("Cleared $bagName")
        }
    }

    fun packStorageUnitInRangeBag(bagName: String, storageUnitName: String) {
        val trimmedBag = bagName.trim().ifEmpty { RangePresets.commonRangeBags.first() }
        val trimmedUnit = storageUnitName.trim()
        if (trimmedUnit.isEmpty()) return
        selectedRangeBagFlow.value = trimmedBag
        val matchingLots = uiState.value.allEntries.filter {
            it.storageLocation.trim().equals(trimmedUnit, ignoreCase = true) && it.currentCount > 0
        }
        if (matchingLots.isEmpty()) {
            _events.tryEmit("No in-stock rounds found in $trimmedUnit")
            return
        }
        viewModelScope.launch {
            var totalPacked = 0
            matchingLots.forEach { entry ->
                repository.upsertRangeBagItem(trimmedBag, entry.id, entry.currentCount)
                totalPacked += entry.currentCount
            }
            _events.tryEmit("Packed ${matchingLots.size} lot(s) ($totalPacked rds) from $trimmedUnit into $trimmedBag")
        }
    }

    fun addStorageUnit(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.addStorageUnit(trimmed)
            _events.tryEmit("Added storage unit \"$trimmed\"")
        }
    }

    fun renameStorageUnit(oldName: String, newName: String) {
        val trimmedOld = oldName.trim()
        val trimmedNew = newName.trim()
        if (trimmedOld.isEmpty() || trimmedNew.isEmpty() || trimmedOld == trimmedNew) return
        viewModelScope.launch {
            repository.renameStorageUnit(trimmedOld, trimmedNew)
            if (selectedLocationFlow.value?.equals(trimmedOld, ignoreCase = true) == true) {
                selectedLocationFlow.value = trimmedNew
            }
            _events.tryEmit("Renamed storage unit \"$trimmedOld\" to \"$trimmedNew\"")
        }
    }

    fun deleteStorageUnit(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.deleteStorageUnit(trimmed)
            if (selectedLocationFlow.value?.equals(trimmed, ignoreCase = true) == true) {
                selectedLocationFlow.value = null
            }
            _events.tryEmit("Deleted storage unit \"$trimmed\" (lots moved to Unassigned)")
        }
    }

    private fun caliberCurrentTotal(caliber: String): Int =
        uiState.value.allEntries.filter { it.caliber == caliber }.sumOf { it.currentCount }

    private fun caliberMinThreshold(caliber: String): Int =
        uiState.value.caliberTargets.find { it.caliber == caliber }?.minThresholdRounds ?: 0

    fun logRangeSession(
        session: RangeSession,
        usages: List<RangeSessionAmmoUsage>,
        clearSourceBag: Boolean,
        onComplete: () -> Unit = {}
    ) {
        val validUsages = usages.filter { it.roundsUsed > 0 }
        if (validUsages.isEmpty()) return
        val preTotalsByCaliber = validUsages.map { it.caliber }.distinct()
            .associateWith { caliberCurrentTotal(it) }
        val deductedByCaliber = validUsages.groupBy { it.caliber }
            .mapValues { (_, list) -> list.sumOf { it.roundsUsed } }
        val thresholdsByCaliber = preTotalsByCaliber.keys.associateWith { caliberMinThreshold(it) }

        viewModelScope.launch {
            repository.logRangeSession(
                session = session,
                usages = validUsages,
                clearSourceBag = clearSourceBag
            )
            val totalRounds = validUsages.sumOf { it.roundsUsed }
            val droppedAlerts = deductedByCaliber.mapNotNull { (caliber, deducted) ->
                val minThreshold = thresholdsByCaliber[caliber] ?: 0
                val prevTotal = preTotalsByCaliber[caliber] ?: 0
                val newTotal = (prevTotal - deducted).coerceAtLeast(0)
                if (minThreshold > 0 && newTotal < minThreshold && newTotal < prevTotal) {
                    "$caliber ($newTotal/$minThreshold)"
                } else {
                    null
                }
            }
            if (droppedAlerts.isNotEmpty()) {
                _events.tryEmit("⚠ Low Stock Alert: ${droppedAlerts.joinToString(", ")} below minimum (-$totalRounds rds logged)")
            } else {
                _events.tryEmit("Logged range session (-$totalRounds rds deducted)")
            }
            onComplete()
        }
    }

    fun deleteRangeSession(sessionId: Long, restoreInventory: Boolean) {
        viewModelScope.launch {
            repository.deleteRangeSession(sessionId = sessionId, restoreInventory = restoreInventory)
            val suffix = if (restoreInventory) " & restored rounds" else ""
            _events.tryEmit("Deleted range log$suffix")
        }
    }

    fun saveEntry(entry: AmmoEntry, onComplete: (Long) -> Unit = {}) {
        val existingEntry = if (entry.id != 0L) uiState.value.allEntries.find { it.id == entry.id } else null
        val prevCaliberTotal = caliberCurrentTotal(entry.caliber)
        val minThreshold = caliberMinThreshold(entry.caliber)
        val projectedNewTotal = if (existingEntry != null && existingEntry.caliber == entry.caliber) {
            (prevCaliberTotal - existingEntry.currentCount + entry.currentCount).coerceAtLeast(0)
        } else {
            prevCaliberTotal + entry.currentCount
        }
        viewModelScope.launch {
            val savedId = repository.saveEntry(entry)
            if (minThreshold > 0 && projectedNewTotal < minThreshold && projectedNewTotal < prevCaliberTotal) {
                _events.tryEmit("⚠ Low Stock Alert: ${entry.caliber} dropped to $projectedNewTotal rds (min: $minThreshold rds)")
            } else {
                _events.tryEmit(if (entry.id == 0L) "Added ${entry.brand} ${entry.caliber}" else "Updated ${entry.brand} ${entry.caliber}")
            }
            onComplete(savedId)
        }
    }

    fun adjustRounds(entry: AmmoEntry, delta: Int) {
        val prevTotal = caliberCurrentTotal(entry.caliber)
        val newEntryCount = (entry.currentCount + delta).coerceAtLeast(0)
        val actualDelta = newEntryCount - entry.currentCount
        val projectedNewTotal = (prevTotal + actualDelta).coerceAtLeast(0)
        val minThreshold = caliberMinThreshold(entry.caliber)
        viewModelScope.launch {
            repository.adjustRounds(entry.id, delta)
            if (minThreshold > 0 && projectedNewTotal < minThreshold && projectedNewTotal < prevTotal) {
                _events.tryEmit("⚠ Low Stock Alert: ${entry.caliber} dropped to $projectedNewTotal rds (min: $minThreshold rds)")
            } else {
                val action = if (delta >= 0) "+$delta rds" else "$delta rds"
                _events.tryEmit("${entry.caliber} (${entry.brand}): $action")
            }
        }
    }

    fun setRoundsOnHand(entry: AmmoEntry, exactRounds: Int) {
        val clamped = exactRounds.coerceAtLeast(0)
        val prevTotal = caliberCurrentTotal(entry.caliber)
        val projectedNewTotal = (prevTotal - entry.currentCount + clamped).coerceAtLeast(0)
        val minThreshold = caliberMinThreshold(entry.caliber)
        viewModelScope.launch {
            repository.setRoundsOnHand(entry.id, clamped)
            if (minThreshold > 0 && projectedNewTotal < minThreshold && projectedNewTotal < prevTotal) {
                _events.tryEmit("⚠ Low Stock Alert: ${entry.caliber} dropped to $projectedNewTotal rds (min: $minThreshold rds)")
            } else {
                _events.tryEmit("${entry.caliber} (${entry.brand}) set to $clamped rds")
            }
        }
    }

    fun updateMarketPrice(entry: AmmoEntry, newPricePerRound: Double, source: String = "Market Update") {
        viewModelScope.launch {
            repository.updateMarketPrice(entry.id, newPricePerRound, source)
            _events.tryEmit("Recorded $source for ${entry.brand} ${entry.caliber}")
        }
    }

    fun updateEntryBuyTarget(entry: AmmoEntry, targetBuyPricePerRound: Double?) {
        viewModelScope.launch {
            repository.updateEntryBuyTarget(entry.id, targetBuyPricePerRound)
            if (targetBuyPricePerRound != null) {
                _events.tryEmit("Set buy target for ${entry.brand} ${entry.caliber}")
            } else {
                _events.tryEmit("Cleared buy target for ${entry.brand} ${entry.caliber}")
            }
        }
    }

    fun deleteEntry(entry: AmmoEntry, onComplete: () -> Unit = {}) {
        val prevTotal = caliberCurrentTotal(entry.caliber)
        val projectedNewTotal = (prevTotal - entry.currentCount).coerceAtLeast(0)
        val minThreshold = caliberMinThreshold(entry.caliber)
        viewModelScope.launch {
            repository.deleteEntry(entry.id)
            if (minThreshold > 0 && projectedNewTotal < minThreshold && projectedNewTotal < prevTotal) {
                _events.tryEmit("⚠ Low Stock Alert: ${entry.caliber} dropped to $projectedNewTotal rds (min: $minThreshold rds)")
            } else {
                _events.tryEmit("Removed ${entry.brand} ${entry.caliber}")
            }
            onComplete()
        }
    }

    fun setCaliberTarget(
        caliber: String,
        targetRounds: Int,
        targetBuyPricePerRound: Double? = null,
        minThresholdRounds: Int? = null
    ) {
        viewModelScope.launch {
            repository.setCaliberTarget(caliber, targetRounds, targetBuyPricePerRound, minThresholdRounds)
            _events.tryEmit("Updated $caliber par & threshold settings")
        }
    }

    fun setCaliberMinThreshold(caliber: String, minThresholdRounds: Int) {
        val trimmed = caliber.trim()
        if (trimmed.isEmpty()) return
        val clamped = minThresholdRounds.coerceAtLeast(0)
        val currentTotal = caliberCurrentTotal(trimmed)
        viewModelScope.launch {
            repository.setCaliberMinThreshold(trimmed, clamped)
            when {
                clamped == 0 -> _events.tryEmit("Disabled minimum threshold alert for $trimmed")
                currentTotal < clamped -> _events.tryEmit("⚠ Low Stock Alert: $trimmed is below min ($currentTotal / $clamped rds)")
                else -> _events.tryEmit("Set $trimmed minimum threshold to $clamped rds")
            }
        }
    }

    fun applyDefaultMinThresholds() {
        val unconfigured = uiState.value.thresholdConfigItems.filter { it.minThresholdRounds == 0 }
        if (unconfigured.isEmpty()) {
            _events.tryEmit("All ammunition types already have minimum thresholds configured")
            return
        }
        viewModelScope.launch {
            unconfigured.forEach { item ->
                val defaultMin = StarterRiflePresets.defaultMinThresholdForCaliber(item.caliber)
                repository.setCaliberMinThreshold(item.caliber, defaultMin)
            }
            _events.tryEmit("Applied smart minimum thresholds to ${unconfigured.size} ammo type(s)")
        }
    }

    fun removeThresholdOnlyCaliber(caliber: String) {
        val trimmed = caliber.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.deleteCaliberTarget(trimmed)
            _events.tryEmit("Removed threshold configuration for $trimmed")
        }
    }

    fun renameCaliber(oldCaliber: String, newCaliber: String) {
        val trimmed = newCaliber.trim()
        if (trimmed.isEmpty() || oldCaliber == trimmed) return
        viewModelScope.launch {
            repository.renameCaliber(oldCaliber, trimmed)
            if (selectedCaliberFlow.value == oldCaliber) {
                selectedCaliberFlow.value = trimmed
            }
            _events.tryEmit("Renamed $oldCaliber to $trimmed")
        }
    }

    fun deleteCaliberInventory(caliber: String) {
        viewModelScope.launch {
            repository.deleteCaliberInventory(caliber)
            if (selectedCaliberFlow.value == caliber) {
                selectedCaliberFlow.value = null
            }
            _events.tryEmit("Deleted all $caliber inventory")
        }
    }

    fun exportBackupJson(): String {
        val state = uiState.value
        return VaultBackupManager.exportToJson(
            entries = state.allEntries,
            targets = state.caliberTargets,
            history = state.allPriceHistory,
            rangeBagItems = state.rawRangeBagItems,
            rangeSessions = state.rawRangeSessions,
            rangeSessionUsages = state.rawRangeSessionUsages,
            storageUnits = state.allStorageUnitNames
        )
    }

    fun exportBackupZip(outputStream: OutputStream): Int {
        val state = uiState.value
        return VaultBackupManager.exportZipBackup(
            outputStream = outputStream,
            entries = state.allEntries,
            targets = state.caliberTargets,
            history = state.allPriceHistory,
            rangeBagItems = state.rawRangeBagItems,
            rangeSessions = state.rawRangeSessions,
            rangeSessionUsages = state.rawRangeSessionUsages,
            storageUnits = state.allStorageUnitNames
        )
    }

    fun exportCatalogCsv(): String {
        val state = uiState.value
        return VaultBackupManager.exportCatalogToCsv(
            entries = state.allEntries,
            targets = state.caliberTargets
        )
    }

    fun importBackup(payload: VaultBackupPayload, replaceExisting: Boolean) {
        viewModelScope.launch {
            val count = repository.importBackup(payload, replaceExisting)
            val modeLabel = if (replaceExisting) "Restored" else "Merged"
            _events.tryEmit("$modeLabel $count ammo lots from backup")
        }
    }

    fun loadStarterEntries() {
        viewModelScope.launch {
            repository.loadStarterEntries()
            _events.tryEmit("Loaded 5 starter rifle ammo entries")
        }
    }

    fun notifyMessage(message: String) {
        _events.tryEmit(message)
    }

    class Factory(private val repository: AmmoRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AmmoViewModel::class.java)) {
                return AmmoViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
