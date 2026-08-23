# KoSch AI Android

KoSch AI Android ist ein nativer, KI-zentrierter Android-Launcher: ein programmierbarer Workspace statt eines starren App-Rasters. Er registriert sich als echte HOME-App, indexiert startbare Apps über Androids Launcher-API und stellt einen universellen Eingang für lokale Befehle sowie bewusst gewählte KI-Übergaben bereit.

LCARS ist **kein Bestandteil des Kerns**. Darstellung und Verhalten werden später als austauschbare Themes/Programme behandelt – LCARS kann dann eines davon sein, ohne die Architektur zu bestimmen.

## Aktueller Stand: M1 Vertical Slice

Dieser erste Meilenstein ist absichtlich schmal, aber echt:

- echte HOME-Rolle mit Android-Systemdialog;
- App-Katalog und App-Start via `LauncherApps`, ohne `QUERY_ALL_PACKAGES`;
- Szenen: AI, Work, Studio, Social und Evening;
- PLAY-/EDIT-Modus mit frei verschiebbaren, persistenten Workspace-Karten;
- lokaler Layout-Vorschlag mit **Vorschau → Anwenden/Verwerfen → Rückgängig**;
- `⌘ Ask` mit lokaler deutscher/englischer Befehlsplanung;
- explizite Auswahl zwischen ChatGPT, Gemini, Claude, Grok, Meta AI, Perplexity und NotebookLM;
- Übergaben nur über dokumentierte Android-Wege: App-Start, Share-Intent oder Web-Fallback;
- lokaler Kontext aus Uhrzeit, Akku, Netzwerkstatus und Audioausgabe;
- animierter Begleiter als Sprach-Einstieg – ohne so zu tun, als wäre schon ein Agent aktiv;
- lokale Unit-Tests, Lint und reproduzierbarer Debug-APK-Build in GitHub Actions.

Reguläre Android-Widgets und direkte Modell-APIs sind in diesem Commit noch nicht als fertige Funktionen sichtbar. Der `AppWidgetHost`-Lebenszyklus und die Provider-Grenzen sind vorbereitet; Bindung, persistente Host-IDs, Credential Vault und Agenten-Aktionen folgen in separaten Meilensteinen.

## Projekt bauen

Voraussetzungen:

- JDK 17
- Android SDK 36
- Android Studio mit Android-Gradle-Plugin-8.13-Unterstützung oder die Gradle-Kommandozeile

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Die APK liegt danach unter `app/build/outputs/apk/debug/app-debug.apk`. Bei jedem Push auf `main` erstellt GitHub Actions zusätzlich das Artefakt `kosch-ai-launcher-debug`.

## Auf einem Testgerät verwenden

1. Debug-APK installieren.
2. KoSch AI Launcher öffnen.
3. Auf **Festlegen** tippen und KoSch als Start-App auswählen.
4. Für Experimente zunächst ein Zweitgerät oder Emulatorprofil verwenden.

KoSch lässt sich jederzeit in den Android-Einstellungen unter Standard-Apps → Start-App wieder ersetzen.

## Sicherheitsprinzipien

- local-first und minimale Berechtigungen;
- keine Accessibility-Automation im Basissystem;
- keine verdeckte Kontrolle fremder Apps;
- keine ungefragte Cloud-Übertragung;
- Vorschau und Bestätigung vor KI-basierten Änderungen;
- zukünftige API-Schlüssel ausschließlich verschlüsselt über Android Keystore;
- Themes erhalten keine impliziten Systemrechte.

Mehr Details stehen in [Architektur](docs/ARCHITECTURE.md), [Sicherheit & Datenschutz](docs/SECURITY.md) und [Roadmap](docs/ROADMAP.md).

## Technischer Rahmen

- Package: `cloud.kosch.aiandroid`
- minSdk 29, targetSdk/compileSdk 36
- Kotlin 2.3, Jetpack Compose, Material 3
- Gradle 8.13, Android Gradle Plugin 8.13

## Lizenz

Apache License 2.0 – siehe [LICENSE](LICENSE).

