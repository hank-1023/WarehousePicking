package com.topsmarteye.warehousepicking.stockList

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.topsmarteye.warehousepicking.network.*
import com.topsmarteye.warehousepicking.network.networkServices.LoadOrderService
import com.topsmarteye.warehousepicking.network.networkServices.UpdateItemService
import kotlinx.coroutines.*
import java.lang.Exception


class StockListViewModel : ViewModel() {

    /* When set to true: both keyboard and touch controls are disabled
    both the activity and StockListViewModel will listen to this variable to get information
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

    //endregion

    // The live data of order number
    private val _orderNumber = MutableLiveData<String>()
    val orderNumber: LiveData<String>
        get() = _orderNumber

    // The live data field of stock item list
    private var itemList: MutableList<StockItem>? = null

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

    private val _eventFinishActivity = MutableLiveData<Boolean>()
    val eventFinishActivity: LiveData<Boolean>
        get() = _eventFinishActivity

    private var stockListJob: Job? = null
    private var stockListCoroutineScope: CoroutineScope? = null

    private val _listFragmentApiStatus = MutableLiveData<ApiStatus>()
    val listFragmentApiStatus: LiveData<ApiStatus>
        get() = _listFragmentApiStatus

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
            _inputFragmentApiStatus.value = ApiStatus.LOADING

            try {
                itemList = LoadOrderService
                    .loadOrderWithStatus(orderNumber, ItemStatus.NOTPICKED).toMutableList()
                _orderNumber.value = orderNumber
                _totalItems.value = itemList!!.size
                _currentIndex.value = 0
                _isLastItem.value = itemList!!.size <= 1
                // update currentItem and nextItem
                updateLiveDataWithIndex(0)
                _inputFragmentApiStatus.value = ApiStatus.DONE
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

    private suspend fun checkRestockItems() {
        val restockItems = stockListCoroutineScope!!.async {
            LoadOrderService.loadOrderWithStatus(_orderNumber.value!!, ItemStatus.RESTOCK)
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
            currentItem.status = ItemStatus.NOTPICKED.value

            _listFragmentApiStatus.value = ApiStatus.LOADING

            try {
                UpdateItemService.putItemWithStatus(currentItem, true)
                _currentIndex.value = _currentIndex.value!! + 1
                updateLiveDataWithIndex(_currentIndex.value!!)
                _listFragmentApiStatus.value = ApiStatus.DONE
            } catch (e: Exception) {
                Log.d("StockListViewMode", "onNextComplete error ${e.message}")
                _listFragmentApiStatus.value = ApiStatus.ERROR
            } finally {
                _listFragmentApiStatus.value = ApiStatus.NONE
            }

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
        _listFragmentApiStatus.value = ApiStatus.NONE
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
                _listFragmentApiStatus.value = ApiStatus.NONE
                return@launch
            }
            val currentItem = itemList!![currentIndex.value!!]
            currentItem.status = ItemStatus.NOTPICKED.value
            currentItem.outOfStockQuantity = currentItem.quantity

            _listFragmentApiStatus.value = ApiStatus.LOADING

            try {
                UpdateItemService.putItemWithStatus(currentItem, false)
                if (isLastItem.value!!) {
                    // Finish the activity if is last item
                    _eventFinishActivity.value = true
                } else {
                    _currentIndex.value = _currentIndex.value!! + 1
                    updateLiveDataWithIndex(_currentIndex.value!!)
                }
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

    fun onResetOrderComplete() {
        _eventResetOrder.value = false
        _listFragmentApiStatus.value = ApiStatus.NONE
    }



    fun onFinishOrder() {
        _eventFinishOrder.value = true
    }

    fun onFinishOrderComplete() {
        _eventFinishOrder.value = false

        stockListCoroutineScope!!.launch {
            val currentItem = itemList!![currentIndex.value!!]
            currentItem.status = ItemStatus.NOTPICKED.value

            _listFragmentApiStatus.value = ApiStatus.LOADING

            try {
                UpdateItemService.putItemWithStatus(currentItem, true)
                _eventFinishActivity.value = true
                _listFragmentApiStatus.value = ApiStatus.DONE
            } catch (e: Exception) {
                Log.d("StockListViewMode", "onFinishOrderComplete error ${e.message}")
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


    //endregion

    override fun onCleared() {
        super.onCleared()
        stockListJob?.cancel()
        inputAndScanJob.cancel()
    }


}