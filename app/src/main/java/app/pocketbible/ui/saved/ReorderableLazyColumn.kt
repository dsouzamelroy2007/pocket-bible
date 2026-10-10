package app.pocketbible.ui.saved

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun <T> ReorderableLazyColumn(
    values: List<T>,
    key: (T) -> Any,
    onReorder: (List<T>) -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable (item: T, dragHandleModifier: Modifier) -> Unit
) {
    val listState = rememberLazyListState()
    val orderedItems = remember { mutableStateListOf<T>() }
    var draggingKey by remember { mutableStateOf<Any?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var dragDirection by remember { mutableFloatStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(values) {
        orderedItems.replaceWith(values)
    }

    LazyColumn(state = listState, modifier = modifier) {
        items(orderedItems, key = key) { item ->
            val itemKey = key(item)
            val isDragging = draggingKey == itemKey
            Box(
                Modifier
                    .fillMaxWidth()
                    .zIndex(if (isDragging) 1f else 0f)
                    .offset {
                        IntOffset(0, if (isDragging) dragOffset.roundToInt() else 0)
                    }
            ) {
                itemContent(
                    item,
                    Modifier.pointerInput(itemKey) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingKey = itemKey
                                dragOffset = 0f
                                dragDirection = 0f
                            },
                            onDragEnd = {
                                draggingKey = null
                                dragOffset = 0f
                                dragDirection = 0f
                                onReorder(orderedItems.toList())
                            },
                            onDragCancel = {
                                draggingKey = null
                                dragOffset = 0f
                                dragDirection = 0f
                                onReorder(orderedItems.toList())
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount.y
                                if (dragAmount.y != 0f) dragDirection = dragAmount.y
                                val visibleItems = listState.layoutInfo.visibleItemsInfo
                                val draggedItem = visibleItems.firstOrNull { it.key == itemKey } ?: return@detectDragGesturesAfterLongPress
                                val draggedCenter = draggedItem.offset + draggedItem.size / 2f + dragOffset
                                val fromIndex = orderedItems.indexOfFirst { key(it) == itemKey }
                                val targetItem = if (dragDirection > 0f) {
                                    visibleItems.lastOrNull {
                                        val index = orderedItems.indexOfFirst { item -> key(item) == it.key }
                                        index > fromIndex && draggedCenter > it.offset + it.size / 2f
                                    }
                                } else {
                                    visibleItems.firstOrNull {
                                        val index = orderedItems.indexOfFirst { item -> key(item) == it.key }
                                        index in 0 until fromIndex && draggedCenter < it.offset + it.size / 2f
                                    }
                                }
                                if (draggedCenter < listState.layoutInfo.viewportStartOffset + 56.dp.roundToPx()) {
                                    coroutineScope.launch { listState.scrollBy(-18f) }
                                } else if (draggedCenter > listState.layoutInfo.viewportEndOffset - 56.dp.roundToPx()) {
                                    coroutineScope.launch { listState.scrollBy(18f) }
                                }
                                if (targetItem != null) {
                                    val toIndex = orderedItems.indexOfFirst { key(it) == targetItem.key }
                                    if (fromIndex >= 0 && toIndex >= 0 && fromIndex != toIndex) {
                                        dragOffset -= targetItem.offset - draggedItem.offset
                                        val dragged = orderedItems.removeAt(fromIndex)
                                        orderedItems.add(toIndex, dragged)
                                    }
                                }
                            }
                        )
                    }
                )
            }
        }
    }
}

private fun <T> SnapshotStateList<T>.replaceWith(items: List<T>) {
    clear()
    addAll(items)
}
