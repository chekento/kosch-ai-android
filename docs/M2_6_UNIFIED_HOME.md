# M2.6 Unified Home · Stage B

This stage turns `WorkspaceDocument` v7 into a user-facing launcher Home surface.

## Delivered in this slice

- the existing migrated scene pages remain available as compatibility pages;
- user-created Home pages (`sceneAdapter == null`) can be created, renamed, reordered and deleted;
- at least one workspace page is always retained;
- apps and launcher folders can be placed as portable workspace items;
- apps/folders use deterministic 2x2 cells on the existing 12x12 logical grid;
- placement and movement use deterministic collision/reflow rules;
- explicit up/down/left/right controls provide a keyboard/TalkBack-compatible alternative before pointer drag/drop lands;
- one-step Home editing undo is retained across Activity recreation through `LauncherViewModel`;
- the existing dock, Apps route, Ask route and system surfaces remain reachable;
- v7 Home pages/items are included in portable backup format version 3;
- backup versions 1 and 2 remain importable;
- Android `appWidgetId` values remain device-bound and are rejected from portable v7 data.

## Deliberate next slices

### Interaction slice

- direct pointer/stylus drag/drop on v7 items;
- snap/drop preview;
- cross-page drag targets;
- visual edit handles and reduced-motion behavior.

The explicit move buttons remain even after drag/drop is added so drag is never the only interaction path.

### Unified widgets

- place hosted widgets on the same v7 grid;
- keep the stable portable workspace item ID separate from the local Android widget host ID;
- show a safe remap placeholder when a restored widget has no valid local binding;
- provider-constrained resize.

### Widget stacks

- explicit stack membership and ordering;
- persisted active index;
- accessible previous/next controls;
- no hidden background automation.

## Safety / recovery boundaries

- no new Android permissions;
- corrupt or future v7 state keeps the existing recovery/fallback behavior;
- legacy scene writes update legacy action-tile adapters without hijacking an active user-created Home page;
- user Home pages are not represented by Android widget host IDs or other device-local identifiers;
- backup v3 preserves the portable v7 document, while widget host IDs, document grants, credentials, notification data and audit data remain local to the device.

## Score discipline

This source change does not itself justify a benchmark increase. Launcher-parity claims are updated only after the exact merge candidate passes unit/lint/release/permission gates and the API 36 managed-device evidence lane.
