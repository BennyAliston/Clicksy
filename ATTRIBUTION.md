# Clicksy Keyboard - Dataset Attribution & Licensing

This document records the provenance, download URLs, licensing, and attribution notices for the datasets integrated into the Clicksy Keyboard Unified English + Hinglish Dictionary (`clicksy_dictionary.json`).

---

## 1. Google Dakshina Dataset (`dakshina_hi`)

- **Resource Name:** Dakshina Dataset v1.0 (Hindi Transliteration Lexicon)
- **Publisher / Authors:** Google Research (Roark, Sproat, Sahrawat, & Bao, 2020)
- **Publication Reference:** *"Processing South Asian Languages Written in the Latin Script: the Dakshina Dataset"* (LREC 2020)
- **Official Repository:** [https://github.com/google-research-datasets/dakshina](https://github.com/google-research-datasets/dakshina)
- **Source Archive URL:** `https://storage.googleapis.com/gresearch/dakshina/dakshina_dataset_v1.0.tar`
- **Extracted Resource Paths:**
  - `dakshina_dataset_v1.0/hi/lexicons/hi.translit.sampled.train.tsv` (1,326,377 bytes)
  - `dakshina_dataset_v1.0/hi/lexicons/hi.translit.sampled.dev.tsv` (125,512 bytes)
  - `dakshina_dataset_v1.0/hi/lexicons/hi.translit.sampled.test.tsv` (128,062 bytes)
- **License:** Creative Commons Attribution-ShareAlike 4.0 International ([CC-BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/))
- **Usage & Modification Notice:**
  - 53,064 native Devanagari-to-Roman Hindi transliteration pairs spanning 30,000 Hindi lemmas were parsed, normalized, and merged into the canonical variant mapping of `clicksy_dictionary.json`.
  - Transliteration pairs are recognized as valid spelling variants (`variants` attribute).
- **Attribution Statement:**
  > Based on the Dakshina Dataset (v1.0) developed by Google Research, licensed under CC-BY-SA 4.0.

---

## 2. OpenHinglish Dataset (`openhinglish`)

- **Resource Name:** OpenHinglish (Roman Hindi Vocabulary, SMS Abbreviations, Indian Named Entities & Brands)
- **Author / Maintainer:** Shankar Mishra (`shankarmishra/openhinglish`)
- **Official Repository:** [https://github.com/shankarmishra/openhinglish](https://github.com/shankarmishra/openhinglish)
- **Source URLs:**
  - Roman Hindi Lexicon: `https://raw.githubusercontent.com/shankarmishra/openhinglish/main/roman_hindi.tsv`
  - SMS & Chat Abbreviations: `https://raw.githubusercontent.com/shankarmishra/openhinglish/main/sms_abbrev.tsv`
  - Named Entities: `https://raw.githubusercontent.com/shankarmishra/openhinglish/main/names.tsv`
  - Brands & Products: `https://raw.githubusercontent.com/shankarmishra/openhinglish/main/brands.tsv`
- **License:** MIT License
- **Usage & Modification Notice:**
  - Natural Roman Hindi chat vocabulary, common casual abbreviations (`nhi`, `kr`, `bhot`, `smjh`), and Indian cultural names were integrated into `clicksy_dictionary.json` and the shortcut expansions table.
- **MIT License Notice:**
  > Copyright (c) 2023 Shankar Mishra
  > Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so.

---

## 3. English FrequencyWords Lexicon (`english`)

- **Resource Name:** FrequencyWords (Hermit Dave English Frequency Lexicon / OpenSubtitles 2018)
- **Author / Maintainer:** Hermit Dave (`hermitdave/FrequencyWords`)
- **Official Repository:** [https://github.com/hermitdave/FrequencyWords](https://github.com/hermitdave/FrequencyWords)
- **Source File URL:** `https://raw.githubusercontent.com/hermitdave/FrequencyWords/master/content/2018/en/en_50k.txt`
- **License:** MIT License
- **Usage & Modification Notice:**
  - Top calibrated English unigram frequencies were logarithmically normalized to an integer ranking score (4,000 to 24,800) and merged with common contraction handling (`dont` -> `don't`, `im` -> `I'm`, etc.) without overwriting valid English homographs (`were`, `well`, `its`).
- **MIT License Notice:**
  > Copyright (c) 2016-2018 Hermit Dave
  > Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so.

---

## 4. Summary of Unified Dictionary Metadata

The generated dictionary (`app/src/main/assets/clicksy_dictionary.json`) contains:
- **Total Entries:** 36,633
  - **Hinglish Entries:** 24,615
  - **English Entries:** 8,377
  - **Bilingual Entries:** 3,641
- **Shortcuts & Abbreviation Expansions:** 91 entries
- **Contextual Bigrams:** 168 pairs
- **Contextual Trigrams:** 103 triples
- **Cultural Proper Casing Tokens:** 351 entities
- **Inline Contextual Emojis:** 71 keywords
- **Offline / On-Device Guarantee:** Zero external network calls; fully autonomous, offline-first execution with local privacy-preserving user learning.
