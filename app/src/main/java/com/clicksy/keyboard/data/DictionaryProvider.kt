package com.clicksy.keyboard.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.ln
import kotlin.math.max

/**
 * Advanced Professional Suggestion & Auto-Correction Engine for Clicksy Keyboard.
 *
 * Capabilities:
 * - High-speed Trie with sub-millisecond prefix autocomplete over 10,000+ English words
 * - Spatial QWERTY distance modeling for accidental adjacent keypresses (e.g., 'o' vs 'p', 's' vs 'd')
 * - Contraction & Slang expansions (e.g., "dont" -> "don't", "im" -> "I'm", "ill" -> "I'll")
 * - N-Gram / Bigram Contextual Next-Word Prediction (e.g., "Good" -> "morning", "Thank" -> "you")
 * - Real-time User Learning Engine with asynchronous background persistence
 * - Contextual inline emoji suggestions
 * - Smart auto-capitalization & casing preservation
 */
object DictionaryProvider {

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private class TrieNode {
        val children = mutableMapOf<Char, TrieNode>()
        var isWord = false
        var frequency = 0
        var word: String? = null
    }

    private val root = TrieNode()

    @Volatile
    private var initialized = false

    // Fast lookup sets & maps
    private val allDictionaryWords = mutableSetOf<String>()
    private val wordsByLength = Array(25) { mutableListOf<String>() }
    private val userWords = mutableMapOf<String, Int>()
    private var cachedTopUserWords: List<String>? = null
    private val bigramMap = mutableMapOf<String, MutableMap<String, Int>>()

    // Direct common typo / contraction corrections map
    private val commonTypoMap: Map<String, String> by lazy {
        EnglishDictionaryData.commonTypoMap
    }

    // Inline contextual emoji recommendations
    private val emojiMap: Map<String, String> by lazy {
        EnglishDictionaryData.inlineEmojiMap
    }

    /**
     * Initializes core dictionary corpus, pre-built bigrams & user learned vocabulary.
     */
    fun initialize(context: Context? = null) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return

            // 1. Load core 10k English corpus frequencies into Trie
            EnglishDictionaryData.loadWordFrequencies { word, freq ->
                val lower = word.lowercase()
                insert(lower, freq)
                allDictionaryWords.add(lower)
                if (lower.length < 25) {
                    wordsByLength[lower.length].add(lower)
                }
            }

            // 2. Pre-seed default English conversational bigrams
            EnglishDictionaryData.defaultBigrams.forEach { (prev, nextMap) ->
                bigramMap[prev] = nextMap.toMutableMap()
            }

