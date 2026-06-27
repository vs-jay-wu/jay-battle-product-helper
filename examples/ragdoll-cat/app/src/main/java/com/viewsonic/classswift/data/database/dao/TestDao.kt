package com.viewsonic.classswift.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.viewsonic.classswift.data.database.entity.TestDbEntity

@Dao
interface TestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TestDbEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<TestDbEntity>)

    @Query("SELECT * FROM table_test WHERE id = :id")
    suspend fun load(id: Long): TestDbEntity?

    @Query("SELECT * FROM table_test")
    suspend fun loadAll(): List<TestDbEntity>

    @Query("SELECT * FROM table_test WHERE id = (SELECT MAX(id) FROM table_test)")
    suspend fun loadLast(): TestDbEntity?

    @Delete
    suspend fun delete(entity: TestDbEntity): Int

    @Query("DELETE FROM table_test WHERE id = :id")
    suspend fun delete(id: Long): Int

    @Query("SELECT COUNT(id) FROM table_test")
    suspend fun count(): Int

    @Query("DELETE FROM table_test")
    suspend fun clearAll()
}