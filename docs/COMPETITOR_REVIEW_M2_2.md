# M2.2: strenger Launcher- und Expertenvergleich

Stand: 24. August 2026

## Urteil

**KoSch M2.2 erreicht allgemein 7,0 / 10,0 und im Mittel der 25 simulierten Fachperspektiven 6,8 / 10,0.** Gegenüber M2.1 sind das +0,4 beziehungsweise +0,3 Punkte. KoSch liegt im gerundeten Allgemeinwert nun in derselben 7,0-Gruppe wie Niagara und Lawnchair, bleibt im 25-Rollen-Mittel aber Letzter. Pixel/Android 17 führt mit 8,1; Microsoft Launcher folgt mit 7,2. Nova und Smart Launcher erreichen jeweils 7,1.

M2.2 ist sichtbar moderner und eigenständiger: adaptive Breitbild-Shell, Material-You-Farben, profilbewusster App-Katalog, live erkannter Smartpen, lokaler Pen Space und eine durchsuchbare Offline-FAQ. Das reicht noch nicht für die Behauptung „bester Launcher“. Widget-Tiefe, Kontakte, Backup, gemessene Laufzeit, Accessibility-Nachweis, OEM-Abdeckung, Lokalisierung, Audit und Produktionsreife sind weiterhin klare Rückstände.

## Methodik und Einschränkungen

- Vergleich: KoSch M2.2, Pixel/Android 17 als aktuelle Systemreferenz sowie Nova 8/8.1 Beta, Niagara, Smart Launcher 6.6, Microsoft Launcher und Lawnchair 15 Beta 3.
- Skala: 0,1 = praktisch nicht vorhanden; 10,0 = nachweislich erstklassig und produktionsbewährt.
- 65 Kategorien, sieben Kandidaten und 25 Fachrollen. Die Rohdaten stehen in [launcher_comparison_m2_2.csv](launcher_comparison_m2_2.csv), [expert_scores_m2_2.csv](expert_scores_m2_2.csv), [expert_launcher_overall_m2_2.csv](expert_launcher_overall_m2_2.csv) und der formatierten [launcher_benchmark_m2_2.xlsx](launcher_benchmark_m2_2.xlsx).
- Die 25 Rollen sind **simulierte, reproduzierbare Fachperspektiven**, keine tatsächlich befragten Menschen. Jede Rolle besitzt 65 KoSch-Einzelwerte; neue Kategorien erhalten eine dokumentierte Rollenstrenge von −0,3 bis +0,2.
- Die M2.1-Rollenwerte wurden um das nachgewiesene Delta der jeweiligen KoSch-Kategorie fortgeschrieben. Konkurrenz-Rollenmittel kombinieren das frühere 60-Kategorien-Mittel mit den fünf neuen, rollenjustierten Kategorien. Das Build-Skript liegt unter `tools/benchmark/build_m2_2_benchmark.mjs`.
- KoSch wurde anhand des Quellstands und des grünen Android-CI-Laufs bewertet. Es gab keinen identischen Sieben-Launcher-Test auf derselben Gerätefarm; unbelegte Eigenschaften werden konservativ bewertet.
- Pixel/Android 17 umfasst Pixel Launcher und eng verbundene Android-HOME-/Systemflächen. Das ist absichtlich eine anspruchsvolle Systemreferenz und kein reiner APK-gegen-APK-Vergleich.
- Fehlende Funktionen werden auch dann niedrig bewertet, wenn ein Konkurrenzprodukt sie bewusst nicht in den Launcher integriert. Das entspricht dem Produktziel, die Stärken aller Kandidaten in einer sicheren KI-System-Shell zusammenzuführen.

## Gesamtrangliste

