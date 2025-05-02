package com.example.autorecorder.common

import android.content.Context
import android.content.SharedPreferences
import android.util.DisplayMetrics
import android.view.WindowManager
import com.example.autorecorder.AutoRecorderApp
import java.util.Date

object SharedPreferencesHelper {
    private val sharedPreferences: SharedPreferences =
        AutoRecorderApp.appContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

    var startCommand: String
        get() = sharedPreferences.getString("startCommand", "") ?: ""
        set(value) = sharedPreferences.edit().putString("startCommand", value).apply()

    var endCommand: String
        get() = sharedPreferences.getString("endCommand", "") ?: ""
        set(value) = sharedPreferences.edit().putString("endCommand", value).apply()

    var adbPort: Int
        get() = sharedPreferences.getInt("adbPort", 5555)
        set(value) = sharedPreferences.edit().putInt("adbPort", value).apply()

    var streamer: String
        get() = sharedPreferences.getString("streamer", "") ?: ""
        set(value) = sharedPreferences.edit().putString("streamer", value).apply()

    var upCdn: String
        get() = sharedPreferences.getString("upCdn", UpCdn.BDA2.name.lowercase()) ?: UpCdn.BDA2.name.lowercase()
        set(value) = sharedPreferences.edit().putString("upCdn", value).apply()

    var latestRecorded: Date
        get() {
            val timestamp = sharedPreferences.getLong("latestRecorded", 0L)
            return Date(timestamp)
        }
        set(value) = sharedPreferences.edit().putLong("latestRecorded", value.time).apply()

    var quality: Quality
        get() {
            val px = sharedPreferences.getInt("quality", Quality.FHD.width)
            return Quality.from(px)
        }
        set(value) = sharedPreferences.edit().putInt("quality", value.width).apply()

    var bitrate: Int
        get() = sharedPreferences.getInt("bitrate", 8)
        set(value) = sharedPreferences.edit().putInt("bitrate", value).apply()

    val bitrateList: List<Int>
        get() {
            return listOf(1, 2, 3, 4, 6, 8, 10 ,12, 16).reversed()
        }
    var frameRate: Int
        get() = sharedPreferences.getInt("frameRate", 30)
        set(value) = sharedPreferences.edit().putInt("frameRate", value).apply()

    val frameRateList: List<Int>
        get() {
            return listOf(15, 20, 25, 30, 60).reversed()
        }

    var needBackupVideo: Boolean
        get() = sharedPreferences.getBoolean("needBackupVideo", true)
        set(value) = sharedPreferences.edit().putBoolean("needBackupVideo", value).apply()

    var advancedAdb: Boolean
        get() = sharedPreferences.getBoolean("advancedAdb", false)
        set(value) = sharedPreferences.edit().putBoolean("advancedAdb", value).apply()

    var splitFile: Boolean
        get() = sharedPreferences.getBoolean("splitFile", true)
        set(value) = sharedPreferences.edit().putBoolean("splitFile", value).apply()
}

fun String.adbCommands(): List<String> {
    return this
        .trimIndent()
        .lines()
        .map {
            val regex = """adb shell "?([^"]+)"?""".toRegex()
            regex.find(it)?.groupValues?.get(1)?.trim() ?: ""
        }
        .filter { it.isNotEmpty() }
}

enum class UpCdn(val title: String) {
    BDA2("百度"),
    WS("网宿"),
    QN("七牛"),
    BDA("百度云海外"),
    TX("腾讯云"),
    TXA("腾讯云海外"),
    BLDSA("B站自建"),
    ALIA("阿里云");
}

enum class Quality(val width: Int) {
    FHD(1080),
    HD(720),
    SD(360);

    val height: Int
        get() {
            val context = AutoRecorderApp.appContext
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val displayMetrics = DisplayMetrics().apply {
                windowManager.defaultDisplay.getRealMetrics(this)
            }
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            return (width.toFloat() / screenWidth.toFloat() * screenHeight.toFloat()).toInt()
        }

    companion object {
        fun from(width: Int): Quality {
            return entries.firstOrNull { it.width == width } ?: FHD
        }
    }
}