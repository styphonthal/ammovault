package com.example.data

import java.net.URLEncoder

object AmmoSeekUrlBuilder {
    private val caliberSlugMap = mapOf(
        "5.56 nato" to "5.56x45mm-nato",
        "5.56x45mm" to "5.56x45mm-nato",
        "5.56x45mm nato" to "5.56x45mm-nato",
        "5.56" to "5.56x45mm-nato",
        ".223 rem" to "223-remington",
        ".223 remington" to "223-remington",
        "223 rem" to "223-remington",
        ".308 win" to "308-winchester",
        ".308 winchester" to "308-winchester",
        "308 win" to "308-winchester",
        "7.62x51mm" to "308-winchester",
        "7.62x51mm nato" to "308-winchester",
        "7.62 nato" to "308-winchester",
        "6.5 creedmoor" to "6.5mm-creedmoor",
        "6.5mm creedmoor" to "6.5mm-creedmoor",
        ".300 blk" to "300-aac-blackout",
        "300 blk" to "300-aac-blackout",
        "300 blackout" to "300-aac-blackout",
        ".300 aac blackout" to "300-aac-blackout",
        "300 aac blackout" to "300-aac-blackout",
        "7.62x39mm" to "7.62x39mm",
        "7.62x39" to "7.62x39mm",
        ".30-06 springfield" to "30-06-springfield",
        ".30-06" to "30-06-springfield",
        "30-06" to "30-06-springfield",
        ".270 win" to "270-winchester",
        ".270 winchester" to "270-winchester",
        ".243 win" to "243-winchester",
        ".243 winchester" to "243-winchester",
        ".300 win mag" to "300-winchester-magnum",
        ".300 winchester magnum" to "300-winchester-magnum",
        "6mm creedmoor" to "6mm-creedmoor",
        "6mm arc" to "6mm-arc",
        "6.5 grendel" to "6.5mm-grendel",
        "6.5mm grendel" to "6.5mm-grendel",
        ".224 valkyrie" to "224-valkyrie",
        ".350 legend" to "350-legend",
        ".450 bushmaster" to "450-bushmaster",
        ".30-30 win" to "30-30-winchester",
        ".30-30 winchester" to "30-30-winchester",
        ".45-70 govt" to "45-70-government",
        ".45-70 government" to "45-70-government",
        ".338 lapua" to "338-lapua-magnum",
        ".338 lapua magnum" to "338-lapua-magnum",
        ".22 lr" to "22-long-rifle",
        ".22 long rifle" to "22-long-rifle",
        "9mm" to "9mm-luger",
        "9mm luger" to "9mm-luger",
        "9x19mm" to "9mm-luger",
        ".45 acp" to "45-acp",
        ".40 s&w" to "40-sw",
        "10mm" to "10mm-auto",
        "10mm auto" to "10mm-auto",
        ".380 acp" to "380-acp",
        "12 gauge" to "12-gauge",
        "20 gauge" to "20-gauge"
    )

    private val brandFillerTokens = setOf(
        "premium",
        "ammunition",
        "ammo",
        "cartridges",
        "arms",
        "usa",
        "inc",
        "llc",
        "corp",
        "co"
    )

    /**
     * Builds AmmoSeek's native path-based URL for the lot's caliber and grain weight
     * (e.g., https://ammoseek.com/ammo/5.56x45mm-nato/77grains).
     * Avoids invalid ?grains= query strings and overly restrictive multi-word ?ikw= strings
     * that cause AmmoSeek to return "No ammo found".
     */
    fun buildSearchUrl(entry: AmmoEntry): String {
        val slug = resolveCaliberSlug(entry.caliber)
        if (slug != null) {
            return if (entry.grain > 0) {
                "https://ammoseek.com/ammo/$slug/${entry.grain}grains"
            } else {
                "https://ammoseek.com/ammo/$slug"
            }
        }
        val trimmedCaliber = entry.caliber.trim()
        return if (trimmedCaliber.isNotEmpty()) {
            val encoded = URLEncoder.encode(trimmedCaliber, "UTF-8")
            "https://ammoseek.com/ammo/rifle?ikw=$encoded"
        } else {
            "https://ammoseek.com/ammo/rifle"
        }
    }

    /**
     * Builds a narrower caliber + grain URL filtered by a normalized single-brand keyword
     * via ?ikw= (e.g., https://ammoseek.com/ammo/5.56x45mm-nato/77grains?ikw=Federal).
     */
    fun buildBrandSearchUrl(entry: AmmoEntry): String {
        val baseUrl = buildSearchUrl(entry)
        val brandKeyword = normalizeBrandKeyword(entry.brand)
        if (brandKeyword.isEmpty()) return baseUrl

        val slug = resolveCaliberSlug(entry.caliber)
        return if (slug != null) {
            val encodedBrand = URLEncoder.encode(brandKeyword, "UTF-8")
            "$baseUrl?ikw=$encodedBrand"
        } else {
            val combined = listOfNotNull(
                entry.caliber.trim().takeIf { it.isNotEmpty() },
                brandKeyword
            ).joinToString(" ")
            val encoded = URLEncoder.encode(combined, "UTF-8")
            "https://ammoseek.com/ammo/rifle?ikw=$encoded"
        }
    }

    fun buildCaliberSearchUrl(caliber: String): String {
        val slug = resolveCaliberSlug(caliber)
        return if (slug != null) {
            "https://ammoseek.com/ammo/$slug"
        } else if (caliber.isNotBlank()) {
            val encoded = URLEncoder.encode(caliber.trim(), "UTF-8")
            "https://ammoseek.com/ammo/rifle?ikw=$encoded"
        } else {
            "https://ammoseek.com/ammo/rifle"
        }
    }

    fun resolveCaliberSlug(caliber: String): String? {
        val normalized = caliber
            .trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
        return caliberSlugMap[normalized]
    }

    internal fun normalizeBrandKeyword(brand: String): String {
        val tokens = brand
            .trim()
            .replace("&", " ")
            .split(Regex("\\s+"))
            .map { it.trim(',', '.', '-', '/') }
            .filter { it.isNotEmpty() && it.lowercase() !in brandFillerTokens }
        return when {
            tokens.isEmpty() -> ""
            tokens.first().equals("sellier", ignoreCase = true) -> "Sellier"
            tokens.first().equals("palmetto", ignoreCase = true) -> "Palmetto"
            tokens.first().equals("sig", ignoreCase = true) -> "Sig"
            else -> tokens.joinToString(" ")
        }
    }
}
