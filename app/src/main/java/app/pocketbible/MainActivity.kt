package app.pocketbible

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.collectAsState
import app.pocketbible.ui.MainViewModel
import app.pocketbible.ui.bible.BibleBookListScreen
import app.pocketbible.ui.bible.BibleChapterListScreen
import app.pocketbible.ui.bible.BibleReaderScreen
import app.pocketbible.ui.home.HomeScreen
import app.pocketbible.ui.saved.SavedScreen
import app.pocketbible.ui.theme.PocketBibleTheme
import app.pocketbible.ui.verse.VerseScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = (application as PocketBibleApp).repository
        setContent {
            PocketBibleTheme {
                val viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory(repo))
                AppScaffold(viewModel)
            }
        }
    }
}

@Composable
private fun AppScaffold(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val bibleRoutes = setOf("bible", "bible_chapters", "bible_reader")

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == "home" || currentRoute == "verse" || currentRoute == null,
                    onClick = { navController.navigate("home") { launchSingleTop = true } },
                    icon = {
                        Icon(
                            if (currentRoute == "verse") Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null
                        )
                    },
                    label = { Text("Feelings") }
                )
                NavigationBarItem(
                    selected = currentRoute in bibleRoutes,
                    onClick = { navController.navigate("bible") { launchSingleTop = true } },
                    icon = {
                        Icon(
                            if (currentRoute in bibleRoutes) Icons.Filled.MenuBook else Icons.Outlined.MenuBook,
                            contentDescription = null
                        )
                    },
                    label = { Text("Read") }
                )
                NavigationBarItem(
                    selected = currentRoute == "saved",
                    onClick = { navController.navigate("saved") { launchSingleTop = true } },
                    icon = {
                        Icon(
                            if (currentRoute == "saved") Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = null
                        )
                    },
                    label = { Text("Saved") }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") {
                val feelings by viewModel.feelings.collectAsState()
                val searchQuery by viewModel.searchQuery.collectAsState()
                val searchResults by viewModel.searchResults.collectAsState()
                val verseOfDay by viewModel.verseOfDay.collectAsState()
                HomeScreen(
                    feelings = feelings,
                    searchQuery = searchQuery,
                    searchResults = searchResults,
                    verseOfDay = verseOfDay,
                    onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                    onFeelingSelected = {
                        viewModel.selectFeeling(it)
                        navController.navigate("verse")
                    }
                )
            }
            composable("verse") {
                val entries by viewModel.feelingEntries.collectAsState()
                val index by viewModel.currentIndex.collectAsState()
                val feeling by viewModel.selectedFeeling.collectAsState()
                val extra by viewModel.extraPassages.collectAsState()
                VerseScreen(
                    feelingLabel = feeling?.label ?: "",
                    entry = entries.getOrNull(index),
                    extraPassages = extra,
                    onAnother = { viewModel.another() },
                    onToggleSave = { viewModel.toggleSaveCurrent() }
                )
            }
            composable("saved") {
                val saved by viewModel.saved.collectAsState()
                SavedScreen(saved = saved)
            }
            composable("bible") {
                val books by viewModel.readableBooks.collectAsState()
                val allBooks by viewModel.allBooks.collectAsState()
                BibleBookListScreen(
                    books = books,
                    allBooks = allBooks,
                    onBookSelected = {
                        viewModel.selectBook(it)
                        navController.navigate("bible_chapters")
                    },
                    onGoToReference = { book, chapter, verse -> viewModel.goToReference(book, chapter, verse) },
                    onReferenceFound = { navController.navigate("bible_reader") }
                )
            }
            composable("bible_chapters") {
                val book by viewModel.selectedBook.collectAsState()
                val chapters by viewModel.chapters.collectAsState()
                BibleChapterListScreen(
                    bookName = book?.displayName ?: "",
                    chapters = chapters,
                    onBack = { navController.popBackStack() },
                    onChapterSelected = {
                        viewModel.openChapter(it)
                        navController.navigate("bible_reader")
                    }
                )
            }
            composable("bible_reader") {
                val book by viewModel.selectedBook.collectAsState()
                val chapter by viewModel.currentChapter.collectAsState()
                val chapters by viewModel.chapters.collectAsState()
                val verses by viewModel.chapterVerses.collectAsState()
                val highlightVerse by viewModel.scrollToVerse.collectAsState()
                val index = chapters.indexOf(chapter)
                BibleReaderScreen(
                    bookName = book?.displayName ?: "",
                    chapter = chapter,
                    verses = verses,
                    highlightVerse = highlightVerse,
                    hasPrevious = index > 0,
                    hasNext = index in 0 until chapters.lastIndex,
                    onBack = { navController.popBackStack() },
                    onPrevious = { viewModel.previousChapter() },
                    onNext = { viewModel.nextChapter() }
                )
            }
        }
    }
}
