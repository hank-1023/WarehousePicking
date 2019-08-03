package com.topsmarteye.warehousepicking.network.networkServices

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.topsmarteye.warehousepicking.network.GlobalApi
import com.topsmarteye.warehousepicking.network.UserData
import java.lang.Exception

object LoginService {
    var authToken: String? = null

    private var userData: UserData? = null
    private val loggedIn = MutableLiveData<Boolean>()
    val isLoggedIn: LiveData<Boolean>
        get() = loggedIn

    init {
        loggedIn.value = false
    }

    suspend fun updateAuthToken(): Boolean {
        try {
            val tokenResponse
                    = GlobalApi.retrofitService.getLoginToken("testRest", 123456)
            if (tokenResponse.isSuccessful) {
                authToken = tokenResponse.body()
                if (!authToken.isNullOrEmpty()) {
                    return true
                }
            } else {
                Log.d("LoginService", "Update auth token response unsuccessful")
            }
        } catch (e: Exception) {
            Log.d("LoginService", "Update auth token failed ${e.message}")
        }

        return false
    }

    suspend fun loginUser(username: String, password: Int): Boolean {
        try {
            val response = GlobalApi.retrofitService.login(authToken!!, username, password)
            if (response.isSuccessful && response.body() != null) {
                userData = response.body()!!.userData
                loggedIn.value = true
                return true
            } else {
                Log.d("LoginService", "login user unsuccessful ${response.message()}")
            }
        } catch (e: Exception) {
            Log.d("LoginService", "login user exception ${e.message}")
        }

        return false
    }

    fun getUserData(): UserData? {
        return if (loggedIn.value!!) {
            // Will crash the app if tried to get userdata when not isLoggedIn
            userData!!
        } else {
            null
        }
    }

    fun logOut() {
        userData = null
        loggedIn.value = false
    }
}

