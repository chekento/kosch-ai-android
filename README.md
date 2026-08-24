# KoSch AI Android

KoSch ist ein nativer, KI-zentrierter Android-Launcher: eine belastbare HOME-Shell und ein programmierbarer Workspace statt eines starren App-Rasters. Der Kern funktioniert **ab dem ersten Start offline, ohne Konto, API-Schlüssel oder Modell-Download**. KI ist eine austauschbare Schicht unter Suche, Kontext, Dateien, Aktionen und Layout – nicht eine einzelne Chat-Seite.

LCARS gehört bewusst nicht zum Kern. Themes werden später deklarative, austauschbare Programme; LCARS kann eines davon sein, ohne Architektur oder Bedienung festzulegen.

## Aktueller Stand: M2.2 Adaptive Pen Space

- professionelle vierstufige Ersteinrichtung mit klarer Datenschutz- und Sicherheitskommunikation;
- echte HOME-Rolle und ein dauerhaft erreichbarer **Sicherheitsausgang** in Androids Start-App-Auswahl;
- App-Katalog und App-Start via `LauncherApps`, ohne `QUERY_ALL_PACKAGES`;
- profilbewusster App-Katalog über alle zugänglichen `LauncherApps.profiles`, mit stabilen User-Schlüsseln, Work-Kennzeichnung und systemgebadgten Icons;
- veröffentlichte App-Shortcuts per langem Druck;
- echtes `AppWidgetHost`-Board mit persistierten IDs, Konfiguration, Entfernen und Cleanup abgebrochener Bindungen;
- fünf Szenen, frei verschiebbare Karten, PLAY/EDIT, Vorschau, Anwenden/Verwerfen und Undo;
- zwei persistente Home-Räume, lokale Smart-Ordner und ein szenenadaptives Dock mit Pinning;
- optionale Notification Dots aus Paketname und Anzahl – ohne Kopie von Titel, Text oder Personen;
- `⌘ Ask` mit lokalem deutschen/englischen Command Planner;
- Telefonzugang über `ACTION_DIAL`: KoSch bereitet vor, die Person bestätigt den Anruf im System-Wähler;
- Dateiauswahl über das Storage Access Framework und begrenzte lokale Metadaten-/Textanalyse;
- genau eine verwaltete read-only Dokumentfreigabe, die ersetzt und wieder vollständig gelöst werden kann;
- Kontrollzentrum für WLAN, Bluetooth, Benachrichtigungen, Android-Einstellungen, Widgets und HOME-Auswahl;
- lokaler Kontext aus Uhrzeit, Akku, Netzwerkstatus und Audioausgabe;
- PocketPal AI, ChatterUI und Maid als freie/Open-Source-Übergabeziele;
- bewusste Übergabe an ChatGPT, Gemini, Claude, Grok, Meta AI, Perplexity oder NotebookLM per App, Android Share oder Web;
- Runtime-Registry für Local Core, llama.cpp, LiteRT-LM und MLC LLM;
- vorbereiteter Android-Keystore-Vault mit AES-GCM und HTTPS-/Loopback-Endpoint-Policy;
- lebender, code-nativer Neural-Glass-Hintergrund – kein fest verdrahtetes Theme;
- Material-You-Dynamic-Color auf Android 12+ und eine adaptive Split-Shell für breite, Landscape-, Tablet- und Foldable-Fenster;
- Reduced-Motion-Fallback, sobald Android Systemanimationen deaktiviert;
- generische Smartpen-Erkennung über Android `InputManager`, einschließlich Druck, Neigung, Orientierung, Hover, Radierer, Stifttasten und Live-Gerätewechsel;
- zusätzlicher **Pen Space** mit druckempfindlichen lokalen Vektorstrichen, Stift, Marker, Radierer, Hover-Cursor, Undo, Leeren und Autosave;
- 32 Einträge umfassende, kategorisierte und lokal durchsuchbare **FAQ & Hilfe**, direkt im Kontrollzentrum und per `⌘ Ask`;
- versioniertes Persistenzschema v3, Widget-Pending-Restore und Race-Schutz für App-Shortcuts;
- Unit-Tests, Android Lint und reproduzierbarer Debug-APK-Build in GitHub Actions.

