package com.tinta.reader.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.tinta.reader.domain.model.Comic
import com.tinta.reader.ui.theme.Dimens
import com.tinta.reader.ui.theme.Ink
import com.tinta.reader.util.Strings

@Composable
fun gridColumns(): Int = if (LocalConfiguration.current.screenWidthDp >= 600) 3 else 2

/** Menampilkan PagedUi lengkap: skeleton, error + retry, kosong, grid dengan muat bertahap. */
@Composable
fun PagedComicContent(
    paged: PagedUi,
    onComic: (Comic) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    emptyTitle: String = Strings.noResults,
    emptyBody: String = Strings.noResultsBody,
    contentPadding: PaddingValues = PaddingValues(bottom = 24.dp),
) {
    Box(modifier.fillMaxSize()) {
        when (paged.status) {
            PagedUi.Status.Idle -> Unit
            PagedUi.Status.Loading -> GridSkeleton(gridColumns(), Modifier.padding(top = 8.dp))
            PagedUi.Status.Error -> ErrorState(paged.error ?: Strings.errUnknown, onRetry, Modifier.align(Alignment.Center))
            PagedUi.Status.Empty -> EmptyState(emptyTitle, emptyBody, Modifier.align(Alignment.Center))
            PagedUi.Status.Content -> ComicGrid(paged, onComic, onLoadMore, contentPadding)
        }
    }
}

@Composable
fun ComicGrid(
    paged: PagedUi,
    onComic: (Comic) -> Unit,
    onLoadMore: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(bottom = 24.dp),
) {
    val state = rememberLazyGridState()
    val size by rememberUpdatedState(paged.items.size)
    val loadMore by rememberUpdatedState(onLoadMore)
    LaunchedEffect(state, paged.items.size) {
        snapshotFlow { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .collect { last -> if (last >= size - 6) loadMore() }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumns()),
        state = state,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.ScreenPadding, end = Dimens.ScreenPadding,
            top = 8.dp, bottom = contentPadding.calculateBottomPadding(),
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(paged.items, key = { it.key }) { comic ->
            ComicCard(comic, onClick = { onComic(comic) })
        }
        if (paged.loadingMore || paged.moreError != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                if (paged.moreError != null) {
                    ErrorState(paged.moreError, onRetry = onLoadMore, modifier = Modifier.fillMaxWidth())
                } else {
                    Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.Center) {
                        Text(Strings.loadMore, style = MaterialTheme.typography.bodySmall, color = Ink.TextMuted)
                    }
                }
            }
        }
    }
}
