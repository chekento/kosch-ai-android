# M2.1: strenger Launcher- und Expertenvergleich

Stand: 23. August 2026

## Urteil

**KoSch M2.1 erreicht allgemein 6,6 / 10,0 und in den 25 simulierten Fachperspektiven 6,5 / 10,0.** Das ist ein klarer Fortschritt gegenüber M2 (6,1), aber noch kein Sieg über einen der sechs Referenzprodukte im Gesamtmittel. Pixel/Android 16 führt mit 8,1; danach folgen Microsoft Launcher mit 7,5, Nova und Smart Launcher mit jeweils 7,4, Niagara mit 7,3 und Lawnchair 15 Beta 3 mit 7,2.

KoSch ist bereits der stärkste Kandidat in der untersuchten Kombination aus API-freiem KI-Kern, sichtbarem Launcher-Notausgang, sicherem Telefon-/Datei-Gateway, Datenminimierung und explizit gegroundeten KI-Aktionen. Es verliert noch deutlich bei Launcher-Tiefe, Widget-Lifecycle, Accessibility, adaptiven Layouts, Backup, gemessener Performance, OEM-Reife und einem wirklich integrierten lokalen LLM.

## Methodik und harte Einschränkungen

- Vergleich: KoSch M2.1, Pixel Launcher/Android 16 als Systemreferenz sowie Nova 8/8.1 Beta, Niagara, Smart Launcher 6.6, Microsoft Launcher und Lawnchair 15 Beta 3.
- Skala: 0,1 = praktisch nicht vorhanden; 10,0 = nachweislich erstklassig und produktionsbewährt.
- 60 Kategorien, sieben Produkte und 25 Fachrollen. Die vollständigen Rohdaten stehen in [launcher_comparison_m2_1.csv](launcher_comparison_m2_1.csv), [expert_scores_m2_1.csv](expert_scores_m2_1.csv) und [expert_launcher_overall_m2_1.csv](expert_launcher_overall_m2_1.csv).
- Die 25 Rollen sind **simulierte, reproduzierbare Fachperspektiven**, keine tatsächlich befragten Menschen. Jede Rolle bewertet alle 60 Kategorien; im eigenen Fachgebiet wird strenger gewichtet.
- KoSch wurde aus dem gebauten Quellstand und der grünen Android-CI bewertet. Die Konkurrenzwerte beruhen auf aktuellen offiziellen Produkt-/Supportquellen. Es gab keinen identischen Sieben-Launcher-Gerätetest; unbelegte Eigenschaften wurden konservativ bewertet.
- „Pixel / Android 16“ ist bewusst eine Referenz aus Pixel-Launcher plus den eng verbundenen Android-16-Home-Surfaces. Das ist kein reiner APK-Benchmark und wird im CSV entsprechend als Systemreferenz bezeichnet.
- Fehlende Funktionen werden auch dann niedrig bewertet, wenn ein Produkt sie absichtlich nicht anbietet. Das ist gewollt: KoSch soll die Funktionsmenge aller Kandidaten abdecken, ohne deren jeweilige Stärken zu verlieren.

## Gesamtrangliste

| Rang | Launcher | Ø aus 60 | Strenges Urteil |
|---:|---|---:|---|
| 1 | Pixel / Android 16 | 8,1 | Referenz für Integration, Accessibility, Stabilität und Performance |
| 2 | Microsoft Launcher | 7,5 | stärkste Produktivitäts-, Feed-, Work-Profile- und Importbreite |
| 3 | Nova 8 / 8.1 Beta | 7,4 | stärkste klassische Anpassbarkeit, Backup und Launcher-Tiefe |
| 4 | Smart Launcher 6.6 | 7,4 | stärkste automatische Kategorisierung und sehr breite Workspace-Funktionen |
| 5 | Niagara | 7,3 | stärkste Klarheit, Einhandbedienung und notification-nahe Pop-ups |
| 6 | Lawnchair 15 Beta 3 | 7,2 | stärkste offene Launcher3-/Pixel-Basis, aber Beta-/Regressionsrisiko |
| 7 | **KoSch M2.1** | **6,6** | eigenständig und sicher, jedoch weiterhin Alpha mit Reifelücken |

## Bereichsergebnis

