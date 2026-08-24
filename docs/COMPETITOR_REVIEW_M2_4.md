# M2.4: strenger Professional-Launcher- und Expertenvergleich

Stand: 24. August 2026

## Urteil

**KoSch M2.4 erreicht 8,1 / 10,0 allgemein und 7,9 / 10,0 im Mittel von 25 simulierten Fachperspektiven.** Gegenüber M2.3 sind das jeweils +0,5 Punkte. KoSch bleibt in dieser breiten Matrix auf Rang 2 hinter Pixel/Android 17 und vor Microsoft Launcher.

Das Ziel **über 9,5** ist ausdrücklich **nicht erreicht**. M2.4 ist eine ungewöhnlich breite, local-first Professional-Alpha mit belastbaren Sicherheitsgrenzen. Es ist aber noch nicht der beste Alltagslauncher: Pixel führt bei Systemreife, Nova und Smart Launcher bei klassischer Anpassungs-/Widgettiefe, Niagara bei fokussierter Interaktion und Microsoft bei etablierter Productivity-Integration.

## Methodik und Grenzen

- Vergleich: KoSch M2.4, Pixel/Android 17 als Systemreferenz sowie Nova 8/8.1 Beta, Niagara 1.x, Smart Launcher 6.6, Microsoft Launcher und Lawnchair 15 Beta 3.
- Skala: 0,1 = praktisch nicht vorhanden; 10,0 = nachweislich erstklassig und produktionsbewährt.
- 90 gleich gewichtete Kategorien, sieben Kandidaten und 25 Fachrollen.
- Rohdaten: [7 × 90 Vergleich](launcher_comparison_m2_4.csv), [25 × 90 KoSch-Fachmatrix](expert_scores_m2_4.csv), [25 Rollen × 7 Launcher](expert_launcher_overall_m2_4.csv) und [formatierte Arbeitsmappe](launcher_benchmark_m2_4.xlsx).
- Die 25 Rollen sind **simulierte, reproduzierbare Fachperspektiven**, keine interviewten Personen. Relevante Fokusbereiche werden je Rolle zusätzlich um 0,2 Punkte strenger bewertet.
- M2.3-Werte wurden nur für quellenbelegte M2.4-Deltas verändert. Die 15 neuen Kategorien C76–C90 bewerten Lifecycle, Export-Handoff, Migration, lokale Personalisierung, App-Verwaltung, SAF-Arbeitsraum, Systemwege, Widgets, Pen, Accessibility und Evidenz.
- KoSch-Codebasis: M2.4-Branch und [grüner GitHub-Actions-Lauf #30](https://github.com/chekento/kosch-ai-android/actions/runs/32709807342) mit Tests, Lint, Debug-/Release-Build, Permission-Budget und APK-Prüfsumme.
- Es gab **keinen** identischen Sieben-Launcher-Labortest. Unbelegte Performance-, Akku-, OEM-, Accessibility- oder Recovery-Eigenschaften bleiben konservativ.
- Pixel/Android 17 umfasst Pixel Launcher und eng verbundene Systemflächen. Das ist bewusst eine anspruchsvolle Systemreferenz, kein reiner APK-gegen-APK-Test.
- Konkurrenzmerkmale stammen aus offiziellen Hersteller-/Projektquellen. Marketingaussagen erhalten ohne reproduzierbaren Labornachweis keine 10,0.

## Gesamtrangliste

| Rang | Launcher | Allgemein | 25 Rollen | Strenges Urteil |
|---:|---|---:|---:|---|
| 1 | Pixel / Android 17 | 8,3 | 8,3 | stärkste System-, Accessibility-, Performance- und Reifereferenz |
| 2 | **KoSch M2.4** | **8,1** | **7,9** | breiteste local-first Professional-/Privacy-Shell; noch Alpha |
| 3 | Microsoft Launcher | 7,3 | 7,3 | etablierte Feed-, Kalender-, Aufgaben- und Work-Profile-Integration |
| 4= | Nova 8 / 8.1 Beta | 7,0 | 7,0 | sehr starke klassische Anpassung, Gesten, Ordner und Backup |
| 4= | Smart Launcher 6.6 | 7,0 | 7,0 | tiefe Organisation, Dock-/Widget-/Foldable-Funktionen |
| 6= | Niagara 1.x | 6,9 | 6,9 | außergewöhnlich klare Navigation, Pop-ups, Themes und Widget Stacks |
| 6= | Lawnchair 15 Beta 3 | 6,9 | 6,9 | offene Launcher3-/Pixel-Basis; 16er Linie ist Entwicklung |

Gleichstände entstehen durch Veröffentlichung auf eine Dezimalstelle. Einzelwerte und Formeln stehen in der Arbeitsmappe.

## Bereichsergebnis

| Bereich | KoSch | Pixel | Nova | Niagara | Smart | Microsoft | Lawnchair |
|---|---:|---:|---:|---:|---:|---:|---:|
| UX | 7,9 | 9,0 | 8,0 | 8,5 | 8,4 | 8,1 | 8,0 |
| Launcher | 7,9 | 8,5 | 8,7 | 8,1 | 8,6 | 8,1 | 7,9 |
| System | 8,3 | 8,1 | 3,4 | 3,2 | 3,3 | 5,5 | 3,6 |
| AI | 7,8 | 6,7 | 5,8 | 5,8 | 5,9 | 6,0 | 6,3 |
| Security | 8,8 | 8,5 | 7,0 | 7,0 | 7,1 | 7,5 | 7,4 |
| Engineering | 7,7 | 8,4 | 7,6 | 7,4 | 7,4 | 7,5 | 7,6 |
| Product | 8,4 | 8,7 | 8,0 | 8,5 | 8,5 | 8,6 | 7,4 |

KoSch führt im Mittel bei System, AI und Security. Das ist eine echte Differenzierung, aber auch ein Scope-Effekt: Der Launcher übernimmt mehr sichere Gateway-Aufgaben. Im klassischen Launcher-Handwerk liegt er weiter hinter Pixel, Nova und Smart Launcher. Engineering bleibt ohne Geräte-/Performancebeweise 0,7 Punkte hinter Pixel.

## M2.3 → M2.4: belegter Fortschritt

| Kategorie | M2.3 | M2.4 | Delta | Begründung |
|---|---:|---:|---:|---|
| Lernen und Personalisierung | 3,2 | 7,6 | +4,4 | begrenzte App-Startsignale, Count/Recency-Ranking, Reset, sichtbare Sortierung |
| Nutzen der Datei-Intelligenz | 5,6 | 7,8 | +2,2 | SAF-Arbeitsraum, Metadatenübersicht, Suche/Sortierung, bestätigte Operationen |
| Crash Recovery | 7,4 | 8,4 | +1,0 | ViewModel-Eigentum und process-restorable Export-Handoff |
| Widget Resize/Restore/Undo | 5,6 | 6,4 | +0,8 | Reihenfolge und Undo; freie Platzierung/Stacks/Restore weiter offen |
| KI-Befehlsabdeckung | 7,4 | 8,2 | +0,8 | Datei-Arbeitsraum und zusätzliche Systemwege im Local Planner |
| Zuletzt verwendet | 7,4 | 8,2 | +0,8 | explizite Häufig-/Zuletzt-Sortierung und adaptive lokale Signale |
| System-Kontrollzentrum | 8,1 | 8,8 | +0,7 | Hintergrund, Anzeige, Ton, Akku, Datenschutz, Accessibility, Defaults, Speicher |
| Testabdeckung | 6,9 | 7,6 | +0,7 | Pending Export, Migration, Ranking, Datei-Planer, Ink/SVG |
| Backup und Migration | 7,9 | 8,5 | +0,6 | stabilere App-Key-Migration und process-sicherer Export-Handoff |
| Semantische Accessibility | 6,7 | 7,3 | +0,6 | App-/Dock-Semantik und Pen-Custom-Actions; Lab noch offen |

Der Zuwachs stammt nicht aus einem eingebauten LLM. Er entsteht aus erklärbarer lokaler Personalisierung, Systemtiefe und Resilienz – genau den Funktionen, die auch ohne API täglich nutzbar sind.

## Funktionsvergleich M2.4

Legende: **Ja** = belegt vorhanden; **Teil** = begrenzt, systemabhängig oder anders gelöst; **Nein** = nicht als Launcher-Funktion belegt.

| Funktion | KoSch | Pixel | Nova | Niagara | Smart | Microsoft | Lawnchair |
|---|---|---|---|---|---|---|---|
| echte HOME-App | Ja | Ja | Ja | Ja | Ja | Ja | Ja |
| sofort ohne Konto/API | Ja | Ja/Basis | Ja | Ja | Ja | Teil | Ja |
| Pro-/Command-Center | Ja | Teil | Teil | Teil | Teil | Ja/Feed | Nein |
| lokaler deterministischer Action Planner | Ja | Teil | Teil | Teil | Teil | Teil | Nein |
| integriertes generatives Launcher-LLM | Nein | Nein | Nein | Nein | Nein | Nein | Nein |
| profilbewusster App-Katalog | Ja | Ja | Teil | Teil | Teil | Ja | Teil |
| lokale Count/Recency-Personalisierung mit Reset | Ja | Teil | Teil | Teil | Teil | Teil | Teil |
| steuerbare App-Sortierung / verborgene Apps | Ja | Ja | Ja | Ja | Ja | Ja | Ja |
| App-Shortcuts | Ja | Ja | Ja | Ja | Ja | Teil | Ja |
| sicherer App-Aktionsraum / System-Uninstall | Ja | Ja | Ja | Ja | Ja | Ja | Ja |
| Hardware-Shortcut-Hilfe | Ja | Ja | Teil | Teil | Teil | Teil | Teil |
| lokale Smart-Ordner mit Vorschau | Ja | Nein | Teil | Nein | Ja | Teil | Teil |
| manuelle Ordner/Drag-and-drop | Teil | Ja | Ja | Ja/Pop-ups | Ja | Ja | Ja |
| mehrere Seiten/Dock-Reorder/Gesten/Icon-Packs | Teil | Teil | Ja | Teil | Ja | Teil | Ja |
| Widget Hosting | Ja/Board | Ja | Ja | Ja | Ja | Ja | Ja |
| Widget-Größenpresets | Ja/3 | Ja | Ja | Ja | Ja | Ja | Ja |
| Widget-Reihenfolge und Undo | Ja | Teil | Ja | Ja | Ja | Teil | Teil |
| Widget Stacks / Geräte-Restore | Nein | Teil | Ja | Ja | Ja | Teil | Teil |
| Telefon-Gateway ohne Anrufrecht | Ja | Teil | Nein | Nein | Teil | Teil | Nein |
| datensparsame Einzelkontaktwahl | Ja | Ja/System | Nein | Nein | Nein | Teil | Teil |
| read-only SAF-Einzeldatei | Ja | Teil/System | Nein | Nein | Nein | Teil | Nein |
| begrenzter SAF-Datei-Arbeitsraum | Ja | Teil/System | Nein | Nein | Nein | Teil | Nein |
| bestätigtes Create/Rename/Delete + Rename-Undo | Ja | Teil/System | Nein | Nein | Nein | Teil | Nein |
| lokale Dateimetadaten-Hinweise | Ja | Teil/System | Nein | Nein | Nein | Teil | Nein |
| verschlüsseltes portables Workspace-Backup | Ja | Teil/System | Teil | Teil | Teil | Teil | Teil |
| Restore-Dry-Run und zweite Bestätigung | Ja | Teil | Teil | Teil | Teil | Teil | Nein/belegt |
| process-restorable Export-Handoff | Ja | Teil/System | Teil | Teil | Teil | Teil | Teil |
| metadatenarmes lokales Audit | Ja | Teil/System | Nein/belegt | Nein/belegt | Nein/belegt | Teil | Teil/Projekt |
| sichtbare Capability-Risikopolicy | Ja | Teil/System | Nein | Nein | Nein | Nein | Nein |
| sichtbarer HOME-Sicherheitsausgang | Ja | Teil/System | Teil/System | Teil/System | Teil/System | Teil/System | Teil/System |
| adaptive Split-Shell / Foldable-Basis | Ja | Ja | Teil | Teil | Ja | Ja | Teil |
| Material-You-Dynamic-Color | Ja | Ja | Teil | Teil | Ja | Teil | Ja |
| explizite Semantik / Reduced Motion | Teil/Code | Ja/Lab | Teil | Teil | Teil | Teil | Teil |
| Smartpen live erkannt | Ja | Ja/System | Nein | Nein | Nein | Nein | Teil |
| druckempfindlicher Pen Workspace | Ja | Teil/System-Apps | Nein | Nein | Nein | Nein | Nein |
| Pen-SVG mit Langstrich-Integrität | Ja | Teil/System-Apps | Nein | Nein | Nein | Nein | Nein |
| lokale durchsuchbare In-App-FAQ | Ja/52 | Teil | Nein | Ja | Ja | Teil | Teil |
| offener Launcher-Quellcode | Ja | Teil/AOSP | Nein | Nein | Nein | Nein | Ja |

## Neue Evidenzkategorien C76–C90

| ID | Kategorie | KoSch | Bestwert | Leader | Lücke |
|---|---|---:|---:|---|---:|
| C76 | Konfigurationswechsel-/ViewModel-Resilienz | 8,8 | 9,6 | Pixel | 0,8 |
| C77 | Prozesssichere Exportübergabe | 9,0 | 9,3 | Pixel | 0,3 |
| C78 | Profilstabile App-Key-Migration | 8,8 | 9,5 | Pixel | 0,7 |
| C79 | Adaptives lokales Nutzungsranking | 8,6 | 9,3 | Smart Launcher | 0,7 |
| C80 | Transparenz/Löschung lokaler Lernsignale | 9,2 | 9,2 | KoSch | 0,0 |
| C81 | Verborgene Apps und App-Sortierung | 8,5 | 9,4 | Nova / Smart Launcher | 0,9 |
| C82 | Sichere App-Verwaltung/Deinstallationsgrenze | 8,8 | 9,2 | Pixel | 0,4 |
| C83 | Begrenzter SAF-Datei-Arbeitsraum | 9,1 | 9,1 | KoSch | 0,0 |
| C84 | Dateioperationen mit Preview/Bestätigung/Undo | 8,8 | 9,0 | Pixel | 0,2 |
| C85 | Lokale Dateimetadaten-Intelligenz | 7,8 | 7,8 | KoSch | 0,0 |
| C86 | Systemeinstellungs-Abdeckung/Fallback | 9,0 | 9,7 | Pixel | 0,7 |
| C87 | Widget-Reihenfolge und Undo | 8,2 | 9,3 | Pixel | 1,1 |
| C88 | Pen-SVG und Integrität langer Striche | 9,0 | 9,0 | KoSch | 0,0 |
| C89 | Explizite Semantik/Accessibility-Aktionen | 7,8 | 9,3 | Pixel | 1,5 |
| C90 | Evidenztreue/Release-Nachvollziehbarkeit | 9,1 | 9,5 | Pixel | 0,4 |

## 25 simulierte Fachperspektiven

| Fachperspektive | KoSch M2.4 |
|---|---:|
| Android Launcher Architect | 7,9 |
| Android Framework Engineer | 7,9 |
| Jetpack Compose Engineer | 7,9 |
| Mobile UX Director | 8,1 |
| Visual Design Lead | 8,0 |
| Interaction Designer | 8,0 |
| Accessibility Auditor | 7,7 |
| Privacy Engineer | 8,0 |
| Mobile Security Engineer | 7,9 |
| Applied AI Architect | 7,9 |
| On-device ML Engineer | 7,8 |
| LLM Safety Researcher | 7,9 |
| Open-source Compliance Counsel | 8,0 |
| Product Manager | 8,0 |
| Android Power User | 8,0 |
| Accessibility User Advocate | 7,8 |
| SAF and File Systems Expert | 7,9 |
| Telephony Integration Engineer | 8,0 |
| Widget and Shortcut Expert | 7,9 |
| Mobile Performance Engineer | 7,8 |
| Battery and Thermal Engineer | 7,8 |
| QA Automation Lead | 7,8 |
| Reliability/SRE Engineer | 7,8 |
| Google Play Policy Reviewer | 7,9 |
| Competitive Product Analyst | 7,9 |

Die niedrigste Rolle bleibt Accessibility Auditor mit 7,7, weil Code-Semantik keinen realen TalkBack-/Switch-/200-%-Test ersetzt. Mobile UX führt mit 8,1. Keine Fachperspektive rechtfertigt 9,5.

## Wo KoSch führt

KoSch liegt in **31 von 90 Kategorien** allein oder geteilt vorn. Besonders belastbar sind:

- offline und ohne Konto/API nutzbarer Kern;
- Daten-/Berechtigungsminimierung und transparente lokale Lernsignale;
- HOME-Sicherheitsausgang und feste Capability-Grenzen;
- sichere Einzeldatei- und Tree-SAF-Routen;
- Datei-Metadaten-Intelligenz ohne Vollinhaltsindex;
- Pen Workspace und lokaler SVG-Export;
- verschlüsseltes Backup, Restore-Kontrolle und metadatenarmes Audit;
- Professional Command Center, Tastatur-Recovery und lokale FAQ;
- nachvollziehbare, nicht auf 9,5 geschönte Release-Evidenz.

Ein Teil der Führung entsteht aus KoSchs breiterem System-Scope. Sie ersetzt nicht Nova-/Smart-Launcher-Tiefe oder Niagara-Klarheit.

## Größte Rückstände

| Prio | Kategorie | KoSch | Bestwert/Leader | Lücke |
|---:|---|---:|---|---:|
| P0 | Lokalisierung | 4,2 | 9,5 / Pixel | 5,3 |
| P0 | gemessene Laufzeitperformance | 4,4 | 9,6 / Pixel | 5,2 |
| P0 | Produktionsreife | 5,6 | 9,6 / Pixel | 4,0 |
| P0 | Wallpaper-/Theme-System | 6,4 | 9,6 / Nova | 3,2 |
| P0 | OEM-Kompatibilität | 5,4 | 8,5 / Pixel | 3,1 |
| P0 | Notification Dots/Badges | 6,3 | 9,2 / Pixel/Niagara | 2,9 |
| P0 | Akku-/Energieeffizienz | 6,4 | 9,1 / Pixel | 2,7 |
| P0 | Ordner | 6,7 | 9,3 / Nova | 2,6 |
| P0 | Widget Resize/Restore/Undo | 6,4 | 9,0 / Nova | 2,6 |
| P1 | Spracheingabe | 6,0 | 8,4 / Pixel | 2,4 |
| P1 | Prompt-Injection-Abwehr | 6,7 | 9,0 / Lawnchair | 2,3 |
| P1 | kleine Displays | 7,2 | 9,3 / Niagara | 2,1 |
| P1 | App-Shortcuts | 7,4 | 9,5 / Pixel | 2,1 |
| P1 | Widget Hosting | 7,2 | 9,3 / Smart Launcher | 2,1 |
| P1 | Workspace-Anpassung | 7,6 | 9,6 / Nova | 2,0 |
| P1 | Play-Policy-Readiness | 7,8 | 9,8 / Pixel | 2,0 |

Der absolute Tiefstwert ist das integrierte generative Modell mit 1,3. Auch die Vergleichslauncher belegen kein voll integriertes Launcher-LLM; für KoSchs Produktvision bleibt dies dennoch eine zentrale, aber bewusst nachgelagerte Lücke.

## Verbesserungsvorschläge für RUN M2.5

| Prio | Arbeitspaket | überprüfbares Akzeptanzkriterium |
|---:|---|---|
| P0 | Macrobenchmark und generiertes Baseline-Profil | Cold P95 ≤ 1.000 ms, Warm P95 ≤ 500 ms, Frame P95 ≤ 16,7 ms; Rohdaten/Geräteklasse versioniert |
| P0 | Accessibility-Lab | TalkBack, Switch, 200 %, Bold Text, Reduced Motion, 320 dp; keine blockierte/abgeschnittene Primäraktion |
| P0 | HOME-/OEM-/Lifecycle-Lab | Pixel/AOSP, Samsung, weiterer OEM; API 29/33/36/37; Neustart, Crash, Prozess-Tod und Launcher-Wechsel |
| P0 | SAF-Provider-Lab | lokale, Cloud- und OEM-Provider; Grant-Verlust, 500 Einträge, Create/Rename/Delete/Abbruch |
| P0 | vollständige Widget-Engine | freie Platzierung, Stacks, Provider-Restore-Mapping; Pick/Configure/Cancel/Delete/Prozess-Tod instrumentiert |
| P0 | Launcher-Parität | manuelle Ordner/Seiten, Drag/drop, Dock-Reorder, Gesten und Icon-Packs mit Undo/A11y |
| P0 | Lokalisierung | vollständiges Deutsch/Englisch; keine hardcodierten UI-Strings; RTL-/Plural-/Screenshot-Test |
| P0 | Release Engineering | signierter APK/AAB, SBOM, Dependency-/License-/Secret-Scan, reproduzierbarer Upgrade-/Rollback-Test |
| P1 | Notification-/Profile-Härtung | Personal/Work/Private, Pause/Lock, Multi-User und OEM-Service-Restart ohne Leaks |
| P1 | Smartpen-Gerätelabor | S Pen, USI, Pixel Pen, Bluetooth; Historical Events/AndroidX Ink; Latenz P95 ≤ 25 ms |
| P1 | API-37-Upgrade | compile/target 37, nativer Contact Picker und API-37-Instrumentierung |
| P1 | isoliertes lokales LLM | erst danach: separater Prozess, SAF-Modellimport, Hash/Lizenz, Geräteprobe, Stream/Cancel/Unload, Thermikgate |
| P1 | unabhängige Prüfungen | Privacy-/Security-Review von Backup, Audit, SAF, Learning; Restore-/Provider-Fuzzing |

## 9,5-Gate

Eine Bewertung über 9,5 ist erst zulässig, wenn alle automatisierten und manuellen Gates in [QUALITY_GATES.md](QUALITY_GATES.md) bestanden sind, keine der 90+ Kategorien unter 8,5 liegt, kritische Bereiche jeweils mindestens 9,5 erreichen und Messdaten/Build-SHA/Geräteklassen auditierbar sind.

M2.4 erfüllt die automatisierten Code-Gates, aber nicht die Geräte-, Accessibility-, Performance-, Release- und unabhängigen Review-Gates. Eine Selbsteinstufung über 9,5 wäre fachlich falsch.

## Aktuelle Primärquellen

### Android und Systemreferenz

- [Android 17](https://developer.android.com/about/versions/17/), [Release Notes](https://developer.android.com/about/versions/17/release-notes) und [SDK-Setup](https://developer.android.com/about/versions/17/setup-sdk).
- [Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files) und [DocumentsContract](https://developer.android.com/reference/android/provider/DocumentsContract).
- [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel) und [Compose Accessibility Semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics).
- [LauncherApps](https://developer.android.com/reference/kotlin/android/content/pm/LauncherApps.html), [App Widgets](https://developer.android.com/develop/ui/views/appwidgets/host), [Macrobenchmark](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview) und [Advanced Stylus](https://developer.android.com/develop/ui/views/touch-and-input/stylus-input/advanced-stylus-features).

### Konkurrenzprodukte

- Nova: [Featureübersicht](https://novalauncher.com/) und [offizielles Beta-Changelog](https://novalauncher.com/beta/).
- Niagara: [Pro-Funktionen](https://help.niagaralauncher.app/article/40-niagara-pro-features).
- Smart Launcher: [6.6-Changelog](https://docs.smartlauncher.net/faq/changelog/6.6) und [Backup/Umzug](https://docs.smartlauncher.net/faq/move-smart-launcher-to-a-new-phone).
- Microsoft: [Using Microsoft Launcher on Android](https://support.microsoft.com/en-us/office/using-microsoft-launcher-on-android).
- Lawnchair: [offizielles Repository und Release-Hinweis](https://github.com/LawnchairLauncher/lawnchair).

## Professionelles Fazit

M2.4 ist sichtbar besser als M2.3: lokale Personalisierung ist transparent und löschbar, Dateien sind ein echter begrenzter Workspace statt nur eine Demo-Analyse, Exporte überleben Lifecycle-Unterbrechungen, App-/Widget-/Pen-Flächen sind tiefer und die Dokumentation ist vollständiger.

Der harte Befund bleibt: Rang 2 in einer breiten Matrix macht KoSch nicht automatisch zum zweitbesten Launcher für jeden Alltag. Die nächste große Verbesserung kommt nicht durch mehr Oberflächen, sondern durch reale Messung, Accessibility/OEM/SAF-Labs, vollständige Launcher-/Widget-Parität, Release Engineering und erst danach ein isoliertes lokales Modell.
