package com.topsmarteye.warehousepicking.network.networkServices

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.topsmarteye.warehousepicking.network.ApiStatus
import com.topsmarteye.warehousepicking.network.GlobalApi
import com.topsmarteye.warehousepicking.network.StockItem
import java.lang.Exception

object LoadOrderService {

    private val mutableLoadApiStatus = MutableLiveData<ApiStatus>()
    val loadApiStatus: LiveData<ApiStatus>
        get() = mutableLoadApiStatus

    suspend fun loadOrder(orderNumber: String): List<StockItem>? {
        var itemList: List<StockItem>? = null
        mutableLoadApiStatus.value =
            ApiStatus.LOADING

        try {
            var response = GlobalApi.retrofitService
                .getOrderItems(LoginService.authToken!!, orderNumber, 0)
            if (!response.isSuccessful) {
                //update auth token if out-of-date
                if (LoginService.updateAuthToken()) {
                    response = GlobalApi.retrofitService
                        .getOrderItems(LoginService.authToken!!, orderNumber, 0)
                } else {
                    mutableLoadApiStatus.value =
                        ApiStatus.ERROR
                    return null
                }
            }
            if (response.isSuccessful) {
                if (!response.body()?.stockList.isNullOrEmpty()) {
                    itemList = response.body()?.stockList
                    mutableLoadApiStatus.value =
                        ApiStatus.DONE
                } else {
                    mutableLoadApiStatus.value =
                        ApiStatus.ERROR
                }
            } else {
                mutableLoadApiStatus.value =
                    ApiStatus.ERROR
            }
        } catch (e: Exception) {
            Log.d("loadStockList", "Load stock list network error ${e.message}")
            mutableLoadApiStatus.value =
                ApiStatus.ERROR
        }

        return itemList
    }

    fun resetLoadApiStatus() {
        mutableLoadApiStatus.value =
            ApiStatus.NONE
    }

}