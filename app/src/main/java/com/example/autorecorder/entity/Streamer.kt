package com.example.autorecorder.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.io.Serializable


@kotlinx.serialization.Serializable
data class Streamer(
    val name: String,
    val url: String,
    val templateString: String? = null,
): Serializable {
    val webRid: String
        get() {
            val regex = """https://live.douyin.com/(\d+)"""
            val matchResult = Regex(regex).find(url)
            return matchResult?.groups?.get(1)?.value ?: ""
        }

    val entity: StreamerEntity
        get() = StreamerEntity(url = url, name = name, templateString = templateString)

    companion object {
        fun new() = Streamer(name = "", url = "https://live.douyin.com/")
    }
}

@Entity(
    tableName = "streamers",
    indices = [Index(value = ["name"], unique = true)]
)
data class StreamerEntity(
    @PrimaryKey val url: String,  // URL as primary key
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "template") val templateString: String?,
) {
    val item: Streamer
        get() = Streamer(name = name, url = url, templateString = templateString)
}
