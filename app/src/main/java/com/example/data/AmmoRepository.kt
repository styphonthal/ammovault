package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlin.math.abs

interface AmmoRepository {
    val allEntries: Flow<List<AmmoEntry>>
    val allCaliberTargets: Flow<List<CaliberTarget>>
    val allPriceHistory: Flow<List<PriceHistoryEntry>>
    val caliberTotals: Flow<List<CaliberTotalProjection>>
    val allRangeBagItems: Flow<List<RangeBagItem>>
    val allRangeSessions: Flow<List<RangeSession>>
    val allRangeSessionUsages: Flow<List<RangeSessionAmmoUsage>>
    val allStorageUnits: Flow<List<StorageUnit>>

    fun observeEntriesByCaliber(caliber: String): Flow<List<AmmoEntry>>
    fun observeEntry(id: Long): Flow<AmmoEntry?>
    fun observePriceHistory(entryId: Long): Flow<List<PriceHistoryEntry>>
    suspend fun getEntry(id: Long): AmmoEntry?
    suspend fun saveEntry(entry: AmmoEntry): Long
    suspend fun adjustRounds(entryId: Long, delta: Int)
    suspend fun setRoundsOnHand(entryId: Long, rounds: Int)
    suspend fun updateMarketPrice(entryId: Long, newPricePerRound: Double, source: String = "Market Update")
    suspend fun updateEntryBuyTarget(entryId: Long, targetBuyPricePerRound: Double?)
    suspend fun deleteEntry(entryId: Long)
    suspend fun setCaliberTarget(
        caliber: String,
        targetRounds: Int,
        targetBuyPricePerRound: Double? = null,
        minThresholdRounds: Int? = null
    )
    suspend fun setCaliberMinThreshold(caliber: String, minThresholdRounds: Int)
    suspend fun deleteCaliberTarget(caliber: String)
    suspend fun renameCaliber(oldCaliber: String, newCaliber: String)
    suspend fun deleteCaliberInventory(caliber: String)
    suspend fun addStorageUnit(name: String)
    suspend fun renameStorageUnit(oldName: String, newName: String)
    suspend fun deleteStorageUnit(name: String)
    suspend fun upsertRangeBagItem(bagName: String, entryId: Long, packedRounds: Int)
    suspend fun deleteRangeBagItem(itemId: Long)
    suspend fun clearRangeBag(bagName: String)
    suspend fun logRangeSession(
        session: RangeSession,
        usages: List<RangeSessionAmmoUsage>,
        clearSourceBag: Boolean
    ): Long
    suspend fun deleteRangeSession(sessionId: Long, restoreInventory: Boolean)
    suspend fun importBackup(payload: VaultBackupPayload, replaceExisting: Boolean): Int
    suspend fun loadStarterEntries()
}

class RoomAmmoRepository(private val dao: AmmoDao) : AmmoRepository {
    override val allEntries: Flow<List<AmmoEntry>> = dao.observeAllEntries()
    override val allCaliberTargets: Flow<List<CaliberTarget>> = dao.observeAllCaliberTargets()
    override val allPriceHistory: Flow<List<PriceHistoryEntry>> = dao.observeAllPriceHistory()
    override val caliberTotals: Flow<List<CaliberTotalProjection>> = dao.observeCaliberTotals()
    override val allRangeBagItems: Flow<List<RangeBagItem>> = dao.observeAllRangeBagItems()
    override val allRangeSessions: Flow<List<RangeSession>> = dao.observeAllRangeSessions()
    override val allRangeSessionUsages: Flow<List<RangeSessionAmmoUsage>> = dao.observeAllRangeSessionUsages()
    override val allStorageUnits: Flow<List<StorageUnit>> = dao.observeAllStorageUnits()

    override fun observeEntriesByCaliber(caliber: String): Flow<List<AmmoEntry>> =
        dao.observeEntriesByCaliber(caliber)

    override fun observeEntry(id: Long): Flow<AmmoEntry?> = dao.observeEntryById(id)

    override fun observePriceHistory(entryId: Long): Flow<List<PriceHistoryEntry>> =
        dao.observePriceHistoryForEntry(entryId)

    override suspend fun getEntry(id: Long): AmmoEntry? = dao.getEntryById(id)

