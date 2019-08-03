package com.topsmarteye.warehousepicking.network

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.topsmarteye.warehousepicking.formatToStandardDateString
import com.topsmarteye.warehousepicking.getCurrentTimeString
import java.lang.Exception

object UpdateItemApi {

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

        LoginApi.getUserData()!!.let {
            item.updateName = it.displayName
            item.updateBy = it.workID
            mutableUpdateApiStatus.value = ApiStatus.LOADING
            // Put item
            putItem(item)
        }
    }

    // Only called after item has been prepared
    suspend fun putItem(item: StockItem) {
        try {
            var response = GlobalApi.retrofitService
                .updateOrderItem(LoginApi.authToken!!, item.stockId!!, item)
            if (!response.isSuccessful) {
                if (LoginApi.updateAuthToken()) {
                    response = GlobalApi.retrofitService
                        .updateOrderItem(LoginApi.authToken!!, item.stockId, item)
                } else {
                    mutableUpdateApiStatus.value =  ApiStatus.ERROR
                    return
                }
            }

            if (response.isSuccessful) {
                mutableUpdateApiStatus.value = ApiStatus.DONE
            } else {
                mutableUpdateApiStatus.value = ApiStatus.ERROR
                Log.d("UpdateItemApi", "putItem error: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.d("UpdateItemApi", "putItem exception: ${e.message}")
            mutableUpdateApiStatus.value = ApiStatus.ERROR
        }
    }

    fun resetUpdateApiStatus() {
        mutableUpdateApiStatus.value = ApiStatus.NONE
    }

    fun checkNoError(): Boolean {
        return mutableUpdateApiStatus.value != ApiStatus.ERROR
    }
}