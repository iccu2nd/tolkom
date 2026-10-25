package com.tinta.reader.ui.screens.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tinta.reader.domain.model.Comic
import com.tinta.reader.ui.ExploreFilter
import com.tinta.reader.ui.ExploreViewModel
import com.tinta.reader.ui.components.FilterChipTinta
import com.tinta.reader.ui.components.PagedComicContent
import com.tinta.reader.ui.components.UiState
import com.tinta.reader.ui.theme.Dimens
import com.tinta.reader.ui.theme.Ink
import com.tinta.reader.util.Strings
import androidx.compose.foundation.layout.statusBarsPadding

/** Tipe yang benar-benar didukung /api/type/:type (selain "novel", yang bukan fokus aplikasi ini). */
private val Types = listOf("manhwa" to "Manhwa", "manga" to "Manga", "manhua" to "Manhua")

@Composable
fun ExploreScreen(
    vm: ExploreViewModel,
    onComic: (Comic) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val filter by vm.filter.collectAsStateWithLifecycle()
    val paged by vm.paged.collectAsStateWithLifecycle()
    val genres by vm.genres.collectAsStateWithLifecycle()

    Column(modifier.fillMaxSize().background(Ink.Background).statusBarsPadding()) {
        Text(
            Strings.navExplore, style = MaterialTheme.typography.headlineMedium, color = Ink.TextPrimary,
            modifier = Modifier.padding(start = Dimens.ScreenPadding, top = 14.dp, end = Dimens.ScreenPadding),
        )
        Row(
            Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = 12.dp).fillMaxWidth()
                .height(Dimens.MinTouch).clip(RoundedCornerShape(Dimens.ButtonRadius))
                .background(Ink.Surface).border(1.dp, Ink.Border, RoundedCornerShape(Dimens.ButtonRadius))
                .clickable(role = Role.Button, onClickLabel = Strings.search, onClick = onSearch)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Search, null, tint = Ink.TextMuted, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(Strings.searchHint, style = MaterialTheme.typography.bodyMedium, color = Ink.TextMuted, maxLines = 1)
        }

        LazyRow(contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item(key = "all") {
                FilterChipTinta(Strings.allTypes, filter == ExploreFilter.None, { vm.select(ExploreFilter.None) })
            }
            items(Types, key = { it.first }) { (slug, label) ->
                FilterChipTinta(label, filter == ExploreFilter.ByType(slug), { vm.select(ExploreFilter.ByType(slug)) })
            }
        }

        when (val g = genres) {
            is UiState.Success -> LazyRow(
                contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(g.data, key = { it.slug }) { genre ->
                    FilterChipTinta(genre.name, filter == ExploreFilter.ByGenre(genre.slug), { vm.select(ExploreFilter.ByGenre(genre.slug)) })
                }
            }
            is UiState.Error -> Row(
                Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding).defaultMinSize(minHeight = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(g.message, style = MaterialTheme.typography.bodySmall, color = Ink.TextMuted, modifier = Modifier.weight(1f))
                Text(
                    Strings.retry, style = MaterialTheme.typography.labelLarge, color = Ink.TextPrimary,
                    modifier = Modifier.clickable(role = Role.Button) { vm.loadGenres() }.padding(12.dp),
                )
            }
            else -> Unit
        }

        PagedComicContent(
            paged = paged,
            onComic = onComic,
            onLoadMore = vm::loadMore,
            onRetry = vm::refresh,
            modifier = Modifier.weight(1f),
        )
    }
}
