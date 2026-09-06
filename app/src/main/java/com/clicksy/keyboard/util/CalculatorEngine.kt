package com.clicksy.keyboard.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Fast, self-contained mathematical expression parser and evaluator for the keyboard calculator.
 * Supports: +, -, ×, ÷, %, parentheses, negative numbers, and clean number formatting.
 */
object CalculatorEngine {

    private val decimalFormat = DecimalFormat("#,##0.######", DecimalFormatSymbols(Locale.US))
    private val plainFormat = DecimalFormat("0.######", DecimalFormatSymbols(Locale.US))

    /**
     * Evaluates a math expression string and returns the formatted result, or null if invalid.
     */
    fun evaluate(expression: String): String? {
        if (expression.isBlank()) return null

        val sanitized = expression
            .replace("×", "*")
            .replace("÷", "/")
            .replace(",", "")
            .trim()

        return try {
            val parser = MathParser(sanitized)
            val result = parser.parse()
            if (result.isNaN() || result.isInfinite()) {
                "Error"
            } else {
                formatResult(result)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Formats a double to clean readable string (no unnecessary trailing zeros).
     */
    fun formatResult(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "Error"
        // Avoid -0
        val cleanValue = if (value == -0.0) 0.0 else value
        return if (cleanValue % 1.0 == 0.0 && cleanValue <= Long.MAX_VALUE.toDouble() && cleanValue >= Long.MIN_VALUE.toDouble()) {
            cleanValue.toLong().toString()
        } else {
            plainFormat.format(cleanValue)
        }
    }

    /**
     * Recursive Descent Math Parser:
     * Expression = Term (+|- Term)*
     * Term       = Factor (*|/|% Factor)*
     * Factor     = +Factor | -Factor | (Expression) | Number
     */
    private class MathParser(private val input: String) {
        private var pos = -1
        private var ch = ' '

        private fun nextChar() {
            pos++
            ch = if (pos < input.length) input[pos] else '\u0000'
        }

        private fun eat(charToEat: Char): Boolean {
            while (ch == ' ') nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            while (ch == ' ') nextChar()
            if (pos < input.length && ch != '\u0000') {
                throw IllegalArgumentException("Unexpected: $ch")
            }
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+') -> x += parseTerm()
                    eat('-') -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*') -> x *= parseFactor()
                    eat('/') -> {
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("Division by zero")
                        x /= divisor
                    }
                    eat('%') -> {
                        x %= parseFactor()
                    }
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+')) return +parseFactor()
            if (eat('-')) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('(')) {
                x = parseExpression()
                eat(')')
            } else if ((ch in '0'..'9') || ch == '.') {
                while ((ch in '0'..'9') || ch == '.') nextChar()
                x = input.substring(startPos, pos).toDouble()
            } else {
                throw IllegalArgumentException("Unexpected: $ch")
            }

            return x
        }
    }
}
