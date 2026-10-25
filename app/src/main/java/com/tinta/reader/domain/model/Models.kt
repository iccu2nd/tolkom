package com.tinta.reader.domain.model

data class Comic(
    val source: String,
    val sourceId: String,
    val title: String,
    val alternativeTitles: List<String>,
    val type: String?,
    val status: String?,
    val genres: List<String>,
    val author: String?,
    val artist: String?,
    val cover: String?,
    val synopsis: String?,
    val latestChapter: String?,
    val rating: Double?,
    val updatedAt: String?,
) {
    /** Kunci stabil untuk list, database, dan navigasi. */
    val key: String get() = "$source:$sourceId"
}

data class ChapterRef(
    val key: String,        // nilai yang dikirim ke endpoint /chapter/:chapter
    val number: String,
    val title: String,
    val releaseDate: String?,
)

data class ComicDetail(val comic: Comic, val chapters: List<ChapterRef>) // terbaru dulu

data class Genre(val name: String, val slug: String, val category: String?)

data class Page<T>(val items: List<T>, val page: Int, val hasNext: Boolean)

data class HomeFeed(val popular: List<Comic>, val latest: List<Comic>, val fresh: List<Comic>)

data class ReaderPage(val index: Int, val url: String)

data class ReaderChapter(
    val comic: Comic?,
    val key: String,
    val number: String,
    val title: String,
    val isNovel: Boolean,
    val pages: List<ReaderPage>,
    val paragraphs: List<String>,
    val prevKey: String?,
    val nextKey: String?,
)

data class Bookmark(val comic: Comic?, val source: String, val sourceId: String, val title: String, val cover: String?)

data class HistoryItem(
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
