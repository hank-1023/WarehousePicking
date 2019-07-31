package com.topsmarteye.warehousepicking.taskSelection

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.topsmarteye.warehousepicking.network.ApiStatus
import com.topsmarteye.warehousepicking.network.LoginApi
import kotlinx.coroutines.*
import java.lang.Exception


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

    private val _apiStatus = MutableLiveData<ApiStatus>()
    val apiStatus: LiveData<ApiStatus>
        get() = _apiStatus

    private val _isQRCodeError = MutableLiveData<Boolean>()
    val isQRCodeError: LiveData<Boolean>
        get() = _isQRCodeError


    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    init {
        _isLoggedIn.value = false
    }

    fun getTokenAndLogin(username: String, password: Int) {
        _apiStatus.value = ApiStatus.LOADING
        coroutineScope.launch {
            val tokenResultSuccessful = LoginApi.updateAuthToken()
            if (tokenResultSuccessful && LoginApi.authToken != null && LoginApi.authToken != "") {
                val token = LoginApi.authToken!!
                getUserData(token, username, password)
            } else {
                _apiStatus.value = ApiStatus.ERROR
            }
        }
    }

    private suspend fun getUserData(token: String, username: String, password: Int) {

        try {
            val loginResponse = LoginApi.retrofitService.login(token, username, password)
            if (loginResponse.isSuccessful) {
                val userProperty = loginResponse.body()!!
                _isLoggedIn.value = true
                _displayName.value = userProperty.userData.displayName
                _workID.value = userProperty.userData.workID
                _apiStatus.value = ApiStatus.DONE
            } else {
                _apiStatus.value = ApiStatus.ERROR
            }
        } catch (e: Exception) {
            Log.d("TaskSelectionViewModel", "getUserData failed ${e.message}")
        }
    }

    fun onLogOut() {
        _isLoggedIn.value = false
        _displayName.value = null
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