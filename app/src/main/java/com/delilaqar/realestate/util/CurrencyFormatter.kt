package com.delilaqar.realestate.util

import java.util.Locale

object CurrencyFormatter {
    fun format(price: Double): String {
        return "${String.format(Locale.US, "%,.0f", price)} د.ع"
    }
}
