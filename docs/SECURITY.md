# Sicherheit und Datenschutz

## M2.4-Dateninventar

KoSch verarbeitet lokal:

- Uhrzeit, Akkustand/Ladestatus, validierte Netzverfügbarkeit und aktive persönliche Audioausgänge;
- installierte startbare Launcher-Activities, ihre über Android zugängliche Profilzugehörigkeit und von Apps veröffentlichte Shortcuts;
- Szene, Home-Raum, Kartenpositionen, Dock-Pins, lokale Ordner, lokal verborgene App-Schlüssel, Widget-Host-IDs sowie höchstens 512 lokale App-Startsignale aus Schlüssel, Anzahl und letztem Zeitpunkt;
- lokal gezeichnete Pen-Space-Vektorstriche mit Werkzeug, normalisiertem `x/y`, Druck und Neigung; begrenzt auf 100 Striche und 2.048 Punkte je Strich;
- nach ausdrücklicher Android-Auswahl: URI-Metadaten und bei erkannten Textformaten höchstens 4.096 Zeichen Textpräfix;
- nach Wahl eines Datei-Arbeitsraums: genau eine persistierte SAF-Tree-URI, Pfadnavigation und höchstens 500 sichtbare direkte Kind-Metadatensätze; keine Dateiinhalte für die Arbeitsraum-Zusammenfassung;
- nach einmaliger Android-Kontaktauswahl: Anzeigename und eine gewählte Telefonnummer ausschließlich im flüchtigen Telefon-Sheet;
- lokales Audit aus Zeitpunkt, festem Aktionstyp und Ergebnis, begrenzt auf 250 Ereignisse und 90 Tage;
- bei manuellem Backup: ein versionierter Workspace-Snapshot und sein verschlüsselter Export-Envelope;
- während eines offenen Android-Zieldialogs: höchstens 8 MiB Exportpayload in einer privaten No-Backup-Datei sowie ein zufälliger, einmal nutzbarer Saved-State-Token;
- Text, den die Person in `⌘ Ask` eingibt oder aus der gestarteten Spracherkennungs-Activity übernimmt.

Nach separatem Android-Opt-in werden für Notification Dots ausschließlich Paketnamen und Anzahlen aktiver, nicht laufender Meldungen verarbeitet. Nicht kopiert oder gespeichert werden Notification-Titel, Text, Personen, Extras, Aktionen oder RemoteViews.

Nicht gelesen werden Standort, gesamtes Adressbuch, Kalender, Anrufliste, SMS, Zwischenablage, Fotosammlung, permanenter Mikrofonstream oder Bildschirm. Aus einer gewählten Binärdatei werden keine vermeintlichen Textinhalte extrahiert. Namen, Vendor-/Product-ID und Seriennummer erkannter Eingabegeräte werden nicht im Workspace gespeichert.

## Berechtigungsbudget

M2.4 deklariert als `uses-permission` nur `ACCESS_NETWORK_STATE`. Damit erkennt die Context Engine, ob ein validiertes Netz existiert. Stylus, einmalige Kontaktauswahl und SAF-Tree-Zugriff benötigen keine zusätzliche gefährliche Manifest-Berechtigung. Es gibt bewusst kein:

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

KoSch verwendet `ACTION_DIAL`. Eine erkannte Nummer wird höchstens normalisiert und im System-Wähler vorbereitet. Der tatsächliche Anruf bleibt eine Benutzeraktion. `ACTION_PICK` wird auf den Phone-Datentyp begrenzt; auf Android 17 fordert KoSch den datenschutzfreundlichen System-Picker explizit an. Es gibt kein `READ_CONTACTS`; die gewählte Nummer wird nicht persistiert oder in das Audit geschrieben. KoSch übernimmt keine Default-Dialer-, `InCallService`- oder Notruffunktion.

## Portables Backup und Restore

Der Workspace-Snapshot ist größenbegrenzt und versioniert. `PortableBackupCodec` verwendet PBKDF2-HMAC-SHA-256 mit 210.000 Iterationen, zufälligem 16-Byte-Salt und einen 256-Bit-Schlüssel für `AES/GCM/NoPadding` mit zufälliger 12-Byte-Nonce und 128-Bit-Tag. Formatversion und Iterationszahl sind GCM-AAD. Die Passphrase wird nicht gespeichert; verarbeitbare `CharArray`-/Byte-Puffer werden bestmöglich überschrieben.

