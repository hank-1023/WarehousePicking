package com.topsmarteye.warehousepicking.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.*


private const val BASE_URL = "https://www.sh-jinyi.tech/jeecg/rest/"

private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

private val retrofit = Retrofit.Builder()
    .addConverterFactory(ScalarsConverterFactory.create())
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(BASE_URL)
    .build()

interface LoginApiService {
    @POST("tokens")
    suspend fun getLoginToken(@Query("username") username: String,
                              @Query("password") password: Int): Response<String>

    @POST("deviceses/{username}/{password}")
    suspend fun login(@Header("X-AUTH-TOKEN") authToken: String,
                      @Path(value = "username", encoded = false) username: String,
                      @Path("password") password: Int): Response<UserProperty>
}

object LoginApi {
    val retrofitService: LoginApiService by lazy {
        retrofit.create(LoginApiService::class.java)
    }
}