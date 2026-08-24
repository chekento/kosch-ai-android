# M2.3: strenger Professional-Launcher- und Expertenvergleich

Stand: 24. August 2026

## Urteil

**KoSch M2.3 erreicht 7,6 / 10,0 allgemein und 7,4 / 10,0 im Mittel der 25 simulierten Fachperspektiven.** Gegenüber M2.2 sind das jeweils +0,6 Punkte. KoSch steigt damit im veröffentlichten Allgemeinwert auf Rang 2 hinter Pixel/Android 17 und vor Microsoft Launcher.

Das Ziel **über 9,5** ist ausdrücklich **nicht erreicht**. M2.3 besitzt jetzt einen professionell nutzbaren Alpha-Kern mit Pro Desk, Hardware-Tastatur, datensparsamer Kontaktauswahl, verschlüsseltem Restore-Workflow, lokalem Audit, Capability-Policy und Widget-Größenpresets. Für eine seriöse Spitzenwertung fehlen weiterhin gemessene Laufzeitperformance, Accessibility- und OEM-Gerätelabor, vollständige Widget-/Launcher-Parität, Lokalisierung, Private Space, ein isoliertes lokales generatives Modell und Produktionsnachweise.

## Methodik und Grenzen

- Vergleich: KoSch M2.3, Pixel/Android 17 als aktuelle Systemreferenz sowie Nova 8/8.1 Beta, Niagara 1.x, Smart Launcher 6.6, Microsoft Launcher und Lawnchair 15 Beta 3.
- Skala: 0,1 = praktisch nicht vorhanden; 10,0 = nachweislich erstklassig und produktionsbewährt.
- 75 gleich gewichtete Kategorien, sieben Kandidaten und 25 Fachrollen. Die vollständigen Rohdaten stehen in [launcher_comparison_m2_3.csv](launcher_comparison_m2_3.csv), [expert_scores_m2_3.csv](expert_scores_m2_3.csv), [expert_launcher_overall_m2_3.csv](expert_launcher_overall_m2_3.csv) und [launcher_benchmark_m2_3.xlsx](launcher_benchmark_m2_3.xlsx).
- Die 25 Rollen sind **simulierte, reproduzierbare Fachperspektiven**, keine interviewten Personen. Jede Rolle besitzt 75 KoSch-Einzelwerte. Ihre dokumentierte Rollenstrenge liegt zwischen −0,3 und +0,2.
- Bestehende M2.2-Rollenwerte wurden ausschließlich um das nachweisbare Delta der jeweiligen KoSch-Kategorie fortgeschrieben. Zehn neue Kategorien wurden mit derselben Rollenstrenge bewertet.
- KoSch wurde anhand des Quellstands und des grünen GitHub-Actions-Laufs #23 bewertet. Dieser umfasst Unit-Tests, Android Lint, Debug- und minifizierten Release-Build, Manifest-Permission-Gate und APK-Artefakt.
- Es gab keinen identischen Sieben-Launcher-Test auf derselben Gerätefarm. Unbelegte Performance-, OEM-, Accessibility- oder Recovery-Eigenschaften bleiben deshalb niedrig.
- Pixel/Android 17 umfasst Pixel Launcher und eng verbundene Android-HOME-/Systemflächen. Das ist bewusst eine anspruchsvolle Systemreferenz, kein reiner APK-gegen-APK-Vergleich.
- Konkurrenzwerte beruhen auf aktuellen offiziellen Hersteller-/Projektquellen. Marketingaussagen ohne reproduzierbaren Laborbeleg werden nicht als 10,0 gewertet.

## Gesamtrangliste

| Rang | Launcher | Allgemein | 25 Rollen | Strenges Urteil |
|---:|---|---:|---:|---|
| 1 | Pixel / Android 17 | 8,2 | 8,2 | Referenz für Systemintegration, Performance, Accessibility und Reife |
| 2 | **KoSch M2.3** | **7,6** | **7,4** | stärkste Local-first-/Privacy-System-Shell, noch Alpha ohne Gerätelabor |
| 3 | Microsoft Launcher | 7,3 | 7,3 | stärkste etablierte Productivity-/Feed-/Work-Profile-Breite |
| 4= | Smart Launcher 6.6 | 7,1 | 7,1 | sehr tiefe Organisation, Dock-, Widget- und Foldable-Funktionen |
| 4= | Nova 8 / 8.1 Beta | 7,1 | 7,1 | sehr starke klassische Anpassung, Gesten und Backup |
| 4= | Lawnchair 15 Beta 3 | 7,1 | 7,1 | offene Launcher3-/Pixel-Basis; aktuelle 16er Linie bleibt Entwicklung |
| 7 | Niagara 1.x | 7,0 | 7,0 | außergewöhnliche Klarheit, Pop-ups, Themes und Widget Stacks |

