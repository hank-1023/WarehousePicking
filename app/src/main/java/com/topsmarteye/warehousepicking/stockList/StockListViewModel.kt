package com.topsmarteye.warehousepicking.stockList

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.zxing.integration.android.IntentResult
import com.topsmarteye.warehousepicking.network.*
import com.topsmarteye.warehousepicking.network.networkServices.LoadOrderService
import com.topsmarteye.warehousepicking.network.networkServices.UpdateItemService
import kotlinx.coroutines.*
import java.lang.Exception


class StockListViewModel : ViewModel() {

    /* When set to true: both keyboard and touch controls are disabled
     * both the activity and StockListViewModel will listen to this variable to get information
    */
    private val _eventDisableControl = MutableLiveData<Boolean>()
    val eventDisableControl: LiveData<Boolean>
        get() = _eventDisableControl

    //region Variables for InputAndScanFragment
    private val _eventInputFragmentKeyboardScan = MutableLiveData<Boolean>()
    val eventInputFragmentKeyboardScan: LiveData<Boolean>
        get() = _eventInputFragmentKeyboardScan

    private val _eventNavigateToList = MutableLiveData<Boolean>()
    val eventNavigateToList: LiveData<Boolean>
        get() = _eventNavigateToList

    private val _inputFragmentApiStatus = MutableLiveData<ApiStatus>()
    val inputFragmentApiStatus: LiveData<ApiStatus>
        get() = _inputFragmentApiStatus

    private var inputAndScanJob = Job()
    private val inputAndScanCoroutineScope = CoroutineScope(inputAndScanJob + Dispatchers.Main)

    //endregion

    // The live data of order number
    private val _orderNumber = MutableLiveData<String>()
    val orderNumber: LiveData<String>
        get() = _orderNumber

    // The field of stock item list
    private var itemList = mutableListOf<StockItem>()

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

    private val _eventDateFormatError = MutableLiveData<Exception>()
    val eventDateFormatError: LiveData<Exception>
        get() = _eventDateFormatError

    private val _eventFinishActivity = MutableLiveData<Boolean>()
    val eventFinishActivity: LiveData<Boolean>
        get() = _eventFinishActivity

    private val _eventBarcodeConfirmError = MutableLiveData<Boolean>()
    val eventBarcodeConfirmError: LiveData<Boolean>
        get() = _eventBarcodeConfirmError

    private val _eventOrderReloaded = MutableLiveData<Boolean>()
    val eventOrderReloaded: LiveData<Boolean>
        get() = _eventOrderReloaded

    private val _eventRestockItemsAdded = MutableLiveData<Boolean>()
    val eventRestockItemsAdded: LiveData<Boolean>
        get() = _eventRestockItemsAdded

    private var stockListJob: Job? = null
    private var stockListCoroutineScope: CoroutineScope? = null

    private val _listFragmentApiStatus = MutableLiveData<ApiStatus>()
    val listFragmentApiStatus: LiveData<ApiStatus>
        get() = _listFragmentApiStatus


    init {
        // Need initialization here because onRestock methods needs false checking
        _eventRestock.value = false
        _eventOutOfStock.value = false
        _eventResetOrder.value = false
        // Needed to check -- in case of outOfStock or Restock
        _isLastItem.value = false
    }

    //region Logic for InputAndScanFragment

    // Only called once on InputAndScanFragment
    // Will reset order for order number 26084
    fun loadStockList(orderNumber: String) {
        inputAndScanCoroutineScope.launch {
            _inputFragmentApiStatus.value = ApiStatus.LOADING

            try {
                // Reset order every time before loading order: 26084
                if (orderNumber == "26084") {
                    UpdateItemService.resetOrder(orderNumber)
                }

                itemList = LoadOrderService
                    .loadOrderWithStatus(orderNumber, ItemStatus.NOTPICKED).toMutableList()
                itemList.addAll(LoadOrderService
                    .loadOrderWithStatus(orderNumber, ItemStatus.RESTOCK).toMutableList())

                if (itemList.isNotEmpty()) {
                    _orderNumber.value = orderNumber
                    _totalItems.value = itemList.size
                    _currentIndex.value = 0
                    _isLastItem.value = itemList.size <= 1
                    // update currentItem and nextItem
                    updateItemDataWithIndex(0)
                    _inputFragmentApiStatus.value = ApiStatus.DONE
                } else {
                    Log.d("StockListViewModel", "loadStockList is empty")
                    _inputFragmentApiStatus.value = ApiStatus.ERROR
                }
            } catch (e: Exception) {
                Log.d("StockListViewModel", "loadStockList error: ${e.message}")
                _inputFragmentApiStatus.value = ApiStatus.ERROR
            } finally {
                _inputFragmentApiStatus.value = ApiStatus.NONE
            }
        }
    }

