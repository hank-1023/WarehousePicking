package com.topsmarteye.warehousepicking.network

import com.squareup.moshi.Json

data class UserProperty(@Json(name = "data") val userData: UserData)

data class UserData(@Json(name = "updateName") val displayName: String,
                    @Json(name = "departid") val workID: String)

data class StockProperty(@Json(name = "data") val stockList: List<StockItem>)

data class StockItem(@Json(name = "matnrName") val name: String,
                     @Json(name = "matnr") val id: String,
                     @Json(name = "qty") val quantity: Int,
                     @Json(name = "fromLoc") val location: String)