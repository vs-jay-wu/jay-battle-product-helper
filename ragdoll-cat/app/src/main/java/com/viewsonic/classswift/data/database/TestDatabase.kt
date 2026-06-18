package com.viewsonic.classswift.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.viewsonic.classswift.data.database.dao.TestDao
import com.viewsonic.classswift.data.database.entity.TestDbEntity

@Database(entities = [TestDbEntity::class], version = 1)
abstract class TestDatabase : RoomDatabase() {

    abstract fun testDao(): TestDao

    companion object {
        private const val TEST_DATABASE_NAME = "database_test"

        fun build(context: Context): TestDatabase =
            Room.databaseBuilder(context, TestDatabase::class.java, TEST_DATABASE_NAME)
                .build()
    }
}