| Rang | Launcher | Allgemein | 25 Rollen | Strenges Urteil |
|---:|---|---:|---:|---|
| 1 | Pixel / Android 17 | 8,1 | 8,1 | Referenz für Systemintegration, Performance, Accessibility und Reife |
| 2 | Microsoft Launcher | 7,2 | 7,2 | stärkste Produktivitäts-, Feed-, Kontakte- und Work-Profile-Breite |
| 3= | Nova 8 / 8.1 Beta | 7,1 | 7,1 | stärkste klassische Anpassbarkeit, Backup und Launcher-Tiefe |
| 3= | Smart Launcher 6.6 | 7,1 | 7,1 | starke automatische Organisation und breite Workspace-Funktionen |
| 5= | Niagara 1.x | 7,0 | 7,1 | beste Klarheit, Einhandbedienung und Self-Service-Reife |
| 5= | Lawnchair 15 Beta 3 | 7,0 | 7,1 | stärkste offene Launcher3-/Pixel-Basis, aber Beta-Risiko |
| 5= | **KoSch M2.2** | **7,0** | **6,8** | stärkste sichere Local-first-Systemerweiterung, aber Alpha-Reifelücken |

Die Ränge mit Gleichheitszeichen beruhen auf den auf eine Dezimalstelle veröffentlichten Allgemeinmitteln. Das schwächere Expertenmittel verhindert eine seriöse KoSch-Führungsbehauptung.

## Bereichsergebnis

| Bereich | KoSch | Pixel | Nova | Niagara | Smart | Microsoft | Lawnchair |
|---|---:|---:|---:|---:|---:|---:|---:|
| UX | 7,3 | 9,0 | 8,2 | 8,7 | 8,5 | 8,1 | 8,1 |
| Launcher | 6,9 | 8,3 | 8,6 | 7,9 | 8,5 | 8,0 | 7,9 |
| System | 6,8 | 7,6 | 3,2 | 2,9 | 3,0 | 5,2 | 2,9 |
| AI | 6,9 | 6,6 | 5,9 | 6,0 | 6,1 | 5,9 | 6,8 |
| Security | 8,0 | 8,2 | 7,1 | 7,3 | 7,2 | 7,1 | 8,1 |
| Engineering | 5,9 | 8,3 | 7,6 | 7,5 | 7,4 | 7,4 | 7,5 |
| Product | 7,3 | 8,7 | 8,2 | 9,0 | 8,7 | 8,5 | 7,3 |

KoSch führt gegenüber klassischen Dritt-Launchern klar im Systembereich und knapp im AI-Bereich. Der Engineering-Abstand von 2,4 Punkten zur Pixel-Referenz ist das wichtigste Warnsignal: Architekturideen und CI ersetzen keine gemessene Produktreife.

## M2.1 → M2.2: nachgewiesener Fortschritt

| Kategorie | M2.1 | M2.2 | Delta | Begründung |
|---|---:|---:|---:|---|
| Tablet/Foldable/Landscape | 4,9 | 7,2 | +2,3 | breitenabhängige Split-Shell mit kompaktem Fallback |
| Wallpaper-/Theme-System | 4,3 | 5,8 | +1,5 | Material-You-Dynamic-Color, Theme-Programm fehlt weiter |
| Kleine Displays | 5,8 | 6,9 | +1,1 | Constraint-basierte Shell; 320-dp-/200-%-Test fehlt |
| Visuelle Kohärenz | 7,8 | 8,2 | +0,4 | dynamische, systemnahe Farbpalette und konsistente Pen-Surface |
| App-Entdeckung | 7,6 | 7,8 | +0,2 | profilbewusster Katalog und Badging |
| OEM-Kompatibilität | 4,4 | 5,0 | +0,6 | generische Android-Stylus-Pipeline statt Vendor-SDK; Labor fehlt |
| Wartbarkeit | 7,3 | 7,6 | +0,3 | getrennte Stylus-, Ink-, FAQ- und Profilmodelle plus Tests |
| Produktionsreife | 4,7 | 4,9 | +0,2 | CI grün; weiterhin keine Release-/Geräteabnahme |

Die fünf neuen Kategorien C61–C65 erhöhen nicht automatisch den Score. Sie erweitern die Messlatte um Smartpen, Pen Workspace, Handschrift und Self-Service, wodurch auch etablierte Launcher für fehlende Integration Punkte verlieren.

## Smartpen- und FAQ-Vergleich

