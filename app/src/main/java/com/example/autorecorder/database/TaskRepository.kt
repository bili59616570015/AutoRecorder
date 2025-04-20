package com.example.autorecorder.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.example.autorecorder.AutoRecorderApp
import com.example.autorecorder.entity.Task
import com.example.autorecorder.entity.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TaskRepository {
    private val itemDao: TaskDao = DatabaseProvider.getDatabase(AutoRecorderApp.appContext).taskDao()

    suspend fun upsertItem(item: Task) = withContext(Dispatchers.IO) {
        itemDao.upsert(item.entity)
    }

    suspend fun deleteItem(item: Task) = withContext(Dispatchers.IO) {
        itemDao.delete(item.entity)
    }

    suspend fun deleteItem(planIds: List<String>) = withContext(Dispatchers.IO) {
        itemDao.deleteByPlanIds(planIds)
    }

    suspend fun getAllItems(): List<Task> = withContext(Dispatchers.IO) {
        itemDao.getAllItems().map { it.item }.reversed()
    }

    suspend fun getItems(planIds: List<String>): List<Task> = withContext(Dispatchers.IO) {
        itemDao.getItems(planIds).map { it.item }
    }
}

@Dao
interface TaskDao {
    @Upsert
    suspend fun upsert(item: TaskEntity)

    @Delete
    suspend fun delete(item: TaskEntity)

    @Query("DELETE FROM tasks WHERE planId IN (:planIds)")
    suspend fun deleteByPlanIds(planIds: List<String>)

    @Query("SELECT * FROM tasks")
    suspend fun getAllItems(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE planId IN (:planIds)")
    suspend fun getItems(planIds: List<String>): List<TaskEntity>
}