package com.example

import com.example.data.AmmoEntry
import com.example.data.AmmoRepository
import com.example.data.AmmoSeekUrlBuilder
import com.example.data.AmmoSortOption
import com.example.data.CaliberTarget
import com.example.data.CaliberTotalProjection
import com.example.data.PriceClipboardParser
import com.example.data.PriceHistoryEntry
import com.example.data.PriceUnitMode
import com.example.data.RangeBagItem
import com.example.data.RangeSession
import com.example.data.RangeSessionAmmoUsage
import com.example.data.StarterRiflePresets
import com.example.data.StorageQrCodec
import com.example.data.StorageUnit
import com.example.data.UpcCatalog
import com.example.data.VaultBackupManager
import com.example.data.VaultBackupPayload
import com.example.ui.AmmoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

@OptIn(ExperimentalCoroutinesApi::class)
class AmmoViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeAmmoRepository : AmmoRepository {
        private val entriesFlow = MutableStateFlow<List<AmmoEntry>>(emptyList())
        private val targetsFlow = MutableStateFlow<List<CaliberTarget>>(emptyList())
        private val historyFlow = MutableStateFlow<List<PriceHistoryEntry>>(emptyList())
        private val rangeBagFlow = MutableStateFlow<List<RangeBagItem>>(emptyList())
        private val rangeSessionsFlow = MutableStateFlow<List<RangeSession>>(emptyList())
        private val rangeUsagesFlow = MutableStateFlow<List<RangeSessionAmmoUsage>>(emptyList())
        private val storageUnitsFlow = MutableStateFlow<List<StorageUnit>>(emptyList())
        private var nextId = 1L
        private var nextHistoryId = 1L
        private var nextBagItemId = 1L
        private var nextSessionId = 1L
        private var nextUsageId = 1L

        override val allEntries: Flow<List<AmmoEntry>> = entriesFlow
        override val allCaliberTargets: Flow<List<CaliberTarget>> = targetsFlow
        override val allPriceHistory: Flow<List<PriceHistoryEntry>> = historyFlow
        override val allRangeBagItems: Flow<List<RangeBagItem>> = rangeBagFlow
        override val allRangeSessions: Flow<List<RangeSession>> = rangeSessionsFlow
        override val allRangeSessionUsages: Flow<List<RangeSessionAmmoUsage>> = rangeUsagesFlow
        override val allStorageUnits: Flow<List<StorageUnit>> = storageUnitsFlow
        override val caliberTotals: Flow<List<CaliberTotalProjection>> = entriesFlow.map { entries ->
            entries.groupBy { it.caliber }.map { (caliber, group) ->
                CaliberTotalProjection(
                    caliber = caliber,
                    totalRounds = group.sumOf { it.currentCount },
                    lotCount = group.size,
                    totalMarketValue = group.sumOf { it.totalMarketValue },
                    avgPricePerRound = group.map { it.marketPricePerRound }.average()
                )
            }
        }

        override fun observeEntriesByCaliber(caliber: String): Flow<List<AmmoEntry>> =
            entriesFlow.map { list -> list.filter { it.caliber == caliber } }

        override fun observeEntry(id: Long): Flow<AmmoEntry?> =
            entriesFlow.map { list -> list.find { it.id == id } }

        override fun observePriceHistory(entryId: Long): Flow<List<PriceHistoryEntry>> =
            historyFlow.map { list ->
                list.filter { it.entryId == entryId }.sortedByDescending { it.recordedAt }
            }

        override suspend fun getEntry(id: Long): AmmoEntry? =
            entriesFlow.value.find { it.id == id }

        override suspend fun saveEntry(entry: AmmoEntry): Long {
            val current = entriesFlow.value.toMutableList()
            val isNew = entry.id == 0L
            val assignedId = if (isNew) nextId++ else entry.id
            val previous = current.find { it.id == assignedId }
            val updated = entry.copy(id = assignedId)
            val idx = current.indexOfFirst { it.id == assignedId }
            if (idx >= 0) current[idx] = updated else current.add(updated)
            entriesFlow.value = current
            val trimmedLoc = updated.storageLocation.trim()
            if (trimmedLoc.isNotEmpty() && storageUnitsFlow.value.none { it.name == trimmedLoc }) {
                storageUnitsFlow.value = (storageUnitsFlow.value + StorageUnit(trimmedLoc))
                    .sortedBy { it.name.lowercase() }
            }

            if (isNew || previous == null || abs(previous.marketPricePerRound - entry.marketPricePerRound) > 0.0001) {
                val snap = PriceHistoryEntry(
                    id = nextHistoryId++,
                    entryId = assignedId,
                    pricePerRound = entry.marketPricePerRound,
                    vendor = entry.vendor,
                    source = if (isNew) "Initial Entry" else "Entry Edit",
                    recordedAt = System.currentTimeMillis()
                )
                historyFlow.value = listOf(snap) + historyFlow.value
            }
            return assignedId
        }

        override suspend fun adjustRounds(entryId: Long, delta: Int) {
            entriesFlow.value = entriesFlow.value.map { entry ->
                if (entry.id == entryId) {
                    entry.copy(currentCount = (entry.currentCount + delta).coerceAtLeast(0))
                } else {
                    entry
                }
            }
        }

        override suspend fun setRoundsOnHand(entryId: Long, rounds: Int) {
            entriesFlow.value = entriesFlow.value.map { entry ->
                if (entry.id == entryId) entry.copy(currentCount = rounds.coerceAtLeast(0)) else entry
            }
        }

        override suspend fun updateMarketPrice(entryId: Long, newPricePerRound: Double, source: String) {
            val existing = entriesFlow.value.find { it.id == entryId }
            entriesFlow.value = entriesFlow.value.map { entry ->
                if (entry.id == entryId) entry.copy(marketPricePerRound = newPricePerRound) else entry
            }
            val snap = PriceHistoryEntry(
                id = nextHistoryId++,
                entryId = entryId,
                pricePerRound = newPricePerRound,
                vendor = existing?.vendor.orEmpty(),
                source = source,
                recordedAt = System.currentTimeMillis()
            )
            historyFlow.value = listOf(snap) + historyFlow.value
        }

        override suspend fun updateEntryBuyTarget(entryId: Long, targetBuyPricePerRound: Double?) {
            entriesFlow.value = entriesFlow.value.map { entry ->
                if (entry.id == entryId) entry.copy(targetBuyPricePerRound = targetBuyPricePerRound) else entry
            }
        }

        override suspend fun deleteEntry(entryId: Long) {
            entriesFlow.value = entriesFlow.value.filterNot { it.id == entryId }
            historyFlow.value = historyFlow.value.filterNot { it.entryId == entryId }
        }

