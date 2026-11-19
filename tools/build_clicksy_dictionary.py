#!/usr/bin/env python3
"""
build_clicksy_dictionary.py - Unified Vocabulary Pipeline for Clicksy Keyboard.

Builds a normalized, versioned, multi-source English + Hinglish dictionary
named `clicksy_dictionary.json` adhering to `clicksy_dictionary_schema.json`.

Provenance:
1. OpenHinglish (MIT): Roman-Hindi vocabulary, SMS abbreviations, names, brands.
2. Google Dakshina Dataset (CC-BY-SA-4.0): Transliteration pairs and variant spellings.
3. Hermit Dave FrequencyWords (MIT): OpenSubtitles English frequency lexicon.
4. Curated Conversational N-Grams: Bigram and Trigram language models for code-switching.
"""

import os
import sys
import json
import re
import datetime
import unicodedata
from collections import defaultdict
import jsonschema

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
SOURCES_DIR = os.path.join(BASE_DIR, "data_sources")
SCHEMA_PATH = os.path.join(BASE_DIR, "clicksy_dictionary_schema.json")
OUTPUT_TOOLS = os.path.join(BASE_DIR, "clicksy_dictionary.json")
OUTPUT_ASSETS = os.path.join(BASE_DIR, "..", "app", "src", "main", "assets", "clicksy_dictionary.json")

os.makedirs(os.path.dirname(OUTPUT_ASSETS), exist_ok=True)

ZERO_WIDTH_REGEX = re.compile(r'[\u200B\u200C\u200D\uFEFF\uFFFC\u200E\u200F\u00AD]')

def clean_token(token: str) -> str:
    """Normalize unicode, strip invisible chars, and lowercase."""
    if not token:
        return ""
    norm = unicodedata.normalize('NFKC', token)
    norm = ZERO_WIDTH_REGEX.sub('', norm)
    return norm.strip().lower()

def is_valid_word(word: str) -> bool:
    """Check if word consists of valid Latin characters or standard contraction apostrophe."""
    if not word or len(word) > 30:
        return False
    return all(c.isalpha() or c == "'" for c in word)

# High-confidence typos and contractions that must resolve to their target
CORE_COMMON_TYPOS = {
    "teh": "the",
    "tge": "the",
    "thw": "the",
    "becuse": "because",
    "becasue": "because",
    "beacuse": "because",
    "mornign": "morning",
    "morining": "morning",
    "thabks": "thanks",
    "thaks": "thanks",
    "almist": "almost",
    "woukd": "would",
    "recieve": "receive",
    "seperate": "separate",
    "untill": "until",
    "neccessary": "necessary",
    "definately": "definitely",
    "truely": "truly",
    "maintainance": "maintenance",
    "ocassion": "occasion",
    "succesful": "successful",
    "calender": "calendar",
    "collegue": "colleague",
    "acheive": "achieve",
    "alot": "a lot",
    "infront": "in front",
    "aswell": "as well",
    "thankyou": "thank you",
    "noone": "no one",
    # Contractions
    "dont": "don't",
    "cant": "can't",
    "wont": "won't",
    "im": "I'm",
    "youre": "you're",
    "theyre": "they're",
    "didnt": "didn't",
    "couldnt": "couldn't",
    "wouldnt": "wouldn't",
    "shouldnt": "shouldn't",
    "isnt": "isn't",
    "arent": "aren't",
    "wasnt": "wasn't",
    "werent": "weren't",
    "hasnt": "hasn't",
    "havent": "haven't",
    "hadnt": "hadn't",
    # Hinglish chat typos
    "nhi": "nahi",
    "nh": "nahi",
    "bhot": "bohot",
    "boht": "bohot",
    "smjh": "samajh",
    "smjha": "samjha",
    "smjho": "samjho",
    "smjhe": "samjhe",
    "krna": "karna",
    "krne": "karne",
    "krte": "karte",
    "krti": "karti",
    "krta": "karta",
    "kro": "karo",
    "krunga": "karunga",
    "krungi": "karungi",
    "krenge": "karenge",
    "rha": "raha",
    "rhi": "rahi",
    "rhe": "rahe",
    "btao": "batao",
    "btana": "batana",
    "btaye": "bataye",
    "mko": "mujhe",
    "mjhe": "mujhe",
    "tko": "tujhe",
    "tjhe": "tujhe",
    "apko": "aapko",
    "sb": "sab",
    "sbko": "sabko",
    "shyd": "shayad",
    "shi": "sahi",
    "schi": "sachi",
    "blkl": "bilkul",
    "zrur": "zaroor",
    "zroor": "zaroor",
    "thk": "theek",
    "thek": "theek",
    "acha": "accha",
    "achha": "accha",
    "plz": "please",
    "plzz": "please",
    "hlo": "hello",
    "helo": "hello",
    "vaise": "waise",
    "vaisa": "waisa",
    "jyada": "zyada",
    "zarurat": "zaroorat",
    "kyahaal": "kya haal",
    "theekhai": "theek hai",
    "thikhai": "thik hai",
    "tensionmat": "tension mat",
    "kalmilte": "kal milte"
}

