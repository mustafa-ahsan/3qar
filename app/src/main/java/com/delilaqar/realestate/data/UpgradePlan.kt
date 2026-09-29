package com.delilaqar.realestate.data

data class UpgradePlan(
    val id: String,
    val label: String,
    val durationDays: Int,
    val priceIqd: Double
)

object UpgradePlans {
    val LISTING = listOf(
        UpgradePlan("3d", "3 أيام", 3, 2000.0),
        UpgradePlan("7d", "أسبوع", 7, 4000.0),
        UpgradePlan("30d", "شهر", 30, 12000.0),
        UpgradePlan("365d", "سنة", 365, 100000.0)
    )
    val ACCOUNT = listOf(
        UpgradePlan("acc_30d", "اشتراك شهري", 30, 25000.0),
        UpgradePlan("acc_365d", "اشتراك سنوي", 365, 240000.0)
    )
}
