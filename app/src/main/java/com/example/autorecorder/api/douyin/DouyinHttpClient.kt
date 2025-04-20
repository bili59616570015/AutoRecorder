package com.example.autorecorder.api.douyin

import com.example.autorecorder.entity.DouyinApiService
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object DouyinHttpClient {
    private const val BASE_URL = "https://live.douyin.com/"

    // Create an OkHttp client with timeouts (optional, but good practice)
    private val okHttpClient = OkHttpClient.Builder()
        .cookieJar(object : CookieJar {
            private val cookies = mutableListOf<Cookie>()

            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                this.cookies.addAll(cookies)
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookies
            }
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.NONE
            }
        )
        .build()

    // Retrofit instance
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(
            GsonConverterFactory.create(
                GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES) // Convert snake_case to camelCase
                    .create()
            )
        )
        .build()

    val apiService: DouyinApiService = retrofit.create(DouyinApiService::class.java)

    fun randomUserAgent(): String {
        // List of mobile models
        val mobileModels = listOf(
            "SM-G981B", "SM-G9910", "SM-S9080", "SM-S9110", "SM-S921B",
            "Pixel 5", "Pixel 6", "Pixel 7", "Pixel 7 Pro", "Pixel 8"
        )

        // Random Android version between 9 and 14
        val androidVersion = Random.nextInt(9, 15)  // 15 is exclusive

        // Random mobile model from the list
        val mobile = mobileModels.random()

        // Random Chrome version
        val chromeVersion = Random.nextInt(70, 110)  // Random Chrome version between 70 and 110

        // Constructing the user agent string
        return "Mozilla/5.0 (Linux; Android $androidVersion; $mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$chromeVersion.0.0.0 Mobile Safari/537.36"
    }
}
