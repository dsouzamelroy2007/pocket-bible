package app.pocketbible.ui.bible

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.pocketbible.R
import app.pocketbible.data.Book

/**
 * Book names are bundled data (seeded into Room from assets), not Android
 * string resources, so they don't automatically follow the in-app language
 * switcher the way button labels do. This table gives the 73 canon book
 * names -- just names, not scripture text -- a translated resource per
 * supported language, so [localizedBookName] can show one that matches
 * whatever language the user picked. Falls back to the bundled English
 * name for any book id not listed here.
 */
private val BOOK_NAME_RES: Map<String, Int> = mapOf(
    "gen" to R.string.book_gen,
    "ex" to R.string.book_ex,
    "lev" to R.string.book_lev,
    "num" to R.string.book_num,
    "deut" to R.string.book_deut,
    "josh" to R.string.book_josh,
    "judg" to R.string.book_judg,
    "ruth" to R.string.book_ruth,
    "1sam" to R.string.book_1sam,
    "2sam" to R.string.book_2sam,
    "1kgs" to R.string.book_1kgs,
    "2kgs" to R.string.book_2kgs,
    "1chr" to R.string.book_1chr,
    "2chr" to R.string.book_2chr,
    "ezra" to R.string.book_ezra,
    "neh" to R.string.book_neh,
    "tob" to R.string.book_tob,
    "jdt" to R.string.book_jdt,
    "esth" to R.string.book_esth,
    "1macc" to R.string.book_1macc,
    "2macc" to R.string.book_2macc,
    "job" to R.string.book_job,
    "ps" to R.string.book_ps,
    "pr" to R.string.book_pr,
    "eccl" to R.string.book_eccl,
    "song" to R.string.book_song,
    "wis" to R.string.book_wis,
    "sir" to R.string.book_sir,
    "isa" to R.string.book_isa,
    "jer" to R.string.book_jer,
    "lam" to R.string.book_lam,
    "bar" to R.string.book_bar,
    "ezek" to R.string.book_ezek,
    "dan" to R.string.book_dan,
    "hos" to R.string.book_hos,
    "joel" to R.string.book_joel,
    "amos" to R.string.book_amos,
    "obad" to R.string.book_obad,
    "jonah" to R.string.book_jonah,
    "mic" to R.string.book_mic,
    "nah" to R.string.book_nah,
    "hab" to R.string.book_hab,
    "zeph" to R.string.book_zeph,
    "hag" to R.string.book_hag,
    "zech" to R.string.book_zech,
    "mal" to R.string.book_mal,
    "mt" to R.string.book_mt,
    "mk" to R.string.book_mk,
    "lk" to R.string.book_lk,
    "jn" to R.string.book_jn,
    "acts" to R.string.book_acts,
    "ro" to R.string.book_ro,
    "1cor" to R.string.book_1cor,
    "2cor" to R.string.book_2cor,
    "gal" to R.string.book_gal,
    "eph" to R.string.book_eph,
    "phil" to R.string.book_phil,
    "col" to R.string.book_col,
    "1the" to R.string.book_1the,
    "2the" to R.string.book_2the,
    "1tim" to R.string.book_1tim,
    "2tim" to R.string.book_2tim,
    "titus" to R.string.book_titus,
    "phlm" to R.string.book_phlm,
    "heb" to R.string.book_heb,
    "jas" to R.string.book_jas,
    "1pet" to R.string.book_1pet,
    "2pet" to R.string.book_2pet,
    "1jo" to R.string.book_1jo,
    "2jo" to R.string.book_2jo,
    "3jo" to R.string.book_3jo,
    "jude" to R.string.book_jude,
    "rev" to R.string.book_rev,
)

@Composable
fun localizedBookName(book: Book): String {
    val resId = BOOK_NAME_RES[book.id]
    return if (resId != null) stringResource(resId) else book.displayName
}
