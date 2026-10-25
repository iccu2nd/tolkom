package com.tinta.reader.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.tinta.reader.domain.model.Comic
import com.tinta.reader.ui.theme.Dimens
import com.tinta.reader.ui.theme.Ink
import com.tinta.reader.util.Strings
import com.tinta.reader.util.displayLatest
import com.tinta.reader.util.joinGenres
import java.util.Locale

@Composable
fun SkeletonBox(modifier: Modifier = Modifier, radius: Dp = Dimens.CoverRadius) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val a by transition.animateFloat(
        initialValue = 0.45f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a",
    )
    Box(modifier.alpha(a).clip(RoundedCornerShape(radius)).background(Ink.Surface))
}

/** Sampul portrait. Bila gambar gagal/kosong: panel netral dengan judul, bukan ikon rusak. */
@Composable
fun Cover(url: String?, title: String, modifier: Modifier = Modifier, radius: Dp = Dimens.CoverRadius) {
    Box(modifier.clip(RoundedCornerShape(radius)).background(Ink.Surface)) {
        if (url.isNullOrBlank()) {
            CoverFallback(title)
        } else {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { SkeletonBox(Modifier.fillMaxSize(), 0.dp) },
                error = { CoverFallback(title) },
            )
        }
    }
}

@Composable
private fun CoverFallback(title: String) {
    Box(Modifier.fillMaxSize().background(Ink.Elevated).padding(10.dp), contentAlignment = Alignment.Center) {
        Text(
            title, style = MaterialTheme.typography.labelMedium, color = Ink.TextSecondary,
            textAlign = TextAlign.Center, maxLines = 4, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ComicCard(comic: Comic, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(Dimens.CoverRadius))
            .clickable(onClickLabel = comic.title, role = Role.Button, onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(Dimens.CoverRatio)) {
            Cover(comic.cover, comic.title, Modifier.fillMaxSize())
            displayLatest(comic.latestChapter)?.let { LatestBadge(it, Modifier.align(Alignment.BottomStart).padding(6.dp)) }
            comic.rating?.let { RatingChip(it, Modifier.align(Alignment.TopEnd).padding(6.dp)) }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            comic.title, style = MaterialTheme.typography.titleSmall, color = Ink.TextPrimary,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.defaultMinSize(minHeight = 36.dp),
        )
        (comic.author ?: comic.genres.joinGenres(2))?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = Ink.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun LatestBadge(text: String, modifier: Modifier = Modifier) {
    Text(
        text, modifier = modifier.clip(RoundedCornerShape(6.dp)).background(Ink.Scrim.copy(alpha = 0.72f)).padding(horizontal = 7.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelSmall, color = Ink.TextPrimary, maxLines = 1,
    )
}

@Composable
fun RatingChip(value: Double, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(6.dp)).background(Ink.Scrim.copy(alpha = 0.72f)).padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Star, null, tint = Color(0xFFE5B53C), modifier = Modifier.size(11.dp))
        Spacer(Modifier.width(3.dp))
        Text(String.format(Locale.US, "%.1f", value), style = MaterialTheme.typography.labelSmall, color = Ink.TextPrimary)
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, onViewAll: (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = Ink.TextPrimary)
        if (onViewAll != null) {
            TextButton(onClick = onViewAll, modifier = Modifier.defaultMinSize(minHeight = Dimens.MinTouch)) {
                Text(Strings.viewAll, color = Ink.TextSecondary, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(Dimens.ScreenPadding * 2),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = Ink.TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        PrimaryButton(Strings.retry, onRetry, icon = true)
    }
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Column(
        modifier.fillMaxWidth().padding(Dimens.ScreenPadding * 2),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Ink.TextPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = Ink.TextMuted, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            PrimaryButton(actionLabel, onAction)
        }
    }
}

@Composable
fun PrimaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: Boolean = false, light: Boolean = false) {
    val bg = if (light) Ink.TextPrimary else Ink.Accent
    val fg = if (light) Ink.Background else Ink.AccentOn
    Row(
        modifier
            .defaultMinSize(minHeight = Dimens.MinTouch)
            .clip(RoundedCornerShape(Dimens.ButtonRadius))
            .background(bg)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (icon) { Icon(Icons.Filled.Refresh, null, tint = fg, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(label, color = fg, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecondaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .defaultMinSize(minHeight = Dimens.MinTouch)
            .clip(RoundedCornerShape(Dimens.ButtonRadius))
            .background(Ink.Elevated)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        Text(label, color = Ink.TextPrimary, style = MaterialTheme.typography.labelLarge)
    }
}

/** Logo "Tinta": dua panel miring (merah + putih) + wordmark. Digambar di Canvas agar tajam di semua ukuran. */
@Composable
fun TintaLogo(modifier: Modifier = Modifier) {
    Row(modifier.semantics { contentDescription = Strings.appName }, verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(width = 26.dp, height = 22.dp)) {
            val w = size.width; val h = size.height
            fun panel(x0: Float, color: Color) {
                val p = Path().apply {
                    moveTo(x0 + w * 0.14f, 0f); lineTo(x0 + w * 0.50f, 0f)
                    lineTo(x0 + w * 0.36f, h); lineTo(x0, h); close()
                }
                drawPath(p, color)
            }
            panel(0f, Ink.Accent)
            panel(w * 0.46f, Ink.TextPrimary)
        }
        Spacer(Modifier.width(8.dp))
        Text(Strings.appName, color = Ink.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
    }
}

@Composable
fun FullScreenLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        SkeletonBox(Modifier.size(width = 120.dp, height = 8.dp), 4.dp)
    }
}

@Composable
fun GridSkeleton(columns: Int, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = Dimens.ScreenPadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(columns) {
                    Column(Modifier.weight(1f)) {
                        SkeletonBox(Modifier.fillMaxWidth().aspectRatio(Dimens.CoverRatio))
                        Spacer(Modifier.height(8.dp))
                        SkeletonBox(Modifier.fillMaxWidth(0.8f).height(12.dp), 4.dp)
                    }
                }
            }
        }
    }
}