Im strengen M2.2-Vergleich erreicht KoSch **7,0/10 allgemein** und **6,8/10 im Mittel von 25 simulierten Fachperspektiven**. Das ist Fortschritt, aber noch keine Gesamtführung: Widget-Tiefe, Backup, gemessene Performance, Accessibility-/OEM-Nachweis und Produktionsreife bleiben priorisierte Lücken.

## Was „lokale KI“ in M2.2 exakt bedeutet

`KoSch Local Core` ist sofort aktiv und deterministisch: Befehlsplanung einschließlich Pen Space und FAQ, App-Ranking, regelbasierte Szenen, Kontextbewertung, Dock-/Ordnervorschläge, Layoutvorschläge und eine begrenzte Dateiinspektion. Das ist nützlich und offline, aber **noch kein generatives LLM**.

Ein lokales generatives Modell wird nicht ungefragt mit der APK gebündelt: Modellgrößen, RAM, Thermik, Lizenz und Gerätebeschleunigung unterscheiden sich stark. M2.2 zeigt deshalb geprüfte freie Routen und hält die native Backend-Grenze bereit. M2.3 kann einen optionalen, geräteklassifizierten Modell-Pack über llama.cpp oder LiteRT-LM hinter einer getrennten Service-/Prozessgrenze integrieren.

Direkte Cloud-APIs sind in M2.2 absichtlich nicht aktiv. Die App besitzt kein eigenes `INTERNET`-Recht. Der Vault ist eine ungenutzte Sicherheitsgrenze für einen späteren, optionalen Netzwerk-Modul-Split – keine versteckte Verbindung. Notification Dots benötigen einen separat erteilten Android-Systemzugriff; ohne ihn bleibt der Launcher vollständig funktionsfähig.

## Bauen

Voraussetzungen: JDK 17, Android SDK 36 und Android Studio mit AGP-8.13-Unterstützung.

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Die APK liegt danach unter `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions veröffentlicht bei erfolgreichen Läufen zusätzlich das Artefakt `kosch-ai-launcher-debug`.

## Sicher testen

1. Debug-APK zunächst auf einem Emulator oder Zweitgerät installieren.
2. KoSch öffnen und die Einführung durchlaufen.
3. Optional auf **Android-Start-App auswählen** tippen.
4. Das Kontrollzentrum öffnen und den **Sicherheitsausgang** testen.
5. KoSch lässt sich dort jederzeit durch einen anderen Launcher ersetzen.

Telefon, Dateien, Spracheingabe, Widgets und externe KI-Ziele öffnen jeweils sichtbare Android-System- oder App-Oberflächen. KoSch simuliert keine Berührungen und übernimmt keine Notruf-/Dialer-Rolle.

## Dokumentation

- [Architektur](docs/ARCHITECTURE.md)
- [FAQ – Bedienung, Smartpen, KI, Datenschutz und Recovery](docs/FAQ.md)
- [KI-Usecases und Open-Source-Routen](docs/AI_USE_CASES.md)
- [Sicherheit und Datenschutz](docs/SECURITY.md)
- [Roadmap](docs/ROADMAP.md)
- [Strenges M2.2-Konkurrenzreview](docs/COMPETITOR_REVIEW_M2_2.md), [7 Launcher × 65 Kategorien](docs/launcher_comparison_m2_2.csv), [25×65-Fachmatrix](docs/expert_scores_m2_2.csv) und [formatierte Benchmark-Arbeitsmappe](docs/launcher_benchmark_m2_2.xlsx)
- [Historisches M2.1-Review](docs/COMPETITOR_REVIEW_M2_1.md)
- [Historisches M2-Review](docs/EXPERT_REVIEW_M2.md)

## Technischer Rahmen

- Package: `cloud.kosch.aiandroid`
- Version: `0.2.2-alpha01`
- minSdk 29, targetSdk/compileSdk 36
- Kotlin 2.3, Jetpack Compose, Material 3
- Gradle 8.13, Android Gradle Plugin 8.13
- Lizenz: Apache-2.0
