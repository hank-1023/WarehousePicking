package com.topsmarteye.warehousepicking.network

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
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

}

object LoginApi {
    var authToken: String? = null

    val retrofitService: LoginApiService by lazy {
        retrofit.create(LoginApiService::class.java)
    }

    suspend fun updateAuthToken(): Boolean {
        try {
            val tokenResponse
                    = retrofitService.getLoginToken("testRest", 123456)
            if (tokenResponse.isSuccessful) {
                authToken = tokenResponse.body()
                return true
            } else {
                Log.d("LoginApi", "Update auth token response unsuccessful")
            }
        } catch (e: Exception) {
            Log.d("LoginApi", "Update auth token failed ${e.message}")
        }

        return false
    }


}