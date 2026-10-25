package com.tinta.reader.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Pembungkus semua respons JSON server: { success, data } atau { success:false, error }. */
@Serializable
data class EnvelopeDto<T>(
    val success: Boolean = false,
    val data: T? = null,
    val error: ErrorDto? = null,
)

@Serializable
data class ErrorDto(val code: String? = null, val message: String? = null)

@Serializable
data class RatingDto(
    // Tipe sumber tidak dijamin (angka / string / null), jadi dibaca sebagai JsonElement.
    val value: JsonElement? = null,
    val count: JsonElement? = null,
)

@Serializable
data class ComicDto(
    val id: String? = null,
    val source: String? = null,
    val sourceId: String? = null,
    val slug: String? = null,
    val title: String? = null,
    val alternativeTitles: List<String> = emptyList(),
    val type: String? = null,
    val status: String? = null,
    val genres: List<String> = emptyList(),
    val author: String? = null,
    val artist: String? = null,
    val cover: String? = null,
    val rawCover: String? = null,
    val synopsis: String? = null,
    val latestChapter: String? = null,
    val rating: RatingDto? = null,
    val updatedAt: JsonElement? = null,
)

@Serializable
data class HomeDto(
    val popular: List<ComicDto> = emptyList(),
    val latest: List<ComicDto> = emptyList(),
    @SerialName("new") val fresh: List<ComicDto> = emptyList(),
    val scrapedAt: String? = null,
)

/** Dipakai oleh /api/list, /api/search, /api/genre/:g, /api/type/:t. */
@Serializable
data class PagedComicsDto(
    val items: List<ComicDto> = emptyList(),
    val page: Int = 1,
    val hasNextPage: Boolean = false,
    val resolvedQuery: String? = null,
)

@Serializable
data class ChapterItemDto(
    val id: String? = null,
    val comicId: String? = null,
    val source: String? = null,
    val sourceId: String? = null,
    val chapter: String? = null,
    val title: String? = null,
    val url: String? = null,
    val releaseDate: JsonElement? = null,
)

@Serializable
data class DetailDto(
    val comic: ComicDto? = null,
    val chapters: List<ChapterItemDto> = emptyList(),
)

@Serializable
data class PageImageDto(
    val index: Int? = null,
    val image: String? = null,
    val rawUrl: String? = null,
)

@Serializable
data class NovelContentDto(
    val subtitle: String? = null,
    val authorNote: String? = null,
    val paragraphs: List<String> = emptyList(),
)

@Serializable
data class ChapterContentDto(
    val id: String? = null,
    val number: String? = null,
    val title: String? = null,
    val type: String? = null,
    val images: List<PageImageDto> = emptyList(),
    val novelContent: NovelContentDto? = null,
    val prevChapter: String? = null,
    val nextChapter: String? = null,
    val totalChapters: Int? = null,
)

@Serializable
data class ReaderDto(
    val comic: ComicDto? = null,
    val chapter: ChapterContentDto? = null,
    val chapters: List<ChapterItemDto> = emptyList(),
)

@Serializable
data class GenreDto(
    val name: String? = null,
    val slug: String? = null,
    val category: String? = null,
    val countHint: String? = null,
)