Die Gleichstände beruhen auf der veröffentlichten Genauigkeit von einer Dezimalstelle. Die Arbeitsmappe enthält alle Einzelwerte und Formeln.

## Bereichsergebnis

| Bereich | KoSch | Pixel | Nova | Niagara | Smart | Microsoft | Lawnchair |
|---|---:|---:|---:|---:|---:|---:|---:|
| UX | 7,7 | 8,9 | 8,0 | 8,5 | 8,4 | 8,1 | 8,0 |
| Launcher | 7,4 | 8,4 | 8,6 | 8,0 | 8,5 | 8,0 | 7,8 |
| System | 7,7 | 7,7 | 3,0 | 2,8 | 2,9 | 5,4 | 3,3 |
| AI | 7,0 | 6,6 | 5,9 | 6,0 | 6,1 | 5,9 | 6,8 |
| Security | 8,5 | 8,4 | 7,3 | 7,3 | 7,4 | 7,4 | 7,9 |
| Engineering | 7,1 | 8,3 | 7,4 | 7,3 | 7,3 | 7,5 | 7,6 |
| Product | 7,9 | 8,6 | 7,9 | 8,7 | 8,5 | 8,6 | 7,2 |

KoSch erreicht erstmals die Systemreferenz im gerundeten Systemmittel und führt das Security-Mittel. Im klassischen Launcher-Bereich bleibt es jedoch 1,2 Punkte hinter Nova. Die Engineering-Lücke von 1,2 Punkten zu Pixel zeigt, dass grüne CI noch keine Produktionsreife ist.

## M2.2 → M2.3: nachgewiesener Fortschritt

| Kategorie | M2.2 | M2.3 | Delta | Begründung |
|---|---:|---:|---:|---|
| Kontakte-Integration | 1,2 | 6,8 | +5,6 | einmalige Telefonzeile, kein `READ_CONTACTS`, Android-17-Picker bevorzugt, keine Persistenz |
| Observability und Audit | 1,8 | 7,8 | +6,0 | geschlossenes Drei-Feld-Schema, 90 Tage/250 Events, CSV und Double-confirm-Clear |
| Backup und Migration | 5,2 | 7,9 | +2,7 | PBKDF2/AES-GCM, versionierter Envelope, Vorschau, Validierung und atomarer Restore |
| Prompt-Injection-Abwehr | 4,5 | 6,5 | +2,0 | geschlossene Capability-Tabelle verhindert vom Modell erfundene Rechte |
| Widget Resize/Restore/Undo | 4,2 | 5,6 | +1,4 | drei persistierte Größenpresets und Provider-Size-Update; Stacks/Restore/Undo fehlen |
| Testabdeckung | 5,9 | 6,9 | +1,0 | Backup-, Tamper-, Audit-, Capability-, Keyboard- und Kontrasttests |
| Build-Reproduzierbarkeit | 8,0 | 8,6 | +0,6 | Debug und Release, Lint, Tests, Permission Budget und Baseline-Profil-Gate |
| Berechtigungsminimierung | 8,9 | 9,5 | +0,6 | CI erlaubt als Manifest-Recht ausschließlich `ACCESS_NETWORK_STATE` |
| KI-Befehlsabdeckung | 6,9 | 7,4 | +0,5 | Pro Desk, Kontakt, Backup und Audit offline im Planner |
| Produktionsreife | 4,9 | 5,3 | +0,4 | Release-Build grün; Geräte-, Accessibility- und Performance-Gates bleiben offen |

