package com.example.data

data class UpcProductSpec(
    val upc: String,
    val caliber: String,
    val brand: String,
    val productLine: String,
    val grain: Int,
    val bulletType: String,
    val casingMaterial: String = "Brass",
    val boxSize: Int = 20
)

/**
 * Offline reference catalog of popular factory rifle ammunition UPC codes.
 * Any user-scanned UPC saved to an AmmoEntry in Room is also matched automatically,
 * growing the user's personal UPC library over time.
 */
object UpcCatalog {
    private val builtInCatalog: Map<String, UpcProductSpec> = listOf(
        UpcProductSpec(
            upc = "741569010115",
            caliber = "5.56 NATO",
            brand = "PMC",
            productLine = "X-TAC M193",
            grain = 55,
            bulletType = "FMJ-BT",
            casingMaterial = "Brass",
            boxSize = 20
        ),
        UpcProductSpec(
            upc = "741569010122",
            caliber = "5.56 NATO",
            brand = "PMC",
            productLine = "X-TAC M855 Green Tip",
            grain = 62,
            bulletType = "FMJ",
            casingMaterial = "Brass",
            boxSize = 20
        ),
        UpcProductSpec(
            upc = "741569060080",
            caliber = ".223 Rem",
            brand = "PMC",
            productLine = "Bronze",
            grain = 55,
            bulletType = "FMJ-BT",
            casingMaterial = "Brass",
            boxSize = 20
        ),
        UpcProductSpec(
            upc = "029465094034",
            caliber = "5.56 NATO",
            brand = "Federal Premium",
            productLine = "Gold Medal Match",
            grain = 77,
            bulletType = "BTHP",
            casingMaterial = "Brass",
            boxSize = 20
        ),
        UpcProductSpec(
            upc = "029465089986",
            caliber = ".308 Win",
            brand = "Federal Premium",
            productLine = "Gold Medal Sierra",
            grain = 168,
            bulletType = "BTHP",
            casingMaterial = "Brass",
            boxSize = 20
        ),
        UpcProductSpec(
            upc = "029465091989",
            caliber = ".308 Win",
            brand = "Federal Premium",
            productLine = "Gold Medal Sierra",
            grain = 175,
            bulletType = "BTHP",
            casingMaterial = "Brass",
            boxSize = 20
        ),
        UpcProductSpec(
            upc = "029465088293",
            caliber = ".223 Rem",
            brand = "Federal",
            productLine = "American Eagle",
            grain = 55,
            bulletType = "FMJ",
            casingMaterial = "Brass",
            boxSize = 20
        ),
        UpcProductSpec(
            upc = "090255815009",
            caliber = "6.5 Creedmoor",
            brand = "Hornady",
            productLine = "Match",
            grain = 140,
            bulletType = "ELD-M",
            casingMaterial = "Brass",
            boxSize = 20
        ),
        UpcProductSpec(
            upc = "090255815016",
            caliber = "6.5 Creedmoor",
            brand = "Hornady",
            productLine = "Precision Hunter",
            grain = 143,
            bulletType = "ELD-X",
            casingMaterial = "Brass",
            boxSize = 20
        ),
        UpcProductSpec(
            upc = "090255808773",
            caliber = ".300 BLK",
            brand = "Hornady",
            productLine = "Sub-X",
            grain = 190,
            bulletType = "Subsonic",
            casingMaterial = "Brass",
            boxSize = 20
        ),
        UpcProductSpec(
            upc = "090255255553",
            caliber = "5.56 NATO",
            brand = "Hornady",
            productLine = "Frontier M193",
            grain = 55,
            bulletType = "FMJ",
            casingMaterial = "Brass",
            boxSize = 20
        ),
        UpcProductSpec(
            upc = "090255255577",
            caliber = "5.56 NATO",
            brand = "Hornady",
            productLine = "Frontier Match",
            grain = 75,
            bulletType = "BTHP",
            casingMaterial = "Brass",
            boxSize = 20
        ),
        UpcProductSpec(
            upc = "020892224148",
            caliber = "5.56 NATO",
            brand = "Winchester",
            productLine = "USA M193",
            grain = 55,
            bulletType = "FMJ",
            casingMaterial = "Brass",
            boxSize = 20
        ),
        UpcProductSpec(
            upc = "020892224179",
            caliber = "5.56 NATO",
            brand = "Winchester",
            productLine = "USA M855 Green Tip",
            grain = 62,
            bulletType = "FMJ",
            casingMaterial = "Brass",
            boxSize = 20
        ),
        UpcProductSpec(
            upc = "020892213036",
            caliber = ".308 Win",
            brand = "Winchester",
            productLine = "USA Target",
            grain = 147,
            bulletType = "FMJ",
            casingMaterial = "Brass",
            boxSize = 20
        ),
        UpcProductSpec(
            upc = "076683000354",
            caliber = ".22 LR",
            brand = "CCI",
            productLine = "Standard Velocity",
            grain = 40,
            bulletType = "FMJ",
            casingMaterial = "Brass",
            boxSize = 50
        ),
        UpcProductSpec(
            upc = "076683000309",
            caliber = ".22 LR",
            brand = "CCI",
            productLine = "Mini-Mag",
            grain = 40,
            bulletType = "FMJ",
            casingMaterial = "Brass",
            boxSize = 100
        ),
        UpcProductSpec(
            upc = "762344702476",
            caliber = "7.62x39mm",
            brand = "Belom",
            productLine = "Tactical",
            grain = 123,
            bulletType = "FMJ",
            casingMaterial = "Brass",
            boxSize = 20
        )
    ).associateBy { normalizeUpc(it.upc) }

    fun normalizeUpc(raw: String): String {
        val digits = raw.filter(Char::isDigit)
        // Strip leading 0 from 13-digit EAN-13 representations of 12-digit UPC-A codes
        return if (digits.length == 13 && digits.startsWith("0")) {
            digits.drop(1)
        } else {
            digits
        }
    }

    fun lookupBuiltIn(rawUpc: String): UpcProductSpec? {
        val normalized = normalizeUpc(rawUpc)
        if (normalized.isEmpty()) return null
        return builtInCatalog[normalized]
    }

    fun findMatchingEntries(rawUpc: String, entries: List<AmmoEntry>): List<AmmoEntry> {
        val normalized = normalizeUpc(rawUpc)
        if (normalized.isEmpty()) return emptyList()
        return entries.filter { normalizeUpc(it.upcCode) == normalized }
    }
}
