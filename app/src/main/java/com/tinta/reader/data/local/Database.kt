package com.tinta.reader.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val comicKey: String,
    val source: String,
    val sourceId: String,
    val title: String,
    val cover: String?,
    val addedAt: Long,
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val comicKey: String,
    val source: String,
    val sourceId: String,
    val title: String,
    val cover: String?,
    val chapterKey: String,
    val chapterTitle: String,
    val scrollIndex: Int,
    val scrollOffset: Int,
    val updatedAt: Long,
)

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey val query: String,
    val searchedAt: Long,
)

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE comicKey = :key)")
    fun observeIsBookmarked(key: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE comicKey = :key")
    suspend fun delete(key: String)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE comicKey = :key LIMIT 1")
    fun observe(key: String): Flow<HistoryEntity?>

    @Query("SELECT * FROM history WHERE comicKey = :key LIMIT 1")
    suspend fun get(key: String): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun clear()

    @Query("DELETE FROM history WHERE comicKey = :key")
    suspend fun delete(key: String)
}

@Dao
interface RecentSearchDao {
    @Query("SELECT * FROM recent_searches ORDER BY searchedAt DESC LIMIT 10")
    fun observe(): Flow<List<RecentSearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RecentSearchEntity)

    @Query("DELETE FROM recent_searches WHERE `query` = :query")
    suspend fun delete(query: String)

    @Query("DELETE FROM recent_searches")
    suspend fun clear()
}

@Database(
    entities = [BookmarkEntity::class, HistoryEntity::class, RecentSearchEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class TintaDatabase : RoomDatabase() {
    abstract fun bookmarks(): BookmarkDao
    abstract fun history(): HistoryDao
    abstract fun recentSearches(): RecentSearchDao
}