Die zehn neuen Kategorien C66–C75 erweitern die Messlatte um Professional Command Center, Tastatur, datensparsame Kontaktaktion, Backup-Vertraulichkeit, Restore-Kontrolle, lokales Audit, Capability-Gates, Widget-Größenvertrag, Shortcut-Recovery und automatisierte Release-Gates. Sie sind keine Bonussektion: Konkurrenten können darin ebenfalls führen.

## M2.3-Funktionsvergleich

Legende: **Ja** = vorhanden; **Teil** = begrenzt, systemabhängig oder anders gelöst; **Nein** = nicht als Launcher-Funktion belegt.

| Funktion | KoSch | Pixel | Nova | Niagara | Smart | Microsoft | Lawnchair |
|---|---|---|---|---|---|---|---|
| echte HOME-App | Ja | Ja | Ja | Ja | Ja | Ja | Ja |
| sofort ohne Konto/API | Ja | Ja/Basis | Ja/Basis | Ja/Basis | Ja/Basis | Teil | Ja |
| Pro-/Command-Center | Ja | Teil | Teil | Teil | Teil | Ja/Feed | Nein |
| lokale deterministische KI-Schicht | Ja | Teil | Teil | Teil | Teil | Teil | Nein |
| integriertes generatives Launcher-LLM | Nein | Nein | Nein | Nein | Nein | Nein | Nein |
| profilbewusster App-Katalog | Ja/Basis | Ja | Teil | Teil | Teil | Ja | Teil |
| App-Shortcuts | Ja | Ja | Ja | Ja | Ja | Teil | Ja |
| Hardware-Shortcut-Hilfe | Ja | Ja | Teil | Teil | Teil | Teil | Teil |
| lokale Smart-Ordner mit Vorschau | Ja | Nein | Teil | Nein | Ja | Teil | Teil |
| manuelle Ordner/Drag-and-drop | Teil | Ja | Ja | Ja/Pop-ups | Ja | Ja | Ja |
| Widget Hosting | Ja/Board | Ja | Ja | Ja | Ja | Ja | Ja |
| Widget-Größenpresets | Ja/3 | Ja | Ja | Ja | Ja | Ja | Ja |
| Widget Stacks/Restore/Undo | Nein | Teil | Ja | Ja | Ja | Teil | Teil |
| Telefon-Gateway ohne Anrufrecht | Ja | Teil | Nein | Nein | Teil | Teil | Nein |
| datensparsame Einzelkontaktwahl | Ja | Ja/System | Nein | Nein | Nein | Teil | Teil/Suche |
| SAF-Datei-Gateway | Ja | Teil/System-App | Nein | Nein | Nein | Teil | Nein |
| lokale Datei-Hinweise | Ja/heuristisch | Nein | Nein | Nein | Nein | Nein | Nein |
| verschlüsseltes portables Workspace-Backup | Ja | Teil/System | Teil | Teil | Teil | Teil | Teil |
| Restore-Dry-Run und zweite Bestätigung | Ja | Teil | Teil | Teil | Teil | Teil | Nein/belegt |
| metadatenarmes lokales Audit | Ja | Teil/System | Nein/belegt | Nein/belegt | Nein/belegt | Teil | Teil/Projekt |
| sichtbare Capability-Risikopolicy | Ja | Teil/System | Nein | Nein | Nein | Nein | Nein |
| sichtbarer HOME-Sicherheitsausgang | Ja | Teil/Systemweg | Teil/Systemweg | Teil/Systemweg | Teil/Systemweg | Teil/Systemweg | Teil/Systemweg |
| adaptive Split-Shell | Ja | Ja | Teil | Teil | Ja | Ja | Teil |
| Material-You-Dynamic-Color | Ja | Ja | Teil | Teil | Ja | Teil | Ja |
| Smartpen live erkannt | Ja | Ja/System | Nein | Nein | Nein | Nein | Teil/Systembasis |
| druckempfindlicher Pen Workspace | Ja | Teil/System-Apps | Nein | Nein | Nein | Nein | Nein |
| lokale durchsuchbare In-App-FAQ | Ja | Teil | Nein | Ja | Ja | Teil | Teil |
| offener Launcher-Quellcode | Ja | Teil/AOSP | Nein | Nein | Nein | Nein | Ja |

## Neue Professional-Kategorien C66–C75

