package com.topsmarteye.warehousepicking.backOrder

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BackOrderViewModel : ViewModel() {
    private val _orderID = MutableLiveData<String>()
    val orderID: LiveData<String>
        get() = _orderID

    private val _stockID = MutableLiveData<String>()
    val stockID: LiveData<String>
        get() = _stockID

    private val _eventKeyboardScan = MutableLiveData<Boolean>()
    val eventKeyboardScan: LiveData<Boolean>
        get() = _eventKeyboardScan

    private val _eventOrderIDEdit = MutableLiveData<Boolean>()
    val eventOrderIDEdit: LiveData<Boolean>
        get() = _eventOrderIDEdit

    private val _eventStockIDEdit = MutableLiveData<Boolean>()
    val eventStockIDEdit: LiveData<Boolean>
        get() = _eventStockIDEdit

    private val _eventLoadOrderSuccess = MutableLiveData<Boolean>()
    val eventLoadOrderSuccess: LiveData<Boolean>
        get() = _eventLoadOrderSuccess

    init {
        _eventKeyboardScan.value = false
    }


    fun setOrderNumber(orderNumber: String) {
        _orderID.value = orderNumber
    }

    fun setStockNumber(stockNumber: String) {
        _stockID.value = stockNumber
    }

    fun onKeyboardScan() {
        _eventKeyboardScan.value = true
    }

    fun onKeyboardScanComplete() {
        _eventKeyboardScan.value = false
    }

    fun onOrderIDEdit() {
        _eventOrderIDEdit.value = true
    }

    fun onOrderIDEditComplete() {
        _eventOrderIDEdit.value = false
        onLoadOrderSuccess()
    }

    fun onStockIDEdit() {
        _eventStockIDEdit.value = true
    }

    fun onStockIDScanEditComplete() {
        _eventStockIDEdit.value = false
    }

    fun onLoadOrderSuccess() {
        _eventLoadOrderSuccess.value = true
    }

    fun onLoadOrderSuccessComplete() {
        _eventLoadOrderSuccess.value = false
    }

}