        override suspend fun setCaliberTarget(
            caliber: String,
            targetRounds: Int,
            targetBuyPricePerRound: Double?,
            minThresholdRounds: Int?
        ) {
            val existing = targetsFlow.value.find { it.caliber == caliber }
            val resolvedMin = minThresholdRounds ?: existing?.minThresholdRounds ?: 0
            val current = targetsFlow.value.filterNot { it.caliber == caliber }.toMutableList()
            current.add(CaliberTarget(caliber, targetRounds, targetBuyPricePerRound, resolvedMin.coerceAtLeast(0)))
            targetsFlow.value = current
        }

        override suspend fun setCaliberMinThreshold(caliber: String, minThresholdRounds: Int) {
            val existing = targetsFlow.value.find { it.caliber == caliber }
            val current = targetsFlow.value.filterNot { it.caliber == caliber }.toMutableList()
            val targetRounds = existing?.targetRounds ?: StarterRiflePresets.defaultTargetForCaliber(caliber)
            current.add(
                CaliberTarget(
                    caliber = caliber,
                    targetRounds = targetRounds,
                    targetBuyPricePerRound = existing?.targetBuyPricePerRound,
                    minThresholdRounds = minThresholdRounds.coerceAtLeast(0)
                )
            )
            targetsFlow.value = current
        }

        override suspend fun deleteCaliberTarget(caliber: String) {
            targetsFlow.value = targetsFlow.value.filterNot { it.caliber == caliber }
        }

        override suspend fun renameCaliber(oldCaliber: String, newCaliber: String) {
            val trimmed = newCaliber.trim()
            if (trimmed.isEmpty() || oldCaliber == trimmed) return
            entriesFlow.value = entriesFlow.value.map { entry ->
                if (entry.caliber == oldCaliber) entry.copy(caliber = trimmed) else entry
            }
            val oldTarget = targetsFlow.value.find { it.caliber == oldCaliber }
            if (oldTarget != null) {
                val filtered = targetsFlow.value.filterNot { it.caliber == oldCaliber }.toMutableList()
                if (filtered.none { it.caliber == trimmed }) {
                    filtered.add(
                        CaliberTarget(
                            caliber = trimmed,
                            targetRounds = oldTarget.targetRounds,
                            targetBuyPricePerRound = oldTarget.targetBuyPricePerRound,
                            minThresholdRounds = oldTarget.minThresholdRounds
                        )
                    )
                }
                targetsFlow.value = filtered
            }
        }

        override suspend fun deleteCaliberInventory(caliber: String) {
            val removedIds = entriesFlow.value.filter { it.caliber == caliber }.map { it.id }.toSet()
            entriesFlow.value = entriesFlow.value.filterNot { it.caliber == caliber }
            historyFlow.value = historyFlow.value.filterNot { it.entryId in removedIds }
            targetsFlow.value = targetsFlow.value.filterNot { it.caliber == caliber }
        }

