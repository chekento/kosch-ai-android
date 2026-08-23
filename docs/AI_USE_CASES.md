# KI-Usecases und freie Laufzeitoptionen

## Produktregel

KoSch trennt drei Ebenen sichtbar:

1. **Local Core:** deterministische Funktionen, sofort und ohne API verfügbar;
2. **lokales Modell:** optionaler, freier Modell-Pack auf geeigneter Hardware;
3. **externer Anbieter:** nur nach Datenvorschau und ausdrücklicher Übergabe.

„KI verwaltet Android“ bedeutet nicht, Sicherheitsgrenzen zu umgehen. Der Launcher plant, erklärt, priorisiert und bereitet Aktionen vor. Android-Rollen, Intents, SAF, `LauncherApps` und Widgets bleiben die ausführenden Vertrauensgrenzen.

## Die wertvollsten Integrationen

| Priorität | Usecase | Nutzwert | Ausführung / Daten | Stand |
|---:|---|---|---|---|
| 1 | Universeller Intent-Eingang | „Öffne Kamera“, „WLAN“, „Wähle …“ statt Navigation | lokaler Planner → explizites Android-Gateway | M2.1 aktiv |
| 2 | App-Suche und Smart Collections | schnelleres Finden ohne Cloudindex | Labels/Paketnamen lokal, fuzzy Ranking | M2.1 aktiv |
| 3 | Kontextuelle Szenen | passender Workspace für Arbeit, Studio, Social, Abend | Uhr, Akku, Netz, Audio lokal | M2.1 aktiv |
| 4 | Adaptives Dock und Smart-Ordner | wichtige Apps ohne manuelles Mikromanagement | Szene, Recency, Pinning, Label/Paketname; lokal und deterministisch | M2.1 aktiv |
| 5 | Sichere Datei-Intelligenz | Metadaten, Kategorie, Textumfang, Namensvorschlag | genau eine gewählte read-only SAF-URI; Textpräfix max. 4.096 Zeichen | M2.1 aktiv |
| 6 | Layout-Assistent | weniger manuelles Sortieren | Vorschau → Anwenden/Verwerfen → Undo | M2.1 aktiv, regelbasiert |
| 7 | Modell-/Provider-Routing | lokal, frei oder Cloud je Aufgabe wählen | Übergabe erst nach Tipp; keine verdeckte API | M2.1 aktiv |
| 8 | App-Shortcut-Orchestrierung | tiefe App-Aktionen ohne UI-Automation | `LauncherApps.getShortcuts/startShortcut`; verspätete Antworten verworfen | M2.1 aktiv |
| 9 | Widget-Zentrale | Systeminformationen und App-Funktionen bündeln | Android `AppWidgetHost` | M2.1 aktiv |
| 10 | Datensparsame Notification Dots | offene Meldungen sichtbar, ohne Nachrichtentext zu kopieren | opt-in Listener; nur Paket und Anzahl flüchtig | M2.1 aktiv |
| 11 | Lokale Zusammenfassung | Notizen/gewählte Dokumente offline verdichten | optionales On-Device-LLM; Daten bleiben lokal | nächster Lauf |
| 12 | Semantische Gerätesuche | Apps, eigene Dateien, Einstellungen, Shortcuts in einem Index | opt-in lokaler Embedding-Index, je Quelle löschbar | nächster Lauf |
| 13 | Benachrichtigungs-Triage | weniger Unterbrechungen und gruppierte Wichtigkeit | separates opt-in für Inhalte; Vorschau, Retention und Löschen | M3 |
| 14 | Kalender-/Aufgabenplanung | realistische Tagespläne statt bloßer Antworten | einzelne opt-in Provider, Vorschau vor Schreibzugriff | M3 |
| 15 | Kommunikationsentwürfe | Antworten formulieren, Ton/Barrierefreiheit anpassen | ausgewählter Text; immer Vorschau vor Share | M3 |
| 16 | Gerätesupport | „Warum ist mein Akku leer?“ mit nachvollziehbaren Diagnosen | erlaubte lokale Telemetrie, keine Root-/Shell-Tricks | M3 |
| 17 | Adaptive Bedienung | größere Ziele, ruhigere Oberfläche, kontextuelle Vereinfachung | lokale Präferenzregeln, Accessibility-Test | M3 |
| 18 | Regelautomation | Szenen nach Zeit, Akku, Audio oder Netz | Capability-Tokens, Dry Run, Audit, Undo | M3 |
| 19 | Persönlicher Memory-Vault | Vorlieben und Routinen ohne Cloudprofil | verschlüsselt, pro Quelle sichtbar/exportierbar/löschbar | M4 |
| 20 | Lokale Sprache | offline STT/TTS und unterbrechbare Dialoge | austauschbare On-Device-Engines | M4 |
| 21 | Theme-/Wallpaper-Designer | PMDD, Stimmung, Tagesphase oder Fokus in Visuals übersetzen | generiertes Asset nur nach Vorschau setzen | M4 |
| 22 | Multi-App-Workflows | dokumentierte Deep Links/Intents sinnvoll verketten | keine Accessibility-Fernsteuerung; Bestätigung je Risikoklasse | M4 |

