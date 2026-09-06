package com.clicksy.keyboard.data

import kotlin.math.min
import kotlin.math.sqrt

/**
 * QWERTY Keyboard Spatial Proximity Model.
 *
 * Models physical distances between keys on standard touch keyboards.
 * Allows the typo engine to distinguish between likely accidental neighbor keypresses
 * (e.g., 'o' vs 'p' or 's' vs 'd') versus completely unrelated keys (e.g., 'q' vs 'm').
 */
object KeyboardProximity {

    // Physical (row, col) coordinates for 26 English letters on QWERTY
    private val KEY_COORDINATES = mapOf(
        'q' to Pair(0f, 0f),
        'w' to Pair(0f, 1f),
        'e' to Pair(0f, 2f),
        'r' to Pair(0f, 3f),
        't' to Pair(0f, 4f),
        'y' to Pair(0f, 5f),
        'u' to Pair(0f, 6f),
        'i' to Pair(0f, 7f),
        'o' to Pair(0f, 8f),
        'p' to Pair(0f, 9f),

        'a' to Pair(1f, 0.5f),
        's' to Pair(1f, 1.5f),
        'd' to Pair(1f, 2.5f),
        'f' to Pair(1f, 3.5f),
        'g' to Pair(1f, 4.5f),
        'h' to Pair(1f, 5.5f),
        'j' to Pair(1f, 6.5f),
        'k' to Pair(1f, 7.5f),
        'l' to Pair(1f, 8.5f),

        'z' to Pair(2f, 1.5f),
        'x' to Pair(2f, 2.5f),
        'c' to Pair(2f, 3.5f),
        'v' to Pair(2f, 4.5f),
        'b' to Pair(2f, 5.5f),
        'n' to Pair(2f, 6.5f),
        'm' to Pair(2f, 7.5f)
    )

    // Pre-computed 26x26 distance penalty lookup table for 0 allocation during typing
    private val DISTANCE_TABLE = Array(26) { FloatArray(26) }

    init {
        for (i in 0 until 26) {
            val c1 = ('a'.code + i).toChar()
            val coord1 = KEY_COORDINATES[c1] ?: Pair(0f, 0f)
            for (j in 0 until 26) {
                val c2 = ('a'.code + j).toChar()
                val coord2 = KEY_COORDINATES[c2] ?: Pair(0f, 0f)
                if (i == j) {
                    DISTANCE_TABLE[i][j] = 0f
                } else {
                    val dx = coord1.second - coord2.second
                    val dy = coord1.first - coord2.first
                    val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    // If physical distance <= 1.2 (direct neighbor), cost is 0.35f;
                    // otherwise smoothly scale up to max 1.0f
                    DISTANCE_TABLE[i][j] = when {
                        dist <= 1.15f -> 0.35f
                        dist <= 1.6f -> 0.55f
                        dist <= 2.2f -> 0.75f
                        else -> 1.0f
                    }
                }
            }
        }
    }

    /**
     * Returns substitution penalty between two characters.
     */
    fun getSubstitutionCost(c1: Char, c2: Char): Float {
        if (c1 == c2) return 0f
        val lower1 = c1.lowercaseChar()
        val lower2 = c2.lowercaseChar()
        if (lower1 in 'a'..'z' && lower2 in 'a'..'z') {
            return DISTANCE_TABLE[lower1 - 'a'][lower2 - 'a']
        }
        return 1.0f
    }

    /**
     * Calculates spatial Damerau-Levenshtein distance between typed input and candidate word.
     */
    fun calculateWeightedDistance(typed: String, candidate: String): Float {
        val len1 = typed.length
        val len2 = candidate.length
        if (len1 == 0) return len2 * 0.6f
        if (len2 == 0) return len1 * 0.6f

        // Fast length pruning
        if (kotlin.math.abs(len1 - len2) > 2) return 999f

        val dp = Array(len1 + 1) { FloatArray(len2 + 1) }

        for (i in 0..len1) dp[i][0] = i * 0.6f
        for (j in 0..len2) dp[0][j] = j * 0.6f

        for (i in 1..len1) {
            val char1 = typed[i - 1]
            for (j in 1..len2) {
                val char2 = candidate[j - 1]
                val subCost = getSubstitutionCost(char1, char2)

                var minCost = min(
                    min(dp[i - 1][j] + 0.6f, dp[i][j - 1] + 0.6f),
                    dp[i - 1][j - 1] + subCost
                )

                // Transposition check (swap adjacent characters)
                if (i > 1 && j > 1 && typed[i - 1] == candidate[j - 2] && typed[i - 2] == candidate[j - 1]) {
                    minCost = min(minCost, dp[i - 2][j - 2] + 0.4f)
                }

                dp[i][j] = minCost
            }
        }

        return dp[len1][len2]
    }
}
