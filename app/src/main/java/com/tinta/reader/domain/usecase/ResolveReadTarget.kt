package com.tinta.reader.domain.usecase

import com.tinta.reader.data.repository.CatalogRepository
import com.tinta.reader.data.repository.LibraryRepository
import com.tinta.reader.domain.model.Comic
import com.tinta.reader.util.AppException
import com.tinta.reader.util.Strings

/**
 * Menentukan chapter mana yang dibuka tombol "Baca":
 * lanjut dari riwayat bila ada, jika tidak chapter paling awal dari daftar sumber.
 */
class ResolveReadTarget(
    private val catalog: CatalogRepository,
    private val library: LibraryRepository,
) {
    suspend operator fun invoke(comic: Comic): String {
        library.lastRead(comic.source, comic.sourceId)?.let { return it.chapterKey }
        val detail = catalog.detail(comic.source, comic.sourceId)
        // Daftar diurutkan terbaru dulu, jadi chapter pertama ada di akhir.
        return detail.chapters.lastOrNull()?.key
            ?: throw AppException(AppException.Kind.NotFound, Strings.noChapters)
    }
}
