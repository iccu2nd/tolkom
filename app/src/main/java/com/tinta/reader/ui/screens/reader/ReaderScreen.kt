package com.tinta.reader.ui.screens.reader

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tinta.reader.domain.model.ReaderChapter
import com.tinta.reader.ui.ReaderUi
import com.tinta.reader.ui.ReaderViewModel
import com.tinta.reader.ui.components.ErrorState
import com.tinta.reader.ui.components.FullScreenLoading
import com.tinta.reader.ui.components.PrimaryButton
import com.tinta.reader.ui.components.ScreenTopBar
import com.tinta.reader.ui.components.SecondaryButton
import com.tinta.reader.ui.components.SkeletonBox
import com.tinta.reader.ui.components.UiState
import com.tinta.reader.ui.theme.Ink
import com.tinta.reader.util.Strings
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

@Composable
fun ReaderScreen(
    vm: ReaderViewModel,
    onBack: () -> Unit,
    onOpenChapter: (key: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    KeepScreenOn(settings.keepScreenOn)

    Box(modifier.fillMaxSize().background(Color.Black)) {
        when (val s = ui.state) {
            UiState.Loading -> Column(Modifier.fillMaxSize()) {
                ScreenTopBar("", onBack)
                FullScreenLoading()
            }
            is UiState.Error -> Column(Modifier.fillMaxSize()) {
                ScreenTopBar("", onBack)
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { ErrorState(s.message, vm::load) }
            }
            UiState.Empty -> Unit
            is UiState.Success -> ReaderContent(
                chapter = s.data, ui = ui, pageGap = settings.pageGap,
                onBack = onBack, onOpenChapter = onOpenChapter,
                onProgress = vm::saveProgress,
            )
        }
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun ReaderContent(
    chapter: ReaderChapter,
    ui: ReaderUi,
    pageGap: Boolean,
    onBack: () -> Unit,
    onOpenChapter: (String) -> Unit,
    onProgress: (index: Int, offset: Int) -> Unit,
) {
    val listState = rememberLazyListState(ui.startIndex, ui.startOffset)
    var controls by remember { mutableStateOf(true) }
    ImmersiveMode(hidden = !controls)

    // Simpan progres 0,7 detik setelah scroll berhenti, dan sekali lagi saat layar ditutup.
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .debounce(700)
            .collect { (i, o) -> onProgress(i, o) }
    }
    DisposableEffect(listState) {
        onDispose { onProgress(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) }
    }

    Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onTap = { controls = !controls }) }) {
        LazyColumn(Modifier.fillMaxSize(), state = listState) {
            if (chapter.isNovel) {
                itemsIndexed(chapter.paragraphs, key = { i, _ -> "t$i" }) { _, p ->
                    Text(
                        p, style = MaterialTheme.typography.bodyLarge, color = Ink.TextPrimary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            } else if (chapter.pages.isEmpty()) {
                item(key = "empty") {
                    Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text(Strings.readerNoPages, color = Ink.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(32.dp))
                    }
                }
            } else {
                itemsIndexed(chapter.pages, key = { i, _ -> "p$i" }) { _, page ->
                    PageImage(page.url, pageGap)
                }
            }
            item(key = "end") { ChapterEnd(chapter, onOpenChapter) }
        }

        AnimatedVisibility(
            visible = controls, modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn(), exit = fadeOut(),
        ) {
            Box(Modifier.background(Color.Black.copy(alpha = 0.88f))) {
                ScreenTopBar(
                    title = listOfNotNull(chapter.comic?.title, chapter.title).distinct().joinToString(" - "),
                    onBack = onBack,
                )
            }
        }
        AnimatedVisibility(
            visible = controls, modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(), exit = fadeOut(),
        ) {
            Row(
                Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.88f)).navigationBarsPadding().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                NavButton(Strings.prevChapter, chapter.prevKey, Modifier.weight(1f), onOpenChapter)
                NavButton(Strings.nextChapter, chapter.nextKey, Modifier.weight(1f), onOpenChapter, primary = true)
            }
        }
    }
}

@Composable
private fun NavButton(label: String, key: String?, modifier: Modifier, onOpen: (String) -> Unit, primary: Boolean = false) {
    val enabled = key != null
    Box(modifier, contentAlignment = Alignment.Center) {
        if (enabled) {
            if (primary) PrimaryButton(label, { onOpen(key!!) }, Modifier.fillMaxWidth(), light = true)
            else SecondaryButton(label, { onOpen(key!!) }, Modifier.fillMaxWidth())
        } else {
            Text(
                label, style = MaterialTheme.typography.labelLarge, color = Ink.TextMuted,
                modifier = Modifier.padding(vertical = 14.dp),
            )
        }
    }
}

@Composable
private fun ChapterEnd(chapter: ReaderChapter, onOpen: (String) -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding().padding(bottom = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val next = chapter.nextKey
        if (next != null) {
            PrimaryButton(Strings.nextChapter, { onOpen(next) }, Modifier.fillMaxWidth(), light = true)
        } else {
            Text(Strings.noNext, style = MaterialTheme.typography.bodyMedium, color = Ink.TextMuted, textAlign = TextAlign.Center)
        }
    }
}

/** Satu halaman webtoon: lebar penuh, tinggi mengikuti rasio asli gambar (tidak di-stretch). */
@Composable
private fun PageImage(url: String, gap: Boolean) {
    var attempt by remember(url) { mutableIntStateOf(0) }
    val context = LocalContext.current
    key(attempt) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context).data(url).build(),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth(),
            loading = {
                Box(Modifier.fillMaxWidth().height(480.dp).background(Color(0xFF0C0C0C)), contentAlignment = Alignment.Center) {
                    SkeletonBox(Modifier.fillMaxWidth(0.4f).height(6.dp), 3.dp)
                }
            },
            error = {
                Column(
                    Modifier.fillMaxWidth().height(260.dp).background(Color(0xFF0C0C0C)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(Strings.imageFailed, style = MaterialTheme.typography.bodyMedium, color = Ink.TextSecondary)
                    Spacer(Modifier.height(10.dp))
                    SecondaryButton(Strings.reloadImage, { attempt++ })
                }
            },
        )
    }
    if (gap) Spacer(Modifier.height(6.dp))
}

@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}

/** Sembunyikan status/navigation bar saat kontrol disembunyikan; kembalikan saat keluar dari pembaca. */
@Composable
private fun ImmersiveMode(hidden: Boolean) {
    val view = LocalView.current
    DisposableEffect(hidden) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (hidden) controller?.hide(WindowInsetsCompat.Type.systemBars())
        else controller?.show(WindowInsetsCompat.Type.systemBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }
}
