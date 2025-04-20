package com.example.autorecorder.api.bili

import com.example.autorecorder.common.SharedPreferencesHelper
import com.example.autorecorder.common.UpCdn
import com.example.autorecorder.common.Utils
import com.example.autorecorder.database.PlanRepository
import com.example.autorecorder.database.TaskRepository
import com.example.autorecorder.database.TemplateRepository
import com.example.autorecorder.entity.BiliPart
import com.example.autorecorder.entity.CookieData
import com.example.autorecorder.entity.Plan
import com.example.autorecorder.entity.PlanStatus
import com.example.autorecorder.entity.Task
import com.example.autorecorder.entity.Template
import com.example.autorecorder.entity.VideoInfo
import com.example.autorecorder.screen.upload.UploadViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.Date
import kotlin.math.ceil

class BilibiliUseCase {
    private val repository = BilibiliRepository()
    private val taskRepository = TaskRepository()
    private val planRepository = PlanRepository()
    private val templateRepository = TemplateRepository()

    suspend fun upload(
        item: Plan
    ) {
        var plan = item.copy(errorMessage = "")
        try {
            val template = templateRepository.getItem(plan.templateTitle) ?: throw Exception("Template not found")
            val cookie = getValidCookie(template.mid)
            if (plan.status < PlanStatus.PRELOADED) {
                taskRepository.deleteItem(listOf(plan.id))
                preload(plan, cookie, template)
                plan = upsert(plan.copy(status = PlanStatus.PRELOADED))
                UploadViewModel.updatePlans(plan)
            }
            if (plan.status < PlanStatus.UPLOADED) {
                uploadStream(plan)
                plan = upsert(plan.copy(status = PlanStatus.UPLOADED))
                UploadViewModel.updatePlans(plan)
            }
            if (plan.status < PlanStatus.POSTED) {
                val tasks = taskRepository.getItems(listOf(plan.id))
                val list = tasks.map { task ->
                    VideoInfo(
                        title = task.fileName.substringBeforeLast("."),
                        filename = task.path.substringAfter("/").substringBeforeLast(".")
                    )
                }
                val date = Utils.getFileCreationDate(tasks.first().file) ?: Date()
                val addResponse = repository.addVideo(
                    videos = list,
                    cookie = cookie,
                    template = template,
                    date = date
                )
                plan = upsert(
                    plan.copy(
                        status = PlanStatus.POSTED,
                        bvid = addResponse.data.bvid
                    )
                )
                UploadViewModel.updatePlans(plan)
            }
            try {
                val tasks = taskRepository.getItems(listOf(plan.id))
                val needBackup = SharedPreferencesHelper.needBackupVideo
                tasks.forEach { task ->
                    if (needBackup) {
                        Utils.moveToBackup(task.file)
                    } else {
                        Utils.deleteFile(task.file)
                    }
                }
                Utils.scanFolder()
            } catch (_: Exception) {
            }
        } catch (e: Exception) {
            plan = upsert(
                plan.copy(
                    errorMessage = e.localizedMessage ?: "Unknown error"
                )
            )
            UploadViewModel.updatePlans(plan)
        }
    }

    private suspend fun preload(
        plan: Plan,
        cookie: CookieData,
        template: Template,
    ): List<Task> {
        val cdn = try { ping()?.name?.lowercase() } catch (e: Exception) { null } ?: UpCdn.BDA2.name.lowercase()
        SharedPreferencesHelper.upCdn = cdn
        return plan.files.map { file ->
            val profile = repository.preupload(
                size = file.length(),
                name = file.name,
                cookie = cookie
            )
            val path = profile.uposUri.removePrefix("upos://")
            val response = repository.getUploadId(
                auth = profile.auth,
                path = path
            )
            val task = Task(
                planId = plan.id,
                uploadId = response.uploadId,
                fileName = file.name,
                path = path,
                auth = profile.auth,
                bizId = profile.bizId,
                chunkSize = profile.chunkSize,
                partNumbers = emptyList(),
                template = template.title,
                chunksNum = ceil(file.length().toDouble() / profile.chunkSize.toDouble()).toInt(),
                totalSize = file.length()
            )
            taskRepository.upsertItem(task)
            UploadViewModel.updateTasks(task)
            task
        }
    }

