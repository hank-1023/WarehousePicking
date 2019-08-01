package com.topsmarteye.warehousepicking.taskSelection

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.topsmarteye.warehousepicking.network.ApiStatus
import com.topsmarteye.warehousepicking.network.LoginApi
import com.topsmarteye.warehousepicking.network.UserStatus
import kotlinx.coroutines.*


class TaskSelectionViewModel : ViewModel() {

    private val _displayName = MutableLiveData<String>()
    val displayName: LiveData<String>
        get() = _displayName

    private val _workID = MutableLiveData<String>()
    val workID: LiveData<String>
        get() = _workID

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
            val tokenResultSuccessful = LoginApi.updateAuthToken()
            //check if token is got and check if successfully got userdata
            if (tokenResultSuccessful && UserStatus.loginUser(username, password)) {
                val userData = UserStatus.getUserData()
                _displayName.value = userData!!.displayName
                _workID.value = userData.workID
                _apiStatus.value = ApiStatus.DONE
            } else {
                _apiStatus.value = ApiStatus.ERROR
            }
        }
    }

    fun onLogOut() {
        UserStatus.logOut()
        _displayName.value = null
        _workID.value = null
    }


    fun onQRCodeError() {
        _eventQRCodeError.value = true
    }

    fun onQRCodeErrorComplete() {
        _eventQRCodeError.value = false
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }


}