# Sahitya Reader

A native, offline-first Android reader for your own Hindi and English books and study material.

## Build

Open this directory in Android Studio, or run `gradlew.bat assembleDebug` with JDK 17+ and Android SDK installed. The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Supported files

- TXT and EPUB for selectable text, highlights and notes.
- PDF for offline page reading and saved reading progress.
- Use **Pair language** in the reader to show a second TXT/EPUB book alongside the current text.

All imported files and reading data stay in the app's private local storage. The app includes no bundled books, account, or sync service.

## Godaan reading material

An importable Hindi TXT draft and its page-by-page source record are in `content/godaan-first-100/`. In the app, choose **Add book** and select `godaan-first-100-pages-hi.txt`. Read `sources-and-qc.md` first: this is scan leaves 3–102, and its scan sequence should not be mistaken for 100 verified printed folios.

## In-app updates

Tap the circular-arrow icon in the library and enter the HTTPS address of your hosted `update-manifest.json`. The current personal feed is `https://raw.githubusercontent.com/Pranshu-Sahu/sahitya-reader-releases/main/update-manifest.json`. Long-press that icon later to change the update source. The app checks the manifest when asked, downloads a newer APK, checks its SHA-256 checksum, and opens Android's installer. Android may ask you to allow Sahitya Reader to install updates from this source.

Host a copy of `update-manifest.example.json` alongside your APK, then set:

- `versionCode` to a number higher than the installed app's version code (increment it for each release).
- `versionName` to the user-facing release number.
- `apkUrl` to the APK's HTTPS download link.
- `sha256` to the APK's SHA-256 digest. On Windows, run `Get-FileHash .\sahitya-reader.apk -Algorithm SHA256` and copy the `Hash` value.
- `releaseNotes` to a short description of the changes.

Android only accepts an update signed with the same signing certificate as the installed app. This personal APK is signed with the local Android debug key; keep using that key for future releases. If your existing installation was signed with a different key, Android will not install this APK over it. The example manifest is a template; the current feed uses the matching APK and checksum hosted in this repository.
