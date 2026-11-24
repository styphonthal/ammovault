package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class CaliberTotalProjection(
    val caliber: String,
    val totalRounds: Int,
    val lotCount: Int,
    val totalMarketValue: Double,
    val avgPricePerRound: Double
)

@Dao
interface AmmoDao {
    // --- Read Queries ---

    @Query("SELECT * FROM ammo_entries ORDER BY updatedAt DESC")
    fun observeAllEntries(): Flow<List<AmmoEntry>>

    @Query("SELECT * FROM ammo_entries WHERE caliber = :caliber ORDER BY grain ASC, brand ASC")
    fun observeEntriesByCaliber(caliber: String): Flow<List<AmmoEntry>>

    @Query("SELECT * FROM ammo_entries WHERE id = :id")
    fun observeEntryById(id: Long): Flow<AmmoEntry?>

    @Query("SELECT * FROM ammo_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): AmmoEntry?

    @Query(
        """
        SELECT
            caliber,
            COALESCE(SUM(currentCount), 0) AS totalRounds,
            COUNT(*) AS lotCount,
            COALESCE(SUM(currentCount * marketPricePerRound), 0.0) AS totalMarketValue,
            COALESCE(AVG(marketPricePerRound), 0.0) AS avgPricePerRound
        FROM ammo_entries
        GROUP BY caliber
        ORDER BY LOWER(caliber) ASC
        """
    )
    fun observeCaliberTotals(): Flow<List<CaliberTotalProjection>>

    // --- Entry Create / Update / Delete ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: AmmoEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllEntries(entries: List<AmmoEntry>)

    @Update
    suspend fun updateEntry(entry: AmmoEntry)

    @Delete
    suspend fun deleteEntry(entry: AmmoEntry)

    @Query("DELETE FROM ammo_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)

    // --- Round Count & Market Price Updates ---

    @Query("UPDATE ammo_entries SET currentCount = MAX(0, currentCount + :delta), updatedAt = :updatedAt WHERE id = :id")
    suspend fun adjustRounds(id: Long, delta: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE ammo_entries SET currentCount = MAX(0, :rounds), updatedAt = :updatedAt WHERE id = :id")
    suspend fun setRoundsOnHand(id: Long, rounds: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE ammo_entries SET marketPricePerRound = :pricePerRound, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateMarketPrice(id: Long, pricePerRound: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE ammo_entries SET costBasis = :costBasis, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateCostBasis(id: Long, costBasis: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE ammo_entries SET targetBuyPricePerRound = :targetBuyPricePerRound, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateEntryBuyTarget(
        id: Long,
        targetBuyPricePerRound: Double?,
        updatedAt: Long = System.currentTimeMillis()
    )

    // --- Price History Queries & Writes ---

    @Query("SELECT * FROM price_history ORDER BY recordedAt DESC, id DESC")
    fun observeAllPriceHistory(): Flow<List<PriceHistoryEntry>>

    @Query("SELECT * FROM price_history WHERE entryId = :entryId ORDER BY recordedAt DESC, id DESC")
    fun observePriceHistoryForEntry(entryId: Long): Flow<List<PriceHistoryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceHistory(snapshot: PriceHistoryEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPriceHistory(snapshots: List<PriceHistoryEntry>)

    @Transaction
    suspend fun updateMarketPriceWithHistory(
        id: Long,
        pricePerRound: Double,
        source: String = "Market Update",
        vendorOverride: String? = null,
        updatedAt: Long = System.currentTimeMillis()
    ) {
        val clamped = pricePerRound.coerceAtLeast(0.0)
        val existing = getEntryById(id)
        updateMarketPrice(id = id, pricePerRound = clamped, updatedAt = updatedAt)
        insertPriceHistory(
            PriceHistoryEntry(
                entryId = id,
                pricePerRound = clamped,
                vendor = vendorOverride ?: existing?.vendor.orEmpty(),
                source = source,
                recordedAt = updatedAt
            )
        )
    }

    // --- Caliber CRUD & Transactional Operations ---

    @Query("SELECT * FROM caliber_targets")
    fun observeAllCaliberTargets(): Flow<List<CaliberTarget>>

    @Query("SELECT * FROM caliber_targets WHERE caliber = :caliber LIMIT 1")
    suspend fun getCaliberTarget(caliber: String): CaliberTarget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCaliberTarget(target: CaliberTarget)

    @Query("UPDATE ammo_entries SET caliber = :newCaliber, updatedAt = :updatedAt WHERE caliber = :oldCaliber")
    suspend fun renameEntriesCaliber(
        oldCaliber: String,
        newCaliber: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM ammo_entries WHERE caliber = :caliber")
    suspend fun deleteEntriesByCaliber(caliber: String)

    @Query("DELETE FROM caliber_targets WHERE caliber = :caliber")
    suspend fun deleteCaliberTarget(caliber: String)

    @Query("DELETE FROM ammo_entries")
    suspend fun deleteAllEntries()

    @Query("DELETE FROM caliber_targets")
    suspend fun deleteAllCaliberTargets()

    @Transaction
    suspend fun renameCaliber(oldCaliber: String, newCaliber: String) {
        val trimmedNew = newCaliber.trim()
        if (trimmedNew.isEmpty() || oldCaliber == trimmedNew) return
        renameEntriesCaliber(oldCaliber = oldCaliber, newCaliber = trimmedNew)
        val oldTarget = getCaliberTarget(oldCaliber)
        if (oldTarget != null) {
            val existingNewTarget = getCaliberTarget(trimmedNew)
            if (existingNewTarget == null) {
                upsertCaliberTarget(
                    CaliberTarget(
                        caliber = trimmedNew,
                        targetRounds = oldTarget.targetRounds,
                        targetBuyPricePerRound = oldTarget.targetBuyPricePerRound,
                        minThresholdRounds = oldTarget.minThresholdRounds
                    )
                )
            }
            deleteCaliberTarget(oldCaliber)
        }
    }

    @Transaction
    suspend fun setCaliberMinThreshold(
        caliber: String,
        minThresholdRounds: Int,
        defaultTargetRounds: Int
    ) {
        val trimmed = caliber.trim()
        if (trimmed.isEmpty()) return
        val clampedMin = minThresholdRounds.coerceAtLeast(0)
        val existing = getCaliberTarget(trimmed)
        if (existing != null) {
            upsertCaliberTarget(existing.copy(minThresholdRounds = clampedMin))
        } else {
            upsertCaliberTarget(
                CaliberTarget(
                    caliber = trimmed,
                    targetRounds = defaultTargetRounds.coerceAtLeast(1),
                    targetBuyPricePerRound = null,
                    minThresholdRounds = clampedMin
                )
            )
        }
    }

    @Transaction
    suspend fun deleteCaliberInventory(caliber: String) {
        deleteEntriesByCaliber(caliber)
        deleteCaliberTarget(caliber)
    }

    // --- Storage Units (Ammo Cans / Safes / Shelves) ---

    @Query("SELECT * FROM storage_units ORDER BY LOWER(name) ASC")
    fun observeAllStorageUnits(): Flow<List<StorageUnit>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStorageUnit(unit: StorageUnit)

    @Query("DELETE FROM storage_units WHERE name = :name")
    suspend fun deleteStorageUnitByName(name: String)

    @Query("DELETE FROM storage_units")
    suspend fun deleteAllStorageUnits()

    @Query("UPDATE ammo_entries SET storageLocation = :newName, updatedAt = :updatedAt WHERE TRIM(storageLocation) = :oldName")
    suspend fun renameEntriesStorageLocation(
        oldName: String,
        newName: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE ammo_entries SET storageLocation = '', updatedAt = :updatedAt WHERE TRIM(storageLocation) = :name")
    suspend fun clearEntriesStorageLocation(
        name: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE range_sessions SET storageUnitName = :newName WHERE TRIM(storageUnitName) = :oldName")
    suspend fun renameSessionsStorageUnit(oldName: String, newName: String)

    @Transaction
    suspend fun renameStorageUnit(oldName: String, newName: String) {
        val trimmedOld = oldName.trim()
        val trimmedNew = newName.trim()
        if (trimmedOld.isEmpty() || trimmedNew.isEmpty() || trimmedOld == trimmedNew) return
        val now = System.currentTimeMillis()
        insertStorageUnit(StorageUnit(name = trimmedNew, createdAt = now))
        renameEntriesStorageLocation(oldName = trimmedOld, newName = trimmedNew, updatedAt = now)
        renameSessionsStorageUnit(oldName = trimmedOld, newName = trimmedNew)
        deleteStorageUnitByName(trimmedOld)
    }

    @Transaction
    suspend fun deleteStorageUnit(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        clearEntriesStorageLocation(name = trimmed)
        deleteStorageUnitByName(trimmed)
    }

    // --- Virtual Range Bag & Range Session Logs ---

    @Query("SELECT * FROM range_bag_items ORDER BY bagName ASC, updatedAt DESC")
    fun observeAllRangeBagItems(): Flow<List<RangeBagItem>>

    @Query("SELECT * FROM range_bag_items WHERE bagName = :bagName AND entryId = :entryId LIMIT 1")
    suspend fun getRangeBagItem(bagName: String, entryId: Long): RangeBagItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRangeBagItem(item: RangeBagItem): Long

    @Query("DELETE FROM range_bag_items WHERE id = :id")
    suspend fun deleteRangeBagItemById(id: Long)

    @Query("DELETE FROM range_bag_items WHERE bagName = :bagName")
    suspend fun clearRangeBag(bagName: String)

    @Query("DELETE FROM range_bag_items")
    suspend fun deleteAllRangeBagItems()

    @Query("SELECT * FROM range_sessions ORDER BY sessionDateTime DESC, id DESC")
    fun observeAllRangeSessions(): Flow<List<RangeSession>>

    @Query("SELECT * FROM range_session_usage ORDER BY id ASC")
    fun observeAllRangeSessionUsages(): Flow<List<RangeSessionAmmoUsage>>

    @Query("SELECT * FROM range_session_usage WHERE sessionId = :sessionId")
    suspend fun getUsagesForSession(sessionId: Long): List<RangeSessionAmmoUsage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRangeSession(session: RangeSession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRangeSessionUsages(usages: List<RangeSessionAmmoUsage>)

    @Query("DELETE FROM range_sessions WHERE id = :id")
    suspend fun deleteRangeSessionById(id: Long)

    @Query("DELETE FROM range_sessions")
    suspend fun deleteAllRangeSessions()

    @Transaction
    suspend fun logRangeSessionAndDeduct(
        session: RangeSession,
        usages: List<RangeSessionAmmoUsage>,
        clearSourceBag: Boolean
    ): Long {
        val now = System.currentTimeMillis()
        val sessionId = insertRangeSession(session.copy(id = 0L, createdAt = now))
        val validUsages = usages.filter { it.roundsUsed > 0 }
        if (validUsages.isNotEmpty()) {
            insertRangeSessionUsages(validUsages.map { it.copy(id = 0L, sessionId = sessionId) })
            validUsages.forEach { usage ->
                val targetEntryId = usage.entryId
                if (targetEntryId != null && targetEntryId > 0L) {
                    adjustRounds(id = targetEntryId, delta = -usage.roundsUsed, updatedAt = now)
                }
            }
        }
        if (clearSourceBag && session.rangeBagName.isNotBlank()) {
            clearRangeBag(session.rangeBagName)
        }
        return sessionId
    }

    @Transaction
    suspend fun deleteRangeSessionWithOption(
        sessionId: Long,
        restoreInventory: Boolean
    ) {
        if (restoreInventory) {
            val now = System.currentTimeMillis()
            val usages = getUsagesForSession(sessionId)
            usages.forEach { usage ->
                val targetEntryId = usage.entryId
                if (targetEntryId != null && targetEntryId > 0L && usage.roundsUsed > 0) {
                    adjustRounds(id = targetEntryId, delta = usage.roundsUsed, updatedAt = now)
                }
            }
        }
        deleteRangeSessionById(sessionId)
    }

    @Transaction
    suspend fun importBackupData(
        payload: VaultBackupPayload,
        replaceExisting: Boolean
    ): Int {
        if (replaceExisting) {
            deleteAllRangeSessions()
            deleteAllRangeBagItems()
            deleteAllEntries()
            deleteAllCaliberTargets()
            deleteAllStorageUnits()
        }

        val now = System.currentTimeMillis()
        payload.storageUnits
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .forEach { unitName ->
                insertStorageUnit(StorageUnit(name = unitName, createdAt = now))
            }

        val idMap = mutableMapOf<Long, Long>()
        payload.entries.forEach { entry ->
            val oldId = entry.id
            val newId = insertEntry(entry.copy(id = 0L))
            val loc = entry.storageLocation.trim()
            if (loc.isNotEmpty()) {
                insertStorageUnit(StorageUnit(name = loc, createdAt = now))
            }
            if (oldId > 0L) {
                idMap[oldId] = newId
            }
        }

        val remappedHistory = payload.history.mapNotNull { snap ->
            val mappedEntryId = idMap[snap.entryId] ?: return@mapNotNull null
            snap.copy(id = 0L, entryId = mappedEntryId)
        }
        if (remappedHistory.isNotEmpty()) {
            insertAllPriceHistory(remappedHistory)
        }

        payload.targets.forEach { target ->
            if (replaceExisting || getCaliberTarget(target.caliber) == null) {
                upsertCaliberTarget(target)
            }
        }

        payload.rangeBagItems.forEach { bagItem ->
            val mappedEntryId = idMap[bagItem.entryId] ?: return@forEach
            upsertRangeBagItem(bagItem.copy(id = 0L, entryId = mappedEntryId))
        }

        val sessionIdMap = mutableMapOf<Long, Long>()
        payload.rangeSessions.forEach { session ->
            val oldSessionId = session.id
            val newSessionId = insertRangeSession(session.copy(id = 0L))
            if (oldSessionId > 0L) {
                sessionIdMap[oldSessionId] = newSessionId
            }
        }
        val remappedUsages = payload.rangeSessionUsages.mapNotNull { usage ->
            val mappedSessionId = sessionIdMap[usage.sessionId] ?: return@mapNotNull null
            val mappedEntryId = usage.entryId?.let { idMap[it] }
            usage.copy(id = 0L, sessionId = mappedSessionId, entryId = mappedEntryId)
        }
        if (remappedUsages.isNotEmpty()) {
            insertRangeSessionUsages(remappedUsages)
        }

        return payload.entries.size
    }
}