| ID | Kategorie | KoSch | Bestwert | Leader | Lücke |
|---|---|---:|---:|---|---:|
| C66 | Professionelles Command Center | 8,8 | 9,0 | Microsoft | 0,2 |
| C67 | Hardware-Tastatur-Produktivität | 8,6 | 8,8 | Pixel | 0,2 |
| C68 | Datensparsame Kontaktaktion | 9,2 | 9,2 | KoSch | 0,0 |
| C69 | Backup-Vertraulichkeit und Integrität | 8,8 | 8,8 | KoSch | 0,0 |
| C70 | Restore-Vorschau, Validierung und Kontrolle | 8,7 | 8,8 | Nova | 0,1 |
| C71 | Metadatenarmes lokales Audit | 8,2 | 8,2 | KoSch | 0,0 |
| C72 | Capability-Policy und Risikogates | 8,5 | 9,0 | Pixel | 0,5 |
| C73 | Widget-Größenvertrag und Presets | 7,5 | 9,2 | Pixel | 1,7 |
| C74 | Shortcut-Discoverability und Recovery | 8,8 | 8,8 | KoSch | 0,0 |
| C75 | Automatisierte Release- und Sicherheitsgates | 7,6 | 9,5 | Pixel | 1,9 |

## 25 simulierte Fachperspektiven

| Fachperspektive | KoSch M2.3 |
|---|---:|
| Android Launcher Architect | 7,4 |
| Android Framework Engineer | 7,4 |
| Jetpack Compose Engineer | 7,4 |
| Mobile UX Director | 7,6 |
| Visual Design Lead | 7,5 |
| Interaction Designer | 7,4 |
| Accessibility Auditor | 7,2 |
| Privacy Engineer | 7,5 |
| Mobile Security Engineer | 7,4 |
| Applied AI Architect | 7,4 |
| On-device ML Engineer | 7,3 |
| LLM Safety Researcher | 7,4 |
| Open-source Compliance Counsel | 7,5 |
| Product Manager | 7,5 |
| Android Power User | 7,5 |
| Accessibility User Advocate | 7,3 |
| SAF and File Systems Expert | 7,5 |
| Telephony Integration Engineer | 7,5 |
| Widget and Shortcut Expert | 7,3 |
| Mobile Performance Engineer | 7,3 |
| Battery and Thermal Engineer | 7,3 |
| QA Automation Lead | 7,3 |
| Reliability/SRE Engineer | 7,3 |
| Google Play Policy Reviewer | 7,4 |
| Competitive Product Analyst | 7,4 |

Accessibility bleibt mit 7,2 die strengste Perspektive. Die höchste Rollenwertung von 7,6 stammt aus der UX-Sicht, weil Pro Desk und Tastaturführung die professionelle Bedienung sichtbar verbessern. Keine Rolle rechtfertigt auch nur annähernd 9,5.

## Wo KoSch führt

KoSch liegt in 21 von 75 Kategorien allein oder geteilt vorn. Besonders belastbar sind:

- API-freier Offline-Kern und Betrieb ohne Konto;
- Daten- und Berechtigungsminimierung;
- sichere HOME-Auswahl und explizite System-Gateways;
- SAF-Dateiauswahl und lokale Datei-Hinweise;
- integrierter Pen Workspace;
- datensparsame Kontaktaktion;
- verschlüsselter portabler Backup-Envelope;
- metadatenarmes lokales Audit;
- Shortcut-Discoverability und Recovery.

Ein Teil der Führung entsteht aus dem breiteren System-Scope. Das ist für das Produktziel relevant, ersetzt aber nicht die tiefe Launcher-Mechanik von Nova, Smart Launcher, Niagara oder Pixel.

## Größte Rückstände

