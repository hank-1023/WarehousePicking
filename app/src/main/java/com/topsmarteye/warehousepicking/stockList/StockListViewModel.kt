package com.topsmarteye.warehousepicking.stockList

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class StockListViewModel : ViewModel() {
    data class Order(val orderNumber: String, val itemList: List<StockItem>)

    data class StockItem(val category: String,
                         val name: String,
                         val quantity: Int,
                         val location: String)

    private val sampleOrder = Order(
        "76543210", listOf(
            StockItem("化妆品", "Dior", 2, "6排6列5层"),
            StockItem("食品", "KFC", 4, "6排6列3层"),
            StockItem("IT", "富士康", 4, "6排6列12层"),
            StockItem("奶茶", "一点点", 20, "6排6列34层")
        )
    )

    // The live data of order number
    private val _orderNumber = MutableLiveData<String>()
    val orderNumber: LiveData<String>
        get() = _orderNumber


    // The live data field of stock item list
    private val itemList: List<StockItem>

    private val _currentIndex = MutableLiveData<Int>()
    val currentIndex: LiveData<Int>
        get() = _currentIndex

    private val _totalItems = MutableLiveData<Int>()
    val totalItems: LiveData<Int>
        get() = _totalItems

    private val _currentItem = MutableLiveData<StockItem>()
    val currentItem: LiveData<StockItem>
        get() = _currentItem

    private val _nextItem = MutableLiveData<StockItem>()
    val nextItem: LiveData<StockItem>
        get() = _nextItem

    private val _isLastItem = MutableLiveData<Boolean>()
    val isLastItem: LiveData<Boolean>
        get() = _isLastItem

    private val _eventRestock = MutableLiveData<Boolean>()
    val eventRestock: LiveData<Boolean>
        get() = _eventRestock

    private val _eventOutOfStock = MutableLiveData<Boolean>()
    val eventOutOfStock: LiveData<Boolean>
        get() = _eventOutOfStock

    private val _eventResetOrder = MutableLiveData<Boolean>()
    val eventResetOrder: LiveData<Boolean>
        get() = _eventResetOrder

    private val _eventFinishOrder = MutableLiveData<Boolean>()
    val eventFinishOrder: LiveData<Boolean>
        get() = _eventFinishOrder

    private val _eventScan = MutableLiveData<Boolean>()
    val eventScan: LiveData<Boolean>
        get() = _eventScan


    init {
        itemList = sampleOrder.itemList
        _currentIndex.value = 0
        _isLastItem.value = false
        _totalItems.value = sampleOrder.itemList.size
        _eventRestock.value = false
        _eventOutOfStock.value = false
        _eventResetOrder.value = false
        _eventScan.value = false

        updateDataWithIndex(0)
    }

    // Update the mutable live data with the index passed in
    private fun updateDataWithIndex(index: Int) {
        _currentItem.value = itemList[index]

        if (index < itemList.size - 1) {
            _nextItem.value = itemList[index + 1]
        } else {
            _nextItem.value = null
            _isLastItem.value = true
        }

    }

    // Increase currentIndex by 1 and call updateDataWithIndex
    fun onNext() {
        if (_currentIndex.value!! < itemList.size - 1) {
            _currentIndex.value = _currentIndex.value!! + 1
            updateDataWithIndex(_currentIndex.value!!)
        }
    }

    fun onRestock() {
        if (_eventRestock.value == false &&
            _eventOutOfStock.value == false && _eventResetOrder.value == false) {

            _eventRestock.value = true
        }
    }

    fun onRestockComplete() {
        _eventRestock.value = false
    }

    fun onOutOfStock() {
        // Prevent more than one dialog window popping up
        if (_eventRestock.value == false &&
            _eventOutOfStock.value == false && _eventResetOrder.value == false) {

            _eventOutOfStock.value = true
        }
    }

    fun onOutOfStockComplete() {
        _eventOutOfStock.value = false
    }

    fun onResetOrder() {
        if (_eventRestock.value == false &&
            _eventOutOfStock.value == false && _eventResetOrder.value == false) {
            _eventResetOrder.value = true
        }
    }

    fun onResetOrderComplete() {
        _eventResetOrder.value = false
    }

    fun onFinishOrder() {
        _eventFinishOrder.value = true
    }

    fun onFinishOrderComplete() {
        _eventFinishOrder.value = false
    }

    fun onDataLoaded(orderNumber: String) {
        _orderNumber.value = orderNumber
    }

    fun onScanTriggeredByKeyboard() {
        _eventScan.value = true
    }

    fun onScanTriggeredByKeyboardComplete() {
        _eventScan.value = false
    }


}