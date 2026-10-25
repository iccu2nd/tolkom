package com.tinta.reader.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tinta.reader.domain.model.HistoryItem
import com.tinta.reader.ui.AppViewModelFactory
import com.tinta.reader.ui.ExploreViewModel
import com.tinta.reader.ui.HomeViewModel
import com.tinta.reader.ui.LibraryViewModel
import com.tinta.reader.ui.ProfileViewModel
import com.tinta.reader.ui.ReaderViewModel
import com.tinta.reader.ui.SearchViewModel
import com.tinta.reader.ui.DetailViewModel
import com.tinta.reader.ui.ViewAllViewModel
import com.tinta.reader.ui.components.TintaBottomBar
import com.tinta.reader.ui.components.TopDestination
import com.tinta.reader.ui.screens.detail.DetailScreen
import com.tinta.reader.ui.screens.explore.ExploreScreen
import com.tinta.reader.ui.screens.home.HomeScreen
import com.tinta.reader.ui.screens.library.LibraryScreen
import com.tinta.reader.ui.screens.profile.ProfileScreen
import com.tinta.reader.ui.screens.reader.ReaderScreen
import com.tinta.reader.ui.screens.search.SearchScreen
import com.tinta.reader.ui.screens.search.ViewAllScreen
import com.tinta.reader.ui.theme.Ink

/** ID sumber/komik/chapter dikirim ter-encode agar karakter seperti '/' atau ':' tidak merusak rute. */
object Routes {
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val LIBRARY = "library"
    const val PROFILE = "profile"
    const val SEARCH = "search"
    const val DETAIL = "detail/{source}/{id}"
    const val READER = "reader/{source}/{id}/{chapter}"
    const val ALL = "all/{kind}"

    fun detail(source: String, id: String) = "detail/${Uri.encode(source)}/${Uri.encode(id)}"
    fun reader(source: String, id: String, chapter: String) =
        "reader/${Uri.encode(source)}/${Uri.encode(id)}/${Uri.encode(chapter)}"
    fun all(kind: String) = "all/${Uri.encode(kind)}"
}

/** Abaikan tap beruntun: hanya navigasi bila layar saat ini sudah RESUMED. */
private fun NavController.go(route: String, builder: NavOptionsBuilder.() -> Unit = {}) {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) navigate(route, builder)
}

private fun NavController.goBack() {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) popBackStack()
}

@Composable
fun TintaRoot(navController: NavHostController = rememberNavController()) {
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = route in TopDestination.entries.map { it.route }

    val openComic: (com.tinta.reader.domain.model.Comic) -> Unit = { navController.go(Routes.detail(it.source, it.sourceId)) }
    val openReader: (String, String, String) -> Unit = { s, id, ch -> navController.go(Routes.reader(s, id, ch)) }
    val resume: (HistoryItem) -> Unit = { navController.go(Routes.reader(it.source, it.sourceId, it.chapterKey)) }

    Scaffold(
        containerColor = Ink.Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBar) {
                TintaBottomBar(current = route, onSelect = { dest ->
                    if (dest.route != route) {
                        navController.navigate(dest.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                })
            }
        },
    ) { inner ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(bottom = inner.calculateBottomPadding()),
        ) {
            composable(Routes.HOME) {
                val vm: HomeViewModel = viewModel(factory = AppViewModelFactory)
                HomeScreen(
                    vm = vm,
                    onComic = openComic,
                    onReadChapter = { c, key -> openReader(c.source, c.sourceId, key) },
                    onResume = resume,
                    onSearch = { navController.go(Routes.SEARCH) },
                    onViewAll = { navController.go(Routes.all(it)) },
                )
            }
            composable(Routes.EXPLORE) {
                val vm: ExploreViewModel = viewModel(factory = AppViewModelFactory)
                ExploreScreen(vm, onComic = openComic, onSearch = { navController.go(Routes.SEARCH) })
            }
            composable(Routes.LIBRARY) {
                val vm: LibraryViewModel = viewModel(factory = AppViewModelFactory)
                LibraryScreen(
                    vm = vm,
                    onBookmark = { navController.go(Routes.detail(it.source, it.sourceId)) },
                    onResume = resume,
                    onExplore = {
                        navController.navigate(Routes.EXPLORE) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(Routes.PROFILE) {
                val vm: ProfileViewModel = viewModel(factory = AppViewModelFactory)
                ProfileScreen(vm)
            }
            composable(Routes.SEARCH) {
                val vm: SearchViewModel = viewModel(factory = AppViewModelFactory)
                SearchScreen(vm, onBack = { navController.goBack() }, onComic = openComic)
            }
            composable(Routes.ALL) {
                val vm: ViewAllViewModel = viewModel(factory = AppViewModelFactory)
                ViewAllScreen(vm, onBack = { navController.goBack() }, onComic = openComic)
            }
            composable(Routes.DETAIL) {
                val vm: DetailViewModel = viewModel(factory = AppViewModelFactory)
                DetailScreen(vm, onBack = { navController.goBack() }, onReadChapter = openReader)
            }
            composable(Routes.READER) {
                val vm: ReaderViewModel = viewModel(factory = AppViewModelFactory)
                ReaderScreen(
                    vm = vm,
                    onBack = { navController.goBack() },
                    // Pindah chapter menggantikan layar pembaca saat ini agar back tidak menumpuk chapter.
                    onOpenChapter = { key ->
                        navController.go(Routes.reader(vm.source, vm.sourceId, key)) {
                            popUpTo(Routes.READER) { inclusive = true }
                        }
                    },
                )
            }
        }
    }
}
