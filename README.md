# KAL – KoSch AI Launcher

KAL ist ein nativer, local-first Android-Launcher für professionelle Nutzer: eine belastbare HOME-Shell, ein frei konfigurierbarer Workspace und eine sichere KI-Orchestrierung statt eines starren App-Rasters. Der Kern funktioniert **beim ersten Start ohne Konto, API-Schlüssel oder Modell-Download**. App-Start, Suche, Telefon, Dateien, Widgets, Einstellungen, Home-Bearbeitung und der Sicherheitsausgang bleiben auch ohne externes Modell verfügbar.

Local-first bedeutet dabei bewusst **nicht** „niemals Netzwerk“: optionale direkte AI-Provider-Verbindungen sind vorhanden, aber standardmäßig AUS und durch separate Produkt- und Datenschutz-Gates geschützt. Wenn eine Person einen Provider verbindet und Cloud Access aktiviert, kann KAL nach einer bestätigten Vordergrundaktion ausgewählten Prompt-/Kontextinhalt direkt an diesen Provider senden. Es gibt keine Hintergrund-LLM-Anfragen.

LCARS ist bewusst kein Kernbestandteil. Themes sollen als austauschbare Programme entstehen; LCARS kann später eines davon sein.

## Aktueller Integrationsstand: 0.2.5-alpha01

Der aktuelle Integrationsbranch baut auf M2.5 Professional Parity & Correctness auf und erweitert ihn insbesondere um Home Studio, umfangreiche Settings, Universal Search 2.0, Widgets/Stacks und Smart AI Everywhere:

- echte HOME-Rolle und dauerhaft erreichbare Android-Start-App-Auswahl als Sicherheitsausgang;
- profilbewusster App-Katalog mit `LauncherApps`, stabilen User-Seriennummern, Work-Badges und kontrollierten Starts pausierter Arbeitsprofile;
- Home Studio mit frei verwaltbaren Seiten, Drag/Drop, Resize, deterministischem Reflow/Compact, Objektstilen sowie Preview/Apply/Discard/Undo;
- umfangreiches Settings Center mit globalen Defaults und Page-/Object-Overrides samt explizitem `inherit default`;
- persistente Launcher-Settings und portable, validierte Settings-/Workspace-Backups ohne Secrets oder gerätegebundene Grants;
- echtes `AppWidgetHost`-System, Workspace-v7-Bindings, Größenupdates, Recovery/Remap und Widget-Stacks;
- Universal Search 2.0 mit lokalem Index, App-/Shortcut-/Settings-/Action-Quellen und policy-gesteuerter Ausführung;
- Smart Dock, lokale Nutzungs-/Kontextsignale und erklärbare Vorschläge ohne heimliche disruptive Umordnung;
- AI Hub / Smart AI Everywhere mit Local Core als erstem Pfad und explizitem Routing zu installierten AI-Apps, veröffentlichten Shortcuts/Widgets oder optionalen direkten Providern;
- Context Handoff mit sichtbarer Auswahl/Bestätigung statt verdeckter Datenweitergabe;
- Pen-Lasso/Ask und Smartpen-Unterstützung mit lokalem Ink-State und bewusster Kontextfreigabe;
- optionaler Assistant-Agent-Core mit klar getrennten Fähigkeiten; Screen Awareness und Camera Awareness bleiben standardmäßig AUS und erfordern ihre eigenen sichtbaren Android-/Produkt-Opt-ins;
- Notification Dots als separates Android-Special-Access-Opt-in, ohne Speicherung von Notification-Titel, Text, Personen oder Aktionen;
- Telefon über `ACTION_DIAL`, Nachrichten über `ACTION_SENDTO`, Dateien über SAF und sichtbare Android-System-/App-Flows statt überbreiter Rechte;
- lokales, metadatenarmes Audit ohne Freitext, begrenzt und vom portablen Backup getrennt;
- Dynamic Color, adaptive Fenster-/Eingabelogik, Fold-/Pen-Erkennung, Reduced Motion und Accessibility-Semantik;
- automatisierte Unit-/Instrumentation-Tests, Android Lint, Debug-/minifizierter Release-Build, Quell- und APK-Permission-Budget, Baseline-Profil und APK-Prüfsumme in GitHub Actions.

## KI-Ausführung und Provider-Grenzen

KAL behandelt KI als Schicht unter dem Launcher, nicht als einzelne Chat-Seite. Die bevorzugte Reihenfolge ist:

1. **Local Core** – deterministische Regeln, Suche, Ranking, Klassifikation, Theme-/Layout-Planung und Redaction ohne Cloud-Zwang.
2. **On-device / lokales Modell** – wo Gerät und Android-Plattform dies sicher unterstützen; ein großes Modell ist kein Zwangsbestandteil der Basis-APK.
3. **Installierte AI-App** – bewusster Android-Handoff über veröffentlichte App-/Share-/Shortcut-/Widget-Schnittstellen.
4. **Direkt verbundener Provider** – nur nach expliziter Verbindung, aktiviertem Cloud Access und einer bestätigten Vordergrundaktion.

Die Android-Berechtigung `INTERNET` ist deshalb im aktuellen Paket vorhanden. Sie ist **keine** Autorisierung für automatische Cloud-Nutzung. `KalCloudAccessPolicy` verlangt die Produkt-Gates; Background Provider Requests werden abgelehnt.

