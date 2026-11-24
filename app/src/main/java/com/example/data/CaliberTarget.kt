package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "caliber_targets")
data class CaliberTarget(
    @PrimaryKey
    val caliber: String,
    val targetRounds: Int,
    val targetBuyPricePerRound: Double? = null,
    val minThresholdRounds: Int = 0
)
