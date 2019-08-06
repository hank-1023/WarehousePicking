package com.topsmarteye.warehousepicking.network.networkServices

import com.topsmarteye.warehousepicking.formatToStandardDateString
import com.topsmarteye.warehousepicking.getCurrentTimeString
import com.topsmarteye.warehousepicking.network.GlobalApi
import com.topsmarteye.warehousepicking.network.StockItem
import java.lang.Exception

object UpdateItemService {



    // Only handles dates, item should be prepared before passed in
    suspend fun putItemWithStatus(item: StockItem, itemCompleted: Boolean) {
        item.updateDate = getCurrentTimeString()

        if (itemCompleted) {
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

        var response = GlobalApi.retrofitService
            .updateOrderItem(LoginService.authToken!!, item.stockId!!, item)
        if (!response.isSuccessful && LoginService.updateAuthToken()) {
            response = GlobalApi.retrofitService
                .updateOrderItem(LoginService.authToken!!, item.stockId, item)
        }

        if (!response.isSuccessful) {
            throw Exception("putItem error: ${response.message()}")
        }

    }

//    // Should reset the status when error/done event has been handled
//    fun resetUpdateApiStatus() {
//        mutableUpdateApiStatus.value = ApiStatus.NONE
//    }
//
//    fun checkNoError(): Boolean {
//        return mutableUpdateApiStatus.value != ApiStatus.ERROR
//    }
}