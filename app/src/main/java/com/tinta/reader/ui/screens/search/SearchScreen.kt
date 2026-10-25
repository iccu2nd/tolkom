package com.tinta.reader.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tinta.reader.domain.model.Comic
import com.tinta.reader.ui.SearchViewModel
import com.tinta.reader.ui.components.PagedComicContent
import com.tinta.reader.ui.components.PagedUi
import com.tinta.reader.ui.theme.Dimens
import com.tinta.reader.ui.theme.Ink
import com.tinta.reader.util.Strings

@Composable
fun SearchScreen(vm: SearchViewModel, onBack: () -> Unit, onComic: (Comic) -> Unit, modifier: Modifier = Modifier) {
    val query by vm.query.collectAsStateWithLifecycle()
    val paged by vm.paged.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) { if (query.isBlank()) focus.requestFocus() }

    Column(modifier.fillMaxSize().background(Ink.Background).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(end = Dimens.ScreenPadding), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(Dimens.MinTouch)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, Strings.back, tint = Ink.TextPrimary)
            }
            Row(
                Modifier.weight(1f).height(Dimens.MinTouch).clip(RoundedCornerShape(Dimens.ButtonRadius))
                    .background(Ink.Surface).border(1.dp, Ink.Border, RoundedCornerShape(Dimens.ButtonRadius))
                    .padding(start = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Search, null, tint = Ink.TextMuted, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(Strings.searchHint, style = MaterialTheme.typography.bodyMedium, color = Ink.TextMuted, maxLines = 1)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = vm::onQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink.TextPrimary),
                        cursorBrush = SolidColor(Ink.Accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { vm.submit(); keyboard?.hide() }),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = { vm.clear(); focus.requestFocus() }, modifier = Modifier.size(Dimens.MinTouch)) {
                        Icon(Icons.Filled.Close, Strings.clear, tint = Ink.TextSecondary)
                    }
                }
            }
        }

        if (query.isBlank() || paged.status == PagedUi.Status.Idle) {
            RecentSearches(recent, onPick = { vm.submit(it); keyboard?.hide() }, onRemove = vm::removeRecent, onClearAll = vm::clearRecent)
        } else {
            PagedComicContent(
                paged = paged,
                onComic = { vm.rememberCurrent(); onComic(it) },
                onLoadMore = vm::loadMore,
                onRetry = vm::retry,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RecentSearches(
    recent: List<String>,
    onPick: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    if (recent.isEmpty()) return
    Column(Modifier.fillMaxSize().padding(top = 8.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(Strings.recentSearches, style = MaterialTheme.typography.titleMedium, color = Ink.TextPrimary)
            Text(
                Strings.clearRecent, style = MaterialTheme.typography.labelLarge, color = Ink.TextSecondary,
                modifier = Modifier.clickable(role = Role.Button, onClick = onClearAll).padding(vertical = 12.dp),
            )
        }
        LazyColumn {
            items(recent, key = { it }) { q ->
                Row(
                    Modifier.fillMaxWidth().clickable(role = Role.Button) { onPick(q) }
                        .padding(start = Dimens.ScreenPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(q, style = MaterialTheme.typography.bodyLarge, color = Ink.TextPrimary, modifier = Modifier.weight(1f), maxLines = 1)
                    IconButton(onClick = { onRemove(q) }, modifier = Modifier.size(Dimens.MinTouch)) {
                        Icon(Icons.Filled.Close, Strings.clear, tint = Ink.TextMuted, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
