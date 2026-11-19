# Clicksy Keyboard — Vocabulary & Dataset Pipeline Tools

This directory contains offline data ingestion, normalization, and dictionary generation scripts used to build the unified English + Hinglish dictionary (`clicksy_dictionary.json`) bundled with Clicksy Keyboard.

---

## 📁 Directory Structure

```
tools/
├── build_clicksy_dictionary.py     # Main build script to normalize and compile datasets
├── download_sources.py             # Reproducible dataset downloader with SHA-256 validation
├── clicksy_dictionary_schema.json  # JSON schema for validating dictionary structure
├── data_sources/                   # Raw source lexicons & transliteration pairs
│   ├── dakshina_hi/                # Google Dakshina Hindi transliteration lexicons
│   ├── english/                    # Hermit Dave 50k English frequency lexicon
│   └── openhinglish/               # Roman Hindi lexicons, SMS abbreviations, names & brands
└── README.md                       # This documentation file
```

---

## 🚀 Usage

### 1. Requirements

- Python 3.8+
- Required packages:
  ```bash
  pip install jsonschema
  ```

### 2. Downloading / Updating Sources (Optional)

To download or update the raw dataset files into `tools/data_sources/`:

```bash
python tools/download_sources.py
```

### 3. Building the Dictionary

To process raw data sources, normalize Unicode tokens, inject typo corrections, n-grams, and generate the final dictionary:

```bash
python tools/build_clicksy_dictionary.py
```

The script outputs to both:
- `tools/clicksy_dictionary.json`
- `app/src/main/assets/clicksy_dictionary.json`

And validates the resulting JSON against `clicksy_dictionary_schema.json`.

---

## 📜 Attribution & Licenses

All integrated datasets and their licenses (CC-BY-SA 4.0, MIT) are documented in **[ATTRIBUTION.md](../ATTRIBUTION.md)** in the project root.
