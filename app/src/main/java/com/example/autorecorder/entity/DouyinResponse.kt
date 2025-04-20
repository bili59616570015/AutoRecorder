package com.example.autorecorder.entity

import com.example.autorecorder.api.douyin.DouyinHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query


data class LiveRoom(
    val roomId: String,
    val userId: String?,
    val status: Int,
) {
    val statusString: String
        get() = when (status) {
            2 -> "直播中"
            else -> "空闲"
        }
}

interface DouyinApiService {
    @GET("1-2-3-4-5-6-7-8-9-0")
    suspend fun getPage(): Response<ResponseBody>

    @GET("webcast/room/web/enter/")
    suspend fun getRoom(
        @Query("web_rid") webRid: String,
        @Query("aid") aid: String = "6383",
        @Query("device_platform") devicePlatform: String = "web",
        @Query("browser_language") browserLanguage: String = "zh-CN",
        @Query("browser_platform") browserPlatform: String = "Win32",
        @Query("browser_name") browserName: String = "Mozilla",
        @Query("browser_version") browserVersion: String = "5.0",
        @Header("User-Agent") userAgent: String = DouyinHttpClient.randomUserAgent(),
        @Header("Cookie") cookie: String
    ): RoomResponse
}

data class RoomResponse(
    val data: Enter,
) {
    data class Enter(
        val data: List<Data>,
        val enterRoomId: String,
        val userId: String
    ) {
        data class Data(
            val status: Int,
            val title: String,
        )
    }

    fun toEntity(): LiveRoom {
        return LiveRoom(
            roomId = data.enterRoomId,
            userId = data.userId,
            status = data.data.firstOrNull()?.status ?: -1
        )
    }
}