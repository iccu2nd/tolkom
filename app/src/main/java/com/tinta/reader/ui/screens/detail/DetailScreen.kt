package com.tinta.reader.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tinta.reader.domain.model.ChapterRef
import com.tinta.reader.domain.model.Comic
import com.tinta.reader.domain.model.ComicDetail
import com.tinta.reader.domain.model.HistoryItem
import com.tinta.reader.ui.DetailViewModel
import com.tinta.reader.ui.components.Cover
import com.tinta.reader.ui.components.ErrorState
import com.tinta.reader.ui.components.PrimaryButton
import com.tinta.reader.ui.components.RatingChip
import com.tinta.reader.ui.components.ScreenTopBar
import com.tinta.reader.ui.components.SecondaryButton
import com.tinta.reader.ui.components.SkeletonBox
import com.tinta.reader.ui.components.TintaIcons
import com.tinta.reader.ui.components.UiState
import com.tinta.reader.ui.theme.Dimens
import com.tinta.reader.ui.theme.Ink
import com.tinta.reader.util.Strings
import com.tinta.reader.util.shortDate

@Composable
fun DetailScreen(
    vm: DetailViewModel,
    onBack: () -> Unit,
    onReadChapter: (source: String, id: String, chapterKey: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val bookmarked by vm.bookmarked.collectAsStateWithLifecycle()
    val lastRead by vm.lastRead.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    Box(modifier.fillMaxSize().background(Ink.Background)) {
        when (val s = ui.state) {
            UiState.Loading -> DetailSkeleton(onBack)
            is UiState.Error -> Column(Modifier.fillMaxSize()) {
                ScreenTopBar("", onBack)
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { ErrorState(s.message, vm::load) }
            }
            UiState.Empty -> Unit
            is UiState.Success -> DetailContent(
                detail = s.data,
                newestFirst = ui.newestFirst,
                opening = ui.opening,
                bookmarked = bookmarked,
                lastRead = lastRead,
                onBack = onBack,
                onToggleSort = vm::toggleSort,
                onToggleBookmark = vm::toggleBookmark,
                onReadPrimary = { vm.readPrimary { key -> onReadChapter(vm.source, vm.sourceId, key) } },
                onChapter = { key -> onReadChapter(vm.source, vm.sourceId, key) },
            )
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 8.dp))
    }
}

@Composable
private fun DetailContent(
    detail: ComicDetail,
    newestFirst: Boolean,
    opening: Boolean,
    bookmarked: Boolean,
    lastRead: HistoryItem?,
    onBack: () -> Unit,
    onToggleSort: () -> Unit,
    onToggleBookmark: () -> Unit,
    onReadPrimary: () -> Unit,
    onChapter: (String) -> Unit,
) {
    val comic = detail.comic
    val chapters = remember(detail, newestFirst) { if (newestFirst) detail.chapters else detail.chapters.reversed() }
    var expanded by remember(comic.key) { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item(key = "header") { Header(comic) }
            item(key = "actions") {
                Row(
                    Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Tanpa daftar chapter dari sumber, tombol baca tidak ditampilkan (bukan tombol palsu).
                    if (detail.chapters.isNotEmpty()) {
                        PrimaryButton(
                            label = when {
                                opening -> Strings.loadMore
                                lastRead != null -> Strings.continueReading
                                else -> Strings.readNow
                            },
                            onClick = onReadPrimary,
                            modifier = Modifier.weight(1f),
                            light = true,
                        )
                    }
                    BookmarkButton(bookmarked, onToggleBookmark)
                }
            }
            item(key = "synopsis") {
                Column(Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = 12.dp)) {
                    Text(Strings.synopsis, style = MaterialTheme.typography.titleMedium, color = Ink.TextPrimary)
                    Spacer(Modifier.height(6.dp))
                    val text = comic.synopsis
                    Text(
                        text ?: Strings.noSynopsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (text == null) Ink.TextMuted else Ink.TextSecondary,
                        maxLines = if (expanded) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(enabled = text != null) { expanded = !expanded },
                    )
                }
            }
            item(key = "chapters-header") {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "${Strings.chapters} (${detail.chapters.size})",
                        style = MaterialTheme.typography.titleLarge, color = Ink.TextPrimary,
                    )
                    if (detail.chapters.size > 1) {
                        Text(
                            if (newestFirst) Strings.sortNewest else Strings.sortOldest,
                            style = MaterialTheme.typography.labelLarge, color = Ink.TextSecondary,
                            modifier = Modifier.clickable(role = Role.Button, onClick = onToggleSort)
                                .defaultMinSize(minHeight = Dimens.MinTouch).padding(vertical = 14.dp),
                        )
                    }
                }
            }
            if (chapters.isEmpty()) {
                item(key = "no-chapters") {
                    Text(
                        Strings.noChapters, style = MaterialTheme.typography.bodyMedium, color = Ink.TextMuted,
                        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = 12.dp),
                    )
                }
            } else {
                items(chapters, key = { it.key }) { ch ->
                    ChapterRow(ch, isLast = ch.key == lastRead?.chapterKey, onClick = { onChapter(ch.key) })
                }
            }
        }
        // Tombol kembali melayang di atas banner.
        IconButton(
            onClick = onBack,
            modifier = Modifier.statusBarsPadding().padding(4.dp).size(Dimens.MinTouch)
                .clip(CircleShape).background(Ink.Scrim.copy(alpha = 0.45f)),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, Strings.back, tint = Ink.TextPrimary)
        }
    }
}

