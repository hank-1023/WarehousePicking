package com.topsmarteye.warehousepicking.stockList

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.topsmarteye.warehousepicking.network.ApiStatus
import com.topsmarteye.warehousepicking.network.LoginApi
import com.topsmarteye.warehousepicking.network.StockItem
import kotlinx.coroutines.*

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

    private val _apiStatus = MutableLiveData<ApiStatus>()
    val apiStatus: LiveData<ApiStatus>
        get() = _apiStatus

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
        _apiStatus.value = ApiStatus.LOADING
        coroutineScope.launch {
            var response = LoginApi.retrofitService.getOrderItems(LoginApi.authToken!!, orderNumber, 0)
            if (!response.isSuccessful) {
                LoginApi.updateAuthToken()
                response = LoginApi.retrofitService.getOrderItems(LoginApi.authToken!!, orderNumber, 0)
            }
            if (response.isSuccessful) {
                if (!response.body()?.stockList.isNullOrEmpty()) {
                    itemList = response.body()?.stockList
                    onDataLoaded()
                    _orderNumber.value = orderNumber
                    _apiStatus.value = ApiStatus.DONE
                } else {
                    _apiStatus.value = ApiStatus.ERROR
                }
            } else {
                _apiStatus.value = ApiStatus.ERROR
            }
        }
    }

    private fun onDataLoaded() {
        // Reset viewModel Properties
        _totalItems.value = itemList!!.size
        _currentIndex.value = 0
        _isLastItem.value = false
        // update currentItem and nextItem
        updateDataWithIndex(0)
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


    // Increase currentIndex by 1 and call updateDataWithIndex
    fun onNext() {
        if (_currentIndex.value!! < itemList!!.size - 1) {
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
        _apiStatus.value = ApiStatus.NONE
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }


}