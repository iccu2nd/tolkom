package com.tinta.reader.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tinta.reader.AppContainer
import com.tinta.reader.TintaApp
import com.tinta.reader.data.local.ReaderSettings
import com.tinta.reader.data.repository.CatalogRepository
import com.tinta.reader.data.repository.LibraryRepository
import com.tinta.reader.domain.model.Bookmark
import com.tinta.reader.domain.model.Comic
import com.tinta.reader.domain.model.Genre
import com.tinta.reader.domain.model.HistoryItem
import com.tinta.reader.domain.model.HomeFeed
import com.tinta.reader.domain.model.Page
import com.tinta.reader.domain.model.ReaderChapter
import com.tinta.reader.domain.usecase.ResolveReadTarget
import com.tinta.reader.ui.components.PagedLoader
import com.tinta.reader.ui.components.PagedUi
import com.tinta.reader.ui.components.UiState
import com.tinta.reader.util.toAppException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun CreationExtras.container(): AppContainer = (this[APPLICATION_KEY] as TintaApp).container

/** Satu factory untuk semua ViewModel; argumen navigasi dibaca dari SavedStateHandle. */
val AppViewModelFactory = viewModelFactory {
    initializer { val c = container(); HomeViewModel(c.catalog, c.library, ResolveReadTarget(c.catalog, c.library)) }
    initializer { val c = container(); ExploreViewModel(c.catalog) }
    initializer { val c = container(); SearchViewModel(c.catalog, c.library) }
    initializer { val c = container(); ViewAllViewModel(createSavedStateHandle(), c.catalog) }
    initializer { val c = container(); DetailViewModel(createSavedStateHandle(), c.catalog, c.library, ResolveReadTarget(c.catalog, c.library)) }
    initializer { val c = container(); ReaderViewModel(createSavedStateHandle(), c.catalog, c.library, c.settings) }
    initializer { val c = container(); LibraryViewModel(c.library) }
    initializer { val c = container(); ProfileViewModel(c.library, c.settings) }
}

// ---------------------------------------------------------------- Home

data class HomeUi(
    val loading: Boolean = true,
    val feed: HomeFeed? = null,
    val error: String? = null,
    val readingKey: String? = null,
)

class HomeViewModel(
    private val catalog: CatalogRepository,
    library: LibraryRepository,
    private val resolveRead: ResolveReadTarget,
) : ViewModel() {
    private val _ui = MutableStateFlow(HomeUi())
    val ui: StateFlow<HomeUi> = _ui.asStateFlow()

    val history: StateFlow<List<HistoryItem>> = library.historyList()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Pesan sekali-tampil (snackbar), mis. gagal membuka chapter. */
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    private var job: Job? = null

    init { load(refresh = false) }

    fun load(refresh: Boolean = true) {
        job?.cancel()
        // Konten lama tetap tampil saat memuat ulang.
        _ui.update { it.copy(loading = it.feed == null, error = null) }
        job = viewModelScope.launch {
            try {
                val feed = catalog.home(refresh)
                _ui.update { it.copy(loading = false, feed = feed, error = null) }
            } catch (t: Throwable) {
                val msg = t.toAppException().message
                _ui.update { it.copy(loading = false, error = msg) }
            }
        }
    }

    fun read(comic: Comic, onTarget: (chapterKey: String) -> Unit) {
        if (_ui.value.readingKey != null) return
        _ui.update { it.copy(readingKey = comic.key) }
        viewModelScope.launch {
            try {
                onTarget(resolveRead(comic))
            } catch (t: Throwable) {
                _messages.trySend(t.toAppException().message)
            } finally {
                _ui.update { it.copy(readingKey = null) }
            }
        }
    }
}

// ---------------------------------------------------------------- Explore

sealed interface ExploreFilter {
    data object None : ExploreFilter
    data class ByType(val type: String) : ExploreFilter
    data class ByGenre(val slug: String) : ExploreFilter
}

class ExploreViewModel(private val catalog: CatalogRepository) : ViewModel() {
    private val _filter = MutableStateFlow<ExploreFilter>(ExploreFilter.None)
    val filter: StateFlow<ExploreFilter> = _filter.asStateFlow()

    private val _genres = MutableStateFlow<UiState<List<Genre>>>(UiState.Loading)
    val genres: StateFlow<UiState<List<Genre>>> = _genres.asStateFlow()

