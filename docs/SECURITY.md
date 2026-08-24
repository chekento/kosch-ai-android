# Sicherheit und Datenschutz

## M2.2-Dateninventar

KoSch verarbeitet lokal:

- Uhrzeit, Akkustand/Ladestatus, validierte Netzverfügbarkeit und aktive persönliche Audioausgänge;
- installierte startbare Launcher-Activities, ihre über Android zugängliche Profilzugehörigkeit und von Apps veröffentlichte Shortcuts;
- Szene, Home-Raum, Kartenpositionen, Dock-Pins, lokale Ordner, Widget-Host-IDs und zuletzt über KoSch gestartete profilgebundene App-Schlüssel;
- lokal gezeichnete Pen-Space-Vektorstriche mit Werkzeug, normalisiertem `x/y`, Druck und Neigung; begrenzt auf 100 Striche und 2.048 Punkte je Strich;
- nach ausdrücklicher Android-Auswahl: URI-Metadaten und bei erkannten Textformaten höchstens 4.096 Zeichen Textpräfix;
- Text, den die Person in `⌘ Ask` eingibt oder aus der gestarteten Spracherkennungs-Activity übernimmt.

Nach separatem Android-Opt-in werden für Notification Dots ausschließlich Paketnamen und Anzahlen aktiver, nicht laufender Meldungen verarbeitet. Nicht kopiert oder gespeichert werden Notification-Titel, Text, Personen, Extras, Aktionen oder RemoteViews.

Nicht gelesen werden Standort, Kontakte, Kalender, Anrufliste, SMS, Zwischenablage, Fotosammlung, permanenter Mikrofonstream oder Bildschirm. Aus einer gewählten Binärdatei werden keine vermeintlichen Textinhalte extrahiert. Namen, Vendor-/Product-ID und Seriennummer erkannter Eingabegeräte werden nicht im Workspace gespeichert.

## Berechtigungsbudget

M2.2 deklariert als `uses-permission` nur `ACCESS_NETWORK_STATE`. Damit erkennt die Context Engine, ob ein validiertes Netz existiert. Stylus-Erkennung über `InputManager` benötigt keine zusätzliche gefährliche Berechtigung. Es gibt bewusst kein:

- `INTERNET`;
- `CALL_PHONE` oder Kontakte-/Anruflistenrecht;
- `READ_MEDIA_*` oder `MANAGE_EXTERNAL_STORAGE`;
- `QUERY_ALL_PACKAGES`;
- Accessibility Service;
- `ACCESS_HIDDEN_PROFILES`;
- standardmäßig erteilten Benachrichtigungszugriff;
- Standort-/Kalender-/Mikrofonrecht.

Die App deklariert einen durch das System gebundenen Notification-Listener-Service, aber keine entsprechende Laufzeitberechtigung. Erst die Person kann ihn in Androids geschützter Einstellungsseite aktivieren; der Core funktioniert ohne ihn. Spracherkennung, Dateiauswahl, Telefon, Widgets und Systemeinstellungen werden als sichtbare Android-Aktivitäten gestartet. Der jeweilige System-/Zielprozess besitzt seine eigenen Datenschutzregeln.

## Telefon

KoSch verwendet `ACTION_DIAL`. Eine erkannte Nummer wird höchstens normalisiert und im System-Wähler vorbereitet. Der tatsächliche Anruf bleibt eine Benutzeraktion. KoSch übernimmt keine Default-Dialer-, `InCallService`- oder Notruffunktion und behauptet dies auch nicht.

## Dateien

Das Storage Access Framework gibt nur die vom Nutzer gewählte URI frei. Der Zugriff ist read-only. M2.2 verwaltet höchstens eine langfristige Freigabe, löst den vorherigen Zugriff nach erfolgreicher neuer Wahl und bietet „Dateizugriff vergessen“. Die lokale Analyse ist begrenzt und fehlertolerant; es gibt keine Lösch-, Rename-, Move- oder Upload-Aktion. Ein vorgeschlagener Dateiname ist nur Text in einer Vorschau.

## Notification Dots

Der opt-in Listener hält nur ein flüchtiges `packageName → count`-Abbild. Laufende Meldungen und Gruppenzusammenfassungen werden ausgeschlossen; `NotificationListenerService.Ranking.canShowBadge()` respektiert die Badge-Entscheidung von Android und Kanal. Benachrichtigungsinhalte dürfen auch für spätere KI-Triage nicht implizit freigeschaltet werden.

## Smartpen und Ink-Daten

Aktuelle Stiftfähigkeiten, Druck, Neigung, Orientierung, Hover, Werkzeug und Tastenstatus leben im Prozesszustand. Persistiert wird nur die bewusst auf Pen Space erzeugte, begrenzte Vektorgrafik. Die Zeichenfläche akzeptiert ausschließlich Stylus-/Eraser-Werkzeuge; Fingerereignisse werden verworfen. Es gibt weder Cloud-Upload noch OCR, Handschriftprofil, biometrische Identifikation oder semantische Analyse der Striche.

