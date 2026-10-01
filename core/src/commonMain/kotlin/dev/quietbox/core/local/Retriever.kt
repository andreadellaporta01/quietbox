package dev.quietbox.core.local

import dev.quietbox.core.inbox.Message

class Retriever(private val corpus: List<Message>) {

    fun related(to: Message, k: Int = 2, tokenBudget: Int = 300): List<Message> {
        val query = Text.tokens(to.fullText).toSet()
        if (query.isEmpty()) return emptyList()
        val ranked = corpus.asSequence()
            .filter { it.id != to.id }
            .map { candidate -> candidate to score(query, to, candidate) }
            .filter { it.second > 0.15 }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(k)
            .toList()
        var spent = 0
        return ranked.takeWhile { spent += Text.estimateTokens(it.fullText); spent <= tokenBudget }
    }

    private fun score(query: Set<String>, source: Message, candidate: Message): Double {
        val terms = Text.tokens(candidate.fullText).toSet()
        val overlap = query.intersect(terms).size.toDouble() / (query.size + 1)
        val sameSender = if (candidate.senderAddress == source.senderAddress) 0.3 else 0.0
        return overlap + sameSender
    }
}
