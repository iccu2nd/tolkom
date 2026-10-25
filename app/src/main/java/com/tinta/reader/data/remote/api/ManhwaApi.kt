package com.tinta.reader.data.remote.api

import com.tinta.reader.data.remote.dto.DetailDto
import com.tinta.reader.data.remote.dto.EnvelopeDto
import com.tinta.reader.data.remote.dto.GenreDto
import com.tinta.reader.data.remote.dto.HomeDto
import com.tinta.reader.data.remote.dto.PagedComicsDto
import com.tinta.reader.data.remote.dto.ReaderDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Hanya endpoint yang terverifikasi dari kode sumber server (lihat docs/VERIFIED_ENDPOINTS.md). */
interface ManhwaApi {

    @GET("api/home")
    suspend fun home(
        @Query("source") source: String = "all",
        @Query("refresh") refresh: Boolean = false,
    ): EnvelopeDto<HomeDto>

    @GET("api/list")
    suspend fun list(
        @Query("source") source: String = "all",
        @Query("page") page: Int = 1,
    ): EnvelopeDto<PagedComicsDto>

    @GET("api/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("source") source: String = "all",
        @Query("page") page: Int = 1,
    ): EnvelopeDto<PagedComicsDto>

    @GET("api/genres")
    suspend fun genres(): EnvelopeDto<List<GenreDto>>

    @GET("api/genre/{genre}")
    suspend fun byGenre(
        @Path("genre") genre: String,
        @Query("source") source: String = "all",
        @Query("page") page: Int = 1,
    ): EnvelopeDto<PagedComicsDto>

    @GET("api/type/{type}")
    suspend fun byType(
        @Path("type") type: String,
        @Query("source") source: String = "all",
        @Query("page") page: Int = 1,
    ): EnvelopeDto<PagedComicsDto>

    @GET("api/comic/{source}/{id}")
    suspend fun detail(
        @Path("source") source: String,
        @Path("id") id: String,
    ): EnvelopeDto<DetailDto>

    @GET("api/comic/{source}/{id}/chapter/{chapter}")
    suspend fun chapter(
        @Path("source") source: String,
        @Path("id") id: String,
        @Path("chapter") chapter: String,
    ): EnvelopeDto<ReaderDto>
}
