# Changelog

All notable changes to Innovexia will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Fixed
- Guest rate limiting now properly tracks and increments message count
- Rate limit counter in side menu now updates in real-time for guest users
- FirebaseRateLimiter now correctly updates StateFlow for guest sessions
- Added comprehensive logging for rate limit tracking and debugging

### Changed
- Removed duplicate rate limit display from AccountQuickPanel popup
- Rate limit info now only shown in side menu footer for cleaner UI
- Enhanced DrawerFooterProfile to display real-time rate limit counter
- Improved guest mode UI with "Free" badge and clearer status indicators

### Added
- Real-time rate limit monitoring for guest users in side menu
- Visual feedback when approaching or exceeding rate limits (red indicator at 90%+)
- Enhanced logging in HomeViewModel for rate limit flow debugging
- Guest limiter StateFlow integration for live UI updates

---

## [1.0.1] - 2025-01-XX

### Fixed
- Fixed critical crash when sending messages in release builds
- Added comprehensive ProGuard rules to prevent obfuscation issues
- Resolved issues with Firebase and Gemini AI in production

### Changed
- Updated ProGuard configuration for better release build stability
- Improved error handling in message sending flow

---

## [1.0.0] - 2025-01-XX

### Added
- Initial release of Innovexia AI chat application
- Firebase authentication with guest mode support
- Real-time chat with Gemini 2.5 Flash AI
- Persona system for customized AI interactions
- Memory engine for context-aware conversations
- Grounding support with Google Search integration
- Multi-modal support (text, images, PDFs, documents)
- Local-only mode (incognito) for privacy
- Chat history with search and organization
- Rate limiting system (separate for guests and authenticated users)
- Cloud sync for chat history across devices
- Subscription management with tiered plans
- Voice input for messages
- Dark mode UI with glassmorphic design

---

## Version Guidelines

### Version Format: `MAJOR.MINOR.PATCH`

- **MAJOR** (1.x.x): Breaking changes, major feature overhauls
- **MINOR** (x.1.x): New features, non-breaking changes
- **PATCH** (x.x.1): Bug fixes, small improvements

### Change Categories

- **Added**: New features
- **Changed**: Changes to existing functionality
- **Deprecated**: Soon-to-be removed features
- **Removed**: Removed features
- **Fixed**: Bug fixes
- **Security**: Vulnerability fixes
