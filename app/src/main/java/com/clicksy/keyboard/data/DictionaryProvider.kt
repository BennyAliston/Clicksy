package com.clicksy.keyboard.data

import android.content.Context
import java.io.File
import kotlin.math.min

/**
 * Advanced Adaptive Learning Engine for Clicksy Keyboard.
 * Features:
 * - Dynamic vocabulary frequency learning (custom names, slang, user words)
 * - N-Gram / Bigram Contextual Next-Word Prediction (learns typing patterns like "Good" -> "morning")
 * - Damerau-Levenshtein fuzzy matching & typo auto-correction
 */
object DictionaryProvider {

    private class TrieNode {
        val children = mutableMapOf<Char, TrieNode>()
        var isWord = false
        var frequency = 0 // Higher = more common
    }

    private val root = TrieNode()
    private var initialized = false
    private val userWords = mutableMapOf<String, Int>()
    private val allDictionaryWords = mutableSetOf<String>()

    // Bigram Context Model: prevWord -> (nextWord -> frequency)
    private val bigramMap = mutableMapOf<String, MutableMap<String, Int>>()

    // Direct common typo / slang / contraction corrections map
    private val commonTypoMap = mapOf(
        "teh" to "the",
        "taht" to "that",
        "thier" to "their",
        "woudl" to "would",
        "shoudl" to "should",
        "coudl" to "could",
        "recieve" to "receive",
        "seperate" to "separate",
        "untill" to "until",
        "tomorow" to "tomorrow",
        "tomoroww" to "tomorrow",
        "yestarday" to "yesterday",
        "awsome" to "awesome",
        "thikn" to "think",
        "thnk" to "think",
        "plz" to "please",
        "pls" to "please",
        "thx" to "thanks",
        "ty" to "thank you",
        "bc" to "because",
        "bcz" to "because",
        "dont" to "don't",
        "cant" to "can't",
        "wont" to "won't",
        "im" to "i'm",
        "its" to "it's",
        "youre" to "you're",
        "hes" to "he's",
        "shes" to "she's",
        "theyre" to "they're",
        "ive" to "i've",
        "id" to "i'd",
        "ill" to "i'll",
        "didnt" to "didn't",
        "isnt" to "isn't",
        "havent" to "haven't",
        "hasnt" to "hasn't",
        "wasnt" to "wasn't",
        "werent" to "weren't",
        "couldnt" to "couldn't",
        "shouldnt" to "shouldn't",
        "wouldnt" to "wouldn't",
        "doesnt" to "doesn't",
        "theres" to "there's",
        "thats" to "that's",
        "whats" to "what's",
        "whos" to "who's",
        "lets" to "let's"
    )