Die Persistenz nutzt normalisierte Koordinaten und Schema v3. Eingaben werden auf endliche Werte, Bereichsgrenzen, maximale Punktzahl und maximale Strichzahl reduziert. Ein späterer Export, Vision-Modell oder Handschrift-Index benötigt eine separate Vorschau, explizite Auswahl, Retention und vollständige Löschung.

## Profile und Private Space

Apps werden mit ihrem Android-`UserHandle` abgefragt und gestartet. Lokale Schlüssel verwenden eine vom System gelieferte Benutzer-Seriennummer, damit gleiche Pakete verschiedener Profile nicht kollidieren. KoSch zeigt Androids gebadgte Icons und umgeht gesperrte Profile nicht.

`ACCESS_HIDDEN_PROFILES` wird nicht deklariert. Android Private Space bleibt dem System-Launcher überlassen, bis KoSch einen getrennten Container, Hide/Show, Lock/Unlock, Authentifizierungsfluss und Tests gegen Label-, Badge-, Search- und Recency-Leaks vollständig implementiert hat.

## Externe KI-Übergaben

Freitext verlässt KoSch erst nach Wahl eines konkreten Ziels und Tipp auf Öffnen/Teilen. Installierte Apps erhalten einen expliziten `ACTION_SEND`; andernfalls öffnet KoSch eine HTTPS- oder Open-Source-Projektseite. KoSch besitzt selbst keine Netzberechtigung und sendet keine Modellanfrage.

PocketPal, ChatterUI und Maid sind optionale Drittprojekte. Die Anzeige ihrer Lizenz und lokalen Fähigkeit ist keine Sicherheitszertifizierung. Installation, Modellquelle und Modelllizenz müssen separat geprüft werden.

## Secret-Grenze

Der ruhende `SecureCredentialVault` nutzt Android Keystore sowie AES-256/GCM. Schlüsselmaterial bleibt nicht exportierbar; Ciphertexte sind per Provider-ID als Associated Data gebunden. `allowBackup=false` verhindert App-Daten-Backup. Der Quellcode enthält keine Provider-Secrets.

M2.2 fordert keine Schlüssel an und liest den Vault nicht aus. Vor einem aktiven API-Modul sind zwingend:

1. getrenntes Netzwerkmodul/Build Flavor;
2. Kontextvorschau mit Feld-für-Feld-Auswahl;
3. TLS-only plus enge Host-Allowlist oder expliziter Loopback-Modus;
4. Redaction von Logs, Crashreports und Clipboard;
5. Secret-Löschen, Rotation und optional Geräteauthentifizierung;
6. Abbruch, Timeout, Backoff, Kosten-/Rate-Limit und Offline-Verhalten;
7. Audit-Metadaten ohne Promptinhalt als Default;
8. Tests für Prompt Injection und bösartige Toolausgaben.

## Sicherheitsklassen für künftige Aktionen

| Klasse | Beispiele | Regel |
|---|---|---|
| A – lesend/reversibel | App suchen, Szene vorschlagen | direkte lokale Ausführung möglich |
| B – externe Ansicht | Dialer, Datei in App, Einstellungen | Ziel und Daten sichtbar, dann Android-UI |
| C – schreibend/reversibel | Layout, Regel, Dateiname | Vorschau, Bestätigung, Undo |
| D – Kommunikation/Kosten | Nachricht, Kauf, Buchung, API-Kosten | Kontextvorschau + erneute Bestätigung |
| E – destruktiv/privilegiert | Löschen, Kontowechsel, Geräteverwaltung | gesonderte Capability; kein autonomer Standard |

LLM-Ausgaben sind Daten, keine Autorität. Sie dürfen keine Capability direkt erzeugen oder erweitern.

## Bekannte M2.2-Risiken

- keine instrumentierten Geräte-/OEM-Tests;
- Smartpen-Erkennung und Ink-Latenz sind noch nicht gegen ein USI-/S-Pen-/Pixel-Pen-Gerätelabor validiert;
- eigene `PressureInkView` nutzt noch keine Historical-/Coalesced-Event-Optimierung und keinen gemessenen Latenz-Budget-Gate;
- Widget-Restore und Größenänderung noch unvollständig;
- Dateianalyse ist heuristisch und erkennt keine Schadsoftware;
- externe Share-Ziele können Text anders behandeln als erwartet;
- Voice hängt vom installierten Android-Spracherkenner ab und ist nicht garantiert offline;
- Vault-Sicherheitscode ist vorbereitet, aber noch nicht durch End-to-End-Key-Rotation oder Hardware-Attestation validiert;
- keine Security-Audit- oder Penetration-Test-Freigabe;
- Notification-Dot-Semantik ist noch nicht gegen alle Work-/Private-Profile-Zustände und OEM-Service-Killer geprüft;
- Smart Dock/Ordner nutzen transparente String-Heuristiken und lernen noch kein persönliches Modell.

Der Launcher ist daher Alpha-Software und sollte zunächst auf Emulator/Zweitgerät getestet werden.
