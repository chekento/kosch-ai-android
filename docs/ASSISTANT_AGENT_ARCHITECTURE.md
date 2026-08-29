# Living AI Assistant – Agent Architecture

## Zielbild

Der KoSch Assistant ist kein fest verdrahteter Avatar. Das System trennt fünf Schichten strikt voneinander:

1. **Character** – Visuals, Asset-Pack, Animationen und Character-Vertrag.
2. **Persona** – Stil, Ton und Verhaltensprofil.
3. **Voice** – explizite Spracheingabe, TTS und gerätegebundene Stimmenzuordnung.
4. **Intelligence** – lokaler Planner/Core, später lokales Modell oder bewusst gewählter externer Provider.
5. **Capabilities** – Wake-Word-Policy, Screen/Cam-Kontext und ausführbare Aktionen mit jeweils eigenen Vertrauensgrenzen.

Dadurch können `default`, `anime_female`, `anime_male` und spätere Charaktere dieselbe Agentenlogik verwenden, ohne dass Berechtigungen, Modellwahl oder Chatverlauf an den Avatar gekoppelt werden.

## Reifegrad-Prinzip

Ein persistierbares Modell oder eine auswählbare Option ist nicht automatisch eine vollständig laufende Capability.

- **LIVE**: reale Runtime ist verdrahtet und durch passende Gates/Tests abgesichert.
- **CORE_READY**: Datenmodell/Policy/UI-Grundlage existiert, die vollständige Runtime noch nicht.
- **PLANNED**: Architekturplatz ist reserviert, aber noch nicht ausführbar.

Für den aktuellen Stand gilt insbesondere:

- Character-Auswahl und Gender-Voice-Gate: **LIVE**;
- Screen Awareness / MediaProjection-Session: **LIVE**;
- Camera Awareness / sichtbare CameraX-Session: **LIVE**;
- One-shot Visual Context: **LIVE**, jedoch noch ohne Modelltransfer;
- Wake-Word-Konfiguration/Policy: **CORE_READY**, kein kontinuierlicher Detektor;
- Presence-Mode-Konfiguration: **CORE_READY**, noch keine vollständige Differenzierung aller fünf Modi.

## Presence Modes · CORE_READY

Persistierbare Architekturmodi:

- `PORTAL_ONLY` – Zielbild: nur Portal-/Spawn-Präsenz, kein dauerhafter Avatar.
- `AMBIENT` – Zielbild: dezente Anwesenheit ohne aktive Agentenaktionen.
- `FLOATING` – Zielbild: frei positionierbarer Companion über dem Home-Raum.
- `FULL_COMPANION` – Zielbild: vollständige Animation, Voice, Emotion und Interaktion.
- `AGENT` – Zielbild: zusätzlich explizit freigegebene Tool-/Action-Fähigkeiten; weiterhin confirmation-first.

Die Auswahl wird bereits gespeichert und Policy-seitig akzeptiert. Die vollständige visuelle und verhaltensseitige Differenzierung aller fünf Modi ist aber noch nicht komplett implementiert. Deshalb darf diese Liste aktuell nicht als fünf vollständig fertige Runtime-Modi beworben werden.

Reduced Motion bleibt ein globaler Sicherheits-/Accessibility-Override und darf künftige Presence-Mode-Animationen reduzieren oder durch statische Zustände ersetzen.

## Runtime State Machine

`DISABLED → IDLE → ARMED → LISTENING → THINKING → SPEAKING`

Zusätzliche explizite Zustände:

- `OBSERVING_SCREEN`
- `OBSERVING_CAMERA`
- `ACTING`
- `PRIVACY_BLOCKED`
- `ERROR`

`ARMED` ist derzeit ein Agent-State/Policy-Zustand; ohne Wake-Word-Audio-Backend bedeutet er **nicht**, dass das Gerät kontinuierlich auf ein Mikrofon-Wake-Word lauscht.

Screen/Cam sind keine unsichtbaren Idle-Unterzustände. UI und Nutzer können dadurch eindeutig erkennen, wann Bildkontext aktiv ist.

## Wake Word · CORE_READY

Persistierbare Modi:

- `OFF`
- `COMPUTER`
- `ASSISTANT_NAME`
- `CUSTOM`

`localWakeWordOnly=true` ist der sichere Default. `AssistantWakeWordResolver` kann aus Preferences und Character eine gültige Phrase ableiten.

