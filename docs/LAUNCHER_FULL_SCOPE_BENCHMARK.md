# KoSch AI Launcher · Full-Scope Benchmark Contract

## Product target
KoSch is not optimized for feature-count parity with one launcher. Each capability domain is benchmarked against the strongest relevant Android launcher/product experience and must reach or exceed that specialist while preserving KoSch's privacy-first local core, adaptive Android integration and AI/Assistant layer.

The benchmark is a **minimum bar**, not a product ceiling.

## Definition of Done for every capability
A capability may be marked `LIVE` only when all applicable layers are present:

1. **User experience** — discoverable UI, coherent defaults, expert controls without forcing complexity on basic users.
2. **Runtime truth** — the feature works against real Android/runtime capabilities; no fake device/model assumptions or marketing-only state.
3. **Persistence** — state survives process death/restart where appropriate.
4. **Scope/inheritance** — global/page/object/device/session ownership is explicit and validated.
5. **Portability** — backup/import/export behavior is defined; device-local grants, secrets and host ids never become portable by accident.
6. **Undo/reset/migration** — destructive/customizing operations have safe reset and migrations where relevant.
7. **Accessibility** — TalkBack semantics, keyboard/non-drag alternatives, touch targets and reduced-motion behavior are covered where applicable.
8. **Adaptive input/form factor** — phone, large screen, foldable posture, desktop windowing, mouse/trackpad, hardware keyboard and stylus behavior are considered where relevant.
9. **Privacy/security** — least privilege, bounded local data, explicit handoff/consent and no hidden collection.
10. **Tests** — deterministic policy/model tests plus instrumentation for user-visible Android integration where feasible.
11. **Fallbacks** — missing app/provider/widget/device capability fails visibly and safely instead of silently breaking.
12. **Documentation** — Settings/FAQ/release notes describe what is actually live and its limits.
13. **Performance** — bounded memory/storage/work, no unbounded indexes/logs/animations, sensible large-install behavior.

`CORE_READY` means the tested non-UI/runtime core exists but one or more LIVE layers above are still missing. `PLANNED` and `EXPERIMENTAL` must never be marketed as live.

## Full benchmark domains

### 1. Workspace freedom · Total Launcher bar
- Free placement, drag/drop, resize and cross-page movement.
- Global → page → object styling and inheritance.
- Per-object scale, label behavior, opacity, rotation, corner/shape, padding, border/background and visual-state hooks.
- Layering/z-order where Android rendering permits it safely.
- Alignment, snapping, distribution, multi-select and batch editing.
- Locking, protected objects and layout-safe edit mode.
- Object/page presets plus precise expert controls.
- Arbitrary user pages without losing accessible non-drag editing.

### 2. Home pages, spaces and profiles · Nova/Total bar
- Create, duplicate, delete, reorder, hide and loop pages.
- Per-page grid, dock, wallpaper/treatment, transition and Assistant visibility.
- Work/private/creative/presentation spaces.
- Profile-aware app identity and missing-app placeholders.
- Page templates and profile-specific startup/restore behavior.

### 3. App drawer and organization · Nova/Kvaesitso bar
- A–Z, usage, recent and manual ordering.
- Categories/tabs, hidden/system/work-profile handling.
- Smart local classification with manual correction.
- Dense/comfortable adaptive layouts and fast alphabet navigation.
- Local usage ranking with bounded, content-free learning.

### 4. Universal Search & Command · Kvaesitso/Niagara bar
- One local index for apps, app shortcuts, folders, pages, settings, actions, AI routes and other safe launcher entities.
- Exact, compact, token-prefix, word-prefix, acronym, typo and subsequence matching.
- Native-script Unicode matching and bounded transliteration fallbacks.
- Explainable ranking; original-script exact matches outrank transliteration.
- Action execution separated from text search; destructive/external routes preserve confirmation gates.
- Keyboard-first command palette and touch-first search surface.
- Session/local history remains optional and privacy-bounded.

### 5. App actions and contextual popup · Niagara/Nova bar
- Open, App Info, Store, hide/show, uninstall route, pin/unpin and ordering.
- Published Android shortcuts surfaced through LauncherApps only.
- Folder/smart-group assignment.
- Contextual prioritization from safe metadata such as local badge count/usage — never notification message content.
- Profile-safe launching with original UserHandle.

### 6. Gestures & input · Nova/Total bar
- Swipe, double tap, long press, multi-finger, pinch, edges and stylus buttons.
- Per-trigger configurable action with safe typed custom routes.
- Conflict detection and accessibility alternatives.
- Mouse/trackpad hover/right-click semantics where useful.
- Hardware-keyboard shortcuts and Android Keyboard Shortcuts Helper publication.

### 7. Dock & quick access · Nova/Smart Launcher bar
- User slots, ordering, adaptive suggestions and Ask/Search entry.
- Per-page overrides, optional multi-page/scrollable dock and slot actions.
- Context-aware suggestions never override explicit pins.
- Large-screen/desktop command rail and one-hand phone presentation.

### 8. Folders & smart groups · Smart Launcher/Nova bar
- Popup/sheet/full-screen presentation modes.
- Grid, sorting, smart membership, manual membership and mixed safe actions.
- Cover/icon-stack options and close-after-launch policy.
- Per-folder gestures and page/object scoped appearance.