# Canonical Hinglish preference map
CANONICAL_HINGLISH_PREFERENCE = {
    "accha": ["achha", "acha"],
    "nahi": ["nahin", "nhi", "nh"],
    "bohot": ["bahut", "bhot", "boht"],
    "theek": ["thik", "thk", "thek"],
    "samajh": ["samjh", "smjh"],
    "karna": ["krna"],
    "karo": ["kro"],
    "karte": ["krte"],
    "karta": ["krta"],
    "karne": ["krne"],
    "kare": ["kre"],
    "karenge": ["krenge"],
    "karunga": ["krunga"],
    "karungi": ["krungi"],
    "raha": ["rha"],
    "rahi": ["rhi"],
    "rahe": ["rhe"],
    "batao": ["btao"],
    "batana": ["btana"],
    "bataye": ["btaye"],
    "bhai": ["bhaii", "bhaiya"],
    "yaar": ["yaarr", "yar"],
    "chalo": ["chaloo", "chlo"],
    "mujhe": ["mko", "mjhe"],
    "tujhe": ["tko", "tjhe"],
    "aapko": ["apko"],
    "sabko": ["sbko"],
    "sab": ["sb"],
    "bilkul": ["blkl"],
    "shayad": ["shyd"],
    "sahi": ["shi"],
    "sachi": ["schi"],
    "zaroor": ["zrur", "zroor"],
    "hoga": ["hga"],
    "hogi": ["hgi"],
    "honge": ["hnge"],
    "dekh": ["dkh"],
    "dekho": ["dkho"],
    "dekhna": ["dkhna"],
    "bolna": ["blna"],
    "suno": ["sno"],
    "sunna": ["snna"],
    "milna": ["mlna"],
    "milte": ["mlte"],
    "rukna": ["rkna"],
    "kuch": ["kuchh", "kch"],
    "kahan": ["kaha", "kha"],
    "wahan": ["wha", "vha"],
    "yahan": ["yha"],
    "idhar": ["idhr"],
    "udhar": ["udhr"],
    "tension": ["tnsn"],
    "problem": ["prblm"],
    "please": ["plz", "plzz"],
    "thanks": ["thx", "thnx"],
    "thank you": ["ty", "tysm"],
    "pahuche": ["phuche"],
    "pahuch": ["phuch"],
    "kyun": ["kyu"],
    "phir": ["fir"],
    "waise": ["vaise"],
    "waisa": ["vaisa"],
    "zyada": ["jyada"],
    "zaroorat": ["zarurat"],
    "college": ["clg"],
    "office": ["ofc"],
    "friend": ["frnd"],
    "friends": ["frnds"],
    "hello": ["helo", "hlo"],
    "good night": ["gn", "sd"],
    "good morning": ["gm"],
    "how are you": ["hru"],
    "what about you": ["wbu"],
    "what are you doing": ["wyd"],
    "take care": ["tc"]
}

