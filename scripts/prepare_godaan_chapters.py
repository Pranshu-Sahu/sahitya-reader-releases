"""Build Godaan's 36 reading chapters from proofread Hindi Wikisource pages.

Source: pages 11-328 of गोदान.pdf on Hindi Wikisource. MediaWiki renders
the transcribed pages before extraction, preserving page-spanning paragraphs.
"""

from pathlib import Path
import re

import requests
from bs4 import BeautifulSoup, NavigableString


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "app/src/main/assets/godaan-chapters"
API = "https://hi.wikisource.org/w/api.php"
HEADERS = {"User-Agent": "SahityaReader/1.7 (offline reading edition; attribution in app)"}
CHAPTER_NAMES = (
    "एक दो तीन चार पांच छह सात आठ नौ दस ग्यारह बारह तेरह चौदह पंद्रह "
    "सोलह सत्रह अठारह उन्नीस बीस इक्कीस बाईस तेईस चौबीस पच्चीस "
    "छब्बीस सत्ताईस अट्ठाईस उनतीस तीस इकतीस बत्तीस तेंतीस चौंतीस "
    "पैंतीस छत्तीस"
).split()


def render_complete_book():
    chunks = []
    for first, last in ((11, 200), (201, 328)):
        markup = f'<pages index="गोदान.pdf" from="{first}" to="{last}" />'
        response = requests.post(
            API,
            data={"action": "parse", "text": markup, "contentmodel": "wikitext",
                  "prop": "text", "format": "json"},
            headers=HEADERS,
            timeout=120,
        )
        response.raise_for_status()
        chunks.append(response.json()["parse"]["text"]["*"])
    return chunks


def extract_chapters(html_chunks):
    soup = BeautifulSoup(html_chunks[0], "html.parser")
    main = soup.select_one(".prp-pages-output")
    if main is None:
        raise ValueError("Wikisource returned no page text")
    for html in html_chunks[1:]:
        following = BeautifulSoup(html, "html.parser").select_one(".prp-pages-output")
        if following is None:
            raise ValueError("Wikisource returned no page text in a later segment")
        for node in list(following.contents):
            main.append(node)
    page_markers = main.select(".ws-pagenum")
    page_numbers = {tag.get("data-page-index") for tag in page_markers}
    if len(page_numbers) != 318:
        raise ValueError(f"Expected 318 source pages; found {len(page_numbers)}")
    qualities = [tag.get("data-page-quality") for tag in page_markers]
    if any(quality not in ("3", "4") for quality in qualities):
        raise ValueError("A source page is below Wikisource proofread status")
    for tag in main.select(".ws-pagenum, .ws-noexport, style, script, sup.reference"):
        tag.decompose()

    found = []
    words = {name: number for number, name in enumerate(CHAPTER_NAMES, 1)}
    for leaf in list(main.find_all(string=True)):
        name = leaf.strip()
        if name not in words:
            continue
        candidate = None
        for ancestor in leaf.parents:
            if ancestor is main:
                break
            if ancestor.name in ("h2", "center", "div", "p") and ancestor.get_text(" ", strip=True) == name:
                candidate = ancestor
        if candidate is None:
            continue
        number = words[name]
        candidate.replace_with(NavigableString(f"\n\n__CHAPTER_{number:02d}__\n\n"))
        found.append(number)
    if found != list(range(1, 37)):
        raise ValueError(f"Chapter headings are missing or out of order: {found}")

    # Add breaks only around block elements. Joining the DOM text without a
    # separator keeps inline emphasis and page-spanning words intact.
    for tag in list(main.find_all(("p", "dd", "dt", "li", "blockquote", "div", "center", "h2", "br"))):
        if tag.parent is not None:
            tag.insert_after(NavigableString("\n\n" if tag.name != "br" else "\n"))
    text = main.get_text(separator="")
    text = re.sub(r"[\u200b\u2060\ufeff]", "", text)
    text = re.sub(r"[ \t]+", " ", text)
    text = re.sub(r" *\n *", "\n", text)
    text = re.sub(r"\n{3,}", "\n\n", text).strip()
    chunks = re.split(r"__CHAPTER_(\d\d)__", text)
    if len(chunks) != 73 or chunks[0].strip():
        raise ValueError("Unexpected chapter split")
    chapters = {}
    for i in range(1, len(chunks), 2):
        number = int(chunks[i])
        body = chunks[i + 1].strip()
        # Two clear transcription slips remain in the otherwise proofread
        # source. Keep corrections here so regenerating preserves them.
        body = body.replace("झुरिर्यों", "झुर्रियों").replace("पछाड खाकर", "पछाड़ खाकर")
        if number == 36 and body.endswith("\n\n..."):
            body = body[:-5].rstrip()
        if len(body) < 2500:
            raise ValueError(f"Chapter {number} unexpectedly short: {len(body)}")
        chapters[number] = body
    if "मालम" in "\n".join(chapters.values()):
        raise ValueError("The previous transcription's 'मालम' error remains")
    return chapters, qualities


def main():
    chapters, qualities = extract_chapters(render_complete_book())
    OUT.mkdir(parents=True, exist_ok=True)
    for number, body in chapters.items():
        (OUT / f"{number:02d}.txt").write_text(body + "\n", encoding="utf-8")
        print(f"{number:02d}: {len(body)} characters")
    print(f"Total: {sum(map(len, chapters.values()))} characters")
    print(f"Source pages: {qualities.count('4')} validated, {qualities.count('3')} proofread")


if __name__ == "__main__":
    main()
