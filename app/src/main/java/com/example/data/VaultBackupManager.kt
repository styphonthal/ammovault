package com.example.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class VaultBackupPayload(
    val entries: List<AmmoEntry>,
    val targets: List<CaliberTarget>,
    val history: List<PriceHistoryEntry>,
    val exportedAt: Long,
    val rangeBagItems: List<RangeBagItem> = emptyList(),
    val rangeSessions: List<RangeSession> = emptyList(),
    val rangeSessionUsages: List<RangeSessionAmmoUsage> = emptyList(),
    val storageUnits: List<String> = emptyList(),
    val restoredPhotosCount: Int = 0
)

object VaultBackupManager {

    private const val BACKUP_FORMAT_VERSION = 3
    private const val ZIP_JSON_ENTRY = "vault_backup.json"
    private const val ZIP_PHOTOS_PREFIX = "photos/"

    fun exportToJson(
        entries: List<AmmoEntry>,
        targets: List<CaliberTarget>,
        history: List<PriceHistoryEntry>,
        exportedAt: Long = System.currentTimeMillis(),
        rangeBagItems: List<RangeBagItem> = emptyList(),
        rangeSessions: List<RangeSession> = emptyList(),
        rangeSessionUsages: List<RangeSessionAmmoUsage> = emptyList(),
        storageUnits: List<String> = emptyList()
    ): String {
        val root = JSONObject().apply {
            put("app", "AmmoVault")
            put("formatVersion", BACKUP_FORMAT_VERSION)
            put("exportedAt", exportedAt)
        }

        val entriesArray = JSONArray()
        entries.forEach { entry ->
            entriesArray.put(
                JSONObject().apply {
                    put("id", entry.id)
                    put("caliber", entry.caliber)
                    put("brand", entry.brand)
                    put("productLine", entry.productLine)
                    put("grain", entry.grain)
                    put("bulletType", entry.bulletType)
                    put("currentCount", entry.currentCount)
                    put("boxSize", entry.boxSize)
                    put("lowStockThreshold", entry.lowStockThreshold)
                    put("marketPricePerRound", entry.marketPricePerRound)
                    put("costBasis", entry.costBasis)
                    if (entry.targetBuyPricePerRound != null) {
                        put("targetBuyPricePerRound", entry.targetBuyPricePerRound)
                    } else {
                        put("targetBuyPricePerRound", JSONObject.NULL)
                    }
                    put("casingMaterial", entry.casingMaterial)
                    put("lotNumber", entry.lotNumber)
                    put("upcCode", entry.upcCode)
                    put("vendor", entry.vendor)
                    put("storageLocation", entry.storageLocation)
                    put("photoUri", entry.photoUri)
                    if (entry.purchaseDate != null) {
                        put("purchaseDate", entry.purchaseDate)
                    } else {
                        put("purchaseDate", JSONObject.NULL)
                    }
                    put("notes", entry.notes)
                    put("createdAt", entry.createdAt)
                    put("updatedAt", entry.updatedAt)
                }
            )
        }
        root.put("entries", entriesArray)

        val targetsArray = JSONArray()
        targets.forEach { target ->
            targetsArray.put(
                JSONObject().apply {
                    put("caliber", target.caliber)
                    put("targetRounds", target.targetRounds)
                    put("minThresholdRounds", target.minThresholdRounds)
                    if (target.targetBuyPricePerRound != null) {
                        put("targetBuyPricePerRound", target.targetBuyPricePerRound)
                    } else {
                        put("targetBuyPricePerRound", JSONObject.NULL)
                    }
                }
            )
        }
        root.put("caliberTargets", targetsArray)

        val historyArray = JSONArray()
        history.forEach { snap ->
            historyArray.put(
                JSONObject().apply {
                    put("id", snap.id)
                    put("entryId", snap.entryId)
                    put("pricePerRound", snap.pricePerRound)
                    put("vendor", snap.vendor)
                    put("source", snap.source)
                    put("recordedAt", snap.recordedAt)
                }
            )
        }
        root.put("priceHistory", historyArray)

        val bagArray = JSONArray()
        rangeBagItems.forEach { item ->
            bagArray.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("bagName", item.bagName)
                    put("entryId", item.entryId)
                    put("packedRounds", item.packedRounds)
                    put("updatedAt", item.updatedAt)
                }
            )
        }
        root.put("rangeBagItems", bagArray)

        val sessionsArray = JSONArray()
        rangeSessions.forEach { session ->
            sessionsArray.put(
                JSONObject().apply {
                    put("id", session.id)
                    put("sessionDateTime", session.sessionDateTime)
                    put("location", session.location)
                    put("durationMinutes", session.durationMinutes)
                    put("rangeBagName", session.rangeBagName)
                    put("storageUnitName", session.storageUnitName)
                    put("sessionType", session.sessionType)
                    if (session.distanceYards != null) {
                        put("distanceYards", session.distanceYards)
                    } else {
                        put("distanceYards", JSONObject.NULL)
                    }
                    if (session.bestGroupMoa != null) {
                        put("bestGroupMoa", session.bestGroupMoa)
                    } else {
                        put("bestGroupMoa", JSONObject.NULL)
                    }
                    if (session.avgVelocityFps != null) {
                        put("avgVelocityFps", session.avgVelocityFps)
                    } else {
                        put("avgVelocityFps", JSONObject.NULL)
                    }
                    put("notes", session.notes)
                    put("photoUri", session.photoUri)
                    put("createdAt", session.createdAt)
                }
            )
        }
        root.put("rangeSessions", sessionsArray)

        val usagesArray = JSONArray()
        rangeSessionUsages.forEach { usage ->
            usagesArray.put(
                JSONObject().apply {
                    put("id", usage.id)
                    put("sessionId", usage.sessionId)
                    if (usage.entryId != null) {
                        put("entryId", usage.entryId)
                    } else {
                        put("entryId", JSONObject.NULL)
                    }
                    put("caliber", usage.caliber)
                    put("brand", usage.brand)
                    put("grain", usage.grain)
                    put("bulletType", usage.bulletType)
                    put("lotNumber", usage.lotNumber)
                    put("roundsUsed", usage.roundsUsed)
                    put("costPerRoundSnapshot", usage.costPerRoundSnapshot)
                    put("malfunctionsCount", usage.malfunctionsCount)
                }
            )
        }
        root.put("rangeSessionUsages", usagesArray)

        val storageUnitsArray = JSONArray()
        storageUnits
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .forEach { unitName -> storageUnitsArray.put(unitName) }
        root.put("storageUnits", storageUnitsArray)

        return root.toString(2)
    }

    fun parseFromJson(jsonString: String): Result<VaultBackupPayload> = runCatching {
        val root = JSONObject(jsonString)
        val entriesArray = root.optJSONArray("entries")
            ?: throw IllegalArgumentException("Missing 'entries' array in backup file")

        val entries = buildList {
            for (i in 0 until entriesArray.length()) {
                val obj = entriesArray.getJSONObject(i)
                val caliber = obj.optString("caliber").trim()
                val brand = obj.optString("brand").trim()
                if (caliber.isEmpty() || brand.isEmpty()) {
                    throw IllegalArgumentException("Entry at index $i is missing caliber or brand")
                }
                add(
                    AmmoEntry(
                        id = obj.optLong("id", 0L),
                        caliber = caliber,
                        brand = brand,
                        productLine = obj.optString("productLine", ""),
                        grain = obj.optInt("grain", 55).coerceIn(1, 2000),
                        bulletType = obj.optString("bulletType", "FMJ"),
                        currentCount = obj.optInt("currentCount", 0).coerceAtLeast(0),
                        boxSize = obj.optInt("boxSize", 20).coerceAtLeast(1),
                        lowStockThreshold = obj.optInt("lowStockThreshold", 60).coerceAtLeast(0),
                        marketPricePerRound = obj.optDouble("marketPricePerRound", 0.0).coerceAtLeast(0.0),
                        costBasis = (obj.optNullableDouble("costBasis")
                            ?: obj.optNullableDouble("purchasePricePerRound")
                            ?: 0.0).coerceAtLeast(0.0),
                        targetBuyPricePerRound = obj.optNullableDouble("targetBuyPricePerRound")?.coerceAtLeast(0.0),
                        casingMaterial = obj.optString("casingMaterial", "Brass").ifBlank { "Brass" },
                        lotNumber = obj.optString("lotNumber", ""),
                        upcCode = obj.optString("upcCode", ""),
                        vendor = obj.optString("vendor", ""),
                        storageLocation = obj.optString("storageLocation", ""),
                        photoUri = obj.optString("photoUri", ""),
                        purchaseDate = obj.optNullableLong("purchaseDate"),
                        notes = obj.optString("notes", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val targetsArray = root.optJSONArray("caliberTargets") ?: JSONArray()
        val targets = buildList {
            for (i in 0 until targetsArray.length()) {
                val obj = targetsArray.getJSONObject(i)
                val caliber = obj.optString("caliber").trim()
                if (caliber.isNotEmpty()) {
                    add(
                        CaliberTarget(
                            caliber = caliber,
                            targetRounds = obj.optInt("targetRounds", 200).coerceAtLeast(1),
                            targetBuyPricePerRound = obj.optNullableDouble("targetBuyPricePerRound")?.coerceAtLeast(0.0),
                            minThresholdRounds = obj.optInt("minThresholdRounds", 0).coerceAtLeast(0)
                        )
                    )
                }
            }
        }

        val historyArray = root.optJSONArray("priceHistory") ?: JSONArray()
        val history = buildList {
            for (i in 0 until historyArray.length()) {
                val obj = historyArray.getJSONObject(i)
                add(
                    PriceHistoryEntry(
                        id = obj.optLong("id", 0L),
                        entryId = obj.optLong("entryId", 0L),
                        pricePerRound = obj.optDouble("pricePerRound", 0.0).coerceAtLeast(0.0),
                        vendor = obj.optString("vendor", ""),
                        source = obj.optString("source", "Backup Restore"),
                        recordedAt = obj.optLong("recordedAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val bagArray = root.optJSONArray("rangeBagItems") ?: JSONArray()
        val rangeBagItems = buildList {
            for (i in 0 until bagArray.length()) {
                val obj = bagArray.getJSONObject(i)
                val entryId = obj.optLong("entryId", 0L)
                val packed = obj.optInt("packedRounds", 0)
                if (entryId > 0L && packed > 0) {
                    add(
                        RangeBagItem(
                            id = obj.optLong("id", 0L),
                            bagName = obj.optString("bagName", "Primary Range Bag").ifBlank { "Primary Range Bag" },
                            entryId = entryId,
                            packedRounds = packed,
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }
        }

        val sessionsArray = root.optJSONArray("rangeSessions") ?: JSONArray()
        val rangeSessions = buildList {
            for (i in 0 until sessionsArray.length()) {
                val obj = sessionsArray.getJSONObject(i)
                add(
                    RangeSession(
                        id = obj.optLong("id", 0L),
                        sessionDateTime = obj.optLong("sessionDateTime", System.currentTimeMillis()),
                        location = obj.optString("location", ""),
                        durationMinutes = obj.optInt("durationMinutes", 60).coerceAtLeast(1),
                        rangeBagName = obj.optString("rangeBagName", ""),
                        storageUnitName = obj.optString("storageUnitName", ""),
                        sessionType = obj.optString("sessionType", "Practice").ifBlank { "Practice" },
                        distanceYards = obj.optNullableInt("distanceYards"),
                        bestGroupMoa = obj.optNullableDouble("bestGroupMoa"),
                        avgVelocityFps = obj.optNullableInt("avgVelocityFps"),
                        notes = obj.optString("notes", ""),
                        photoUri = obj.optString("photoUri", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val usagesArray = root.optJSONArray("rangeSessionUsages") ?: JSONArray()
        val rangeSessionUsages = buildList {
            for (i in 0 until usagesArray.length()) {
                val obj = usagesArray.getJSONObject(i)
                val sessionId = obj.optLong("sessionId", 0L)
                val rounds = obj.optInt("roundsUsed", 0)
                if (sessionId > 0L && rounds > 0) {
                    add(
                        RangeSessionAmmoUsage(
                            id = obj.optLong("id", 0L),
                            sessionId = sessionId,
                            entryId = obj.optNullableLong("entryId"),
                            caliber = obj.optString("caliber", ""),
                            brand = obj.optString("brand", ""),
                            grain = obj.optInt("grain", 55),
                            bulletType = obj.optString("bulletType", ""),
                            lotNumber = obj.optString("lotNumber", ""),
                            roundsUsed = rounds,
                            costPerRoundSnapshot = obj.optDouble("costPerRoundSnapshot", 0.0).coerceAtLeast(0.0),
                            malfunctionsCount = obj.optInt("malfunctionsCount", 0).coerceAtLeast(0)
                        )
                    )
                }
            }
        }

        val storageUnitsArray = root.optJSONArray("storageUnits") ?: JSONArray()
        val storageUnits = buildList {
            for (i in 0 until storageUnitsArray.length()) {
                val unitName = storageUnitsArray.optString(i, "").trim()
                if (unitName.isNotEmpty()) add(unitName)
            }
        }

        VaultBackupPayload(
            entries = entries,
            targets = targets,
            history = history,
            exportedAt = root.optLong("exportedAt", System.currentTimeMillis()),
            rangeBagItems = rangeBagItems,
            rangeSessions = rangeSessions,
            rangeSessionUsages = rangeSessionUsages,
            storageUnits = storageUnits
        )
    }

    /**
     * Exports a portable ZIP archive containing vault_backup.json plus all attached lot & range photos.
     * Returns the count of photo files bundled into the archive.
     */
    fun exportZipBackup(
        outputStream: OutputStream,
        entries: List<AmmoEntry>,
        targets: List<CaliberTarget>,
        history: List<PriceHistoryEntry>,
        rangeBagItems: List<RangeBagItem> = emptyList(),
        rangeSessions: List<RangeSession> = emptyList(),
        rangeSessionUsages: List<RangeSessionAmmoUsage> = emptyList(),
        storageUnits: List<String> = emptyList()
    ): Int {
        val photoFileToZipPath = LinkedHashMap<File, String>()
        var photoCounter = 0

        fun mapPhotoUriToZip(rawUri: String): String {
            if (rawUri.isBlank()) return ""
            val parsed = runCatching { Uri.parse(rawUri) }.getOrNull() ?: return ""
            if (parsed.scheme != "file") return ""
            val path = parsed.path ?: return ""
            val file = File(path)
            if (!file.exists() || !file.isFile) return ""
            return photoFileToZipPath.getOrPut(file) {
                photoCounter++
                val ext = file.extension.ifBlank { "jpg" }
                "${ZIP_PHOTOS_PREFIX}img_${photoCounter}_${file.nameWithoutExtension.take(16)}.$ext"
            }
        }

        val mappedEntries = entries.map { entry ->
            entry.copy(photoUri = mapPhotoUriToZip(entry.photoUri))
        }
        val mappedSessions = rangeSessions.map { session ->
            session.copy(photoUri = mapPhotoUriToZip(session.photoUri))
        }

        val jsonText = exportToJson(
            entries = mappedEntries,
            targets = targets,
            history = history,
            rangeBagItems = rangeBagItems,
            rangeSessions = mappedSessions,
            rangeSessionUsages = rangeSessionUsages,
            storageUnits = storageUnits
        )

        ZipOutputStream(outputStream).use { zos ->
            zos.putNextEntry(ZipEntry(ZIP_JSON_ENTRY))
            zos.write(jsonText.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            photoFileToZipPath.forEach { (file, zipPath) ->
                runCatching {
                    zos.putNextEntry(ZipEntry(zipPath))
                    file.inputStream().use { input -> input.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
        return photoFileToZipPath.size
    }

    /**
     * Imports either a full AmmoVault ZIP archive (restoring bundled photos into app-private storage)
     * or a raw JSON backup file.
     */
    fun importZipOrJsonBackup(
        context: Context,
        inputStream: InputStream
    ): Result<VaultBackupPayload> = runCatching {
        val rawBytes = inputStream.readBytes()
        if (rawBytes.size >= 2 && rawBytes[0] == 0x50.toByte() && rawBytes[1] == 0x4B.toByte()) {
            // ZIP magic header ("PK")
            val photosDir = File(context.filesDir, "vault_photos").apply {
                if (!exists()) mkdirs()
            }
            var jsonContent: String? = null
            val restoredPhotoMap = mutableMapOf<String, String>()

            ZipInputStream(rawBytes.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val safeName = entry.name.replace("\\", "/")
                        when {
                            safeName == ZIP_JSON_ENTRY || safeName.endsWith(".json", ignoreCase = true) -> {
                                val buffer = ByteArrayOutputStream()
                                zis.copyTo(buffer)
                                jsonContent = buffer.toString(Charsets.UTF_8.name())
                            }
                            safeName.startsWith(ZIP_PHOTOS_PREFIX) && !safeName.contains("..") -> {
                                val ext = safeName.substringAfterLast('.', "jpg")
                                    .filter(Char::isLetterOrDigit)
                                    .ifBlank { "jpg" }
                                val destFile = File(photosDir, "restored_${UUID.randomUUID()}.$ext")
                                destFile.outputStream().use { out -> zis.copyTo(out) }
                                restoredPhotoMap[safeName] = Uri.fromFile(destFile).toString()
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            val jsonText = jsonContent
                ?: throw IllegalArgumentException("ZIP archive does not contain vault_backup.json")
            val basePayload = parseFromJson(jsonText).getOrThrow()

            val remappedEntries = basePayload.entries.map { e ->
                val restoredUri = restoredPhotoMap[e.photoUri]
                if (restoredUri != null) e.copy(photoUri = restoredUri) else e
            }
            val remappedSessions = basePayload.rangeSessions.map { s ->
                val restoredUri = restoredPhotoMap[s.photoUri]
                if (restoredUri != null) s.copy(photoUri = restoredUri) else s
            }

            basePayload.copy(
                entries = remappedEntries,
                rangeSessions = remappedSessions,
                restoredPhotosCount = restoredPhotoMap.size
            )
        } else {
            parseFromJson(rawBytes.toString(Charsets.UTF_8)).getOrThrow()
        }
    }

    /**
     * Parses AmmoVault CSV exports or standard third-party Excel/Google Sheets ammo spreadsheets.
     */
    fun parseFromCsv(csvString: String): Result<VaultBackupPayload> = runCatching {
        val rows = parseCsvRows(csvString).filter { row -> row.any { it.isNotBlank() } }
        if (rows.size < 2) {
            throw IllegalArgumentException("CSV file must include a header row and at least one ammo row.")
        }

        val headers = rows.first().map { normalizeHeader(it) }
        fun colIndex(vararg names: String): Int {
            for (name in names) {
                val idx = headers.indexOf(name)
                if (idx >= 0) return idx
            }
            return -1
        }

        val caliberIdx = colIndex("caliber", "cartridge", "gauge")
        val brandIdx = colIndex("brand", "manufacturer", "make")
        val lineIdx = colIndex("productline", "load", "loadname", "product", "name", "model")
        val grainIdx = colIndex("grain", "grains", "weight", "bulletweight")
        val bulletIdx = colIndex("bullettype", "bullet", "type", "projectile")
        val casingIdx = colIndex("casing", "casingmaterial", "case")
        val roundsIdx = colIndex("rounds", "count", "quantity", "qty", "currentcount", "onhand")
        val boxIdx = colIndex("boxsize", "rdsperbox", "roundsperbox")
        val lowIdx = colIndex("lowstockthreshold", "lowstock", "threshold", "minrounds")
        val storageIdx = colIndex("storagelocation", "storage", "location", "can", "ammocan")
        val costIdx = colIndex("purchasecpr", "costbasis", "costperround", "paidcpr", "pricepaid")
        val marketIdx = colIndex("marketcpr", "marketpriceperround", "cpr", "priceperround", "price")
        val targetIdx = colIndex("buytargetcpr", "targetbuycpr", "targetprice")
        val lotIdx = colIndex("lotnumber", "lot", "batch")
        val upcIdx = colIndex("upccode", "upc", "barcode", "ean")
        val vendorIdx = colIndex("vendor", "store", "retailer")
        val dateIdx = colIndex("purchasedate", "date")
        val notesIdx = colIndex("notes", "note", "comments")

        if (caliberIdx < 0 || brandIdx < 0) {
            throw IllegalArgumentException("CSV header must include at least 'Caliber' and 'Brand' columns.")
        }

        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        fun List<String>.cell(idx: Int): String = if (idx in indices) this[idx].trim() else ""
        fun parseMoney(raw: String): Double? = raw.replace("$", "").replace(",", "").trim().toDoubleOrNull()

        val entries = rows.drop(1).mapNotNull { row ->
            val caliber = row.cell(caliberIdx)
            val brand = row.cell(brandIdx)
            if (caliber.isBlank() || brand.isBlank()) return@mapNotNull null

            val grain = row.cell(grainIdx).filter(Char::isDigit).toIntOrNull()?.coerceIn(15, 2000) ?: 55
            val rounds = row.cell(roundsIdx).filter(Char::isDigit).toIntOrNull()?.coerceAtLeast(0) ?: 20
            val boxSize = row.cell(boxIdx).filter(Char::isDigit).toIntOrNull()?.coerceAtLeast(1) ?: 20
            val lowThreshold = row.cell(lowIdx).filter(Char::isDigit).toIntOrNull()?.coerceAtLeast(0) ?: 60
            val costCpr = parseMoney(row.cell(costIdx))?.coerceAtLeast(0.0) ?: 0.0
            val marketCpr = parseMoney(row.cell(marketIdx))?.coerceAtLeast(0.0) ?: costCpr
            val buyTarget = parseMoney(row.cell(targetIdx))?.coerceAtLeast(0.0)
            val purchaseDate = row.cell(dateIdx).takeIf { it.isNotBlank() }?.let { rawDate ->
                runCatching { dateFormatter.parse(rawDate)?.time }.getOrNull()
            }

            AmmoEntry(
                caliber = caliber,
                brand = brand,
                productLine = row.cell(lineIdx),
                grain = grain,
                bulletType = row.cell(bulletIdx).ifBlank { "FMJ" },
                currentCount = rounds,
                boxSize = boxSize,
                lowStockThreshold = lowThreshold,
                marketPricePerRound = marketCpr,
                costBasis = costCpr,
                targetBuyPricePerRound = buyTarget,
                casingMaterial = row.cell(casingIdx).ifBlank { "Brass" },
                lotNumber = row.cell(lotIdx),
                upcCode = UpcCatalog.normalizeUpc(row.cell(upcIdx)),
                vendor = row.cell(vendorIdx),
                storageLocation = row.cell(storageIdx),
                purchaseDate = purchaseDate,
                notes = row.cell(notesIdx)
            )
        }

        if (entries.isEmpty()) {
            throw IllegalArgumentException("No valid ammo rows were found in the CSV file.")
        }

        VaultBackupPayload(
            entries = entries,
            targets = emptyList(),
            history = emptyList(),
            exportedAt = System.currentTimeMillis()
        )
    }

    fun exportCatalogToCsv(
        entries: List<AmmoEntry>,
        targets: List<CaliberTarget>
    ): String {
        val targetMap = targets.associateBy { it.caliber }
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val headers = listOf(
            "Caliber",
            "Brand",
            "Product Line",
            "Grain",
            "Bullet Type",
            "Casing",
            "Rounds",
            "Box Size",
            "Low Stock Threshold",
            "Storage Location",
            "Purchase CPR",
            "Market CPR",
            "Buy Target CPR",
            "Total Cost Basis",
            "Total Market Value",
            "Unrealized Gain/Loss",
            "Lot Number",
            "UPC Code",
            "Vendor",
            "Purchase Date",
            "Notes"
        )

        val sb = StringBuilder()
        sb.appendLine(headers.joinToString(",") { escapeCsv(it) })

        entries.forEach { entry ->
            val effectiveTarget = entry.targetBuyPricePerRound ?: targetMap[entry.caliber]?.targetBuyPricePerRound
            val purchaseDateStr = entry.purchaseDate?.let { dateFormatter.format(Date(it)) } ?: ""
            val row = listOf(
                entry.caliber,
                entry.brand,
                entry.productLine,
                entry.grain.toString(),
                entry.bulletType,
                entry.casingMaterial,
                entry.currentCount.toString(),
                entry.boxSize.toString(),
                entry.lowStockThreshold.toString(),
                entry.storageLocation,
                if (entry.costBasis > 0.0) String.format(Locale.US, "%.4f", entry.costBasis) else "",
                String.format(Locale.US, "%.4f", entry.marketPricePerRound),
                effectiveTarget?.let { String.format(Locale.US, "%.4f", it) } ?: "",
                String.format(Locale.US, "%.2f", entry.totalCostBasis),
                String.format(Locale.US, "%.2f", entry.totalMarketValue),
                String.format(Locale.US, "%.2f", entry.unrealizedGainLoss),
                entry.lotNumber,
                entry.upcCode,
                entry.vendor,
                purchaseDateStr,
                entry.notes
            )
            sb.appendLine(row.joinToString(",") { escapeCsv(it) })
        }
        return sb.toString()
    }

    private fun parseCsvRows(input: String): List<List<String>> {
        val cleaned = input.removePrefix("\uFEFF")
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < cleaned.length) {
            val c = cleaned[i]
            when {
                inQuotes -> {
                    if (c == '"') {
                        if (i + 1 < cleaned.length && cleaned[i + 1] == '"') {
                            currentField.append('"')
                            i++
                        } else {
                            inQuotes = false
                        }
                    } else {
                        currentField.append(c)
                    }
                }
                c == '"' -> inQuotes = true
                c == ',' -> {
                    currentRow.add(currentField.toString())
                    currentField.clear()
                }
                c == '\n' -> {
                    currentRow.add(currentField.toString())
                    currentField.clear()
                    rows.add(currentRow.toList())
                    currentRow.clear()
                }
                c == '\r' -> {
                    // ignore CR in CRLF
                }
                else -> currentField.append(c)
            }
            i++
        }
        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString())
            rows.add(currentRow.toList())
        }
        return rows
    }

    private fun normalizeHeader(raw: String): String =
        raw.lowercase(Locale.US).filter(Char::isLetterOrDigit)

    private fun escapeCsv(value: String): String {
        val needsQuoting = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
        return if (needsQuoting) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun JSONObject.optNullableDouble(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        val value = optDouble(key, Double.NaN)
        return if (value.isNaN()) null else value
    }

    private fun JSONObject.optNullableLong(key: String): Long? {
        if (!has(key) || isNull(key)) return null
        return optLong(key)
    }

    private fun JSONObject.optNullableInt(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return optInt(key)
    }
}
