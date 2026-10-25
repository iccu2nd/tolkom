package com.tinta.reader

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

/** Alur dasar: bottom navigation berpindah antar tujuan. Tidak bergantung pada data server. */
class NavigationSmokeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun bottomBarSwitchesDestinations() {
        rule.onNodeWithText("Jelajah").performClick()
        rule.onNodeWithText("Cari judul manhwa, manga, manhua").assertIsDisplayed()

        rule.onNodeWithText("Pustaka").performClick()
        rule.onNodeWithText("Tersimpan").assertIsDisplayed()

        rule.onNodeWithText("Profil").performClick()
        rule.onNodeWithText("Pengaturan pembaca").assertIsDisplayed()

        rule.onNodeWithText("Beranda").performClick()
    }
}