    override suspend fun saveEntry(entry: AmmoEntry): Long {
        val now = System.currentTimeMillis()
        val trimmedLocation = entry.storageLocation.trim()
        if (trimmedLocation.isNotEmpty()) {
            dao.insertStorageUnit(StorageUnit(name = trimmedLocation, createdAt = now))
        }
        return if (entry.id == 0L) {
            val newId = dao.insertEntry(entry.copy(createdAt = now, updatedAt = now))
            val paidCpr = entry.costBasis
            if (paidCpr > 0.0 && abs(paidCpr - entry.marketPricePerRound) > 0.0001) {
                dao.insertPriceHistory(
                    PriceHistoryEntry(
                        entryId = newId,
                        pricePerRound = paidCpr,
                        vendor = entry.vendor,
                        source = "Purchase Cost Basis",
                        recordedAt = entry.purchaseDate ?: (now - 1000L)
                    )
                )
            }
            dao.insertPriceHistory(
                PriceHistoryEntry(
                    entryId = newId,
                    pricePerRound = entry.marketPricePerRound,
                    vendor = entry.vendor,
                    source = if (entry.purchaseDate != null && paidCpr <= 0.0) "Purchase Log" else "Initial Market Check",
                    recordedAt = now
                )
            )
            newId
        } else {
            val previous = dao.getEntryById(entry.id)
            val updated = entry.copy(updatedAt = now)
            dao.updateEntry(updated)
            if (previous == null || abs(previous.marketPricePerRound - entry.marketPricePerRound) > 0.0001) {
                dao.insertPriceHistory(
                    PriceHistoryEntry(
                        entryId = entry.id,
                        pricePerRound = entry.marketPricePerRound,
                        vendor = entry.vendor,
                        source = "Entry Edit",
                        recordedAt = now
                    )
                )
            }
            entry.id
        }
    }

    override suspend fun adjustRounds(entryId: Long, delta: Int) {
        dao.adjustRounds(id = entryId, delta = delta)
    }

    override suspend fun setRoundsOnHand(entryId: Long, rounds: Int) {
        dao.setRoundsOnHand(id = entryId, rounds = rounds.coerceAtLeast(0))
    }

    override suspend fun updateMarketPrice(entryId: Long, newPricePerRound: Double, source: String) {
        dao.updateMarketPriceWithHistory(
            id = entryId,
            pricePerRound = newPricePerRound.coerceAtLeast(0.0),
            source = source
        )
    }

    override suspend fun updateEntryBuyTarget(entryId: Long, targetBuyPricePerRound: Double?) {
        dao.updateEntryBuyTarget(
            id = entryId,
            targetBuyPricePerRound = targetBuyPricePerRound?.coerceAtLeast(0.0)
        )
    }

    override suspend fun deleteEntry(entryId: Long) {
        dao.deleteEntryById(entryId)
    }

    override suspend fun setCaliberTarget(
        caliber: String,
        targetRounds: Int,
        targetBuyPricePerRound: Double?,
        minThresholdRounds: Int?
    ) {
        val existing = dao.getCaliberTarget(caliber)
        val resolvedMin = (minThresholdRounds ?: existing?.minThresholdRounds ?: 0).coerceAtLeast(0)
        dao.upsertCaliberTarget(
            CaliberTarget(
                caliber = caliber,
                targetRounds = targetRounds.coerceAtLeast(1),
                targetBuyPricePerRound = targetBuyPricePerRound?.coerceAtLeast(0.0),
                minThresholdRounds = resolvedMin
            )
        )
    }

    override suspend fun setCaliberMinThreshold(caliber: String, minThresholdRounds: Int) {
        dao.setCaliberMinThreshold(
            caliber = caliber,
            minThresholdRounds = minThresholdRounds.coerceAtLeast(0),
            defaultTargetRounds = StarterRiflePresets.defaultTargetForCaliber(caliber)
        )
    }

    override suspend fun deleteCaliberTarget(caliber: String) {
        dao.deleteCaliberTarget(caliber)
    }

    override suspend fun renameCaliber(oldCaliber: String, newCaliber: String) {
        dao.renameCaliber(oldCaliber = oldCaliber, newCaliber = newCaliber)
    }

    override suspend fun deleteCaliberInventory(caliber: String) {
        dao.deleteCaliberInventory(caliber)
    }

