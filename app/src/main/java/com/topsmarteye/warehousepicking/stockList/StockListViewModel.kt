package com.topsmarteye.warehousepicking.stockList

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.topsmarteye.warehousepicking.network.*
import kotlinx.coroutines.*
import java.lang.Exception


class StockListViewModel : ViewModel() {

    //region Variables for InputAndScanFragment

    private val _eventScan = MutableLiveData<Boolean>()
    val eventScan: LiveData<Boolean>
        get() = _eventScan

    private val _loadApiStatus = MutableLiveData<ApiStatus>()
    val loadApiStatus: LiveData<ApiStatus>
        get() = _loadApiStatus

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

    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)


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
        coroutineScope.launch {
            _loadApiStatus.value = ApiStatus.LOADING

            try {
                var response = GlobalApi.retrofitService
                    .getOrderItems(LoginApi.authToken!!, orderNumber, 0)
                if (!response.isSuccessful) {
                    //update auth token if out-of-date
                    if (LoginApi.updateAuthToken()) {
                        response = GlobalApi.retrofitService
                            .getOrderItems(LoginApi.authToken!!, orderNumber, 0)
                    } else {
                        _loadApiStatus.value = ApiStatus.ERROR
                        return@launch
                    }
                }
                if (response.isSuccessful) {
                    if (!response.body()?.stockList.isNullOrEmpty()) {
                        itemList = response.body()?.stockList
                        onListLoaded()
                        _orderNumber.value = orderNumber
                        _loadApiStatus.value = ApiStatus.DONE
                    } else {
                        _loadApiStatus.value = ApiStatus.ERROR
                    }
                } else {
                    _loadApiStatus.value = ApiStatus.ERROR
                }
            } catch (e: Exception) {
                Log.d("loadStockList", "Load stock list network error ${e.message}")
                _loadApiStatus.value = ApiStatus.ERROR
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
        _loadApiStatus.value = ApiStatus.NONE
    }

    fun onInputNetworkErrorComplete() {
        _loadApiStatus.value = ApiStatus.NONE
    }

    //endregion

    //region Logic for StockListFragment

    private fun onListLoaded() {
        // Reset viewModel Properties
        _totalItems.value = itemList!!.size
        _currentIndex.value = 0
        _isLastItem.value = false
        // update currentItem and nextItem
        updateLiveDataWithIndex(0)
    }

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
        coroutineScope.launch {
            val currentItem = itemList!![currentIndex.value!!]
            UpdateItemApi.prepareAndPutItemWithStatus(currentItem, ItemStatus.NOTPICKED)
            // check if update is successful, if true, update view LiveData
            if (UpdateItemApi.checkNoError()) {
                _currentIndex.value = _currentIndex.value!! + 1
                updateLiveDataWithIndex(_currentIndex.value!!)
            }
            UpdateItemApi.resetUpdateApiStatus()
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
        UpdateItemApi.resetUpdateApiStatus()
    }

    fun onOutOfStock() {
        // Prevent more than one dialog window popping up
        if (_eventRestock.value == false &&
            _eventOutOfStock.value == false && _eventResetOrder.value == false) {

            _eventOutOfStock.value = true
        }
    }

    fun onOutOfStockComplete(cancelled: Boolean) {
        coroutineScope.launch {
            if (!cancelled) {
                val currentItem = itemList!![currentIndex.value!!]
                UpdateItemApi.prepareAndPutItemWithStatus(currentItem, ItemStatus.NOTPICKED)
                // check if update is successful and is last item
                if (UpdateItemApi.checkNoError()) {
                    if (isLastItem.value!!) {
                        // Finish the activity if is last item
                        _eventFinishActivity.value = true
                    } else {
                        _currentIndex.value = _currentIndex.value!! + 1
                        updateLiveDataWithIndex(_currentIndex.value!!)
                    }
                }
            }
            _eventOutOfStock.value = false
            UpdateItemApi.resetUpdateApiStatus()
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
        UpdateItemApi.resetUpdateApiStatus()
    }

    fun onFinishOrder() {
        _eventFinishOrder.value = true
    }

    fun onFinishOrderComplete() {
        coroutineScope.launch {
            val currentItem = itemList!![currentIndex.value!!]
            UpdateItemApi.prepareAndPutItemWithStatus(currentItem, ItemStatus.NOTPICKED)
            // check if update is successful, if true, finish the activity
            if (UpdateItemApi.checkNoError()) {
                // this will trigger the finish of activity
                _eventFinishActivity.value = true
            }
            _eventFinishOrder.value = false
            UpdateItemApi.resetUpdateApiStatus()
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

    //endregion

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }


}