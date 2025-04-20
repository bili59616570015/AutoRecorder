package com.example.autorecorder.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.autorecorder.common.Utils
import java.io.File
import java.io.Serializable

@kotlinx.serialization.Serializable
data class Task(
    val planId: String,
    val uploadId: String,
    val fileName: String,
    val path: String,
    val auth: String,
    val bizId: Long,
    val chunkSize: Long,
    val partNumbers: List<Int> = emptyList(),
    val template: String,
    val chunksNum: Int,
    val totalSize: Long,
): Serializable {
    val entity: TaskEntity
        get() = TaskEntity(
            planId = planId,
            uploadId = uploadId,
            file = fileName,
            path = path,
            auth = auth,
            bizId = bizId,
            chunkSize = chunkSize,
            partNumbers = partNumbers,
            template = template,
            chunksNum = chunksNum,
            totalSize = totalSize,
        )

    val file: File
        get() = Utils.getRecordFiles(listOf(fileName)).first()
}

@Entity(
    tableName = "Tasks",
)
data class TaskEntity(
    @PrimaryKey val uploadId: String,
    @ColumnInfo(name = "planId") val planId: String,
    @ColumnInfo(name = "file") val file: String,
    @ColumnInfo(name = "path") val path: String,
    @ColumnInfo(name = "auth") val auth: String,
    @ColumnInfo(name = "bizId") val bizId: Long,
    @ColumnInfo(name = "chunkSize") val chunkSize: Long,
    @ColumnInfo(name = "partNumbers") val partNumbers: List<Int>,
    @ColumnInfo(name = "template") val template: String,
    @ColumnInfo(name = "chunksNum") val chunksNum: Int,
    @ColumnInfo(name = "totalSize") val totalSize: Long,
) {
    val item: Task
        get() = Task(
            planId = planId,
            uploadId = uploadId,
            fileName = file,
            path = path,
            auth = auth,
            bizId = bizId,
            chunkSize = chunkSize,
            partNumbers = partNumbers,
            template = template,
            chunksNum = chunksNum,
            totalSize = totalSize,
        )
}