    private val loader = PagedLoader(viewModelScope) { page ->
        when (val f = _filter.value) {
            ExploreFilter.None -> catalog.latest(page)
            is ExploreFilter.ByType -> catalog.byType(f.type, page)
            is ExploreFilter.ByGenre -> catalog.byGenre(f.slug, page)
        }
    }
    val paged: StateFlow<PagedUi> = loader.state

    init { loader.refresh(); loadGenres() }

    fun select(filter: ExploreFilter) {
        if (filter == _filter.value) return
        _filter.value = filter
        loader.refresh()
    }

    fun refresh() = loader.refresh()
    fun loadMore() = loader.loadMore()

    fun loadGenres() {
        _genres.value = UiState.Loading
        viewModelScope.launch {
            _genres.value = try {
                catalog.genres().let { if (it.isEmpty()) UiState.Empty else UiState.Success(it) }
            } catch (t: Throwable) {
                UiState.Error(t.toAppException().message)
            }
        }
    }
}

// ---------------------------------------------------------------- Search

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val catalog: CatalogRepository,
    private val library: LibraryRepository,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private var activeQuery = ""
    private val loader = PagedLoader(viewModelScope) { page -> catalog.search(activeQuery, page) }
    val paged: StateFlow<PagedUi> = loader.state

    val recent: StateFlow<List<String>> = library.recentSearches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            _query.debounce(450).map { it.trim() }.distinctUntilChanged().collect { q -> if (q != activeQuery) execute(q) }
        }
    }

    private fun execute(q: String) {
        activeQuery = q
        if (q.isEmpty()) loader.reset() else loader.refresh()
    }

    fun onQueryChange(value: String) { _query.value = value }

    /** Aksi "Cari" di keyboard / chip pencarian terakhir: jalan langsung tanpa menunggu debounce. */
    fun submit(value: String = _query.value) {
        val q = value.trim()
        _query.value = value
        if (q.isEmpty()) return
        viewModelScope.launch { library.addSearch(q) }
        execute(q)
    }

    fun clear() { _query.value = ""; execute("") }
    fun loadMore() = loader.loadMore()
    fun retry() = execute(activeQuery)
    fun removeRecent(q: String) { viewModelScope.launch { library.removeSearch(q) } }
    fun clearRecent() { viewModelScope.launch { library.clearSearches() } }
    fun rememberCurrent() { val q = _query.value.trim(); if (q.isNotEmpty()) viewModelScope.launch { library.addSearch(q) } }
}

// ---------------------------------------------------------------- View all

class ViewAllViewModel(handle: SavedStateHandle, private val catalog: CatalogRepository) : ViewModel() {
    val kind: String = handle.get<String>("kind") ?: "latest"

    private val loader = PagedLoader(viewModelScope) { page ->
        when (kind) {
            "latest" -> catalog.latest(page)
            else -> {
                // Beranda hanya satu halaman; endpoint /api/home tidak punya paginasi.
                val home = catalog.cachedHome ?: catalog.home()
                val list = if (kind == "popular") home.popular else home.fresh
                Page(if (page == 1) list else emptyList(), page, false)
            }
        }
    }
    val paged: StateFlow<PagedUi> = loader.state

    init { loader.refresh() }
    fun refresh() = loader.refresh()
    fun loadMore() = loader.loadMore()
}

// ---------------------------------------------------------------- Detail

data class DetailUi(
    val state: UiState<com.tinta.reader.domain.model.ComicDetail> = UiState.Loading,
    val newestFirst: Boolean = true,
    val opening: Boolean = false,
)

