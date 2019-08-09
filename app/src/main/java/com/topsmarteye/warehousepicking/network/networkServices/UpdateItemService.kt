package com.topsmarteye.warehousepicking.network.networkServices

import com.topsmarteye.warehousepicking.formatToStandardDateString
import com.topsmarteye.warehousepicking.getCurrentTimeString
import com.topsmarteye.warehousepicking.network.RetrofitApi
import com.topsmarteye.warehousepicking.network.ItemStatus
import com.topsmarteye.warehousepicking.network.StockItem
import java.lang.Exception

object UpdateItemService {

    // Only handles dates, item should be prepared before passed in
    suspend fun updateItem(item: StockItem, status: ItemStatus) {
        item.status = status.value
        item.updateDate = getCurrentTimeString()

        if (status == ItemStatus.COMPLETE) {
            // item finished
            item.finishTime = getCurrentTimeString()
        } else {
            item.finishTime?.let {
                item.finishTime = it.formatToStandardDateString()
            }
        }

        item.createDate?.let {
            item.createDate = it.formatToStandardDateString()
        }

        LoginService.getUserData()!!.let {
            item.updateName = it.displayName
            item.updateBy = it.workID
            // Put item
            putItem(item)
        }
    }

    // Only called after item has been prepared
    private suspend fun putItem(item: StockItem) {

        var response = RetrofitApi.retrofitService
            .updateOrderItem(LoginService.authToken!!, item.stockId!!, item)
        if (response.code() == 401) {
            // May throw an exception, should be handled by caller
            LoginService.updateAuthToken()
            response = RetrofitApi.retrofitService
                .updateOrderItem(LoginService.authToken!!, item.stockId, item)
        }

        if (!response.isSuccessful) {
            throw Exception("updateItem error: ${response.message()}")
        }

    }

    suspend fun resetOrder(orderNumber: String) {
        var response = RetrofitApi.retrofitService
            .resetOrderToStatus(LoginService.authToken!!, orderNumber, ItemStatus.NOTPICKED.value)
        if (response.code() == 401) {
            // May throw an exception, should be handled by caller
            LoginService.updateAuthToken()
            response = RetrofitApi.retrofitService
                .resetOrderToStatus(LoginService.authToken!!, orderNumber, ItemStatus.NOTPICKED.value)
        }

        if (!response.isSuccessful) {
            throw Exception("resetOrder error: ${response.message()}")
        }
    }

}