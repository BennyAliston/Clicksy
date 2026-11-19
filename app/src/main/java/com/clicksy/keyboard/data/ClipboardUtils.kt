package com.clicksy.keyboard.data

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Intelligent categorization and content analysis for clipboard items.
 */
enum class ClipboardCategory(val label: String, val icon: String) {
    ALL("All", "📋"),
    PINNED("Pinned", "📌"),
    FAVORITES("Favorites", "⭐"),
    LINKS("Links", "🔗"),
    CODE("Code", "💻"),
    CONTACTS("Contacts", "✉️"),
    SNIPPETS("Snippets", "📝")
}

/**
 * Type of content detected within a clipboard item.
 */
enum class ClipboardContentType(val label: String, val icon: String) {
    URL("Link", "🔗"),
    EMAIL("Email", "✉️"),
    PHONE("Phone", "📞"),
    CODE("Code", "💻"),
    COLOR("Color", "🎨"),
    TEXT("Text", "📄")
}

/**
 * Available text transformation utilities for clipboard clips.
 */
enum class TextTransformType(val label: String, val icon: String) {
    UPPERCASE("UPPER", "🔠"),
    LOWERCASE("lower", "🔡"),
    TITLE_CASE("Title", "🔤"),
    TRIM("Trim", "✂️"),
    SINGLE_LINE("Single Line", "➖"),
    CLEAN_URL("Clean Link", "🧹")
}

object ClipboardUtils {

    // Regex for detecting web links
    private val URL_REGEX = Regex(
        "(?i)\\b(?:https?://|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\((?:[^\\s()<>]+|(?:\\([^\\s()<>]+\\)))*\\))+(?:\\((?:[^\\s()<>]+|(?:\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’])"
    )

    // Regex for detecting email addresses
    private val EMAIL_REGEX = Regex(
        "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b"
    )

    // Regex for detecting phone numbers (international or local formats with 7+ digits)
    private val PHONE_REGEX = Regex(
        "(?:\\+?\\d{1,3}[-.\\s]?)?(?:\\(?\\d{2,4}\\)?[-.\\s]?)?\\d{3,4}[-.\\s]?\\d{4}"
    )

