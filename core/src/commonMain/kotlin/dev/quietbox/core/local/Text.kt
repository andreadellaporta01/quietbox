package dev.quietbox.core.local

object Text {
    private val WORD = Regex("[\\p{L}\\p{N}]+")
    private val STOPWORDS = setOf(
        "the", "a", "an", "and", "or", "to", "of", "in", "on", "for", "is", "it", "you", "your", "we", "our",
        "i", "me", "my", "this", "that", "with", "at", "be", "are", "was", "will", "can", "hi", "hello", "thanks",
        "der", "die", "das", "und", "ist", "zu", "ich", "sie", "wir", "re", "fwd",
    )

    fun tokens(text: String): List<String> =
        WORD.findAll(text.lowercase()).map { it.value }.filter { it.length > 2 && it !in STOPWORDS }.toList()

    fun estimateTokens(text: String): Int = (text.length + 3) / 4

    fun containsAny(text: String, words: Collection<String>): Boolean {
        val lower = text.lowercase()
        return words.any { lower.contains(it) }
    }

    fun normalize(text: String): String = text.lowercase().replace(Regex("[^\\p{L}\\p{N}.,:/€$-]+"), " ").trim()
}
