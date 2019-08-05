package com.topsmarteye.warehousepicking.stockList

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.topsmarteye.warehousepicking.network.*
import com.topsmarteye.warehousepicking.network.networkServices.LoadOrderService
import com.topsmarteye.warehousepicking.network.networkServices.UpdateItemService
import kotlinx.coroutines.*
import java.lang.Exception


class StockListViewModel : ViewModel() {

    //region Variables for InputAndScanFragment

    private val _eventScan = MutableLiveData<Boolean>()
    val eventScan: LiveData<Boolean>
        get() = _eventScan

    private val _eventNavigateToList = MutableLiveData<Boolean>()
    val eventNavigateToList: LiveData<Boolean>
        get() = _eventNavigateToList
    //endregion

    // The live data of order number
    private val _orderNumber = MutableLiveData<String>()
    val orderNumber: LiveData<String>
        get() = _orderNumber

    // The live data field of stock item list
    private var itemList: List<StockItem>? = null

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

    private val _eventNext = MutableLiveData<Boolean>()
    val eventNext: LiveData<Boolean>
        get() = _eventNext

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

    private val _eventDateFormatError = MutableLiveData<Exception>()
    val eventDateFormatError: LiveData<Exception>
        get() = _eventDateFormatError

    private val _eventDisableControl = MutableLiveData<Boolean>()
    val eventDisableControl: LiveData<Boolean>
        get() = _eventDisableControl

    private val _eventFinishActivity = MutableLiveData<Boolean>()
    val eventFinishActivity: LiveData<Boolean>
        get() = _eventFinishActivity

    private var stockListJob: Job? = null
    private var stockListCoroutineScope: CoroutineScope? = null

    private var inputAndScanJob = Job()
    private val inputAndScanCoroutineScope = CoroutineScope(inputAndScanJob + Dispatchers.Main)


    init {
        // Need initialization here because onRestock methods needs false checking
        _eventRestock.value = false
        _eventOutOfStock.value = false
        _eventResetOrder.value = false
        // Needed to check -- in case of outOfStock or Restock
        _isLastItem.value = false
    }

    //region Logic for InputAndScanFragment

    fun loadStockList(orderNumber: String) {
        inputAndScanCoroutineScope.launch {
            itemList = LoadOrderService.loadOrderWithStatus(orderNumber, ItemStatus.NOTPICKED)

            if (!itemList.isNullOrEmpty()) {
                _orderNumber.value = orderNumber
                _totalItems.value = itemList!!.size
                _currentIndex.value = 0
                _isLastItem.value = false
                // update currentItem and nextItem
                updateLiveDataWithIndex(0)
            }
        }
    }

    fun onScan() {
        _eventScan.value = true
    }

    fun onScanComplete() {
        _eventScan.value = false
    }

    fun onNavigationToList() {
        _eventNavigateToList.value = true
    }

    fun onNavigationToListComplete() {
        _eventNavigateToList.value = false
        LoadOrderService.resetLoadApiStatus()

        stockListJob = Job()
        stockListCoroutineScope = CoroutineScope(stockListJob!! + Dispatchers.Main)
    }

    fun onInputNetworkErrorComplete() {
        LoadOrderService.resetLoadApiStatus()
    }

    //endregion

    //region Logic for StockListFragment

    // Update the mutable live data with the index passed in
    private fun updateLiveDataWithIndex(index: Int) {
        itemList!!.let {
            _currentItem.value = it[index]

            if (index < it.size - 1) {
                _nextItem.value = it[index + 1]
            } else {
                _nextItem.value = null
                _isLastItem.value = true
            }
        }
    }

    // Called by xml to trigger action
    fun onNext() {
        _eventNext.value = true

    }

    fun onNextComplete() {
        _eventNext.value = false
        stockListCoroutineScope!!.launch {
            val currentItem = itemList!![currentIndex.value!!]
            UpdateItemService.prepareAndPutItemWithStatus(currentItem, ItemStatus.NOTPICKED)
            // check if update is successful, if true, update view LiveData
            if (UpdateItemService.checkNoError()) {
                _currentIndex.value = _currentIndex.value!! + 1
                updateLiveDataWithIndex(_currentIndex.value!!)
            }
            UpdateItemService.resetUpdateApiStatus()
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
        UpdateItemService.resetUpdateApiStatus()
    }

    fun onOutOfStock() {
        // Prevent more than one dialog window popping up
        if (_eventRestock.value == false &&
            _eventOutOfStock.value == false && _eventResetOrder.value == false) {

            _eventOutOfStock.value = true
        }
    }

    fun onOutOfStockComplete(cancelled: Boolean) {
        _eventOutOfStock.value = false

        stockListCoroutineScope!!.launch {
            if (cancelled) {
                _eventOutOfStock.value = false
                UpdateItemService.resetUpdateApiStatus()
                return@launch
            }
            val currentItem = itemList!![currentIndex.value!!]
            UpdateItemService.prepareAndPutItemWithStatus(currentItem, ItemStatus.NOTPICKED)
            // check if update is successful and is last item
            if (UpdateItemService.checkNoError()) {
                if (isLastItem.value!!) {
                    // Finish the activity if is last item
                    _eventFinishActivity.value = true
                } else {
                    _currentIndex.value = _currentIndex.value!! + 1
                    updateLiveDataWithIndex(_currentIndex.value!!)
                }
            }
            UpdateItemService.resetUpdateApiStatus()
        }
    }

    fun onResetOrder() {
        if (_eventRestock.value == false &&
            _eventOutOfStock.value == false && _eventResetOrder.value == false) {
            _eventResetOrder.value = true
        }
    }

    fun onResetOrderComplete() {
        _eventResetOrder.value = false
        UpdateItemService.resetUpdateApiStatus()
    }



    fun onFinishOrder() {
        _eventFinishOrder.value = true
    }

    fun onFinishOrderComplete() {
        _eventFinishOrder.value = false

        stockListCoroutineScope!!.launch {
            val currentItem = itemList!![currentIndex.value!!]
            UpdateItemService.prepareAndPutItemWithStatus(currentItem, ItemStatus.NOTPICKED)
            // check if update is successful, if true, finish the activity
            if (UpdateItemService.checkNoError()) {
                // this will trigger the finish of activity
                _eventFinishActivity.value = true
            }
            UpdateItemService.resetUpdateApiStatus()
        }
    }

    fun onFinishActivityComplete() {
        _eventFinishActivity.value = false
    }

    fun onDisableControl() {
        _eventDisableControl.value = true
    }

    fun onDisableControlComplete() {
        _eventDisableControl.value = false
    }

    fun onDateFormatErrorComplete() {
        _eventDateFormatError.value = null
    }

    fun onNavigateToInput() {
        stockListJob!!.cancel()
        LoadOrderService.resetResetApiStatus()
        UpdateItemService.resetUpdateApiStatus()
    }


    //endregion

    override fun onCleared() {
        super.onCleared()
        inputAndScanJob.cancel()
    }


}