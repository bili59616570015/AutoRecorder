package com.example.autorecorder.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import com.example.autorecorder.entity.PlanEntity
import com.example.autorecorder.entity.StreamerEntity
import com.example.autorecorder.entity.TaskEntity
import com.example.autorecorder.entity.TemplateEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Database(
    entities = [
        StreamerEntity::class,
        TemplateEntity::class,
        TaskEntity::class,
        PlanEntity::class
    ],
    version = 2,
    autoMigrations = [
         AutoMigration(from = 1, to = 2)
                     ],
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun streamerDao(): StreamerDao
    abstract fun templateDao(): TemplateDao
    abstract fun taskDao(): TaskDao
    abstract fun planDao(): PlanDao
}

object DatabaseProvider {
    private var appDatabase: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        if (appDatabase == null) {
            appDatabase = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "app_database"
            )
                .fallbackToDestructiveMigration()
                .addMigrations(Migration(1, 2) {})
            .build()
        }
        return appDatabase!!
    }
}

class Converters {
    @TypeConverter
    fun fromIntList(value: List<Int>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toIntList(value: String): List<Int> {
        val listType = object : TypeToken<List<Int>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }
}