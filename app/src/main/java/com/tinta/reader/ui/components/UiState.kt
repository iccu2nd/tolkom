package com.tinta.reader.ui.components

import com.tinta.reader.domain.model.Comic
import com.tinta.reader.domain.model.Page
import com.tinta.reader.util.toAppException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Pola status layar yang konsisten: Loading, Success, Empty, Error. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data object Empty : UiState<Nothing>
    data class Error(val message: String) : UiState<Nothing>
}

data class PagedUi(
    val status: Status = Status.Idle,
    val items: List<Comic> = emptyList(),
    val page: Int = 0,
    val hasNext: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val moreError: String? = null,
) {
    enum class Status { Idle, Loading, Content, Empty, Error }
}

/**
 * Memuat daftar komik bertahap. refresh() membatalkan permintaan yang masih berjalan,
 * sehingga hasil kueri lama tidak menimpa kueri baru.
 */
class PagedLoader(
    private val scope: CoroutineScope,
    private val fetch: suspend (page: Int) -> Page<Comic>,
) {
    private val _state = MutableStateFlow(PagedUi())
    val state: StateFlow<PagedUi> = _state.asStateFlow()
    private var job: Job? = null

    fun refresh() {
        job?.cancel()
        _state.value = PagedUi(status = PagedUi.Status.Loading)
        job = scope.launch {
            try {
                val p = fetch(1)
                val items = p.items.distinctBy { it.key }
                _state.value = if (items.isEmpty()) PagedUi(status = PagedUi.Status.Empty)
                else PagedUi(PagedUi.Status.Content, items, 1, p.hasNext)
            } catch (t: Throwable) {
                _state.value = PagedUi(status = PagedUi.Status.Error, error = t.toAppException().message)
            }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.status != PagedUi.Status.Content || !s.hasNext || s.loadingMore) return
        _state.update { it.copy(loadingMore = true, moreError = null) }
        job = scope.launch {
            try {
                val p = fetch(s.page + 1)
                val merged = (s.items + p.items).distinctBy { it.key }
                val grew = merged.size > s.items.size
                _state.update { it.copy(items = merged, page = s.page + 1, hasNext = p.hasNext && grew, loadingMore = false) }
            } catch (t: Throwable) {
                _state.update { it.copy(loadingMore = false, moreError = t.toAppException().message) }
            }
        }
    }

    fun cancel() { job?.cancel() }

    fun reset() {
        job?.cancel()
        _state.value = PagedUi()
    }
}
