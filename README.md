# KAL – KoSch AI Launcher

KAL ist ein nativer, local-first Android-Launcher für professionelle Nutzer: eine belastbare HOME-Shell, ein programmierbarer Workspace und eine sichere KI-Orchestrierung statt eines starren App-Rasters. Der Kern funktioniert **beim ersten Start offline – ohne Konto, API-Schlüssel oder Modell-Download**. KI liegt unter Suche, Kontext, Dateien, Aktionen und Layout; App-Start, Telefon, Dateien, Widgets, Einstellungen und der Sicherheitsausgang bleiben auch ohne Modell verfügbar.

LCARS ist bewusst kein Kernbestandteil. Themes sollen später als austauschbare Programme entstehen; LCARS kann dann eines davon sein.

## Aktueller Stand: M2.5 Professional Parity & Correctness

M2.5 vertieft den professionellen Alpha-Kern an den Stellen, an denen klassische Launcher-Parität und korrekte Android-Semantik im Alltag zählen:

- echte HOME-Rolle sowie dauerhaft erreichbare Android-Start-App-Auswahl als Sicherheitsausgang;
- profilbewusster App-Katalog mit `LauncherApps`, stabilen User-Seriennummern, Work-Badges und Migration alter App-Schlüssel;
- lokaler App-Raum mit Suche, Smart-/A–Z-/Häufig-/Zuletzt-Sortierung, erklärbarem lokalen Nutzungsranking, Reset und eigener Ansicht für verborgene Apps;
- sicherer App-Aktionsraum für Start, Shortcuts, App-Info, Store, Dock, Ordner, Sichtbarkeit und Androids Deinstallationsdialog;
- profilbezogene App-Info und Deinstallationsanfrage für genau das gewählte persönliche oder Arbeitsprofil;
- Arbeitsprofil-Pause/-Aktivierung über Androids geschützten Quiet Mode mit sichtbarem Status und kontrolliert abgelehnten Starts pausierter Work-Apps;
- Pro Desk als professioneller Standardbereich mit HOME-, Work-, Audit- und Local-Core-Lage;
- zwei Home-Räume, fünf Szenen, frei verschiebbare Karten, Preview/Apply/Discard/Undo, Smart Dock und lokale Smart-Ordner;
- eigene persistente Ordner mit Erstellen, Umbenennen, Add/Remove, App-Reihenfolge und klaren Grenzen sowie steuerbare Dock-Pin-Reihenfolge;
- echtes `AppWidgetHost`-Board mit Provider-Konfiguration, Größenpresets, Reihenfolge, Undo, Persistenz und Cleanup;
- Telefon über `ACTION_DIAL` sowie einmalige Kontaktauswahl ohne `READ_CONTACTS`;
- Nachrichten, Kalender, Wecker und Kamera über dokumentierte Android-Verträge ohne überbreite Rechte;
- einzelne read-only Dateiinspektion und ein zusätzlicher, **vom Nutzer gewählter SAF-Datei-Arbeitsraum**;
- Navigation, Suche, Sortierung, lokale Metadatenanalyse, Duplikatnamenshinweise, größte Dateien, Ordnererstellung, Rename mit Undo und separat bestätigtes Löschen – ausschließlich innerhalb des gewählten Dokumentbaums;
- getrennte Datei-Mutations-, Audit- und Refresh-Semantik: bestätigte Änderungen bleiben erfolgreich, nach Flächenschluss auditiert und im aktuellen Verzeichnis verankert;
- Kontrollzentrum für HOME, WLAN, Bluetooth, Meldungen, Hintergrund, Anzeige, Ton, Akku, Datenschutz, Accessibility, Standard-Apps, Speicher und Widgets;
- Hardware-Tastatursteuerung, Android-Shortcut-Hilfe und Escape-Recovery;
- generische Smartpen-Erkennung mit Druck, Neigung, Orientierung, Hover, Radierer, Tasten und Live-Gerätewechsel;
- Pen Space mit lokalen Vektorstrichen, Stift/Marker/Radierer, Undo/Clear/Autosave, SVG-Export und endpoint-erhaltendem Resampling sehr langer Striche;
- Android-14+-Systemnotiz mit Stylus-Modus bei erkanntem Smartpen und Pen Space als ehrlichem Fallback;
- Activity-unabhängiger `LauncherViewModel`; begrenzte Exportdaten werden hinter einem Saved-State-Einmaltoken in privaten No-Backup-Dateien wiederaufnehmbar gehalten;
- portables Workspace-Backup mit PBKDF2-HMAC-SHA-256, AES-256-GCM, Restore-Dry-Run, strikter Validierung und zweiter Bestätigung;
- metadatenarmes Audit ohne Freitext, maximal 250 Ereignisse/90 Tage, CSV-Export und vollständige Löschung;
- Notification Dots als separates Android-Opt-in, ohne Speicherung von Titel, Text, Personen oder Aktionen;
- Dynamic Color, adaptive Split-Shell, edge-to-edge, Reduced Motion und explizite Accessibility-Semantik einschließlich Pen-Custom-Actions;
- 57 lokale, kategorisierte FAQ-Einträge;
- PocketPal AI, ChatterUI und Maid als freie/Open-Source-Übergabeziele sowie eine vorbereitete Runtime-Grenze für llama.cpp, LiteRT-LM und MLC LLM;
- Unit-Tests, Android Lint, Debug-/minifizierter Release-Build, Quell- und APK-Berechtigungsbudget, Baseline-Profil und APK-Prüfsumme in modernisierten GitHub Actions.

