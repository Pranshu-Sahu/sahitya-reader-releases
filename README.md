# Sahitya Reader

A native, offline-first Android reader for your own Hindi and English books and study material.

## Build

Open this directory in Android Studio, or run `gradlew.bat assembleDebug` with JDK 17+ and Android SDK installed. The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Supported files

- TXT and EPUB for selectable text, highlights and notes.
- PDF for offline page reading and saved reading progress.
- Use **Pair language** in the reader to show a second TXT/EPUB book alongside the current text.

Imported files and reading data stay in the app's private local storage. The app includes no account or sync service.

## Godaan reading material

The APK includes the complete Hindi novel as selectable text in 36 chapters. Swipe horizontally between chapters and long press near the center of the reading area to open the reading menu. The old 100-page draft is removed automatically when an existing installation updates. The previous 611-page PDF remains in `content/` for source reference; its embedded Hindi text layer is too error-prone for comfortable reading. Version 1.7 replaces the earlier error-prone transcription with the [proofread Wikisource pages of गोदान.pdf](https://hi.wikisource.org/wiki/%E0%A4%AA%E0%A5%83%E0%A4%B7%E0%A5%8D%E0%A4%A0:%E0%A4%97%E0%A5%8B%E0%A4%A6%E0%A4%BE%E0%A4%A8.pdf/%E0%A5%A7%E0%A5%A7). On update, the app refreshes the saved chapter files while preserving reading progress. Attribution and edition notes are available by long-pressing the Godaan library tile.

## In-app updates

Tap the circular-arrow icon in the library to check the personal update feed at `https://raw.githubusercontent.com/Pranshu-Sahu/sahitya-reader-releases/main/update-manifest.json`, download a newer APK, verify its SHA-256 checksum, and open Android's installer. Long-press the icon to change the update source. Android may ask you to allow Sahitya Reader to install updates from this source and confirm the installation.

Host a copy of `update-manifest.example.json` alongside your APK, then set:

- `versionCode` to a number higher than the installed app's version code (increment it for each release).
- `versionName` to the user-facing release number.
- `apkUrl` to the APK's HTTPS download link.
- `sha256` to the APK's SHA-256 digest. On Windows, run `Get-FileHash .\sahitya-reader.apk -Algorithm SHA256` and copy the `Hash` value.
- `releaseNotes` to a short description of the changes.

Android only accepts an update signed with the same signing certificate as the installed app. This personal APK is signed with the local Android debug key; keep using that key for future releases. If your existing installation was signed with a different key, Android will not install this APK over it. The example manifest is a template; the current feed uses the matching APK and checksum hosted in this repository.
