package com.topsmarteye.warehousepicking.network

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.*
import java.lang.Exception


private const val BASE_URL = "https://www.sh-jinyi.tech/jeecg/rest/"

private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

private val retrofit = Retrofit.Builder()
    .addConverterFactory(ScalarsConverterFactory.create())
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(BASE_URL)
    .build()

enum class ApiStatus { LOADING, ERROR, DONE, NONE }

interface LoginApiService {
    @POST("tokens")
    suspend fun getLoginToken(@Query("username") username: String,
                              @Query("password") password: Int): Response<String>

    @POST("deviceses/{username}/{password}")
    suspend fun login(@Header("X-AUTH-TOKEN") authToken: String,
                      @Path(value = "username", encoded = false) username: String,
                      @Path("password") password: Int): Response<UserProperty>

    @GET("pPickDetialController/list/{taskID}")
    suspend fun getOrderItems(@Header("X-AUTH-TOKEN") authToken: String,
                              @Path("taskID", encoded = false) taskID: String,
                              @Query("statuts") status: Int): Response<StockProperty>

    @PUT("pPickDetialController/{stockID}")
    suspend fun updateOrderItems(@Header("X-AUTH-TOKEN") authToken: String,
                                 @Path("stockID", encoded = false) stockID: String,
                                 @Body item: StockItem): Response<Void>
}

object LoginApi {
    val retrofitService: LoginApiService by lazy {
        retrofit.create(LoginApiService::class.java)
    }

    var authToken: String? = null


    suspend fun updateAuthToken(): Boolean {
        try {
            val tokenResponse
                    = retrofitService.getLoginToken("testRest", 123456)
            if (tokenResponse.isSuccessful) {
                authToken = tokenResponse.body()
                if (!authToken.isNullOrEmpty()) {
                    return true
                }
            } else {
                Log.d("LoginApi", "Update auth token response unsuccessful")
            }
        } catch (e: Exception) {
            Log.d("LoginApi", "Update auth token failed ${e.message}")
        }

        return false
    }

}

object UserStatus {
    private var userData: UserData? = null
    private val loggedIn = MutableLiveData<Boolean>()
    val isLoggedIn: LiveData<Boolean>
        get() = loggedIn

    init {
        loggedIn.value = false
    }

    suspend fun loginUser(username: String, password: Int): Boolean {
        try {
            val response = LoginApi.retrofitService.login(LoginApi.authToken!!, username, password)
            if (response.isSuccessful && response.body() != null) {
                userData = response.body()!!.userData
                loggedIn.value = true
                return true
            } else {
                Log.d("UserStatus", "login user unsuccessful ${response.message()}")
            }
        } catch (e: Exception) {
            Log.d("UserStatus", "login user exception ${e.message}")
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