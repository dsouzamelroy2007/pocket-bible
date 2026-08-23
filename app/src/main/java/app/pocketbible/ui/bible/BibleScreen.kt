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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.launch
import app.pocketbible.data.Book
import app.pocketbible.data.ScriptureVerse

@Composable
fun BibleBookListScreen(
    books: List<Book>,
    allBooks: List<Book>,
    onBookSelected: (Book) -> Unit,
    onGoToReference: suspend (Book, Int, Int?) -> String?,
    onReferenceFound: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("Read", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Jump straight to a reference, or browse what's loaded below.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(14.dp))

        GoToReferenceCard(
            allBooks = allBooks,
            onSubmit = onGoToReference,
            onFound = onReferenceFound
        )

        Spacer(Modifier.height(20.dp))

        if (books.isEmpty()) {
            Text(
                "No chapters browsable yet below — this prototype ships a couple of " +
                    "sample psalms. Full text comes from a bulk import, not from this screen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        } else {
            Text("Browse", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            books.forEach { book ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onBookSelected(book) }
                        .padding(vertical = 12.dp)
                ) {
                    Text(book.displayName, style = MaterialTheme.typography.bodyLarge)
                }
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoToReferenceCard(
    allBooks: List<Book>,
    onSubmit: suspend (Book, Int, Int?) -> String?,
    onFound: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedBook by remember(allBooks) { mutableStateOf(allBooks.firstOrNull()) }
    var chapterText by remember { mutableStateOf("") }
    var verseText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Go to a verse", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                TextField(
                    value = selectedBook?.displayName ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Book") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                androidx.compose.material3.ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    allBooks.forEach { book ->
                        DropdownMenuItem(
                            text = { Text(book.displayName) },
                            onClick = {
                                selectedBook = book
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = chapterText,
                    onValueChange = { chapterText = it.filter { c -> c.isDigit() } },
                    label = { Text("Chapter") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = verseText,
                    onValueChange = { verseText = it.filter { c -> c.isDigit() } },
                    label = { Text("Verse (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    val book = selectedBook
                    val chapter = chapterText.toIntOrNull()
                    if (book == null || chapter == null) {
                        error = "Pick a book and enter a chapter number."
                        return@Button
                    }
                    val verse = verseText.toIntOrNull()
                    error = null
                    scope.launch {
                        val result = onSubmit(book, chapter, verse)
                        if (result == null) onFound() else error = result
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Go")
            }

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun BibleChapterListScreen(
    bookName: String,
    chapters: List<Int>,
    onBack: () -> Unit,
    onChapterSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(bookName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chapters) { chapter ->
                Card(onClick = { onChapterSelected(chapter) }) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                        Text(chapter.toString(), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun BibleReaderScreen(
    bookName: String,
    chapter: Int?,
    verses: List<ScriptureVerse>,
    highlightVerse: Int?,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onBack: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

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
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back to chapters")
            }
            Text(
                "$bookName ${chapter ?: ""}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )
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
                Text("Previous")
            }
            OutlinedButton(onClick = onNext, enabled = hasNext, modifier = Modifier.weight(1f)) {
                Text("Next")
            }
        }
    }
}
