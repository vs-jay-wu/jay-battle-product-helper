package com.viewsonic.classswift.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "table_test")
data class TestDbEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "name", defaultValue = "")
    val name: String,
    @ColumnInfo(name = "age", defaultValue = "1")
    val age: Int
)