CULTURAL_PROPER_CASING = {
    "india": "India", "bharat": "Bharat", "delhi": "Delhi", "mumbai": "Mumbai",
    "bangalore": "Bangalore", "bengaluru": "Bengaluru", "hyderabad": "Hyderabad",
    "kolkata": "Kolkata", "chennai": "Chennai", "pune": "Pune", "ahmedabad": "Ahmedabad",
    "jaipur": "Jaipur", "lucknow": "Lucknow", "chandigarh": "Chandigarh", "indore": "Indore",
    "bhopal": "Bhopal", "patna": "Patna", "diwali": "Diwali", "deepavali": "Deepavali",
    "holi": "Holi", "eid": "Eid", "rakhi": "Rakhi", "rakshabandhan": "Raksha Bandhan",
    "navratri": "Navratri", "dussehra": "Dussehra", "ganesh": "Ganesh", "ganpati": "Ganpati",
    "durga": "Durga", "puja": "Puja", "hindi": "Hindi", "hinglish": "Hinglish",
    "punjabi": "Punjabi", "bengali": "Bengali", "tamil": "Tamil", "telugu": "Telugu",
    "marathi": "Marathi", "gujarati": "Gujarati", "paytm": "Paytm", "zomato": "Zomato",
    "rapido": "Rapido", "zepto": "Zepto", "flipkart": "Flipkart", "swiggy": "Swiggy",
    "ola": "Ola", "phonepe": "PhonePe", "jio": "Jio", "blinkit": "Blinkit",
    "github": "GitHub", "linkedin": "LinkedIn", "youtube": "YouTube", "whatsapp": "WhatsApp",
    "instagram": "Instagram", "facebook": "Facebook", "twitter": "Twitter", "snapchat": "Snapchat",
    "telegram": "Telegram", "monday": "Monday", "tuesday": "Tuesday", "wednesday": "Wednesday",
    "thursday": "Thursday", "friday": "Friday", "saturday": "Saturday", "sunday": "Sunday",
    "january": "January", "february": "February", "march": "March", "april": "April",
    "may": "May", "june": "June", "july": "July", "august": "August", "september": "September",
    "october": "October", "november": "November", "december": "December", "asap": "ASAP",
    "fyi": "FYI", "id": "ID", "ok": "OK", "ai": "AI", "english": "English"
}

INLINE_EMOJIS = {
    "chai": "☕", "tea": "☕", "coffee": "☕", "khana": "🍲", "food": "🍲",
    "paani": "🥛", "pani": "🥛", "water": "🥛", "bhai": "🫂", "yaar": "🫂",
    "dost": "🫂", "friend": "🫂", "namaste": "🙏", "pranam": "🙏",
    "shukriya": "🙏", "dhanyawad": "🙏", "dhanyavaad": "🙏", "thanks": "🙏",
    "thank": "🙏", "please": "🙏", "pyaar": "❤️", "pyar": "❤️", "love": "❤️",
    "dil": "❤️", "heart": "❤️", "badhiya": "👍", "good": "👍", "mast": "👌",
    "perfect": "👌", "party": "🥳", "congrats": "🎉", "congratulations": "🎉",
    "birthday": "🎂", "bday": "🎂", "paisa": "💰", "paise": "💰", "money": "💰",
    "gaadi": "🚗", "gadi": "🚗", "car": "🚗", "ghar": "🏠", "home": "🏠",
    "mithai": "🍬", "meetha": "🍨", "mitha": "🍨", "nashta": "🥪", "nasta": "🥪",
    "sach": "💯", "sachi": "💯", "biryani": "🍗", "chawal": "🍚", "roti": "🫓",
    "neend": "😴", "sleep": "😴", "barish": "🌧️", "rain": "🌧️", "thand": "🥶",
    "cold": "🥶", "garmi": "🥵", "hot": "🥵", "gussa": "😡", "angry": "😡",
    "hasi": "😂", "hans": "😂", "laugh": "😂", "rona": "😭", "cry": "😭",
    "fire": "🔥", "deal": "🤝", "star": "⭐", "rocket": "🚀"
}

def load_dakshina_variants(dakshina_dir):
    devanagari_to_roman = defaultdict(lambda: defaultdict(int))
    tsv_files = [
        "hi.translit.sampled.train.tsv",
        "hi.translit.sampled.dev.tsv",
        "hi.translit.sampled.test.tsv"
    ]
    total_pairs = 0
    for fname in tsv_files:
        fpath = os.path.join(dakshina_dir, fname)
        if not os.path.exists(fpath):
            continue
        with open(fpath, "r", encoding="utf-8") as f:
            for line in f:
                parts = line.strip().split("\t")
                if len(parts) >= 2:
                    dev_word = parts[0].strip()
                    roman = clean_token(parts[1])
                    count = int(parts[2]) if len(parts) >= 3 and parts[2].isdigit() else 1
                    if is_valid_word(roman):
                        devanagari_to_roman[dev_word][roman] += count
                        total_pairs += 1

    print(f"Loaded {total_pairs} Dakshina transliteration pairs across {len(devanagari_to_roman)} Hindi lemmas.")

    dakshina_lexicon = {}
    for dev_word, roman_map in devanagari_to_roman.items():
        sorted_romans = sorted(roman_map.items(), key=lambda x: x[1], reverse=True)
        canonical = sorted_romans[0][0]
        total_attestation = sum(roman_map.values())
        variants = {r for r, _ in sorted_romans if r != canonical}

        if canonical not in dakshina_lexicon:
            dakshina_lexicon[canonical] = {
                "variants": variants,
                "count": total_attestation,
                "devanagari": dev_word
            }
        else:
            dakshina_lexicon[canonical]["variants"].update(variants)
            dakshina_lexicon[canonical]["count"] += total_attestation

    return dakshina_lexicon

