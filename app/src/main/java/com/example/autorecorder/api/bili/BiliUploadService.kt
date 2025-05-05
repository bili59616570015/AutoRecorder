package com.example.autorecorder.api.bili

import com.example.autorecorder.common.SharedPreferencesHelper
import com.example.autorecorder.entity.AddVideoRequest
import com.example.autorecorder.entity.BiliAddResponse
import com.example.autorecorder.entity.BiliProfileResponse
import com.example.autorecorder.entity.BiliUploadIdResponse
import com.example.autorecorder.entity.PingGetResponse
import com.example.autorecorder.entity.VideoInfoRequest
import com.google.gson.annotations.SerializedName
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.time.Instant

interface BiliUploadService {
    // member, bili stateful
    @GET("preupload")
    suspend fun preupload(
        @Header("User-Agent") userAgent: String = UserAgent,
        @Header("Referer") referer: String = "https://www.bilibili.com",
        @Header("Cookie") cookie: String,
        @Query("profile") profile: String = "ugcupos/bup",
        @Query("name") name: String,
        @Query("size") size: Long,
        @Query("r") r: String = "upos",
        @Query("ssl") ssl: String = "0",
        @Query("version") version: String = "2.11.0",
        @Query("build") build: String = "2100400",
        @Query("upcdn") upcdn: String = SharedPreferencesHelper.upCdn,
        @Query("probe_version") probeVersion: String = "20221109",
    ): BiliProfileResponse

    // upload, stateless client.client_with_middleware
    @POST("{path}")
    suspend fun getUploadId(
        @Header("X-Upos-Auth") auth: String,
        @Header("User-Agent") userAgent: String = UserAgent,
        @Path("path") path: String,
        @Query("output") output: String = "json",
        @Query("uploads") uploads: String = "",
    ): BiliUploadIdResponse

    // upload, stateless client.client
    @PUT("{path}")
    suspend fun upload(
        @Header("X-Upos-Auth") auth: String,
        @Header("User-Agent") userAgent: String = UserAgent,
        @Path("path") path: String,
        @Query("uploadId") uploadId: String,
        @Query("chunks") chunksNum: Int,
        @Query("total") totalSize: Long,
        @Query("chunk") index: Int,
        @Query("size") size: Long,
        @Query("partNumber") partNumber: Int = index + 1,
        @Query("start") start: Long,
        @Query("end") end: Long,
        @Body filePart: RequestBody
    ): Response<Void>  // The return type can be customized as per response

    // upload, stateless client.client_with_middleware
    @POST("{path}")
    suspend fun endUpload(
        @Header("X-Upos-Auth") auth: String,
        @Header("Referer") referer: String = "https://www.bilibili.com/",
        @Header("User-Agent") userAgent: String = UserAgent,
        @Path("path") path: String,
        @Query("name") name: String,
        @Query("uploadId") uploadId: String,
        @Query("biz_id") bizId: Long,
        @Query("profile") profile: String = "ugcupos/bup",
        @Query("output") output: String = "json",
        @Body request: VideoInfoRequest,
    )

    // member
    @POST("x/vu/app/add")
    suspend fun addVideo(
        @Header("Content-Type") contentType: String = "application/json; charset=UTF-8",
        @Header("User-Agent") userAgent: String = "Mozilla/5.0 BiliDroid/7.80.0 (bbcallen@gmail.com) os/android model/MI 6 mobi_app/android build/7800300 channel/bili innerVer/7800310 osVer/13 network/2",
        @Header("Referer") referer: String = "https://www.bilibili.com/",
        @Query("access_key") accessKey: String,
        @Query("appkey") appkey: String = AppKey,
        @Query("build") build: Int = 7800300,
        @Query("c_locale") cLocale: String = "zh-Hans_CN",
        @Query("channel") channel: String = "bili",
        @Query("disable_rcmd") disableRcmd: Int = 0,
        @Query("mobi_app") mobiApp: String = "android",
        @Query("platform") platform: String = "android",
        @Query("s_locale") sLocale: String = "zh-Hans_CN",
        @Query("statistics") statistics: String = "\"appId\":1,\"platform\":3,\"version\":\"7.80.0\",\"abtest\":\"\"",
        @Query("ts") ts: Long = Instant.now().epochSecond,
        @Query("sign") sign: String,
        @Body request: AddVideoRequest
    ): BiliAddResponse

    @GET("preupload")
    suspend fun pingPreload(
        @Query("r") r: String = "probe",
    ): PingGetResponse

    @POST("OK")
    suspend fun pingUpload(
        @Body request: RequestBody,
    ): Response<Void>
}