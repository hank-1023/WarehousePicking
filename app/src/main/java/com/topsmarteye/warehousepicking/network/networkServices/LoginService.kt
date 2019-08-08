package com.topsmarteye.warehousepicking.network.networkServices

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.topsmarteye.warehousepicking.network.RetrofitApi
import com.topsmarteye.warehousepicking.network.UserData
import java.lang.Exception

object LoginService {
    var authToken: String? = null

    private val mutableUserData = MutableLiveData<UserData>()
    val userData: LiveData<UserData>
        get() = mutableUserData

    private val loggedIn = MutableLiveData<Boolean>()
    val isLoggedIn: LiveData<Boolean>
        get() = loggedIn

    init {
        loggedIn.value = false
    }

    suspend fun updateAuthToken() {
        val tokenResponse
                = RetrofitApi.retrofitService.getLoginToken("testRest", 123456)
        if (tokenResponse.isSuccessful && !tokenResponse.body().isNullOrEmpty()) {
            authToken = tokenResponse.body()
        } else {
            throw Exception("LoginService#updateAuthToken failed to response")
        }
    }

    suspend fun loginUser(username: String, password: Int) {
        val response = RetrofitApi.retrofitService.login(authToken!!, username, password)
        if (response.isSuccessful && response.body() != null) {
            mutableUserData.value = response.body()!!.userData
            loggedIn.value = true
        } else {
            throw Exception("LoginService#loginUser failed to response")
        }
    }

    fun getUserData(): UserData? {
        return if (loggedIn.value!!) {
            // Will crash the app if tried to get userdata when not isLoggedIn
            mutableUserData.value!!
        } else {
            null
        }
    }

    fun logOut() {
        mutableUserData.value = null
        loggedIn.value = false
    }
}

