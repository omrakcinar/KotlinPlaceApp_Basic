package com.omerakcinar.kotlinplacesapp_basic.roomdb

import androidx.room.Database
import androidx.room.RoomDatabase
import com.omerakcinar.kotlinplacesapp_basic.model.Place

@Database(entities = [Place::class], version = 1)
abstract class PlaceDb : RoomDatabase() {
    abstract fun placeDao() : PlaceDao
}