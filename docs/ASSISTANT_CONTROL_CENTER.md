# Living AI Assistant – Control Center

## Einstieg

Der bestehende Assistant-Sheet besitzt zwei Ansichten:

- **Chat** – lokaler Launcher-Core, explizite Spracheingabe, TTS und bewusster Provider-Handoff.
- **Steuerung** – Character, Rufname, Voice, Presence-Konfiguration, Wake-Word-Konfiguration, Privacy und Agent-Rechte.

Die Steuerung ist absichtlich Teil des Assistant-Sheets. Das globale Settings Center verweist für echte Assistant-Runtime-Rechte auf diesen Control Center, statt einen zweiten widersprüchlichen Zustand zu führen.

## Charakter & Rufname

Built-in Profile:

- `default`
- `anime_female`
- `anime_male`

Der frei wählbare Assistentenname ist unabhängig vom Character-Pack. Bei `Wake Word = Assistentenname` wird zuerst dieser Rufname verwendet; ohne Rufname fällt die gespeicherte Wake-Word-Konfiguration auf den Character-Displaynamen zurück.

Character-Wechsel verändert nicht automatisch Provider, Berechtigungen, Chatverlauf oder Agent-Rechte. Solange finale kalibrierte WebP-Packs fehlen, bleiben alle drei IDs funktionsfähig; Anime Female und Anime Male besitzen eigene animierte Procedural-Fallbacks statt still denselben Default-Roboter zu rendern.

## Voice-Gender-Vertrag

- `anime_female` benötigt den **weiblichen Voice-Slot**.
- `anime_male` benötigt den **männlichen Voice-Slot**.
- `default` ist neutral und darf die Systemstimme verwenden.

Android TTS liefert kein plattformweit verlässliches Geschlechtsmerkmal für konkrete Stimmen. Deshalb werden installierte TTS-Stimmen im Control Center angezeigt und nach Probehören bewusst einem gerätegebundenen Slot zugeordnet.

Harte Regeln:

1. Ein weiblicher Character verwendet niemals den männlichen Slot.
2. Ein männlicher Character verwendet niemals den weiblichen Slot.
3. Dieselbe konkrete Android-TTS-Stimme darf nicht gleichzeitig weiblichem und männlichem Slot zugeordnet sein.
4. Fehlt bei einem geschlechtsspezifischen Character die passende Zuordnung, bleibt TTS fail-closed statt auf eine unpassende Stimme zurückzufallen.
5. Netzwerkabhängige TTS-Stimmen werden sichtbar gekennzeichnet; lokale Stimmen werden bevorzugt sortiert.

Die konkreten Android-TTS-Stimmennamen werden in `AssistantDeviceVoiceStore` **nur gerätegebunden** gespeichert. Sie gehören nicht in portable Launcher-Settings oder Backups.

Die Lippensynchronisation verwendet den verbindlichen 15-Visem-Vertrag:

`sil pp ff th dd kk ch ss nn rr aa ee ih oh ou`

Der E-Slot erzeugt daher den Runtime-Dateinamen `..._mouth_viseme_ee.webp`.

## Wake Word · CORE_READY / vorbereitet

Persistierbare Modi:

- Aus
- `Computer`
- Assistentenname
- Eigenes Wake Word

Das eigene Wake Word darf während der Bearbeitung als Entwurf gespeichert werden, wird aber erst ab zwei Zeichen als gültige Konfiguration aufgelöst. `localWakeWordOnly=true` bleibt der sichere Default.

**Wichtig:** Das aktuelle APK enthält noch keinen kontinuierlich lauschenden Wake-Word-Audiodetektor. Die Auswahl eines Modus startet keine Mikrofonaufnahme und aktiviert keinen Hintergrunddienst. Der Launcher besitzt hierfür bewusst kein `RECORD_AUDIO`-Recht. Die UI kennzeichnet diesen Bereich deshalb sichtbar als **vorbereitet** und meldet `Detektor nicht aktiv`.

Ein späteres lokales Wake-Word-Backend benötigt eine eigene explizite Runtime-, Permission-, Privacy-, Akku- und Play-Review-Grenze.

## Screen & Camera Awareness · LIVE

Beide Capability-Schalter sind standardmäßig **AUS**.

Ein Modell, Provider, Agentenplan oder eine Automation darf den Wechsel von `false` zu `true` nicht auslösen. Dafür existiert ausschließlich der explizite user-initiierte Control-Center-Pfad.

Auch nach Aktivierung des Schalters startet noch keine Aufnahme. Eine echte Session benötigt weiterhin:

