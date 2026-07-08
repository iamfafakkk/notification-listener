package com.karuhundeveloper.notification.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.karuhundeveloper.notification.data.local.converter.Converters
import com.karuhundeveloper.notification.data.local.dao.InterceptRuleDao
import com.karuhundeveloper.notification.data.local.entity.InterceptRule

/**
 * The single Room database for the NotificationInterceptor app.
 *
 * ### Schema migrations
 * Increment [version] whenever you change the schema and provide a [Migration] object in
 * [Room.databaseBuilder]. During active development you can use
 * [Room.databaseBuilder(...).fallbackToDestructiveMigration()] instead, but **never** do
 * this in a production release.
 *
 * ### Usage
 * Obtain the singleton via [AppDatabase.getInstance] and call the relevant DAO method.
 * Always perform database operations off the main thread (e.g. inside a coroutine or
 * via a Repository that injects a [CoroutineDispatcher]).
 */
@Database(
    entities = [InterceptRule::class],
    version = 1,
    exportSchema = true          // keeps a JSON schema history in app/schemas/ for migration audits
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun interceptRuleDao(): InterceptRuleDao

    companion object {
        private const val DATABASE_NAME = "notification_interceptor.db"

        // Volatile ensures that the INSTANCE write is immediately visible to all threads.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the existing singleton or builds a new one using double-checked locking.
         *
         * @param context Use the application context to avoid memory leaks.
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                // Add Migration objects here as the schema evolves, e.g.:
                // .addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}
