package com.topsmarteye.warehousepicking.network.networkServices

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.topsmarteye.warehousepicking.network.ApiStatus
import com.topsmarteye.warehousepicking.network.GlobalApi
import com.topsmarteye.warehousepicking.network.ItemStatus
import com.topsmarteye.warehousepicking.network.StockItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.Exception

object LoadOrderService {

    private val mutableLoadApiStatus = MutableLiveData<ApiStatus>()
    val loadApiStatus: LiveData<ApiStatus>
        get() = mutableLoadApiStatus

    private val mutableResetApiStatus = MutableLiveData<ApiStatus>()
    val resetApiStatus: LiveData<ApiStatus>
        get() = mutableResetApiStatus

    suspend fun loadOrderWithStatus(orderNumber: String, status: ItemStatus): List<StockItem>? {
        var itemList: List<StockItem>? = null

        mutableLoadApiStatus.value = ApiStatus.LOADING

        try {
            var response = GlobalApi.retrofitService
                .getOrderItems(LoginService.authToken!!, orderNumber, status.value)
            if (!response.isSuccessful) {
                //update auth token if out-of-date
                if (LoginService.updateAuthToken()) {
                    response = GlobalApi.retrofitService
                        .getOrderItems(LoginService.authToken!!, orderNumber, status.value)
                } else {
                    mutableLoadApiStatus.value = ApiStatus.ERROR
                    return null
                }
            }
            if (response.isSuccessful) {
                if (!response.body()?.stockList.isNullOrEmpty()) {
                    itemList = response.body()?.stockList
                    mutableLoadApiStatus.value = ApiStatus.DONE
                } else {
                    // Doesn't need to signal error if loading for ResetOrder
                    mutableLoadApiStatus.value = ApiStatus.ERROR
                }
            } else {
                mutableLoadApiStatus.value = ApiStatus.ERROR
            }
        } catch (e: Exception) {
            Log.d("loadStockList", "Load stock list network error ${e.message}")
            mutableLoadApiStatus.value = ApiStatus.ERROR
        }

        return itemList
    }


    suspend fun resetOrderItems(items: List<StockItem>): List<StockItem> {
        if (items.isEmpty()) { return listOf() }

        val result = mutableListOf<StockItem>()

        withContext(Dispatchers.IO) {
            for (item in items) {
                item.status = ItemStatus.NOTPICKED.value
                item.finishTime = null
                result.add(item)
            }
        }
        return result
    }

    fun resetLoadApiStatus() {
        mutableLoadApiStatus.value = ApiStatus.NONE
    }

    fun resetResetApiStatus() {
        mutableResetApiStatus.value = ApiStatus.NONE
    }

}