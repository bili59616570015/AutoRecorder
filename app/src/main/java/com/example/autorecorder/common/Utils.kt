package com.example.autorecorder.common

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.os.Environment
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.autorecorder.AutoRecorderApp
import com.example.autorecorder.api.bili.fromJson
import com.example.autorecorder.api.bili.toJson
import com.example.autorecorder.entity.CookieData
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

const val FILES_FOLDER = "AutoRecorder_files"
const val BACKUP_FOLDER = "AutoRecorder_backup"
const val FOLDER = "AutoRecorder"

object Utils {
    fun saveCookieToFile(cookie: CookieData) {
        val fileName = "${cookie.tokenInfo.mid}.json"
        val file = File(AutoRecorderApp.appContext.filesDir, fileName)
        file.writeText(cookie.toJson())
    }

    fun readCookieFromFile(fileName: String): CookieData? {
        val file = File(AutoRecorderApp.appContext.filesDir, fileName)
        return if (file.exists()) {
            val string = file.readText()
            string.fromJson()
        } else {
            null
        }
    }

    fun getCookieJsonNames(): List<String> {
        val files = AutoRecorderApp.appContext.filesDir.listFiles() ?: return emptyList()
        return files.filter { it.isFile && it.extension == "json" }
            .map { it.name.substringBefore(".") }
    }

    fun deleteFile(file: File) {
        if (file.exists()) {
            file.delete()
        }
    }

    fun getFileCreationDate(file: File): Date? {
        return try {
            val path = Paths.get(file.absolutePath)
            val attr = Files.readAttributes(path, BasicFileAttributes::class.java)
            Date(attr.creationTime().toMillis()) // Convert to Date
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun initRecordFolder() {
        val moviesFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val appFilesDir = File(moviesFolder, FOLDER)
        if (!appFilesDir.exists()) {
            appFilesDir.mkdirs()
        }
        val filesDir = File(appFilesDir, FILES_FOLDER)
        if (!filesDir.exists()) {
            filesDir.mkdirs()
        }
        val backupDir = File(appFilesDir, BACKUP_FOLDER)
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        scanFolder()
    }

    fun scanFolder() {
        // trigger the system to index it:
        val videoFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        MediaScannerConnection.scanFile(
            AutoRecorderApp.appContext,
            arrayOf(videoFile.absolutePath),
            arrayOf("video/mp4") // or null
        ) { path, uri -> }
    }

    fun getRecordFiles(fileNames: List<String>): List<File> {
        val filesDir = getDir(FILES_FOLDER)
        return fileNames.map {
            File(filesDir, it)
        }
    }

    fun getRecorderFiles(): List<File> {
        val filesDir = getDir(FILES_FOLDER)
        val files = filesDir.listFiles() ?: return emptyList()
        return files.map { it }
    }

    fun getBackupFiles(): List<File> {
        val backupDir = getDir(BACKUP_FOLDER)
        val files = backupDir.listFiles() ?: return emptyList()
        return files.map { it }
    }

    fun getDir(folder: String): File {
        val moviesFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val appFilesDir = File(moviesFolder, FOLDER)
        val dir = File(appFilesDir, folder)

        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun moveToBackup(file: File) {
        kotlin.runCatching {
            val backupDir = getDir(BACKUP_FOLDER)
            val newFile = File(backupDir, file.name)
            file.renameTo(newFile)
        }
    }

    fun formatFileSize(sizeInBytes: Long): String {
        val kb = 1024
        val mb = kb * 1024
        val gb = mb * 1024

        return when {
            sizeInBytes >= gb -> String.format(Locale.US, "%.2f GB", sizeInBytes.toDouble() / gb)
            sizeInBytes >= mb -> String.format(Locale.US, "%.2f MB", sizeInBytes.toDouble() / mb)
            sizeInBytes >= kb -> String.format(Locale.US, "%.2f KB", sizeInBytes.toDouble() / kb)
            else -> "$sizeInBytes bytes"
        }
    }

    fun getVideoDurationFormatted(videoFile: File): String {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoFile.absolutePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L

            String.format(
                Locale.US,
                "%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(durationMs),
                TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60,
                TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        } finally {
            retriever.release()
        }
    }

    fun openMoviesFolder(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider", // Make sure this matches your FileProvider authority
            file
        )
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension) ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.setDataAndType(uri, mimeType)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "No file manager app found", Toast.LENGTH_SHORT).show()
        }
    }
}