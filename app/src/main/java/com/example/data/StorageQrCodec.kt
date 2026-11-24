package com.example.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Locale

/**
 * Encodes/decodes AmmoVault Ammo Can QR codes, renders printable label PNG bitmaps,
 * and estimates loaded ammunition weight in pounds.
 */
object StorageQrCodec {

    private const val CAN_PREFIX = "ammovault://can/"

    fun encodeCanQr(locationName: String): String {
        val trimmed = locationName.trim()
        val encoded = URLEncoder.encode(trimmed, Charsets.UTF_8.name())
        return "$CAN_PREFIX$encoded"
    }

    fun decodeCanQr(rawPayload: String): String? {
        val trimmed = rawPayload.trim()
        if (!trimmed.startsWith(CAN_PREFIX, ignoreCase = true)) return null
        val encoded = trimmed.substring(CAN_PREFIX.length)
        if (encoded.isBlank()) return null
        return runCatching {
            URLDecoder.decode(encoded, Charsets.UTF_8.name()).trim()
        }.getOrNull()?.takeIf { it.isNotEmpty() }
    }

    /**
     * Estimates total loaded cartridge + box weight in pounds.
     * 1 pound = 7,000 grains; loaded cartridge weight includes brass case + powder + primer + box.
     */
    fun estimateAmmoWeightLbs(caliber: String, bulletGrain: Int, rounds: Int): Double {
        if (rounds <= 0) return 0.0
        val normalized = caliber.lowercase(Locale.US)
        val totalCartridgeGrains = when {
            normalized.contains("22 lr") || normalized.contains(".22lr") -> 52.0
            normalized.contains("5.56") || normalized.contains("223") -> bulletGrain + 135.0
            normalized.contains("300 blk") || normalized.contains("300 blackout") -> bulletGrain + 130.0
            normalized.contains("7.62x39") -> bulletGrain + 132.0
            normalized.contains("6.5 creed") -> bulletGrain + 230.0
            normalized.contains("308") || normalized.contains("7.62x51") -> bulletGrain + 225.0
            normalized.contains("30-06") -> bulletGrain + 260.0
            normalized.contains("300 win") -> bulletGrain + 315.0
            normalized.contains("338") -> bulletGrain + 420.0
            normalized.contains("243") -> bulletGrain + 215.0
            normalized.contains("270") -> bulletGrain + 245.0
            normalized.contains("6.5 grendel") -> bulletGrain + 145.0
            normalized.contains("6mm arc") -> bulletGrain + 145.0
            else -> bulletGrain * 2.55
        }
        // Add ~6% for cardboard/plastic trays
        return (totalCartridgeGrains * rounds * 1.06) / 7000.0
    }

    fun generateQrBitmap(payload: String, sizePx: Int = 512): Bitmap {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1
        )
        val bitMatrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val pixels = IntArray(sizePx * sizePx)
        for (y in 0 until sizePx) {
            val rowOffset = y * sizePx
            for (x in 0 until sizePx) {
                pixels[rowOffset + x] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        return Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
        }
    }

    /**
     * Renders a crisp high-contrast printable PNG label card for taping onto an ammo can or safe shelf.
     */
    fun renderPrintableCanLabel(summary: StorageContainerSummary): Bitmap {
        val width = 900
        val height = 460
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C1F24")
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawRoundRect(RectF(16f, 16f, width - 16f, height - 16f), 22f, 22f, borderPaint)

        // Header accent strip
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C1F24")
            style = Paint.Style.FILL
        }
        canvas.drawRect(19f, 19f, width - 19f, 96f, headerPaint)

        val headerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D4A745")
            textSize = 30f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        canvas.drawText("AMMOVAUlT // STORAGE LABEL".replace("AMMOVAUlT", "AMMOVAULT"), 44f, 68f, headerTitlePaint)

        // QR Code on left
        val qrSize = 310
        val qrBitmap = generateQrBitmap(encodeCanQr(summary.locationName), qrSize)
        canvas.drawBitmap(qrBitmap, 44f, 120f, null)

        // Text metadata on right
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 42f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2B303A")
            textSize = 26f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#555D6B")
            textSize = 22f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val textX = 386f
        val safeName = if (summary.locationName.length > 22) {
            summary.locationName.take(21) + "…"
        } else {
            summary.locationName
        }
        canvas.drawText(safeName, textX, 178f, titlePaint)

        val caliberLine = summary.calibers.joinToString(" · ").let {
            if (it.length > 34) it.take(33) + "…" else it
        }
        canvas.drawText(caliberLine.ifBlank { "General Ammo Storage" }, textX, 232f, bodyPaint)
        canvas.drawText("${summary.totalRounds} Rounds (${summary.lotCount} Lots)", textX, 286f, bodyPaint)
        canvas.drawText(
            String.format(Locale.US, "Est. Weight: ~%.1f lbs", summary.estimatedWeightLbs),
            textX,
            336f,
            bodyPaint
        )
        canvas.drawText("SCAN WITH AMMOVAULT TO VIEW", textX, 404f, monoPaint)

        return bitmap
    }
}