## Ehrliche Qualitätslage

Der reproduzierbare M2.5-Vergleich bewertet KAL, Pixel/Android 17 als Systemreferenz sowie Nova, Niagara, Smart Launcher, Microsoft Launcher und Lawnchair in **100 Kategorien von 0,1 bis 10,0**. Zusätzlich werden **25 simulierte Fachperspektiven** berechnet; sie sind keine tatsächlich befragten Personen.

- KAL M2.5: **8,2/10 allgemein**
- Mittel der 25 Fachperspektiven: **8,1/10**
- Rang in dieser breiten Matrix: **2**
- Zielwert über 9,5: **nicht erreicht**

Der Fortschritt von M2.4 (8,1/7,9) ist durch konkrete Quellcode-, Test- und CI-Deltas begründet. Für 9,5 fehlen weiterhin reale OEM-/Foldable-/Stylus-Labs, TalkBack/Switch-Access/200-%-Abnahme, gespeicherte Macrobenchmarks, vollständige Widget-/Launcher-Parität, Release-Signing/SBOM, Lokalisierung und unabhängige Security-Prüfung. Zahlen werden nicht auf das Ziel hochgesetzt.

## Was „lokale KI“ in M2.5 bedeutet

`KAL Local Core` ist sofort aktiv und deterministisch. Er plant deutsche und englische Befehle, rankt Apps mit transparenten lokalen Startsignalen, bewertet Kontext, schlägt Szenen/Dock/Ordner/Layout vor und analysiert ausgewählte Dateimetadaten. Er hat keine autonomen Android-Rechte: schreibende oder destruktive Aktionen laufen über feste Capabilities, Vorschau, Bestätigung und – wo technisch ehrlich möglich – Undo.

Ein generatives LLM ist noch nicht ungefragt in der APK gebündelt. Modellgröße, RAM, Thermik, Beschleunigung und Lizenz unterscheiden sich zu stark. Ein späterer optionaler Modell-Pack muss in einer getrennten Service-/Prozessgrenze laufen; der Local Core bleibt Fallback.

Direkte Cloud-APIs sind deaktiviert. Die App besitzt kein eigenes `INTERNET`-Recht. Der vorbereitete Keystore-Vault und die HTTPS-/Loopback-Policy sind Sicherheitsgrenzen für einen späteren, separaten Netzwerk-Flavour – keine versteckte Verbindung.

## Bauen

Voraussetzungen: JDK 17, Android SDK 36 und Android Studio/AGP 8.13.

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
```

Die lokale Debug-APK liegt unter `app/build/outputs/apk/debug/app-debug.apk`. Ein grüner GitHub-Actions-Lauf veröffentlicht das Artefakt `kal-ai-launcher-m2.5-debug` mit:

- `KAL-AI-Launcher-M2.5-debug.apk`
- `KAL-AI-Launcher-M2.5-debug.apk.sha256`

## Sicher testen

1. APK zuerst auf Emulator oder Zweitgerät installieren.
2. Einführung durchlaufen und KAL öffnen.
3. Optional **Android-Start-App auswählen** verwenden.
4. **Kontrollzentrum → Sicherheitsausgang** testen.
5. KAL dort jederzeit durch einen anderen Launcher ersetzen.

Telefon, Dateien, Kontakte, Sprache, Widgets, Deinstallation und Systemeinstellungen öffnen sichtbare Android-System- oder App-Oberflächen. KAL simuliert keine Berührungen und übernimmt weder Notruf- noch vollständige Dialer-/Dateisystemrechte.

## Dokumentation

- [Architektur](docs/ARCHITECTURE.md)
- [FAQ – Bedienung, Dateien, Smartpen, KI, Datenschutz und Recovery](docs/FAQ.md)
- [KI-Usecases und Open-Source-Routen](docs/AI_USE_CASES.md)
- [Sicherheit und Datenschutz](docs/SECURITY.md)
- [Qualitätsgates](docs/QUALITY_GATES.md)
- [Roadmap](docs/ROADMAP.md)
- [M2.5 Release Notes](docs/RELEASE_NOTES_M2_5.md)
- [Strenges M2.5-Konkurrenzreview](docs/COMPETITOR_REVIEW_M2_5.md)
- [7 Launcher × 100 Kategorien](docs/launcher_comparison_m2_5.csv)
- [25 × 100 KAL-Fachmatrix](docs/expert_scores_m2_5.csv)
- [25 Rollen × 7 Launcher](docs/expert_launcher_overall_m2_5.csv)
- [Formatierte M2.5-Benchmark-Arbeitsmappe](docs/launcher_benchmark_m2_5.xlsx)
- Historie: [M2.4](docs/COMPETITOR_REVIEW_M2_4.md), [M2.3](docs/COMPETITOR_REVIEW_M2_3.md), [M2.2](docs/COMPETITOR_REVIEW_M2_2.md), [M2.1](docs/COMPETITOR_REVIEW_M2_1.md), [M2](docs/EXPERT_REVIEW_M2.md)

## Technischer Rahmen

- Package: `cloud.kosch.aiandroid`
- Version: `0.2.5-alpha01` (Version Code 7)
- minSdk 29, targetSdk/compileSdk 36
- Kotlin 2.3, Jetpack Compose, Material 3
- Gradle 8.13, Android Gradle Plugin 8.13
- Lizenz: Apache-2.0
