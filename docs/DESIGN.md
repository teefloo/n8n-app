# n8n Mobile Manager — Android Design Direction

## Direction

An operations console built on Material 3: calm neutral surfaces, strong type hierarchy, compact status-forward data presentation, and n8n orange reserved for primary actions and active states. The interface keeps the product's orange/teal identity while replacing heavy neumorphic chrome with restrained tonal elevation and clear component affordances.

## Signature details

- Status-first language: every live/remote state includes a clear label and, where useful, freshness context.
- Execution pulse: success, error, running, waiting, and canceled states use a consistent icon + color + text treatment.
- Automation graph vocabulary: workflow cards and details use small structural accents and node counts rather than decorative gradients.
- Native ergonomics: Material app bars, NavigationBar/NavigationRail, standard dialogs and sheets, 48dp minimum targets, edge-to-edge insets, and predictable predictive-back behavior.

## System rules

- Use semantic Material color roles; do not hard-code surface/text colors in screens.
- Use the platform typography scale and allow text to grow with system settings.
- Keep interactive targets at least 48dp and provide content descriptions or state semantics.
- Prefer one clear primary action per surface.
- Use subtle motion for state change and navigation; skip nonessential motion when reduced motion is enabled.
- Keep lists lazy, avoid duplicate loads, and show stale/refreshing state without hiding usable cached data.
