package com.delilaqar.realestate.data

data class Property(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var price: Double = 0.0,
    var currency: String = "USD",
    var listingType: String = "",
    var propertyType: String = "",
    var cityId: String = "",
    var district: String = "",
    var bedrooms: Int = 0,
    var bathrooms: Int = 0,
    var area: Double = 0.0,
    var images: List<String> = emptyList(),
    var featured: Boolean = false,
    var status: String = "active"
)