    // Tracking query parameter keys that clutter URLs
    private val TRACKING_PARAMS = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content", "utm_id",
        "fbclid", "gclid", "gclsrc", "dclid", "msclkid",
        "ref", "ref_src", "source", "igshid", "si", "feature", "ved",
        "t", "s", // Twitter/X share tracking
        "twclid", "mc_cid", "mc_eid",
        "zanpid", "origin", "tracking_id", "tag",
        "_hsenc", "_hsmi", "vero_id", "wickedid"
    )

    /**
     * Determines the primary content type of the given text.
     */
    fun detectContentType(text: String): ClipboardContentType {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ClipboardContentType.TEXT

        // 1. URL check
        if (isUrl(trimmed)) {
            return ClipboardContentType.URL
        }

        // 2. Email check
        if (isEmail(trimmed)) {
            return ClipboardContentType.EMAIL
        }

        // 3. Phone check
        if (isPhone(trimmed)) {
            return ClipboardContentType.PHONE
        }

        // 4. Color check (Hex / RGB)
        if (isColor(trimmed)) {
            return ClipboardContentType.COLOR
        }

        // 5. Code / Snippet check (brackets, syntax indicators, or multiline indented blocks)
        if (isCodeSnippet(trimmed)) {
            return ClipboardContentType.CODE
        }

        return ClipboardContentType.TEXT
    }

    /**
     * Checks if the given text is or contains a valid web URL.
     */
    fun isUrl(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("www.", ignoreCase = true)
        ) {
            return true
        }
        return URL_REGEX.containsMatchIn(trimmed)
    }

    /**
     * Checks if the given text contains or matches an email address.
     */
    fun isEmail(text: String): Boolean {
        return EMAIL_REGEX.containsMatchIn(text)
    }

    /**
     * Checks if the given text appears to be a phone number.
     */
    fun isPhone(text: String): Boolean {
        val digitsCount = text.count { it.isDigit() }
        if (digitsCount in 7..15 && PHONE_REGEX.containsMatchIn(text.trim())) {
            val letterCount = text.count { it.isLetter() }
            return letterCount <= digitsCount / 2
        }
        return false
    }

    /**
     * Checks if the text looks like a programming code snippet or JSON payload.
     */
    fun isCodeSnippet(text: String): Boolean {
        val trimmed = text.trim()
        val codeMarkers = listOf(
            "function", "class ", "def ", "val ", "var ", "const ",
            "import ", "export ", "return ", "if (", "while (", "for (",
            "SELECT ", "FROM ", "WHERE ", "INSERT INTO", "<div", "</", "/>",
            "{\n", "};\n", "=>", "fun ", "interface ", "package ", "struct ",
            "public ", "private ", "protected ", "override ", "async ", "await "
        )
        val hasMarker = codeMarkers.any { trimmed.contains(it) }
        val hasBraces = (trimmed.contains("{") && trimmed.contains("}")) ||
                (trimmed.contains("<") && trimmed.contains(">"))
        val isJson = (trimmed.startsWith("{") && trimmed.endsWith("}") && trimmed.contains("\":")) ||
                (trimmed.startsWith("[") && trimmed.endsWith("]") && trimmed.contains("{\""))
        return hasMarker || isJson || (hasBraces && trimmed.contains("\n"))
    }

    /**
     * Checks if the given text is a valid Hex or RGB/RGBA color code.
     */
    fun isColor(text: String): Boolean {
        return parseHexColor(text) != null
    }

    /**
     * Parses a hex color code string into an ARGB 64-bit Long (e.g. 0xFF5722 -> 0xFFFF5722L).
     * Supports formats: #RGB, #RGBA, #RRGGBB, #AARRGGBB.
     */
    fun parseHexColor(text: String): Long? {
        val trimmed = text.trim()
        if (!trimmed.startsWith("#")) return null
        val hex = trimmed.substring(1)
        if (hex.isEmpty() || !hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
            return null
        }
        return when (hex.length) {
            3 -> { // #RGB -> #RRGGBB
                val r = hex[0].digitToInt(16)
                val g = hex[1].digitToInt(16)
                val b = hex[2].digitToInt(16)
                val fullR = (r shl 4) or r
                val fullG = (g shl 4) or g
                val fullB = (b shl 4) or b
                (0xFFL shl 24) or (fullR.toLong() shl 16) or (fullG.toLong() shl 8) or fullB.toLong()
            }
            4 -> { // #RGBA -> #AARRGGBB
                val r = hex[0].digitToInt(16)
                val g = hex[1].digitToInt(16)
                val b = hex[2].digitToInt(16)
                val a = hex[3].digitToInt(16)
                val fullR = (r shl 4) or r
                val fullG = (g shl 4) or g
                val fullB = (b shl 4) or b
                val fullA = (a shl 4) or a
                (fullA.toLong() shl 24) or (fullR.toLong() shl 16) or (fullG.toLong() shl 8) or fullB.toLong()
            }
            6 -> { // #RRGGBB
                val v = hex.toLong(16)
                (0xFFL shl 24) or v
            }
            8 -> { // #AARRGGBB
                hex.toLong(16)
            }
            else -> null
        }
    }

    /**
     * Tests if an item matches the selected category filter.
     */
    fun matchesCategory(item: ClipboardEntity, category: ClipboardCategory): Boolean {
        return when (category) {
            ClipboardCategory.ALL -> true
            ClipboardCategory.PINNED -> item.pinned
            ClipboardCategory.FAVORITES -> item.favorite
            ClipboardCategory.LINKS -> isUrl(item.text)
            ClipboardCategory.CODE -> isCodeSnippet(item.text)
            ClipboardCategory.CONTACTS -> isEmail(item.text) || isPhone(item.text)
            ClipboardCategory.SNIPPETS -> item.text.contains("\n") || item.text.length > 80
        }
    }

    /**
     * Extracts the first URL from text if present.
     */
    fun extractFirstUrl(text: String): String? {
        val match = URL_REGEX.find(text) ?: return null
        var url = match.value
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            url = "https://$url"
        }
        return url
    }

    /**
     * Extracts the web domain name from a URL (e.g. "github.com").
     */
    fun extractDomain(url: String): String {
        return try {
            val trimmed = url.trim()
            val candidate = if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
                "https://$trimmed"
            } else {
                trimmed
            }
            val uri = URI(candidate)
            val host = uri.host ?: ""
            host.removePrefix("www.")
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Cleans URLs by stripping tracking and analytics parameters (e.g., utm_source, fbclid).
     */
    fun cleanUrl(input: String): String {
        return try {
            val trimmed = input.trim()
            val hasScheme = trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)
            val candidate = if (!hasScheme) "https://$trimmed" else trimmed
            val uri = URI(candidate)
            val query = uri.rawQuery ?: return trimmed

            val cleanedParams = query.split("&")
                .mapNotNull { param ->
                    val parts = param.split("=", limit = 2)
                    val key = parts[0].lowercase()
                    if (key in TRACKING_PARAMS) null else param
                }

            val newQuery = if (cleanedParams.isEmpty()) null else cleanedParams.joinToString("&")
            val newUri = URI(uri.scheme, uri.authority, uri.path, newQuery, uri.fragment)
            val result = newUri.toString()
            if (!hasScheme && result.startsWith("https://")) {
                result.removePrefix("https://")
            } else {
                result
            }
        } catch (e: Exception) {
            input
        }
    }

    /**
     * Applies standard text transformations to clipboard content.
     */
    fun transformText(text: String, transform: TextTransformType): String {
        return when (transform) {
            TextTransformType.UPPERCASE -> text.uppercase()
            TextTransformType.LOWERCASE -> text.lowercase()
            TextTransformType.TITLE_CASE -> toTitleCase(text)
            TextTransformType.TRIM -> trimWhitespace(text)
            TextTransformType.SINGLE_LINE -> toSingleLine(text)
            TextTransformType.CLEAN_URL -> cleanUrl(text)
        }
    }

    /**
     * Converts string to Title Case.
     */
    fun toTitleCase(text: String): String {
        return text.split(Regex("(?<=\\s)|(?=\\s)"))
            .joinToString("") { word ->
                if (word.isNotBlank()) {
                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                } else {
                    word
                }
            }
    }

    /**
     * Trims leading/trailing whitespace per line and strips blank lines.
     */
    fun trimWhitespace(text: String): String {
        return text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }

    /**
     * Collapses newlines and consecutive whitespaces into a single clean line.
     */
    fun toSingleLine(text: String): String {
        return text.replace(Regex("[\\r\\n]+"), " ")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
    }

    /**
     * Formats human-readable metadata for a clip (e.g. "3 lines • 45 words" or "28 chars").
     */
    fun formatItemMetadata(text: String): String {
        val lines = text.lines().size
        val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size
        val chars = text.length

        return when {
            lines > 1 -> "$lines lines • $words words"
            words > 4 -> "$words words • $chars chars"
            else -> "$chars chars"
        }
    }

    /**
     * Detects potential credit card numbers to safeguard user privacy.
     */
    fun isPotentialCreditCard(text: String): Boolean {
        val digitsOnly = text.filter { it.isDigit() }
        if (digitsOnly.length in 11..19) {
            val nonDigitChars = text.filterNot { it.isDigit() || it.isWhitespace() || it == '-' }
            if (nonDigitChars.isEmpty()) {
                return passesLuhnChecksum(digitsOnly)
            }
        }
        return false
    }

    /**
     * Detects sensitive developer credentials, API tokens, and private keys
     * to protect users against accidental recording of sensitive data.
     */
    fun isPotentialSecretOrApiKey(text: String): Boolean {
        val trimmed = text.trim()
        // 1. GitHub Personal Access Tokens
        if (trimmed.startsWith("ghp_") || trimmed.startsWith("gho_") || trimmed.startsWith("ghu_") ||
            trimmed.startsWith("ghs_") || trimmed.startsWith("ghr_") || trimmed.startsWith("github_pat_")
        ) {
            return true
        }
        // 2. AWS Access Key ID
        val awsPattern = Regex("^AKIA[0-9A-Z]{16}$")
        if (awsPattern.matches(trimmed)) {
            return true
        }
        // 3. Stripe Secret Key
        if (trimmed.startsWith("sk_live_") || trimmed.startsWith("rk_live_")) {
            return true
        }
        // 4. PEM Private Key block
        if (trimmed.contains("BEGIN PRIVATE KEY") || trimmed.contains("BEGIN RSA PRIVATE KEY") ||
            trimmed.contains("BEGIN EC PRIVATE KEY") || trimmed.contains("BEGIN OPENSSH PRIVATE KEY")
        ) {
            return true
        }
        // 5. JWT tokens (standard 3-segment base64 url format)
        val jwtPattern = Regex("^eyJ[A-Za-z0-9_-]{10,}\\.eyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}$")
        if (jwtPattern.matches(trimmed)) {
            return true
        }
        // 6. OpenAI or generic Secret Key
        if (trimmed.startsWith("sk-") && trimmed.length in 40..60 && !trimmed.contains(" ")) {
            return true
        }
        return false
    }

    private fun passesLuhnChecksum(number: String): Boolean {
        var sum = 0
        var alternate = false
        for (i in number.length - 1 downTo 0) {
            var n = number[i].digitToInt()
            if (alternate) {
                n *= 2
                if (n > 9) n = (n % 10) + 1
            }
            sum += n
            alternate = !alternate
        }
        return sum % 10 == 0
    }
}
