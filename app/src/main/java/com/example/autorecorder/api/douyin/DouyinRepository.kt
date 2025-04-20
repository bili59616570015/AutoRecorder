package com.example.autorecorder.api.douyin

import com.example.autorecorder.entity.LiveRoom

class DouyinRepository {
    suspend fun getRoom(
        webRid: String,
    ): LiveRoom {
        return DouyinHttpClient.apiService.getRoom(
            webRid = webRid,
            cookie = "ttwid=${getTtwid()}"
        ).toEntity()
    }

    private suspend fun getTtwid(): String? {
        try {
            val response = DouyinHttpClient.apiService.getPage()
            if (response.isSuccessful) {
                val cookies = response.headers()["Set-Cookie"]
                return extractTtwidCookie(cookies)
            }
        } catch (e: Exception) {
            e.printStackTrace()  // Handle exceptions here
        }
        return null
    }

    // Utility function to extract ttwid from cookies
    private fun extractTtwidCookie(cookies: String?): String? {
        cookies?.split(";")?.forEach { cookie ->
            if (cookie.contains("ttwid")) {
                return cookie.split("=")[1]
            }
        }
        return null
    }
}