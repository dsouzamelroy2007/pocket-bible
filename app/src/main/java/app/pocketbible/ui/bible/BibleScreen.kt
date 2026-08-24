package app.pocketbible.ui.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import app.pocketbible.R
import app.pocketbible.data.Book
import app.pocketbible.data.ScriptureVerse

@Composable
fun BibleBookListScreen(
    books: List<Book>,
    allBooks: List<Book>,
    onBookSelected: (Book) -> Unit,
    onLoadChapters: suspend (Book) -> List<Int>,
    onLoadVerses: suspend (Book, Int) -> List<Int>,
    onGoToReference: suspend (Book, Int, Int?) -> String?,
    onReferenceFound: () -> Unit,
    modifier: Modifier = Modifier
) {
    val readableBookIds = remember(books) { books.map { it.id }.toSet() }

    LazyColumn(modifier.padding(horizontal = 20.dp)) {
        item {
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.read_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.read_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(14.dp))

            GoToReferenceCard(
                allBooks = allBooks,
                readableBookIds = readableBookIds,
                onLoadChapters = onLoadChapters,
                onLoadVerses = onLoadVerses,
                onSubmit = onGoToReference,
                onFound = onReferenceFound
            )

            Spacer(Modifier.height(20.dp))
        }

        if (books.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.read_empty_browse),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        } else {
            item {
                Text(stringResource(R.string.read_browse), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
            }
            items(books, key = { it.id }) { book ->
                Column {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onBookSelected(book) }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(localizedBookName(book), style = MaterialTheme.typography.bodyLarge)
                    }
                    HorizontalDivider()
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoToReferenceCard(
    allBooks: List<Book>,
    readableBookIds: Set<String>,
    onLoadChapters: suspend (Book) -> List<Int>,
    onLoadVerses: suspend (Book, Int) -> List<Int>,
    onSubmit: suspend (Book, Int, Int?) -> String?,
    onFound: () -> Unit
) {
    // Loaded books float to the top so it's obvious, before you even pick one,
    // which of the 73 actually have text behind them yet.
    val orderedBooks = remember(allBooks, readableBookIds) {
        allBooks.sortedBy { if (it.id in readableBookIds) 0 else 1 }
    }
    var expandedBook by remember { mutableStateOf(false) }
    var selectedBook by remember(orderedBooks) { mutableStateOf(orderedBooks.firstOrNull()) }

    var expandedChapter by remember { mutableStateOf(false) }
    var availableChapters by remember { mutableStateOf<List<Int>>(emptyList()) }
    var selectedChapter by remember { mutableStateOf<Int?>(null) }

    var expandedVerse by remember { mutableStateOf(false) }
    var availableVerses by remember { mutableStateOf<List<Int>>(emptyList()) }
    var selectedVerse by remember { mutableStateOf<Int?>(null) }

    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedBook) {
        val book = selectedBook
        availableChapters = if (book != null) onLoadChapters(book) else emptyList()
        selectedChapter = availableChapters.firstOrNull()
    }
    LaunchedEffect(selectedBook, selectedChapter) {
        val book = selectedBook
        val chapter = selectedChapter
        availableVerses = if (book != null && chapter != null) onLoadVerses(book, chapter) else emptyList()
        selectedVerse = null
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.read_go_to_verse), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))

            ExposedDropdownMenuBox(expanded = expandedBook, onExpandedChange = { expandedBook = it }) {
                TextField(
                    value = selectedBook?.let { localizedBookName(it) } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.read_book_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBook) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedBook,
                    onDismissRequest = { expandedBook = false }
                ) {
                    orderedBooks.forEach { book ->
                        val loaded = book.id in readableBookIds
                        DropdownMenuItem(
                            text = {
                                Text(
                                    localizedBookName(book),
                                    color = if (loaded) Color.Unspecified else MaterialTheme.colorScheme.secondary
                                )
                            },
                            onClick = {
                                selectedBook = book
                                expandedBook = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expandedChapter && availableChapters.isNotEmpty(),
                    onExpandedChange = { expandedChapter = it },
                    modifier = Modifier.weight(1f)
                ) {
                    TextField(
                        value = selectedChapter?.toString() ?: "",
                        onValueChange = {},
                        readOnly = true,
                        enabled = availableChapters.isNotEmpty(),
                        label = { Text(stringResource(R.string.read_chapter_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedChapter) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedChapter && availableChapters.isNotEmpty(),
                        onDismissRequest = { expandedChapter = false }
                    ) {
                        availableChapters.forEach { chapter ->
                            DropdownMenuItem(
                                text = { Text(chapter.toString()) },
                                onClick = {
                                    selectedChapter = chapter
                                    expandedChapter = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = expandedVerse && availableVerses.isNotEmpty(),
                    onExpandedChange = { expandedVerse = it },
                    modifier = Modifier.weight(1f)
                ) {
                    val wholeChapterLabel = stringResource(R.string.read_whole_chapter)
                    TextField(
                        value = selectedVerse?.toString() ?: wholeChapterLabel,
                        onValueChange = {},
                        readOnly = true,
                        enabled = availableVerses.isNotEmpty(),
                        label = { Text(stringResource(R.string.read_verse_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVerse) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedVerse && availableVerses.isNotEmpty(),
                        onDismissRequest = { expandedVerse = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(wholeChapterLabel) },
                            onClick = {
                                selectedVerse = null
                                expandedVerse = false
                            }
                        )
                        availableVerses.forEach { verse ->
                            DropdownMenuItem(
                                text = { Text(verse.toString()) },
                                onClick = {
                                    selectedVerse = verse
                                    expandedVerse = false
                                }
                            )
                        }
                    }
                }
            }

            if (selectedBook != null && availableChapters.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.read_book_not_loaded, selectedBook?.let { localizedBookName(it) } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            val pickBookChapterError = stringResource(R.string.read_pick_book_chapter)
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    val book = selectedBook
                    val chapter = selectedChapter
                    if (book == null || chapter == null) {
                        error = pickBookChapterError
                        return@Button
                    }
                    error = null
                    scope.launch {
                        val result = onSubmit(book, chapter, selectedVerse)
                        if (result == null) onFound() else error = result
                    }
                },
                enabled = selectedBook != null && selectedChapter != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.read_go))
            }

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleReaderScreen(
    bookName: String,
    chapter: Int?,
    chapters: List<Int>,
    verses: List<ScriptureVerse>,
    highlightVerse: Int?,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onBack: () -> Unit,
    onChapterSelected: (Int) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var chapterMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(highlightVerse, verses) {
        if (highlightVerse != null) {
            val index = verses.indexOfFirst { it.verse == highlightVerse }
            if (index >= 0) listState.animateScrollToItem(index)
        }
    }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.padding(horizontal = 20.dp).padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.read_back))
            }
            Text(bookName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(6.dp))
            Box {
                Row(
                    Modifier
                        .clickable(enabled = chapters.isNotEmpty()) { chapterMenuExpanded = true }
                        .padding(vertical = 4.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${chapter ?: ""}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (chapters.isNotEmpty()) {
                        Icon(
                            Icons.Filled.ArrowDropDown,
                            contentDescription = stringResource(R.string.read_jump_to_chapter),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                DropdownMenu(expanded = chapterMenuExpanded, onDismissRequest = { chapterMenuExpanded = false }) {
                    chapters.forEach { c ->
                        DropdownMenuItem(
                            text = { Text(c.toString()) },
                            onClick = {
                                chapterMenuExpanded = false
                                onChapterSelected(c)
                            }
                        )
                    }
                }
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp)
        ) {
            items(verses, key = { it.id }) { v ->
                val isHighlighted = v.verse == highlightVerse
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            if (isHighlighted) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                            RoundedCornerShape(6.dp)
                        )
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                ) {
                    Text(
                        "${v.verse} ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(v.text, style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(Modifier.height(4.dp))
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(onClick = onPrevious, enabled = hasPrevious, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.read_previous))
            }
            OutlinedButton(onClick = onNext, enabled = hasNext, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.read_next))
            }
        }
    }
}