    /**
     * Initializes static common dictionary, pre-built bigrams & user-learned patterns.
     */
    fun initialize(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return

            // 1. Load static common words
            commonWords.forEachIndexed { index, word ->
                val lower = word.lowercase()
                val freq = maxOf(1, commonWords.size - index)
                insert(lower, freq)
                allDictionaryWords.add(lower)
            }

            // 2. Pre-seed popular English bigrams
            seedDefaultBigrams()

            // 3. Load user-learned words from user_dict.txt
            try {
                val file = File(context.filesDir, "user_dict.txt")
                if (file.exists()) {
                    file.readLines().forEach { line ->
                        val parts = line.split(":")
                        if (parts.size == 2) {
                            val word = parts[0].trim().lowercase()
                            val freq = parts[1].toIntOrNull() ?: 10
                            if (word.isNotEmpty()) {
                                userWords[word] = freq
                                insert(word, freq + 1000)
                                allDictionaryWords.add(word)
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
                                val freq = nextParts[1].toIntOrNull() ?: 5
                                bigramMap.getOrPut(prev) { mutableMapOf() }[next] = freq
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
        node.frequency = maxOf(node.frequency, frequency)
    }

    /**
     * Learns a custom user word and boosts its frequency rank.
     */
    fun learnWord(context: Context, word: String) {
        if (word.isBlank() || word.length < 2) return
        val normalized = word.trim().lowercase()
        if (!normalized.all { it.isLetter() || it == '\'' }) return

        initialize(context)

        synchronized(this) {
            val currentFreq = userWords[normalized] ?: 0
            val newFreq = currentFreq + 50
            userWords[normalized] = newFreq
            insert(normalized, newFreq + 1000)
            allDictionaryWords.add(normalized)

            try {
                val file = File(context.filesDir, "user_dict.txt")
                file.printWriter().use { writer ->
                    userWords.forEach { (w, f) ->
                        writer.println("$w:$f")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Learns a sequence of 2 words (Bigram typing pattern) to predict next words dynamically.
     */
    fun learnWordSequence(context: Context, prevWord: String, nextWord: String) {
        if (prevWord.isBlank() || nextWord.isBlank()) return
        val prev = prevWord.trim().lowercase()
        val next = nextWord.trim().lowercase()

        if (!prev.all { it.isLetter() || it == '\'' } || !next.all { it.isLetter() || it == '\'' }) return

        initialize(context)

        synchronized(this) {
            val nextMap = bigramMap.getOrPut(prev) { mutableMapOf() }
            val currentFreq = nextMap[next] ?: 0
            nextMap[next] = currentFreq + 10

            // Save learned bigram patterns to disk
            try {
                val file = File(context.filesDir, "user_bigrams.txt")
                file.printWriter().use { writer ->
                    bigramMap.forEach { (p, map) ->
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

    /**
     * Returns next-word predictions based on the previous word context (Bigram model).
     */
    fun getNextWordPredictions(prevWord: String, limit: Int = 3): List<String> {
        if (prevWord.isBlank()) return emptyList()
        val lowerPrev = prevWord.trim().lowercase()

        val nextMap = bigramMap[lowerPrev] ?: return emptyList()
        return nextMap.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }
    }

    /**
     * Returns autocomplete suggestions for current prefix, combining Trie search,
     * typo map, and Damerau-Levenshtein distance matching.
     */
    fun getSuggestions(prefix: String, limit: Int = 3): List<String> {
        if (prefix.isBlank()) return emptyList()

        val lowerPrefix = prefix.lowercase().trim()
        val results = mutableListOf<String>()

        // 1. Direct typo map check
        commonTypoMap[lowerPrefix]?.let { typoCorrection ->
            results.add(typoCorrection)
        }

        // 2. Trie Prefix Search
        var node: TrieNode? = root
        for (char in lowerPrefix) {
            node = node?.children?.get(char)
            if (node == null) break
        }

        if (node != null) {
            val prefixMatches = mutableListOf<Pair<String, Int>>()
            collectWords(node, StringBuilder(lowerPrefix), prefixMatches)
            val sortedPrefix = prefixMatches
                .sortedByDescending { it.second }
                .map { it.first }

            sortedPrefix.forEach { match ->
                if (!results.contains(match)) {
                    results.add(match)
                }
            }
        }

        // 3. Damerau-Levenshtein Fuzzy Matching
        if (results.size < limit && lowerPrefix.length >= 3) {
            val fuzzyCandidates = allDictionaryWords
                .filter { Math.abs(it.length - lowerPrefix.length) <= 2 }
                .map { candidate -> candidate to damerauLevenshteinDistance(lowerPrefix, candidate) }
                .filter { it.second in 1..2 }
                .sortedBy { it.second }
                .map { it.first }

            fuzzyCandidates.forEach { match ->
                if (!results.contains(match)) {
                    results.add(match)
                }
            }
        }

        return results.take(limit)
    }

    /**
     * Returns auto-correction target for a completed word.
     */
    fun getAutoCorrection(word: String): String? {
        if (word.isBlank() || word.length < 2) return null
        val lower = word.lowercase().trim()

        if (allDictionaryWords.contains(lower)) return null
        commonTypoMap[lower]?.let { return it }

        if (lower.length >= 3) {
            val bestFuzzy = allDictionaryWords
                .filter { Math.abs(it.length - lower.length) <= 1 }
                .map { candidate -> candidate to damerauLevenshteinDistance(lower, candidate) }
                .filter { it.second == 1 }
                .minByOrNull { it.second }
                ?.first

            if (bestFuzzy != null) return bestFuzzy
        }

        return null
    }

    private fun collectWords(
        node: TrieNode,
        current: StringBuilder,
        results: MutableList<Pair<String, Int>>
    ) {
        if (node.isWord) {
            results.add(current.toString() to node.frequency)
        }
        for ((char, child) in node.children) {
            current.append(char)
            collectWords(child, current, results)
            current.deleteCharAt(current.length - 1)
        }
    }

    private fun damerauLevenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
                if (i > 1 && j > 1 && s1[i - 1] == s2[j - 2] && s1[i - 2] == s2[j - 1]) {
                    dp[i][j] = min(dp[i][j], dp[i - 2][j - 2] + cost)
                }
            }
        }
        return dp[len1][len2]
    }

    private fun seedDefaultBigrams() {
        val defaultPairs = mapOf(
            "good" to mapOf("morning" to 100, "night" to 80, "idea" to 60, "luck" to 50),
            "thank" to mapOf("you" to 120, "so" to 80, "much" to 70),
            "thanks" to mapOf("for" to 90, "a" to 60, "lot" to 50),
            "how" to mapOf("are" to 110, "is" to 80, "was" to 60, "about" to 50),
            "happy" to mapOf("birthday" to 110, "new" to 90, "anniversary" to 50),
            "see" to mapOf("you" to 110, "later" to 80, "soon" to 70),
            "let" to mapOf("me" to 100, "know" to 90, "us" to 50),
            "i" to mapOf("am" to 110, "will" to 90, "have" to 80, "love" to 70, "think" to 60),
            "you" to mapOf("are" to 110, "can" to 80, "have" to 70, "know" to 60),
            "what" to mapOf("is" to 110, "are" to 80, "about" to 70, "do" to 60),
            "have" to mapOf("a" to 110, "been" to 80, "to" to 70, "fun" to 60),
            "call" to mapOf("me" to 100, "you" to 70, "back" to 60),
            "meet" to mapOf("at" to 90, "you" to 80, "up" to 70)
        )

        defaultPairs.forEach { (prev, nextMap) ->
            bigramMap[prev] = nextMap.toMutableMap()
        }
    }

    private val commonWords = listOf(
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "i",
        "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
        "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
        "or", "an", "will", "my", "one", "all", "would", "there", "their", "what",
        "so", "up", "out", "if", "about", "who", "get", "which", "go", "me",
        "when", "make", "can", "like", "time", "no", "just", "him", "know", "take",
        "people", "into", "year", "your", "good", "some", "could", "them", "see", "other",
        "than", "then", "now", "look", "only", "come", "its", "over", "think", "also",
        "back", "after", "use", "two", "how", "our", "work", "first", "well", "way",
        "even", "new", "want", "because", "any", "these", "give", "day", "most", "us",
        "great", "between", "need", "large", "often", "hand", "high", "place", "keep", "help",
        "every", "never", "start", "city", "right", "small", "night", "always", "next", "hard",
        "open", "seem", "together", "each", "begin", "while", "own", "point", "house", "world",
        "near", "build", "self", "home", "much", "both", "here", "move", "still", "end",
        "school", "head", "turn", "real", "leave", "might", "door", "set", "close", "long",
        "before", "last", "left", "few", "side", "been", "call", "part", "early", "water",
        "find", "put", "thing", "many", "play", "away", "animal", "old", "follow", "learn",
        "change", "more", "run", "off", "again", "read", "sure", "under", "going", "stop",
        "let", "thought", "important", "until", "children", "food", "kind", "country", "number", "line",
        "tell", "does", "same", "mean", "differ", "boy", "did", "three", "air", "land",
        "must", "big", "such", "act", "why", "ask", "men", "went", "light", "try",
        "mother", "earth", "father", "stand", "page", "should", "found", "answer", "grow", "study",
        "plant", "cover", "sun", "four", "state", "eye", "tree", "cross", "farm", "story",
        "saw", "far", "sea", "draw", "late", "press", "life", "north", "white", "got",
        "walk", "example", "ease", "paper", "group", "music", "those", "mark", "letter", "mile",
        "river", "car", "feet", "care", "second", "book", "carry", "took", "science", "eat",
        "room", "friend", "began", "idea", "fish", "mountain", "once", "base", "hear", "horse",
        "cut", "watch", "color", "face", "wood", "main", "enough", "plain", "girl", "usual",
        "young", "ready", "above", "ever", "red", "list", "though", "feel", "talk", "bird",
        "soon", "body", "dog", "family", "direct", "pose", "song", "measure", "product", "black",
        "short", "numeral", "class", "wind", "question", "happen", "complete", "ship", "area", "half",
        "rock", "order", "fire", "south", "problem", "piece", "told", "knew", "pass", "since",
        "top", "whole", "king", "space", "heard", "best", "hour", "better", "true", "during",
        "hundred", "five", "remember", "step", "hold", "west", "ground", "interest", "reach", "fast",
        "verb", "sing", "listen", "six", "table", "travel", "less", "morning", "ten", "simple",
        "several", "vowel", "toward", "war", "lay", "against", "pattern", "slow", "center", "love",
        "person", "money", "serve", "appear", "road", "map", "rain", "rule", "govern", "pull",
        "cold", "notice", "voice", "unit", "power", "town", "fine", "certain", "fly", "fall",
        "lead", "cry", "dark", "machine", "note", "wait", "plan", "figure", "star", "box",
        "noun", "field", "rest", "correct", "able", "pound", "done", "beauty", "drive", "stood",
        "contain", "front", "teach", "week", "final", "gave", "green", "oh", "quick", "develop",
        "ocean", "warm", "free", "minute", "strong", "special", "mind", "behind", "clear", "tail",
        "produce", "fact", "street", "inch", "lot", "nothing", "course", "stay", "wheel", "full",
        "force", "blue", "object", "decide", "surface", "deep", "moon", "island", "foot", "system",
        "busy", "test", "record", "boat", "common", "gold", "possible", "plane", "age", "dry",
        "wonder", "laugh", "thousand", "ago", "ran", "check", "game", "shape", "yes", "hot",
        "miss", "brought", "heat", "snow", "bed", "bring", "sit", "perhaps", "fill", "east",
        "weight", "language", "among", "please", "thank", "hello", "okay", "sorry", "welcome", "goodbye",
        "maybe", "today", "tomorrow", "yesterday", "evening", "afternoon", "tonight", "happy", "awesome", "amazing",
        "beautiful", "wonderful", "perfect", "nice", "cool", "funny", "thanks", "congrats", "birthday", "party",
        "dinner", "lunch", "breakfast", "coffee", "meeting", "message", "phone", "email", "address", "name",
        "password", "account", "update", "download", "upload", "don't", "can't", "won't", "i'm", "it's",
        "you're", "he's", "she's", "they're", "i've", "i'd", "i'll", "didn't", "isn't", "haven't",
        "hasn't", "wasn't", "weren't", "couldn't", "shouldn't", "wouldn't", "doesn't", "there's", "that's", "what's",
        "who's", "let's"
    )
}
