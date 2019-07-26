package com.topsmarteye.warehousepicking.network

import com.squareup.moshi.Json

data class UserProperty(@Json(name = "data") val data: Data)

data class Data(@Json(name = "updateName") val displayName: String,
                @Json(name = "departid") val workID: String)