def load_openhinglish(openhinglish_dir):
    roman_hindi_path = os.path.join(openhinglish_dir, "roman_hindi.tsv")
    sms_abbrev_path = os.path.join(openhinglish_dir, "sms_abbrev.tsv")
    names_path = os.path.join(openhinglish_dir, "names.tsv")
    brands_path = os.path.join(openhinglish_dir, "brands.tsv")

    hinglish_words = {}
    if os.path.exists(roman_hindi_path):
        with open(roman_hindi_path, "r", encoding="utf-8") as f:
            for line in f:
                parts = line.strip().split("\t")
                if len(parts) >= 1:
                    w = clean_token(parts[0])
                    if is_valid_word(w):
                        freq = int(parts[2]) if len(parts) >= 3 and parts[2].isdigit() else 8000
                        hinglish_words[w] = freq

    shortcuts = defaultdict(list)
    if os.path.exists(sms_abbrev_path):
        with open(sms_abbrev_path, "r", encoding="utf-8") as f:
            for line in f:
                parts = line.strip().split("\t")
                if len(parts) >= 2:
                    short = clean_token(parts[0])
                    exp = parts[1].strip()
                    if short and exp and exp not in shortcuts[short]:
                        shortcuts[short].append(exp)

    names = {}
    if os.path.exists(names_path):
        with open(names_path, "r", encoding="utf-8") as f:
            for line in f:
                parts = line.strip().split("\t")
                if len(parts) >= 2:
                    lower = clean_token(parts[0])
                    proper = parts[1].strip()
                    if lower and proper:
                        names[lower] = proper

    brands = {}
    if os.path.exists(brands_path):
        with open(brands_path, "r", encoding="utf-8") as f:
            for line in f:
                parts = line.strip().split("\t")
                if len(parts) >= 2:
                    lower = clean_token(parts[0])
                    proper = parts[1].strip()
                    if lower and proper:
                        brands[lower] = proper

    return {
        "hinglish_words": hinglish_words,
        "shortcuts": shortcuts,
        "names": names,
        "brands": brands
    }

def load_english_frequencies(english_file, limit=12000):
    words = {}
    if not os.path.exists(english_file):
        return words
    with open(english_file, "r", encoding="utf-8") as f:
        for line in f:
            parts = line.strip().split()
            if len(parts) >= 2:
                w = clean_token(parts[0])
                if is_valid_word(w) and parts[1].isdigit():
                    freq = int(parts[1])
                    if w not in words:
                        words[w] = freq
                        if len(words) >= limit:
                            break
    return words

