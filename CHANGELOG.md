# Changelog

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