## Freie und Open-Source-Optionen

| Option | Rolle | Lizenz | M2.1-Verwendung |
|---|---|---|---|
| KoSch Local Core | Befehle, Suche, Kontext, Dateien, Layout, Dock und Ordner | Apache-2.0 | eingebaut und aktiv |
| [PocketPal AI](https://github.com/a-ghorbani/pocketpal-ai) | lokale GGUF-Inferenz über llama.cpp | MIT | installierte App erkennen/öffnen/teilen, sonst Projektseite |
| [ChatterUI](https://github.com/Vali-98/ChatterUI) | lokaler GGUF-Frontend oder optionale Backends | AGPL-3.0 | Label-Erkennung und Projekt-Route |
| [Maid](https://github.com/Mobile-Artificial-Intelligence/maid) | lokale llama.cpp-Inferenz und Modellverwaltung | MIT | Label-Erkennung und Projekt-Route |
| [llama.cpp](https://github.com/ggml-org/llama.cpp/blob/master/docs/android.md) | native GGUF-Engine mit offiziellem Android-Beispiel | MIT | bevorzugte Adapterroute; noch nicht gebündelt |
| [LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM) | produktionsorientierte Edge-LLM-Laufzeit | Apache-2.0 | Geräte-/Beschleunigungs-Evaluation |
| [MLC LLM](https://github.com/mlc-ai/mlc-llm) | kompilierte GPU-Laufzeit auf Android | Apache-2.0 | alternatives High-End-Profil in Evaluation |

Ein Modell selbst hat zusätzlich eine eigene Lizenz. „Engine ist Open Source“ bedeutet nicht automatisch, dass jedes Modell frei weitergegeben oder kommerziell nutzbar ist.

## Auswahl für den nächsten Lauf

Die professionelle Default-Route ist ein **optionaler GGUF-Modell-Pack hinter einem `LocalModelBackend`**, voraussichtlich über llama.cpp:

- breitestes Modellökosystem und offizielles Android-Beispiel;
- Modell wird per SAF gewählt oder bewusst heruntergeladen, nicht in die Launcher-APK gezwungen;
- Hardware-Probe vor dem Laden (RAM, ABI, verfügbare Beschleunigung, Speicher);
- kleine, klar lizenzierte Modelle zuerst;
- Streaming, Cancel, Thermiklimit und Speicherfreigabe als harte Abnahmekriterien;
- Local Core bleibt Fallback, wenn Modell oder Gerät nicht geeignet ist.

LiteRT-LM ist für spätere optimierte Modellprofile attraktiv. MLC LLM bleibt eine zweite GPU-orientierte Backend-Option. Keine dieser Laufzeiten darf die HOME-Shell beim Modellfehler mitreißen; der Inferenzprozess gehört langfristig in eine isolierte Service-/Prozessgrenze.

## Bewusst ausgeschlossene „KI“-Muster

- keine simulierten Berührungen oder heimliche Accessibility-Automation;
- keine automatische Anrufauslösung, Notruf- oder vollständige Dialer-Rolle;
- kein `MANAGE_EXTERNAL_STORAGE` für allgemeines Dateilesen;
- kein unkontrollierter Agent mit Löschen, Kaufen, Senden oder Kontowechsel;
- keine dauerhafte Aufzeichnung von Mikrofon, Bildschirm, Benachrichtigungen oder Standort;
- keine Behauptung, ein heuristischer Planner sei ein generatives LLM;
- keine API-Schlüssel im Quelltext, in Assets, Logs oder Klartext-Preferences.

## Maßgebliche Android-Grenzen

- [HOME-Rolle über `RoleManager`](https://developer.android.com/reference/android/app/role/RoleManager)
- [Start-App-Auswahl über `Settings.ACTION_HOME_SETTINGS`](https://developer.android.com/reference/android/provider/Settings#ACTION_HOME_SETTINGS)
- [Telefon über `Intent.ACTION_DIAL`](https://developer.android.com/reference/android/content/Intent#ACTION_DIAL)
- [Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files)
- [Launcher-Shortcuts über `LauncherApps`](https://developer.android.com/reference/android/content/pm/LauncherApps)
- [Widget-Host](https://developer.android.com/develop/ui/views/appwidgets/host)
- [Android Keystore](https://developer.android.com/privacy-and-security/keystore)
