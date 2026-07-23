package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE verseKey = :verseKey")
    suspend fun deleteBookmark(verseKey: String)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE verseKey = :verseKey)")
    fun isBookmarked(verseKey: String): Flow<Boolean>

    @Query("SELECT * FROM recent_position WHERE id = 1")
    fun getRecentPosition(): Flow<RecentPositionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRecentPosition(recent: RecentPositionEntity)
}

@Database(entities = [BookmarkEntity::class, RecentPositionEntity::class], version = 1, exportSchema = false)
abstract class GitaDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        @Volatile
        private var INSTANCE: GitaDatabase? = null

        fun getDatabase(context: Context): GitaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GitaDatabase::class.java,
                    "gita_saathi_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