| Bereich | KoSch | Pixel | Nova | Niagara | Smart | Microsoft | Lawnchair |
|---|---:|---:|---:|---:|---:|---:|---:|
| UX | 6,8 | 9,0 | 8,2 | 8,7 | 8,5 | 8,1 | 8,1 |
| Launcher | 6,6 | 8,6 | 9,0 | 8,4 | 9,0 | 8,4 | 8,2 |
| System | 6,1 | 6,9 | 3,1 | 2,7 | 2,8 | 6,0 | 2,2 |
| AI | 6,8 | 6,6 | 5,9 | 6,0 | 6,1 | 5,9 | 6,8 |
| Security | 7,9 | 8,2 | 7,1 | 7,3 | 7,2 | 7,1 | 8,1 |
| Engineering | 5,7 | 8,3 | 7,6 | 7,5 | 7,4 | 7,4 | 7,5 |
| Product | 6,5 | 8,8 | 8,8 | 9,0 | 8,7 | 8,5 | 7,3 |

Der AI-Bereich misst mehr als ein Chatfenster: Offline-Kern, API-Freiheit, Grounding, Kontext, Personalisierung, Open Source und Halluzinationsbegrenzung. Produkte ohne generative Launcher-KI erhalten dadurch Punkte für sichere Deterministik, aber keine Punkte für ein nicht vorhandenes Modell.

## Funktionsvergleich des aktuellen Stands

Legende: **Ja** = vorhanden; **Teil** = vorhanden, aber eingeschränkt/beta/anders gelöst; **Nein** = nicht als Launcher-Funktion belegt.

| Funktion | KoSch | Pixel | Nova | Niagara | Smart | Microsoft | Lawnchair |
|---|---|---|---|---|---|---|---|
| echte HOME-App | Ja | Ja | Ja | Ja | Ja | Ja | Ja |
| App-Katalog und Suche | Ja | Ja | Ja | Ja | Ja | Ja | Ja |
| App-Shortcuts | Ja | Ja | Ja | Ja | Ja | Teil | Ja |
| persistentes App-Pinning | Ja | Ja | Ja | Ja/Favoriten | Ja | Ja | Ja |
| szenenadaptives Dock | Ja | Teil/Vorschläge | Teil/Nova Now | Teil/Favoriten | Teil | Teil/Häufig | Nein |
| mehrere Home-Seiten | Teil/2 feste Räume | Ja | Ja | Nein/Designwahl | Ja | Ja | Ja |
| manuelle Ordner | Teil | Ja | Ja | Ja/Pop-ups | Ja | Ja | Ja |
| automatische App-Ordner | Ja/lokal | Nein | Ja/8.1 Beta | Nein | Ja | Teil/Work | Ja/Caddy exp. |
| Widget Hosting | Ja/Board | Ja | Ja | Ja | Ja | Ja | Ja |
| Widget Resize/Stack/Restore | Nein | Ja | Ja | Ja/Stacks | Ja | Ja | Teil |
| Notification Dots | Ja/Opt-in, count-only | Ja | Ja | Ja/Preview | Ja | Ja | Teil/frühere Beta-Regression, Fix in Beta-3-Notizen nicht belegt |
| Notification-Inhalt im Launcher | Nein/bewusst | Teil | Teil | Ja | Teil | Teil | Teil |
| Backup/Export/Restore | Teil/Schema, kein Export | Ja/System | Ja | Teil | Ja/täglich | Ja/Import | Teil |
| Private-Space-Integration | Nein | Ja | Teil | Teil | Teil | Work Profile statt Private Space | Android-15-Basis, Reife offen |
| Work-Profile-Spezialisierung | Nein | Ja | Teil | Teil | Teil | Ja | Teil |
| sichtbarer HOME-Notausgang | Ja | Teil/Systemweg | Teil/Systemweg | Teil/Systemweg | Teil/Systemweg | Teil/Systemweg | Teil/Systemweg |
| Telefon-Gateway ohne Anrufrecht | Ja | Teil/Systemsuche | Nein | Nein | Nein | Teil/People | Nein |
| SAF-Datei-Gateway | Ja | Teil/System-Apps | Nein | Nein | Nein | Teil/Dateikontinuität | Nein |
| lokale Datei-Intelligenz | Ja/heuristisch | Nein | Nein | Nein | Nein | Nein | Nein |
| System-Kontrollzentrum | Ja | Teil/Android | Nein | Nein | Nein | Teil | Nein |
| lokale Sprachbefehle | Ja | Teil/Assistant | Teil/Gesten | Nein | Teil/Suche | Teil | Nein |
| ohne Konto/API funktionsfähig | Ja | Ja/Basis | Ja/Basis | Ja/Basis | Ja/Basis | Teil | Ja |
| deterministischer KI-Local-Core | Ja | Teil | Teil | Teil | Teil | Teil | Nein |
| externe KI-Zielauswahl | Ja | Nein | Nein | Nein | Nein | Teil/Copilot-Ökosystem | Nein |
| lokale Open-Source-Modellrouten | Ja | Nein | Nein | Nein | Nein | Nein | Nein |
| integriertes On-Device-LLM | Nein | Nein im Launcher | Nein | Nein | Nein | Nein | Nein |
| Preview vor KI-Layoutmutation | Ja | Nein | Nein | Nein | Nein | Nein | Nein |
| Read-only-Dateigrants lösbar | Ja | n/a | n/a | n/a | n/a | n/a | n/a |
| Reduced Motion | Ja/Systemwert | Ja | Ja | Ja | Ja | Ja | Teil |
| offener Launcher-Quellcode | Ja | Teil/AOSP-Basis | Nein | Nein | Nein | Nein | Ja |

