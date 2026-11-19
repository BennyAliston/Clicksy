package com.clicksy.keyboard.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DictionaryProviderTest {

    @Before
    fun setUp() {
        DictionaryProvider.initialize(null)
    }

    @Test
    fun testContractionExpansions() {
        val dontSuggestions = DictionaryProvider.getSuggestions("dont")
        assertTrue("Suggestions for 'dont' should contain 'don't'", dontSuggestions.any { it.equals("don't", ignoreCase = true) })

        val cantSuggestions = DictionaryProvider.getSuggestions("cant")
        assertTrue("Suggestions for 'cant' should contain 'can't'", cantSuggestions.any { it.equals("can't", ignoreCase = true) })

        val imSuggestions = DictionaryProvider.getSuggestions("im")
        assertTrue("Suggestions for 'im' should contain 'I'm'", imSuggestions.any { it.equals("I'm", ignoreCase = true) })

        val youreSuggestions = DictionaryProvider.getSuggestions("youre")
        assertTrue("Suggestions for 'youre' should contain 'you're'", youreSuggestions.any { it.equals("you're", ignoreCase = true) })

        val theyreSuggestions = DictionaryProvider.getSuggestions("theyre")
        assertTrue("Suggestions for 'theyre' should contain 'they're'", theyreSuggestions.any { it.equals("they're", ignoreCase = true) })

        val didntSuggestions = DictionaryProvider.getSuggestions("didnt")
        assertTrue("Suggestions for 'didnt' should contain 'didn't'", didntSuggestions.any { it.equals("didn't", ignoreCase = true) })

        val illSuggestions = DictionaryProvider.getSuggestions("ill")
        assertTrue("Suggestions for 'ill' should contain 'I'll' or 'ill'", illSuggestions.isNotEmpty())
    }

    @Test
    fun testCommonTypoCorrections() {
        val tehCorrection = DictionaryProvider.getAutoCorrection("teh")
        assertEquals("the", tehCorrection)

        val becuseCorrection = DictionaryProvider.getAutoCorrection("becuse")
        assertEquals("because", becuseCorrection)

        val mornignCorrection = DictionaryProvider.getAutoCorrection("mornign")
        assertEquals("morning", mornignCorrection)

        val thabksCorrection = DictionaryProvider.getAutoCorrection("thabks")
        assertEquals("thanks", thabksCorrection)

        val almistCorrection = DictionaryProvider.getAutoCorrection("almist")
        assertEquals("almost", almistCorrection)

        val woukdCorrection = DictionaryProvider.getAutoCorrection("woukd")
        assertEquals("would", woukdCorrection)

        val iCorrection = DictionaryProvider.getAutoCorrection("i")
        assertEquals("I", iCorrection)
    }

    @Test
    fun testSlangAndAbbreviationExpansions() {
        val idkSuggestions = DictionaryProvider.getSuggestions("idk")
        assertTrue("Suggestions for 'idk' should contain 'I don't know'", idkSuggestions.any { it.equals("I don't know", ignoreCase = true) })

        val tbhSuggestions = DictionaryProvider.getSuggestions("tbh")
        assertTrue("Suggestions for 'tbh' should contain 'to be honest'", tbhSuggestions.any { it.equals("to be honest", ignoreCase = true) })

        val btwSuggestions = DictionaryProvider.getSuggestions("btw")
        assertTrue("Suggestions for 'btw' should contain 'by the way'", btwSuggestions.any { it.equals("by the way", ignoreCase = true) })
    }

    @Test
    fun testNextWordBigramPredictions() {
        val goodPredictions = DictionaryProvider.getNextWordPredictions("good")
        assertTrue("Predictions for 'good' should contain 'morning' or 'night'",
            goodPredictions.contains("morning") || goodPredictions.contains("night"))

        val thankPredictions = DictionaryProvider.getNextWordPredictions("thank")
        assertTrue("Predictions for 'thank' should contain 'you'", thankPredictions.contains("you"))

        val howPredictions = DictionaryProvider.getNextWordPredictions("how")
        assertTrue("Predictions for 'how' should contain 'are' or 'is'",
            howPredictions.contains("are") || howPredictions.contains("is"))
    }

    @Test
    fun testTrigramNextWordPredictions() {
        // "looking forward" -> "to"
        val lookingForwardPreds = DictionaryProvider.getNextWordPredictions("forward", prevPrevWord = "looking")
        assertTrue("Predictions for 'looking forward' should predict 'to'", lookingForwardPreds.contains("to"))

        // "forward to" -> "hearing" or "seeing"
        val forwardToPreds = DictionaryProvider.getNextWordPredictions("to", prevPrevWord = "forward")
        assertTrue("Predictions for 'forward to' should predict 'hearing'", forwardToPreds.any { it.startsWith("hearing") })

        // "to hearing" -> "from"
        val toHearingPreds = DictionaryProvider.getNextWordPredictions("hearing", prevPrevWord = "to")
        assertTrue("Predictions for 'to hearing' should predict 'from'", toHearingPreds.contains("from"))

        // "please let" -> "me"
        val pleaseLetPreds = DictionaryProvider.getNextWordPredictions("let", prevPrevWord = "please")
        assertTrue("Predictions for 'please let' should predict 'me'", pleaseLetPreds.contains("me"))

        // "let me" -> "know"
        val letMePreds = DictionaryProvider.getNextWordPredictions("me", prevPrevWord = "let")
        assertTrue("Predictions for 'let me' should predict 'know'", letMePreds.contains("know"))

        // "thank you" -> "for" or "so" or "very"
        val thankYouPreds = DictionaryProvider.getNextWordPredictions("you", prevPrevWord = "thank")
        assertTrue("Predictions for 'thank you' should predict 'for', 'so', or 'very'",
            thankYouPreds.contains("for") || thankYouPreds.contains("so") || thankYouPreds.contains("very"))

        // "for your" -> "time" or "help" or "patience"
        val forYourPreds = DictionaryProvider.getNextWordPredictions("your", prevPrevWord = "for")
        assertTrue("Predictions for 'for your' should predict professional words like 'time', 'help', 'patience'",
            forYourPreds.contains("time") || forYourPreds.contains("help") || forYourPreds.contains("patience"))

        // "as soon" -> "as"
        val asSoonPreds = DictionaryProvider.getNextWordPredictions("soon", prevPrevWord = "as")
        assertTrue("Predictions for 'as soon' should predict 'as'", asSoonPreds.contains("as"))

        // "soon as" -> "possible"
        val soonAsPreds = DictionaryProvider.getNextWordPredictions("as", prevPrevWord = "soon")
        assertTrue("Predictions for 'soon as' should predict 'possible'", soonAsPreds.contains("possible"))

        // "feel free" -> "to"
        val feelFreePreds = DictionaryProvider.getNextWordPredictions("free", prevPrevWord = "feel")
        assertTrue("Predictions for 'feel free' should predict 'to'", feelFreePreds.contains("to"))
    }

    @Test
    fun testContextAwarePrefixCompletion() {
        val morningSuggestions = DictionaryProvider.getSuggestions("m", prevWord = "good")
        assertEquals("morning", morningSuggestions.firstOrNull()?.lowercase())

        val areSuggestions = DictionaryProvider.getSuggestions("a", prevWord = "how")
        assertEquals("are", areSuggestions.firstOrNull()?.lowercase())

        val youSuggestions = DictionaryProvider.getSuggestions("y", prevWord = "thank")
        assertEquals("you", youSuggestions.firstOrNull()?.lowercase())
    }

    @Test
    fun testContextAwareTrigramPrefixCompletion() {
        // "looking forward" + prefix "t" -> "to"
        val toSuggs = DictionaryProvider.getSuggestions("t", prevWord = "forward", prevPrevWord = "looking")
        assertEquals("to", toSuggs.firstOrNull()?.lowercase())

        // "forward to" + prefix "h" -> "hearing"
        val hearingSuggs = DictionaryProvider.getSuggestions("h", prevWord = "to", prevPrevWord = "forward")
        assertEquals("hearing", hearingSuggs.firstOrNull()?.lowercase())

        // "thank you for" + prefix "y" -> "your"
        val yourSuggs = DictionaryProvider.getSuggestions("y", prevWord = "for", prevPrevWord = "you")
        assertEquals("your", yourSuggs.firstOrNull()?.lowercase())

        // "for your" + prefix "t" -> "time"
        val timeSuggs = DictionaryProvider.getSuggestions("t", prevWord = "your", prevPrevWord = "for")
        assertEquals("time", timeSuggs.firstOrNull()?.lowercase())
    }

    @Test
    fun testSpatialProximityDistance() {
        val distAdj = KeyboardProximity.getSubstitutionCost('o', 'p')
        assertTrue("Substitution cost between adjacent keys 'o' and 'p' should be low (<= 0.5)", distAdj <= 0.5f)

        val distFar = KeyboardProximity.getSubstitutionCost('q', 'm')
        assertEquals(1.0f, distFar, 0.01f)

        val distTypo = KeyboardProximity.calculateWeightedDistance("thabks", "thanks")
        assertTrue("Weighted distance for adjacent typo 'thabks' -> 'thanks' should be <= 1.0", distTypo <= 1.0f)
    }

    @Test
    fun testInlineEmojiSuggestions() {
        val fireSuggestions = DictionaryProvider.getSuggestions("fire")
        assertTrue("Suggestions for 'fire' should contain fire emoji 🔥", fireSuggestions.contains("🔥"))

        val loveSuggestions = DictionaryProvider.getSuggestions("love")
        assertTrue("Suggestions for 'love' should contain heart emoji ❤️", loveSuggestions.contains("❤️"))

        val coffeeSuggestions = DictionaryProvider.getSuggestions("coffee")
        assertTrue("Suggestions for 'coffee' should contain coffee emoji ☕", coffeeSuggestions.contains("☕"))

        val dealSuggestions = DictionaryProvider.getSuggestions("deal")
        assertTrue("Suggestions for 'deal' should contain handshake emoji 🤝", dealSuggestions.contains("🤝"))
    }

    @Test
    fun testCasingPreservation() {
        val upper = DictionaryProvider.applyCasing("HEL", "hello")
        assertEquals("HELLO", upper)

        val title = DictionaryProvider.applyCasing("Hel", "hello")
        assertEquals("Hello", title)

        val lower = DictionaryProvider.applyCasing("hel", "hello")
        assertEquals("hello", lower)

        val pronoun = DictionaryProvider.applyCasing("im", "i'm")
        assertEquals("I'm", pronoun)
    }

    @Test
    fun testProperCasingPreservation() {
        // Tech platforms
        assertEquals("GitHub", DictionaryProvider.getAutoCorrection("github"))
        assertEquals("LinkedIn", DictionaryProvider.getAutoCorrection("linkedin"))
        assertEquals("YouTube", DictionaryProvider.getAutoCorrection("youtube"))
        assertEquals("WhatsApp", DictionaryProvider.getAutoCorrection("whatsapp"))

        // Days of week
        assertEquals("Monday", DictionaryProvider.getAutoCorrection("monday"))
        assertEquals("Friday", DictionaryProvider.getAutoCorrection("friday"))
        assertEquals("Sunday", DictionaryProvider.getAutoCorrection("sunday"))

        // Months
        assertEquals("January", DictionaryProvider.getAutoCorrection("january"))
        assertEquals("December", DictionaryProvider.getAutoCorrection("december"))

        // Languages
        assertEquals("English", DictionaryProvider.getAutoCorrection("english"))

        // Professional Acronyms
        assertEquals("ASAP", DictionaryProvider.getAutoCorrection("asap"))
        assertEquals("FYI", DictionaryProvider.getAutoCorrection("fyi"))
    }

    @Test
    fun testProfessionalAcronymExpansions() {
        val asapSuggs = DictionaryProvider.getSuggestions("asap")
        assertTrue("Suggestions for 'asap' should offer 'ASAP'", asapSuggs.contains("ASAP"))
        assertTrue("Suggestions for 'asap' should offer 'as soon as possible'", asapSuggs.contains("as soon as possible"))

        val fyiSuggs = DictionaryProvider.getSuggestions("fyi")
        assertTrue("Suggestions for 'fyi' should offer 'FYI'", fyiSuggs.contains("FYI"))
        assertTrue("Suggestions for 'fyi' should offer 'for your information'", fyiSuggs.contains("for your information"))

        val lmkSuggs = DictionaryProvider.getSuggestions("lmk")
        assertTrue("Suggestions for 'lmk' should offer 'let me know'", lmkSuggs.contains("let me know"))

        val wrtSuggs = DictionaryProvider.getSuggestions("wrt")
        assertTrue("Suggestions for 'wrt' should offer 'with respect to'", wrtSuggs.contains("with respect to"))

        val eodSuggs = DictionaryProvider.getSuggestions("eod")
        assertTrue("Suggestions for 'eod' should offer 'end of day'", eodSuggs.contains("end of day"))
    }

    @Test
    fun testProfessionalTypoCorrections() {
        assertEquals("receive", DictionaryProvider.getAutoCorrection("recieve"))
        assertEquals("separate", DictionaryProvider.getAutoCorrection("seperate"))
        assertEquals("until", DictionaryProvider.getAutoCorrection("untill"))
        assertEquals("necessary", DictionaryProvider.getAutoCorrection("neccessary"))
        assertEquals("definitely", DictionaryProvider.getAutoCorrection("definately"))
        assertEquals("truly", DictionaryProvider.getAutoCorrection("truely"))
        assertEquals("maintenance", DictionaryProvider.getAutoCorrection("maintainance"))
        assertEquals("occasion", DictionaryProvider.getAutoCorrection("ocassion"))
        assertEquals("successful", DictionaryProvider.getAutoCorrection("succesful"))
        assertEquals("calendar", DictionaryProvider.getAutoCorrection("calender"))
        assertEquals("colleague", DictionaryProvider.getAutoCorrection("collegue"))
        assertEquals("achieve", DictionaryProvider.getAutoCorrection("acheive"))

        // Split multi-word typo corrections
        assertEquals("a lot", DictionaryProvider.getAutoCorrection("alot"))
        assertEquals("in front", DictionaryProvider.getAutoCorrection("infront"))
        assertEquals("as well", DictionaryProvider.getAutoCorrection("aswell"))
        assertEquals("thank you", DictionaryProvider.getAutoCorrection("thankyou"))
        assertEquals("no one", DictionaryProvider.getAutoCorrection("noone"))
    }

    @Test
    fun testRepeatedCharacterNormalization() {
        // Fast touchscreen typing with elongated / repeated letters
        assertEquals("hello", DictionaryProvider.getAutoCorrection("helllo"))
        assertEquals("please", DictionaryProvider.getAutoCorrection("pleeease"))
        assertEquals("so", DictionaryProvider.getAutoCorrection("soooo"))
        assertEquals("really", DictionaryProvider.getAutoCorrection("reallly"))
        assertEquals("cool", DictionaryProvider.getAutoCorrection("cooool"))
        assertEquals("this", DictionaryProvider.getAutoCorrection("thiiis"))
        assertEquals("tomorrow", DictionaryProvider.getAutoCorrection("tomorowww"))
    }

    @Test
    fun testValidWordsNotAltered() {
        // Valid English words must NEVER be erroneously altered by autocorrect
        assertNull("Valid word 'were' must not be auto-corrected to 'we're'", DictionaryProvider.getAutoCorrection("were"))
        assertNull("Valid word 'well' must not be auto-corrected to 'we'll'", DictionaryProvider.getAutoCorrection("well"))
        assertNull("Valid word 'its' must not be auto-corrected to 'it's'", DictionaryProvider.getAutoCorrection("its"))
        assertNull("Valid word 'than' must not be auto-corrected", DictionaryProvider.getAutoCorrection("than"))
    }

    @Test
    fun testTrigramLearning() {
        DictionaryProvider.learnTrigram(null, "quarterly", "budget", "review")
        val preds = DictionaryProvider.getNextWordPredictions("budget", prevPrevWord = "quarterly")
        assertTrue("Predictions should reflect learned trigram 'review'", preds.contains("review"))
    }

    @Test
    fun testProfessionalMessageStarters() {
        val starters = DictionaryProvider.getNextWordPredictions("")
        assertTrue("Starters should contain professional greeting like 'Hi' or 'Hello'",
            starters.contains("Hi") || starters.contains("Hello"))
        assertTrue("Starters should contain courteous starter like 'Please' or 'Thank' or 'Hope'",
            starters.contains("Please") || starters.contains("Thank") || starters.contains("Hope") || starters.contains("Good"))
    }

    @Test
    fun testHinglishValidWordsNotCorrupted() {
        // High-frequency Hinglish words must NEVER be erroneously altered to English words by autocorrect
        assertNull("Valid Hinglish word 'kya' must not be auto-corrected", DictionaryProvider.getAutoCorrection("kya"))
        assertNull("Valid Hinglish word 'hai' must not be auto-corrected to 'has'", DictionaryProvider.getAutoCorrection("hai"))
        assertNull("Valid Hinglish word 'nahi' must not be auto-corrected", DictionaryProvider.getAutoCorrection("nahi"))
        assertNull("Valid Hinglish word 'kaise' must not be auto-corrected", DictionaryProvider.getAutoCorrection("kaise"))
        assertNull("Valid Hinglish word 'bhai' must not be auto-corrected", DictionaryProvider.getAutoCorrection("bhai"))
        assertNull("Valid Hinglish word 'yaar' must not be auto-corrected", DictionaryProvider.getAutoCorrection("yaar"))
        assertNull("Valid Hinglish word 'theek' must not be auto-corrected", DictionaryProvider.getAutoCorrection("theek"))
        assertNull("Valid Hinglish word 'thik' must not be auto-corrected", DictionaryProvider.getAutoCorrection("thik"))
        assertNull("Valid Hinglish word 'tum' must not be auto-corrected", DictionaryProvider.getAutoCorrection("tum"))
        assertNull("Valid Hinglish word 'mera' must not be auto-corrected", DictionaryProvider.getAutoCorrection("mera"))
        assertNull("Valid Hinglish word 'bohot' must not be auto-corrected", DictionaryProvider.getAutoCorrection("bohot"))
        assertNull("Valid Hinglish word 'bahut' must not be auto-corrected", DictionaryProvider.getAutoCorrection("bahut"))
        assertNull("Valid Hinglish word 'chalo' must not be auto-corrected", DictionaryProvider.getAutoCorrection("chalo"))
        assertNull("Valid Hinglish word 'haan' must not be auto-corrected", DictionaryProvider.getAutoCorrection("haan"))
    }

    @Test
    fun testHinglishChatTypoCorrections() {
        assertEquals("nahi", DictionaryProvider.getAutoCorrection("nhi"))
        assertEquals("bohot", DictionaryProvider.getAutoCorrection("bhot"))
        assertEquals("samajh", DictionaryProvider.getAutoCorrection("smjh"))
        assertEquals("karna", DictionaryProvider.getAutoCorrection("krna"))
        assertEquals("karo", DictionaryProvider.getAutoCorrection("kro"))
        assertEquals("raha", DictionaryProvider.getAutoCorrection("rha"))
        assertEquals("batao", DictionaryProvider.getAutoCorrection("btao"))
        assertEquals("mujhe", DictionaryProvider.getAutoCorrection("mko"))
        assertEquals("tujhe", DictionaryProvider.getAutoCorrection("tko"))
        assertEquals("aapko", DictionaryProvider.getAutoCorrection("apko"))
        assertEquals("theek", DictionaryProvider.getAutoCorrection("thk"))
        assertEquals("accha", DictionaryProvider.getAutoCorrection("acha"))
        assertEquals("please", DictionaryProvider.getAutoCorrection("plz"))
        assertEquals("bilkul", DictionaryProvider.getAutoCorrection("blkl"))
        assertEquals("shayad", DictionaryProvider.getAutoCorrection("shyd"))
        assertEquals("sahi", DictionaryProvider.getAutoCorrection("shi"))
        assertEquals("hello", DictionaryProvider.getAutoCorrection("hlo"))
        assertEquals("waise", DictionaryProvider.getAutoCorrection("vaise"))

        // Multi-word Hinglish typos
        assertEquals("kya haal", DictionaryProvider.getAutoCorrection("kyahaal"))
        assertEquals("theek hai", DictionaryProvider.getAutoCorrection("theekhai"))
        assertEquals("tension mat", DictionaryProvider.getAutoCorrection("tensionmat"))
    }

    @Test
    fun testHinglishCasualTrailingDoubleLetterNormalization() {
        assertEquals("bhai", DictionaryProvider.getAutoCorrection("bhaii"))
        assertEquals("yaar", DictionaryProvider.getAutoCorrection("yaarr"))
        assertEquals("chalo", DictionaryProvider.getAutoCorrection("chaloo"))
        assertEquals("suno", DictionaryProvider.getAutoCorrection("sunoo"))
        assertEquals("bolo", DictionaryProvider.getAutoCorrection("boloo"))
    }

    @Test
    fun testHinglishProperCasing() {
        assertEquals("Delhi", DictionaryProvider.getAutoCorrection("delhi"))
        assertEquals("Mumbai", DictionaryProvider.getAutoCorrection("mumbai"))
        assertEquals("Bangalore", DictionaryProvider.getAutoCorrection("bangalore"))
        assertEquals("Diwali", DictionaryProvider.getAutoCorrection("diwali"))
        assertEquals("Holi", DictionaryProvider.getAutoCorrection("holi"))
        assertEquals("India", DictionaryProvider.getAutoCorrection("india"))
    }

    @Test
    fun testHinglishNextWordBigrams() {
        val kyaPreds = DictionaryProvider.getNextWordPredictions("kya")
        assertTrue("Predictions for 'kya' should contain 'haal' or 'chal' or 'kar'",
            kyaPreds.contains("haal") || kyaPreds.contains("chal") || kyaPreds.contains("kar"))

        val kaisePreds = DictionaryProvider.getNextWordPredictions("kaise")
        assertTrue("Predictions for 'kaise' should contain 'ho'", kaisePreds.contains("ho"))

        val theekPreds = DictionaryProvider.getNextWordPredictions("theek")
        assertTrue("Predictions for 'theek' should contain 'hai'", theekPreds.contains("hai"))

        val tensionPreds = DictionaryProvider.getNextWordPredictions("tension")
        assertTrue("Predictions for 'tension' should contain 'mat'", tensionPreds.contains("mat"))

        val callPreds = DictionaryProvider.getNextWordPredictions("call")
        assertTrue("Predictions for 'call' should contain 'karo'", callPreds.contains("karo"))

        val locationPreds = DictionaryProvider.getNextWordPredictions("location")
        assertTrue("Predictions for 'location' should contain 'bhejo' or 'share' or 'send'",
            locationPreds.contains("bhejo") || locationPreds.contains("share") || locationPreds.contains("send"))
    }

    @Test
    fun testHinglishTrigramPredictions() {
        // "kya haal" -> "hai"
        val kyaHaalPreds = DictionaryProvider.getNextWordPredictions("haal", prevPrevWord = "kya")
        assertTrue("Predictions for 'kya haal' should predict 'hai'", kyaHaalPreds.contains("hai"))

        // "kya chal" -> "raha"
        val kyaChalPreds = DictionaryProvider.getNextWordPredictions("chal", prevPrevWord = "kya")
        assertTrue("Predictions for 'kya chal' should predict 'raha'", kyaChalPreds.contains("raha"))

        // "chal raha" -> "hai"
        val chalRahaPreds = DictionaryProvider.getNextWordPredictions("raha", prevPrevWord = "chal")
        assertTrue("Predictions for 'chal raha' should predict 'hai'", chalRahaPreds.contains("hai"))

        // "kaise ho" -> "bhai" or "yaar"
        val kaiseHoPreds = DictionaryProvider.getNextWordPredictions("ho", prevPrevWord = "kaise")
        assertTrue("Predictions for 'kaise ho' should predict 'bhai' or 'yaar'",
            kaiseHoPreds.contains("bhai") || kaiseHoPreds.contains("yaar"))

        // "kal milte" -> "hain" or "hai"
        val kalMiltePreds = DictionaryProvider.getNextWordPredictions("milte", prevPrevWord = "kal")
        assertTrue("Predictions for 'kal milte' should predict 'hain' or 'hai'",
            kalMiltePreds.contains("hain") || kalMiltePreds.contains("hai"))

        // "tension mat" -> "lo"
        val tensionMatPreds = DictionaryProvider.getNextWordPredictions("mat", prevPrevWord = "tension")
        assertTrue("Predictions for 'tension mat' should predict 'lo'", tensionMatPreds.contains("lo"))

        // "khana kha" -> "liya"
        val khanaKhaPreds = DictionaryProvider.getNextWordPredictions("kha", prevPrevWord = "khana")
        assertTrue("Predictions for 'khana kha' should predict 'liya'", khanaKhaPreds.contains("liya"))
    }

    @Test
    fun testHinglishInlineEmojiRecommendations() {
        val chaiSuggestions = DictionaryProvider.getSuggestions("chai")
        assertTrue("Suggestions for 'chai' should contain tea emoji ☕", chaiSuggestions.contains("☕"))

        val bhaiSuggestions = DictionaryProvider.getSuggestions("bhai")
        assertTrue("Suggestions for 'bhai' should contain hug/handshake emoji 🫂", bhaiSuggestions.contains("🫂"))

        val khanaSuggestions = DictionaryProvider.getSuggestions("khana")
        assertTrue("Suggestions for 'khana' should contain food emoji 🍲", khanaSuggestions.contains("🍲"))

        val namasteSuggestions = DictionaryProvider.getSuggestions("namaste")
        assertTrue("Suggestions for 'namaste' should contain folded hands emoji 🙏", namasteSuggestions.contains("🙏"))

        val badhiyaSuggestions = DictionaryProvider.getSuggestions("badhiya")
        assertTrue("Suggestions for 'badhiya' should contain thumbs up emoji 👍", badhiyaSuggestions.contains("👍"))
    }

    @Test
    fun testHinglishPrefixAutocompleteFastTrie() {
        val kySuggs = DictionaryProvider.getSuggestions("ky")
        assertTrue("Suggestions for 'ky' should contain 'kya' or 'kyun'",
            kySuggs.any { it.equals("kya", ignoreCase = true) || it.equals("kyun", ignoreCase = true) })

        val bhSuggs = DictionaryProvider.getSuggestions("bh")
        assertTrue("Suggestions for 'bh' should contain 'bhai' or 'bhi'",
            bhSuggs.any { it.equals("bhai", ignoreCase = true) || it.equals("bhi", ignoreCase = true) })

        val thSuggs = DictionaryProvider.getSuggestions("th")
        assertTrue("Suggestions for 'th' should contain 'the' or 'theek' or 'thik'",
            thSuggs.any { it.equals("the", ignoreCase = true) || it.equals("theek", ignoreCase = true) || it.equals("thik", ignoreCase = true) })
    }

    @Test
    fun testHinglishAbbreviationExpansions() {
        val tcSuggs = DictionaryProvider.getSuggestions("tc")
        assertTrue("Suggestions for 'tc' should contain 'take care'", tcSuggs.any { it.equals("take care", ignoreCase = true) })

        val gnSuggs = DictionaryProvider.getSuggestions("gn")
        assertTrue("Suggestions for 'gn' should contain 'good night'", gnSuggs.any { it.contains("good night", ignoreCase = true) })

        val gmSuggs = DictionaryProvider.getSuggestions("gm")
        assertTrue("Suggestions for 'gm' should contain 'good morning'", gmSuggs.any { it.contains("good morning", ignoreCase = true) })
    }

    @Test
    fun testHinglishKyaKrRheHoScenario() {
        // Prompt Scenario: "kya kr rhe ho"
        // 1. Recognize individual Hinglish words and offer appropriate spelling variants
        val kyaSuggs = DictionaryProvider.getSuggestions("kya")
        assertTrue("Suggestions for 'kya' should contain 'kya'", kyaSuggs.any { it.equals("kya", ignoreCase = true) })
        assertNull("Valid Hinglish word 'kya' must not be auto-corrected", DictionaryProvider.getAutoCorrection("kya"))

        val krSuggs = DictionaryProvider.getSuggestions("kr")
        assertTrue("Suggestions for 'kr' should offer canonical 'kar'", krSuggs.any { it.equals("kar", ignoreCase = true) })
        assertEquals("kar", DictionaryProvider.getAutoCorrection("kr"))

        val rheSuggs = DictionaryProvider.getSuggestions("rhe")
        assertTrue("Suggestions for 'rhe' should offer canonical 'rahe'", rheSuggs.any { it.equals("rahe", ignoreCase = true) })
        assertEquals("rahe", DictionaryProvider.getAutoCorrection("rhe"))

        // 2. Contextual prediction: after "kya kar", prefer contextually plausible words such as "rahe", "raha", or "rahi"
        val kyaKarPreds = DictionaryProvider.getNextWordPredictions("kar", prevPrevWord = "kya")
        assertTrue("Predictions after 'kya kar' should contain 'rahe' or 'raha' or 'rahi'",
            kyaKarPreds.contains("rahe") || kyaKarPreds.contains("raha") || kyaKarPreds.contains("rahi"))

        // 3. After "kar rahe", prefer "ho" or "hain"
        val karRahePreds = DictionaryProvider.getNextWordPredictions("rahe", prevPrevWord = "kar")
        assertTrue("Predictions after 'kar rahe' should contain 'ho' or 'hain'",
            karRahePreds.contains("ho") || karRahePreds.contains("hain"))
    }

    @Test
    fun testHinglishMNhiAaSktaScenario() {
        // Prompt Scenario: "m nhi aa skta"
        // Expected: offer "main nahi aa sakta" without forcibly rewriting the entire sentence
        val mSuggs = DictionaryProvider.getSuggestions("m")
        assertTrue("Suggestions for 'm' should offer 'main'", mSuggs.any { it.equals("main", ignoreCase = true) })

        val nhiSuggs = DictionaryProvider.getSuggestions("nhi")
        assertTrue("Suggestions for 'nhi' should offer 'nahi'", nhiSuggs.any { it.equals("nahi", ignoreCase = true) })
        assertEquals("nahi", DictionaryProvider.getAutoCorrection("nhi"))

        val aaSuggs = DictionaryProvider.getSuggestions("aa")
        assertTrue("Suggestions for 'aa' should offer 'aa'", aaSuggs.any { it.equals("aa", ignoreCase = true) })
        assertNull("Valid word 'aa' must not be forcibly altered", DictionaryProvider.getAutoCorrection("aa"))

        val sktaSuggs = DictionaryProvider.getSuggestions("skta")
        assertTrue("Suggestions for 'skta' should offer 'sakta'", sktaSuggs.any { it.equals("sakta", ignoreCase = true) })
        assertEquals("sakta", DictionaryProvider.getAutoCorrection("skta"))

        // Contextual prediction: "nahi aa" -> "sakta"
        val nahiAaPreds = DictionaryProvider.getNextWordPredictions("aa", prevPrevWord = "nahi")
        assertTrue("Predictions after 'nahi aa' should predict 'sakta'", nahiAaPreds.contains("sakta"))
    }

    @Test
    fun testHinglishGmShortcutPreservationScenario() {
        // Prompt Scenario: "gm"
        // Expected: suggest "good morning" while allowing the user to keep "gm"
        val gmSuggs = DictionaryProvider.getSuggestions("gm")
        assertTrue("Suggestions for 'gm' should suggest 'good morning'", gmSuggs.any { it.contains("good morning", ignoreCase = true) })
        assertTrue(gmSuggs.isNotEmpty())
    }

    @Test
    fun testEnglishIAmGoingContextualScenario() {
        // Prompt Scenario: "I am going"
        // Expected: offer plausible next-word candidates based on context
        val goingPreds = DictionaryProvider.getNextWordPredictions("going", prevPrevWord = "am")
        assertTrue("Predictions after 'I am going' should offer plausible candidates like 'to', 'home', 'there', or 'out'",
            goingPreds.contains("to") || goingPreds.contains("home") || goingPreds.contains("there") || goingPreds.contains("out"))
    }

    @Test
    fun testHinglishSpellingVariantsRecognition() {
        // "mai ghar ja rha hu" vs "main ghar ja raha hoon"
        val maiSuggs = DictionaryProvider.getSuggestions("mai")
        assertTrue("Suggestions for 'mai' should recognize 'main' or 'mai'",
            maiSuggs.any { it.equals("mai", ignoreCase = true) || it.equals("main", ignoreCase = true) })

        val rhaSuggs = DictionaryProvider.getSuggestions("rha")
        assertTrue("Suggestions for 'rha' should recognize 'raha'", rhaSuggs.any { it.equals("raha", ignoreCase = true) })
        assertEquals("raha", DictionaryProvider.getAutoCorrection("rha"))

        // "acha", "achha", "accha"
        assertEquals("accha", DictionaryProvider.getAutoCorrection("acha"))
        assertEquals("accha", DictionaryProvider.getAutoCorrection("achha"))
        assertNull("Canonical 'accha' must not be auto-corrected", DictionaryProvider.getAutoCorrection("accha"))

        // "mujhe nhi pta" vs "mujhe nahi pata"
        assertEquals("nahi", DictionaryProvider.getAutoCorrection("nhi"))
        val ptaSuggs = DictionaryProvider.getSuggestions("pta")
        assertTrue("Suggestions for 'pta' should offer 'pata'", ptaSuggs.any { it.equals("pata", ignoreCase = true) })

        // "kal milte h" vs "kal milte hain"
        val hSuggs = DictionaryProvider.getSuggestions("h", prevWord = "milte", prevPrevWord = "kal")
        assertTrue("Suggestions for 'h' after 'kal milte' should offer 'hain' or 'hai'",
            hSuggs.any { it.equals("hain", ignoreCase = true) || it.equals("hai", ignoreCase = true) })
    }

    @Test
    fun testNamesAndSlangNotErased() {
        // Popular cultural names and slang should not be mangled
        assertNull("Name 'Aarav' should not be mangled", DictionaryProvider.getAutoCorrection("Aarav"))
        assertNull("Name 'Pooja' should not be mangled", DictionaryProvider.getAutoCorrection("Pooja"))
        assertNull("Slang 'bro' should not be auto-corrected", DictionaryProvider.getAutoCorrection("bro"))
        assertNull("Slang 'yaar' should not be auto-corrected", DictionaryProvider.getAutoCorrection("yaar"))
        assertNull("Slang 'mast' should not be auto-corrected", DictionaryProvider.getAutoCorrection("mast"))
        assertNull("Slang 'jugaad' should not be auto-corrected", DictionaryProvider.getAutoCorrection("jugaad"))
    }

    @Test
    fun testCandidateLookupLatencyBenchmark() {
        // Warm up
        DictionaryProvider.getSuggestions("kya")
        DictionaryProvider.getSuggestions("hel")

        val queryPrefixes = listOf("k", "ky", "kya", "m", "mh", "b", "bh", "bha", "th", "the", "w", "wh", "wha", "g", "gm", "r", "rh")
        val startTime = System.nanoTime()
        val iterations = 100
        for (i in 0 until iterations) {
            val prefix = queryPrefixes[i % queryPrefixes.size]
            val results = DictionaryProvider.getSuggestions(prefix)
            assertTrue("Each lookup must return results", results.isNotEmpty())
        }
        val elapsedMs = (System.nanoTime() - startTime) / 1_000_000.0
        val avgLatencyMs = elapsedMs / iterations
        println("Performance benchmark: $iterations queries completed in ${String.format("%.2f", elapsedMs)} ms (avg ${String.format("%.3f", avgLatencyMs)} ms/lookup)")
        assertTrue("Average candidate lookup latency must be under 5ms, actual was $avgLatencyMs ms", avgLatencyMs < 5.0)
    }

    @Test
    fun testAutoSuggestionAndAutoCorrectionSeparation() {
        // 1. Auto-Suggestion capabilities: prefix completion & next-word prediction
        val prefixSuggestions = DictionaryProvider.getSuggestions("kya")
        assertTrue("Suggestions for 'kya' should provide completions or variants", prefixSuggestions.isNotEmpty())

        val nextWords = DictionaryProvider.getNextWordPredictions("kya")
        assertTrue("Next-word predictions after 'kya' should include common Hinglish words",
            nextWords.any { it.equals("kar", ignoreCase = true) || it.equals("kr", ignoreCase = true) || it.equals("baat", ignoreCase = true) })

        // 2. Auto-Correction capabilities: typo & shortcut replacement
        val tehCorrection = DictionaryProvider.getAutoCorrection("teh")
        assertEquals("Typo 'teh' should correct to 'the'", "the", tehCorrection)

        val nhiCorrection = DictionaryProvider.getAutoCorrection("nhi")
        assertEquals("Hinglish shortcut 'nhi' should correct to 'nahi'", "nahi", nhiCorrection)

        // 3. Verification of distinct behavior:
        // A valid word should NOT have an autocorrection forced upon it
        assertNull("Valid word 'orange' should return null for autocorrection", DictionaryProvider.getAutoCorrection("orange"))
        assertNull("Valid word 'water' should return null for autocorrection", DictionaryProvider.getAutoCorrection("water"))
        assertNull("Valid word 'kya' should return null for autocorrection", DictionaryProvider.getAutoCorrection("kya"))
        // But should still yield rich predictive suggestions
        val waterSuggestions = DictionaryProvider.getSuggestions("wat")
        assertTrue("Prefix 'wat' should yield suggestions including 'water'",
            waterSuggestions.any { it.equals("water", ignoreCase = true) })
    }
}
