package com.topsmarteye.warehousepicking.stockList

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.topsmarteye.warehousepicking.network.*
import kotlinx.coroutines.*
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import java.lang.Exception
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

    fun loadStockList(orderNumber: String) {
        coroutineScope.launch {
            _loadApiStatus.value = ApiStatus.LOADING

            try {
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
            } catch (e: Exception) {
                Log.d("loadStockList", "Load stock list network error ${e.message}")
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
        updateLiveDataWithIndex(0)
    }

    // Called by xml to trigger action
    // onNext() -> updateItemStatusForIndex(), put relevant data in -> putUpdateItem()
    // Doesn't check for indexing since nextButton should disappear on last item
    fun onNext() {
        coroutineScope.launch {
            updateItemStatusForIndex(currentIndex.value!!, ItemStatus.NOTPICKED)
            // check if update is successful, if true, update view LiveData
            if (_updateApiStatus.value != ApiStatus.ERROR) {
                _currentIndex.value = _currentIndex.value!! + 1
                updateLiveDataWithIndex(_currentIndex.value!!)
            }
            _updateApiStatus.value = ApiStatus.NONE
        }
    }

    private suspend fun updateItemStatusForIndex(index: Int, status: ItemStatus) {

        itemList!![index].let { item ->
            item.updateDate = getCurrentTimeString()
            // item finished
            item.finishTime = getCurrentTimeString()
            item.status = status.value

            item.createDate?.let {
                item.createDate = formatExistingDateString(it)
            }

            UserStatus.getUserData()!!.let {
                item.updateName = it.displayName
                item.updateBy = it.workID
                putUpdateItem(item)
            }
        }
    }

    // Usually not to be called directly
    // Can be called when network request on updating item is required
    private suspend fun putUpdateItem(item: StockItem) {
        _updateApiStatus.value = ApiStatus.LOADING
        try {
            var response = LoginApi.retrofitService.updateOrderItem(LoginApi.authToken!!, item.stockId!!, item)
            if (!response.isSuccessful) {
                if (LoginApi.updateAuthToken()) {
                    response = LoginApi.retrofitService.updateOrderItem(LoginApi.authToken!!, item.stockId, item)
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
        } catch (e: Exception) {
            Log.d("StockListViewModel", "putUpdate Item exception ${e.message}")
            _updateApiStatus.value = ApiStatus.ERROR
        }
    }


    private fun formatExistingDateString(dateString: String): String? {
        var formattedString: String? = null

        val fmtWithSeconds = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").withLocale(Locale.CHINA)
        val fmtNoSecond = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm").withLocale(Locale.CHINA)

        try {
            val dt = fmtWithSeconds.parseDateTime(dateString).withZone(DateTimeZone.forID("Asia/Shanghai"))
            formattedString = dt.toString()
        } catch (e: Exception) {
            try {
                val dt = fmtNoSecond.parseDateTime(dateString).withZone(DateTimeZone.forID("Asia/Shanghai"))
                formattedString = dt.toString()
            } catch (e: Exception) {
                Log.d("formatDateString", "Unknown date format ${e.message}")
            }
        } finally {
            return formattedString
        }
    }

    private fun getCurrentTimeString(): String {
        val dt = DateTime()
        return dt.toString()
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

    fun onRestock() {
        if (_eventRestock.value == false &&
            _eventOutOfStock.value == false && _eventResetOrder.value == false) {

            _eventRestock.value = true
        }
    }

    fun onRestockComplete() {
        _eventRestock.value = false
        _updateApiStatus.value = ApiStatus.NONE
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
                updateItemStatusForIndex(currentIndex.value!!, ItemStatus.NOTPICKED)
                // check if update is successful and is last item
                if (_updateApiStatus.value != ApiStatus.ERROR) {
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
            _updateApiStatus.value = ApiStatus.NONE
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
        _updateApiStatus.value = ApiStatus.NONE
    }

    fun onFinishOrder() {
        _eventFinishOrder.value = true
    }

    fun onFinishOrderComplete() {
        coroutineScope.launch {
            updateItemStatusForIndex(currentIndex.value!!, ItemStatus.NOTPICKED)
            // check if update is successful, if true, finish the activity
            if (_updateApiStatus.value != ApiStatus.ERROR) {
                // this will trigger the finish of activity
                _eventFinishActivity.value = true
            }
            _eventFinishOrder.value = false
            _updateApiStatus.value = ApiStatus.NONE
        }
    }

    fun onFinishActivityComplete() {
        _eventFinishActivity.value = false
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

    fun onInputNetworkErrorComplete() {
        _loadApiStatus.value = ApiStatus.NONE
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

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }


}