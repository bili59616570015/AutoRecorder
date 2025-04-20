package com.example.autorecorder.entity

import androidx.compose.ui.graphics.Color
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.autorecorder.common.SharedPreferencesHelper
import com.example.autorecorder.common.Utils
import java.io.File
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

@kotlinx.serialization.Serializable
data class Plan(
    val id: String = UUID.randomUUID().toString(),
    val fileNames: List<String>,
    val templateTitle: String,
    val status: PlanStatus = PlanStatus.PENDING,
    val progress: Double = 0.0,
    val title: String,
    val bvid: String = "",
    val errorMessage: String = "",
): Serializable {
    val entity: PlanEntity
        get() = PlanEntity(
            id = id,
            fileNames = fileNames,
            template = templateTitle,
            status = status.code,
            progress = progress,
            title = title,
            bvid = bvid,
            errorMessage = errorMessage,
        )
    val files: List<File>
        get() = Utils.getRecordFiles(fileNames)

    companion object {
        fun new(streamer: Streamer): Plan? {
            val templateTitle = streamer.templateString ?: return null
            val files = Utils.getRecorderFiles().filter {
                it.name.split("_").firstOrNull() == streamer.name &&
                        (Utils.getFileCreationDate(it) ?: Date()) >= SharedPreferencesHelper.latestRecorded
            }
            val date = files.firstOrNull()?.let { Utils.getFileCreationDate(it) } ?: return null
            return Plan(
                fileNames = files.map { it.name },
                templateTitle = templateTitle,
                title = displayTitle(templateTitle, date),
            )
        }

        private fun displayTitle(title: String, date: Date = Date()): String {
            val regex = "\\{(.*?)\\}".toRegex() // Capture text inside {}
            val formatPattern = regex.find(title)?.groupValues?.get(1) ?: return title
            val formatter = SimpleDateFormat(formatPattern, Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("Asia/Shanghai") // Set to Beijing Time (UTC+8)
            }
            return title.replace(regex, formatter.format(date))
        }
    }
}

enum class PlanStatus(val code: Int) {
    PENDING(0),
    PRELOADED(1),
    UPLOADED(2),
    POSTED(3);
    val color: Color
        get() = when (this) {
            PENDING -> Color.Gray
            PRELOADED -> Color.Gray
            UPLOADED -> Color.Gray
            POSTED -> Color.Green
        }
}

@Entity(
    tableName = "Plans"
)
data class PlanEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "fileNames") val fileNames: List<String>,
    @ColumnInfo(name = "template") val template: String,
    @ColumnInfo(name = "status") val status: Int,
    @ColumnInfo(name = "progress") val progress: Double,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "bvid") val bvid: String,
    @ColumnInfo(name = "errorMessage") val errorMessage: String,
) {
    val item: Plan
        get() = Plan(
            id = id,
            fileNames = fileNames,
            templateTitle = template,
            status = PlanStatus.entries.find { it.code == status } ?: PlanStatus.PENDING,
            progress = progress,
            title = title,
            bvid = bvid,
            errorMessage = errorMessage,
        )
}