| Prio | Kategorie | KoSch | Bestwert/Leader | Lücke |
|---:|---|---:|---|---:|
| P0 | gemessene Laufzeitperformance | 4,2 | 9,6 / Pixel | 5,4 |
| P0 | Lernen und Personalisierung | 3,2 | 8,5 / Microsoft | 5,3 |
| P0 | Lokalisierung | 4,2 | 9,5 / Pixel | 5,3 |
| P0 | Produktionsreife | 5,3 | 9,6 / Pixel | 4,3 |
| P0 | Wallpaper-/Theme-System | 5,8 | 9,6 / Nova | 3,8 |
| P0 | Widget Resize/Restore/Undo | 5,6 | 9,0 / Nova | 3,4 |
| P0 | OEM-Kompatibilität | 5,2 | 8,5 / Pixel | 3,3 |
| P1 | Ordner | 6,4 | 9,3 / Nova | 2,9 |
| P1 | Notification Dots/Badges | 6,3 | 9,2 / Pixel/Niagara | 2,9 |
| P1 | Akku- und Energieeffizienz | 6,3 | 9,1 / Pixel | 2,8 |
| P1 | Widget Hosting | 6,7 | 9,3 / Smart | 2,6 |
| P1 | Testabdeckung | 6,9 | 9,5 / Pixel | 2,6 |
| P1 | Prompt-Injection-Abwehr | 6,5 | 9,0 / Lawnchair | 2,5 |
| P1 | semantische Accessibility | 6,7 | 9,1 / Pixel | 2,4 |
| P1 | Spracheingabe | 6,0 | 8,4 / Pixel | 2,4 |

Der niedrigste absolute KoSch-Wert bleibt das integrierte generative Modell mit 1,3. Diese Lücke erscheint nicht oben, weil auch die Vergleichslauncher kein echtes integriertes Launcher-LLM belegen. Für KoSchs eigenes Produktversprechen bleibt sie dennoch zentral.

## Verbesserungsvorschläge für RUN M2.4

| Prio | Arbeitspaket | überprüfbares Akzeptanzkriterium | Hauptkategorien |
|---:|---|---|---|
| P0 | Macrobenchmark und generiertes Baseline-Profil | Cold Start P95 ≤ 1.000 ms, Warm P95 ≤ 500 ms, Frame P95 ≤ 16,7 ms; Rohdaten und Geräteklasse versioniert | C50, C53, C58, C75 |
| P0 | Accessibility-Abnahme | TalkBack, Switch Access, 200-%-Schrift, Bold Text, Reduced Motion und 320 dp; keine blockierte oder abgeschnittene Primäraktion | C06–C10, C67, C74 |
| P0 | HOME-/OEM-Recovery-Lab | Pixel/AOSP, Samsung und weiterer OEM auf API 29/33/36/37; Neustart, Crash, Prozess-Tod und Launcher-Wechsel bestanden | C12, C25, C48, C51, C52, C58 |
| P0 | vollständige Widget-Engine | freie Platzierung, Stacks, Provider-Restore-Mapping und transaktionales Undo; Pick/Configure/Cancel/Delete/Prozesstod instrumentiert | C20, C21, C73 |
| P0 | Lokalisierung | Deutsch und Englisch vollständig; keine hardcodierten User-Strings; Plural-, RTL- und Screenshot-Test | C56, C65, C75 |
| P0 | sichere Personalisierung | lokaler, erklärbarer Embedding-/Preference-Index mit Opt-in, Reset, Export und Löschung | C13, C14, C38, C39, C41 |
| P1 | isoliertes lokales LLM | separater Prozess; SAF-Modellimport, Hash/Lizenz, Geräteprobe, Stream/Cancel/Unload, RAM-/Thermikgate und Local-Core-Fallback | C34–C40, C50, C53 |
| P1 | native API-37-Kontaktauswahl | Mehrfeldauswahl mit Session-Lebenszyklus; Legacy-Routen auf API 29/33/36; Work-/Personal-Testmatrix | C26, C27, C42, C68 |
| P1 | Theme Program v1 | capability-loses, signiertes Paket mit Material-Tokens, Icon-/Wallpaper-Vorschau und atomarem Rollback; LCARS optional | C01, C05, C23, C57 |
| P1 | Launcher-Parität | manuelle Ordner/Seiten, Drag-and-drop, Dock-Reorder, Icon Packs und Gesten mit Undo und Accessibility | C14–C24 |
| P1 | Security-Verifikation | Restore-Fuzzing, unabhängiges Backup-/Audit-/Threat-Model-Review, SBOM, Dependency- und Secret-Scan | C41–C46, C49, C59, C69–C72, C75 |
| P1 | Smartpen-Gerätelabor | S Pen, USI, Pixel Pen und Bluetooth-Stift; Historical Events/Jetpack Ink evaluieren; Ink-Latenz P95 ≤ 25 ms | C50, C52, C61–C64 |

