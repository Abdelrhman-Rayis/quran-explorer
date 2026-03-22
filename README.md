# Quran Explorer

A localhost web app for exploring the Quran — read verses, inspect word-by-word morphology, and search in Arabic text.

Built with [JQuranTree](http://jqurantree.org/) and the [Quranic Arabic Corpus](http://corpus.quran.com/).

---

## Features

- **114 Surahs** — browse the full Quran with Arabic names and verse counts
- **Verse reader** — clean Arabic text with Uthmani script and bismillah headers
- **Token inspector** — click any verse to break it into individual words
- **Morphological analysis** — click any word to see its full grammatical breakdown:
  - Part of speech (Noun, Verb, Adjective, Preposition…)
  - Root (3-letter Arabic root)
  - Lemma
  - Gender, number, case, mood, voice
  - Prefix / Stem / Suffix segments
- **Arabic search** — search across all 6,236 verses; works with or without diacritics (tashkeel)

---

## Project Structure

```
quran-app/
├── src/
│   └── QuranServer.java      # Java HTTP server + all API logic
├── web/
│   └── index.html            # Frontend (single-file, no dependencies)
├── corpus.txt                # Quranic Arabic Corpus morphology data (not in git)
├── run.sh                    # Build & run script
└── .gitignore
```

The JQuranTree JAR is expected one directory up at `../jqurantree-1.0.0-bin/jqurantree-1.0.0.jar`.

---

## Requirements

- Java 11 or later
- The JQuranTree binary: `jqurantree-1.0.0-bin/jqurantree-1.0.0.jar`
- The corpus file: `quranic-corpus-morphology-0.4.txt` (see below)

---

## Getting Started

**1. Get the corpus file**

Download `quranic-corpus-morphology-0.4.txt` from [corpus.quran.com/download](http://corpus.quran.com/download) and place it in the `quran-app/` folder as `corpus.txt`, or one level up as `../quranic-corpus-morphology-0.4.txt`.

**2. Run the server**

```bash
cd quran-app
bash run.sh
```

**3. Open in browser**

```
http://localhost:9090
```

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/chapters` | All 114 chapters (number, Arabic name, English name, verse count) |
| GET | `/api/chapter/{n}` | Chapter details + all verses |
| GET | `/api/verse/{ch}/{v}` | Single verse with tokenized words and POS labels |
| GET | `/api/morph/{ch}/{v}/{t}` | Full morphological analysis for one token |
| GET | `/api/search?q={arabic}` | Search all verses (diacritic-normalized, max 100 results) |

### Example responses

**GET /api/verse/1/1**
```json
{
  "chapter": 1,
  "verse": 1,
  "text": "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
  "buckwalter": "bisomi {ll~ahi {lr~aHoma`ni {lr~aHiymi",
  "tokens": [
    { "number": 1, "text": "بِسْمِ", "buckwalter": "bisomi", "pos": "Noun" },
    { "number": 2, "text": "ٱللَّهِ", "buckwalter": "{ll~ahi", "pos": "Proper Noun" },
    { "number": 3, "text": "ٱلرَّحْمَٰنِ", "buckwalter": "{lr~aHoma`ni", "pos": "Adjective" },
    { "number": 4, "text": "ٱلرَّحِيمِ", "buckwalter": "{lr~aHiymi", "pos": "Adjective" }
  ]
}
```

**GET /api/morph/1/1/3**
```json
{
  "segments": [
    { "type": "PREFIX", "form": "{l", "tag": "DET", "tagLabel": "Definite Article" },
    { "type": "STEM",   "form": "r~aHoma`ni", "tag": "ADJ", "pos": "ADJ",
      "root": "rHm", "lem": "r~aHoma`n", "attr_MS": "true", "attr_GEN": "true" }
  ]
}
```

---

## Data Sources

- **Quran text** — [Tanzil Project](http://tanzil.net) (Uthmani script), bundled inside JQuranTree
- **JQuranTree** — Java API by Kais Dukes, GPL v3
- **Quranic Arabic Corpus** — morphological annotation by Kais Dukes, University of Leeds, GPL v3

---

## License

This project is released under the MIT License.
The Quranic Arabic Corpus data is licensed under the GNU GPL v3 — see [corpus.quran.com](http://corpus.quran.com) for terms.
