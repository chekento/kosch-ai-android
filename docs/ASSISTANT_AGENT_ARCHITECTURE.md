# Living AI Assistant – Agent Architecture

## Zielbild

Der KoSch Assistant ist kein fest verdrahteter Avatar. Das System trennt fünf Schichten strikt voneinander:

1. **Character** – Visuals, Asset-Pack, Animationen, Name.
2. **Persona** – Stil, Ton, Verhaltensprofil.
3. **Voice** – STT/TTS-Provider und Stimme.
4. **Intelligence** – lokaler Planner, lokales Modell oder explizit gewählter externer Provider.
5. **Capabilities** – Wake Word, Screen/Cam-Kontext und ausführbare Aktionen mit eigenen Vertrauensgrenzen.

Dadurch können `default`, `anime_female`, `anime_male` und spätere Charaktere dieselbe Agentenlogik verwenden, ohne dass Berechtigungen, Modellwahl oder Chatverlauf an den Avatar gekoppelt werden.

## Presence Modes

- `PORTAL_ONLY` – nur Portal-/Spawn-Präsenz, kein dauerhafter Avatar.
- `AMBIENT` – dezente Anwesenheit ohne aktive Agentenaktionen.
- `FLOATING` – frei positionierbarer Companion über dem Home-Raum.
- `FULL_COMPANION` – vollständige Animation, Voice, Emotion und Interaktion.
- `AGENT` – zusätzlich freigegebene Tool-/Action-Fähigkeiten; weiterhin confirmation-first.

Reduced Motion bleibt ein globaler Sicherheits-/Accessibility-Override und darf Presence-Mode-Animationen reduzieren oder durch statische Zustände ersetzen.

## Runtime State Machine

`DISABLED → IDLE → ARMED → LISTENING → THINKING → SPEAKING`

Zusätzliche explizite Zustände:

- `OBSERVING_SCREEN`
- `OBSERVING_CAMERA`
- `ACTING`
- `PRIVACY_BLOCKED`
- `ERROR`

Screen/Cam sind **keine Unterzustände von Idle**. Dadurch können UI und Nutzer jederzeit eindeutig erkennen, wann Bildkontext aktiv ist.

## Wake Word

Persistierbare Modi:

- `OFF`
- `COMPUTER`
- `ASSISTANT_NAME`
- `CUSTOM`

`localWakeWordOnly=true` ist der sichere Default. Ein späterer Wake-Word-Backend darf Audio nicht an einen Netzwerkprovider senden, solange der Nutzer diese Vertrauensgrenze nicht explizit geändert hat.

## Screen- und Camera-Awareness

Die portable Konfiguration speichert ausschließlich, **ob** die Fähigkeit grundsätzlich freigeschaltet ist. Sie speichert niemals:

- MediaProjection-Tokens,
- Kamera-Session-Tokens,
- URI-Grants,
- dauerhafte Capture-Berechtigungen,
- Screenshots oder Kameraframes als versteckte Historie.

Jede Beobachtung benötigt zusätzlich eine sichtbare Session und den passenden Android-Consent. Die reine Setting-Aktivierung reicht nicht aus.

Policy-Reihenfolge:

1. Assistant aktiv?
2. Capability in Settings freigegeben?
3. Session sichtbar?
4. Android-/Platform-Consent vorhanden?
5. Erst dann Übergang in `OBSERVING_SCREEN` oder `OBSERVING_CAMERA`.

Wird die Session unsichtbar, muss die Beobachtung beendet und der Zustand auf `PRIVACY_BLOCKED` oder `IDLE` zurückgeführt werden.

## Action Safety

Aktionen werden in Risikoklassen aufgeteilt:

- `LOCAL_READ_ONLY`
- `LOCAL_REVERSIBLE`
- `EXTERNAL_SIDE_EFFECT`
- `SENSITIVE_SIDE_EFFECT`

Lokales Lesen darf ohne Agent-Execution-Schalter erfolgen. Side-Effect-Aktionen benötigen `actionExecutionEnabled`. Externe oder sensitive Side Effects verlangen standardmäßig eine explizite Nutzerbestätigung unmittelbar vor der Ausführung.

Das ist die Grundlage für spätere Tool-Calling-/Agent-Funktionen, ohne dem Modell pauschale Android-Rechte zu geben.

## Character Packs

Jeder Charakter verwendet dieselben standardisierten Slots des bestehenden Assistant-Asset-Systems. Charakter-Packs liefern nur Assets und Metadaten. Die Runtime fällt bei fehlenden/inkompatiblen optionalen Assets auf den Default-Fallback zurück.

Built-ins in Stage E:

- `default`
- `anime_female`
- `anime_male`

Die beiden Anime-Profile sind bereits als first-class Charakter-IDs vorgesehen; die finalen Bild-Assets werden separat in das standardisierte Pack-Schema eingehängt und können APK-seitig komprimiert werden.

## Persistenzgrenze

`AssistantAgentStore` enthält nur portable Präferenzen:

- Charakter,
- Presence Mode,
- Wake-Word-Modus,
- optionales Custom Wake Word,
- local-only Wake-Word-Policy,
- Capability-Toggles,
- Action-Execution und Confirmation-Policy.

Nicht portable oder sensible Runtime-Grants werden bewusst nicht gespeichert.

## Nächste technische Stufen

1. Settings-Center UI für Character, Presence, Wake Word und Capability-Toggles.
2. sichtbare Privacy-Chips für Mic/Screen/Cam/Acting.
3. lokales Wake-Word-Backend mit Lifecycle-/Akku-Gates.
4. MediaProjection-Bridge für explizites Screen Share.
5. CameraX-Bridge mit sichtbarer Preview/Session-Anzeige.
6. Agent Action Router auf Basis der bestehenden lokalen Command-/System-Fähigkeiten.
7. Provider-unabhängige Realtime-Voice-Abstraktion.
8. finale Character-Packs für Default, Anime-Frau und Anime-Mann.
