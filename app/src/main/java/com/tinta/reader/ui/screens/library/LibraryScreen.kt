package com.tinta.reader.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tinta.reader.domain.model.Bookmark
import com.tinta.reader.domain.model.HistoryItem
import com.tinta.reader.ui.LibraryViewModel
import com.tinta.reader.ui.components.Cover
import com.tinta.reader.ui.components.EmptyState
import com.tinta.reader.ui.components.GridSkeleton
import com.tinta.reader.ui.components.gridColumns
import com.tinta.reader.ui.theme.Dimens
import com.tinta.reader.ui.theme.Ink
import com.tinta.reader.util.Strings

@Composable
fun LibraryScreen(
    vm: LibraryViewModel,
    onBookmark: (Bookmark) -> Unit,
    onResume: (HistoryItem) -> Unit,
    onExplore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bookmarks by vm.bookmarks.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier.fillMaxSize().background(Ink.Background).statusBarsPadding()) {
        Text(
            Strings.libraryTitle, style = MaterialTheme.typography.headlineMedium, color = Ink.TextPrimary,
            modifier = Modifier.padding(start = Dimens.ScreenPadding, top = 14.dp, end = Dimens.ScreenPadding),
        )
        Row(Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            TabLabel(Strings.tabBookmarks, tab == 0) { tab = 0 }
            TabLabel(Strings.tabHistory, tab == 1) { tab = 1 }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (tab == 0) {
                val list = bookmarks
                when {
                    list == null -> GridSkeleton(gridColumns())
                    list.isEmpty() -> EmptyState(
                        Strings.emptyBookmarks, Strings.emptyBookmarksBody,
                        Modifier.align(Alignment.Center), Strings.goExplore, onExplore,
                    )
                    else -> LazyVerticalGrid(
                        columns = GridCells.Fixed(gridColumns()),
                        contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(list, key = { "${it.source}:${it.sourceId}" }) { b ->
                            Column(
                                Modifier.clip(RoundedCornerShape(Dimens.CoverRadius))
                                    .clickable(role = Role.Button, onClickLabel = b.title) { onBookmark(b) },
                            ) {
                                Cover(b.cover, b.title, Modifier.fillMaxWidth().aspectRatio(Dimens.CoverRatio))
                                Spacer(Modifier.height(8.dp))
                                Text(b.title, style = MaterialTheme.typography.titleSmall, color = Ink.TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            } else {
                val list = history
                when {
                    list == null -> GridSkeleton(1)
                    list.isEmpty() -> EmptyState(
                        Strings.emptyHistory, Strings.emptyHistoryBody,
                        Modifier.align(Alignment.Center), Strings.goExplore, onExplore,
                    )
                    else -> LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                        items(list, key = { "${it.source}:${it.sourceId}" }) { h ->
                            HistoryRow(h, onClick = { onResume(h) }, onRemove = { vm.removeHistory(h) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabLabel(text: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.defaultMinSize(minHeight = Dimens.MinTouch).clickable(role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text, style = MaterialTheme.typography.titleMedium,
            color = if (selected) Ink.TextPrimary else Ink.TextMuted,
        )
        Spacer(Modifier.height(4.dp))
        Box(Modifier.height(2.dp).width(24.dp).background(if (selected) Ink.Accent else Ink.Background))
    }
}

@Composable
private fun HistoryRow(item: HistoryItem, onClick: () -> Unit, onRemove: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)
            .padding(start = Dimens.ScreenPadding, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Cover(item.cover, item.title, Modifier.width(56.dp).aspectRatio(Dimens.CoverRatio))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleSmall, color = Ink.TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(item.chapterTitle, style = MaterialTheme.typography.bodySmall, color = Ink.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(Strings.continueReading, style = MaterialTheme.typography.labelMedium, color = Ink.Accent)
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(Dimens.MinTouch)) {
            Icon(Icons.Filled.Delete, Strings.removeFromHistory, tint = Ink.TextMuted)
        }
    }
}
