# Godaan Hindi source and checking notes

## What this draft contains

`godaan-first-100-pages-hi.txt` contains 100 consecutive Wikisource scan-page transcriptions, from scan leaves 3–102 of the 1936 Saraswati Press edition. The first two leaves are cover material and were not included. The text preserves blank or decorative leaves as page markers. The sequence numbers in the TXT are scan sequence markers; they are not asserted to be the printed folio numbers.

## Sources and rights

- Facsimile: Premchand, *Godaan* (गोदान), Saraswati Press, Banaras, 1936. The scan record identifies the source as Digital Library of India item 2015.489953 and gives 369 DjVu scan pages. [Wikimedia Commons scan record](https://commons.wikimedia.org/wiki/File:%E0%A4%97%E0%A5%8B-%E0%A4%A6%E0%A4%BE%E0%A4%A8.djvu)
- Transcription: the corresponding page records on Hindi Wikisource. Every included page’s revision ID and proofread status are in `godaan-first-100-pages-hi.json` and `page-check.csv`. Status 3 is marked checked/proofread; status 4 is validated. [Wikisource text and edition details](https://hi.wikisource.org/wiki/%E0%A4%97%E0%A5%8B-%E0%A4%A6%E0%A4%BE%E0%A4%A8)
- The original novel is public domain in India. Wikisource’s contributed transcription text is offered under CC BY-SA; attribution and the source links are retained here. This is for a personal, offline reader; do not redistribute the transcription without following the applicable attribution and share-alike terms.

## Checks completed

- Retrieved exactly 100 consecutive page records, mapped each record to its scan leaf and revision, and retained proofread status.
- Checked the scan images for leaves 13, 14, 52, and 53 against their transcribed prose. The visible prose matched in those spot checks.
- Spot-checked the opening passage against the [Internet Archive DjVu OCR text](https://archive.org/download/in.ernet.dli.2015.489953/2015.489953.Godaan_djvu.txt) for DLI item 2015.489953. The text is corroborated, though this OCR has substantial recognition noise and different pagination, so it is not suitable as an exact page-by-page comparator.
- Removed page-layout templates and markup for the TXT reading copy. The JSON preserves the corresponding page text and source revision IDs for audit.

## Issue found; do not treat as a fully verified 100 printed-page edition

The Wikisource running-head metadata is not consistently aligned with the scan image. For example, its record for scan leaf 52 gives folio 50, while the image visibly shows folio 40; scan leaf 53 gives folio 59, while its image visibly shows folio 48. The prose on those spot-checked pages matches, but the folio metadata does not. The visible scan sequence also jumps from printed folio 40 to 48 here, so the first-edition scan may be missing printed folios 41–47. See [scan leaf 52](https://hi.wikisource.org/wiki/%E0%A4%AA%E0%A5%83%E0%A4%B7%E0%A5%8D%E0%A4%A0:%E0%A4%97%E0%A5%8B-%E0%A4%A6%E0%A4%BE%E0%A4%A8.djvu/%E0%A5%AB%E0%A5%A8) and [scan leaf 53](https://hi.wikisource.org/wiki/%E0%A4%AA%E0%A5%83%E0%A4%B7%E0%A5%8D%E0%A4%A0:%E0%A4%97%E0%A5%8B-%E0%A4%A6%E0%A4%BE%E0%A4%A8.djvu/%E0%A5%AB%E0%A5%A9). Therefore, the TXT identifies page boundaries by scan leaf, not by printed page number, and this package does **not** claim that the 100 included scan leaves are the first 100 printed folios or that all 100 pages have been independently checked line by line against a second transcription.

The second-source comparison so far is a spot check, not a full independent collation. The first 100 printed pages should not be marked final until the printed-page sequence is reconciled to the facsimile and the text is collated against an independent edition or page images. The source image remains the authority for resolving any reading.