            // 3. Load user-learned words from user_dict.txt
            if (context != null) {
                try {
                    val file = File(context.filesDir, "user_dict.txt")
                    if (file.exists()) {
                        file.readLines().forEach { line ->
                            val parts = line.split(":")
                            if (parts.size == 2) {
                                val word = parts[0].trim().lowercase()
                                val freq = parts[1].toIntOrNull() ?: 50
                                if (word.isNotEmpty()) {
                                    userWords[word] = freq
                                    insert(word, freq + 15000)
                                    allDictionaryWords.add(word)
                                    if (word.length < 25) {
                                        wordsByLength[word.length].add(word)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 4. Load user-learned bigrams from user_bigrams.txt
                try {
                    val file = File(context.filesDir, "user_bigrams.txt")
                    if (file.exists()) {
                        file.readLines().forEach { line ->
                            val parts = line.split("->")
                            if (parts.size == 2) {
                                val prev = parts[0].trim().lowercase()
                                val nextParts = parts[1].split(":")
                                if (nextParts.size == 2) {
                                    val next = nextParts[0].trim().lowercase()
                                    val freq = nextParts[1].toIntOrNull() ?: 10
                                    bigramMap.getOrPut(prev) { mutableMapOf() }[next] = freq
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            initialized = true
        }
    }

    private fun insert(word: String, frequency: Int) {
        var node = root
        for (char in word) {
            node = node.children.getOrPut(char) { TrieNode() }
        }
        node.isWord = true
        node.frequency = max(node.frequency, frequency)
        node.word = word
    }

    /**
     * Learns a user word dynamically and boosts its frequency rank.
     */
    fun learnWord(context: Context? = null, word: String) {
        if (word.isBlank() || word.length < 2) return
        val normalized = word.trim().lowercase()
        if (!normalized.all { it.isLetter() || it == '\'' }) return

        initialize(context)

        val wordsSnapshot: Map<String, Int>
        synchronized(this) {
            val currentFreq = userWords[normalized] ?: 0
            val newFreq = currentFreq + 100
            userWords[normalized] = newFreq
            cachedTopUserWords = null
            insert(normalized, newFreq + 15000)
            if (allDictionaryWords.add(normalized) && normalized.length < 25) {
                wordsByLength[normalized.length].add(normalized)
            }
            wordsSnapshot = userWords.toMap()
        }

        if (context != null) {
            ioScope.launch {
                try {
                    val file = File(context.filesDir, "user_dict.txt")
                    file.printWriter().use { writer ->
                        wordsSnapshot.forEach { (w, f) ->
                            writer.println("$w:$f")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Learns a sequence of 2 words (Bigram typing pattern) to predict next words dynamically.
     */
    fun learnWordSequence(context: Context? = null, prevWord: String, nextWord: String) {
        if (prevWord.isBlank() || nextWord.isBlank()) return
        val prev = prevWord.trim().lowercase()
        val next = nextWord.trim().lowercase()

        if (!prev.all { it.isLetter() || it == '\'' } || !next.all { it.isLetter() || it == '\'' }) return

        initialize(context)

        val bigramsSnapshot: Map<String, Map<String, Int>>
        synchronized(this) {
            val nextMap = bigramMap.getOrPut(prev) { mutableMapOf() }
            val currentFreq = nextMap[next] ?: 0
            nextMap[next] = currentFreq + 20
            bigramsSnapshot = bigramMap.mapValues { it.value.toMap() }
        }

        if (context != null) {
            ioScope.launch {
                try {
                    val file = File(context.filesDir, "user_bigrams.txt")
                    file.printWriter().use { writer ->
                        bigramsSnapshot.forEach { (p, map) ->
                            map.forEach { (n, f) ->
                                writer.println("$p->$n:$f")
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Preserves user casing style (e.g., "Th" -> "The", "TH" -> "THE", "th" -> "the", "im" -> "I'm").
     */
    fun applyCasing(source: String, target: String): String {
        if (source.isEmpty() || target.isEmpty()) return target

        // Special standalone pronoun "I"
        if (target.equals("i", ignoreCase = true)) {
            return "I"
        }

        // Special contractions starting with I: "I'm", "I've", "I'll", "I'd"
        if (target.startsWith("i'", ignoreCase = true) || target.equals("i'm", ignoreCase = true) ||
            target.equals("i've", ignoreCase = true) || target.equals("i'll", ignoreCase = true) ||
            target.equals("i'd", ignoreCase = true)) {
            return target.replaceFirstChar { it.uppercase() }
        }

        // All uppercase input: "HELLO" -> "WORLD"
        if (source.length >= 2 && source.all { it.isUpperCase() || !it.isLetter() }) {
            return target.uppercase()
        }

        // Title case: "Hel" -> "Hello"
        if (source.first().isUpperCase()) {
            return target.replaceFirstChar { it.uppercase() }
        }

        return target
    }

    /**
     * Returns next-word predictions based on previous word context (Bigram model)
     * supplemented by user's most frequently typed words, universal conversation starters,
     * and contextual emojis.
     */
    fun getNextWordPredictions(prevWord: String, limit: Int = 3): List<String> {
        val lowerPrev = prevWord.trim().lowercase()
        val results = mutableListOf<String>()

        if (lowerPrev.isNotEmpty()) {
            // 1. Contextual Bigram lookup
            val nextMap = synchronized(this) {
                bigramMap[lowerPrev]?.toMap()
            }
            if (nextMap != null) {
                val sortedBigrams = nextMap.entries
                    .sortedByDescending { it.value }
                    .map { it.key }
                for (word in sortedBigrams) {
                    if (word !in results) {
                        results.add(word)
                        if (results.size >= limit) break
                    }
                }
            }

            // Check if there is an emoji corresponding to the previous word (e.g., "love" -> ❤️)
            val emojiMatch = emojiMap[lowerPrev]
            if (emojiMatch != null && emojiMatch !in results && results.size < limit) {
                results.add(emojiMatch)
            }
        }

        // 2. User top frequent words
        if (results.size < limit) {
            val topUserWords = synchronized(this) {
                cachedTopUserWords ?: userWords.entries
                    .sortedByDescending { it.value }
                    .map { it.key }
                    .also { cachedTopUserWords = it }
            }
            for (word in topUserWords) {
                if (word !in results && word != lowerPrev) {
                    results.add(word)
                    if (results.size >= limit) break
                }
            }
        }

        // 3. Conversational high-frequency fallbacks
        if (results.size < limit) {
            val fallbacks = listOf("I", "the", "to", "you", "and", "a", "is", "for", "in", "it", "that", "my", "we")
            for (word in fallbacks) {
                if (word !in results && !word.equals(lowerPrev, ignoreCase = true)) {
                    results.add(word)
                    if (results.size >= limit) break
                }
            }
        }

        return results.take(limit).map { applyCasing(prevWord, it) }
    }

    /**
     * Returns ranked autocomplete & auto-correct suggestions for current prefix.
     * Combines:
     * 1. Direct typo/contraction map (e.g., "dont" -> "don't", "im" -> "I'm")
     * 2. Context-aware bigram prefix matching
     * 3. Prefix matches in the 10k Trie corpus with frequency scoring
     * 4. Spatial QWERTY proximity & Damerau-Levenshtein fuzzy matching
     * 5. Contextual inline emoji match
     */
    fun getSuggestions(prefix: String, prevWord: String = "", limit: Int = 3): List<String> {
        if (prefix.isBlank()) return emptyList()

        val lowerPrefix = prefix.lowercase().trim()
        val lowerPrev = prevWord.lowercase().trim()
        val scoredCandidates = mutableMapOf<String, Float>()

        // 1. Direct typo / contraction check (Highest confidence)
        commonTypoMap[lowerPrefix]?.let { typoCorrection ->
            scoredCandidates[typoCorrection.lowercase()] = 100000f
        }

        // 2. Exact match in dictionary gets strong baseline score (only for actual words, not random single-letter prefixes)
        if (allDictionaryWords.contains(lowerPrefix) && (lowerPrefix.length >= 2 || lowerPrefix == "a" || lowerPrefix == "i")) {
            scoredCandidates[lowerPrefix] = scoredCandidates.getOrDefault(lowerPrefix, 0f) + 30000f
        }

        // 3. Context-Aware Bigram Match: If prevWord has bigrams starting with this prefix
        if (lowerPrev.isNotEmpty()) {
            val contextBigrams = synchronized(this) {
                bigramMap[lowerPrev]?.toMap()
            }
            if (contextBigrams != null) {
                contextBigrams.forEach { (candidate, freq) ->
                    if (candidate.startsWith(lowerPrefix)) {
                        val boost = freq * 250f + 50000f
                        scoredCandidates[candidate] = scoredCandidates.getOrDefault(candidate, 0f) + boost
                    }
                }
            }
        }

        // 4. Trie Prefix Search (Unigram Frequency Ranking)
        var node: TrieNode? = root
        for (char in lowerPrefix) {
            node = node?.children?.get(char)
            if (node == null) break
        }

        if (node != null) {
            val prefixMatches = mutableListOf<Pair<String, Int>>()
            collectWords(node, StringBuilder(lowerPrefix), prefixMatches, maxResults = 40)
            prefixMatches.forEach { (matchedWord, freq) ->
                // Exact prefix match bonus
                val lenDiff = matchedWord.length - lowerPrefix.length
                val lenPenalty = lenDiff * 15f
                val score = freq.toFloat() - lenPenalty + 5000f
                scoredCandidates[matchedWord] = scoredCandidates.getOrDefault(matchedWord, 0f) + score
            }
        }

        // 5. Spatial QWERTY Proximity & Fuzzy Typo Search (if not enough high-confidence candidates)
        if (scoredCandidates.size < 10 && lowerPrefix.length >= 2) {
            val prefixLen = lowerPrefix.length
            val minLen = (prefixLen - 1).coerceAtLeast(1)
            val maxLen = (prefixLen + 1).coerceAtMost(24)
            val firstChar = lowerPrefix[0]

            var testedCount = 0
            for (len in minLen..maxLen) {
                if (testedCount >= 120) break
                val bucket = wordsByLength[len]
                for (i in 0 until bucket.size) {
                    val candidate = bucket[i]
                    if (candidate.startsWith(firstChar) ||
                        KeyboardProximity.getSubstitutionCost(candidate[0], firstChar) <= 0.6f ||
                        candidate.length == prefixLen
                    ) {
                        val distance = KeyboardProximity.calculateWeightedDistance(lowerPrefix, candidate)
                        if (distance <= 1.8f) {
                            val baseFreq = 2000f
                            val score = baseFreq - (distance * 2500f)
                            val existing = scoredCandidates[candidate] ?: 0f
                            if (score > existing) {
                                scoredCandidates[candidate] = score
                            }
                        }
                        testedCount++
                        if (testedCount >= 120) break
                    }
                }
            }
        }

        // 6. Inline Emoji match (e.g., typing "fire" suggests "🔥" as an option)
        val matchingEmoji = emojiMap[lowerPrefix]

        // Rank all candidates by score descending
        val sortedList = scoredCandidates.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .toMutableList()

        // Construct 3-slot professional layout:
        // Slot 1 (Center) = Top candidate (Primary / Autocorrect target)
        // Slot 0 (Left) = Literal user typed text (if distinct) OR 2nd candidate
        // Slot 2 (Right) = 3rd candidate OR matched Emoji
        val results = mutableListOf<String>()

        for (cand in sortedList) {
            if (cand !in results) {
                results.add(cand)
                if (results.size >= limit + 1) break
            }
        }

        // Append emoji if available
        if (matchingEmoji != null && matchingEmoji !in results) {
            if (results.size >= limit) {
                results[limit - 1] = matchingEmoji
            } else {
                results.add(matchingEmoji)
            }
        }

        return results.take(limit).map { applyCasing(prefix, it) }
    }

    /**
     * Determines whether to auto-correct the typed word when SPACE is pressed.
     * Returns the target correction string if high confidence, or null if no correction.
     */
    fun getAutoCorrection(word: String, prevWord: String = ""): String? {
        if (word.isBlank() || word.length < 2) {
            // Special single-letter auto-correction for "i" -> "I"
            if (word == "i") return "I"
            return null
        }

        val lower = word.lowercase().trim()

        // 1. Direct typo / contraction check (e.g., "dont" -> "don't", "im" -> "I'm")
        commonTypoMap[lower]?.let {
            return applyCasing(word, it)
        }

        // If the word is already a valid dictionary word, do not aggressively change it unless it's a known typo
        if (allDictionaryWords.contains(lower)) {
            return null
        }

        // 2. Spatial QWERTY Proximity fuzzy correction for mistyped non-dictionary words
        var bestCandidate: String? = null
        var minDistance = Float.MAX_VALUE

        val candidates = allDictionaryWords.filter { candidate ->
            kotlin.math.abs(candidate.length - lower.length) <= 1
        }.take(150)

        for (candidate in candidates) {
            val dist = KeyboardProximity.calculateWeightedDistance(lower, candidate)
            // Distance <= 1.05 means either 1 adjacent touch typo, 1 transposition, or 1 missed letter
            if (dist <= 1.05f && dist < minDistance) {
                minDistance = dist
                bestCandidate = candidate
            }
        }

        if (bestCandidate != null) {
            return applyCasing(word, bestCandidate)
        }

        return null
    }

    private fun collectWords(
        node: TrieNode,
        current: StringBuilder,
        results: MutableList<Pair<String, Int>>,
        maxResults: Int = 30
    ) {
        if (results.size >= maxResults) return
        if (node.isWord && node.word != null) {
            results.add(node.word!! to node.frequency)
        }
        for ((char, child) in node.children) {
            if (results.size >= maxResults) return
            current.append(char)
            collectWords(child, current, results, maxResults)
            current.deleteCharAt(current.length - 1)
        }
    }
}
