"""Build the offline reading edition from Hindi Wikisource's 36 chapters.

The bundled 611-page PDF has a damaged Hindi text layer. Wikisource's
transcribed chapters provide the readable text of the same complete novel.
"""

from pathlib import Path
import re
import time

import requests
from bs4 import BeautifulSoup


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "app/src/main/assets/godaan-chapters"
API = "https://hi.wikisource.org/w/api.php"
HEADERS = {"User-Agent": "SahityaReader/1.6 (personal offline reading; source attribution in app)"}
DIGITS = "०१२३४५६७८९"


def fetch(number):
    title = "गो-दान/" + "".join(DIGITS[int(d)] for d in str(number))
    for attempt in range(8):
        try:
            response = requests.get(API, params={"action": "parse", "page": title,
                                                 "prop": "text|revid", "format": "json"},
                                    headers=HEADERS, timeout=40)
            response.raise_for_status()
            parsed = response.json()["parse"]
            soup = BeautifulSoup(parsed["text"]["*"], "html.parser")
            content = soup.select_one(".prp-pages-output > div:not(.ws-noexport)")
            if content is None:
                raise ValueError(f"No text for {title}")
            paragraphs = []
            for paragraph in content.find_all("p"):
                text = paragraph.get_text(" ", strip=True)
                text = re.sub(r"[\u200b\u2060\ufeff]", "", text)
                text = re.sub(r"\s+", " ", text).strip()
                if text:
                    paragraphs.append(text)
            result = "\n\n".join(paragraphs)
            if len(result) < 2000:
                raise ValueError(f"Chapter {number} unexpectedly short: {len(result)}")
            return number, result, parsed.get("revid")
        except (requests.RequestException, KeyError, ValueError) as error:
            if attempt == 7:
                raise
            time.sleep(min(90, 20 * (attempt + 1)) if "429" in str(error) else 2 ** attempt)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    chapters = {}
    for number in range(1, 37):
        path = OUT / f"{number:02}.txt"
        if path.is_file() and path.stat().st_size > 2000:
            body = path.read_text(encoding="utf-8")
            revision = "cached"
        else:
            _, body, revision = fetch(number)
            path.write_text(body + "\n", encoding="utf-8")
            time.sleep(3)
        chapters[number] = (body, revision)
        print(f"{number:02}: {len(body)} characters; revision {revision}", flush=True)
    print("Total characters:", sum(len(body) for body, _ in chapters.values()))


if __name__ == "__main__":
    main()
