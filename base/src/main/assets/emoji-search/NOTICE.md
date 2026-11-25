# Emoji search data

The locale-specific emoji names and keywords in this directory are generated from Unicode CLDR 49.0.0 character annotations, including derived annotations, at commit `7d2b9cb11cec08f4d333ad73209deb3df0711c3b`. The emoji sequence list comes from the bundled `androidx.emoji2:emoji2-emojipicker:1.5.0` data.

CLDR-derived indexes are included only for app locales with annotation data: English, Arabic, Catalan, German, Spanish, Finnish, French, Italian, Japanese, Norwegian Bokmål, Portuguese, Russian, Tamil, Ukrainian, and Simplified Chinese. The app's Esperanto locale has no CLDR emoji annotations, so search falls back to English there. Other device locales also use the app's English fallback.

The build packs the TSV files from `base/src/main/emoji-search` into a shared emoji sequence list and one compressed annotation file per language. Each annotation contains the description followed by the remaining search terms, in the same row order as the sequence list.

CLDR data is distributed under the Unicode License V3. See [UNICODE-LICENSE.txt](UNICODE-LICENSE.txt).
