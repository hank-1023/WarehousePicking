package com.topsmarteye.warehousepicking.network.networkServices

import com.topsmarteye.warehousepicking.network.GlobalApi
import com.topsmarteye.warehousepicking.network.ItemStatus
import com.topsmarteye.warehousepicking.network.StockItem
import java.lang.Exception

object LoadOrderService {

    suspend fun loadOrderWithStatus(orderNumber: String, status: ItemStatus): List<StockItem> {
        val itemList: List<StockItem>

        var response = GlobalApi.retrofitService
            .getOrderItems(LoginService.authToken!!, orderNumber, status.value)

        //update auth token if out-of-date
        if (!response.isSuccessful && LoginService.updateAuthToken()) {
            response = GlobalApi.retrofitService
                .getOrderItems(LoginService.authToken!!, orderNumber, status.value)
        }

        if (response.isSuccessful && !response.body()?.stockList.isNullOrEmpty()) {
            itemList = response.body()?.stockList!!
        } else {
            throw Exception("loadOrderWithStatus failed to get response/list empty")
        }

        return itemList
    }


//    suspend fun resetOrderItems(items: List<StockItem>): List<StockItem> {
//        if (items.isEmpty()) { return listOf() }
//
//        val result = mutableListOf<StockItem>()
//
//        withContext(Dispatchers.IO) {
//            for (item in items) {
//                item.status = ItemStatus.NOTPICKED.value
//                item.finishTime = null
//                result.add(item)
//            }
//        }
//        return result
//    }

}