## M2 → M2.1: nachgewiesener Fortschritt

| Kategorie | M2 | M2.1 | Delta | Warum |
|---|---:|---:|---:|---|
| Reduced Motion | 4,5 | 7,3 | +2,8 | statischer Neural-Glass-Fallback bei deaktivierten Systemanimationen |
| Ordner | 1,2 | 6,4 | +5,2 | lokale Kategorien, persistente Inhalte, Vorschau und Entfernen |
| Seiten und Dock | 2,9 | 6,6 | +3,7 | persistenter Smart Space, szenenadaptives Dock, Pin/Unpin |
| Notification Dots | 0,7 | 6,3 | +5,6 | expliziter Android-Opt-in, nur Paket und Anzahl, keine Textkopie |
| Backup/Migration | 2,7 | 4,7 | +2,0 | versioniertes Schema und JSON-sichere neue Collections; Export fehlt |
| Crash Recovery | 5,3 | 6,7 | +1,4 | Pending-Widget-ID-Restore, Shortcut-Request-Tokens, Grant-Verwaltung |
| Suchrelevanz | 6,5 | 7,2 | +0,7 | `E-Mail`, `E Mail` und kompakte Varianten sind bidirektional äquivalent |
| Kontextbewusstsein | 5,7 | 6,4 | +0,7 | Szene, Recency und Pinning steuern den Dock-Vorschlag lokal |

## Wo KoSch bereits führt

KoSch ist in 12 von 60 Kategorien allein oder geteilt vorne: Onboarding, sicherer Launcher-Wechsel, Telefon-Einstieg, sichere Dateiauswahl, Datei-Intelligenz, Offline-Kern, Betrieb ohne Konto/API, KI-Befehlsabdeckung, Grounding, Datenminimierung, Berechtigungsminimierung und Privacy-Kommunikation.

Das sind wertvolle Differenzierer, aber noch kein Freifahrtschein. Manche Führung entsteht, weil andere Produkte diese Systemfunktionen bewusst nicht als Launcher-Aufgabe behandeln. Für das Ziel „KI-Android unter dem Launcher“ ist genau diese Scope-Erweiterung jedoch relevant.

## Größte Rückstände

| Priorität | Kategorie | KoSch | Bestwert | Lücke |
|---:|---|---:|---:|---:|
| P0 | Kontakte-Integration | 1,2 | 8,0 | 6,8 |
| P0 | gemessene Laufzeitperformance | 3,8 | 9,6 | 5,8 |
| P0 | Wallpaper-/Theme-System | 4,3 | 9,6 | 5,3 |
| P0 | Lernen/Personalisierung | 3,2 | 8,5 | 5,3 |
| P0 | Lokalisierung | 4,2 | 9,5 | 5,3 |
| P0 | Observability/Audit | 1,8 | 7,0 | 5,2 |
| P0 | Produktionsreife | 4,7 | 9,6 | 4,9 |
| P0 | Widget Resize/Restore/Undo | 4,2 | 9,0 | 4,8 |
| P0 | Backup/Migration | 4,7 | 9,4 | 4,7 |
| P0 | Prompt-Injection-Abwehr | 4,5 | 9,0 | 4,5 |
| P1 | OEM-Kompatibilität | 4,4 | 8,5 | 4,1 |
| P1 | Testabdeckung | 5,7 | 9,5 | 3,8 |
| P1 | Accessibility | 5,6 | 9,1 | 3,5 |
| P1 | kleine Displays | 5,8 | 9,3 | 3,5 |
| P1 | Tablet/Foldable/Landscape | 4,9 | 8,3 | 3,4 |

