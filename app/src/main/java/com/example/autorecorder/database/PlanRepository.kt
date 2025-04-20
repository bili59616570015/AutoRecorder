package com.example.autorecorder.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.example.autorecorder.AutoRecorderApp
import com.example.autorecorder.entity.Plan
import com.example.autorecorder.entity.PlanEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlanRepository {
    private val itemDao: PlanDao = DatabaseProvider.getDatabase(AutoRecorderApp.appContext).planDao()

    suspend fun upsertItem(item: Plan) = withContext(Dispatchers.IO) {
        itemDao.upsert(item.entity)
    }

    suspend fun deleteItem(item: Plan) = withContext(Dispatchers.IO) {
        itemDao.delete(item.entity)
    }

    suspend fun getAllItems(): List<Plan> = withContext(Dispatchers.IO) {
        itemDao.getAllItems().map { it.item }.reversed()
    }
}

@Dao
interface PlanDao {
    @Upsert
    suspend fun upsert(item: PlanEntity)

    @Delete
    suspend fun delete(item: PlanEntity)

    @Query("SELECT * FROM plans")
    suspend fun getAllItems(): List<PlanEntity>
}