    override suspend fun addStorageUnit(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) {
            dao.insertStorageUnit(StorageUnit(name = trimmed))
        }
    }

    override suspend fun renameStorageUnit(oldName: String, newName: String) {
        dao.renameStorageUnit(oldName = oldName, newName = newName)
    }

    override suspend fun deleteStorageUnit(name: String) {
        dao.deleteStorageUnit(name)
    }

    override suspend fun upsertRangeBagItem(bagName: String, entryId: Long, packedRounds: Int) {
        val trimmedBag = bagName.trim().ifEmpty { "Primary Range Bag" }
        val existing = dao.getRangeBagItem(trimmedBag, entryId)
        val clamped = packedRounds.coerceAtLeast(0)
        if (clamped <= 0) {
            if (existing != null) dao.deleteRangeBagItemById(existing.id)
            return
        }
        dao.upsertRangeBagItem(
            RangeBagItem(
                id = existing?.id ?: 0L,
                bagName = trimmedBag,
                entryId = entryId,
                packedRounds = clamped,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteRangeBagItem(itemId: Long) {
        dao.deleteRangeBagItemById(itemId)
    }

    override suspend fun clearRangeBag(bagName: String) {
        dao.clearRangeBag(bagName)
    }

    override suspend fun logRangeSession(
        session: RangeSession,
        usages: List<RangeSessionAmmoUsage>,
        clearSourceBag: Boolean
    ): Long {
        return dao.logRangeSessionAndDeduct(
            session = session,
            usages = usages,
            clearSourceBag = clearSourceBag
        )
    }

    override suspend fun deleteRangeSession(sessionId: Long, restoreInventory: Boolean) {
        dao.deleteRangeSessionWithOption(sessionId = sessionId, restoreInventory = restoreInventory)
    }

    override suspend fun importBackup(payload: VaultBackupPayload, replaceExisting: Boolean): Int {
        return dao.importBackupData(payload = payload, replaceExisting = replaceExisting)
    }

    override suspend fun loadStarterEntries() {
        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L
        val entries = StarterRiflePresets.starterEntries(now)
        val snapshots = mutableListOf<PriceHistoryEntry>()

        entries.forEach { entry ->
            val insertedId = dao.insertEntry(entry)
            val loc = entry.storageLocation.trim()
            if (loc.isNotEmpty()) {
                dao.insertStorageUnit(StorageUnit(name = loc, createdAt = now))
            }
            val baselineCpr = if (entry.costBasis > 0.0) entry.costBasis else entry.marketPricePerRound
            val midCpr = ((baselineCpr + entry.marketPricePerRound) / 2.0)
                .let { Math.round(it * 100.0) / 100.0 }

            snapshots += PriceHistoryEntry(
                entryId = insertedId,
                pricePerRound = baselineCpr,
                vendor = entry.vendor,
                source = "Purchase Cost Basis",
                recordedAt = entry.purchaseDate ?: (now - dayMs * 30)
            )
            snapshots += PriceHistoryEntry(
                entryId = insertedId,
                pricePerRound = midCpr,
                vendor = "AmmoSeek Check",
                source = "AmmoSeek Check",
                recordedAt = now - dayMs * 9
            )
            snapshots += PriceHistoryEntry(
                entryId = insertedId,
                pricePerRound = entry.marketPricePerRound,
                vendor = entry.vendor,
                source = "Market Update",
                recordedAt = entry.updatedAt
            )
        }

        if (snapshots.isNotEmpty()) {
            dao.insertAllPriceHistory(snapshots)
        }

        val defaultTargets = mapOf(
            "5.56 NATO" to 0.45,
            ".308 Win" to 1.50,
            "6.5 Creedmoor" to 1.65,
            ".300 BLK" to 1.50
        )

        entries.map { it.caliber }.distinct().forEach { caliber ->
            dao.upsertCaliberTarget(
                CaliberTarget(
                    caliber = caliber,
                    targetRounds = StarterRiflePresets.defaultTargetForCaliber(caliber),
                    targetBuyPricePerRound = defaultTargets[caliber],
                    minThresholdRounds = StarterRiflePresets.defaultMinThresholdForCaliber(caliber)
                )
            )
        }
    }
}
