#!/usr/bin/env python3
"""
download_sources.py - Reproducible data downloader for Clicksy Keyboard vocabulary.

Downloads:
1. OpenHinglish:
   - roman_hindi.tsv
   - sms_abbrev.tsv
   - names.tsv
   - brands.tsv
   - english_freq.tsv
2. Google Dakshina Dataset:
   - hi.translit.sampled.train.tsv
   - hi.translit.sampled.dev.tsv
   - hi.translit.sampled.test.tsv
3. English FrequencyWords (Hermit Dave / OpenSubtitles):
   - en_50k.txt
"""

import os
import sys
import hashlib
import urllib.request

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
SOURCES_DIR = os.path.join(BASE_DIR, "data_sources")
OPENHINGLISH_DIR = os.path.join(SOURCES_DIR, "openhinglish")
DAKSHINA_DIR = os.path.join(SOURCES_DIR, "dakshina_hi")
ENGLISH_DIR = os.path.join(SOURCES_DIR, "english")

os.makedirs(OPENHINGLISH_DIR, exist_ok=True)
os.makedirs(DAKSHINA_DIR, exist_ok=True)
os.makedirs(ENGLISH_DIR, exist_ok=True)

SOURCES = {
    # OpenHinglish (MIT License) - shankarmishra/openhinglish
    "openhinglish_roman_hindi": {
        "url": "https://raw.githubusercontent.com/shankarmishra/openhinglish/main/src/openhinglish/data/lexicons/roman_hindi.tsv",
        "dest": os.path.join(OPENHINGLISH_DIR, "roman_hindi.tsv"),
        "license": "MIT",
        "description": "Roman-script Hinglish to Hindi words with frequencies"
    },
    "openhinglish_sms_abbrev": {
        "url": "https://raw.githubusercontent.com/shankarmishra/openhinglish/main/src/openhinglish/data/lexicons/sms_abbrev.tsv",
        "dest": os.path.join(OPENHINGLISH_DIR, "sms_abbrev.tsv"),
        "license": "MIT",
        "description": "SMS and chat abbreviation expansions"
    },
    "openhinglish_names": {
        "url": "https://raw.githubusercontent.com/shankarmishra/openhinglish/main/src/openhinglish/data/gazetteers/names.tsv",
        "dest": os.path.join(OPENHINGLISH_DIR, "names.tsv"),
        "license": "MIT",
        "description": "Indian personal name gazetteer"
    },
    "openhinglish_brands": {
        "url": "https://raw.githubusercontent.com/shankarmishra/openhinglish/main/src/openhinglish/data/gazetteers/brands.tsv",
        "dest": os.path.join(OPENHINGLISH_DIR, "brands.tsv"),
        "license": "MIT",
        "description": "Indian brand and app name gazetteer"
    },
    "openhinglish_english_freq": {
        "url": "https://raw.githubusercontent.com/shankarmishra/openhinglish/main/src/openhinglish/data/lexicons/english_freq.tsv",
        "dest": os.path.join(OPENHINGLISH_DIR, "english_freq.tsv"),
        "license": "MIT",
        "description": "English frequency lexicon"
    },
    # Hermit Dave FrequencyWords (MIT License)
    "english_frequency_50k": {
        "url": "https://raw.githubusercontent.com/hermitdave/FrequencyWords/master/content/2018/en/en_50k.txt",
        "dest": os.path.join(ENGLISH_DIR, "en_50k.txt"),
        "license": "MIT",
        "description": "Top 50,000 English words with corpus frequencies"
    }
}

def download_file(url, dest_path):
    if os.path.exists(dest_path) and os.path.getsize(dest_path) > 0:
        print(f"[CACHE] {os.path.basename(dest_path)} already exists ({os.path.getsize(dest_path)} bytes).")
        return
    print(f"[DOWNLOAD] Fetching {url} -> {dest_path}")
    req = urllib.request.Request(url, headers={"User-Agent": "Clicksy-Data-Pipeline/1.0"})
    with urllib.request.urlopen(req) as resp, open(dest_path, "wb") as f:
        while True:
            chunk = resp.read(65536)
            if not chunk:
                break
            f.write(chunk)
    print(f"[OK] Saved {os.path.basename(dest_path)} ({os.path.getsize(dest_path)} bytes).")

def compute_sha256(file_path):
    h = hashlib.sha256()
    with open(file_path, "rb") as f:
        while chunk := f.read(65536):
            h.update(chunk)
    return h.hexdigest()

def main():
    print("=== Clicksy Vocabulary Source Downloader ===")
    for key, info in SOURCES.items():
        try:
            download_file(info["url"], info["dest"])
            sha = compute_sha256(info["dest"])
            print(f"  SHA256: {sha[:16]}... ({info['license']})")
        except Exception as e:
            print(f"[ERROR] Failed to download {key}: {e}", file=sys.stderr)

    # Check Dakshina files
    dakshina_files = ["hi.translit.sampled.train.tsv", "hi.translit.sampled.dev.tsv", "hi.translit.sampled.test.tsv"]
    print("\n--- Checking Dakshina Dataset Files ---")
    for df in dakshina_files:
        p = os.path.join(DAKSHINA_DIR, df)
        if os.path.exists(p) and os.path.getsize(p) > 0:
            sha = compute_sha256(p)
            print(f"[FOUND] Dakshina {df}: {os.path.getsize(p)} bytes, SHA256: {sha[:16]}... (CC-BY-SA-4.0)")
        else:
            print(f"[MISSING] Dakshina {df} at {p}", file=sys.stderr)

if __name__ == "__main__":
    main()