    fun onInputFragmentKeyboardScan() {
        _eventInputFragmentKeyboardScan.value = true
    }

    fun onInputFragmentKeyboardScanComplete() {
        _eventInputFragmentKeyboardScan.value = false
    }

    fun onNavigationToList() {
        _eventNavigateToList.value = true
    }

    fun onNavigationToListComplete() {
        _eventNavigateToList.value = false

        stockListJob = Job()
        stockListCoroutineScope = CoroutineScope(stockListJob!! + Dispatchers.Main)
    }


    //endregion

    //region Logic for StockListFragment

    // Update the mutable live data with the index passed in
    private fun updateItemDataWithIndex(index: Int) {
        itemList.let {
            _currentItem.value = it[index]
            if (index < it.size - 1) {
                _nextItem.value = it[index + 1]
            } else {
                _nextItem.value = null
                _isLastItem.value = true
            }
        }
    }

    private fun updateViewToNext() {
        if (isLastItem.value!!) {
            checkForRestockItems()
        } else {
            _currentIndex.value = _currentIndex.value!! + 1
            updateItemDataWithIndex(_currentIndex.value!!)
        }
    }

    // Helper function for updateViewToNext()
    // Will finish activity if no extra items are returned
    // Otherwise will update the view for new items
    private fun checkForRestockItems() {
        stockListCoroutineScope!!.launch {
            _listFragmentApiStatus.value = ApiStatus.LOADING
            try {
                val reloadListResult = reloadListForStatus(ItemStatus.RESTOCK)

                if (reloadListResult) {
                    updateItemDataWithIndex(0)
                    _eventRestockItemsAdded.value = true
                } else {
                    // List is empty, finish activity
                    _eventFinishActivity.value = true
                }
                _listFragmentApiStatus.value = ApiStatus.DONE
            } catch (e: Exception) {
                Log.d(
                    "StockListViewModel",
                    "checkForRemainingItemsOrFinish exception: ${e.message}"
                )
                _listFragmentApiStatus.value = ApiStatus.ERROR
            } finally {
                _listFragmentApiStatus.value = ApiStatus.NONE
            }
        }
    }

    private suspend fun reloadListForStatus(status: ItemStatus): Boolean {
        itemList = LoadOrderService
            .loadOrderWithStatus(_orderNumber.value!!, status).toMutableList()

        return if (itemList.isNotEmpty()) {
            _totalItems.value = itemList.size
            _currentIndex.value = 0
            _isLastItem.value = itemList.size <= 1
            true
        } else {
            false
        }
    }

    // Called by xml to trigger action
    fun onNext() {
        _eventNext.value = true

    }

    fun onNextComplete(scanResult: IntentResult?) {
        _eventNext.value = false

        if (scanResult == null) {
            return
        } else if (!confirmBarcodeFromScanResult(scanResult)) {
            _eventBarcodeConfirmError.value = true
            return
        }

        stockListCoroutineScope!!.launch {

            val currentItem = itemList[currentIndex.value!!]
            currentItem.status = ItemStatus.COMPLETE.value
            currentItem.pickQuantity = currentItem.quantity

            _listFragmentApiStatus.value = ApiStatus.LOADING

            try {
                UpdateItemService.putItem(currentItem, true)
                updateViewToNext()
                _listFragmentApiStatus.value = ApiStatus.DONE
            } catch (e: Exception) {
                Log.d("StockListViewMode", "onNextComplete error ${e.message}")
                _listFragmentApiStatus.value = ApiStatus.ERROR
            } finally {
                _listFragmentApiStatus.value = ApiStatus.NONE
            }

        }
    }

