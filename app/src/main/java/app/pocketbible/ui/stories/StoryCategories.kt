package app.pocketbible.ui.stories

import app.pocketbible.R

/** Book-group codes in display order, mapped to their header string resource -- same pattern as characters.CATEGORY_ORDER. Shared between [StoriesScreen] (filter chips/section headers) and [StoryDetailScreen] (the badge row). */
internal val BOOK_GROUP_ORDER: List<Pair<String, Int>> = listOf(
    "pentateuch" to R.string.story_book_group_pentateuch,
    "historical" to R.string.story_book_group_historical,
    "wisdom" to R.string.story_book_group_wisdom,
    "prophets" to R.string.story_book_group_prophets,
    "deuterocanonical" to R.string.story_book_group_deuterocanonical,
    "infancy" to R.string.story_book_group_infancy,
    "ministry_miracles" to R.string.story_book_group_ministry_miracles,
    "parables" to R.string.story_book_group_parables,
    "teachings_encounters" to R.string.story_book_group_teachings_encounters,
    "passion_resurrection" to R.string.story_book_group_passion_resurrection,
    "acts" to R.string.story_book_group_acts,
    "revelation" to R.string.story_book_group_revelation
)

internal val STORY_TYPE_ORDER: List<Pair<String, Int>> = listOf(
    "narrative" to R.string.story_type_narrative,
    "parable" to R.string.story_type_parable,
    "miracle" to R.string.story_type_miracle
)

internal fun storyTypeLabelRes(storyType: String): Int =
    STORY_TYPE_ORDER.firstOrNull { it.first == storyType }?.second ?: R.string.story_type_narrative

internal fun bookGroupLabelRes(bookGroup: String): Int =
    BOOK_GROUP_ORDER.firstOrNull { it.first == bookGroup }?.second ?: R.string.story_book_group_historical