- aktiven Assistant,
- sichtbare Nutzeroberfläche,
- passenden Android-Consent bzw. Runtime-Permission,
- aktiven Sessionzustand,
- jederzeit erreichbaren Stop-Pfad.

Platform-Grants und Capture-Tokens werden nicht als portable Einstellungen persistiert.

### Screen Share

Die MediaProjection-Bridge ist **angeschlossen**. Nach explizitem Android-Screen-Capture-Consent läuft Screen Share über den sichtbaren `AssistantScreenShareService` mit `foregroundServiceType="mediaProjection"`. Wird der Assistant deaktiviert oder die Session beendet, wird auch die Observation beendet.

### Camera

Die CameraX-Bridge ist **angeschlossen**. Camera Awareness benötigt die manuelle Capability-Freigabe plus Android-Kamera-Permission. Die Session läuft nur mit sichtbarer Preview im Control Center; verlässt die Preview ihren Lifecycle, wird CameraX wieder ungebunden.

Nur eine visuelle Quelle wird gleichzeitig aktiv gehalten. Provider-Verbindungen schalten Screen oder Camera niemals automatisch frei.

## One-shot Visual Context · LIVE, noch ohne Modelltransfer

Aus einer bereits sichtbaren, freigegebenen Screen- oder Camera-Session kann genau ein aktueller Visual-Context-Frame angefordert werden. Der Frame ist kurzlebig im RAM, besitzt eine enge TTL und wird danach verworfen.

Der aktuelle Stage-H-Pfad **überträgt diesen Frame noch an kein LLM**. Das ist eine absichtliche Grenze: Capture-Fähigkeit und externer Modelltransfer bleiben zwei getrennte Freigabestufen.

## Privacy Live

Die Steuerung zeigt separate Statusindikatoren für:

- `MIC`
- `SCREEN`
- `CAM`
- `ACTING`

`MIC` bezeichnet hier die sichtbare, vom Nutzer gestartete Android-Speech-Recognition-Interaktion; KAL hält dafür kein eigenes dauerhaftes Mikrofon offen. Ein Capability-Schalter ist ausdrücklich nicht dasselbe wie ein aktiver Sensor-/Agent-Zustand.

## Presence · CORE_READY / vorbereitet

Persistierbare Modi:

- Portal
- Ambient
- Floating
- Full Companion
- Agent

Diese Werte bilden den stabilen Architekturvertrag für die weitere Runtime. Die vollständige visuelle und verhaltensseitige Differenzierung aller fünf Modi ist im aktuellen APK **noch nicht komplett**. Die UI kennzeichnet den Bereich deshalb als vorbereitet. Presence darf unabhängig vom Reifegrad niemals automatisch Capability- oder Sicherheitsgrenzen aufheben.

## Agent-Rechte

`Aktionen ausführen` und `Externe Aktionen bestätigen` bleiben separate Kontrollen. Der zentrale Risk-Classifier trennt lokale Read-only-/reversible Aktionen von externen oder sensitiven Side Effects. Sensitive Aktionen bleiben confirmation-first und können nicht durch eine Preference auf stille Ausführung heruntergestuft werden.

## Netzwerkgrenze

Das Gesamt-APK enthält `INTERNET` und `ACCESS_NETWORK_STATE` für die separate optionale Provider-Connections-Schicht. Diese Android-Permissions sind **kein** Assistant-Cloud-Opt-in. Cloud Access ist produktseitig standardmäßig AUS; ein Provider muss explizit verbunden sein und Direct Provider Requests benötigen eine sichtbare Nutzeraktion.

Der Assistant-Chat selbst führt freie generative Anfragen derzeit nicht heimlich direkt aus, sondern bietet den bewussten Provider-/AI-Hub-Handoff. Screen/Camera-Inhalte werden durch eine Provider-Verbindung nicht automatisch angehängt.

## Noch bewusst nicht vorgetäuscht

Der aktuelle Stand behauptet insbesondere **nicht**:

- dass finale kalibrierte Anime-/Default-WebP-Packs bereits im APK liegen,
- dass ein kontinuierlicher Wake-Word-Detektor aktiv ist,
- dass alle fünf Presence Modes bereits vollständig unterschiedlich gerendert/ausgeführt werden,
- dass Visual-Context-Frames bereits an ein LLM übertragen werden,
- dass ein generatives lokales Modell im APK gebündelt ist,
- dass Agent-Aktionen Android-Sicherheits- oder Bestätigungsgrenzen umgehen können.

Diese Grenzen sind im Settings-Maturity-Katalog, im Control Center und in den automatisierten Tests ausdrücklich sichtbar.