def build_unified_dictionary():
    print("--- Starting Clicksy Unified Dictionary Generation ---")
    openhinglish = load_openhinglish(os.path.join(SOURCES_DIR, "openhinglish"))
    dakshina = load_dakshina_variants(os.path.join(SOURCES_DIR, "dakshina_hi"))
    english_words_raw = load_english_frequencies(os.path.join(SOURCES_DIR, "english", "en_50k.txt"), limit=12000)

    # Invert canonical preference map
    variant_to_canonical = {}
    for canon, vars_list in CANONICAL_HINGLISH_PREFERENCE.items():
        for v in vars_list:
            variant_to_canonical[v] = canon

    # Shortcuts map
    shortcuts = defaultdict(list)
    for k, v_list in openhinglish["shortcuts"].items():
        shortcuts[k].extend(v_list)

    core_shortcuts = {
        "gm": ["good morning", "Good morning ☕"],
        "gn": ["good night", "Good night 😴"],
        "idk": ["I don't know"],
        "brb": ["be right back"],
        "nhi": ["nahi"],
        "kr": ["kar"],
        "mtlb": ["matlab"],
        "hbd": ["happy birthday", "Happy Birthday! 🎂"],
        "tc": ["take care", "Take Care! 🫂"],
        "tysm": ["thank you so much", "Thank you so much! 🙏"],
        "tbh": ["to be honest"],
        "asap": ["as soon as possible", "ASAP"],
        "fyi": ["for your information", "FYI"],
        "lmk": ["let me know"],
        "wrt": ["with respect to"],
        "eod": ["end of day"],
        "m": ["main"],
        "b": ["bhi", "be"],
        "k": ["kya", "ok"],
        "r": ["are"],
        "u": ["you"],
        "ur": ["your"],
        "thx": ["thanks"],
        "thnx": ["thanks"],
        "ty": ["thank you"],
        "plz": ["please"],
        "bhot": ["bohot"],
        "smjh": ["samajh"],
        "rha": ["raha"],
        "rhi": ["rahi"],
        "rhe": ["rahe"],
        "btao": ["batao"],
        "mko": ["mujhe"],
        "tko": ["tujhe"],
        "apko": ["aapko"],
        "kl": ["kal"],
        "aj": ["aaj"],
        "thk": ["theek"],
        "acha": ["accha"],
        "achha": ["accha"]
    }
    for k, expansions in core_shortcuts.items():
        for exp in expansions:
            if exp not in shortcuts[k]:
                shortcuts[k].append(exp)

    # Proper casing map
    proper_casing = dict(CULTURAL_PROPER_CASING)
    for k, v in openhinglish["names"].items():
        proper_casing[k] = v
    for k, v in openhinglish["brands"].items():
        proper_casing[k] = v

    # Build entries map
    entries = {}

    tier1_hinglish = {
        "kya": 24500, "hai": 24500, "hain": 23500, "ho": 24000, "hoon": 22500, "hun": 22000,
        "tha": 23000, "thi": 22500, "the": 22500, "nahi": 24000, "nahin": 21500, "haan": 23500,
        "han": 22000, "kaise": 23500, "kaisa": 22500, "kaisi": 22000, "kahan": 23000, "kaha": 22500,
        "kab": 23000, "kyun": 23500, "kyu": 23000, "kaun": 22000, "mera": 23500, "meri": 23000,
        "mere": 23000, "tera": 23000, "teri": 22500, "tere": 22500, "uska": 22000, "uski": 21500,
        "uske": 22000, "hum": 23000, "tum": 23500, "aap": 24000, "yeh": 23500, "ye": 24000,
        "woh": 23500, "wo": 24000, "apna": 23000, "apni": 22500, "apne": 22500, "kar": 24000,
        "karo": 23500, "karna": 23500, "karne": 23000, "karta": 22500, "karti": 22000, "karte": 22500,
        "kiya": 23000, "raha": 24000, "rahi": 23500, "rahe": 23500, "hoga": 23000, "hogi": 22500,
        "honge": 22000, "chalo": 24000, "chal": 23500, "bhai": 23500, "yaar": 23500, "theek": 23500,
        "thik": 23000, "accha": 23500, "bohot": 23000, "bahut": 23000, "batana": 22500, "batao": 23500,
        "mujhe": 23500, "tujhe": 23000, "aapko": 23000, "ghar": 23000, "aaj": 23500, "kal": 23500,
        "milte": 23000, "samajh": 23000, "pata": 23500, "matlab": 22500, "dekh": 23000, "dekho": 23000,
        "sakta": 22500, "sakti": 22000, "sakte": 22500, "aana": 23000, "jaana": 22500, "bhejo": 23500
    }

    all_hinglish_keys = set(CANONICAL_HINGLISH_PREFERENCE.keys()) | set(openhinglish["hinglish_words"].keys()) | set(dakshina.keys())

    # Map target word -> list of typos from CORE_COMMON_TYPOS
    typos_by_target = defaultdict(list)
    for typo, target in CORE_COMMON_TYPOS.items():
        typos_by_target[target].append(typo)

    for word in all_hinglish_keys:
        if not is_valid_word(word):
            continue

        canonical = variant_to_canonical.get(word, word)
        variants = set(CANONICAL_HINGLISH_PREFERENCE.get(canonical, []))

        dak_entry = dakshina.get(canonical) or dakshina.get(word)
        if dak_entry:
            variants.update(dak_entry["variants"])

        if canonical in tier1_hinglish:
            freq = tier1_hinglish[canonical]
        elif canonical in openhinglish["hinglish_words"]:
            freq = max(12000, min(24000, openhinglish["hinglish_words"][canonical] * 2))
        elif dak_entry:
            freq = max(8000, min(22000, 8000 + dak_entry["count"] * 50))
        else:
            freq = 10000

        has_dakshina = bool(dak_entry)
        has_openhinglish = (canonical in openhinglish["hinglish_words"]) or (word in openhinglish["hinglish_words"])

        if has_dakshina and has_openhinglish:
            source = "OpenHinglish+Dakshina"
            license_type = "CC-BY-SA-4.0"
        elif has_dakshina:
            source = "Dakshina"
            license_type = "CC-BY-SA-4.0"
        else:
            source = "OpenHinglish"
            license_type = "MIT"

        short_forms = [v for v in variants if len(v) <= 3 and len(v) < len(canonical)]
        # Corrections: ONLY genuine typos for this target, NOT ambiguous transliteration variants
        corrections = typos_by_target.get(canonical, [])

        clean_variants = sorted([v for v in variants if v != canonical and is_valid_word(v)])

        if canonical not in entries:
            entries[canonical] = {
                "word": canonical,
                "language": "Hinglish",
                "variants": clean_variants,
                "frequency": freq,
                "shortForms": sorted(list(set(short_forms))),
                "corrections": sorted(list(set(corrections))),
                "source": source,
                "license": license_type
            }
        else:
            existing = entries[canonical]
            existing["frequency"] = max(existing["frequency"], freq)
            existing["variants"] = sorted(list(set(existing["variants"] + clean_variants)))
            existing["shortForms"] = sorted(list(set(existing["shortForms"] + short_forms)))
            existing["corrections"] = sorted(list(set(existing["corrections"] + corrections)))
            if "Dakshina" in source:
                existing["source"] = "OpenHinglish+Dakshina"
                existing["license"] = "CC-BY-SA-4.0"

    print(f"Compiled {len(entries)} Hinglish entries.")

    # 2. English words
    max_raw_eng = max(english_words_raw.values()) if english_words_raw else 1
    english_word_count = 0
    bilingual_count = 0

    for w, raw_freq in english_words_raw.items():
        if not is_valid_word(w):
            continue

        norm_score = (raw_freq / max_raw_eng) ** 0.25
        calibrated_freq = int(4000 + norm_score * 20800)

        # Contractions check
        corr = typos_by_target.get(w, [])

        if w in entries:
            entry = entries[w]
            entry["language"] = "Bilingual"
            entry["frequency"] = max(entry["frequency"], calibrated_freq)
            if corr:
                entry["corrections"] = sorted(list(set(entry["corrections"] + corr)))
            bilingual_count += 1
        else:
            entries[w] = {
                "word": w,
                "language": "English",
                "variants": [],
                "frequency": calibrated_freq,
                "shortForms": [],
                "corrections": corr,
                "source": "FrequencyWords",
                "license": "MIT"
            }
            english_word_count += 1

    print(f"Added {english_word_count} English entries, {bilingual_count} Bilingual entries.")

    # Ensure contractions like "don't", "can't", "I'm" are present and linked to typos
    contractions_to_ensure = [
        ("don't", "dont"), ("can't", "cant"), ("won't", "wont"), ("i'm", "im"),
        ("you're", "youre"), ("they're", "theyre"),
        ("didn't", "didnt"), ("couldn't", "couldnt"),
        ("wouldn't", "wouldnt"), ("shouldn't", "shouldnt"), ("isn't", "isnt")
    ]
    for c_word, c_typo in contractions_to_ensure:
        if c_word in entries:
            if c_typo not in entries[c_word]["corrections"]:
                entries[c_word]["corrections"].append(c_typo)
        else:
            entries[c_word] = {
                "word": c_word,
                "language": "English",
                "variants": [],
                "frequency": 24000,
                "shortForms": [],
                "corrections": [c_typo],
                "source": "FrequencyWords",
                "license": "MIT"
            }

    # 3. Proper casing entities
    for lower, proper in proper_casing.items():
        if lower not in entries and is_valid_word(lower):
            entries[lower] = {
                "word": lower,
                "language": "Hinglish" if lower in openhinglish["names"] or lower in openhinglish["brands"] else "English",
                "variants": [],
                "frequency": 18000,
                "shortForms": [],
                "corrections": [],
                "source": "OpenHinglish",
                "license": "MIT"
            }

    # 4. Bigrams
    bigrams = {
        "kya": {"haal": 200, "chal": 195, "kar": 190, "baat": 180, "hua": 175, "hai": 170, "plan": 160, "batao": 155, "scene": 145},
        "kaise": {"ho": 220, "hai": 180, "karein": 165, "batao": 145},
        "kahan": {"ho": 210, "hai": 185, "gaye": 170, "ja": 160, "rahe": 155, "milte": 130},
        "theek": {"hai": 220, "ho": 170, "tha": 150},
        "thik": {"hai": 220, "ho": 170, "bhai": 150, "yaar": 140},
        "haan": {"bhai": 190, "yaar": 185, "theek": 170, "bol": 160, "sahi": 145},
        "nahi": {"yaar": 190, "bhai": 185, "hai": 180, "pata": 175, "hoga": 160, "karna": 155, "aa": 150},
        "mujhe": {"bhi": 190, "nahi": 185, "batao": 170, "pata": 165, "call": 145},
        "tujhe": {"kya": 195, "pata": 170, "nahi": 165},
        "aap": {"kaise": 210, "kahan": 180, "batao": 175, "kya": 170},
        "tum": {"kaise": 200, "kahan": 185, "kya": 180, "kab": 150},
        "aaj": {"ka": 190, "kya": 185, "milte": 170, "sham": 160, "free": 140},
        "kal": {"milte": 195, "aana": 175, "karenge": 170, "subah": 165, "baat": 140},
        "chalo": {"theek": 195, "thik": 190, "milte": 185, "phir": 170},
        "bohot": {"achha": 200, "accha": 195, "badhiya": 185, "sahi": 175, "shukriya": 165},
        "bahut": {"achha": 200, "accha": 195, "badhiya": 185, "sahi": 175},
        "koi": {"baat": 210, "nahi": 190, "problem": 175, "scene": 160},
        "tension": {"mat": 220, "nahi": 160, "hai": 150},
        "call": {"karo": 200, "karna": 185, "karta": 170, "karein": 160},
        "location": {"bhejo": 210, "share": 200, "send": 190},
        "free": {"ho": 210, "hoke": 190, "hai": 170, "time": 160},
        "ghar": {"pe": 200, "par": 185, "pahuch": 180, "ja": 170},
        "mai": {"ghar": 210, "aa": 200, "nahi": 190, "bhi": 180},
        "main": {"ghar": 210, "nahi": 200, "aa": 195, "bhi": 180},
        "bhai": {"kya": 195, "kahan": 190, "sun": 185, "ek": 175},
        "yaar": {"kya": 195, "kahan": 190, "suno": 175, "matlab": 170},
        "good": {"morning": 220, "night": 210, "afternoon": 190, "job": 180, "to": 170},
        "thank": {"you": 230},
        "how": {"are": 220, "is": 200, "was": 190, "about": 185},
        "looking": {"forward": 220, "for": 200, "good": 180, "at": 175},
        "please": {"let": 210, "find": 200, "call": 190, "send": 185, "help": 180},
        "let": {"me": 220, "us": 200, "you": 180},
        "as": {"soon": 220, "well": 210, "per": 200, "always": 185},
        "feel": {"free": 220, "good": 190, "like": 185},
        "i": {"am": 220, "have": 210, "will": 205, "was": 200, "can": 195, "think": 190, "would": 185},
        "we": {"are": 220, "will": 210, "can": 205, "have": 200},
        "you": {"are": 220, "can": 210, "have": 205, "will": 195},
        "for": {"your": 230, "you": 210, "the": 190, "me": 180, "sure": 170},
        "going": {"to": 240, "home": 210, "there": 200, "out": 195}
    }

    # 5. Trigrams
    trigrams = {
        "kya haal": {"hai": 220, "chal": 160},
        "kya chal": {"raha": 220},
        "chal raha": {"hai": 220, "tha": 160},
        "kya kar": {"rahe": 220, "raha": 210, "rahi": 200, "sakte": 170},
        "kar rahe": {"ho": 220, "the": 195, "hain": 190},
        "kar raha": {"hoon": 210, "hu": 205, "tha": 195, "hai": 190},
        "kaise ho": {"bhai": 200, "yaar": 195, "aap": 185},
        "kahan ho": {"bhai": 200, "yaar": 195, "abhi": 185},
        "ja rahe": {"ho": 210, "the": 185, "hain": 180},
        "aa rahe": {"ho": 210, "the": 185, "hain": 180},
        "theek hai": {"bhai": 200, "yaar": 195, "toh": 185},
        "thik hai": {"bhai": 200, "yaar": 195, "toh": 185},
        "nahi pata": {"mujhe": 200, "bhai": 190, "yaar": 185},
        "mujhe nahi": {"pata": 220, "chahiye": 190, "lagta": 185},
        "kal milte": {"hain": 210, "hai": 200, "yaar": 190, "bhai": 185},
        "chalo theek": {"hai": 220},
        "tension mat": {"lo": 220, "lena": 200},
        "call karo": {"mujhe": 210, "abhi": 195, "bhai": 190},
        "location share": {"karo": 210, "karna": 190},
        "free ho": {"to": 210, "toh": 205, "kya": 195},
        "mai ghar": {"ja": 210, "pahuch": 190},
        "main ghar": {"ja": 210, "raha": 200, "pahuch": 190},
        "ghar ja": {"raha": 220, "rahe": 210, "rahi": 200},
        "aa sakta": {"hoon": 220, "hu": 210},
        "nahi aa": {"sakta": 220, "sakti": 210, "paunga": 200},
        "looking forward": {"to": 230},
        "forward to": {"hearing": 220, "seeing": 210, "meeting": 205},
        "to hearing": {"from": 230},
        "hearing from": {"you": 230},
        "please let": {"me": 220, "us": 200},
        "let me": {"know": 230},
        "thank you": {"for": 220, "so": 215, "very": 200},
        "you for": {"your": 230, "the": 210, "all": 200},
        "for your": {"time": 220, "help": 215, "patience": 210, "support": 205},
        "as soon": {"as": 230},
        "soon as": {"possible": 230},
        "feel free": {"to": 230},
        "i am": {"going": 220, "doing": 210, "coming": 205, "happy": 200, "sure": 195, "fine": 190},
        "am going": {"to": 240, "home": 215, "there": 205, "out": 200},
        "i would": {"like": 220, "love": 210, "appreciate": 205}
    }

    # Sort words by frequency descending
    sorted_words = sorted(entries.values(), key=lambda x: x["frequency"], reverse=True)

    hinglish_count = sum(1 for w in sorted_words if w["language"] == "Hinglish")
    english_final_count = sum(1 for w in sorted_words if w["language"] == "English")
    bilingual_final_count = sum(1 for w in sorted_words if w["language"] == "Bilingual")

    dictionary_data = {
        "$schema": "./clicksy_dictionary_schema.json",
        "version": "1.0.0",
        "generatedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "description": "Clicksy Keyboard unified English + Hinglish dictionary and prediction vocabulary",
        "metadata": {
            "totalWords": len(sorted_words),
            "englishWordCount": english_final_count + bilingual_final_count,
            "hinglishWordCount": hinglish_count + bilingual_final_count,
            "totalShortcuts": len(shortcuts),
            "totalBigrams": sum(len(v) for v in bigrams.values()),
            "totalTrigrams": sum(len(v) for v in trigrams.values()),
            "sources": [
                {
                    "name": "OpenHinglish",
                    "url": "https://github.com/shankarmishra/openhinglish",
                    "license": "MIT",
                    "provenance": "src/openhinglish/data/lexicons/roman_hindi.tsv, sms_abbrev.tsv, names.tsv, brands.tsv"
                },
                {
                    "name": "Google Dakshina Dataset",
                    "url": "https://github.com/google-research-datasets/dakshina",
                    "license": "CC-BY-SA-4.0",
                    "provenance": "dakshina_dataset_v1.0/hi/lexicons/hi.translit.sampled.*.tsv"
                },
                {
                    "name": "FrequencyWords English",
                    "url": "https://github.com/hermitdave/FrequencyWords",
                    "license": "MIT",
                    "provenance": "content/2018/en/en_50k.txt"
                }
            ]
        },
        "shortcuts": dict(sorted(shortcuts.items())),
        "words": sorted_words,
        "bigrams": bigrams,
        "trigrams": trigrams,
        "properCasing": proper_casing,
        "emojis": INLINE_EMOJIS
    }

    # Validate against JSON schema
    print("\n--- Validating Dictionary Against Schema ---")
    with open(SCHEMA_PATH, "r", encoding="utf-8") as sf:
        schema = json.load(sf)
    jsonschema.validate(instance=dictionary_data, schema=schema)
    print("[VALIDATION PASSED] Schema compliance confirmed successfully.")

    for out_path in [OUTPUT_TOOLS, OUTPUT_ASSETS]:
        with open(out_path, "w", encoding="utf-8") as out_f:
            json.dump(dictionary_data, out_f, ensure_ascii=False, indent=2)
        print(f"[SAVED] {out_path} ({os.path.getsize(out_path)} bytes)")

    print("\n=== Generation Summary ===")
    print(f"Total entries: {len(sorted_words)}")
    print(f"  - Hinglish entries: {hinglish_count}")
    print(f"  - English entries:  {english_final_count}")
    print(f"  - Bilingual entries:{bilingual_final_count}")
    print(f"Total shortcuts: {len(shortcuts)}")
    print(f"Total bigrams:   {sum(len(v) for v in bigrams.values())}")
    print(f"Total trigrams:  {sum(len(v) for v in trigrams.values())}")
    print(f"Proper casing tokens: {len(proper_casing)}")
    print(f"Inline emoji tokens:  {len(INLINE_EMOJIS)}")

if __name__ == "__main__":
    build_unified_dictionary()