Das aktuelle APK besitzt aber **keinen kontinuierlichen Wake-Word-Audiodetektor**, kein Launcher-eigenes `RECORD_AUDIO`-Recht und keinen versteckten Mikrofon-Hintergrunddienst. Das Setzen eines Wake-Word-Modus startet daher keine Aufnahme. Ein späteres Backend darf erst nach eigenem Runtime-, Permission-, Privacy-, Akku- und Play-Gate als LIVE markiert werden.

## Screen- und Camera-Awareness · LIVE

**Screen- und Camera-Awareness sind ab Werk immer ausgeschaltet.** Weder Agent Mode, Charakterwahl, Provider, Automationen noch ein Modell dürfen diese Fähigkeiten automatisch aktivieren.

Die Aktivierung erfolgt ausschließlich manuell über eine direkte Nutzeraktion im Assistant Control Center. `AssistantAgentStore` blockiert normale Preference-Schreibvorgänge, wenn sie versuchen, Screen oder Camera von `false` auf `true` zu eskalieren. Dafür existiert ein eigener explizit benannter User-Opt-in-Persistenzpfad.

Persistiert wird nur die manuelle Capability-Freigabe. Nie persistiert werden:

- MediaProjection-Tokens,
- aktive Kamera-Session-Tokens,
- URI-Grants als Assistant-Capture-Recht,
- Screenshots oder Kameraframes als versteckte Historie.

Jede Beobachtung benötigt zusätzlich eine sichtbare Session und den passenden Android-Consent. Die reine manuelle Capability-Aktivierung reicht nicht aus.

Policy-Reihenfolge:

1. Assistant aktiv?
2. Capability vom Nutzer manuell freigegeben?
3. Session sichtbar?
4. Android-/Platform-Consent vorhanden?
5. Erst dann Übergang in `OBSERVING_SCREEN` oder `OBSERVING_CAMERA`.

### Screen

Die MediaProjection-Bridge ist angeschlossen. Eine echte Session startet erst nach Androids sichtbarem Capture-Consent und läuft über den privaten `AssistantScreenShareService` als `mediaProjection` Foreground Service. Stop/Disable beendet die Observation wieder.

### Camera

Die CameraX-Bridge ist angeschlossen. Kamera startet nur nach manueller Capability-Freigabe, Runtime-Permission und sichtbarer Preview im Control Center. Verlässt die Preview ihren Lifecycle, wird CameraX ungebunden.

Nur eine visuelle Observation-Quelle wird gleichzeitig aktiv gehalten.

## One-shot Visual Context · LIVE ohne LLM-Transfer

Aus einer bereits sichtbaren Screen-/Camera-Session kann ein einzelner aktueller Context-Frame angefordert werden. Er wird kurzzeitig im RAM gehalten, nach enger TTL verworfen und derzeit noch **an kein LLM übertragen**.

Diese Trennung ist beabsichtigt: Capture-Berechtigung ist keine automatische Erlaubnis zum externen Modelltransfer.

## Voice-Gender-Policy · LIVE

Das Charaktergeschlecht ist für die Sprachausgabe verbindlich:

- weibliche Charaktere → ausschließlich `FEMALE` Voice-Slot,
- männliche Charaktere → ausschließlich `MALE` Voice-Slot,
- neutrale/default Charaktere → neutraler/systemischer Pfad oder explizite neutrale Zuordnung.

Built-in-Zuordnung:

- `anime_female` → `female_default`
- `anime_male` → `male_default`
- `default` → `neutral_default`

Die Policy ist **fail-closed**. Wenn keine passende gerätegebundene Stimme zugeordnet/verfügbar ist, darf die Runtime nicht still auf eine Stimme des anderen Geschlechts zurückfallen.

Androids generisches `TextToSpeech.Voice` besitzt kein plattformweit verlässliches Gender-Feld. Deshalb werden installierte Systemstimmen im Control Center explizit einem FEMALE-/MALE-/NEUTRAL-Slot zugeordnet und können probegehört werden; die Runtime prüft anschließend die Character-Kompatibilität vor jeder Ausgabe.

Der TTS-Visualpfad nutzt exakt diese 15 Visem-Codes:

`sil pp ff th dd kk ch ss nn rr aa ee ih oh ou`

## Speech Input

