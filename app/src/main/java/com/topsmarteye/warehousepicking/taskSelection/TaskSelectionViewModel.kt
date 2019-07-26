package com.topsmarteye.warehousepicking.taskSelection

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.topsmarteye.warehousepicking.network.LoginApi
import kotlinx.coroutines.*

class TaskSelectionViewModel : ViewModel() {

    private val _displayName = MutableLiveData<String>()
    val displayName: LiveData<String>
        get() = _displayName

    private val _workID = MutableLiveData<String>()
    val workID: LiveData<String>
        get() = _workID

    private val _isLoggedIn = MutableLiveData<Boolean>()
    val isLoggedIn: LiveData<Boolean>
        get() = _isLoggedIn

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    private val _isNetworkError = MutableLiveData<Boolean>()
    val isNetworkError: LiveData<Boolean>
        get() = _isNetworkError

    private val _isQRCodeError = MutableLiveData<Boolean>()
    val isQRCodeError: LiveData<Boolean>
        get() = _isQRCodeError

    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    init {
        _isLoggedIn.value = false
    }

    fun getTokenAndLogin(username: String, password: Int) {
        _isLoading.value = true
        coroutineScope.launch {
            try {
                val tokenResponse
                        = LoginApi.retrofitService.getLoginToken("testRest", 123456)
                if (tokenResponse.isSuccessful) {
                    val token = tokenResponse.body()!!

                    val loginResponse = LoginApi.retrofitService.login(token, username, password)
                    if (loginResponse.isSuccessful) {
                        val userProperty = loginResponse.body()!!
                        _isLoggedIn.value = true
                        _displayName.value = userProperty.data.displayName
                        _workID.value = userProperty.data.workID
                    } else {
                        _isNetworkError.value = true
                    }
                } else {
                    _isNetworkError.value = true
                }
            } catch (e: Exception) {
                Log.d("TaskSelectionViewModel", "Failed login: ${e.message}")
                _isNetworkError.value = true
            }
            _isLoading.value = false
        }
    }

    fun onLogOut() {
        _isLoggedIn.value = false
        _displayName.value = null
    }

    fun onNetworkErrorComplete() {
        _isNetworkError.value = false
    }

    fun onQRCodeError() {
        _isQRCodeError.value = true
    }

    fun onQRCodeErrorComplete() {
        _isQRCodeError.value = false
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }


}