| Kategorie | KoSch | Pixel | Nova | Niagara | Smart | Microsoft | Lawnchair |
|---|---:|---:|---:|---:|---:|---:|---:|
| C61 Smartpen-Erkennung/Gerätewechsel | 8,6 | 8,8 | 2,0 | 2,5 | 2,5 | 2,5 | 3,5 |
| C62 Druck/Neigung/Hover/Radierer | 8,3 | 9,2 | 1,5 | 1,5 | 1,5 | 1,5 | 2,5 |
| C63 Integrierter Pen-Workspace | **8,5** | 5,0 | 1,5 | 1,0 | 1,2 | 1,5 | 2,0 |
| C64 Systemhandschrift/IME | 7,2 | 9,0 | 6,8 | 6,5 | 6,5 | 6,8 | 6,8 |
| C65 In-App FAQ/Self-Service | 8,6 | 8,5 | 6,5 | **9,0** | 8,5 | 8,2 | 7,5 |

KoSch gewinnt C63, weil Pen Space tatsächlich Teil der HOME-Shell ist. Pixel bleibt bei der Hardware-/Systemintegration vorne. Niagara erhält den Self-Service-Bestwert wegen seiner ungewöhnlich ausführlichen, gepflegten Hilfestruktur. KoSch-Handschrift bleibt korrekt als Android-14+-IME-Fähigkeit ausgewiesen und wird nicht als eigene Erkennung verkauft.

## Funktionsvergleich des aktuellen Stands

Legende: **Ja** = vorhanden; **Teil** = vorhanden, aber begrenzt, systemabhängig oder anders gelöst; **Nein** = als Launcher-Funktion nicht belegt.

| Funktion | KoSch | Pixel | Nova | Niagara | Smart | Microsoft | Lawnchair |
|---|---|---|---|---|---|---|---|
| echte HOME-App | Ja | Ja | Ja | Ja | Ja | Ja | Ja |
| sofort ohne Konto/API | Ja | Ja/Basis | Ja/Basis | Ja/Basis | Ja/Basis | Teil | Ja |
| lokaler deterministischer KI-Kern | Ja | Teil | Teil | Teil | Teil | Teil | Nein |
| integriertes generatives Launcher-LLM | Nein | Nein | Nein | Nein | Nein | Nein | Nein |
| App-Katalog/Suche | Ja | Ja | Ja | Ja | Ja | Ja | Ja |
| App-Shortcuts | Ja | Ja | Ja | Ja | Ja | Teil | Ja |
| profilbewusste App-Schlüssel/Badges | Ja/Basis | Ja | Teil | Teil | Teil | Ja | Teil |
| Private-Space-Container | Nein/bewusst | Ja | Teil | Teil | Teil | Nein/Work | Teil |
| persistentes Pinning/Smart Dock | Ja | Ja/Vorschläge | Ja | Ja/Favoriten | Ja | Ja | Ja |
| mehrere Home-Räume | Teil/2+Pen | Ja | Ja | Nein/Design | Ja | Ja | Ja |
| lokale Smart-Ordner mit Vorschau | Ja | Nein | Teil/Beta | Nein | Ja | Teil | Teil |
| manuelle Ordner/Drag-and-drop | Teil | Ja | Ja | Ja/Pop-ups | Ja | Ja | Ja |
| Widget Hosting | Ja/Board | Ja | Ja | Ja | Ja | Ja | Ja |
| Widget Resize/Stack/Restore | Nein | Ja | Ja | Ja | Ja | Ja | Teil |
| Notification Dots | Ja/Opt-in | Ja | Ja | Ja/Preview | Ja | Ja | Teil |
| sichtbarer HOME-Sicherheitsausgang | Ja | Teil/Systemweg | Teil/Systemweg | Teil/Systemweg | Teil/Systemweg | Teil/Systemweg | Teil/Systemweg |
| Telefon-Gateway ohne Anrufrecht | Ja | Teil | Nein | Nein | Nein | Teil/People | Nein |
| SAF-Datei-Gateway | Ja | Teil/System-App | Nein | Nein | Nein | Teil | Nein |
| lokale Datei-Hinweise | Ja/heuristisch | Nein | Nein | Nein | Nein | Nein | Nein |
| System-Kontrollzentrum | Ja | Teil/Android | Nein | Nein | Nein | Teil | Nein |
| adaptive Split-Shell | Ja | Ja | Teil | Teil | Ja | Ja | Teil |
| Material-You-Dynamic-Color | Ja | Ja | Teil | Teil | Ja | Teil | Ja |
| Smartpen live erkannt | Ja | Ja/System | Nein | Nein | Nein | Nein | Teil/Systembasis |
| druckempfindlicher Pen Workspace | Ja | Teil/System-Apps | Nein | Nein | Nein | Nein | Nein |
| systemweite Stylus-Handschrift | Teil/IME | Ja | Teil/IME | Teil/IME | Teil/IME | Teil/IME | Teil/IME |
| lokale durchsuchbare In-App-FAQ | Ja | Teil | Nein | Teil | Teil | Teil | Nein |
| Preview vor KI-Layoutmutation | Ja | Nein | Nein | Nein | Nein | Nein | Nein |
| offener Launcher-Quellcode | Ja | Teil/AOSP | Nein | Nein | Nein | Nein | Ja |

