package com.topsmarteye.warehousepicking.network

import com.squareup.moshi.Json


data class UserProperty(@Json(name = "data") val userData: UserData)

data class UserData(@Json(name = "updateName") val displayName: String?,
                    @Json(name = "departid") val workID: String?)

data class StockProperty(@Json(name = "data") val stockList: List<StockItem>)


// All the task/order/stock number are declared as String for compatibility reasons
data class StockItem(val id: String?,
                     val unit: String?,
                     var status: String?,
                     val internalTaskNum: String?,
                     val sysCompanyCode: String?,
                     val createBy: String?,
                     var updateName: String?,
                     var updateBy: String?,
                     var createDate: String?,
                     val bpmStatus: String?,
                     val sysOrgCode: String?,
                     var updateDate: String?,
                     val createName: String?,
                     val taskId: String?,
                     @Json(name = "matnrName") val name: String?,
                     val company: String?,
                     @Json(name = "fromLoc") val location: String?,
                     var finishTime: String?,
                     @Json(name = "matnrBarcode") val stockBarcode: String?,
                     @Json(name = "qty") val quantity: Int,
                     @Json(name = "matnr") val stockId: String?,
                     @Json(name = "dutoQty") var restockQuantity: Int,
                     @Json(name = "pickQty") var pickQuantity: Int,
                     @Json(name = "shortQty") var outOfStockQuantity: Int)


enum class ItemStatus(val value: String) {
    NOTPICKED("0"),
    WORKING("1"),
    RESTOCK("2"),
    OUTOFSTOCK("3"),
    COMPLETE("4")
}