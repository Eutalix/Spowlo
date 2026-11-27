# Changelog

## [2.0.0]
Huge update!!! Spowlo no longer needs a Spotify Developer account, the app is much smaller, and most of the screens have been redone.

### Added
- **No Client ID or Secret.** Spotify metadata comes from the web player now, so there's nothing to set up, and it won't break nearly as often.
- **A Library tab.** Sign in with Spotify to see your playlists, liked songs and Spotify's recommendations.
- **Recent songs** on the home screen.
- **Albums and singles on artist pages.**
- **Size estimates** before downloading an album, playlist or artist section.
- **Download only the missing songs** from a collection.
- **Search and filter your downloads.** The improved downloads list tab lets you sort by title, artist, duration or size, and filter by artist and format. You can delete songs from there too.
- **Export** selected songs as an M3U playlist or CSV.
- **Import files** you already have from your download folders, then update their tags or redownload them with ease.
- **More on the track page:** file details, tag fixing and a shortcut to the song's folder.
- **Cover art colours** on album, playlist and artist pages (off by default, turn it on in Appearance settings).
- **Some settings from spotDL**, including proxy and a couple of playlist tagging settings.

### Changed
- **Search got a redesign** with tabs and a top result, and you can start a search from the home screen too.
- **New look** for the downloads list, metadata pages and settings. Settings are grouped into clearer sections too.
- (slightly) **Better notifications**, they show cover art now, song and artist for each download, and no longer print raw log output.
- **Much, much smaller.** The APK is around **14 MB** now instead of **60 MB**. A fresh install used to take up to 170 MB for the app and up to 170 MB of data; it's around 21 MB of app and 54 MB of data now. The exact numbers depend on your phone.
- **Spowlo handles Spotify's web player changes on its own**, so search shouldn't break when Spotify updates. If it does, be sure to open an issue or shoot us a message in the [telegram group](https://t.me/spowlo_chatroom).
- **32-bit phones are no longer supported**; Spowlo needs a 64-bit device.

### Fixed
- Downloads that fail for a temporary reason **retry on their own** + clearer error messages.
- **Long and non-Latin titles** no longer fail on the filename length limit.
- **Tapping download twice** no longer starts the same song twice.
- **Fixed a crash on some phones.**

### Removed
- Dead code.

## [1.6.2]
### Added
- **News & Announcements System:** Added an in-app news dialog to alert users about critical issues (such as the recent Spotify API revocation) and updates.
- Added a toggle in **General Settings** to enable/disable news notifications.

### Fixed
- **Critical Search Fix:** Fixed a bug where the Search feature ignored custom Spotify credentials. Searching now correctly uses your personal Client ID and Secret if enabled.
- **Updater UI:** Fixed layout issues in the Update Dialog where text would disappear or not scroll correctly.
- Improved the stability of the in-app updater (better handling of cached APKs and auto-update checks on startup).

## [1.6.1]
### Fixed
- Fixed a critical issue where updating Spotify credentials in settings required a full app restart to take effect.
- Added automatic input trimming for Client ID and Secret to prevent authentication errors caused by accidental whitespace.
- Resolved "Failed to fetch metadata" errors by ensuring the API instance is properly reset when switching credentials.

## [1.6.0]
### Added
- Unified Dependency Updater: spotDL and yt-dlp checks on startup.
- New Settings UI to manage updates and frequency.
- Major architecture refactor: moved from spotDL CLI to Spotify Web API for metadata.

### Fixed
- Fixed "ghost version" bugs where metadata fetch failed.
- Fixed instability with spotDL binary parsing.

## [1.5.3]
...