## Wo KoSch führt

KoSch ist in 14 von 65 Kategorien allein oder geteilt vorne:

- Onboarding und sicherer Launcher-Wechsel;
- Telefon-Einstieg, sichere Dateiauswahl, Datei-Intelligenz und Kontrollzentrum;
- Offline-Kern, Betrieb ohne Konto/API, KI-Befehlsabdeckung und Grounding;
- Daten- und Berechtigungsminimierung sowie Privacy-Kommunikation;
- integrierter Pen Workspace.

Ein Teil dieser Führung entsteht durch den größeren System-Scope. Das ist für das Ziel „KI unter dem Launcher“ relevant, darf aber nicht über die geringere klassische Launcher-Tiefe hinwegtäuschen.

## Größte Rückstände

| Prio | Kategorie | KoSch | Bestwert/Leader | Lücke |
|---:|---|---:|---|---:|
| P0 | Kontakte-Integration | 1,2 | 8,0 / Microsoft | 6,8 |
| P0 | gemessene Laufzeitperformance | 3,8 | 9,6 / Pixel | 5,8 |
| P0 | Lernen und Personalisierung | 3,2 | 8,5 / Microsoft | 5,3 |
| P0 | Lokalisierung | 4,2 | 9,5 / Pixel | 5,3 |
| P0 | Observability und Audit | 1,8 | 7,0 / Microsoft, Lawnchair | 5,2 |
| P0 | Widget Resize/Restore/Undo | 4,2 | 9,0 / Nova | 4,8 |
| P0 | Produktionsreife | 4,9 | 9,6 / Pixel | 4,7 |
| P0 | Prompt-Injection-Abwehr | 4,5 | 9,0 / Lawnchair | 4,5 |
| P0 | Backup und Migration | 5,2 | 9,4 / Nova | 4,2 |
| P1 | Wallpaper-/Theme-System | 5,8 | 9,6 / Nova | 3,8 |
| P1 | Testabdeckung | 5,9 | 9,5 / Pixel | 3,6 |
| P1 | OEM-Kompatibilität | 5,0 | 8,5 / Pixel | 3,5 |

## 25 simulierte Fachperspektiven

| Fachperspektive | KoSch Ø | Fachperspektive | KoSch Ø |
|---|---:|---|---:|
| Android Launcher Architect | 6,8 | Product Manager | 6,9 |
| Android Framework Engineer | 6,8 | Android Power User | 6,9 |
| Jetpack Compose Engineer | 6,8 | Accessibility User Advocate | 6,6 |
| Mobile UX Director | 6,9 | SAF and File Systems Expert | 6,8 |
| Visual Design Lead | 6,9 | Telephony Integration Engineer | 6,9 |
| Interaction Designer | 6,8 | Widget and Shortcut Expert | 6,7 |
| Accessibility Auditor | 6,6 | Mobile Performance Engineer | 6,6 |
| Privacy Engineer | 6,9 | Battery and Thermal Engineer | 6,7 |
| Mobile Security Engineer | 6,8 | QA Automation Lead | 6,6 |
| Applied AI Architect | 6,8 | Reliability/SRE Engineer | 6,6 |
| On-device ML Engineer | 6,7 | Google Play Policy Reviewer | 6,8 |
| LLM Safety Researcher | 6,7 | Competitive Product Analyst | 6,8 |
| Open-source Compliance Counsel | 6,9 | **Mittel** | **6,8** |