class DetailViewModel(
    handle: SavedStateHandle,
    private val catalog: CatalogRepository,
    private val library: LibraryRepository,
    private val resolveRead: ResolveReadTarget,
) : ViewModel() {
    val source: String = checkNotNull(handle.get<String>("source"))
    val sourceId: String = checkNotNull(handle.get<String>("id"))

    private val _ui = MutableStateFlow(DetailUi())
    val ui: StateFlow<DetailUi> = _ui.asStateFlow()

    val bookmarked: StateFlow<Boolean> = library.isBookmarked(source, sourceId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val lastRead: StateFlow<HistoryItem?> = library.historyFor(source, sourceId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    init { load() }

    fun load() {
        _ui.update { it.copy(state = UiState.Loading) }
        viewModelScope.launch {
            val next = try {
                UiState.Success(catalog.detail(source, sourceId))
            } catch (t: Throwable) {
                UiState.Error(t.toAppException().message)
            }
            _ui.update { it.copy(state = next) }
        }
    }

    fun toggleSort() = _ui.update { it.copy(newestFirst = !it.newestFirst) }

    fun toggleBookmark() {
        val detail = (_ui.value.state as? UiState.Success)?.data ?: return
        viewModelScope.launch { library.setBookmarked(detail.comic, !bookmarked.value) }
    }

    fun readPrimary(onTarget: (String) -> Unit) {
        val detail = (_ui.value.state as? UiState.Success)?.data ?: return
        if (_ui.value.opening) return
        _ui.update { it.copy(opening = true) }
        viewModelScope.launch {
            try { onTarget(resolveRead(detail.comic)) }
            catch (t: Throwable) { _messages.trySend(t.toAppException().message) }
            finally { _ui.update { it.copy(opening = false) } }
        }
    }
}

// ---------------------------------------------------------------- Reader

data class ReaderUi(
    val state: UiState<ReaderChapter> = UiState.Loading,
    val startIndex: Int = 0,
    val startOffset: Int = 0,
)

class ReaderViewModel(
    handle: SavedStateHandle,
    private val catalog: CatalogRepository,
    private val library: LibraryRepository,
    settingsStore: com.tinta.reader.data.local.SettingsStore,
) : ViewModel() {
    val source: String = checkNotNull(handle.get<String>("source"))
    val sourceId: String = checkNotNull(handle.get<String>("id"))
    val chapterKey: String = checkNotNull(handle.get<String>("chapter"))

    private val _ui = MutableStateFlow(ReaderUi())
    val ui: StateFlow<ReaderUi> = _ui.asStateFlow()

    val settings: StateFlow<ReaderSettings> = settingsStore.reader
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderSettings())

    init { load() }

    fun load() {
        _ui.update { it.copy(state = UiState.Loading) }
        viewModelScope.launch {
            try {
                val chapter = catalog.chapter(source, sourceId, chapterKey)
                val saved = library.lastRead(source, sourceId)?.takeIf { it.chapterKey == chapterKey }
                val idx = saved?.scrollIndex ?: 0
                val off = saved?.scrollOffset ?: 0
                _ui.value = ReaderUi(UiState.Success(chapter), idx, off)
                // Catat chapter ini di riwayat segera, agar "Lanjut Membaca" muncul.
                persist(chapter, idx, off)
            } catch (t: Throwable) {
                _ui.update { it.copy(state = UiState.Error(t.toAppException().message)) }
            }
        }
    }

    fun saveProgress(index: Int, offset: Int) {
        val chapter = (_ui.value.state as? UiState.Success)?.data ?: return
        viewModelScope.launch { persist(chapter, index, offset) }
    }

    private suspend fun persist(chapter: ReaderChapter, index: Int, offset: Int) {
        // NonCancellable: simpan tetap selesai walau layar langsung ditutup.
        withContext(NonCancellable) {
            library.saveProgress(
                HistoryItem(
                    source = source,
                    sourceId = sourceId,
                    title = chapter.comic?.title ?: sourceId.replace('-', ' '),
                    cover = chapter.comic?.cover,
                    chapterKey = chapterKey,
                    chapterTitle = chapter.title,
                    scrollIndex = index,
                    scrollOffset = offset,
                    updatedAt = 0L,
                )
            )
        }
    }
}

// ---------------------------------------------------------------- Library & Profile

class LibraryViewModel(private val library: LibraryRepository) : ViewModel() {
    val bookmarks: StateFlow<List<Bookmark>?> = library.bookmarkList()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val history: StateFlow<List<HistoryItem>?> = library.historyList()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    fun removeHistory(item: HistoryItem) { viewModelScope.launch { library.removeHistory(item.source, item.sourceId) } }
}

class ProfileViewModel(
    private val library: LibraryRepository,
    private val settingsStore: com.tinta.reader.data.local.SettingsStore,
) : ViewModel() {
    val settings: StateFlow<ReaderSettings> = settingsStore.reader
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderSettings())

    fun setKeepScreenOn(v: Boolean) { viewModelScope.launch { settingsStore.setKeepScreenOn(v) } }
    fun setPageGap(v: Boolean) { viewModelScope.launch { settingsStore.setPageGap(v) } }
    fun clearHistory() { viewModelScope.launch { library.clearHistory() } }
    fun clearSearches() { viewModelScope.launch { library.clearSearches() } }
}