## 25 simulierte Fachperspektiven

| Fachperspektive | KoSch Ø | Fachperspektive | KoSch Ø |
|---|---:|---|---:|
| Android Launcher Architect | 6,5 | Product Manager | 6,6 |
| Android Framework Engineer | 6,5 | Android Power User | 6,6 |
| Jetpack Compose Engineer | 6,5 | Accessibility User Advocate | 6,3 |
| Mobile UX Director | 6,6 | SAF/File Systems Expert | 6,5 |
| Visual Design Lead | 6,6 | Telephony Integration Engineer | 6,5 |
| Interaction Designer | 6,4 | Widget/Shortcut Expert | 6,4 |
| Accessibility Auditor | 6,3 | Mobile Performance Engineer | 6,3 |
| Privacy Engineer | 6,5 | Battery/Thermal Engineer | 6,4 |
| Mobile Security Engineer | 6,4 | QA Automation Lead | 6,3 |
| Applied AI Architect | 6,5 | Reliability/SRE Engineer | 6,3 |
| On-device ML Engineer | 6,4 | Google Play Policy Reviewer | 6,4 |
| LLM Safety Researcher | 6,4 | Competitive Product Analyst | 6,5 |
| Open-source Compliance Counsel | 6,5 | **Mittel** | **6,5** |

Die niedrigen Werte von Accessibility, Performance, QA und SRE sind plausibel: CI-Erfolg ersetzt weder Nutzerstudien noch Macrobenchmarks, Prozess-Tod-Tests oder ein OEM-Gerätelabor.

## Verbesserungsvorschläge für RUN M2.2

| Prio | Arbeitspaket | überprüfbares Akzeptanzkriterium | Zielkategorien |
|---:|---|---|---|
| P0 | Adaptive Shell | keine abgeschnittenen Controls bei 320 dp, 200-%-Schrift, Landscape, Tablet und Foldable; Screenshot-Tests | C06–C10 |
| P0 | Instrumentierte Systemtests | HOME, Prozess-Tod, SAF Grant/Replace/Release und Widget Pick/Configure/Cancel/Delete auf API 29/33/36 | C48, C51, C52, C58 |
| P0 | vollständige Widget-Engine | freie Platzierung, Resize, Provider-Restore-Mapping und transaktionales Undo | C20, C21 |
| P0 | Backup/Restore | exportierbarer, versionierter Snapshot; Dry-Run-Import; defekte/stale App- und Widget-IDs überleben | C24, C51 |
| P0 | Performancebudgets | Macrobenchmark für Cold Start, P95 Jank, RSS, Badge-Update und Akku; CI-Grenzwerte | C50, C53, C58 |
| P0 | lokales Audit | metadatenarmes Aktionsprotokoll mit Retention, Export/Löschen und ohne Prompt-/Notification-Inhalt | C45, C54, C55 |
| P1 | Launcher-Parität | beliebige manuelle Ordner, Rename, Drag/Drop, Dock-Reorder, frei erstellbare Seiten, Widget im Dock, Stacks | C16–C24 |
| P1 | Profile/Private Space | mehrere `UserHandle`, Work/Private/Archived Apps, gesperrte Profile ohne Datenleck | C11–C19, C41 |
| P1 | sichere Kontakte | Kontakte nur über Picker oder einzeln erteiltes Opt-in; ACTION_DIAL bleibt Bestätigungspunkt | C26, C27, C42 |
| P1 | isoliertes lokales LLM | separater Prozess/Service; GGUF-Import per SAF; Hash/Lizenz; Geräteprobe; stream/cancel/unload; Local-Core-Fallback | C34–C40, C50, C53 |
| P1 | strukturierter Capability Planner | strikt validiertes Schema, Allowlist, Risikoklassen, Preview/Confirm/Undo und negative Tool-Output-Evals | C36, C37, C40, C45, C46 |
| P2 | löschbare lokale Semantik | opt-in Embedding-Index für Apps/Shortcuts/gewählte Dateien; vollständiges Löschen und Rebuild | C13, C29, C38, C39 |
| P2 | Theme Program v1 | signiertes, capability-loses Theme-Paket mit Vorschau/Rollback; Wallpaper einschließlich PMDD nur als optionale Gestaltung | C01, C05, C23, C57 |
| P2 | Release Engineering | signierter Alpha-Track, SBOM, Dependency-/Secret-Scan, reproduzierbare Release-Artefakte, Crash-free Gate | C49, C58–C60 |