Accessibility, Performance, QA und SRE bleiben am strengsten. Das ist plausibel: Ein moderner Bildschirm und grüner JVM-/Lint-Build sind kein Ersatz für TalkBack-Nutzertests, Macrobenchmarks, Prozess-Tod-Matrizen und echte OEM-/Stiftgeräte.

## Verbesserungsvorschläge für RUN M2.3

| Prio | Arbeitspaket | überprüfbares Akzeptanzkriterium | Hauptkategorien |
|---:|---|---|---|
| P0 | Performancebudgets | Macrobenchmark für Cold/Warm Start, P95 Frame Time, RSS, App-Katalog, Badge-Update und Akku; harte CI-Grenzen auf Referenzgeräten | C50, C53, C58 |
| P0 | instrumentierte Recovery-Matrix | HOME, Rotation, Prozess-Tod, SAF Replace/Release, Widget Pick/Configure/Cancel/Delete und Ink-Restore auf API 29/33/36/37 | C48, C51, C52, C58 |
| P0 | Accessibility-Abnahme | TalkBack, Switch Access, Tastatur, 200-%-Schrift, Kontrast, Reduced Motion; keine abgeschnittene Primäraktion bei 320 dp | C06–C10, C63, C65 |
| P0 | vollständige Widget-Engine | Raster/freie Platzierung, Resize, Stacks, Provider-Restore-Mapping und transaktionales Undo | C20, C21 |
| P0 | verschlüsseltes Backup/Restore | versionierter Export, Dry-Run-Import, Konfliktbericht, stale Apps/Profiles und Widget-Mapping; automatisierte Migration von Schema v1–v3 | C24, C51 |
| P0 | sichere Kontakte | Android-17-Kontaktpicker oder explizite Einzelwahl ohne `READ_CONTACTS`; `ACTION_DIAL` bleibt Bestätigungspunkt | C26, C27, C42 |
| P1 | Multi-Profile-/Private-Space-Lab | getrennte Container, Lock/Unlock, Hide/Show und keinerlei Label-/Badge-Leak im gesperrten Zustand auf unterstützten Geräten | C11–C19, C41, C52 |
| P1 | Pen-Latenz und Export | gemessene Ink-Latenz, Coalesced/Historical Events, AndroidX Ink evaluieren, PNG/PDF/Share nur nach Vorschau; USI/S Pen/Pixel Pen-Matrix | C61–C64, C50, C52 |
| P1 | lokales Audit | metadatenarmes Aktionsprotokoll mit Retention, Ansicht, Export und vollständigem Löschen; kein Prompt-, Ink- oder Notification-Inhalt | C45, C54, C55 |
| P1 | isoliertes lokales LLM | eigener Service/Prozess; GGUF-Import per SAF, Hash/Lizenz, Geräteprobe, stream/cancel/unload, Memory-/Thermal-Gate und Local-Core-Fallback | C34–C40, C50, C53 |
| P1 | Capability Planner | strikt validiertes Schema, Allowlist, Risikoklassen, Preview/Confirm/Undo sowie negative Prompt-Injection-/Tool-Output-Evals | C36, C37, C40, C45, C46 |
| P2 | Launcher-Parität | manuelle Ordner/Seiten, Drag-and-drop, Dock-Reorder, Gesten, Icon-Packs, Widget im Dock und Stacks | C14–C24 |
| P2 | Theme Program v1 | signiertes capability-loses Theme-Paket, Material-Token, Icon-/Wallpaper-Vorschau und atomarer Rollback; LCARS nur als optionales Paket | C01, C05, C23, C57 |
| P2 | Lokalisierung und Self-Service | mindestens Deutsch/Englisch vollständig, String-Ressourcen, Plural/RTL-Test, FAQ-Deep-Links und Diagnoseexport ohne Nutzdaten | C56, C65 |
| P2 | Release Engineering | signierter Alpha-Track, SBOM, Dependency-/Secret-Scan, reproduzierbare Artefakte, Crash-free-/ANR-Gate | C49, C58–C60 |

### Führungs-Gate