OpenRouter ist der erste direkt implementierte OAuth-Pfad und verwendet PKCE mit einem einmaligen Loopback-Callback auf `127.0.0.1`; kein wiederverwendbares OAuth-Client-Secret wird in die APK eingebettet. Provider-Keys/Tokens liegen verschlüsselt im Android-Keystore-gestützten Credential Vault und sind aus portablem Backup ausgeschlossen. Weitere Providerprofile können BYOK oder organisationsspezifische Konfiguration voraussetzen.

Für externe AI-Verarbeitung gilt: Ziel, Transportweg und Datenfreigabe dürfen nicht miteinander verwechselt werden. Ein Android-App-Handoff ist etwas anderes als eine direkte KAL-Provider-Anfrage. Die aktuelle Data-Safety-Wahrheit ist in [`docs/PLAY_DATA_SAFETY_MATRIX.md`](docs/PLAY_DATA_SAFETY_MATRIX.md) dokumentiert.

## Screen, Camera und sensible Beobachtung

- Screen Awareness: standardmäßig AUS; Android MediaProjection-Consent und sichtbarer Foreground-Service bleiben Autorität.
- Camera Awareness: standardmäßig AUS; sichtbare CameraX-Session und explizite Freigabe erforderlich.
- `RECORD_AUDIO`, Standort-, Kontakte-, SMS-, Call-Log-, Phone-State- und `QUERY_ALL_PACKAGES`-Rechte gehören nicht zum Observation-Permission-Budget.
- Ein verbundener Provider erhält nicht automatisch Screen-/Camera-Inhalt. Capture-Opt-in und Provider-Kontextfreigabe bleiben getrennte Grenzen.
- Der N1-VPN-Prototyp ist debug-only und verarbeitet im aktuellen Stand keinen Traffic; `VpnService` und die N1-Security-Fläche dürfen nicht in die Release-APK gelangen.

## Ehrliche Qualitätslage

Die letzte vollständig dokumentierte breite M2.5-Bewertung lag bei **8,2/10 allgemein** und **8,1/10 im Mittel von 25 simulierten Fachperspektiven**. Diese Werte werden durch den großen Integrationsbranch **nicht automatisch hochgesetzt**. Ein Zielwert über 9,5 darf erst nach realen OEM-/Foldable-/Stylus-, Accessibility-, Performance-, Upgrade-/Restore- und unabhängigen Security-Tests vergeben werden.

Automatisierte CI ist ein Release-Gate, ersetzt aber keine reale Geräteabnahme. Die verbindlichen Gates stehen in [`docs/QUALITY_GATES.md`](docs/QUALITY_GATES.md).

## Bauen

Voraussetzungen: JDK 17, Android SDK 36 und Android Studio/AGP 8.13.

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease assembleDebugAndroidTest
```

Die lokale Debug-APK liegt unter `app/build/outputs/apk/debug/app-debug.apk`. Ein grüner GitHub-Actions-Lauf erzeugt ein installierbares Debug-Artefakt samt SHA-256 sowie ein Instrumentation-Paket und führt zusätzlich die API-36-Managed-Device-Tests aus.

## Sicher testen

1. APK zuerst auf Emulator oder Testgerät installieren.
2. Einführung durchlaufen und KAL öffnen.
3. Optional **Android-Start-App auswählen** verwenden.
4. **Kontrollzentrum → Sicherheitsausgang** testen.
5. KAL dort jederzeit durch einen anderen Launcher ersetzen.
6. Danach Home Studio, Seitenwechsel, Widgets/Stacks, Search, Settings und Backup/Restore prüfen.
7. Screen/Camera Awareness nur bewusst aktivieren und die sichtbaren Consent-/Session-Zustände prüfen.
8. Provider Connections zunächst getrennt testen: frische Installation → Cloud Access AUS → Verbindung → bestätigte Vordergrundanfrage → Disconnect.

Telefon, Dateien, Kontakte, Sprache, Widgets, Deinstallation und Systemeinstellungen öffnen sichtbare Android-System- oder App-Oberflächen. KAL simuliert keine Berührungen und übernimmt weder Notruf- noch vollständige Dialer-/Dateisystemrechte.

## Dokumentation

- [Architektur](docs/ARCHITECTURE.md)
- [AI Everywhere Architecture](docs/AI_EVERYWHERE_ARCHITECTURE.md)
- [Settings Center Architecture](docs/SETTINGS_CENTER_ARCHITECTURE.md)
- [Play Data Safety Truth Matrix](docs/PLAY_DATA_SAFETY_MATRIX.md)
- [FAQ – Bedienung, Dateien, Smartpen, KI, Datenschutz und Recovery](docs/FAQ.md)
- [KI-Usecases und Open-Source-Routen](docs/AI_USE_CASES.md)
- [Sicherheit und Datenschutz](docs/SECURITY.md)
- [Qualitätsgates](docs/QUALITY_GATES.md)
- [Roadmap](docs/ROADMAP.md)
- [Product Track](docs/LAUNCHER_PRODUCT_TRACK.md)
- [Aktuelle Benchmark-Baseline 2026-08-28](docs/LAUNCHER_BENCHMARK_BASELINE_2026-08-28.md)
- [M2.5 Release Notes](docs/RELEASE_NOTES_M2_5.md)
- [Strenges M2.5-Konkurrenzreview](docs/COMPETITOR_REVIEW_M2_5.md)

## Technischer Rahmen

- Package: `cloud.kosch.aiandroid`
- Version: `0.2.5-alpha01` (Version Code 7)
- minSdk 29, targetSdk/compileSdk 36
- Kotlin 2.3, Jetpack Compose, Material 3
- Gradle 8.13, Android Gradle Plugin 8.13
- Lizenz: Apache-2.0
