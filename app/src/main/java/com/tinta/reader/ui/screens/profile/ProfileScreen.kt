package com.tinta.reader.ui.screens.profile

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tinta.reader.BuildConfig
import com.tinta.reader.ui.ProfileViewModel
import com.tinta.reader.ui.theme.Dimens
import com.tinta.reader.ui.theme.Ink
import com.tinta.reader.util.Strings

@Composable
fun ProfileScreen(vm: ProfileViewModel, modifier: Modifier = Modifier) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }

    Column(
        modifier.fillMaxSize().background(Ink.Background).statusBarsPadding()
            .verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
    ) {
        Text(
            Strings.profileTitle, style = MaterialTheme.typography.headlineMedium, color = Ink.TextPrimary,
            modifier = Modifier.padding(start = Dimens.ScreenPadding, top = 14.dp, end = Dimens.ScreenPadding, bottom = 8.dp),
        )
        Text(
            Strings.noAccount, style = MaterialTheme.typography.bodyMedium, color = Ink.TextMuted,
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = 4.dp),
        )

        Group(Strings.readerSettings) {
            SwitchRow(Strings.keepScreenOn, settings.keepScreenOn, vm::setKeepScreenOn)
            SwitchRow(Strings.pageGap, settings.pageGap, vm::setPageGap)
        }

        Group(Strings.dataSection) {
            ActionRow(Strings.clearSearches) { vm.clearSearches() }
            ActionRow(Strings.clearHistory, destructive = true) { confirmClear = true }
        }

        Group(Strings.about) {
            InfoRow(Strings.appName, "${Strings.version} ${BuildConfig.VERSION_NAME}")
            InfoRow(Strings.serverLabel, Uri.parse(BuildConfig.BASE_URL).host ?: BuildConfig.BASE_URL)
            Text(
                Strings.sourceNote, style = MaterialTheme.typography.bodySmall, color = Ink.TextMuted,
                modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = 8.dp),
            )
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = Ink.Elevated,
            title = { Text(Strings.clearHistoryTitle, color = Ink.TextPrimary) },
            text = { Text(Strings.clearHistoryBody, color = Ink.TextSecondary) },
            confirmButton = {
                TextButton(onClick = { vm.clearHistory(); confirmClear = false }) {
                    Text(Strings.clear, color = Ink.Accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(Strings.cancel, color = Ink.TextPrimary) }
            },
        )
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(top = Dimens.SectionGap)) {
        Text(
            title, style = MaterialTheme.typography.titleMedium, color = Ink.TextPrimary,
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = 4.dp),
        )
        content()
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp)
            .clickable(role = Role.Switch) { onChange(!checked) }
            .padding(horizontal = Dimens.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Ink.TextSecondary, modifier = Modifier.weight(1f))
        Switch(
            checked = checked, onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Ink.TextPrimary, checkedTrackColor = Ink.Accent,
                uncheckedThumbColor = Ink.TextSecondary, uncheckedTrackColor = Ink.Elevated, uncheckedBorderColor = Ink.Border,
            ),
        )
    }
}

@Composable
private fun ActionRow(label: String, destructive: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp).clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Dimens.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = if (destructive) Ink.Accent else Ink.TextSecondary)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).padding(horizontal = Dimens.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Ink.TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Ink.TextMuted)
    }
}