Vor dem Restore werden Authentizität, Format, Version, Zeitgrenze, Payload-Größe, Listenlimits, Enum-Werte, Stringlängen, endliche normalisierte Positionen und Stiftwerte geprüft. Die Oberfläche zeigt einen Dry Run und verlangt eine zweite Bestätigung. Erst dann folgt ein synchroner, einzelner Preferences-Commit. Widget-Host-IDs, URI-Grants, Secrets, Notification-Daten und Audit sind ausgeschlossen. Eine zwischenzeitlich geschlossene Backup-Fläche invalidiert laufende Prepare-/Preview-Ergebnisse über einen Request-Token.

Backup-, Audit- und SVG-Exporte werden für den Zeitraum hinter Androids `CreateDocument`-Dialog atomar in `noBackupFilesDir` bereitgestellt. Der Saved State enthält nur einen validierten Token mit Exporttyp, nie den Payload. Die Datei wird einmal konsumiert, bei Abbruch verworfen oder nach 24 Stunden bereinigt; Token und Payload sind auf Typ, Name, Elternpfad und 8 MiB begrenzt.

## Lokales Audit

`LocalAuditLog` besitzt absichtlich kein Freitextfeld. Ein Event enthält nur Millisekunden-Zeitpunkt, `AuditAction` und `AuditOutcome`. So können Prompts, Kontaktwerte, Nummern, Dateinamen, Pfade und Benachrichtigungsinhalte nicht über eine frei beschreibbare Detailspalte einsickern. Die Liste ist auf 250 Einträge beziehungsweise 90 Tage begrenzt, kann als dreispaltige CSV über SAF exportiert und vollständig gelöscht werden. Sie ist vom Workspace-Backup getrennt.

## Dateien

Die Einzeldatei-Route verwaltet höchstens eine langfristige read-only Freigabe. Eine neue Wahl ersetzt die alte; „Dateizugriff vergessen“ löst sie. Die Textanalyse ist auf bekannte Formate und 4.096 Zeichen begrenzt.

Der Datei-Arbeitsraum ist eine getrennte, explizite `ACTION_OPEN_DOCUMENT_TREE`-Freigabe mit READ und WRITE. KoSch besitzt nur einen Baum, prüft Provider/Tree-Grenze, listet höchstens 500 direkte Kinder und richtet sichtbare Aktionen nach den vom `DocumentsProvider` gemeldeten Flags aus. Namen sind auf 120 Zeichen begrenzt; Pfadseparatoren, Steuerzeichen, `.` und `..` werden abgelehnt.

- Ordner erstellen und Umbenennen: Vorschau plus Bestätigung;
- Umbenennen: genau ein lokales Undo, solange der Provider die Rückbenennung zulässt;
- Löschen: gesonderte destruktive Bestätigung, danach direkter Provider-Aufruf und **kein** vorgetäuschtes Undo;
- Öffnen: sichtbare externe Android-App;
- Zusammenfassung: nur Name, MIME-Kategorie, bekannte Größe und Änderungszeit; kein Inhaltsindex;
- Vergessen/Wechsel: persistierte URI-Freigabe wird gelöst.

SAF-Provider können Cloudspeicher abbilden. „Lokal analysiert“ bedeutet daher, dass KoSch keine eigene Cloud/API nutzt; der ausgewählte Provider kann seine Daten nach eigenen Regeln laden. Move/Copy, Rekursion, Volltextsuche, Papierkorbgarantie und Malware-Scan sind nicht implementiert.

## Lokale Personalisierung und Sichtbarkeit

`LocalUsageModel` speichert pro App-Schlüssel nur Startanzahl und letzten Startzeitpunkt, begrenzt auf 512 Einträge. Es liest weder Androids globalen Usage-Verlauf noch fremde App-Inhalte. Die Person kann alle Signale nach zweiter Bestätigung löschen.

„App verbergen“ entfernt eine App nur aus normalen KoSch-Sammlungen, Dock und Ordneransichten. Die App bleibt in der expliziten Ansicht **Verborgen**, in Android-Einstellungen und gegebenenfalls in anderen Launchern sichtbar. Das ist Organisation, kein Schutz vor Dritten und keine Deaktivierung.

## Notification Dots

Der opt-in Listener hält nur ein flüchtiges `packageName → count`-Abbild. Laufende Meldungen und Gruppenzusammenfassungen werden ausgeschlossen; `NotificationListenerService.Ranking.canShowBadge()` respektiert die Badge-Entscheidung von Android und Kanal. Benachrichtigungsinhalte dürfen auch für spätere KI-Triage nicht implizit freigeschaltet werden.

## Smartpen und Ink-Daten

Aktuelle Stiftfähigkeiten, Druck, Neigung, Orientierung, Hover, Werkzeug und Tastenstatus leben im Prozesszustand. Persistiert wird nur die bewusst auf Pen Space erzeugte, begrenzte Vektorgrafik. Die Zeichenfläche akzeptiert ausschließlich Stylus-/Eraser-Werkzeuge; Fingerereignisse werden verworfen. Es gibt weder Cloud-Upload noch OCR, Handschriftprofil, biometrische Identifikation oder semantische Analyse der Striche.

