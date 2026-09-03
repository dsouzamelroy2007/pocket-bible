package app.pocketbible

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.padding
import androidx.core.os.LocaleListCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import app.pocketbible.R
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.collectAsState
import app.pocketbible.ui.MainViewModel
import app.pocketbible.ui.about.AboutScreen
import app.pocketbible.ui.bible.BibleBookListScreen
import app.pocketbible.ui.bible.BibleReaderScreen
import app.pocketbible.ui.bible.localizedBookName
import app.pocketbible.ui.characters.CharacterDetailScreen
import app.pocketbible.ui.characters.CharactersScreen
import app.pocketbible.ui.home.HomeScreen
import app.pocketbible.ui.reading.DailyReadingScreen
import app.pocketbible.ui.saved.SavedScreen
import app.pocketbible.ui.theme.PocketBibleTheme
import app.pocketbible.ui.verse.VerseScreen
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = (application as PocketBibleApp).repository
        setContent {
            PocketBibleTheme {
                val viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory(repo))
                AppScaffold(
                    viewModel = viewModel,
                    onLanguageSelected = { tag ->
                        val current = AppCompatDelegate.getApplicationLocales()
                        val alreadySelected = if (tag == null) current.isEmpty else current[0]?.language == tag
                        if (!alreadySelected) {
                            // Shown in the language that's about to be replaced, since the
                            // switch (and recreate()) hasn't happened yet at this point.
                            Toast.makeText(this, getString(R.string.language_restarting), Toast.LENGTH_SHORT).show()
                            val locales = if (tag == null) {
                                LocaleListCompat.getEmptyLocaleList()
                            } else {
                                LocaleListCompat.forLanguageTags(tag)
                            }
                            AppCompatDelegate.setApplicationLocales(locales)
                            recreate()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun NavLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        textAlign = TextAlign.Center,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun AppScaffold(viewModel: MainViewModel, onLanguageSelected: (String?) -> Unit) {
    // Covers the case where this ViewModel instance survived the recreate()
    // a language switch triggers, so its topics/saved Flows would otherwise
    // stay pinned to whatever language was current when they were first
    // collected. Cheap no-op once the language is already current.
    viewModel.ensureFreshForCurrentLanguage()

    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val bibleRoutes = setOf("bible", "bible_reader")
    val characterRoutes = setOf("characters", "character_detail")

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
                    label = { NavLabel(stringResource(R.string.nav_topics)) }
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
                    label = { NavLabel(stringResource(R.string.nav_read)) }
                )
                NavigationBarItem(
                    selected = currentRoute in characterRoutes,
                    onClick = { navController.navigate("characters") { launchSingleTop = true } },
                    icon = {
                        Icon(
                            if (currentRoute in characterRoutes) Icons.Filled.People else Icons.Outlined.People,
                            contentDescription = null
                        )
                    },
                    label = { NavLabel(stringResource(R.string.nav_characters)) }
                )
                NavigationBarItem(
                    selected = currentRoute == "daily",
                    onClick = { navController.navigate("daily") { launchSingleTop = true } },
                    icon = {
                        Icon(
                            if (currentRoute == "daily") Icons.Filled.CalendarToday else Icons.Outlined.CalendarToday,
                            contentDescription = null
                        )
                    },
                    label = { NavLabel(stringResource(R.string.nav_daily)) }
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
                HomeScreen(
                    feelings = feelings,
                    searchQuery = searchQuery,
                    searchResults = searchResults,
                    onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                    onFeelingSelected = {
                        viewModel.selectFeeling(it)
                        navController.navigate("verse")
                    },
                    onLanguageSelected = onLanguageSelected,
                    onSavedClicked = { navController.navigate("saved") },
                    onAboutClicked = { navController.navigate("about") }
                )
            }
            composable("daily") {
                val verseOfDay by viewModel.verseOfDay.collectAsState()
                val dailyReading by viewModel.dailyReading.collectAsState()
                val resolvedReadings by viewModel.resolvedReadings.collectAsState()
                DailyReadingScreen(
                    verseOfDay = verseOfDay,
                    dailyReading = dailyReading,
                    readings = resolvedReadings
                )
            }
            composable("about") {
                val translations by viewModel.translations.collectAsState()
                AboutScreen(translations = translations)
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
            composable("characters") {
                val characters by viewModel.characters.collectAsState()
                CharactersScreen(
                    characters = characters,
                    onCharacterSelected = {
                        viewModel.selectCharacter(it)
                        navController.navigate("character_detail")
                    }
                )
            }
            composable("character_detail") {
                val character by viewModel.selectedCharacter.collectAsState()
                val verses by viewModel.characterVerses.collectAsState()
                CharacterDetailScreen(
                    character = character,
                    verses = verses,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("bible") {
                val books by viewModel.readableBooks.collectAsState()
                val bookmarks by viewModel.bookmarks.collectAsState()
                val scope = rememberCoroutineScope()
                BibleBookListScreen(
                    books = books,
                    bookmarks = bookmarks,
                    onBookSelected = { book ->
                        scope.launch {
                            viewModel.openBook(book)
                            navController.navigate("bible_reader")
                        }
                    },
                    onLoadChapters = { book -> viewModel.chaptersAvailable(book) },
                    onLoadVerses = { book, chapter -> viewModel.versesAvailable(book, chapter) },
                    onGoToReference = { book, chapter, verse -> viewModel.goToReference(book, chapter, verse) },
                    onReferenceFound = { navController.navigate("bible_reader") },
                    onBookmarkSelected = { bookmark ->
                        scope.launch {
                            if (viewModel.openBookmark(bookmark)) navController.navigate("bible_reader")
                        }
                    },
                    onBookmarkDeleted = { viewModel.deleteBookmark(it.id) }
                )
            }
            composable("bible_reader") {
                val book by viewModel.selectedBook.collectAsState()
                val chapter by viewModel.currentChapter.collectAsState()
                val chapters by viewModel.chapters.collectAsState()
                val verses by viewModel.chapterVerses.collectAsState()
                val highlightVerse by viewModel.scrollToVerse.collectAsState()
                val bookmarks by viewModel.bookmarks.collectAsState()
                val index = chapters.indexOf(chapter)
                val currentBook = book
                val isBookmarked = currentBook != null && chapter != null && bookmarks.any {
                    it.bookId == currentBook.id && it.chapter == chapter && it.verse == highlightVerse
                }
                BibleReaderScreen(
                    bookName = book?.let { localizedBookName(it) } ?: "",
                    chapter = chapter,
                    chapters = chapters,
                    verses = verses,
                    highlightVerse = highlightVerse,
                    hasPrevious = index > 0,
                    hasNext = index in 0 until chapters.lastIndex,
                    isBookmarked = isBookmarked,
                    onBack = { navController.popBackStack() },
                    onChapterSelected = { viewModel.openChapter(it) },
                    onPrevious = { viewModel.previousChapter() },
                    onNext = { viewModel.nextChapter() },
                    onToggleBookmark = { viewModel.toggleBookmarkCurrent() }
                )
            }
        }
    }
}
