# KoSch AI Launcher · Specialist benchmark baseline · 2026-08-28

This file is a dated product-engineering baseline, not a claim that every listed KoSch capability is already LIVE. Maturity remains authoritative in code and `LAUNCHER_FULL_SCOPE_BENCHMARK.md`.

## Current specialist bars checked

### Total Launcher · object/layout freedom
Reference: Google Play listing, updated 2026-08-20.

Current bar relevant to KoSch:
- unusually broad free-form customization;
- object-centric layout model;
- scrollable/gesture-capable objects;
- graphic objects including images/shapes;
- shape-driven text/background styling;
- more granular shape geometry including different corner radii;
- launcher search support.

KoSch response:
- keep grid-safe direct manipulation as the easy default;
- add expert object transforms/styles/layers without turning standard use into a graphics editor;
- presets and exact controls write the same scoped style tokens;
- full target additionally includes alignment/distribution, batch edit, text/image/shape/sticker objects and robust performance at large object counts.

### Smart Launcher 6 · organization, widgets and polished customization
References: Smart Launcher feature page and current Google Play listing; Smart Launcher 6.6 updates in 2026.

Current bar relevant to KoSch:
- automatic app categories;
- adaptive grid and one-hand-oriented organization;
- wallpaper-driven theming, fonts and icon packs;
- integrated widgets plus widget stacks;
- search across apps/contacts/web/calculation/common actions;
- gestures/hotkeys;
- notification badges;
- private/hidden apps with PIN;
- Wallpaper Studio;
- stickers and pasted text elements;
- RSS/news surface;
- backup quality, including improvements around interrupted overwrites.

KoSch response:
- local smart classification + manual correction;
- first-class AppWidgetHost plus stack/smart-stack target;
- Universal Search/Command including local arithmetic and unit conversion;
- theme creator and decorative object system target;
- private-space design must use Android/profile/security primitives rather than pretending that launcher-only hiding is equivalent to device encryption;
- backup writes remain transactional/user-controlled and device-bound grants stay excluded.

### Niagara Launcher · contextual minimalism
References: Niagara help pages for pop-ups, Pro features and Niagara Button.

Current bar relevant to KoSch:
- app pop-ups expose notifications and app shortcuts with minimal navigation;
- pop-up folders can mix apps, shortcuts and widgets;
- widget stacks can live in pop-ups;
- a single programmable Niagara Button can host an app/action/folder/widget with a secondary swipe;
- strong restraint: power is surfaced contextually instead of as permanent chrome.

KoSch response:
- retain the richer App Actions space but prioritize safe context rather than adding more permanent buttons;
- target mixed contextual pop-ups with apps, published shortcuts, package-level badge state and widgets;
- preserve notification-content privacy by default: counts/state may rank locally, message bodies do not enter launcher ranking;
- make the adaptive Power Rail/Ask entry context-sensitive and removable rather than mandatory.

### Kvaesitso · search-first launcher
References: MM2-0/Kvaesitso GitHub repository/releases, including v1.39 transliteration work.

Current bar relevant to KoSch:
- search-first interaction model;
- apps and shortcuts as first-class search targets;
- transliteration between native/non-Latin script and Latin search forms;
- configurable gestures around search/widgets;
- utility-style search expectations such as calculation/conversion.

KoSch response:
- native-script exact matching remains higher priority than romanized fallback;
- one typed bounded index spans apps, shortcuts, folders, pages, settings, custom actions and AI routes;
- arithmetic and common unit conversion are evaluated locally at query time and never require a web service;
- execution stays separate from ranking so search cannot bypass confirmation/capability gates;
- next target: timezone/date utilities, settings deep navigation and published shortcut execution directly from the unified palette.

### Nova Launcher · mature power-user baseline
Reference: current Nova Launcher website.

Current bar relevant to KoSch:
- desktop/drawer/dock/folder customization;
- icon themes;
- backup/restore;
- gestures;
- notification badges;
- drawer folders.

KoSch response:
- these are release-baseline capabilities rather than differentiation;
- KoSch must match them while adding scoped object/page inheritance, Android form-factor awareness, local command routing and Assistant/AI layers.

## KoSch lead domains that must not regress while chasing parity
- Local/deterministic launcher core first.
- External AI/provider handoff is explicit and confirmation-gated.
- Screen/camera awareness is default OFF and session/capability separated.
- No blanket contacts/call/storage/accessibility rights just to imitate another launcher feature.
- Device-local secrets/host IDs/grants stay out of portable backups.
- Real Android capability discovery rather than fabricated deep links/device allowlists.
- Professional keyboard, mouse/trackpad, stylus and foldable architecture are first-class, not tablet afterthoughts.
- Assistant character/voice/agent system remains a KoSch-specific lead domain.

## Current implementation status at this baseline

### LIVE before this benchmark pass
- V7 workspace pages, drag/drop/resize/cross-page editing and undo/redo paths.
- Android AppWidget host/picker/configuration/remap/recovery foundations.
- extensive Settings Center and scoped settings persistence.
- app actions including published Android shortcuts.
- Smart AI routing, provider preference and explicit context/provider handoff gates.
- Assistant runtime/capture safety foundations.
- production permission/release compliance budget.

### Newly strengthened CORE_READY in this benchmark pass
- privacy-minimal precise-pointer/hardware-keyboard capability model.
- native-script + transliterated local SearchRanker.
- bounded launcher-wide Universal Search entity index.
- local arithmetic and common unit conversion query utilities.
- full scoped object-style token contract and coherent style presets.
- real Jetpack WindowManager fold-posture monitor and book/tabletop/separating/occlusion policy.

### Still required before those new domains may be called LIVE
- connect Universal Search Query Engine to the visible search/command palette and typed execution routes;
- connect fold posture monitor to Activity lifecycle and actual hinge-safe/book/tabletop Home/Settings presentation;
- render scoped object styles in normal Home and Home Studio;
- provide Home Studio preset + precise style editor, batch selection/alignment/distribution and accessible non-drag alternatives;
- add theme/decorative object system, widget stacks, mixed contextual pop-ups and deeper private-space handling;
- add instrumentation/performance tests on the visible implementations and large collections.

## Product rule
A competitor feature is not copied merely because it exists. We either:
1. implement an equivalent or stronger version that fits KoSch's architecture, or
2. deliberately replace it with a safer/better Android-native mechanism and document the difference.

A privacy or accessibility regression is never accepted merely to achieve checkbox parity.
