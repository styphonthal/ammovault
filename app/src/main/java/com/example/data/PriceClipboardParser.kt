package com.example.data

import java.util.Locale
import kotlin.math.roundToInt

enum class PriceUnitMode(val label: String) {
    DOLLARS_PER_ROUND("$/rd"),
    CENTS_PER_ROUND("¢/rd"),
    BOX_TOTAL("Box $")
}

data class ParsedPriceResult(
    val pricePerRound: Double,
    val unitMode: PriceUnitMode,
    val displayInputValue: String,
    val summary: String
)

/**
 * Extracts ammunition pricing from copied text or listing snippets (e.g., "47.9¢/rd",
 * "48 cpr", "$0.48/rd", "$28.99 / 20 rds", or "$28.99") and converts it into a
 * dollar price-per-round snapshot.
 */
object PriceClipboardParser {
    private val centsRegex = Regex(
        """(\d+(?:\.\d+)?)\s*(?:¢|cpr|c/rd|cents?\b)""",
        RegexOption.IGNORE_CASE
    )
    private val explicitDollarPerRoundRegex = Regex(
        """\$\s*(\d+(?:\.\d+)?)\s*(?:/\s*(?:rd|round|rds|rounds|ea)\b|per\s*round\b|cpr\b)""",
        RegexOption.IGNORE_CASE
    )
    private val totalForRoundsRegex = Regex(
        """\$\s*(\d+(?:,\d{3})*(?:\.\d+)?)\s*(?:/|for)\s*(\d+)\s*(?:rds?|rounds?)?\b""",
        RegexOption.IGNORE_CASE
    )
    private val dollarAmountRegex = Regex("""\$\s*(\d+(?:,\d{3})*(?:\.\d+)?)""")
    private val plainDecimalRegex = Regex("""(?<!\d)(\d+(?:\.\d{1,3})?)(?!\d)""")

    fun parsePriceFromText(rawText: String, boxSize: Int = 20): ParsedPriceResult? {
        val cleaned = rawText.trim()
        if (cleaned.isEmpty()) return null

        // 1. Explicit cents per round (e.g., "47.9¢/rd", "48¢", "47.5 cpr")
        centsRegex.find(cleaned)?.let { match ->
            val cents = match.groupValues[1].toDoubleOrNull()
            if (cents != null && cents > 0.0 && cents <= 10_000.0) {
                val cpr = roundCpr(cents / 100.0)
                val formattedCents = formatCleanNumber(cents)
                return ParsedPriceResult(
                    pricePerRound = cpr,
                    unitMode = PriceUnitMode.CENTS_PER_ROUND,
                    displayInputValue = formattedCents,
                    summary = "${formattedCents}¢/rd → ${formatDollarCpr(cpr)}/rd"
                )
            }
        }

        // 2. Explicit dollar per round (e.g., "$0.48/rd", "$1.62 per round")
        explicitDollarPerRoundRegex.find(cleaned)?.let { match ->
            val dollars = match.groupValues[1].toDoubleOrNull()
            if (dollars != null && dollars in 0.01..100.0) {
                val cpr = roundCpr(dollars)
                val formatted = formatInputCpr(cpr)
                return ParsedPriceResult(
                    pricePerRound = cpr,
                    unitMode = PriceUnitMode.DOLLARS_PER_ROUND,
                    displayInputValue = formatted,
                    summary = "Parsed ${formatDollarCpr(cpr)}/rd"
                )
            }
        }

        // 3. Total price for N rounds (e.g., "$28.99 / 20", "$480.00 for 1000 rds")
        totalForRoundsRegex.find(cleaned)?.let { match ->
            val total = match.groupValues[1].replace(",", "").toDoubleOrNull()
            val rounds = match.groupValues[2].toIntOrNull()
            if (total != null && rounds != null && rounds > 0 && total > 0.0) {
                val cpr = roundCpr(total / rounds.toDouble())
                if (cpr in 0.01..100.0) {
                    return ParsedPriceResult(
                        pricePerRound = cpr,
                        unitMode = PriceUnitMode.DOLLARS_PER_ROUND,
                        displayInputValue = formatInputCpr(cpr),
                        summary = "${formatCurrencySimple(total)} ÷ $rounds rds = ${formatDollarCpr(cpr)}/rd"
                    )
                }
            }
        }

        // 4. General dollar figure or standalone decimal
        val numericCandidate = dollarAmountRegex.find(cleaned)
            ?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
            ?: plainDecimalRegex.find(cleaned)?.groupValues?.get(1)?.toDoubleOrNull()

        if (numericCandidate != null && numericCandidate > 0.0) {
            return when {
                numericCandidate <= 5.0 -> {
                    val cpr = roundCpr(numericCandidate)
                    ParsedPriceResult(
                        pricePerRound = cpr,
                        unitMode = PriceUnitMode.DOLLARS_PER_ROUND,
                        displayInputValue = formatInputCpr(cpr),
                        summary = "Parsed ${formatDollarCpr(cpr)}/rd"
                    )
                }
                boxSize > 1 && numericCandidate <= 5_000.0 -> {
                    val cpr = roundCpr(numericCandidate / boxSize.toDouble())
                    if (cpr in 0.01..100.0) {
                        val totalText = formatCleanNumber(numericCandidate)
                        ParsedPriceResult(
                            pricePerRound = cpr,
                            unitMode = PriceUnitMode.BOX_TOTAL,
                            displayInputValue = totalText,
                            summary = "Box ${formatCurrencySimple(numericCandidate)} ÷ $boxSize rds = ${formatDollarCpr(cpr)}/rd"
                        )
                    } else {
                        null
                    }
                }
                else -> null
            }
        }

        return null
    }

    fun computeCpr(inputText: String, mode: PriceUnitMode, boxSize: Int): Double? {
        val raw = inputText.toDoubleOrNull() ?: return null
        if (raw < 0.0) return null
        val cpr = when (mode) {
            PriceUnitMode.DOLLARS_PER_ROUND -> raw
            PriceUnitMode.CENTS_PER_ROUND -> raw / 100.0
            PriceUnitMode.BOX_TOTAL -> {
                if (boxSize <= 0) return null
                raw / boxSize.toDouble()
            }
        }
        val rounded = roundCpr(cpr)
        return if (rounded in 0.0..100.0) rounded else null
    }

    fun formatValueForMode(cpr: Double, mode: PriceUnitMode, boxSize: Int): String {
        return when (mode) {
            PriceUnitMode.DOLLARS_PER_ROUND -> formatInputCpr(cpr)
            PriceUnitMode.CENTS_PER_ROUND -> formatCleanNumber(cpr * 100.0)
            PriceUnitMode.BOX_TOTAL -> String.format(Locale.US, "%.2f", cpr * boxSize.coerceAtLeast(1))
        }
    }

    private fun roundCpr(value: Double): Double {
        return (value * 1000.0).roundToInt() / 1000.0
    }

    private fun formatInputCpr(cpr: Double): String {
        val threeDec = String.format(Locale.US, "%.3f", cpr)
        return if (threeDec.endsWith("0")) {
            String.format(Locale.US, "%.2f", cpr)
        } else {
            threeDec
        }
    }

    private fun formatCleanNumber(value: Double): String {
        val twoDec = String.format(Locale.US, "%.2f", value)
        return twoDec.trimEnd('0').trimEnd('.')
    }

    private fun formatDollarCpr(cpr: Double): String {
        return String.format(Locale.US, "$%.2f", cpr)
    }

    private fun formatCurrencySimple(amount: Double): String {
        return String.format(Locale.US, "$%.2f", amount)
    }
}
