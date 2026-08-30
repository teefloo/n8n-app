# n8n Mobile Manager — Product Context

## Platform

- Android native application built with Kotlin and Jetpack Compose.
- Minimum supported Android API: 26.
- Primary UI toolkit: Material 3 with the repository's existing n8n brand colors.

## Product purpose

n8n Mobile Manager gives n8n users a practical mobile control surface for monitoring and managing configured n8n instances. The core jobs are:

1. Connect to one or more n8n instances securely.
2. Understand instance health and recent execution activity quickly.
3. Inspect and activate/deactivate workflows.
4. Investigate execution details and safely retry or stop runs.
5. Review credentials metadata without exposing secret values.
6. Configure instances, theme, notifications, and local security preferences.

## Product truths and constraints

- Existing correct API, Room, DataStore, encryption, notification, and MVVM behavior must be preserved.
- The application must remain usable with slow, unavailable, partial, or permission-limited n8n APIs.
- Primary actions need clear confirmation or immediate feedback; silent failures are not acceptable.
- The app must remain readable in light and dark themes and at larger system font scales.
- No production deployment, irreversible data deletion, secret exposure, or destructive migration is part of this work.

## Experience priorities

1. Reliable navigation and state restoration.
2. Fast comprehension of health, workflow state, and execution status.
3. Safe, keyboard-friendly, accessible interactions.
4. Consistent visual hierarchy with restrained depth and n8n orange used as an action accent.
5. Honest loading, offline, error, and partial-data states.
