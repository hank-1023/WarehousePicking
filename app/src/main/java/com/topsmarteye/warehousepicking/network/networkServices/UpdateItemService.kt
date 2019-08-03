package com.topsmarteye.warehousepicking.network.networkServices

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.topsmarteye.warehousepicking.formatToStandardDateString
import com.topsmarteye.warehousepicking.getCurrentTimeString
import com.topsmarteye.warehousepicking.network.ApiStatus
import com.topsmarteye.warehousepicking.network.GlobalApi
import com.topsmarteye.warehousepicking.network.ItemStatus
import com.topsmarteye.warehousepicking.network.StockItem
import java.lang.Exception

object UpdateItemService {

    private val mutableUpdateApiStatus = MutableLiveData<ApiStatus>()
    val updateApiStatus: LiveData<ApiStatus>
        get() = mutableUpdateApiStatus

    suspend fun prepareAndPutItemWithStatus(item: StockItem, status: ItemStatus) {
        item.updateDate = getCurrentTimeString()
        item.status = status.value

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
            mutableUpdateApiStatus.value =
                ApiStatus.LOADING
            // Put item
            putItem(item)
        }
    }

    // Only called after item has been prepared
    suspend fun putItem(item: StockItem) {
        try {
            var response = GlobalApi.retrofitService
                .updateOrderItem(LoginService.authToken!!, item.stockId!!, item)
            if (!response.isSuccessful) {
                if (LoginService.updateAuthToken()) {
                    response = GlobalApi.retrofitService
                        .updateOrderItem(LoginService.authToken!!, item.stockId, item)
                } else {
                    mutableUpdateApiStatus.value =
                        ApiStatus.ERROR
                    return
                }
            }

            if (response.isSuccessful) {
                mutableUpdateApiStatus.value =
                    ApiStatus.DONE
            } else {
                mutableUpdateApiStatus.value =
                    ApiStatus.ERROR
                Log.d("UpdateItemService", "putItem error: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.d("UpdateItemService", "putItem exception: ${e.message}")
            mutableUpdateApiStatus.value =
                ApiStatus.ERROR
        }
    }

    // Should reset the status when error/done event has been handled
    fun resetUpdateApiStatus() {
        mutableUpdateApiStatus.value =
            ApiStatus.NONE
    }

    fun checkNoError(): Boolean {
        return mutableUpdateApiStatus.value != ApiStatus.ERROR
    }
}