@Composable
private fun Header(comic: Comic) {
    Box(Modifier.fillMaxWidth()) {
        Cover(comic.cover, comic.title, Modifier.fillMaxWidth().height(300.dp), radius = 0.dp)
        Box(
            Modifier.fillMaxWidth().height(300.dp).background(
                Brush.verticalGradient(0.35f to Color.Transparent, 0.8f to Ink.Background.copy(alpha = 0.85f), 1f to Ink.Background),
            ),
        )
        Column(
            Modifier.align(Alignment.BottomStart).padding(horizontal = Dimens.ScreenPadding).padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(comic.title, style = MaterialTheme.typography.headlineMedium, color = Ink.TextPrimary, maxLines = 3, overflow = TextOverflow.Ellipsis)
            comic.alternativeTitles.firstOrNull()?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = Ink.TextMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            val meta = listOfNotNull(comic.type?.replaceFirstChar { c -> c.uppercase() }, comic.status).joinToString(" / ")
            if (meta.isNotEmpty() || comic.rating != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    comic.rating?.let { RatingChip(it) }
                    if (meta.isNotEmpty()) Text(meta, style = MaterialTheme.typography.labelMedium, color = Ink.TextSecondary)
                }
            }
            if (comic.genres.isNotEmpty()) {
                Text(comic.genres.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = Ink.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            val credits = listOfNotNull(
                comic.author?.let { "${Strings.author}: $it" },
                comic.artist?.takeIf { it != comic.author }?.let { "${Strings.artist}: $it" },
            ).joinToString("  |  ")
            if (credits.isNotEmpty()) {
                Text(credits, style = MaterialTheme.typography.bodySmall, color = Ink.TextMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun BookmarkButton(bookmarked: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.defaultMinSize(minHeight = Dimens.MinTouch).clip(RoundedCornerShape(Dimens.ButtonRadius))
            .background(Ink.Elevated).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            if (bookmarked) TintaIcons.Bookmark else TintaIcons.BookmarkBorder, null,
            tint = if (bookmarked) Ink.Accent else Ink.TextPrimary, modifier = Modifier.size(20.dp),
        )
        Text(if (bookmarked) Strings.bookmarked else Strings.bookmark, style = MaterialTheme.typography.labelLarge, color = Ink.TextPrimary)
    }
}

@Composable
private fun ChapterRow(ch: ChapterRef, isLast: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp).clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Dimens.ScreenPadding, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                ch.title, style = MaterialTheme.typography.bodyLarge,
                color = if (isLast) Ink.TextPrimary else Ink.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            shortDate(ch.releaseDate)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Ink.TextMuted) }
        }
        if (isLast) {
            Spacer(Modifier.width(12.dp))
            Text(Strings.continueReading, style = MaterialTheme.typography.labelMedium, color = Ink.Accent)
        }
    }
}

@Composable
private fun DetailSkeleton(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenTopBar("", onBack)
        Column(Modifier.padding(Dimens.ScreenPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonBox(Modifier.fillMaxWidth().height(220.dp))
            SkeletonBox(Modifier.fillMaxWidth(0.7f).height(24.dp), 4.dp)
            SkeletonBox(Modifier.fillMaxWidth().height(48.dp), Dimens.ButtonRadius)
            SkeletonBox(Modifier.fillMaxWidth().height(80.dp))
        }
    }
}
