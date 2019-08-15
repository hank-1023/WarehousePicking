package com.topsmarteye.warehousepicking.taskSelection

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.topsmarteye.warehousepicking.network.ApiStatus
import com.topsmarteye.warehousepicking.network.networkServices.LoginService
import kotlinx.coroutines.*
import java.lang.Exception


class TaskSelectionViewModel : ViewModel() {

    private val _apiStatus = MutableLiveData<ApiStatus>()
    val apiStatus: LiveData<ApiStatus>
        get() = _apiStatus

    private val _eventQRCodeError = MutableLiveData<Boolean>()
    val eventQRCodeError: LiveData<Boolean>
        get() = _eventQRCodeError


    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)


    fun getTokenAndLogin(username: String, password: Int) {
        _apiStatus.value = ApiStatus.LOADING
        coroutineScope.launch {

            try {
                LoginService.updateAuthToken()
                LoginService.loginUser(username, password)
                _apiStatus.value = ApiStatus.DONE
            } catch (e: Exception) {
                Log.d("TaskSelectionViewModel", "getTokenAndLogin failed ${e.message}")
                _apiStatus.value = ApiStatus.ERROR
            } finally {
                _apiStatus.value = ApiStatus.NONE
            }
        }
    }

    fun resetNetworkStatus() {
        _apiStatus.value = ApiStatus.NONE
    }

    fun onLogOut() {
        LoginService.logOut()
    }


    fun onQRCodeError() {
        _eventQRCodeError.value = true
    }

    fun onQRCodeErrorComplete() {
        _eventQRCodeError.value = false
        resetNetworkStatus()
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }


}