Das langfristige Ziel „besser als jeder einzelne Referenz-Launcher“ wird erst akzeptiert, wenn KoSch im dann aktuellen 65+-Kategorien-Satz mindestens 8,3 allgemein und 8,0 im Rollenmittel erreicht, keine Kategorie unter 7,0 liegt und alle P0-Gates auf echten Geräten bestehen. Der Score darf nicht durch neue Nischenkategorien allein gewonnen werden; UX, Widgets, Performance, Accessibility, Backup und Stabilität müssen ebenfalls Referenzniveau erreichen.

## Primärquellen

### Android und Pixel-Systemreferenz

- [Android 17](https://developer.android.com/about/versions/17/) und [Android-17-Geräte/Emulator](https://developer.android.com/about/versions/17/get).
- [Android Stylus](https://developer.android.com/develop/ui/views/touch-and-input/stylus-input), [Compose Stylus](https://developer.android.com/develop/ui/compose/touch-input/stylus-input) und [Ink API Setup](https://developer.android.com/develop/ui/compose/touch-input/stylus-input/ink-api-setup).
- [Stylus-Handschrift in Textfeldern](https://developer.android.com/develop/ui/views/touch-and-input/stylus-input/stylus-input-in-text-fields), [`InputDevice`](https://developer.android.com/reference/android/view/InputDevice) und [`InputManager.InputDeviceListener`](https://developer.android.com/reference/android/hardware/input/InputManager.InputDeviceListener).
- [Adaptive Dos and Don'ts](https://developer.android.com/develop/adaptive-apps/guides/adaptive-dos-and-donts), [Large-screen input](https://developer.android.com/develop/ui/views/touch-and-input/input-compatibility-on-large-screens), [Material 3](https://developer.android.com/develop/ui/compose/designsystems/material3) und [Compose Insets](https://developer.android.com/develop/ui/compose/system/insets).
- [`LauncherApps`](https://developer.android.com/reference/kotlin/android/content/pm/LauncherApps.html) und [Private-Space-Pflichten für Launcher](https://developer.android.com/about/versions/15/behavior-changes-all).

### Konkurrenzprodukte

- Nova: [Featureübersicht](https://novalauncher.com/), [Nova Beta](https://novalauncher.com/beta/), [FAQ](https://novalauncher.com/faq/) und [Privacy](https://novalauncher.com/privacy/).
- Niagara: [Pro-Funktionen](https://help.niagaralauncher.app/article/40-niagara-pro-features), [Pop-ups](https://help.niagaralauncher.app/article/115-pop-ups), [Niagara Button](https://help.niagaralauncher.app/article/116-niagara-button) und [Notification-Hilfe](https://help.niagaralauncher.app/article/31-notifications-not-showing).
- Smart Launcher: [Glossar](https://docs.smartlauncher.net/faq/glossary), [Versionsvergleich](https://docs.smartlauncher.net/faq/start-here/differences-between-versions), [6.6-Changelog](https://docs.smartlauncher.net/faq/changelog/6.6) und [6.0-Changelog](https://docs.smartlauncher.net/faq/changelog/6.0).
- Microsoft: [Using Microsoft Launcher on Android](https://support.microsoft.com/en-us/office/using-microsoft-launcher-on-android).
- Lawnchair: [offizielles Repository](https://github.com/LawnchairLauncher/lawnchair); dort wird Lawnchair 16 als Entwicklungslinie und Lawnchair 15 Beta 3 als empfohlener Download ausgewiesen.

## Professionelles Fazit

M2.2 erfüllt den konkreten Run-Auftrag: modernes, adaptives Design, herstellerneutrale Smartpen-Erkennung, zusätzliche Pen-Funktionen, Material-You-Integration, Arbeitsprofil-Badging, lokale FAQ und weiterhin ein funktionsfähiger API-freier Kern mit HOME-Notausgang. Besonders Pen Space ist ein echter Differenzierer statt einer Marketingzeile.

Der wichtigste professionelle Befund bleibt unbequem: KoSch ist noch nicht besser als jeder etablierte Launcher. Es ist bereits breiter und sicherer in ausgewählten System-/AI-Funktionen, aber im täglichen Launcher-Handwerk und in der nachgewiesenen Reife zurück. M2.3 muss weniger neue Demo-Flächen und mehr Messung, Restore, Accessibility, Widget-Parität und Gerätebeweise liefern.