Aktuelle Spracheingabe erfolgt über Androids sichtbare `RecognizerIntent`-UI nach einem Nutzer-Tap. Dadurch hält KAL selbst kein dauerhaftes Mikrofon offen und benötigt für diesen Pfad kein `RECORD_AUDIO`.

Das ist ausdrücklich nicht dasselbe wie ein Wake-Word-Backend.

## Action Safety

Aktionen werden zentral in Risikoklassen aufgeteilt:

- `LOCAL_READ_ONLY`
- `LOCAL_REVERSIBLE`
- `EXTERNAL_SIDE_EFFECT`
- `SENSITIVE_SIDE_EFFECT`

Lokales Lesen darf ohne Agent-Execution-Schalter erfolgen. Side-Effect-Aktionen benötigen `actionExecutionEnabled`. Externe Aktionen folgen der Confirmation-Policy; sensitive Side Effects verlangen immer explizite Nutzerbestätigung und können nicht durch eine Preference auf stille Ausführung heruntergestuft werden.

Das ist die Grundlage für spätere Tool-Calling-/Agent-Funktionen, ohne dem Modell pauschale Android-Rechte zu geben.

## Character Packs

Jeder Charakter verwendet dieselben standardisierten Slots des bestehenden Assistant-Asset-Systems. Charakter-Packs liefern Visual-Assets/Metadaten; Rechte und Provider werden davon nicht abgeleitet.

Built-ins:

- `default`
- `anime_female`
- `anime_male`

Die finalen kalibrierten WebP-Packs stehen noch aus. Bis dahin bleibt der Default-Roboter-Fallback für `default` aktiv und die beiden Anime-Profile nutzen eigene animierte Procedural-Fallbacks, damit Character-Wechsel bereits sichtbar unterscheidbar ist. Fehlende oder defekte finale Assets dürfen HOME nie crashen.

## Persistenzgrenzen

`AssistantAgentStore` enthält gerätebezogene Assistant-Präferenzen, u. a.:

- Charakter,
- Assistant-Rufname,
- Presence Mode,
- Wake-Word-Modus,
- optionales Custom Wake Word,
- local-only Wake-Word-Policy,
- manuell gesetzte Screen/Camera-Capability-Toggles,
- Action-Execution und Confirmation-Policy.

Separat bleiben:

- `AssistantSessionController`-Einstellungen und flüchtiger Chat,
- gerätegebundene TTS-Stimmennamen,
- MediaProjection-/Camera-Runtime-Grants,
- aktive Observation-Sessions,
- kurzlebige Visual-Context-Frames,
- Provider-Credentials im Keystore-Vault.

Portable Restore darf Screen/Camera oder Action Execution niemals automatisch opt-in setzen.

## Netzwerkgrenze

Das aktuelle Gesamt-APK enthält `INTERNET` und `ACCESS_NETWORK_STATE` für die optionale Provider-Connections-Schicht. Das ist kein automatisches Assistant- oder Cloud-Opt-in. `KalCloudAccessPolicy`, Provider-Verbindung und sichtbare Nutzeraktion bleiben separate Gates. Provider-Konnektivität schaltet keine Screen-/Camera-Fähigkeit frei.

## Nächste technische Stufen

Bereits umgesetzt und nicht mehr als „nächster Schritt“ zu führen:

- Control-Center UI für Character, Voice, Wake-Word-/Presence-Konfiguration und Capability-Toggles;
- Voice-Auswahl mit Vorschau und Gender-Slot-Gate;
- Privacy-Chips für MIC/SCREEN/CAM/ACTING;
- MediaProjection-Screen-Share;
- CameraX-Live-Session;
- Action-Risk-Classifier;
- distinct procedural Anime-Fallbacks.

Weiter offen:

1. echter lokaler Wake-Word-Detektor mit eigener expliziter Permission-/Lifecycle-/Akku-/Play-Grenze;
2. vollständige Runtime-Differenzierung der fünf Presence Modes;
3. final kalibrierte Character-WebP-Packs gemäß Matrix;
4. isoliertes lokales generatives Modell-Backend;
5. explizit consent-basierter multimodaler Visual-Context-Transfer an geeignete Modelle;
6. Ausbau des Agent Action Routers auf freigegebene reversible/externe Tools;
7. provider-unabhängige Realtime-Voice-Abstraktion ohne Aufweichung der Privacy-Gates.