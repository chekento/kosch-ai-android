# Living AI Assistant – Stage F Control Center

## Einstieg

Der bestehende Assistant-Sheet besitzt jetzt zwei gleichwertige Ansichten:

- **Chat** – lokaler Launcher-Core, Spracheingabe, TTS und bewusster Provider-Handoff.
- **Steuerung** – Character, Rufname, Voice, Presence, Wake Word, Privacy und Agent-Rechte.

Die Steuerung ist absichtlich Teil des Assistant-Sheets. Wenn das globale Settings Center aus dem Home-Studio-Track zusammengeführt wird, kann derselbe Control-Center-Composable dort wiederverwendet werden.

## Charakter & Rufname

Built-in Profile:

- `default`
- `anime_female`
- `anime_male`

Der frei wählbare Assistentenname ist unabhängig vom Character-Pack. Bei `Wake Word = Assistentenname` wird zuerst dieser Rufname verwendet; ohne Rufname fällt die Aktivierung auf den Character-Displaynamen zurück.

Character-Wechsel verändert nicht automatisch Provider, Berechtigungen, Chatverlauf oder Agent-Rechte.

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

## Wake Word

Modi:

- Aus
- `Computer`
- Assistentenname
- Eigenes Wake Word

Das eigene Wake Word darf während der Bearbeitung als Entwurf gespeichert werden, wird aber erst ab zwei Zeichen als nutzbare Aktivierung aufgelöst. `localWakeWordOnly=true` bleibt der sichere Default.

Stage F konfiguriert das Wake Word; ein permanentes lokales Wake-Word-Audio-Backend wird erst in einer nachfolgenden Stufe angebunden.

## Screen & Camera Awareness

Beide Schalter sind standardmäßig **AUS**.

Ein Modell, Provider, Agentenplan oder eine Automation darf den Wechsel von `false` zu `true` nicht auslösen. Dafür existiert ausschließlich der explizite user-initiierte Control-Center-Pfad.

Auch nach Aktivierung des Schalters startet keine Aufnahme. Eine echte Session benötigt weiterhin:

- sichtbare Nutzeroberfläche,
- passenden Android-Consent,
- aktiven Sessionzustand,
- jederzeit erreichbaren Stop-Pfad.

Platform-Grants und Capture-Tokens werden nicht persistiert.

## Privacy Live

Die Steuerung zeigt separate Statusindikatoren für:

- `MIC`
- `SCREEN`
- `CAM`
- `ACTING`

Ein Capability-Schalter ist ausdrücklich nicht dasselbe wie ein aktiver Sensor-/Agent-Zustand.

## Presence

Verfügbare Modi:

- Portal
- Ambient
- Floating
- Full Companion
- Agent

Presence ändert die Darstellung, nicht automatisch die Capability- oder Sicherheitsgrenzen.

## Agent-Rechte

`Aktionen ausführen` und `Externe Aktionen bestätigen` bleiben separate Kontrollen. Externe/sensitive Seiteneffekte sind standardmäßig confirmation-first.

## Noch bewusst nicht vorgetäuscht

Stage F schafft Konfiguration und Runtime-Verträge, aber behauptet keine noch nicht implementierte Plattformfähigkeit:

- finale Anime-Bild-Asset-Packs stehen noch aus; bis dahin bleibt der sichere visuelle Fallback aktiv,
- MediaProjection-Screen-Share ist noch nicht als Capture-Bridge angeschlossen,
- CameraX-Live-Session ist noch nicht angeschlossen,
- permanentes lokales Wake-Word-Listening ist noch nicht angeschlossen.

Diese Komponenten folgen auf dem jetzt sichtbaren, testbaren und consent-first Control-Center-Unterbau.
