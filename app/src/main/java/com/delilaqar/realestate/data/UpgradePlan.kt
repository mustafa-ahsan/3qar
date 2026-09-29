package com.delilaqar.realestate.data

data class UpgradePlan(
    val id: String,
    val label: String,
    val durationDays: Int,
    val priceIqd: Double
)

object UpgradePlans {
    val ALL = listOf(
        UpgradePlan("3d", "3 أيام", 3, 2000.0),
        UpgradePlan("7d", "أسبوع", 7, 4000.0),
        UpgradePlan("30d", "شهر", 30, 12000.0),
        UpgradePlan("365d", "سنة", 365, 100000.0)
    )
}