## 9,5-Gate

Eine Bewertung über 9,5 ist erst zulässig, wenn gleichzeitig:

1. alle automatisierten CI-Gates grün sind;
2. alle manuellen Beta-Gates in `QUALITY_GATES.md` bestanden sind;
3. keine der 75 Kategorien unter 8,5 liegt;
4. Security, Accessibility, Reliability, Startleistung und HOME-Recovery jeweils mindestens 9,5 erreichen;
5. Messdaten, Geräteklassen, Testprotokolle und Bewertungsformeln im Repository auditierbar sind.

M2.3 erfüllt Punkt 1, aber nicht 2 bis 5. Eine Selbsteinstufung über 9,5 wäre daher fachlich falsch.

## Aktuelle Primärquellen

### Android und Systemreferenz

- [Android 17](https://developer.android.com/about/versions/17/), [Contact Picker](https://developer.android.com/about/versions/17/features/contact-picker) und [`Intent`-Referenz](https://developer.android.com/reference/android/content/Intent).
- [Hardware-Keyboard](https://developer.android.com/develop/ui/views/touch-and-input/keyboard-input), [Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files) und [Android-Kryptografie](https://developer.android.com/privacy-and-security/cryptography).
- [App Widgets](https://developer.android.com/reference/android/appwidget/AppWidgetProviderInfo), [Startup Profiles](https://developer.android.com/topic/performance/startupprofiles/dex-layout-optimizations), [Baseline Profiles](https://developer.android.com/develop/ui/compose/performance/baseline-profiles) und [Macrobenchmark](https://developer.android.com/codelabs/android-macrobenchmark-inspect/).
- [Android Stylus](https://developer.android.com/develop/ui/views/touch-and-input/stylus-input), [Adaptive Apps](https://developer.android.com/develop/adaptive-apps/guides/adaptive-dos-and-donts), [`LauncherApps`](https://developer.android.com/reference/kotlin/android/content/pm/LauncherApps.html) und [Private-Space-Pflichten](https://developer.android.com/about/versions/15/behavior-changes-all).

### Konkurrenzprodukte

- Nova: [Featureübersicht](https://novalauncher.com/) und [Beta-Changelog](https://novalauncher.com/beta/).
- Niagara: [Pro-Funktionen](https://help.niagaralauncher.app/article/40-niagara-pro-features), zuletzt am 25. Juni 2026 aktualisiert.
- Smart Launcher: [6.6-Changelog](https://docs.smartlauncher.net/faq/changelog/6.6), [Versionsvergleich](https://docs.smartlauncher.net/faq/start-here/differences-between-versions) und [Backup-Gerätewechsel](https://docs.smartlauncher.net/faq/move-smart-launcher-to-a-new-phone).
- Microsoft: [Using Microsoft Launcher on Android](https://support.microsoft.com/en-us/office/using-microsoft-launcher-on-android).
- Lawnchair: [offizielles Repository](https://github.com/LawnchairLauncher/lawnchair); Lawnchair 16 ist Entwicklung, für reguläre Nutzer wird dort weiterhin 15 Beta 3 empfohlen.

## Professionelles Fazit

M2.3 ist der erste Stand, den man als **Professional Command Center Alpha** verteidigen kann. Die wichtigsten neuen Workflows sind nicht nur sichtbare Karten: Sie besitzen Datenschutzgrenzen, Fehlerpfade, Vorschau, Bestätigung, Persistenzregeln und Tests. KoSch ist dadurch in Security, Systemnähe und API-freiem Betrieb eigenständiger als die Referenzlauncher.

Der harte Befund bleibt: Rang 2 in dieser breiten Matrix ist nicht gleichbedeutend mit dem zweitbesten Launcher für jeden Alltag. Pixel, Nova, Smart Launcher, Niagara und Microsoft sind KoSch bei Reife, Tiefe oder Spezialdisziplinen weiterhin deutlich voraus. Der Weg zu 9,5 führt ab jetzt primär über Messung, Accessibility, OEM-Recovery, Widget-/Launcher-Parität und Release Engineering – nicht über weitere Demo-Flächen.
