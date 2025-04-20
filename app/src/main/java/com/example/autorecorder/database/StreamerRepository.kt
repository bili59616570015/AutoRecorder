package com.example.autorecorder.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.example.autorecorder.AutoRecorderApp
import com.example.autorecorder.entity.Streamer
import com.example.autorecorder.entity.StreamerEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StreamerRepository {
    private val itemDao: StreamerDao = DatabaseProvider.getDatabase(AutoRecorderApp.appContext).streamerDao()

    suspend fun upsertItem(item: Streamer) = withContext(Dispatchers.IO) {
        itemDao.upsert(item.entity)
    }

    suspend fun deleteItem(item: Streamer) = withContext(Dispatchers.IO) {
        itemDao.delete(item.entity)
    }

    suspend fun getAllItems(): List<Streamer> = withContext(Dispatchers.IO) {
        itemDao.getAllItems().map { it.item }.reversed()
    }

    suspend fun getItem(streamer: String): Streamer? = withContext(Dispatchers.IO) {
        itemDao.getItem(streamer).firstOrNull()?.item
    }
}

@Dao
interface StreamerDao {
    @Upsert
    suspend fun upsert(item: StreamerEntity)

    @Delete
    suspend fun delete(item: StreamerEntity)

    @Query("SELECT * FROM streamers")
    suspend fun getAllItems(): List<StreamerEntity>

    @Query("SELECT * FROM streamers WHERE name = :streamer")
    suspend fun getItem(streamer: String): List<StreamerEntity>
}
