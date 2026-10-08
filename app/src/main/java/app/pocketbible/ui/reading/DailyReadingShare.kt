package app.pocketbible.ui.reading

data class DailyReadingShareSection(
    val heading: String,
    val text: String,
    val citation: String,
    val refrain: String? = null
)

data class DailyReadingShareContent(
    val subject: String,
    val title: String,
    val verseHeading: String,
    val verseText: String,
    val verseCitation: String,
    val readingsHeading: String,
    val readings: List<DailyReadingShareSection>,
    val reflectionHeading: String,
    val reflection: String?,
    val unavailableText: String,
    val invitation: String,
    val downloadLabel: String,
    val downloadUrl: String
) {
    fun asPlainText(): String = buildString {
        appendLine("*$title*")
        appendLine()
        appendLine("*$verseHeading*")
        appendLine("_“${verseText.trim()}”_")
        appendLine("_${verseCitation}_")
        appendLine()
        appendLine("*$readingsHeading*")
        readings.forEach { reading ->
            appendLine()
            appendLine("*${reading.heading}*")
            reading.refrain?.let { refrain -> appendLine("_${refrain}_") }
            appendLine("_${reading.citation}_")
            appendLine(reading.text.ifBlank { unavailableText })
        }
        if (!reflection.isNullOrBlank()) {
            appendLine()
            appendLine("*$reflectionHeading*")
            appendLine("_${reflection.trim()}_")
        }
        appendLine()
        appendLine(invitation)
        appendLine("*$downloadLabel*")
        append(downloadUrl)
    }.trimEnd()
}
