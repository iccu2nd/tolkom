package com.tinta.reader.data.mapper

import com.tinta.reader.data.remote.dto.ChapterItemDto
import com.tinta.reader.data.remote.dto.ComicDto
import com.tinta.reader.data.remote.dto.GenreDto
import com.tinta.reader.data.remote.dto.PagedComicsDto
import com.tinta.reader.data.remote.dto.ReaderDto
import com.tinta.reader.domain.model.ChapterRef
import com.tinta.reader.domain.model.Comic
import com.tinta.reader.domain.model.Genre
import com.tinta.reader.domain.model.Page
import com.tinta.reader.domain.model.ReaderChapter
import com.tinta.reader.domain.model.ReaderPage
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull

private fun JsonElement?.text(): String? =
    (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull?.takeIf { it.isNotBlank() }

private fun JsonElement?.number(): Double? = (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.doubleOrNull

private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

/** Mengembalikan null bila item tidak punya id yang bisa dipakai untuk navigasi. */
fun ComicDto.toDomain(): Comic? {
    val src = source.clean() ?: return null
    val sid = sourceId.clean() ?: slug.clean() ?: return null
    val rawRating = rating?.value.number()
    return Comic(
        source = src,
        sourceId = sid,
        title = title.clean() ?: sid.replace('-', ' '),
        alternativeTitles = alternativeTitles.mapNotNull { it.clean() },
        type = type.clean(),
        status = status.clean(),
        genres = genres.mapNotNull { it.clean() },
        author = author.clean(),
        artist = artist.clean(),
        cover = cover.clean() ?: rawCover.clean(),
        synopsis = synopsis.clean(),
        latestChapter = latestChapter.clean(),
        // Rating 0 dari sumber berarti "tidak ada", jangan ditampilkan sebagai nilai asli.
        rating = rawRating?.takeIf { it > 0.0 },
        updatedAt = updatedAt.text(),
    )
}

fun List<ComicDto>.toDomainList(): List<Comic> =
    mapNotNull { it.toDomain() }.distinctBy { it.key }

fun PagedComicsDto.toPage(): Page<Comic> = Page(items.toDomainList(), page, hasNextPage)

fun ChapterItemDto.toDomain(): ChapterRef? {
    val number = chapter.clean() ?: return null
    return ChapterRef(
        // Untuk Shinigami `chapter` sudah berisi kunci chapter (UUID) yang diterima endpoint baca.
        key = number,
        number = number,
        title = title.clean() ?: "Bab $number",
        releaseDate = releaseDate.text(),
    )
}

fun GenreDto.toDomain(): Genre? {
    val slug = slug.clean() ?: return null
    return Genre(name = name.clean() ?: slug, slug = slug, category = category.clean())
}

fun ReaderDto.toDomain(requestedKey: String): ReaderChapter? {
    val ch = chapter ?: return null
    val paragraphs = ch.novelContent?.paragraphs.orEmpty().filter { it.isNotBlank() }
    val pages = ch.images.mapIndexedNotNull { i, img ->
        img.image.clean()?.let { ReaderPage(img.index ?: i, it) }
    }.sortedBy { it.index }
    val number = ch.number.clean() ?: requestedKey
    return ReaderChapter(
        comic = comic?.toDomain(),
        key = requestedKey,
        number = number,
        title = ch.title.clean() ?: "Bab $number",
        isNovel = paragraphs.isNotEmpty() || ch.type == "novel",
        pages = pages,
        paragraphs = paragraphs,
        prevKey = ch.prevChapter.clean(),
        nextKey = ch.nextChapter.clean(),
    )
}
