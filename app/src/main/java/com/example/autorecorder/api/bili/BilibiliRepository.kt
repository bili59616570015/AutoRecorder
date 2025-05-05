package com.example.autorecorder.api.bili

import com.example.autorecorder.common.Utils
import com.example.autorecorder.entity.AddVideoRequest
import com.example.autorecorder.entity.BiliAddResponse
import com.example.autorecorder.entity.BiliPart
import com.example.autorecorder.entity.BiliProfileResponse
import com.example.autorecorder.entity.BiliUploadIdResponse
import com.example.autorecorder.entity.CookieData
import com.example.autorecorder.entity.PingGetResponse
import com.example.autorecorder.entity.Task
import com.example.autorecorder.entity.Template
import com.example.autorecorder.entity.VideoInfo
import com.example.autorecorder.entity.VideoInfoRequest
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import retrofit2.Response
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.Date

class BilibiliRepository {
    suspend fun getAuthInfo(cookie: CookieData): Boolean {
        val accessKey = cookie.tokenInfo.accessToken
        val ts = Instant.now().epochSecond
        val params = "access_key=$accessKey&actionKey=appkey&appkey=$AppKey&ts=$ts"
        val signString = sign(params)
        return BilibiliHttpClient.loginService.getAuthInfo(
            accessKey = accessKey,
            sign = signString,
            ts = ts,
            cookie = cookie.toCookieString()
        ).data.refresh == false
    }

    suspend fun refreshToken(cookie: CookieData) {
        val accessKey = cookie.tokenInfo.accessToken
        val refreshToken = cookie.tokenInfo.refreshToken ?: ""
        val ts = Instant.now().epochSecond
        val params = "access_key=$accessKey&actionKey=appkey&appkey=$AppKey&refresh_token=$refreshToken&ts=$ts"
        val signString = sign(params)
        val newCookie = BilibiliHttpClient.loginService.refreshToken(
            accessKey = accessKey,
            refreshToken = cookie.tokenInfo.refreshToken ?: "",
            sign = signString,
            ts = ts,
        ).data
        Utils.saveCookieToFile(newCookie)
    }

    suspend fun getAuthCode(): Pair<String, String> {
        val ts = Instant.now().epochSecond
        val params = "appkey=$AppKey&local_id=0&ts=$ts"
        val signString = sign(params)
        val data = BilibiliHttpClient.loginService.getAuthCode(
            ts = ts,
            sign = signString
        ).data
        return data.authCode to data.url
    }

    suspend fun loginByQrCode(
        authCode: String
    ) {
        val ts = Instant.now().epochSecond
        val params = "appkey=$AppKey&auth_code=$authCode&local_id=0&ts=$ts"
        val signString = sign(params)
        val cookieData = BilibiliHttpClient.loginService.loginByQrCode(
            authCode = authCode,
            ts = ts,
            sign = signString
        ).data
        Utils.saveCookieToFile(cookieData)
    }

    suspend fun preupload(
        size: Long,
        name: String,
        cookie: CookieData,
    ): BiliProfileResponse {
        return BilibiliHttpClient.uploadService(BiliEndPoint.MEMBER).preupload(
            size = size,
            name = name,
            cookie = cookie.toCookieString()
        )
    }

    suspend fun getUploadId(
        auth: String,
        path: String
    ): BiliUploadIdResponse {
        return BilibiliHttpClient.uploadService(BiliEndPoint.UPLOAD).getUploadId(
            auth = auth,
            path = path
        )
    }

    suspend fun endUpload(
        task: Task
    ) {
       BilibiliHttpClient.uploadService(BiliEndPoint.UPLOAD).endUpload(
            auth = task.auth,
            path = task.path,
            name = task.fileName,
            uploadId = task.uploadId,
            bizId = task.bizId,
            request = VideoInfoRequest(
                parts = task.partNumbers.map { partNumber ->
                    BiliPart(partNumber)
                }
            )
        )
    }

    suspend fun addVideo(
        videos: List<VideoInfo>,
        cookie: CookieData,
        template: Template,
        date: Date,
    ): BiliAddResponse {
        val ts = Instant.now().epochSecond
        val accessKey = cookie.tokenInfo.accessToken
        val params = "access_key=$accessKey&appkey=$AppKey&build=7800300&c_locale=zh-Hans_CN&channel=bili&disable_rcmd=0&mobi_app=android&platform=android&s_locale=zh-Hans_CN&statistics={\"appId\":1,\"platform\":3,\"version\":\"7.80.0\",\"abtest\":\"\"}&ts=$ts"
        val signString = sign(params)
        return BilibiliHttpClient.uploadService(BiliEndPoint.MEMBER).addVideo(
            accessKey = accessKey,
            sign = signString,
            request = AddVideoRequest(
                source = template.source,
                tag = template.tag,
                title = template.displayTitle(date),
                videos = videos,
                isOnlySelf = template.isOnlySelf,
                tid = template.tid,
                watermark = if (template.copyright == 2) null else AddVideoRequest.Watermark(
                    state = template.watermark
                ),
                desc = template.desc,
                copyright = template.copyright,
//                recreate = template.recreate,
            )
        )
    }

    suspend fun uploadChunkWithKtor(
        index: Int,
        start: Long,
        end: Long,
        task: Task,
    ): Result<BiliPart> {
        val chunkSize = end - start

        val limitedStream = withContext(Dispatchers.IO) {
            val inputStream = task.file.inputStream()
            inputStream.skip(start)
            object : InputStream() {
                private var remaining = chunkSize

                override fun read(): Int {
                    if (remaining <= 0) return -1
                    val byte = inputStream.read()
                    if (byte >= 0) remaining--
                    return byte
                }

                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    if (remaining <= 0) return -1
                    val bytesToRead = minOf(len.toLong(), remaining).toInt()
                    val read = inputStream.read(b, off, bytesToRead)
                    if (read > 0) remaining -= read
                    return read
                }

                override fun close() {
                    inputStream.close()
                }
            }
        }

        val result = retry(10) {
            val response: HttpResponse = BilibiliHttpClient.client.put("${BiliEndPoint.UPLOAD.url}${task.path}") {
                header("X-Upos-Auth", task.auth)
                header("User-Agent", UserAgent)
                header("Content-Length", chunkSize)
                parameter("uploadId", task.uploadId)
                parameter("chunks", task.chunksNum)
                parameter("total", task.totalSize)
                parameter("chunk", index)
                parameter("size", chunkSize)
                parameter("partNumber", index + 1)
                parameter("start", start)
                parameter("end", end)

                setBody(limitedStream)
                contentType(ContentType.Application.OctetStream)
            }

            if (response.status.isSuccess()) {
                Result.success(BiliPart(index + 1))
            } else {
                Result.failure(Exception("Upload failed for part ${index + 1}: ${response.status}"))
            }
        }

        limitedStream.close()
        return result
    }

    suspend fun pingPreUpload(): PingGetResponse {
        return BilibiliHttpClient.pingService(BiliEndPoint.MEMBER.url).pingPreload()
    }

    suspend fun pingUpload(upCdn: String, totalSize: Long) {
        BilibiliHttpClient.pingService("https://upos-cs-upcdn$upCdn.bilivideo.com").pingUpload(
            request = RequestBody.create(
                "text/plain".toMediaType(),
                "0".repeat(totalSize.toInt())
            )
        )
    }
}

private fun sign(param: String, appSec: String = AppSecret): String {
    val input = param + appSec
    val md5Digest = MessageDigest.getInstance("MD5").digest(input.toByteArray(StandardCharsets.UTF_8))
    return md5Digest.joinToString("") { "%02x".format(it) }
}