    private suspend fun uploadStream(plan: Plan): List<VideoInfo> = withContext(Dispatchers.IO) {
        val tasks = taskRepository.getItems(listOf(plan.id))
        tasks.map { item ->
            val partResponse = tryUploadStream(item)
            repository.endUpload(
                item.copy(
                    partNumbers = partResponse.mapNotNull {
                        it.getOrNull()?.partNumber
                    }
                )
            )
            VideoInfo(
                title = item.fileName.substringBeforeLast("."),
                filename = item.path.substringAfter("/").substringBeforeLast(".")
            )
        }
    }

    private suspend fun tryUploadStream(
        task: Task,
        limit: Int = 3,
    ): List<Result<BiliPart>> = withContext(Dispatchers.IO) {
        val totalSize = task.totalSize
        val semaphore = Semaphore(limit)
        val results = mutableListOf<Result<BiliPart>>()
        task.partNumbers.forEach {
            results.add(Result.success(BiliPart(it)))
        }
        val mutex = Mutex()
        coroutineScope {
            val deferredResults = mutableListOf<Deferred<Boolean>>()
            for (index in 0 until task.chunksNum) {
                semaphore.acquire()
                val currentIndex = index
                val start = index * task.chunkSize
                val end = minOf(start + task.chunkSize, totalSize)
                println("Processing part ${currentIndex + 1} of ${task.chunksNum} ($start-$end) / $totalSize")
                val deferred = async {
                    if (task.partNumbers.contains(currentIndex + 1)) {
                        println("Skipping part ${currentIndex + 1}")
                        semaphore.release()
                        return@async true
                    }
                    try {
                        val result = repository.uploadChunkWithKtor(
                            index = currentIndex,
                            start = start,
                            end = end,
                            task = task
                        )
                        mutex.withLock {
                            results.add(result)
                        }
                        val newTask = task.copy(
                            partNumbers = results.mapNotNull { it.getOrNull()?.partNumber }
                        )
                        taskRepository.upsertItem(newTask)
                        UploadViewModel.updateTasks(newTask)
                        true
                    } catch (e: Exception) {
                        println(e.localizedMessage)
                        false
                    } finally {
                        semaphore.release()
                    }
                }
                deferredResults.add(deferred)
            }
            deferredResults.awaitAll()
        }
        results
    }

    private suspend fun upsert(item: Plan): Plan {
        planRepository.upsertItem(item)
        return item
    }

    private suspend fun getValidCookie(mid: Long): CookieData {
        val cookie = Utils.readCookieFromFile("$mid.json") ?: throw Exception("Cookie not found")
        if ((cookie.cookieInfo.cookies.firstOrNull()?.expires ?: 0) < Instant.now().epochSecond) {
            BilibiliRepository().refreshToken(cookie)
        }
        return Utils.readCookieFromFile("$mid.json") ?: throw Exception("Cookie not found")
    }

    suspend fun ping(): UpCdn? {
        val pingPreUpload = repository.pingPreUpload()
        val cdnList = pingPreUpload.lines.mapNotNull { line ->
            val cdn = line.probeUrl.substringBefore(".").removePrefix("//upos-cs-upcdn")
            val item = UpCdn.entries.firstOrNull { it.name.lowercase() == cdn }
            if (item == UpCdn.TXA) UpCdn.QN else item
        }
        val list = cdnList.map {
            val time = Date().time
            repository.pingUpload(
                upCdn = it.name.lowercase(),
                totalSize = 1024_00L
            )
            val pingTime = Date().time - time
            it to pingTime
        }
        return list.minByOrNull { it.second }?.first
    }
}