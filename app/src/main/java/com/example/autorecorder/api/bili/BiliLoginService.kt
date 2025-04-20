package com.example.autorecorder.api.bili

import com.example.autorecorder.entity.BiliAuthCodeResponse
import com.example.autorecorder.entity.BiliAuthInfoResponse
import com.example.autorecorder.entity.BiliCookieResponse
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface BiliLoginService {
    @FormUrlEncoded
    @POST("x/passport-tv-login/qrcode/auth_code")
    suspend fun getAuthCode(
        @Header("Referer") referer: String = "https://www.bilibili.com/",
        @Field("appkey") appkey: String = AppKey,
        @Field("local_id") localId: String = "0",
        @Field("ts") ts: Long,
        @Field("sign") sign: String,
    ): BiliAuthCodeResponse

    @FormUrlEncoded
    @POST("x/passport-tv-login/qrcode/poll")
    suspend fun loginByQrCode(
        @Header("Referer") referer: String = "https://www.bilibili.com/",
        @Field("appkey") appkey: String = AppKey,
        @Field("auth_code") authCode: String,
        @Field("local_id") localId: String = "0",
        @Field("ts") ts: Long,
        @Field("sign") sign: String,
    ): BiliCookieResponse

    @GET("x/passport-login/oauth2/info")
    suspend fun getAuthInfo(
        @Header("Cookie") cookie: String,
        @Query("access_key") accessKey: String,
        @Query("actionKey") actionKey: String = "appkey",
        @Query("appkey") appkey: String = AppKey,
        @Query("ts") ts: Long,
        @Query("sign") sign: String,
    ): BiliAuthInfoResponse

    @FormUrlEncoded
    @POST("x/passport-login/oauth2/refresh_token")
    suspend fun refreshToken(
        @Field("access_key") accessKey: String,
        @Field("actionKey") actionKey: String = "appkey",
        @Field("appkey") appkey: String = AppKey,
        @Field("refresh_token") refreshToken: String,
        @Field("ts") ts: Long,
        @Field("sign") sign: String,
    ): BiliCookieResponse
}

