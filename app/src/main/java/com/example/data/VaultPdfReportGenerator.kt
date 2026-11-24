package com.example.data

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.OutputStream
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

/**
 * Generates a clean multi-page printable PDF Inventory & Insurance Valuation Report
 * using Android's native PdfDocument API (zero external dependencies).
 */
object VaultPdfReportGenerator {

    private const val PAGE_WIDTH = 612   // US Letter @ 72 DPI
    private const val PAGE_HEIGHT = 792
    private const val MARGIN = 36f

    fun generateValuationReport(
        outputStream: OutputStream,
        entries: List<AmmoEntry>,
        caliberSummaries: List<CaliberInventorySummary>,
        storageSummaries: List<StorageContainerSummary>,
        totalRounds: Int,
        totalCostBasis: Double,
        totalMarketValue: Double,
        weightedAvgCpr: Double
    ) {
        val document = PdfDocument()
        val currency = NumberFormat.getCurrencyInstance(Locale.US)
        val intFmt = NumberFormat.getIntegerInstance(Locale.US)
        val reportDate = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date())

        val headerBgPaint = Paint().apply {
            color = Color.parseColor("#1D2229")
            style = Paint.Style.FILL
        }
        val sectionBgPaint = Paint().apply {
            color = Color.parseColor("#EFECE4")
            style = Paint.Style.FILL
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#D0C9BA")
            strokeWidth = 0.8f
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D4A745")
            textSize = 18f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1D2229")
            textSize = 11.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val colHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4A505C")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1D2229")
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1D2229")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6B7280")
            textSize = 8f
        }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y = MARGIN

        fun drawFooter() {
            canvas.drawLine(MARGIN, PAGE_HEIGHT - 28f, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 28f, linePaint)
            canvas.drawText("AmmoVault Inventory & Valuation Report · Generated $reportDate", MARGIN, PAGE_HEIGHT - 14f, footerPaint)
            canvas.drawText("Page $pageNumber", PAGE_WIDTH - MARGIN - 36f, PAGE_HEIGHT - 14f, footerPaint)
        }

        fun ensureSpace(neededHeight: Float) {
            if (y + neededHeight > PAGE_HEIGHT - 42f) {
                drawFooter()
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                y = MARGIN
            }
        }

        fun drawSectionHeader(title: String) {
            ensureSpace(34f)
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20f, sectionBgPaint)
            canvas.drawText(title, MARGIN + 8f, y + 14f, sectionPaint)
            y += 28f
        }

        // Top Banner
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 58f, headerBgPaint)
        canvas.drawText("AMMOVAULT — INVENTORY & VALUATION REPORT", MARGIN + 14f, y + 26f, titlePaint)
        canvas.drawText("Generated $reportDate · Private Offline Export", MARGIN + 14f, y + 44f, subtitlePaint)
        y += 72f

        // Portfolio Summary Metrics
        val gainLoss = totalMarketValue - totalCostBasis
        val gainSign = if (gainLoss >= 0) "+" else "-"
        val totalWeightLbs = storageSummaries.sumOf { it.estimatedWeightLbs }

        canvas.drawText("Total Rounds: ${intFmt.format(totalRounds)}", MARGIN, y, sectionPaint)
        canvas.drawText("Total Lots: ${entries.size}", MARGIN + 185f, y, sectionPaint)
        canvas.drawText(String.format(Locale.US, "Est. Total Weight: ~%.1f lbs", totalWeightLbs), MARGIN + 340f, y, sectionPaint)
        y += 18f

        canvas.drawText("Cost Basis: ${currency.format(totalCostBasis)}", MARGIN, y, bodyPaint)
        canvas.drawText("Market Value: ${currency.format(totalMarketValue)}", MARGIN + 185f, y, bodyPaint)
        canvas.drawText(
            "Unrealized: $gainSign${currency.format(kotlin.math.abs(gainLoss))} · Avg ${String.format(Locale.US, "$%.2f/rd", weightedAvgCpr)}",
            MARGIN + 340f,
            y,
            bodyPaint
        )
        y += 22f

        // 1. Caliber Summary Table
        if (caliberSummaries.isNotEmpty()) {
            drawSectionHeader("1. SUMMARY BY CALIBER")
            canvas.drawText("CALIBER", MARGIN, y, colHeaderPaint)
            canvas.drawText("LOTS", MARGIN + 150f, y, colHeaderPaint)
            canvas.drawText("ROUNDS", MARGIN + 205f, y, colHeaderPaint)
            canvas.drawText("AVG CPR", MARGIN + 280f, y, colHeaderPaint)
            canvas.drawText("COST BASIS", MARGIN + 360f, y, colHeaderPaint)
            canvas.drawText("MARKET VALUE", MARGIN + 455f, y, colHeaderPaint)
            y += 6f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
            y += 13f

            caliberSummaries.forEach { summary ->
                ensureSpace(18f)
                canvas.drawText(summary.caliber.take(22), MARGIN, y, bodyPaint)
                canvas.drawText(summary.lotCount.toString(), MARGIN + 150f, y, monoPaint)
                canvas.drawText(intFmt.format(summary.totalRounds), MARGIN + 205f, y, monoPaint)
                canvas.drawText(String.format(Locale.US, "$%.2f", summary.weightedAvgPricePerRound), MARGIN + 280f, y, monoPaint)
                canvas.drawText(currency.format(summary.totalCostBasis), MARGIN + 360f, y, monoPaint)
                canvas.drawText(currency.format(summary.totalMarketValue), MARGIN + 455f, y, monoPaint)
                y += 16f
            }
            y += 8f
        }

        // 2. Storage Container / Ammo Can Summary Table
        if (storageSummaries.isNotEmpty()) {
            drawSectionHeader("2. STORAGE LOCATIONS & AMMO CANS")
            canvas.drawText("LOCATION / CONTAINER", MARGIN, y, colHeaderPaint)
            canvas.drawText("LOTS", MARGIN + 190f, y, colHeaderPaint)
            canvas.drawText("ROUNDS", MARGIN + 245f, y, colHeaderPaint)
            canvas.drawText("EST. WEIGHT", MARGIN + 325f, y, colHeaderPaint)
            canvas.drawText("MARKET VALUE", MARGIN + 440f, y, colHeaderPaint)
            y += 6f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
            y += 13f

            storageSummaries.forEach { container ->
                ensureSpace(18f)
                canvas.drawText(container.locationName.take(28), MARGIN, y, bodyPaint)
                canvas.drawText(container.lotCount.toString(), MARGIN + 190f, y, monoPaint)
                canvas.drawText(intFmt.format(container.totalRounds), MARGIN + 245f, y, monoPaint)
                canvas.drawText(String.format(Locale.US, "~%.1f lbs", container.estimatedWeightLbs), MARGIN + 325f, y, monoPaint)
                canvas.drawText(currency.format(container.totalMarketValue), MARGIN + 440f, y, monoPaint)
                y += 16f
            }
            y += 8f
        }

        // 3. Detailed Lot Ledger
        drawSectionHeader("3. DETAILED LOT LEDGER (${entries.size} LOTS)")
        fun drawLedgerColumnHeaders() {
            canvas.drawText("CALIBER", MARGIN, y, colHeaderPaint)
            canvas.drawText("BRAND / LOAD", MARGIN + 88f, y, colHeaderPaint)
            canvas.drawText("SPEC", MARGIN + 225f, y, colHeaderPaint)
            canvas.drawText("RDS", MARGIN + 305f, y, colHeaderPaint)
            canvas.drawText("STORAGE / LOT", MARGIN + 350f, y, colHeaderPaint)
            canvas.drawText("VALUE", MARGIN + 475f, y, colHeaderPaint)
            y += 6f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
            y += 13f
        }

        drawLedgerColumnHeaders()

        entries.sortedWith(compareBy({ it.caliber.lowercase(Locale.US) }, { it.brand.lowercase(Locale.US) }))
            .forEach { entry ->
                if (y + 20f > PAGE_HEIGHT - 42f) {
                    drawFooter()
                    document.finishPage(page)
                    pageNumber++
                    page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                    canvas = page.canvas
                    y = MARGIN
                    drawLedgerColumnHeaders()
                }
                val brandLoad = "${entry.brand} ${entry.productLine}".trim().take(24)
                val spec = "${entry.grain}gr ${entry.bulletType}".take(13)
                val storageLot = buildList {
                    if (entry.storageLocation.isNotBlank()) add(entry.storageLocation)
                    if (entry.lotNumber.isNotBlank()) add("#${entry.lotNumber}")
                }.joinToString(" ").ifBlank { "—" }.take(20)

                canvas.drawText(entry.caliber.take(14), MARGIN, y, bodyPaint)
                canvas.drawText(brandLoad, MARGIN + 88f, y, bodyPaint)
                canvas.drawText(spec, MARGIN + 225f, y, monoPaint)
                canvas.drawText(intFmt.format(entry.currentCount), MARGIN + 305f, y, monoPaint)
                canvas.drawText(storageLot, MARGIN + 350f, y, bodyPaint)
                canvas.drawText(currency.format(entry.totalMarketValue), MARGIN + 475f, y, monoPaint)
                y += 15f
            }

        drawFooter()
        document.finishPage(page)
        document.writeTo(outputStream)
        document.close()
    }
}