Die Persistenz nutzt normalisierte Koordinaten und Schema v6. Eingaben werden auf endliche Werte, Bereichsgrenzen, maximale Punktzahl und maximale Strichzahl reduziert. Der manuelle Workspace-Export übernimmt die Striche erst nach Passphrase-Eingabe; Vision-Modell oder Handschrift-Index benötigen weiterhin eine separate Vorschau, explizite Auswahl, Retention und vollständige Löschung.

## Profile und Private Space

Apps werden mit ihrem Android-`UserHandle` abgefragt und gestartet. Lokale Schlüssel verwenden eine vom System gelieferte Benutzer-Seriennummer, damit gleiche Pakete verschiedener Profile nicht kollidieren. KoSch zeigt Androids gebadgte Icons und umgeht gesperrte Profile nicht.

`ACCESS_HIDDEN_PROFILES` wird nicht deklariert. Android Private Space bleibt dem System-Launcher überlassen, bis KoSch einen getrennten Container, Hide/Show, Lock/Unlock, Authentifizierungsfluss und Tests gegen Label-, Badge-, Search- und Recency-Leaks vollständig implementiert hat.

## Externe KI-Übergaben

Freitext verlässt KoSch erst nach Wahl eines konkreten Ziels und Tipp auf Öffnen/Teilen. Installierte Apps erhalten einen expliziten `ACTION_SEND`; andernfalls öffnet KoSch eine HTTPS- oder Open-Source-Projektseite. KoSch besitzt selbst keine Netzberechtigung und sendet keine Modellanfrage.

PocketPal, ChatterUI und Maid sind optionale Drittprojekte. Die Anzeige ihrer Lizenz und lokalen Fähigkeit ist keine Sicherheitszertifizierung. Installation, Modellquelle und Modelllizenz müssen separat geprüft werden.

## Secret-Grenze

Der ruhende `SecureCredentialVault` nutzt Android Keystore sowie AES-256/GCM. Schlüsselmaterial bleibt nicht exportierbar; Ciphertexte sind per Provider-ID als Associated Data gebunden. `allowBackup=false` verhindert App-Daten-Backup. Der Quellcode enthält keine Provider-Secrets.

M2.4 fordert keine API-Schlüssel an und liest den Vault nicht aus. Vor einem aktiven API-Modul sind zwingend:

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

## Bekannte M2.4-Risiken

- keine instrumentierten Geräte-/OEM-Tests;
- Smartpen-Erkennung und Ink-Latenz sind noch nicht gegen ein USI-/S-Pen-/Pixel-Pen-Gerätelabor validiert;
- eigene `PressureInkView` resampled lange Striche und exportiert SVG, nutzt aber noch keine gemessene Historical-/Coalesced-Event-Latenzabnahme;
- Widget-Größenpresets, Reihenfolge und Undo sind implementiert; freie Platzierung, Stacks und geräteübergreifendes Provider-Restore-Mapping bleiben unvollständig;
- Dateianalyse ist heuristisch und erkennt keine Schadsoftware;
- externe Share-Ziele können Text anders behandeln als erwartet;
- Voice hängt vom installierten Android-Spracherkenner ab und ist nicht garantiert offline;
- Vault-Sicherheitscode ist vorbereitet, aber noch nicht durch End-to-End-Key-Rotation oder Hardware-Attestation validiert;
- keine Security-Audit- oder Penetration-Test-Freigabe;
- Backup-Kryptografie ist unit-getestet, aber noch nicht unabhängig auditiert oder über Prozess-Tod/OEM-Dateiprovider instrumentiert getestet;
- Notification-Dot-Semantik ist noch nicht gegen alle Work-/Private-Profile-Zustände und OEM-Service-Killer geprüft;
- SAF-Create/Rename/Delete sind nicht gegen eine repräsentative lokale/Cloud/OEM-Provider-Matrix instrumentiert getestet; Provider können abweichende Semantik besitzen;
- lokales App-Ranking ist transparent und löschbar, aber noch nicht in einer Langzeitstudie auf Fehlpriorisierung, Fairness oder Work-Profile-Leaks geprüft;
- App-Verbergen ist keine Android-Sicherheitsfunktion;
- ViewModel und Pending-Export-Token sind unit-/quellseitig abgesichert, aber vollständiger Prozess-Tod bleibt auf realen Geräten zu instrumentieren.

Der Launcher ist daher Alpha-Software und sollte zunächst auf Emulator/Zweitgerät getestet werden.
