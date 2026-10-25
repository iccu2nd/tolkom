package com.tinta.reader

import com.tinta.reader.data.mapper.toDomain
import com.tinta.reader.data.mapper.toDomainList
import com.tinta.reader.data.mapper.toPage
import com.tinta.reader.data.remote.dto.ComicDto
import com.tinta.reader.data.remote.dto.EnvelopeDto
import com.tinta.reader.data.remote.dto.HomeDto
import com.tinta.reader.data.remote.dto.PagedComicsDto
import com.tinta.reader.data.remote.dto.ReaderDto
import com.tinta.reader.util.AppException
import com.tinta.reader.util.unwrap
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Contoh JSON mengikuti bentuk respons yang dibuat server (decorate.js / comicService.js). */
class MapperTest {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    private val comicJson = """
        {"id":"komiku:solo-leveling","source":"komiku","sourceId":"solo-leveling","slug":"solo-leveling",
         "title":"Solo Leveling","alternativeTitles":[],"type":"manhwa","status":null,"genres":["Action"],
         "author":null,"artist":null,"cover":"https://x.test/api/image-proxy?url=a&source=komiku","rawCover":"https://a",
         "synopsis":null,"latestChapter":"200","rating":{"value":null,"count":null},"updatedAt":null}
    """.trimIndent()

    @Test fun parsesComicAndKeepsNullsAsNull() {
        val c = json.decodeFromString<ComicDto>(comicJson).toDomain()!!
        assertEquals("komiku:solo-leveling", c.key)
        assertEquals("Solo Leveling", c.title)
        assertNull(c.rating)          // tidak ada rating palsu
        assertNull(c.author)
        assertNull(c.synopsis)
        assertEquals(listOf("Action"), c.genres)
        assertEquals("200", c.latestChapter)
    }

    @Test fun ratingAcceptsNumberOrQuotedStringAndDropsZero() {
        fun rating(raw: String) = json.decodeFromString<ComicDto>(
            """{"source":"x","sourceId":"y","title":"t","rating":{"value":$raw,"count":3}}"""
        ).toDomain()!!.rating
        assertEquals(8.5, rating("8.5")!!, 0.0001)
        assertEquals(7.0, rating("\"7\"")!!, 0.0001)
        assertNull(rating("0"))
        assertNull(rating("null"))
    }

    @Test fun updatedAtAcceptsNumberAndString() {
        val a = json.decodeFromString<ComicDto>("""{"source":"x","sourceId":"y","updatedAt":1700000000}""").toDomain()!!
        val b = json.decodeFromString<ComicDto>("""{"source":"x","sourceId":"y","updatedAt":"2025-01-02T00:00:00Z"}""").toDomain()!!
        assertEquals("1700000000", a.updatedAt)
        assertEquals("2025-01-02T00:00:00Z", b.updatedAt)
    }

    @Test fun itemsWithoutUsableIdAreDropped() {
        val list = listOf(
            ComicDto(source = "komiku", sourceId = "a", title = "A"),
            ComicDto(source = "komiku", sourceId = "a", title = "A duplicate"),
            ComicDto(source = null, sourceId = "b", title = "B"),
            ComicDto(source = "komiku", sourceId = " ", title = "C"),
        ).toDomainList()
        assertEquals(1, list.size)
    }

    @Test fun homeUsesNewKeyForFreshList() {
        val dto = json.decodeFromString<HomeDto>("""{"popular":[],"latest":[$comicJson],"new":[$comicJson],"scrapedAt":"2025"}""")
        assertEquals(1, dto.latest.size)
        assertEquals(1, dto.fresh.size)
    }

    @Test fun pagedMapping() {
        val dto = json.decodeFromString<PagedComicsDto>("""{"items":[$comicJson],"page":2,"hasNextPage":true}""")
        val page = dto.toPage()
        assertEquals(2, page.page)
        assertTrue(page.hasNext)
        assertEquals(1, page.items.size)
    }

    @Test fun envelopeErrorBecomesAppException() {
        val env = json.decodeFromString<EnvelopeDto<HomeDto>>(
            """{"success":false,"error":{"code":"SOURCE_TIMEOUT","message":"Scraper timeout"}}"""
        )
        try { env.unwrap(); fail("harus melempar") } catch (e: AppException) {
            assertEquals("SOURCE_TIMEOUT", e.code)
        }
    }

    @Test fun readerChapterMapsImagesInOrderAndNeighbours() {
        val dto = json.decodeFromString<EnvelopeDto<ReaderDto>>(
            """{"success":true,"data":{"comic":$comicJson,"chapter":{"id":"c","number":"12","title":"Bab 12","type":"manhwa",
              "images":[{"index":1,"image":"https://x/b","rawUrl":"r"},{"index":0,"image":"https://x/a","rawUrl":"r"}],
              "prevChapter":"11","nextChapter":null,"totalChapters":20},"chapters":[]}}"""
        ).unwrap()
        val ch = dto.toDomain("12")!!
        assertEquals(listOf("https://x/a", "https://x/b"), ch.pages.map { it.url })
        assertEquals("11", ch.prevKey)
        assertNull(ch.nextKey)
        assertFalse(ch.isNovel)
    }

    @Test fun novelChapterIsDetectedFromParagraphs() {
        val dto = json.decodeFromString<ReaderDto>(
            """{"chapter":{"number":"1","title":"Bab 1","type":"novel","images":[],
              "novelContent":{"paragraphs":["satu","dua"]}}}"""
        )
        val ch = dto.toDomain("1")!!
        assertTrue(ch.isNovel)
        assertEquals(2, ch.paragraphs.size)
    }
}
