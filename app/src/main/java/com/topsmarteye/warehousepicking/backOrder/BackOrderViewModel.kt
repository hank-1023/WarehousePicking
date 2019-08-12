package com.topsmarteye.warehousepicking.backOrder

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.topsmarteye.warehousepicking.network.ApiStatus
import com.topsmarteye.warehousepicking.network.ItemStatus
import com.topsmarteye.warehousepicking.network.StockItem
import com.topsmarteye.warehousepicking.network.networkServices.LoadOrderService
import com.topsmarteye.warehousepicking.network.networkServices.UpdateItemService
import kotlinx.coroutines.*
import java.lang.Exception

class BackOrderViewModel : ViewModel() {

    private var itemMap: Map<String, StockItem>? = null

    private val _orderID = MutableLiveData<String>()
    val orderID: LiveData<String>
        get() = _orderID

    private val _stockBarcode = MutableLiveData<String>()
    val stockBarcode: LiveData<String>
        get() = _stockBarcode

    private val _itemToRestock = MutableLiveData<StockItem>()
    val itemToRestock: LiveData<StockItem>
        get() = _itemToRestock

    private val _eventDisableInteraction = MutableLiveData<Boolean>()
    val eventDisableInteraction: LiveData<Boolean>
        get() = _eventDisableInteraction

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

    private val _eventNavigateToInput = MutableLiveData<Boolean>()
    val eventNavigateToInput: LiveData<Boolean>
        get() = _eventNavigateToInput

    private val _eventSubmitQuantityError = MutableLiveData<Boolean>()
    val eventSubmitQuantityError: LiveData<Boolean>
        get() = _eventSubmitQuantityError

    private val _inputApiStatus = MutableLiveData<ApiStatus>()
    val inputApiStatus: LiveData<ApiStatus>
        get() = _inputApiStatus

    private val _submitApiStatus = MutableLiveData<ApiStatus>()
    val submitApiStatus: LiveData<ApiStatus>
        get() = _submitApiStatus

    private var job = Job()
    private var coroutineScope = CoroutineScope(job + Dispatchers.Main)

    init {
        _eventKeyboardScan.value = false
    }

    fun setOrderNumber(orderNumber: String) {
        _orderID.value = orderNumber

        coroutineScope.launch {
            _inputApiStatus.value = ApiStatus.LOADING
            try {

                // Reset order every time before loading order: 26084
                if (orderNumber == "26084") {
                    UpdateItemService.resetOrder(orderNumber)
                }

                val itemList = LoadOrderService.loadOrderWithStatus(orderNumber, ItemStatus.NOTPICKED)
                if (itemList.isNullOrEmpty()) {
                    throw Exception("setOrderNumber: itemList is empty")
                }
                withContext(Dispatchers.IO) {
                    val map = mutableMapOf<String, StockItem>()
                    itemList.forEach {
                        it.stockBarcode?.let { barcode -> map[barcode] = it }
                    }
                    itemMap = map.toMap()
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

        coroutineScope.launch {
            if (itemMap!!.containsKey(stockBarcode)) {
                _itemToRestock.value = itemMap!![stockBarcode]
                _eventNavigateToSubmit.value = true
            } else {
                _eventStockBarcodeNotFound.value = true
            }
        }
    }

    fun onDisableInteraction() {
        _eventDisableInteraction.value = true
    }

    fun onDisableInteractionComplete() {
        _eventDisableInteraction.value = false
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

    fun onSubmit(quantity: Int) {
        coroutineScope.launch {
            val item = _itemToRestock.value!!
            item.restockQuantity = quantity

            _submitApiStatus.value = ApiStatus.LOADING
            try {
                UpdateItemService.updateItemWithStatus(item, ItemStatus.RESTOCK)
                _submitApiStatus.value = ApiStatus.DONE
                onNavigateToInput()
            } catch (e: Exception) {
                Log.d("BackOrderViewModel", "onSubmit exception: ${e.message}")
                _submitApiStatus.value = ApiStatus.ERROR
            } finally {
                _submitApiStatus.value = ApiStatus.NONE
            }
        }
    }


    fun onNavigateToInput() {
        // This order matters
        job.cancel()

        _eventNavigateToInput.value = true
    }

    fun onNavigateToInputComplete() {
        _eventNavigateToInput.value = false
        job = Job()
        coroutineScope = CoroutineScope(job + Dispatchers.Main)
    }

    fun onSubmitQuantityError() {
        _eventSubmitQuantityError.value = true
    }

    fun onSubmitQuantityErrorComplete() {
        _eventSubmitQuantityError.value = false
    }

    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }

}