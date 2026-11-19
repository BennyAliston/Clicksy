package com.clicksy.keyboard.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardUtilsTest {

    @Test
    fun testDetectContentTypeUrl() {
        val type1 = ClipboardUtils.detectContentType("https://github.com/clicksylabs/clicksy")
        assertEquals(ClipboardContentType.URL, type1)

        val type2 = ClipboardUtils.detectContentType("www.example.com/pricing?plan=pro")
        assertEquals(ClipboardContentType.URL, type2)
    }

    @Test
    fun testDetectContentTypeEmail() {
        val type1 = ClipboardUtils.detectContentType("contact@company.org")
        assertEquals(ClipboardContentType.EMAIL, type1)

        val type2 = ClipboardUtils.detectContentType("john.smith+dev@subdomain.example.co.uk")
        assertEquals(ClipboardContentType.EMAIL, type2)
    }

    @Test
    fun testDetectContentTypePhone() {
        val type1 = ClipboardUtils.detectContentType("+1 (555) 234-5678")
        assertEquals(ClipboardContentType.PHONE, type1)

        val type2 = ClipboardUtils.detectContentType("9876543210")
        assertEquals(ClipboardContentType.PHONE, type2)
    }

    @Test
    fun testDetectContentTypeColor() {
        val type1 = ClipboardUtils.detectContentType("#FF5722")
        assertEquals(ClipboardContentType.COLOR, type1)

        val type2 = ClipboardUtils.detectContentType("#fff")
        assertEquals(ClipboardContentType.COLOR, type2)

        val type3 = ClipboardUtils.detectContentType("#80FF5722")
        assertEquals(ClipboardContentType.COLOR, type3)
    }

    @Test
    fun testParseHexColor() {
        val c6 = ClipboardUtils.parseHexColor("#FF5722")
        assertNotNull(c6)
        assertEquals(0xFFFF5722L, c6)

        val c3 = ClipboardUtils.parseHexColor("#fff")
        assertNotNull(c3)
        assertEquals(0xFFFFFFFFL, c3)

        val c4 = ClipboardUtils.parseHexColor("#8abc")
        assertNotNull(c4)

        val invalid = ClipboardUtils.parseHexColor("#XYZ123")
        assertNull(invalid)

        val notAColor = ClipboardUtils.parseHexColor("Hello #world")
        assertNull(notAColor)
    }

    @Test
    fun testDetectContentTypeCodeSnippet() {
        val type1 = ClipboardUtils.detectContentType("fun calculateTotal(items: List<Item>) {\n    return items.sum()\n}")
        assertEquals(ClipboardContentType.CODE, type1)

        val type2 = ClipboardUtils.detectContentType("SELECT id, name FROM users WHERE active = 1")
        assertEquals(ClipboardContentType.CODE, type2)

        val type3 = ClipboardUtils.detectContentType("{\"user\": \"alice\", \"id\": 101}")
        assertEquals(ClipboardContentType.CODE, type3)
    }

    @Test
    fun testDetectContentTypePlainText() {
        val type = ClipboardUtils.detectContentType("Please let me know if you have any questions.")
        assertEquals(ClipboardContentType.TEXT, type)
    }

    @Test
    fun testCleanUrlTrackingParameters() {
        val dirtyUrl = "https://example.com/article?utm_source=newsletter&utm_medium=email&utm_campaign=summer&id=42"
        val cleanUrl = ClipboardUtils.cleanUrl(dirtyUrl)
        
        assertFalse("Cleaned URL must not contain utm_source", cleanUrl.contains("utm_source"))
        assertFalse("Cleaned URL must not contain utm_medium", cleanUrl.contains("utm_medium"))
        assertFalse("Cleaned URL must not contain utm_campaign", cleanUrl.contains("utm_campaign"))
        assertTrue("Cleaned URL must preserve functional parameter id=42", cleanUrl.contains("id=42"))
    }

    @Test
    fun testCleanUrlFbclidAndSi() {
        val dirtyUrl = "https://music.youtube.com/watch?v=dQw4w9WgXcQ&si=sampletrackingsi&fbclid=samplefbclid"
        val cleanUrl = ClipboardUtils.cleanUrl(dirtyUrl)

        assertFalse("Cleaned URL must not contain fbclid", cleanUrl.contains("fbclid"))
        assertFalse("Cleaned URL must not contain si tracking", cleanUrl.contains("si="))
        assertTrue("Cleaned URL must preserve v parameter", cleanUrl.contains("v=dQw4w9WgXcQ"))
    }

    @Test
    fun testCleanUrlTwitterShare() {
        val dirtyUrl = "https://x.com/compose/tweet?url=https://test.org&t=xyz123&s=20"
        val cleanUrl = ClipboardUtils.cleanUrl(dirtyUrl)

        assertFalse("Cleaned URL must strip t parameter", cleanUrl.contains("t=xyz123"))
        assertFalse("Cleaned URL must strip s parameter", cleanUrl.contains("s=20"))
    }

    @Test
    fun testExtractDomain() {
        val domain1 = ClipboardUtils.extractDomain("https://github.com/clicksylabs/clicksy")
        assertEquals("github.com", domain1)

        val domain2 = ClipboardUtils.extractDomain("www.youtube.com/watch?v=123")
        assertEquals("youtube.com", domain2)
    }

    @Test
    fun testTextTransformations() {
        val raw = "hello world"
        assertEquals("HELLO WORLD", ClipboardUtils.transformText(raw, TextTransformType.UPPERCASE))
        assertEquals("hello world", ClipboardUtils.transformText("HELLO WORLD", TextTransformType.LOWERCASE))
        assertEquals("Hello World", ClipboardUtils.transformText(raw, TextTransformType.TITLE_CASE))

        val multiline = "  line 1  \n\n  line 2  "
        val trimmed = ClipboardUtils.transformText(multiline, TextTransformType.TRIM)
        assertEquals("line 1\nline 2", trimmed)

        val multiWithNewlines = "The quick brown fox\njumps over\nthe lazy dog"
        val singleLine = ClipboardUtils.transformText(multiWithNewlines, TextTransformType.SINGLE_LINE)
        assertEquals("The quick brown fox jumps over the lazy dog", singleLine)
    }

    @Test
    fun testSensitiveApiKeysAndTokens() {
        val mockGithubPat = listOf("ghp", "mockTestToken12345678901234567890").joinToString("_")
        val mockFineGrainedPat = listOf("github", "pat", "mockTestToken1234567890").joinToString("_")
        val mockAwsKey = "AKIA" + "0000000000000000"
        val mockStripeKey = listOf("sk", "live", "mockTestKey12345678901234567890").joinToString("_")
        val mockPemKey = "-----" + "BEGIN RSA PRIVATE KEY-----\nMOCK_KEY_DATA"
        val mockJwt = "eyJ" + "hbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.mock_signature_12345"

        assertTrue(ClipboardUtils.isPotentialSecretOrApiKey(mockGithubPat))
        assertTrue(ClipboardUtils.isPotentialSecretOrApiKey(mockFineGrainedPat))
        assertTrue(ClipboardUtils.isPotentialSecretOrApiKey(mockAwsKey))
        assertTrue(ClipboardUtils.isPotentialSecretOrApiKey(mockStripeKey))
        assertTrue(ClipboardUtils.isPotentialSecretOrApiKey(mockPemKey))
        assertTrue(ClipboardUtils.isPotentialSecretOrApiKey(mockJwt))

        assertFalse(ClipboardUtils.isPotentialSecretOrApiKey("Normal message to a friend"))
        assertFalse(ClipboardUtils.isPotentialSecretOrApiKey("https://github.com/project"))
    }

    @Test
    fun testCategoryMatching() {
        val pinnedEntity = ClipboardEntity(id = 1, text = "Normal note", copiedTimestamp = 1000L, pinned = true)
        assertTrue(ClipboardUtils.matchesCategory(pinnedEntity, ClipboardCategory.PINNED))
        assertTrue(ClipboardUtils.matchesCategory(pinnedEntity, ClipboardCategory.ALL))

        val favEntity = ClipboardEntity(id = 2, text = "Starred note", copiedTimestamp = 1000L, favorite = true)
        assertTrue(ClipboardUtils.matchesCategory(favEntity, ClipboardCategory.FAVORITES))

        val linkEntity = ClipboardEntity(id = 3, text = "Check https://kotlinlang.org", copiedTimestamp = 1000L)
        assertTrue(ClipboardUtils.matchesCategory(linkEntity, ClipboardCategory.LINKS))

        val codeEntity = ClipboardEntity(id = 4, text = "fun add(a: Int, b: Int) = a + b", copiedTimestamp = 1000L)
        assertTrue(ClipboardUtils.matchesCategory(codeEntity, ClipboardCategory.CODE))

        val contactEntity = ClipboardEntity(id = 5, text = "Email me at support@clicksy.com", copiedTimestamp = 1000L)
        assertTrue(ClipboardUtils.matchesCategory(contactEntity, ClipboardCategory.CONTACTS))

        val multilineEntity = ClipboardEntity(id = 6, text = "Line 1\nLine 2\nLine 3", copiedTimestamp = 1000L)
        assertTrue(ClipboardUtils.matchesCategory(multilineEntity, ClipboardCategory.SNIPPETS))
    }

    @Test
    fun testFormatItemMetadata() {
        val meta1 = ClipboardUtils.formatItemMetadata("Hello world")
        assertTrue("Short text should report char count", meta1.contains("chars"))

        val meta2 = ClipboardUtils.formatItemMetadata("Line 1\nLine 2\nLine 3")
        assertTrue("Multiline text should report line count", meta2.contains("lines"))

        val meta3 = ClipboardUtils.formatItemMetadata("The quick brown fox jumps over the lazy dog")
        assertTrue("Long single line should report word count", meta3.contains("words"))
    }

    @Test
    fun testCreditCardPrivacyDetection() {
        // Standard Luhn valid test card
        assertTrue(ClipboardUtils.isPotentialCreditCard("49927398716"))
        
        // Random short number or words
        assertFalse(ClipboardUtils.isPotentialCreditCard("12345"))
        assertFalse(ClipboardUtils.isPotentialCreditCard("Hello world 1234"))
    }
}
