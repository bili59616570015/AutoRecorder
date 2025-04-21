package com.example.autorecorder.api.bili

import android.util.Log
import com.example.autorecorder.BuildConfig
import com.example.autorecorder.common.SharedPreferencesHelper
import com.example.autorecorder.entity.BiliErrorResponse
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.ktor.client.plugins.logging.Logger
import kotlinx.coroutines.delay
import okhttp3.ConnectionPool
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

const val AppKey = "4409e2ce8ffd12b8"
const val AppSecret = "59b43e04ad6965f34319062b478f83dd"
const val UserAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/63.0.3239.108"

object BilibiliHttpClient {
    val uploadClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .connectionPool(ConnectionPool(50, 5, TimeUnit.MINUTES)) // 🚀 提升连接池性能
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(ErrorInterceptor())
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }
        )
        .build()

    private fun retrofit(url: String): Retrofit = Retrofit.Builder()
        .baseUrl(url)
        .client(okHttpClient)
        .addConverterFactory(
            GsonConverterFactory.create(
                GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES) // Convert snake_case to camelCase
                    .create()
            )
        )
        .build()

    fun uploadService(endPoint: BiliEndPoint): BiliUploadService = retrofit(endPoint.url).create(BiliUploadService::class.java)
    val loginService: BiliLoginService = retrofit(BiliEndPoint.PASSPORT.url).create(BiliLoginService::class.java)
    fun pingService(url: String): BiliUploadService = retrofit(url).create(BiliUploadService::class.java)
}

enum class BiliEndPoint(val url: String) {
    PASSPORT("https://passport.bilibili.com/"),
    MEMBER("https://member.bilibili.com/"),
    UPLOAD("https://upos-cs-upcdn${SharedPreferencesHelper.upCdn}.bilivideo.com/"),
}

class CustomHttpLogger(): Logger {
    override fun log(message: String) {
        Log.d("uploader", message) // Or whatever logging system you want here
    }
}

suspend fun <T> retry(
    times: Int = 3,
    initialDelay: Long = 100,
    maxDelay: Long = 1000,
    factor: Double = 2.0,
    block: suspend () -> Result<T>
): Result<T> {
    var currentDelay = initialDelay
    var result: Result<T>

    repeat(times - 1) {
        result = block()
        if (result.isSuccess) return result
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }

    return block()
}

sealed class DataState {
    data object Ready: DataState()
    data object Loaded: DataState()
    data class Error(val message: String): DataState()
}

val gson = Gson()
fun <T>T.toJson(): String = gson.toJson(this)
inline fun <reified T> String.fromJson(): T = gson.fromJson(this, T::class.java)

class ErrorInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalResponse = chain.proceed(chain.request())
        val responseBody = originalResponse.body.string()

        return try {
            val jsonObject = JSONObject(responseBody)
            val code = jsonObject.optInt("code")
            // Check if it's an error response
            if (jsonObject.has("code") && code != 0) {
                val errorResponse = Gson().fromJson(responseBody, BiliErrorResponse::class.java)

                // Create an error response for Retrofit
                return Response.Builder()
                    .code(400) // Treat it as a client error
                    .message(errorResponse.message)
                    .protocol(originalResponse.protocol)
                    .request(originalResponse.request)
                    .body(responseBody.toResponseBody(originalResponse.body.contentType()))
                    .build()
            }

            // If it's a normal response, return as is
            originalResponse.newBuilder()
                .body(responseBody.toResponseBody(originalResponse.body?.contentType()))
                .build()
        } catch (e: Exception) {
            originalResponse // If parsing fails, return original response
        }
    }
}