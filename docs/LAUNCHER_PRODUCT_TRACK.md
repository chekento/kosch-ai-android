# KoSch AI Launcher – Product Track

This track keeps the project centered on being an excellent, highly configurable Android launcher. Security remains a quality gate and an isolated subsystem; it must not consume the product roadmap.

## Product principle

The launcher should feel powerful before it feels technical: immediate access to apps, people, files, widgets and AI; a polished living surface; deep customization; fast recovery/undo; and no requirement to understand the internal security architecture.

## P0 – Home Studio and everyday manageability

- Free user pages and deterministic grid placement (landed)
- Cross-page drag/drop with keyboard/accessibility fallback (landed)
- Duplicate a complete user page with fresh portable item IDs (core landed in this track)
- Resize Home items with deterministic collision avoidance (core landed in this track)
- Auto-arrange/compact a user page without changing item order or content (core landed in this track)
- Surface duplicate, resize, auto-arrange and undo in a dedicated Home Studio editing experience (landed in this track)
- Multi-select for move/remove/group where accessibility semantics remain explicit
- Page overview with reorder, rename, duplicate, hide/show and default-page selection

## P0 – Extensive Settings Center

The launcher gets a real configuration center rather than a small preferences page. The architecture is tracked in `docs/SETTINGS_CENTER_ARCHITECTURE.md`.

- Versioned portable `LauncherSettingsDocument` with explicit normalization and migration path (model landed in this track)
- At least 22 primary Settings sections: Home/Grid, Pages, Apps, Dock, Folders, Widgets, Appearance, Themes, Assistant, AI, APIs, Voice, Gestures, Search, Notifications, Pen, Automation, Accessibility, Privacy, Backup, System, Advanced
- Settings search across every subtab
- Global defaults with page-level and object-level overrides where the setting is meaningful
- Explicit `inherit default` state for every scoped override
- Preview / Apply / Discard / Undo for grid, theme, layout, icon-pack and other disruptive visual changes
- Reset by setting, group, tab or complete launcher
- Favorites / pinned settings and recently changed settings
- Partial import/export with dry-run and conflict preview
- API/provider configuration separated from credential secret material
- Secrets, OAuth tokens, widget host IDs, URI grants and other device-bound capabilities excluded from portable settings
- First live tabs: Home/Grid, Appearance and Assistant
- Follow with Theme Import/Export, Gesture Matrix, AI/API Provider Settings, Widget/Folder/App inspectors and Backup scopes

## P0 – Widgets become first-class Home items

- Connect the existing AppWidgetHost to WorkspaceDocument v7
- Add widgets directly from the Home add sheet
- Persist portable provider identity separately from device-local appWidgetId
- Resize through grid spans and update provider size hints
- Missing/remapped widget state that is understandable rather than broken
- Widget stacks with explicit ordering and switch gestures
- Restore mapping after device migration without copying stale host IDs

## P1 – Professional launcher parity

- Icon-pack discovery and per-item icon override
- Gesture matrix: swipe, double tap, long press, edge actions and configurable shortcuts
- Per-page grid profiles and spacing/density controls with safe reflow preview
- App labels, badges and visual density presets
- Search/command palette reachable from hardware keyboard and gesture
- Folder layouts, folder gestures and smart/manual modes
- Page wallpaper/visual treatment overrides while retaining global themes

## P1 – Living AI surface

- Optional assistant that can be completely disabled
- Portal/spawn/rotation/eye/mouth asset system kept independent of launcher logic
- Voice and live-chat entry from Home without forcing a network provider
- Local-first command planning; provider routing is always explicit when data leaves the device
- Contextual suggestions that never rearrange the user’s Home without preview/apply
- Assistant visibility, scale, anchor, motion, gaze, emotion, lip sync, voice and per-page behavior configurable in Settings Center

## P1 – Visual quality

- Neural Glass remains the default design language, with restrained depth and motion
- Consistent corner radii, elevation, typography, state transitions and haptics
- Edit mode becomes visually distinct but not visually noisy
- Smooth page transitions, folder opening, widget placement and assistant appearance
- Reduced Motion, high contrast and large-text behavior are designed variants, not afterthoughts

## P2 – Creator and theme system

- Declarative theme/layout package format
- Theme preview, rollback and export/import
- Per-object inspector for size, position, visual treatment and actions
- Persistent multi-step undo/redo history
- Optional LCARS, Minimal Work, Living AI and Cyberdeck programs without making any one theme a core dependency

## Configuration scope model

Visual and interaction settings should use a predictable inheritance chain where meaningful:

**Global launcher default → Page override → Folder/Widget/App-item override**

Temporary context/session behavior is not allowed to silently overwrite persistent user choices. Every override must expose an explicit `inherit default` option so the configuration can be simplified again.

## Product gates

Security/firewall work may block a release when it affects privacy or correctness, but it runs as a parallel gate. It does not replace launcher work. Every milestone should ship at least one user-visible launcher improvement unless the milestone is explicitly a release-hardening gate.

The next user-visible sequence is therefore: Home Studio → Settings Center foundation → v7 widget placement → icon packs/gestures → assistant/living-surface integration → deeper theme/creator tools.
