# Changelog

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