    fun confirmBarcodeFromScanResult(scanResult: IntentResult?): Boolean {
        if (scanResult == null ||
            scanResult.contents.isNullOrEmpty() ||
            _currentItem.value == null ||
            _currentItem.value!!.stockBarcode == null) {
            return false
        }

        return scanResult.contents == _currentItem.value!!.stockBarcode
    }

    fun onRestock() {
        if (_eventRestock.value == false &&
            _eventOutOfStock.value == false && _eventResetOrder.value == false) {

            _eventRestock.value = true
        }
    }

    fun onRestockComplete(restockQuantity: Int?) {
        _eventRestock.value = false

        // cancelled or barcode incorrect
        if (restockQuantity == null) {return}

        stockListCoroutineScope!!.launch {
            val currentItem = itemList[currentIndex.value!!]
            currentItem.status = ItemStatus.RESTOCK.value
            currentItem.restockQuantity = restockQuantity

            _listFragmentApiStatus.value = ApiStatus.LOADING
            try {
                UpdateItemService.putItem(currentItem, false)
                updateViewToNext()
                _listFragmentApiStatus.value = ApiStatus.DONE
            } catch (e: Exception) {
                Log.d("StockListViewMode", "onRestockComplete error ${e.message}")
                _listFragmentApiStatus.value = ApiStatus.ERROR
            } finally {
                _listFragmentApiStatus.value = ApiStatus.NONE

            }
        }
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

        if (cancelled)
            return

        stockListCoroutineScope!!.launch {
            val currentItem = itemList[currentIndex.value!!]
            currentItem.status = ItemStatus.OUTOFSTOCK.value
            currentItem.outOfStockQuantity = currentItem.quantity

            _listFragmentApiStatus.value = ApiStatus.LOADING

            try {
                UpdateItemService.putItem(currentItem, false)
                updateViewToNext()
                _listFragmentApiStatus.value = ApiStatus.DONE
            } catch (e: Exception) {
                Log.d("StockListViewMode", "onOutOfStockComplete error ${e.message}")
                _listFragmentApiStatus.value = ApiStatus.ERROR
            } finally {
                _listFragmentApiStatus.value = ApiStatus.NONE
            }

        }
    }

    fun onResetOrder() {
        if (_eventRestock.value == false &&
            _eventOutOfStock.value == false && _eventResetOrder.value == false) {
            _eventResetOrder.value = true
        }
    }

    fun onResetOrderComplete(cancelled: Boolean) {
        _eventResetOrder.value = false

        if (cancelled)
            return

        stockListCoroutineScope!!.launch {
            _listFragmentApiStatus.value = ApiStatus.LOADING

            try {
                UpdateItemService.resetOrder(_orderNumber.value!!)
                val reloadResult = reloadListForStatus(ItemStatus.NOTPICKED)

                if (reloadResult) {
                    updateItemDataWithIndex(0)
                    _eventOrderReloaded.value = true
                    _listFragmentApiStatus.value = ApiStatus.DONE
                } else {
                    throw Exception("reloadListForStatus failed: List empty")
                }
            } catch (e: Exception) {
                Log.d("StockListViewModel", "onResetOrderComplete exception: ${e.message}")
                _listFragmentApiStatus.value = ApiStatus.ERROR
            } finally {
                _listFragmentApiStatus.value = ApiStatus.NONE
            }
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
        _listFragmentApiStatus.value = ApiStatus.NONE
    }

    fun onBarcodeConfirmError() {
        _eventBarcodeConfirmError.value = true
    }

    fun onBarcodeConfirmErrorComplete() {
        _eventBarcodeConfirmError.value = false
    }

    fun onOrderReloadedComplete() {
        _eventOrderReloaded.value = false
    }

    fun onRestockItemsAddedComplete() {
        _eventRestockItemsAdded.value = false
    }


    //endregion

    override fun onCleared() {
        super.onCleared()
        stockListJob?.cancel()
        inputAndScanJob.cancel()
    }


}