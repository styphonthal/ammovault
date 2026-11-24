package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AmmoEntry::class,
        CaliberTarget::class,
        PriceHistoryEntry::class,
        RangeBagItem::class,
        RangeSession::class,
        RangeSessionAmmoUsage::class,
        StorageUnit::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AmmoDatabase : RoomDatabase() {
    abstract fun ammoDao(): AmmoDao

    companion object {
        @Volatile
        private var INSTANCE: AmmoDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ammo_entries ADD COLUMN purchasePricePerRound REAL")
                db.execSQL("ALTER TABLE ammo_entries ADD COLUMN targetBuyPricePerRound REAL")
                db.execSQL("ALTER TABLE ammo_entries ADD COLUMN storageLocation TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE caliber_targets ADD COLUMN targetBuyPricePerRound REAL")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Preserve price_history rows because DROP TABLE ammo_entries triggers ON DELETE CASCADE
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS price_history_backup (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        entryId INTEGER NOT NULL,
                        pricePerRound REAL NOT NULL,
                        vendor TEXT NOT NULL,
                        source TEXT NOT NULL,
                        recordedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO price_history_backup (id, entryId, pricePerRound, vendor, source, recordedAt)
                    SELECT id, entryId, pricePerRound, vendor, source, recordedAt FROM price_history
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ammo_entries_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        caliber TEXT NOT NULL,
                        brand TEXT NOT NULL,
                        productLine TEXT NOT NULL,
                        grain INTEGER NOT NULL,
                        bulletType TEXT NOT NULL,
                        currentCount INTEGER NOT NULL,
                        boxSize INTEGER NOT NULL,
                        lowStockThreshold INTEGER NOT NULL,
                        marketPricePerRound REAL NOT NULL,
                        costBasis REAL NOT NULL,
                        targetBuyPricePerRound REAL,
                        casingMaterial TEXT NOT NULL,
                        lotNumber TEXT NOT NULL,
                        vendor TEXT NOT NULL,
                        storageLocation TEXT NOT NULL,
                        purchaseDate INTEGER,
                        notes TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO ammo_entries_new (
                        id, caliber, brand, productLine, grain, bulletType,
                        currentCount, boxSize, lowStockThreshold, marketPricePerRound,
                        costBasis, targetBuyPricePerRound, casingMaterial, lotNumber,
                        vendor, storageLocation, purchaseDate, notes, createdAt, updatedAt
                    )
                    SELECT
                        id, caliber, brand, productLine, grain, bulletType,
                        currentCount, boxSize, lowStockThreshold, marketPricePerRound,
                        COALESCE(purchasePricePerRound, 0.0), targetBuyPricePerRound, casingMaterial, lotNumber,
                        vendor, storageLocation, purchaseDate, notes, createdAt, updatedAt
                    FROM ammo_entries
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE ammo_entries")
                db.execSQL("ALTER TABLE ammo_entries_new RENAME TO ammo_entries")

                db.execSQL(
                    """
                    INSERT OR REPLACE INTO price_history (id, entryId, pricePerRound, vendor, source, recordedAt)
                    SELECT id, entryId, pricePerRound, vendor, source, recordedAt FROM price_history_backup
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE price_history_backup")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ammo_entries ADD COLUMN upcCode TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE ammo_entries ADD COLUMN photoUri TEXT NOT NULL DEFAULT ''")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS range_bag_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        bagName TEXT NOT NULL,
                        entryId INTEGER NOT NULL,
                        packedRounds INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY(entryId) REFERENCES ammo_entries(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_range_bag_items_entryId ON range_bag_items (entryId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_range_bag_items_bagName_entryId ON range_bag_items (bagName, entryId)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS range_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionDateTime INTEGER NOT NULL,
                        location TEXT NOT NULL,
                        durationMinutes INTEGER NOT NULL,
                        rangeBagName TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        photoUri TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS range_session_usage (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionId INTEGER NOT NULL,
                        entryId INTEGER,
                        caliber TEXT NOT NULL,
                        brand TEXT NOT NULL,
                        grain INTEGER NOT NULL,
                        bulletType TEXT NOT NULL,
                        lotNumber TEXT NOT NULL,
                        roundsUsed INTEGER NOT NULL,
                        costPerRoundSnapshot REAL NOT NULL,
                        FOREIGN KEY(sessionId) REFERENCES range_sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(entryId) REFERENCES ammo_entries(id) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_range_session_usage_sessionId ON range_session_usage (sessionId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_range_session_usage_entryId ON range_session_usage (entryId)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE range_sessions ADD COLUMN sessionType TEXT NOT NULL DEFAULT 'Practice'")
                db.execSQL("ALTER TABLE range_sessions ADD COLUMN distanceYards INTEGER")
                db.execSQL("ALTER TABLE range_sessions ADD COLUMN bestGroupMoa REAL")
                db.execSQL("ALTER TABLE range_sessions ADD COLUMN avgVelocityFps INTEGER")
                db.execSQL("ALTER TABLE range_session_usage ADD COLUMN malfunctionsCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS storage_units (
                        name TEXT NOT NULL PRIMARY KEY,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO storage_units (name, createdAt)
                    SELECT DISTINCT TRIM(storageLocation), CAST(strftime('%s','now') AS INTEGER) * 1000
                    FROM ammo_entries
                    WHERE TRIM(storageLocation) != ''
                    """.trimIndent()
                )
                db.execSQL("ALTER TABLE range_sessions ADD COLUMN storageUnitName TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE caliber_targets ADD COLUMN minThresholdRounds INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): AmmoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AmmoDatabase::class.java,
                    "ammo_vault.db"
                )
                    .addMigrations(
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9
                    )
                    .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
