package com.topsmarteye.warehousepicking.backOrder

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BackOrderViewModel : ViewModel() {
    private val _orderNumber = MutableLiveData<String>()
    val orderNumber: LiveData<String>
        get() = _orderNumber

    private val _stockNumber = MutableLiveData<String>()
    val stockNumber: LiveData<String>
        get() = _stockNumber

    private val _eventKeyboardScan = MutableLiveData<Boolean>()
    val eventScan: LiveData<Boolean>
        get() = _eventKeyboardScan

    init {
        _eventKeyboardScan.value = false
    }


    fun onSetOrderNumber(orderNumber: String) {
        _orderNumber.value = orderNumber
    }

    fun onSetStockNumber(stockNumber: String) {
        _stockNumber.value = stockNumber
    }

    fun onKeyboardScan() {
        _eventKeyboardScan.value = true
    }

    fun onKeyboardScanComplete() {
        _eventKeyboardScan.value = false
    }

}