package com.tinta.reader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tinta.reader.ui.theme.Dimens
import com.tinta.reader.ui.theme.Ink
import com.tinta.reader.util.Strings

@Composable
fun HomeTopBar(onSearch: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().background(Ink.Background).statusBarsPadding()
            .height(Dimens.TopBarHeight).padding(start = Dimens.ScreenPadding, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TintaLogo()
        // Ikon notifikasi sengaja tidak ada: server tidak punya endpoint notifikasi.
        IconButton(onClick = onSearch, modifier = Modifier.size(Dimens.MinTouch)) {
            Icon(Icons.Filled.Search, contentDescription = Strings.search, tint = Ink.TextPrimary)
        }
    }
}

@Composable
fun ScreenTopBar(title: String, onBack: (() -> Unit)?, modifier: Modifier = Modifier, actions: @Composable () -> Unit = {}) {
    Row(
        modifier.fillMaxWidth().background(Ink.Background).statusBarsPadding()
            .height(56.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(Dimens.MinTouch)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Strings.back, tint = Ink.TextPrimary)
            }
        } else {
            Box(Modifier.size(12.dp))
        }
        Text(
            title, style = MaterialTheme.typography.titleLarge, color = Ink.TextPrimary,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
        )
        actions()
    }
}

enum class TopDestination(val route: String, val label: String) {
    Home("home", Strings.navHome),
    Explore("explore", Strings.navExplore),
    Library("library", Strings.navLibrary),
    Profile("profile", Strings.navProfile);

    val icon: ImageVector
        get() = when (this) {
            Home -> Icons.Filled.Home
            Explore -> TintaIcons.Explore
            Library -> TintaIcons.Library
            Profile -> Icons.Filled.Person
        }
}

@Composable
fun TintaBottomBar(current: String?, onSelect: (TopDestination) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(Ink.Background)) {
        HorizontalDivider(color = Ink.Border, thickness = 1.dp)
        Row(Modifier.fillMaxWidth().navigationBarsPadding().height(64.dp)) {
            TopDestination.entries.forEach { dest ->
                val selected = dest.route == current
                val tint = if (selected) Ink.TextPrimary else Ink.TextMuted
                Column(
                    Modifier
                        .weight(1f)
                        .height(64.dp)
                        .semantics { this.selected = selected }
                        .clickable(role = Role.Tab) { onSelect(dest) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        Modifier.height(3.dp).size(width = 20.dp, height = 3.dp)
                            .background(if (selected) Ink.Accent else Ink.Background),
                    )
                    Box(Modifier.height(6.dp))
                    Icon(dest.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                    Text(dest.label, style = MaterialTheme.typography.labelSmall, color = tint)
                }
            }
        }
    }
}
