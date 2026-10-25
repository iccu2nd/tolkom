package com.tinta.reader.data.repository

import com.tinta.reader.data.local.BookmarkDao
import com.tinta.reader.data.local.BookmarkEntity
import com.tinta.reader.data.local.HistoryDao
import com.tinta.reader.data.local.HistoryEntity
import com.tinta.reader.data.local.RecentSearchDao
import com.tinta.reader.data.local.RecentSearchEntity
import com.tinta.reader.data.mapper.toDomain
import com.tinta.reader.data.mapper.toDomainList
import com.tinta.reader.data.mapper.toPage
import com.tinta.reader.data.remote.api.ManhwaApi
import com.tinta.reader.domain.model.Bookmark
import com.tinta.reader.domain.model.ChapterRef
import com.tinta.reader.domain.model.Comic
import com.tinta.reader.domain.model.ComicDetail
import com.tinta.reader.domain.model.Genre
import com.tinta.reader.domain.model.HistoryItem
import com.tinta.reader.domain.model.HomeFeed
import com.tinta.reader.domain.model.Page
import com.tinta.reader.domain.model.ReaderChapter
import com.tinta.reader.util.AppException
import com.tinta.reader.util.Strings
import com.tinta.reader.util.toAppException
import com.tinta.reader.util.unwrap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Menjalankan panggilan jaringan dan memetakan semua kegagalan ke AppException. */
private suspend inline fun <T> call(block: () -> T): T =
    try { block() } catch (t: Throwable) { throw t.toAppException() }

class CatalogRepository(private val api: ManhwaApi) {

    @Volatile var cachedHome: HomeFeed? = null
        private set
    @Volatile private var cachedGenres: List<Genre>? = null

    suspend fun home(refresh: Boolean = false): HomeFeed = call {
        val dto = api.home(refresh = refresh).unwrap()
        HomeFeed(
            popular = dto.popular.toDomainList(),
            latest = dto.latest.toDomainList(),
            fresh = dto.fresh.toDomainList(),
        ).also { cachedHome = it }
    }

    suspend fun latest(page: Int): Page<Comic> = call { api.list(page = page).unwrap().toPage() }

    suspend fun search(query: String, page: Int): Page<Comic> =
        call { api.search(query = query, page = page).unwrap().toPage() }

    suspend fun genres(): List<Genre> = call {
        cachedGenres ?: api.genres().unwrap().mapNotNull { it.toDomain() }.also { cachedGenres = it }
    }

    suspend fun byGenre(slug: String, page: Int): Page<Comic> =
        call { api.byGenre(genre = slug, page = page).unwrap().toPage() }

    suspend fun byType(type: String, page: Int): Page<Comic> =
        call { api.byType(type = type, page = page).unwrap().toPage() }

    suspend fun detail(source: String, id: String): ComicDetail = call {
        val dto = api.detail(source, id).unwrap()
        val comic = dto.comic?.toDomain() ?: throw AppException(AppException.Kind.Parse, Strings.errParse)
        ComicDetail(comic, dto.chapters.mapNotNull { it.toDomain() })
    }

    suspend fun chapter(source: String, id: String, chapterKey: String): ReaderChapter = call {
        api.chapter(source, id, chapterKey).unwrap().toDomain(chapterKey)
            ?: throw AppException(AppException.Kind.NotFound, Strings.errNotFound)
    }
}

class LibraryRepository(
    private val bookmarks: BookmarkDao,
    private val history: HistoryDao,
    private val searches: RecentSearchDao,
) {
    fun bookmarkList(): Flow<List<Bookmark>> = bookmarks.observeAll().map { list ->
        list.map { Bookmark(null, it.source, it.sourceId, it.title, it.cover) }
    }

    fun isBookmarked(source: String, sourceId: String): Flow<Boolean> =
        bookmarks.observeIsBookmarked("$source:$sourceId")

    suspend fun setBookmarked(comic: Comic, value: Boolean) {
        if (value) {
            bookmarks.upsert(
                BookmarkEntity(comic.key, comic.source, comic.sourceId, comic.title, comic.cover, System.currentTimeMillis())
            )
        } else {
            bookmarks.delete(comic.key)
        }
    }

    fun historyList(): Flow<List<HistoryItem>> = history.observeAll().map { list -> list.map { it.toDomain() } }

    fun historyFor(source: String, sourceId: String): Flow<HistoryItem?> =
        history.observe("$source:$sourceId").map { it?.toDomain() }

    suspend fun lastRead(source: String, sourceId: String): HistoryItem? =
        history.get("$source:$sourceId")?.toDomain()

    suspend fun saveProgress(item: HistoryItem) {
        history.upsert(
            HistoryEntity(
                comicKey = "${item.source}:${item.sourceId}",
                source = item.source,
                sourceId = item.sourceId,
                title = item.title,
                cover = item.cover,
                chapterKey = item.chapterKey,
                chapterTitle = item.chapterTitle,
                scrollIndex = item.scrollIndex,
                scrollOffset = item.scrollOffset,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun removeHistory(source: String, sourceId: String) = history.delete("$source:$sourceId")
    suspend fun clearHistory() = history.clear()

    fun recentSearches(): Flow<List<String>> = searches.observe().map { l -> l.map { it.query } }
    suspend fun addSearch(query: String) {
        val q = query.trim()
        if (q.isNotEmpty()) searches.upsert(RecentSearchEntity(q, System.currentTimeMillis()))
    }
    suspend fun removeSearch(query: String) = searches.delete(query)
    suspend fun clearSearches() = searches.clear()

    private fun HistoryEntity.toDomain() = HistoryItem(
        source, sourceId, title, cover, chapterKey, chapterTitle, scrollIndex, scrollOffset, updatedAt
    )
}
