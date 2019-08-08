package com.topsmarteye.warehousepicking.backOrder

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.topsmarteye.warehousepicking.network.ApiStatus
import com.topsmarteye.warehousepicking.network.ItemStatus
import com.topsmarteye.warehousepicking.network.StockItem
import com.topsmarteye.warehousepicking.network.networkServices.LoadOrderService
import kotlinx.coroutines.*
import java.lang.Exception

class BackOrderViewModel : ViewModel() {

    private val itemMap = mutableMapOf<String, StockItem>()

    private val _orderID = MutableLiveData<String>()
    val orderID: LiveData<String>
        get() = _orderID

    private val _stockBarcode = MutableLiveData<String>()
    val stockBarcode: LiveData<String>
        get() = _stockBarcode

    private val _itemToRestock = MutableLiveData<StockItem>()
    val itemToRestock: LiveData<StockItem>
        get() = _itemToRestock

    private val _eventKeyboardScan = MutableLiveData<Boolean>()
    val eventKeyboardScan: LiveData<Boolean>
        get() = _eventKeyboardScan

    private val _eventLoadOrderSuccess = MutableLiveData<Boolean>()
    val eventLoadOrderSuccess: LiveData<Boolean>
        get() = _eventLoadOrderSuccess

    private val _eventStockBarcodeNotFound = MutableLiveData<Boolean>()
    val eventStockBarcodeNotFound: LiveData<Boolean>
        get() = _eventStockBarcodeNotFound

    private val _eventNavigateToSubmit = MutableLiveData<Boolean>()
    val eventNavigateToSubmit: LiveData<Boolean>
        get() = _eventNavigateToSubmit

    private val _inputApiStatus = MutableLiveData<ApiStatus>()
    val inputApiStatus: LiveData<ApiStatus>
        get() = _inputApiStatus

    private var inputJob = Job()
    private val inputCoroutineScope = CoroutineScope(inputJob + Dispatchers.Main)

    init {
        _eventKeyboardScan.value = false
    }

    fun setOrderNumber(orderNumber: String) {
        _orderID.value = orderNumber

        inputCoroutineScope.launch {
            _inputApiStatus.value = ApiStatus.LOADING
            try {
                val itemList = LoadOrderService.loadOrderWithStatus(orderNumber, ItemStatus.NOTPICKED)
                if (itemList.isNullOrEmpty()) {
                    throw Exception("setOrderNumber: itemList is empty")
                }
                withContext(Dispatchers.IO) {
                    itemList.forEach {
                        it.stockBarcode?.let { barcode -> itemMap[barcode] = it }
                    }
                }
                _inputApiStatus.value = ApiStatus.DONE
                _eventLoadOrderSuccess.value = true
            } catch (e: Exception) {
                Log.d("BackOrderViewModel", "setOrderNumber error: ${e.message}")
                _inputApiStatus.value = ApiStatus.ERROR
            } finally {
                _inputApiStatus.value = ApiStatus.NONE
            }
        }

    }

    fun setStockBarcode(stockBarcode: String) {
        _stockBarcode.value = stockBarcode

        inputCoroutineScope.launch {
            if (itemMap.containsKey(stockBarcode)){
                _itemToRestock.value = itemMap[stockBarcode]
                _eventNavigateToSubmit.value = true
            } else {
                _eventStockBarcodeNotFound.value = true
            }
        }


    }

    fun onKeyboardScan() {
        _eventKeyboardScan.value = true
    }

    fun onKeyboardScanComplete() {
        _eventKeyboardScan.value = false
    }

    fun onLoadOrderSuccessComplete() {
        _eventLoadOrderSuccess.value = false
    }

    fun onStockBarcodeNotFoundComplete() {
        _eventStockBarcodeNotFound.value = false
    }

    fun onNavigateToSubmitComplete() {
        _eventNavigateToSubmit.value = false
    }

    override fun onCleared() {
        super.onCleared()
        inputJob.cancel()
    }

}