package com.example.autorecorder.entity

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Login response
data class BiliAuthCodeResponse(
    val data: BiliAuthCodeData,
) {
    @Serializable
    data class BiliAuthCodeData(
        val url: String,
        @SerializedName("auth_code") val authCode: String,
    )
}

data class BiliCookieResponse(
    val data: CookieData,
)

data class BiliAuthInfoResponse(
    val data: TokenInfo
)

@Serializable
data class CookieData(
    @SerialName("cookie_info") val cookieInfo: CookieInfo,
    val sso: List<String>,
    @SerialName("token_info") val tokenInfo: TokenInfo,
    val platform: String
) {
    fun toCookieString(): String {
        return cookieInfo.cookies.joinToString("; ") { "${it.name}=${it.value}" }
    }

    companion object {
        fun fromJson(json: String): CookieData {
            return Gson().fromJson(json, CookieData::class.java)
        }
    }
}

@Serializable
data class CookieInfo(
    val cookies: List<Cookie>,
    val domains: List<String>
) {
    @Serializable
    data class Cookie(
        val expires: Long,
        @SerialName("http_only") val httpOnly: Int,
        val name: String,
        val secure: Int,
        val value: String
    )
}

@Serializable
data class TokenInfo(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    val mid: Long,
    @SerialName("refresh_token") val refreshToken: String?,
    val refresh: Boolean?
)

// upload response

data class BiliProfileResponse(
    @SerializedName("OK") val ok: Int,
    val auth: String,
    val uposUri: String,
    val chunkSize: Long,
    val bizId: Long,
)

data class BiliErrorResponse(
    val code: Long,
    val message: String,
    val ttl: Long,
)

data class BiliUploadIdResponse(
    @SerializedName("OK") val ok: Int,
    val bucket: String,
    val key: String,
    val uploadId: String,
)

data class BiliAddResponse(
    val code: Long,
    val message: String,
    val ttl: Long,
    val data: BiliAddData,
) {
    data class BiliAddData(
        val aid: Long,
        val bvid: String,
    )
}

data class BiliPart(
    @SerializedName("partNumber") val partNumber: Int,
    @SerializedName("eTag") val eTag: String = "etag",
)

data class VideoInfoRequest(
    val parts: List<BiliPart>,
)

data class AddVideoRequest(
    val copyright: Int = 1, /// 是否转载, 1-自制 2-转载
    val source: String, /// 转载来源
    val tid: Int = 171, // 投稿分区
    val cover: String = "",
    val title: String,
    val desc: String = "",
    val tag: String? = null,
    val dtime: Long? = null, /// 延时发布时间，距离提交大于4小时，格式为10位时间戳
    val videos: List<VideoInfo>,
    @SerializedName("is_only_self") val isOnlySelf: Int = 0, /// 是否仅自己可见
    val watermark: Watermark? = null,
    @SerializedName("no_reprint") val noReprint: Int = 0, /// 是否禁止转载，0-允许转载 1-禁止转载
//    val recreate: Int = -1,
) {
    data class Watermark(
        val state: Int = 0, // 0-不打水印 1-打水印
    )
}

data class VideoInfo(
    val title: String,
    val filename: String,
    val desc: String = "",
)

data class PingGetResponse(
    val lines: List<Line>,
) {
    data class Line(
        val probeUrl: String,
    )
}