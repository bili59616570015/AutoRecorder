package com.example.autorecorder.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.example.autorecorder.AutoRecorderApp
import com.example.autorecorder.entity.Template
import com.example.autorecorder.entity.TemplateEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TemplateRepository {
    private val itemDao: TemplateDao = DatabaseProvider.getDatabase(AutoRecorderApp.appContext).templateDao()

    suspend fun upsertItem(item: Template) = withContext(Dispatchers.IO) {
        itemDao.upsert(item.entity)
    }

    suspend fun deleteItem(item: Template) = withContext(Dispatchers.IO) {
        itemDao.delete(item.entity)
    }

    suspend fun deleteByMid(mid: Long) = withContext(Dispatchers.IO) {
        itemDao.deleteByMid(mid)
    }

    suspend fun getAllItems(): List<Template> = withContext(Dispatchers.IO) {
        itemDao.getAllItems().map { it.item }.reversed()
    }

    suspend fun getItem(title: String): Template? = withContext(Dispatchers.IO) {
        itemDao.getItem(title).firstOrNull()?.item
    }
}

@Dao
interface TemplateDao {
    @Upsert
    suspend fun upsert(item: TemplateEntity)

    @Delete
    suspend fun delete(item: TemplateEntity)

    @Query("DELETE FROM templates WHERE mid = :mid")
    suspend fun deleteByMid(mid: Long)

    @Query("SELECT * FROM templates")
    suspend fun getAllItems(): List<TemplateEntity>

    @Query("SELECT * FROM templates WHERE title = :title")
    suspend fun getItem(title: String): List<TemplateEntity>
}