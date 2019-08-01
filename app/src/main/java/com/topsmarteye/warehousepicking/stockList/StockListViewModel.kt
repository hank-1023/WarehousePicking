package com.topsmarteye.warehousepicking.stockList

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.topsmarteye.warehousepicking.network.ApiStatus
import com.topsmarteye.warehousepicking.network.ItemState
import com.topsmarteye.warehousepicking.network.LoginApi
import com.topsmarteye.warehousepicking.network.StockItem
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class StockListViewModel : ViewModel() {

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

    private val _eventScan = MutableLiveData<Boolean>()
    val eventScan: LiveData<Boolean>
        get() = _eventScan

    private val _loadApiStatus = MutableLiveData<ApiStatus>()
    val loadApiStatus: LiveData<ApiStatus>
        get() = _loadApiStatus

    private val _updateApiStatus = MutableLiveData<ApiStatus>()
    val updateApiStatus: LiveData<ApiStatus>
        get() = _updateApiStatus

    private val _eventNavigateToList = MutableLiveData<Boolean>()
    val eventNavigateToList: LiveData<Boolean>
        get() = _eventNavigateToList

    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)


    init {
        // Need initialization here because onRestock methods needs false checking
        _eventRestock.value = false
        _eventOutOfStock.value = false
        _eventResetOrder.value = false
    }

    fun loadStockList(orderNumber: String) {
        coroutineScope.launch {
            _loadApiStatus.value = ApiStatus.LOADING
            var response = LoginApi.retrofitService.getOrderItems(LoginApi.authToken!!, orderNumber, 0)
            if (!response.isSuccessful) {
                //update auth token if out-of-date
                if (LoginApi.updateAuthToken()) {
                    response = LoginApi.retrofitService.getOrderItems(LoginApi.authToken!!, orderNumber, 0)
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
        }
    }

    private fun onListLoaded() {
        // Reset viewModel Properties
        _totalItems.value = itemList!!.size
        _currentIndex.value = 0
        _isLastItem.value = false
        // update currentItem and nextItem
        updateDataWithIndex(0)
    }

    // Increase currentIndex by 1 and call updateDataWithIndex
    fun onNext() {
        if (_currentIndex.value!! < itemList!!.size - 1) {

            coroutineScope.launch {
                _updateApiStatus.value = ApiStatus.LOADING
                putCompleteItemForIndex(currentIndex.value!!)
                // check if update is successful
                if (_updateApiStatus.value != ApiStatus.ERROR) {
                    // Increment the current Index
                    _currentIndex.value = _currentIndex.value!! + 1
                    putWorkingItemForIndex(currentIndex.value!!)

                    // check if update is successful
                    if (_updateApiStatus.value != ApiStatus.ERROR) {
                        updateDataWithIndex(_currentIndex.value!!)
                    }
                }
            }
        }
    }

    // Usually not to be called directly
    private suspend fun putUpdateItem(item: StockItem) {
        var response = LoginApi.retrofitService.updateOrderItems(LoginApi.authToken!!, item.stockId!!, item)
        if (!response.isSuccessful) {
            if (LoginApi.updateAuthToken()) {
                response = LoginApi.retrofitService.updateOrderItems(LoginApi.authToken!!, item.stockId, item)
            } else {
                _updateApiStatus.value = ApiStatus.ERROR
                return
            }
        }

        if (response.isSuccessful) {
            _updateApiStatus.value = ApiStatus.DONE
        } else {
            _updateApiStatus.value = ApiStatus.ERROR
            Log.d("StockListViewModel", "putUpdate Item error ${response.message()}")
        }
    }

    private suspend fun putCompleteItemForIndex(index: Int) {

        itemList!![index].let { item ->
            item.updateDate = getCurrentTimeString()
            // item finished
            item.finishTime = getCurrentTimeString()
            item.status = ItemState.COMPLETE.value

            putUpdateItem(item)
        }
    }

    private suspend fun putWorkingItemForIndex(index: Int) {
        itemList!![index].let { item ->
            item.updateDate = getCurrentTimeString()
            item.status = ItemState.WORKING.value

            putUpdateItem(item)
        }

    }

    private fun getCurrentTimeString(): String {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return simpleDateFormat.format(Date())
    }

    // Update the mutable live data with the index passed in
    private fun updateDataWithIndex(index: Int) {
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

    fun onScan() {
        _eventScan.value = true
    }

    fun onScanComplete() {
        _eventScan.value = false
    }

    fun onNavigation() {
        _eventNavigateToList.value = true
    }

    fun onNavigationComplete() {
        _eventNavigateToList.value = false
        _loadApiStatus.value = ApiStatus.NONE
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }


}