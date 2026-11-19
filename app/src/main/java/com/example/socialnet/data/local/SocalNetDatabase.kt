package com.example.socialnet.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.socialnet.data.local.converter.Converters
import com.example.socialnet.data.local.dao.PostDao
import com.example.socialnet.data.local.dao.SubscriptionDao
import com.example.socialnet.data.local.dao.UserDao
import com.example.socialnet.data.local.entity.PostEntity
import com.example.socialnet.data.local.entity.SubscriptionEntity
import com.example.socialnet.data.local.entity.UserEntity

@Database(
    entities = [
        PostEntity::class,
        UserEntity::class,
        SubscriptionEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SocialNetDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun userDao(): UserDao
    abstract fun subscriptionDao(): SubscriptionDao
}