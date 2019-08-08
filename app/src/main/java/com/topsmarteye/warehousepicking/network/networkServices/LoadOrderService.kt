package com.topsmarteye.warehousepicking.network.networkServices

import com.topsmarteye.warehousepicking.network.RetrofitApi
import com.topsmarteye.warehousepicking.network.ItemStatus
import com.topsmarteye.warehousepicking.network.StockItem
import java.lang.Exception

object LoadOrderService {

    suspend fun loadOrderWithStatus(orderNumber: String, status: ItemStatus): List<StockItem> {
        val itemList: List<StockItem>

        var response = RetrofitApi.retrofitService
            .getOrderItems(LoginService.authToken!!, orderNumber, status.value)

        //update auth token if out-of-date
        if (response.code() == 401) {
            // May throw a exception, should be handled by caller
            LoginService.updateAuthToken()
            response = RetrofitApi.retrofitService
                .getOrderItems(LoginService.authToken!!, orderNumber, status.value)
        }

        if (response.isSuccessful && response.body()?.stockList != null) {
            itemList = response.body()?.stockList!!
        } else {
            throw Exception("loadOrderWithStatus failed to get response/list empty")
        }

        return itemList
    }

}