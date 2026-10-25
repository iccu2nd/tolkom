package com.tinta.reader.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tinta.reader.domain.model.Comic
import com.tinta.reader.ui.ViewAllViewModel
import com.tinta.reader.ui.components.PagedComicContent
import com.tinta.reader.ui.components.ScreenTopBar
import com.tinta.reader.ui.theme.Ink
import com.tinta.reader.util.Strings

@Composable
fun ViewAllScreen(vm: ViewAllViewModel, onBack: () -> Unit, onComic: (Comic) -> Unit, modifier: Modifier = Modifier) {
    val paged by vm.paged.collectAsStateWithLifecycle()
    val title = when (vm.kind) {
        "popular" -> Strings.popular
        "new" -> Strings.newlyAdded
        else -> Strings.latestUpdates
    }
    Column(modifier.fillMaxSize().background(Ink.Background).navigationBarsPadding()) {
        ScreenTopBar(title, onBack)
        PagedComicContent(
            paged = paged, onComic = onComic, onLoadMore = vm::loadMore, onRetry = vm::refresh,
            modifier = Modifier.weight(1f),
        )
    }
}
