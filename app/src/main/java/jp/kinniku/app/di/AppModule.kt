package jp.kinniku.app.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jp.kinniku.app.data.alarm.AndroidTime
import jp.kinniku.app.data.db.AppDatabase
import jp.kinniku.app.domain.TimeProvider
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "kinniku.db").addCallback(AppDatabase.callback).build()
    @Provides fun time(value: AndroidTime): TimeProvider = value
}