### 9. Widgets & stacks · Smart Launcher/Samsung-class bar
- First-class Android AppWidgetHost integration.
- Picker/configuration, resize, provider size hints and device-local id recovery.
- Missing-provider placeholder/remap.
- Widget stacks, manual switching, smart/context stacks and optional bounded auto-cycle.
- Padding/crop/scale controls without breaking host ownership.

### 10. Themes, icons and creator system · Nova/Total bar
- Material You plus explicit light/dark/theme-defined modes.
- Icon packs, per-app icon/label overrides, monochrome/themed icons and shapes.
- Wallpaper dim/blur/color extraction and per-page treatment.
- Theme preview, import/export, partial-component apply, rollback/history and creator metadata.
- Assistant assets and optional sounds as separately controllable theme components.
- Asset/license provenance is part of release readiness.

### 11. Motion & depth
- None/reduced/balanced/expressive profiles.
- Page transitions, depth/parallax and object feedback with reduced-motion compliance.
- No decorative animation may block input or create unbounded battery work.

### 12. Foldables & adaptive layouts · Android/Smart Launcher bar
- Real Jetpack WindowManager `WindowInfoTracker`/`FoldingFeature` posture data.
- Book/tabletop/separating-fold aware presentation where useful.
- No device-model allowlists or width-only fake fold detection.
- Split-screen, resizable windows and configuration change continuity.
- Hinge/occlusion-safe content placement.

### 13. Desktop windowing & external displays
- Precise-pointer and hardware-keyboard capability detection without device fingerprinting.
- Productivity density, hover affordances, command rail and discoverable shortcuts.
- External-display workspace policy: independent, mirror or profile-following.
- Window-size driven layouts; no assumption that a large display is full-screen.

### 14. Smartpen & Pen Space
- Pressure, tilt, hover, eraser and stylus-button support when Android exposes them.
- Finger rejection during pen drawing where appropriate.
- Local vector ink, undo/redo, export and privacy-bounded AI summaries.
- Pen-optimized adaptive entry without penalizing non-pen devices.

### 15. Notifications & badges
- User-controlled notification-listener capability.
- Package/count-level badge information by default; no message-body ingestion for launcher ranking.
- Dot/count/off modes and disconnect cleanup.
- Notification access is never implied by launcher role.

### 16. Assistant & agent presence · KoSch lead domain
- Character/presence/anchor/scale/opacity and matching voice identity.
- Optional wake word, push-to-talk/manual fallbacks.
- Portal/spawn, idle, gaze, emotion and viseme systems.
- Screen/camera awareness remain explicit session capabilities and default off.
- External actions remain confirmation-gated.
- Character packs share one standardized asset/pose contract.

### 17. AI routing & model/provider freedom · KoSch lead domain
- Deterministic local core first.
- Installed-app, local-runtime, browser and explicit compatible API/provider routes.
- Task-aware, context-aware recommendations using abstract signals only.
- User provider preference never bypasses availability/privacy/offline gates.
- Explicit context preview and explicit provider handoff are separate gates.
- Free provider configuration must keep credentials device-local and secrets out of portable backups.

### 18. Automation & contextual adaptation
- Suggestions and adaptations are explainable and reversible.
- User preference outranks learned suggestions.
- Context never silently grants permissions, enables sensors or performs destructive/external actions.
- Time/device/input/profile signals remain bounded and purpose-specific.

### 19. Privacy, security and compliance
- Least-permission production manifest and packaged-permission CI budget.
- No broad contacts/call/storage/accessibility rights merely for launcher convenience.
- SAF/pickers/system intents instead of blanket access where Android supports them.
- Secrets remain in device-local secure storage/references.
- Screen/camera data is transient unless an explicit future feature states otherwise.
- Play Data Safety, privacy policy, AI transparency/reporting, asset provenance and release-specific compliance are release gates.

### 20. Backup, restore and migration
- Versioned portable workspace/settings/scoped-style/custom-action formats.
- Encrypted user-controlled backup path.
- Device-local widget host ids, permission grants, capture sessions, TTS ids and credentials are excluded or remapped.
- Restore never auto-enables screen/camera/action-execution capabilities.
- Diff/preview/rollback where practical.

### 21. Accessibility
- TalkBack content descriptions and roles for all actionable surfaces.
- Keyboard and non-drag alternatives for spatial editing.
- Reduced motion, scalable text/touch targets and understandable focus order.
- High-contrast/readability options must not depend solely on color.

### 22. Reliability, performance and release quality
- API 36 target/build policy and current Android compatibility.
- Unit + lint + debug/release build + packaged-permission checks + managed-device instrumentation.
- Bounded caches/indexes/history and process-death recovery.
- Large app/widget/page collections tested for responsiveness.
- Release artifact, not only source/debug APK, is the final truth boundary.

## Benchmark policy
For every release-candidate milestone, score each domain 0.1–10.0 against the current strongest relevant launchers. A high global average cannot hide a weak domain. Any domain below the release target remains an explicit gap.

**Target:** no core benchmark domain below 9.5/10 for the intended professional-user release, with KoSch-leading AI/Assistant/privacy domains aiming higher.