### Empfohlene Reihenfolge

1. Adaptive Shell + instrumentierte Tests + Performancebudgets.
2. Widget/Backup/Launcher-Parität.
3. Audit/Capability Planner.
4. Erst dann isoliertes lokales LLM und semantischer Index.
5. Theme-/Wallpaper-System nach stabiler Basisshell.

Ein integriertes LLM vor Prozess-, Accessibility- und Widget-Reife würde den Demo-Wert erhöhen, aber nicht den Launcher besser machen. Das Ziel für M2.2 sollte mindestens **7,4 allgemein**, **7,2 im 25-Rollen-Mittel**, kein Score unter 5,5 und grüne P0-Gates sein.

## Primärquellen

Die folgenden Hersteller-/Projektquellen wurden am Stichtag geprüft:

- Google: [At a Glance](https://support.google.com/pixelphone/answer/14884067?hl=en), [Private Space](https://support.google.com/pixelphone/answer/15341885?hl=en), [Android Notification Badges](https://developer.android.com/develop/ui/views/notifications/badges).
- Nova: [Featureübersicht](https://novalauncher.com/), [Nova 8.1 Beta/Nova Now](https://novalauncher.com/beta/), [FAQ](https://novalauncher.com/faq/), [Privacy](https://novalauncher.com/privacy/).
- Niagara: [Pro und Pop-ups](https://help.niagaralauncher.app/article/40-niagara-pro-features), [mehrere Widgets/Stacks](https://help.niagaralauncher.app/article/41-adding-multiple-widgets), [Notification Previews](https://help.niagaralauncher.app/article/131-enabling-notification-dots), [Media Widget](https://help.niagaralauncher.app/article/11-media-widget).
- Smart Launcher: [Versionsvergleich](https://docs.smartlauncher.net/faq/start-here/differences-between-versions), [6.6 Changelog](https://docs.smartlauncher.net/faq/changelog/6.6), [6.4 Changelog](https://docs.smartlauncher.net/faq/changelog/6.4), [responsive Widgets 6.2](https://docs.smartlauncher.net/faq/changelog/6.2).
- Microsoft: [Using Microsoft Launcher on Android](https://support.microsoft.com/en-us/office/using-microsoft-launcher-on-android), [Release Notes](https://support.microsoft.com/en-au/topic/microsoft-launcher-release-notes-707aeeac-9fd4-f2f1-c315-301d8ef74571).
- Lawnchair: [offizielles Repository](https://github.com/LawnchairLauncher/lawnchair), [Lawnchair 15 Beta 1](https://lawnchair.app/blog/lawnchair-15-beta-1/), [Beta-3-Release](https://github.com/LawnchairLauncher/lawnchair/releases/tag/v15.0.0-beta3.0), [Theming und Icons](https://docs.lawnchair.app/core-features/theming-and-icons).

## Professionelles Fazit

M2.1 ist der erste Stand, der sichtbar wie ein eigener Launcher und nicht nur wie eine KI-Demo argumentiert: Dock, Ordner, Seiten, Badges, Widgets, Apps, Telefon, Dateien und ein lokaler Command-Eingang bilden eine zusammenhängende Shell. Der Sicherheitsansatz ist ungewöhnlich stark. Trotzdem wäre die Behauptung „besser als jeder etablierte Launcher“ heute falsch. KoSch ist in ausgewählten System-/AI-/Privacy-Kategorien besser, im Gesamterlebnis aber noch 0,6 bis 1,5 Punkte zurück.

Der kürzeste Weg zur Führung ist nicht eine weitere Featureliste. Es sind belastbare Beweise: adaptive Bedienung, Widget-/Restore-Tiefe, reproduzierbares Backup, gemessene Performance, Geräteabdeckung und erst darauf ein isoliertes lokales LLM mit strengem Capability-System.