        override suspend fun addStorageUnit(name: String) {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) return
            if (storageUnitsFlow.value.none { it.name == trimmed }) {
                storageUnitsFlow.value = (storageUnitsFlow.value + StorageUnit(trimmed))
                    .sortedBy { it.name.lowercase() }
            }
        }

        override suspend fun renameStorageUnit(oldName: String, newName: String) {
            val trimmedOld = oldName.trim()
            val trimmedNew = newName.trim()
            if (trimmedOld.isEmpty() || trimmedNew.isEmpty() || trimmedOld == trimmedNew) return
            entriesFlow.value = entriesFlow.value.map { entry ->
                if (entry.storageLocation.trim() == trimmedOld) entry.copy(storageLocation = trimmedNew) else entry
            }
            rangeSessionsFlow.value = rangeSessionsFlow.value.map { session ->
                if (session.storageUnitName.trim() == trimmedOld) session.copy(storageUnitName = trimmedNew) else session
            }
            val updated = storageUnitsFlow.value.filterNot { it.name == trimmedOld }.toMutableList()
            if (updated.none { it.name == trimmedNew }) {
                updated.add(StorageUnit(trimmedNew))
            }
            storageUnitsFlow.value = updated.sortedBy { it.name.lowercase() }
        }

        override suspend fun deleteStorageUnit(name: String) {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) return
            entriesFlow.value = entriesFlow.value.map { entry ->
                if (entry.storageLocation.trim() == trimmed) entry.copy(storageLocation = "") else entry
            }
            storageUnitsFlow.value = storageUnitsFlow.value.filterNot { it.name == trimmed }
        }

        override suspend fun importBackup(payload: VaultBackupPayload, replaceExisting: Boolean): Int {
            if (replaceExisting) {
                entriesFlow.value = emptyList()
                targetsFlow.value = emptyList()
                historyFlow.value = emptyList()
            }
            val idMap = mutableMapOf<Long, Long>()
            val existingEntries = entriesFlow.value.toMutableList()
            payload.entries.forEach { entry ->
                val newId = nextId++
                if (entry.id > 0L) idMap[entry.id] = newId
                existingEntries.add(entry.copy(id = newId))
            }
            entriesFlow.value = existingEntries

            val addedHistory = payload.history.mapNotNull { snap ->
                val mappedId = idMap[snap.entryId] ?: return@mapNotNull null
                snap.copy(id = nextHistoryId++, entryId = mappedId)
            }
            historyFlow.value = addedHistory + historyFlow.value

            val currentTargets = targetsFlow.value.toMutableList()
            payload.targets.forEach { target ->
                if (replaceExisting || currentTargets.none { it.caliber == target.caliber }) {
                    currentTargets.removeAll { it.caliber == target.caliber }
                    currentTargets.add(target)
                }
            }
            targetsFlow.value = currentTargets
            return payload.entries.size
        }

        override suspend fun loadStarterEntries() {
            StarterRiflePresets.starterEntries().forEach { saveEntry(it) }
        }

        override suspend fun upsertRangeBagItem(bagName: String, entryId: Long, packedRounds: Int) {
            val current = rangeBagFlow.value.toMutableList()
            val idx = current.indexOfFirst { it.bagName == bagName && it.entryId == entryId }
            if (packedRounds <= 0) {
                if (idx >= 0) current.removeAt(idx)
            } else if (idx >= 0) {
                current[idx] = current[idx].copy(packedRounds = packedRounds)
            } else {
                current.add(
                    RangeBagItem(
                        id = nextBagItemId++,
                        bagName = bagName,
                        entryId = entryId,
                        packedRounds = packedRounds
                    )
                )
            }
            rangeBagFlow.value = current
        }

        override suspend fun deleteRangeBagItem(itemId: Long) {
            rangeBagFlow.value = rangeBagFlow.value.filterNot { it.id == itemId }
        }

        override suspend fun clearRangeBag(bagName: String) {
            rangeBagFlow.value = rangeBagFlow.value.filterNot { it.bagName == bagName }
        }

        override suspend fun logRangeSession(
            session: RangeSession,
            usages: List<RangeSessionAmmoUsage>,
            clearSourceBag: Boolean
        ): Long {
            val assignedSessionId = nextSessionId++
            rangeSessionsFlow.value = listOf(session.copy(id = assignedSessionId)) + rangeSessionsFlow.value
            val savedUsages = usages.map { usage ->
                usage.copy(id = nextUsageId++, sessionId = assignedSessionId)
            }
            rangeUsagesFlow.value = rangeUsagesFlow.value + savedUsages
            savedUsages.forEach { usage ->
                val eid = usage.entryId
                if (eid != null && usage.roundsUsed > 0) {
                    adjustRounds(eid, -usage.roundsUsed)
                }
            }
            if (clearSourceBag && session.rangeBagName.isNotBlank()) {
                clearRangeBag(session.rangeBagName)
            }
            return assignedSessionId
        }

        override suspend fun deleteRangeSession(sessionId: Long, restoreInventory: Boolean) {
            val usages = rangeUsagesFlow.value.filter { it.sessionId == sessionId }
            if (restoreInventory) {
                usages.forEach { usage ->
                    val eid = usage.entryId
                    if (eid != null && usage.roundsUsed > 0) {
                        adjustRounds(eid, usage.roundsUsed)
                    }
                }
            }
            rangeUsagesFlow.value = rangeUsagesFlow.value.filterNot { it.sessionId == sessionId }
            rangeSessionsFlow.value = rangeSessionsFlow.value.filterNot { it.id == sessionId }
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `caliber aggregation calculates rounds and market valuation accurately`() = runTest {
        val repo = FakeAmmoRepository()
        val vm = AmmoViewModel(repo)
        val collector = launch { vm.uiState.collect {} }

        vm.saveEntry(
            AmmoEntry(
                caliber = ".308 Win",
                brand = "Federal Premium",
                grain = 168,
                bulletType = "BTHP",
                currentCount = 100,
                marketPricePerRound = 1.50
            )
        )
        vm.saveEntry(
            AmmoEntry(
                caliber = ".308 Win",
                brand = "PMC",
                grain = 147,
                bulletType = "FMJ",
                currentCount = 200,
                marketPricePerRound = 0.75
            )
        )
        vm.saveEntry(
            AmmoEntry(
                caliber = "5.56 NATO",
                brand = "Hornady",
                grain = 55,
                bulletType = "FMJ",
                currentCount = 500,
                marketPricePerRound = 0.50
            )
        )
        advanceUntilIdle()

        val state = vm.uiState.first { !it.isLoading && it.allEntries.size == 3 }
        assertEquals(800, state.totalRounds)
        assertEquals(550.0, state.totalMarketValue, 0.01)
        assertEquals(2, state.caliberSummaries.size)

        val win308 = state.caliberSummaries.first { it.caliber == ".308 Win" }
        assertEquals(300, win308.totalRounds)
        assertEquals(300.0, win308.totalMarketValue, 0.01)
        assertEquals(1.0, win308.weightedAvgPricePerRound, 0.01)
        assertEquals(listOf(147, 168), win308.grainWeights)

        collector.cancel()
    }

    @Test
    fun `filtering by caliber and sorting by price works`() = runTest {
        val repo = FakeAmmoRepository()
        val vm = AmmoViewModel(repo)
        val collector = launch { vm.uiState.collect {} }

        vm.loadStarterEntries()
        advanceUntilIdle()

        vm.selectCaliberFilter("5.56 NATO")
        vm.setSortOption(AmmoSortOption.PRICE_DESC)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(2, state.filteredEntries.size)
        assertTrue(state.filteredEntries.all { it.caliber == "5.56 NATO" })
        assertTrue(state.filteredEntries[0].marketPricePerRound >= state.filteredEntries[1].marketPricePerRound)

        collector.cancel()
    }

    @Test
    fun `search matches vendor and preserves purchase metadata`() = runTest {
        val repo = FakeAmmoRepository()
        val vm = AmmoViewModel(repo)
        val collector = launch { vm.uiState.collect {} }
        val purchaseMillis = 1_735_689_600_000L

        vm.saveEntry(
            AmmoEntry(
                caliber = "6.5 Creedmoor",
                brand = "Hornady",
                grain = 140,
                bulletType = "ELD-M",
                currentCount = 120,
                marketPricePerRound = 1.75,
                vendor = "MidwayUSA",
                purchaseDate = purchaseMillis
            )
        )
        vm.saveEntry(
            AmmoEntry(
                caliber = "5.56 NATO",
                brand = "PMC",
                grain = 55,
                bulletType = "FMJ-BT",
                currentCount = 500,
                marketPricePerRound = 0.48,
                vendor = "SGAmmo",
                purchaseDate = purchaseMillis
            )
        )
        advanceUntilIdle()

        vm.updateSearchQuery("midway")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(1, state.filteredEntries.size)
        assertEquals("MidwayUSA", state.filteredEntries.first().vendor)
        assertEquals(purchaseMillis, state.filteredEntries.first().purchaseDate)

        collector.cancel()
    }

    @Test
    fun `price history records initial snapshot and AmmoSeek price updates`() = runTest {
        val repo = FakeAmmoRepository()
        val vm = AmmoViewModel(repo)
        val collector = launch { vm.uiState.collect {} }

        vm.saveEntry(
            AmmoEntry(
                caliber = ".308 Win",
                brand = "Federal Premium",
                grain = 168,
                bulletType = "BTHP",
                currentCount = 100,
                marketPricePerRound = 1.50,
                vendor = "MidwayUSA"
            )
        )
        advanceUntilIdle()

        val entry = vm.uiState.value.allEntries.first()
        vm.updateMarketPrice(entry, 1.32, source = "AmmoSeek Check")
        advanceUntilIdle()

        val state = vm.uiState.value
        val entryHistory = state.priceHistoryByEntryId[entry.id].orEmpty()
        assertEquals(2, entryHistory.size)
        assertEquals(1.32, state.allEntries.first().marketPricePerRound, 0.001)
        assertEquals("AmmoSeek Check", entryHistory.first().source)

        collector.cancel()
    }

    @Test
    fun `AmmoSeekUrlBuilder generates path-based caliber and grain URLs without restrictive ikw`() {
        val federal556 = AmmoEntry(
            caliber = "5.56 NATO",
            brand = "Federal Premium",
            grain = 77,
            bulletType = "BTHP",
            currentCount = 100,
            marketPricePerRound = 1.38
        )
        assertEquals(
            "https://ammoseek.com/ammo/5.56x45mm-nato/77grains",
            AmmoSeekUrlBuilder.buildSearchUrl(federal556)
        )
        assertEquals(
            "https://ammoseek.com/ammo/5.56x45mm-nato/77grains?ikw=Federal",
            AmmoSeekUrlBuilder.buildBrandSearchUrl(federal556)
        )
        assertEquals(
            "https://ammoseek.com/ammo/5.56x45mm-nato",
            AmmoSeekUrlBuilder.buildCaliberSearchUrl("5.56 NATO")
        )

        val hornady65 = AmmoEntry(
            caliber = "6.5 Creedmoor",
            brand = "Hornady",
            grain = 140,
            bulletType = "ELD-M",
            currentCount = 120,
            marketPricePerRound = 1.75
        )
        assertEquals(
            "https://ammoseek.com/ammo/6.5mm-creedmoor/140grains",
            AmmoSeekUrlBuilder.buildSearchUrl(hornady65)
        )

        val customCaliber = AmmoEntry(
            caliber = ".257 Roberts",
            brand = "Nosler",
            grain = 115,
            bulletType = "Ballistic Tip",
            currentCount = 40,
            marketPricePerRound = 2.10
        )
        assertEquals(
            "https://ammoseek.com/ammo/rifle?ikw=.257+Roberts",
            AmmoSeekUrlBuilder.buildSearchUrl(customCaliber)
        )
    }

    @Test
    fun `setRoundsOnHand renameCaliber and deleteCaliberInventory update state cleanly`() = runTest {
        val repo = FakeAmmoRepository()
        val vm = AmmoViewModel(repo)
        val collector = launch { vm.uiState.collect {} }

        vm.saveEntry(
            AmmoEntry(
                caliber = ".223 Rem",
                brand = "PMC",
                grain = 55,
                bulletType = "FMJ",
                currentCount = 100,
                marketPricePerRound = 0.45
            )
        )
        vm.setCaliberTarget(".223 Rem", 500)
        advanceUntilIdle()

        val savedEntry = vm.uiState.value.allEntries.first()
        vm.setRoundsOnHand(savedEntry, 247)
        vm.renameCaliber(".223 Rem", "5.56 NATO")
        advanceUntilIdle()

        val afterRename = vm.uiState.value
        assertEquals(1, afterRename.caliberSummaries.size)
        assertEquals("5.56 NATO", afterRename.caliberSummaries.first().caliber)
        assertEquals(247, afterRename.caliberSummaries.first().totalRounds)
        assertEquals(500, afterRename.caliberSummaries.first().targetRounds)

        vm.deleteCaliberInventory("5.56 NATO")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.allEntries.isEmpty())
        assertTrue(vm.uiState.value.caliberSummaries.isEmpty())

        collector.cancel()
    }

    @Test
    fun `cost basis unrealized gain loss restock queue and buy targets compute accurately`() = runTest {
        val repo = FakeAmmoRepository()
        val vm = AmmoViewModel(repo)
        val collector = launch { vm.uiState.collect {} }

        vm.saveEntry(
            AmmoEntry(
                caliber = "5.56 NATO",
                brand = "PMC",
                grain = 55,
                bulletType = "FMJ-BT",
                currentCount = 100,
                boxSize = 20,
                lowStockThreshold = 150,
                marketPricePerRound = 0.50,
                costBasis = 0.40,
                storageLocation = "Ammo Can #1"
            )
        )
        vm.setCaliberTarget("5.56 NATO", targetRounds = 500, targetBuyPricePerRound = 0.52)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(40.0, state.totalCostBasis, 0.01)
        assertEquals(50.0, state.totalMarketValue, 0.01)
        assertEquals(10.0, state.totalUnrealizedGainLoss, 0.01)

        assertEquals(1, state.restockLots.size)
        val restockItem = state.restockLots.first()
        assertTrue(restockItem.isAtOrBelowBuyTarget)
        assertEquals(0.52, restockItem.effectiveBuyTargetCpr ?: 0.0, 0.001)
        assertEquals(1, state.underParCalibers.size)
        assertEquals(400, state.underParCalibers.first().roundsToTarget)

        val saved = state.allEntries.first()
        vm.updateEntryBuyTarget(saved, 0.45)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.restockLots.first().isAtOrBelowBuyTarget)

        collector.cancel()
    }

    @Test
    fun `storage location filtering and csv export include storage location and cost basis`() = runTest {
        val repo = FakeAmmoRepository()
        val vm = AmmoViewModel(repo)
        val collector = launch { vm.uiState.collect {} }

        vm.saveEntry(
            AmmoEntry(
                caliber = ".308 Win",
                brand = "Federal",
                grain = 168,
                bulletType = "BTHP",
                currentCount = 80,
                marketPricePerRound = 1.60,
                costBasis = 1.40,
                storageLocation = "Safe Top Shelf"
            )
        )
        vm.saveEntry(
            AmmoEntry(
                caliber = "5.56 NATO",
                brand = "PMC",
                grain = 55,
                bulletType = "FMJ",
                currentCount = 400,
                marketPricePerRound = 0.48,
                storageLocation = "Ammo Can #1"
            )
        )
        advanceUntilIdle()

        assertEquals(listOf("Ammo Can #1", "Safe Top Shelf"), vm.uiState.value.activeStorageLocations)
        vm.selectLocationFilter("Safe Top Shelf")
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.filteredEntries.size)
        assertEquals(".308 Win", vm.uiState.value.filteredEntries.first().caliber)

        val csv = VaultBackupManager.exportCatalogToCsv(
            entries = vm.uiState.value.allEntries,
            targets = vm.uiState.value.caliberTargets
        )
        assertTrue(csv.contains("Storage Location"))
        assertTrue(csv.contains("Purchase CPR"))
        assertTrue(csv.contains("Safe Top Shelf"))

        collector.cancel()
    }

    @Test
    fun `importBackup supports both merge and replace modes`() = runTest {
        val repo = FakeAmmoRepository()
        val vm = AmmoViewModel(repo)
        val collector = launch { vm.uiState.collect {} }

        vm.saveEntry(
            AmmoEntry(
                caliber = "5.56 NATO",
                brand = "PMC",
                grain = 55,
                bulletType = "FMJ",
                currentCount = 200,
                marketPricePerRound = 0.48
            )
        )
        advanceUntilIdle()

        val payload = VaultBackupPayload(
            entries = listOf(
                AmmoEntry(
                    id = 99L,
                    caliber = ".308 Win",
                    brand = "Hornady",
                    grain = 168,
                    bulletType = "ELD-M",
                    currentCount = 60,
                    marketPricePerRound = 1.65,
                    storageLocation = "Ammo Can #2"
                )
            ),
            targets = listOf(CaliberTarget(".308 Win", 300, 1.50)),
            history = listOf(PriceHistoryEntry(id = 1L, entryId = 99L, pricePerRound = 1.65, source = "Backup")),
            exportedAt = 1_700_000_000_000L
        )

        vm.importBackup(payload, replaceExisting = false)
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.allEntries.size)

        vm.importBackup(payload, replaceExisting = true)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.allEntries.size)
        assertEquals(".308 Win", vm.uiState.value.allEntries.first().caliber)
        assertEquals(1, vm.uiState.value.allPriceHistory.size)

        collector.cancel()
    }

    @Test
    fun `PriceClipboardParser parses cents dollars box totals and unit conversions accurately`() {
        val centsParsed = PriceClipboardParser.parsePriceFromText("47.5¢/rd Brass New", boxSize = 20)
        assertNotNull(centsParsed)
        assertEquals(PriceUnitMode.CENTS_PER_ROUND, centsParsed!!.unitMode)
        assertEquals(0.475, centsParsed.pricePerRound, 0.0001)
        assertEquals("47.5", centsParsed.displayInputValue)

        val cprTextParsed = PriceClipboardParser.parsePriceFromText("138 cpr", boxSize = 20)
        assertNotNull(cprTextParsed)
        assertEquals(1.38, cprTextParsed!!.pricePerRound, 0.001)

        val dollarPerRound = PriceClipboardParser.parsePriceFromText("$0.52 / rd", boxSize = 20)
        assertNotNull(dollarPerRound)
        assertEquals(PriceUnitMode.DOLLARS_PER_ROUND, dollarPerRound!!.unitMode)
        assertEquals(0.52, dollarPerRound.pricePerRound, 0.001)

        val explicitBox = PriceClipboardParser.parsePriceFromText("$28.00 / 20 rds", boxSize = 20)
        assertNotNull(explicitBox)
        assertEquals(1.40, explicitBox!!.pricePerRound, 0.001)

        val inferredBox = PriceClipboardParser.parsePriceFromText("$29.00", boxSize = 20)
        assertNotNull(inferredBox)
        assertEquals(PriceUnitMode.BOX_TOTAL, inferredBox!!.unitMode)
        assertEquals(1.45, inferredBox.pricePerRound, 0.001)

        assertEquals(0.48, PriceClipboardParser.computeCpr("48", PriceUnitMode.CENTS_PER_ROUND, 20) ?: 0.0, 0.001)
        assertEquals(1.45, PriceClipboardParser.computeCpr("29.00", PriceUnitMode.BOX_TOTAL, 20) ?: 0.0, 0.001)
        assertNull(PriceClipboardParser.parsePriceFromText("In stock - ships free", boxSize = 20))
    }

    @Test
    fun `UpcCatalog normalizes EAN13 and matches built-in specs and vault lots`() {
        assertEquals("741569010115", UpcCatalog.normalizeUpc("0741569010115"))
        assertEquals("741569010115", UpcCatalog.normalizeUpc("741569-010115"))

        val pmcSpec = UpcCatalog.lookupBuiltIn("0741569010115")
        assertNotNull(pmcSpec)
        assertEquals("5.56 NATO", pmcSpec!!.caliber)
        assertEquals("PMC", pmcSpec.brand)
        assertEquals(55, pmcSpec.grain)

        val entries = StarterRiflePresets.starterEntries()
        val matched = UpcCatalog.findMatchingEntries("0090255815009".drop(1), entries)
        assertEquals(1, matched.size)
        assertEquals("6.5 Creedmoor", matched.first().caliber)
    }

    @Test
    fun `range bags and range logs deduct rounds compute burn forecasts and restore on delete`() = runTest {
        val repo = FakeAmmoRepository()
        val vm = AmmoViewModel(repo)
        val collector = launch { vm.uiState.collect {} }

        vm.saveEntry(
            AmmoEntry(
                caliber = "5.56 NATO",
                brand = "PMC",
                grain = 55,
                bulletType = "FMJ-BT",
                currentCount = 300,
                boxSize = 20,
                marketPricePerRound = 0.50,
                costBasis = 0.45,
                lotNumber = "PMC-55-A"
            )
        )
        advanceUntilIdle()

        val entry = vm.uiState.value.allEntries.first()
        vm.packLotInRangeBag("Carbine Drill Bag", entry, 100)
        advanceUntilIdle()

        assertEquals("Carbine Drill Bag", vm.uiState.value.selectedRangeBagName)
        assertEquals(1, vm.uiState.value.selectedRangeBagItems.size)
        assertEquals(100, vm.uiState.value.selectedRangeBagItems.first().bagItem.packedRounds)

        val session = RangeSession(
            sessionDateTime = System.currentTimeMillis() - 60_000L,
            location = "Outdoor 100yd Range",
            durationMinutes = 90,
            rangeBagName = "Carbine Drill Bag",
            notes = "Zero check + drills",
            photoUri = "file:///data/user/0/com.example/files/vault_photos/target.jpg"
        )
        val usage = RangeSessionAmmoUsage(
            entryId = entry.id,
            caliber = entry.caliber,
            brand = entry.brand,
            grain = entry.grain,
            bulletType = entry.bulletType,
            lotNumber = entry.lotNumber,
            roundsUsed = 100,
            costPerRoundSnapshot = entry.effectivePurchaseCpr
        )

        vm.logRangeSession(session = session, usages = listOf(usage), clearSourceBag = true)
        advanceUntilIdle()

        val afterLog = vm.uiState.value
        assertEquals(200, afterLog.allEntries.first().currentCount)
        assertTrue(afterLog.selectedRangeBagItems.isEmpty())
        assertEquals(1, afterLog.rangeSessions.size)
        assertEquals(100, afterLog.totalRoundsFiredAllTime)
        assertEquals(100, afterLog.totalRoundsFiredLast30Days)
        assertEquals(90, afterLog.totalRangeMinutesAllTime)
        assertEquals(45.0, afterLog.totalAmmoCostBurnedAllTime, 0.01)

        assertEquals(1, afterLog.caliberBurnForecasts.size)
        val forecast = afterLog.caliberBurnForecasts.first()
        assertEquals("5.56 NATO", forecast.caliber)
        assertEquals(200, forecast.currentRoundsOnHand)
        assertEquals(100.0, forecast.estimatedMonthlyBurnRate, 0.1)
        assertEquals(2.0, forecast.estimatedMonthsRemaining ?: 0.0, 0.1)

        val loggedSessionId = afterLog.rangeSessions.first().session.id
        vm.deleteRangeSession(loggedSessionId, restoreInventory = true)
        advanceUntilIdle()

        assertEquals(300, vm.uiState.value.allEntries.first().currentCount)
        assertTrue(vm.uiState.value.rangeSessions.isEmpty())

        collector.cancel()
    }

    @Test
    fun `StorageQrCodec round-trips can locations and estimates rifle ammo weight`() {
        val encoded = StorageQrCodec.encodeCanQr("Ammo Can #1 (5.56)")
        assertTrue(encoded.startsWith("ammovault://can/"))
        assertEquals("Ammo Can #1 (5.56)", StorageQrCodec.decodeCanQr(encoded))
        assertNull(StorageQrCodec.decodeCanQr("0741569010115"))
        assertNull(StorageQrCodec.decodeCanQr("https://ammoseek.com"))

        val weight556 = StorageQrCodec.estimateAmmoWeightLbs("5.56 NATO", 55, 1000)
        assertEquals(28.77, weight556, 0.5)

        val weight308 = StorageQrCodec.estimateAmmoWeightLbs(".308 Win", 168, 200)
        assertEquals(11.90, weight308, 0.5)

        assertEquals(0.0, StorageQrCodec.estimateAmmoWeightLbs("5.56 NATO", 55, 0), 0.001)
    }

    @Test
    fun `VaultBackupManager parseFromCsv imports both AmmoVault exports and third-party spreadsheets`() {
        val genericCsv = """
            Cartridge,Manufacturer,Load,Grains,Bullet,Qty,Can,Price Paid,CPR,Lot,Notes
            "5.56 NATO",PMC,"X-TAC M193",55,FMJ-BT,500,"Ammo Can #1",${'$'}0.42,${'$'}0.48,LOT-A,"Range training, bulk"
            ".308 Win",Federal,"Gold Medal",168,BTHP,100,"Safe Top Shelf",1.35,1.55,LOT-B,"Match zeroed"
        """.trimIndent()

        val parsed = VaultBackupManager.parseFromCsv(genericCsv).getOrThrow()
        assertEquals(2, parsed.entries.size)

        val first = parsed.entries[0]
        assertEquals("5.56 NATO", first.caliber)
        assertEquals("PMC", first.brand)
        assertEquals("X-TAC M193", first.productLine)
        assertEquals(55, first.grain)
        assertEquals(500, first.currentCount)
        assertEquals("Ammo Can #1", first.storageLocation)
        assertEquals(0.42, first.costBasis, 0.001)
        assertEquals(0.48, first.marketPricePerRound, 0.001)
        assertEquals("Range training, bulk", first.notes)

        val roundTripCsv = VaultBackupManager.exportCatalogToCsv(parsed.entries, emptyList())
        val reparsed = VaultBackupManager.parseFromCsv(roundTripCsv).getOrThrow()
        assertEquals(2, reparsed.entries.size)
        assertEquals(first.storageLocation, reparsed.entries[0].storageLocation)
        assertEquals(first.costBasis, reparsed.entries[0].costBasis, 0.001)
    }

    @Test
    fun `FIFO oldest lot detection storage summaries lot reliability and burn-rate restock alerts compute accurately`() = runTest {
        val repo = FakeAmmoRepository()
        val vm = AmmoViewModel(repo)
        val collector = launch { vm.uiState.collect {} }

        val now = System.currentTimeMillis()
        // Two in-stock 5.56 lots (older lot should be flagged FIFO)
        vm.saveEntry(
            AmmoEntry(
                caliber = "5.56 NATO",
                brand = "PMC",
                grain = 55,
                bulletType = "FMJ-BT",
                currentCount = 240,
                boxSize = 20,
                lowStockThreshold = 60,
                marketPricePerRound = 0.50,
                storageLocation = "Ammo Can #1",
                purchaseDate = now - 90L * 24 * 60 * 60 * 1000L
            )
        )
        vm.saveEntry(
            AmmoEntry(
                caliber = "5.56 NATO",
                brand = "IMI",
                grain = 77,
                bulletType = "Razor Core",
                currentCount = 60,
                boxSize = 20,
                lowStockThreshold = 20,
                marketPricePerRound = 1.15,
                storageLocation = "Ammo Can #1",
                purchaseDate = now - 5L * 24 * 60 * 60 * 1000L
            )
        )
        // Single .308 lot (should NOT be flagged FIFO since only 1 in-stock lot exists for .308)
        vm.saveEntry(
            AmmoEntry(
                caliber = ".308 Win",
                brand = "Federal",
                grain = 168,
                bulletType = "BTHP",
                currentCount = 100,
                boxSize = 20,
                lowStockThreshold = 40,
                marketPricePerRound = 1.50,
                storageLocation = "Safe Top Shelf"
            )
        )
        advanceUntilIdle()

        val initial = vm.uiState.value
        val older556 = initial.allEntries.first { it.brand == "PMC" }
        val newer556 = initial.allEntries.first { it.brand == "IMI" }
        val win308 = initial.allEntries.first { it.caliber == ".308 Win" }

        assertTrue(older556.id in initial.fifoOldestLotIds)
        assertFalse(newer556.id in initial.fifoOldestLotIds)
        assertFalse(win308.id in initial.fifoOldestLotIds)

        assertEquals(2, initial.storageSummaries.size)
        val can1 = initial.storageSummaries.first { it.locationName == "Ammo Can #1" }
        assertEquals(300, can1.totalRounds)
        assertEquals(2, can1.lotCount)
        assertTrue(can1.estimatedWeightLbs > 7.0)

        // Log a high-burn range session with precision metrics & malfunctions on older556
        vm.logRangeSession(
            session = RangeSession(
                sessionDateTime = now - 60_000L,
                location = "Outdoor 200yd Range",
                durationMinutes = 75,
                sessionType = "Zeroing",
                distanceYards = 100,
                bestGroupMoa = 0.85,
                avgVelocityFps = 2940
            ),
            usages = listOf(
                RangeSessionAmmoUsage(
                    entryId = older556.id,
                    caliber = older556.caliber,
                    brand = older556.brand,
                    grain = older556.grain,
                    bulletType = older556.bulletType,
                    roundsUsed = 200,
                    costPerRoundSnapshot = older556.effectivePurchaseCpr,
                    malfunctionsCount = 2
                )
            ),
            clearSourceBag = false
        )
        advanceUntilIdle()

        val afterSession = vm.uiState.value
        val savedSession = afterSession.rangeSessions.first()
        assertEquals("Zeroing", savedSession.session.sessionType)
        assertEquals(100, savedSession.session.distanceYards)
        assertEquals(0.85, savedSession.session.bestGroupMoa ?: 0.0, 0.001)
        assertEquals(2940, savedSession.session.avgVelocityFps)
        assertEquals(2, savedSession.totalMalfunctions)

        val reliability = afterSession.lotReliabilityByEntryId[older556.id]
        assertNotNull(reliability)
        assertEquals(200, reliability!!.totalRoundsFired)
        assertEquals(2, reliability.totalMalfunctions)
        assertEquals(99.0, reliability.reliabilityPercent, 0.05)

        // Remaining 5.56 = 100 rds (40 + 60), burn rate = 200 rds/mo => ~0.5 months left => triggers burn-rate alert (< 2.5 mo)
        assertEquals(1, afterSession.burnRateRestockAlerts.size)
        assertEquals("5.56 NATO", afterSession.burnRateRestockAlerts.first().caliber)

        collector.cancel()
    }

    @Test
    fun `storage unit add rename delete and range day integration work seamlessly`() = runTest {
        val repo = FakeAmmoRepository()
        val vm = AmmoViewModel(repo)
        val collector = launch { vm.uiState.collect {} }

        // 1. Add an empty storage unit before any lots exist
        vm.addStorageUnit("50 Cal Can #3")
        advanceUntilIdle()
        assertTrue("50 Cal Can #3" in vm.uiState.value.allStorageUnitNames)
        val emptyUnitSummary = vm.uiState.value.storageSummaries.first { it.locationName == "50 Cal Can #3" }
        assertEquals(0, emptyUnitSummary.lotCount)
        assertEquals(0, emptyUnitSummary.totalRounds)

        // 2. Save two lots into "Ammo Can #1"
        vm.saveEntry(
            AmmoEntry(
                caliber = "5.56 NATO",
                brand = "PMC",
                grain = 55,
                bulletType = "FMJ",
                currentCount = 200,
                boxSize = 20,
                marketPricePerRound = 0.48,
                storageLocation = "Ammo Can #1"
            )
        )
        vm.saveEntry(
            AmmoEntry(
                caliber = ".308 Win",
                brand = "Federal",
                grain = 168,
                bulletType = "BTHP",
                currentCount = 60,
                boxSize = 20,
                marketPricePerRound = 1.50,
                storageLocation = "Ammo Can #1"
            )
        )
        advanceUntilIdle()

        // 3. Pack entire storage unit into a range bag
        vm.packStorageUnitInRangeBag("Carbine Drill Bag", "Ammo Can #1")
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.selectedRangeBagItems.size)
        assertEquals(260, vm.uiState.value.selectedRangeBagItems.sumOf { it.bagItem.packedRounds })

        // 4. Log a range session using that storage unit
        val pmcEntry = vm.uiState.value.allEntries.first { it.brand == "PMC" }
        vm.logRangeSession(
            session = RangeSession(
                location = "Outdoor 100yd Range",
                durationMinutes = 60,
                storageUnitName = "Ammo Can #1",
                sessionType = "Practice"
            ),
            usages = listOf(
                RangeSessionAmmoUsage(
                    entryId = pmcEntry.id,
                    caliber = pmcEntry.caliber,
                    brand = pmcEntry.brand,
                    grain = pmcEntry.grain,
                    bulletType = pmcEntry.bulletType,
                    roundsUsed = 60,
                    costPerRoundSnapshot = pmcEntry.effectivePurchaseCpr
                )
            ),
            clearSourceBag = false
        )
        advanceUntilIdle()
        assertEquals("Ammo Can #1", vm.uiState.value.rangeSessions.first().session.storageUnitName)
        assertEquals(140, vm.uiState.value.allEntries.first { it.id == pmcEntry.id }.currentCount)

        // 5. Rename storage unit and verify lots, active location filter, and range session update
        vm.selectLocationFilter("Ammo Can #1")
        advanceUntilIdle()
        vm.renameStorageUnit("Ammo Can #1", "Range Day Can A")
        advanceUntilIdle()

        val afterRename = vm.uiState.value
        assertEquals("Range Day Can A", afterRename.selectedLocationFilter)
        assertTrue(afterRename.allEntries.all { it.storageLocation == "Range Day Can A" })
        assertEquals("Range Day Can A", afterRename.rangeSessions.first().session.storageUnitName)
        assertTrue("Range Day Can A" in afterRename.allStorageUnitNames)
        assertFalse("Ammo Can #1" in afterRename.allStorageUnitNames)

        // 6. Delete storage unit and verify lots are preserved as Unassigned Storage
        vm.deleteStorageUnit("Range Day Can A")
        advanceUntilIdle()

        val afterDelete = vm.uiState.value
        assertNull(afterDelete.selectedLocationFilter)
        assertFalse("Range Day Can A" in afterDelete.allStorageUnitNames)
        assertEquals(2, afterDelete.allEntries.size)
        assertTrue(afterDelete.allEntries.all { it.storageLocation.isEmpty() })
        assertTrue(afterDelete.storageSummaries.any { it.isUnassigned && it.lotCount == 2 })

        collector.cancel()
    }

    @Test
    fun `minimum thresholds trigger alerts on inventory drops clear on restock and persist across backup`() = runTest {
        val repo = FakeAmmoRepository()
        val vm = AmmoViewModel(repo)
        val emittedEvents = mutableListOf<String>()
        val stateCollector = launch { vm.uiState.collect {} }
        val eventCollector = launch { vm.events.collect { emittedEvents.add(it) } }

        vm.saveEntry(
            AmmoEntry(
                caliber = "5.56 NATO",
                brand = "PMC",
                grain = 55,
                bulletType = "FMJ-BT",
                currentCount = 300,
                boxSize = 20,
                marketPricePerRound = 0.48
            )
        )
        vm.saveEntry(
            AmmoEntry(
                caliber = ".308 Win",
                brand = "Federal",
                grain = 168,
                bulletType = "BTHP",
                currentCount = 120,
                boxSize = 20,
                marketPricePerRound = 1.55
            )
        )
        advanceUntilIdle()

        // 1. Configure minimum threshold for 5.56 NATO at 250 rds (currently 300 -> healthy)
        vm.setCaliberMinThreshold("5.56 NATO", 250)
        advanceUntilIdle()

        val initialState = vm.uiState.value
        assertEquals(2, initialState.thresholdConfigItems.size)
        assertTrue(initialState.belowMinThresholdAlerts.isEmpty())
        val summary556 = initialState.caliberSummaries.first { it.caliber == "5.56 NATO" }
        assertEquals(250, summary556.minThresholdRounds)
        assertFalse(summary556.isBelowMinThreshold)

        // 2. Apply smart defaults -> only unconfigured .308 Win gets default (60 rds), 5.56 stays 250
        vm.applyDefaultMinThresholds()
        advanceUntilIdle()
        assertEquals(250, vm.uiState.value.thresholdConfigItems.first { it.caliber == "5.56 NATO" }.minThresholdRounds)
        assertEquals(60, vm.uiState.value.thresholdConfigItems.first { it.caliber == ".308 Win" }.minThresholdRounds)

        // 3. Drop 5.56 NATO below 250 via adjustRounds -> triggers visual alert and emits drop snackbar
        val entry556 = vm.uiState.value.allEntries.first { it.caliber == "5.56 NATO" }
        emittedEvents.clear()
        vm.adjustRounds(entry556, -80)
        advanceUntilIdle()

        val afterDrop = vm.uiState.value
        assertEquals(1, afterDrop.belowMinThresholdAlerts.size)
        assertEquals("5.56 NATO", afterDrop.belowMinThresholdAlerts.first().caliber)
        assertEquals(220, afterDrop.belowMinThresholdAlerts.first().currentRounds)
        assertEquals(30, afterDrop.belowMinThresholdAlerts.first().roundsDeficit)
        assertTrue(afterDrop.caliberSummaries.first { it.caliber == "5.56 NATO" }.isBelowMinThreshold)
        assertTrue(emittedEvents.any { it.contains("Low Stock Alert") && it.contains("5.56 NATO") })

        // 4. Drop .308 Win below 60 via logRangeSession -> triggers second alert + alert event
        val entry308 = vm.uiState.value.allEntries.first { it.caliber == ".308 Win" }
        emittedEvents.clear()
        vm.logRangeSession(
            session = RangeSession(
                location = "200yd Range",
                durationMinutes = 45
            ),
            usages = listOf(
                RangeSessionAmmoUsage(
                    entryId = entry308.id,
                    caliber = entry308.caliber,
                    brand = entry308.brand,
                    grain = entry308.grain,
                    bulletType = entry308.bulletType,
                    roundsUsed = 80,
                    costPerRoundSnapshot = entry308.effectivePurchaseCpr
                )
            ),
            clearSourceBag = false
        )
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.belowMinThresholdAlerts.size)
        assertTrue(emittedEvents.any { it.contains("Low Stock Alert") && it.contains(".308 Win") })

        // 5. Pre-configure a 0-round ammunition type (9mm Luger) -> immediately shows in alerts, then removable
        vm.setCaliberMinThreshold("9mm Luger", 200)
        advanceUntilIdle()
        assertEquals(3, vm.uiState.value.belowMinThresholdAlerts.size)
        assertTrue(vm.uiState.value.belowMinThresholdAlerts.any { it.caliber == "9mm Luger" && it.currentRounds == 0 })

        vm.removeThresholdOnlyCaliber("9mm Luger")
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.belowMinThresholdAlerts.size)

        // 6. Restock 5.56 NATO back to 260 rds -> clears 5.56 NATO alert
        val updated556 = vm.uiState.value.allEntries.first { it.caliber == "5.56 NATO" }
        vm.setRoundsOnHand(updated556, 260)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.belowMinThresholdAlerts.size)
        assertEquals(".308 Win", vm.uiState.value.belowMinThresholdAlerts.first().caliber)

        // 7. Renaming a caliber preserves its configured minimum threshold
        vm.renameCaliber("5.56 NATO", ".223 Rem")
        advanceUntilIdle()
        assertEquals(250, vm.uiState.value.thresholdConfigItems.first { it.caliber == ".223 Rem" }.minThresholdRounds)

        // 8. Backup import preserves minThresholdRounds on CaliberTarget
        val backupPayload = VaultBackupPayload(
            entries = listOf(
                AmmoEntry(
                    id = 50L,
                    caliber = "6.5 Creedmoor",
                    brand = "Hornady",
                    grain = 140,
                    bulletType = "ELD-M",
                    currentCount = 40,
                    marketPricePerRound = 1.75
                )
            ),
            targets = listOf(CaliberTarget("6.5 Creedmoor", targetRounds = 300, targetBuyPricePerRound = 1.60, minThresholdRounds = 100)),
            history = emptyList(),
            exportedAt = 1_700_000_000_000L
        )
        vm.importBackup(backupPayload, replaceExisting = true)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.belowMinThresholdAlerts.size)
        val importedAlert = vm.uiState.value.belowMinThresholdAlerts.first()
        assertEquals("6.5 Creedmoor", importedAlert.caliber)
        assertEquals(100, importedAlert.minThresholdRounds)
        assertEquals(60, importedAlert.roundsDeficit)

        stateCollector.cancel()
        eventCollector.cancel()
    }

    @Test
    fun `caliberPriceSeries aggregates chronological CPR history per caliber and separates multi-lot lines`() = runTest {
        val repo = FakeAmmoRepository()
        val vm = AmmoViewModel(repo)
        val collector = launch { vm.uiState.collect {} }

        vm.saveEntry(
            AmmoEntry(
                caliber = "5.56 NATO",
                brand = "PMC",
                grain = 55,
                bulletType = "FMJ-BT",
                currentCount = 500,
                marketPricePerRound = 0.44,
                vendor = "SGAmmo"
            )
        )
        vm.saveEntry(
            AmmoEntry(
                caliber = "5.56 NATO",
                brand = "Federal",
                grain = 77,
                bulletType = "BTHP",
                currentCount = 100,
                marketPricePerRound = 1.30,
                vendor = "MidwayUSA"
            )
        )
        vm.saveEntry(
            AmmoEntry(
                caliber = ".308 Win",
                brand = "Hornady",
                grain = 168,
                bulletType = "ELD-M",
                currentCount = 80,
                marketPricePerRound = 1.65,
                vendor = "Brownells"
            )
        )
        vm.setCaliberTarget("5.56 NATO", targetRounds = 1000, targetBuyPricePerRound = 0.45)
        advanceUntilIdle()

        val pmc556 = vm.uiState.value.allEntries.first { it.brand == "PMC" }
        val hornady308 = vm.uiState.value.allEntries.first { it.caliber == ".308 Win" }

        vm.updateMarketPrice(pmc556, 0.52, source = "AmmoSeek Check")
        vm.updateMarketPrice(hornady308, 1.50, source = "AmmoSeek Check")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(2, state.caliberPriceSeries.size)

        val series556 = state.caliberPriceSeries.first { it.caliber == "5.56 NATO" }
        assertEquals(3, series556.snapshotCount)
        assertEquals(2, series556.lotLines.size)
        assertEquals(0.44, series556.minPricePerRound, 0.001)
        assertEquals(1.30, series556.maxPricePerRound, 0.001)
        assertEquals(0.45, series556.targetBuyPricePerRound ?: 0.0, 0.001)
        // Verify PMC lot has 2 chronological points (0.44 -> 0.52)
        val pmcLine = series556.lotLines.first { it.entryId == pmc556.id }
        assertEquals(2, pmcLine.points.size)
        assertEquals(0.44, pmcLine.points.first().pricePerRound, 0.001)
        assertEquals(0.52, pmcLine.points.last().pricePerRound, 0.001)

        val series308 = state.caliberPriceSeries.first { it.caliber == ".308 Win" }
        assertEquals(2, series308.snapshotCount)
        assertEquals(1, series308.lotLines.size)
        assertEquals(1.65, series308.earliestPricePerRound, 0.001)
        assertEquals(1.50, series308.latestPricePerRound, 0.001)
        assertEquals(-0.15, series308.priceDelta, 0.001)

        collector.cancel()
    }
}

