package com.tinta.reader.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tinta.reader.domain.model.Comic
import com.tinta.reader.domain.model.HistoryItem
import com.tinta.reader.ui.HomeViewModel
import com.tinta.reader.ui.components.Cover
import com.tinta.reader.ui.components.ComicCard
import com.tinta.reader.ui.components.EmptyState
import com.tinta.reader.ui.components.ErrorState
import com.tinta.reader.ui.components.HomeTopBar
import com.tinta.reader.ui.components.PrimaryButton
import com.tinta.reader.ui.components.RatingChip
import com.tinta.reader.ui.components.SectionHeader
import com.tinta.reader.ui.components.SecondaryButton
import com.tinta.reader.ui.components.SkeletonBox
import com.tinta.reader.ui.theme.Dimens
import com.tinta.reader.ui.theme.Ink
import com.tinta.reader.util.Strings
import com.tinta.reader.util.displayLatest
import com.tinta.reader.util.joinGenres

@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onComic: (Comic) -> Unit,
    onReadChapter: (Comic, String) -> Unit,
    onResume: (HistoryItem) -> Unit,
    onSearch: () -> Unit,
    onViewAll: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    Box(modifier.fillMaxSize().background(Ink.Background)) {
        Column(Modifier.fillMaxSize()) {
            HomeTopBar(onSearch)
            val feed = ui.feed
            when {
                feed == null && ui.loading -> HomeSkeleton()
                feed == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ErrorState(ui.error ?: Strings.errUnknown, onRetry = { vm.load() })
                }
                feed.latest.isEmpty() && feed.popular.isEmpty() && feed.fresh.isEmpty() ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            Strings.emptyFeed, Strings.emptyFeedBody,
                            actionLabel = Strings.retry, onAction = { vm.load() },
                        )
                    }
                else -> {
                    val heroItems = remember(feed) {
                        (feed.popular.ifEmpty { feed.latest }).filter { !it.cover.isNullOrBlank() }.take(5)
                    }
                    val heroKeys = remember(heroItems) { heroItems.map { it.key }.toSet() }
                    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Dimens.SectionGap)) {
                        if (heroItems.isNotEmpty()) {
                            item(key = "hero") {
                                HeroCarousel(
                                    items = heroItems,
                                    readingKey = ui.readingKey,
                                    onRead = { c -> vm.read(c) { key -> onReadChapter(c, key) } },
                                    onDetails = onComic,
                                )
                            }
                        }
                        if (ui.error != null) {
                            item(key = "stale") {
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        ui.error!!, style = MaterialTheme.typography.bodySmall, color = Ink.TextMuted,
                                        modifier = Modifier.weight(1f),
                                    )
                                    SecondaryButton(Strings.retry, { vm.load() })
                                }
                            }
                        }
                        if (history.isNotEmpty()) {
                            item(key = "continue") {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    SectionHeader(Strings.continueSection)
                                    LazyRow(
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Dimens.ScreenPadding),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        items(history.take(10), key = { "${it.source}:${it.sourceId}" }) { h ->
                                            ContinueCard(h, onClick = { onResume(h) })
                                        }
                                    }
                                }
                            }
                        }
                        if (feed.latest.isNotEmpty()) {
                            item(key = "latest") {
                                ComicSection(Strings.latestUpdates, feed.latest.take(15), onComic) { onViewAll("latest") }
                            }
                        }
                        val popular = feed.popular.filter { it.key !in heroKeys }
                        if (popular.isNotEmpty()) {
                            item(key = "popular") {
                                ComicSection(Strings.popular, popular.take(15), onComic) { onViewAll("popular") }
                            }
                        }
                        if (feed.fresh.isNotEmpty()) {
                            item(key = "fresh") {
                                ComicSection(Strings.newlyAdded, feed.fresh.take(15), onComic) { onViewAll("new") }
                            }
                        }
                        item(key = "end") { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
    }
}

@Composable
private fun ComicSection(title: String, items: List<Comic>, onComic: (Comic) -> Unit, onViewAll: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title, onViewAll = onViewAll)
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Dimens.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.key }) { c ->
                ComicCard(c, onClick = { onComic(c) }, modifier = Modifier.width(Dimens.CoverWidth))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroCarousel(
    items: List<Comic>,
    readingKey: String?,
    onRead: (Comic) -> Unit,
    onDetails: (Comic) -> Unit,
) {
    val height = (LocalConfiguration.current.screenHeightDp * 0.46f).dp.coerceIn(300.dp, Dimens.HeroHeight + 20.dp)
    val pager = rememberPagerState(pageCount = { items.size })
    Box(Modifier.fillMaxWidth().height(height)) {
        HorizontalPager(pager, Modifier.fillMaxSize(), key = { items[it].key }) { i ->
            HeroSlide(items[i], busy = readingKey == items[i].key, onRead = { onRead(items[i]) }, onDetails = { onDetails(items[i]) })
        }
        if (items.size > 1) {
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(items.size) { i ->
                    val active = pager.currentPage == i
                    Box(
                        Modifier.height(4.dp).width(if (active) 18.dp else 6.dp).clip(CircleShape)
                            .background(if (active) Ink.TextPrimary else Ink.TextMuted),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroSlide(comic: Comic, busy: Boolean, onRead: () -> Unit, onDetails: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Cover(comic.cover, comic.title, Modifier.fillMaxSize(), radius = 0.dp)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.30f to Color.Transparent,
                    0.70f to Ink.Background.copy(alpha = 0.72f),
                    1.0f to Ink.Background,
                ),
            ),
        )
        Column(
            Modifier.align(Alignment.BottomStart).padding(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            comic.genres.joinGenres()?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = Ink.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(comic.title, style = MaterialTheme.typography.headlineMedium, color = Ink.TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val latest = displayLatest(comic.latestChapter)
            if (comic.rating != null || latest != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    comic.rating?.let { RatingChip(it) }
                    latest?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = Ink.TextSecondary) }
                }
            }
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton(if (busy) Strings.loadMore else Strings.read, onRead, light = true, modifier = Modifier.width(132.dp))
                SecondaryButton(Strings.details, onDetails, modifier = Modifier.width(112.dp))
            }
        }
    }
}

@Composable
private fun ContinueCard(item: HistoryItem, onClick: () -> Unit) {
    Row(
        Modifier.width(250.dp).clip(RoundedCornerShape(Dimens.CoverRadius)).background(Ink.Surface)
            .clickable(role = Role.Button, onClick = onClick).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(item.cover, item.title, Modifier.width(52.dp).aspectRatio(Dimens.CoverRatio))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleSmall, color = Ink.TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(item.chapterTitle, style = MaterialTheme.typography.bodySmall, color = Ink.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(Strings.continueReading, style = MaterialTheme.typography.labelMedium, color = Ink.Accent)
        }
    }
}

@Composable
private fun HomeSkeleton() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Dimens.SectionGap)) {
        SkeletonBox(Modifier.fillMaxWidth().height(320.dp), 0.dp)
        Column(Modifier.padding(horizontal = Dimens.ScreenPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SkeletonBox(Modifier.width(140.dp).height(18.dp), 4.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) { SkeletonBox(Modifier.width(Dimens.CoverWidth).aspectRatio(Dimens.CoverRatio)) }
            }
        }
    }
}
