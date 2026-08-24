# M2.5: strenger Professional-Launcher- und Expertenvergleich

Stand: 24. August 2026

## Urteil

**KoSch M2.5 erreicht 8,2 / 10,0 allgemein und 8,1 / 10,0 im Mittel von 25 simulierten Fachperspektiven.** In der 100-Kategorien-Matrix bleibt KoSch auf Rang 2 hinter Pixel/Android 17 und vor Microsoft Launcher.

Das Ziel **über 9,5** ist ausdrücklich **nicht erreicht**. M2.5 schließt konkrete Fehler- und Paritätslücken bei eigenen Ordnern, Dock-Reihenfolge, Arbeitsprofilen, Dateioperationen, Android-Systemübergaben und Smartpen-Notizen. Es ersetzt jedoch weder echte OEM-/Accessibility-/Performance-Labore noch die fehlende klassische Launcher-Tiefe bei Widgets, freien Seiten, Drag-and-drop, Gesten und Icon Packs.

## Methodik und Grenzen

- Vergleich: KoSch M2.5, Pixel/Android 17 als Systemreferenz sowie Nova 8/8.1 Beta, Niagara 1.x, Smart Launcher 6.6, Microsoft Launcher und Lawnchair 15 Beta 3.
- Skala: 0,1 = praktisch nicht vorhanden; 10,0 = nachweislich erstklassig und produktionsbewährt.
- 100 gleich gewichtete Kategorien, sieben Kandidaten und 25 Fachrollen.
- Rohdaten: [7 × 100 Vergleich](launcher_comparison_m2_5.csv), [25 × 100 KoSch-Fachmatrix](expert_scores_m2_5.csv), [25 Rollen × 7 Launcher](expert_launcher_overall_m2_5.csv) und [formatierte Arbeitsmappe](launcher_benchmark_m2_5.xlsx).
- Die 25 Rollen sind **simulierte, reproduzierbare Fachperspektiven**, keine interviewten Personen. Fokusbereiche werden je Rolle zusätzlich um 0,2 Punkte strenger bewertet.
- M2.4-Werte wurden nur dort verändert, wo der M2.5-Quellstand neue Evidenz liefert. Zehn neue Kategorien C91–C100 erfassen Ordner, Dock, Profilzielbindung, Quiet Mode, Datei-Erfolgssemantik, Navigationskontinuität, Systemübergaben, Systemnotiz, CI-Provenienz und Self-Service.
- KoSch-Codebasis: [PR #7](https://github.com/chekento/kosch-ai-android/pull/7) und [grüner CI-Lauf #35](https://github.com/chekento/kosch-ai-android/actions/runs/32724798845) mit Tests, Lint, Debug-/Release-Build, Permission-Budgets und APK-Prüfsumme.
- Es gab keinen identischen Sieben-Launcher-Labortest. Unbelegte Performance-, Akku-, OEM-, Accessibility- oder Recovery-Eigenschaften bleiben konservativ.
- Pixel/Android 17 umfasst Pixel Launcher und eng verbundene Systemflächen. Das ist eine anspruchsvolle Systemreferenz, kein reiner APK-gegen-APK-Test.
- Konkurrenzmerkmale stammen aus offiziellen Hersteller-/Projektquellen. Marketingaussagen erhalten ohne reproduzierbaren Labornachweis keine 10,0.

## Gesamtrangliste

| Rang | Launcher | Allgemein | 25 Rollen | Strenges Urteil |
|---:|---|---:|---:|---|
| 1 | Pixel / Android 17 | 8,4 | 8,4 | stärkste System-, Accessibility-, Performance- und Reifereferenz |
| 2 | **KoSch M2.5** | **8,2** | **8,1** | breiteste local-first Professional-/Privacy-Shell; weiter Alpha |
| 3 | Microsoft Launcher | 7,4 | 7,4 | etablierte Productivity-, Feed- und Work-Profile-Integration |
| 4 | Smart Launcher 6.6 | 7,0 | 6,9 | starke Organisation, Dock-, Widget- und Foldable-Funktionen |
| 5 | Nova 8 / 8.1 Beta | 6,9 | 6,9 | sehr starke klassische Anpassung, Gesten, Ordner und Backup |
| 6= | Niagara 1.x | 6,8 | 6,8 | außergewöhnlich klare Navigation, Pop-ups, Themes und Widget Stacks |
| 6= | Lawnchair 15 Beta 3 | 6,8 | 6,8 | offene Launcher3-/Pixel-Basis; 16er Linie bleibt Entwicklung |

Die Rangfolge ist eine Folge des breiten, professionellen Kriterienkatalogs. Eine reine Anpassungs- oder Minimalismus-Matrix würde Nova, Smart Launcher beziehungsweise Niagara stärker gewichten.

## Bereichsergebnis

| Bereich | KoSch | Pixel | Nova | Niagara | Smart | Microsoft | Lawnchair |
|---|---:|---:|---:|---:|---:|---:|---:|
| UX | 7,9 | 9,0 | 8,0 | 8,5 | 8,4 | 8,1 | 8,0 |
| Launcher | 8,1 | 8,5 | 8,8 | 8,0 | 8,7 | 8,2 | 8,0 |
| System | 8,5 | 8,4 | 3,7 | 3,4 | 3,7 | 6,0 | 3,9 |
| AI | 7,8 | 6,7 | 5,8 | 5,8 | 5,9 | 6,0 | 6,3 |
| Security | 8,9 | 8,6 | 6,7 | 6,7 | 6,8 | 7,5 | 7,0 |
| Engineering | 7,8 | 8,5 | 7,6 | 7,4 | 7,4 | 7,6 | 7,7 |
| Product | 8,5 | 8,6 | 7,7 | 8,6 | 8,5 | 8,5 | 7,4 |

KoSch führt im Mittel bei System, AI und Security. Diese Führung ist relevant, aber teilweise ein Scope-Effekt: KoSch übernimmt mehr sichere Gateway-Aufgaben als klassische Launcher. Bei Engineering liegt Pixel wegen Produktions-, Geräte- und Performanceevidenz weiter vorn.

## M2.4 → M2.5: belegter Fortschritt

| Kategorie | M2.4 | M2.5 | Delta | Begründung |
|---|---:|---:|---:|---|
| Ordner | 6,7 | 8,2 | +1,5 | eigene persistente Ordner, Rename, Add/Remove, Reorder und Grenzen |
| Mehrere Seiten und Dock | 7,6 | 8,2 | +0,6 | explizite Links-/Rechts-Reihenfolge für gepinnte Dock-Apps |
| KI-Befehlsabdeckung | 8,2 | 8,6 | +0,4 | Nachricht, Kalender, Wecker, Kamera und Systemnotiz |
| Systemeinstellungs-Abdeckung/Fallback | 9,0 | 9,4 | +0,4 | zusätzliche professionelle Android-Verträge und Fallbacks |
| Sichere App-Verwaltung | 8,8 | 9,1 | +0,3 | App-Info und Uninstall bleiben an das gewählte UserHandle gebunden |
| Dateioperationen/Undo | 8,8 | 9,3 | +0,5 | Mutationserfolg, Audit und Refresh sind getrennt und getestet |
| Testabdeckung | 7,6 | 8,0 | +0,4 | Collection- und Datei-Erfolgssemantik als reine Regressionstests |
| Release-/Sicherheitsgates | 8,4 | 8,8 | +0,4 | moderne Actions, offener Cache und APK-Permission-Gate |
| Wartbarkeit | 8,7 | 8,8 | +0,1 | wiederverwendbare pure Collection-/Mutation-Domänenregeln |
| Dokumentation | 8,4 | 8,7 | +0,3 | 57 In-App-FAQ-Einträge und vollständige M2.5-Grenzdokumentation |

## Funktionsvergleich M2.5

Legende: **Ja** = belegt vorhanden; **Teil** = begrenzt, systemabhängig oder anders gelöst; **Nein** = nicht als Launcher-Funktion belegt.

| Funktion | KoSch | Pixel | Nova | Niagara | Smart | Microsoft | Lawnchair |
|---|---|---|---|---|---|---|---|
| echte HOME-App | Ja | Ja | Ja | Ja | Ja | Ja | Ja |
| sofort ohne Konto/API | Ja | Ja/Basis | Ja | Ja | Ja | Teil | Ja |
| Pro-/Command-Center | Ja | Teil | Teil | Teil | Teil | Ja/Feed | Nein |
| lokaler deterministischer Action Planner | Ja | Teil | Teil | Teil | Teil | Teil | Nein |
| integriertes generatives Launcher-LLM | Nein | Nein | Nein | Nein | Nein | Nein | Nein |
| profilbewusster App-Katalog | Ja | Ja | Teil | Teil | Teil | Ja | Teil |
| Arbeitsprofil pausieren/aktivieren | Ja | Ja | Teil/System | Teil/System | Teil/System | Ja | Teil |
| profilbezogene App-Info/Deinstallation | Ja | Ja | Teil | Teil | Teil | Ja | Teil |
| lokale Count/Recency-Personalisierung mit Reset | Ja | Teil | Teil | Teil | Teil | Teil | Teil |
| steuerbare App-Sortierung / verborgene Apps | Ja | Ja | Ja | Ja | Ja | Ja | Ja |
| App-Shortcuts | Ja | Ja | Ja | Ja | Ja | Teil | Ja |
| eigene Ordner mit Rename/Reorder | Ja | Ja | Ja | Ja/Pop-ups | Ja | Ja | Ja |
| freie Home-Seiten/Drag-and-drop | Teil | Ja | Ja | Teil | Ja | Ja | Ja |
| Dock-Reorder | Ja/Pins | Ja | Ja | Teil | Ja | Ja | Ja |
| Widget Hosting | Ja/Board | Ja | Ja | Ja | Ja | Ja | Ja |
| Widget Stacks / Geräte-Restore | Nein | Teil | Ja | Ja | Ja | Teil | Teil |
| Telefon/SMS-Gateway ohne Direktrecht | Ja | Teil/System | Nein | Nein | Teil | Teil | Nein |
| Kalender/Wecker/Kamera-Systemwege | Ja | Ja/System | Teil | Teil | Teil | Ja | Teil |
| datensparsame Einzelkontaktwahl | Ja | Ja/System | Nein | Nein | Nein | Teil | Teil |
| begrenzter SAF-Datei-Arbeitsraum | Ja | Teil/System | Nein | Nein | Nein | Teil | Nein |
| bestätigtes Create/Rename/Delete + Rename-Undo | Ja | Teil/System | Nein | Nein | Nein | Teil | Nein |
| Mutationserfolg getrennt von Refresh | Ja | Teil/System | Nein | Nein | Nein | Teil | Nein |
| verschlüsseltes portables Workspace-Backup | Ja | Teil/System | Teil | Teil | Teil | Teil | Teil |
| metadatenarmes lokales Audit | Ja | Teil/System | Nein/belegt | Nein/belegt | Nein/belegt | Teil | Teil/Projekt |
| sichtbarer HOME-Sicherheitsausgang | Ja | Teil/System | Teil/System | Teil/System | Teil/System | Teil/System | Teil/System |
| adaptive Split-Shell / Foldable-Basis | Ja | Ja | Teil | Teil | Ja | Ja | Teil |
| Material-You-Dynamic-Color | Ja | Ja | Teil | Teil | Ja | Teil | Ja |
| Smartpen live erkannt | Ja | Ja/System | Nein | Nein | Nein | Nein | Teil |
| druckempfindlicher Pen Workspace | Ja | Teil/System-Apps | Nein | Nein | Nein | Nein | Nein |
| Android-Systemnotiz mit Stylus-Modus | Ja | Ja/System | Nein | Nein | Nein | Nein | Teil |
| lokale durchsuchbare In-App-FAQ | Ja/57 | Teil | Nein | Ja | Ja | Teil | Teil |
| offener Launcher-Quellcode | Ja | Teil/AOSP | Nein | Nein | Nein | Nein | Ja |

## Neue Kategorien C91–C100

| ID | Kategorie | KoSch | Bestwert | Leader | Lücke |
|---|---|---:|---:|---|---:|
| C91 | Manuelle Ordner-Lifecycle und Limits | 8,8 | 9,4 | Nova | 0,6 |
| C92 | Dock-Pin-Reihenfolge und Kontrolle | 8,6 | 9,5 | Nova | 0,9 |
| C93 | Profilbezogene App-Aktionen | 9,2 | 9,5 | Pixel | 0,3 |
| C94 | Arbeitsprofil-Pause und Reaktivierung | 8,9 | 9,6 | Pixel | 0,7 |
| C95 | Dateimutations-, Audit- und Refresh-Korrektheit | 9,3 | 9,3 | KoSch | 0,0 |
| C96 | Datei-Navigationskontinuität | 9,1 | 9,2 | Pixel | 0,1 |
| C97 | Professionelle Systemübergaben | 9,0 | 9,7 | Pixel | 0,7 |
| C98 | Stiftbewusste Systemnotiz | 8,8 | 9,5 | Pixel | 0,7 |
| C99 | CI-Provenienz und APK-Permission-Verifikation | 9,0 | 9,7 | Pixel | 0,7 |
| C100 | In-App-Self-Service-Abdeckung | 9,2 | 9,2 | KoSch | 0,0 |

## 25 simulierte Fachperspektiven

| Fachperspektive | KoSch M2.5 |
|---|---:|
| Android Launcher Architect | 8,0 |
| Android Framework Engineer | 8,1 |
| Jetpack Compose Engineer | 8,1 |
| Mobile UX Director | 8,2 |
| Visual Design Lead | 8,2 |
| Interaction Designer | 8,1 |
| Accessibility Auditor | 7,9 |
| Privacy Engineer | 8,2 |
| Mobile Security Engineer | 8,1 |
| Applied AI Architect | 8,1 |
| On-device ML Engineer | 8,0 |
| LLM Safety Researcher | 8,1 |
| Open-source Compliance Counsel | 8,2 |
| Product Manager | 8,2 |
| Android Power User | 8,2 |
| Accessibility User Advocate | 7,9 |
| SAF and File Systems Expert | 8,1 |
| Telephony Integration Engineer | 8,1 |
| Widget and Shortcut Expert | 8,0 |
| Mobile Performance Engineer | 7,9 |
| Battery and Thermal Engineer | 8,0 |
| QA Automation Lead | 7,9 |
| Reliability/SRE Engineer | 7,9 |
| Google Play Policy Reviewer | 8,1 |
| Competitive Product Analyst | 8,1 |

Die niedrigsten Rollen bleiben bei 7,9, weil Code- und Unit-Nachweise keinen realen Accessibility-, Performance- oder Reliability-Lab ersetzen. Keine Fachperspektive rechtfertigt 9,5.

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
| P0 | Widget Resize/Restore/Undo | 6,4 | 9,0 / Nova | 2,6 |
| P1 | Spracheingabe | 6,0 | 8,4 / Pixel | 2,4 |
| P1 | Prompt-Injection-Abwehr | 6,7 | 9,0 / Lawnchair | 2,3 |
| P1 | integriertes generatives Modell | 1,3 | 3,5 / Pixel/Microsoft | 2,2 |
| P1 | kleine Displays | 7,2 | 9,3 / Niagara | 2,1 |
| P1 | App-Shortcuts | 7,4 | 9,5 / Pixel | 2,1 |
| P1 | Widget Hosting | 7,2 | 9,3 / Smart Launcher | 2,1 |
| P1 | Workspace-Anpassung | 7,6 | 9,6 / Nova | 2,0 |

## Verbesserungsvorschläge für RUN M2.6

| Prio | Arbeitspaket | überprüfbares Akzeptanzkriterium |
|---:|---|---|
| P0 | Macrobenchmark und generiertes Baseline-Profil | Cold P95 ≤ 1.000 ms, Warm P95 ≤ 500 ms, Frame P95 ≤ 16,7 ms; Rohdaten/Geräteklasse versioniert |
| P0 | Accessibility-Lab | TalkBack, Switch, 200 %, Bold Text, Reduced Motion, 320 dp; keine blockierte/abgeschnittene Primäraktion |
| P0 | HOME-/OEM-/Lifecycle-Lab | Pixel/AOSP, Samsung, weiterer OEM; API 29/33/36/37; Neustart, Crash, Prozess-Tod und Launcher-Wechsel |
| P0 | SAF-Provider-Lab | lokale, Cloud- und OEM-Provider; Grant-Verlust, Mutation, Audit, Refresh und Abbruch |
| P0 | vollständige Widget-Engine | freie Platzierung, Stacks, Provider-Restore-Mapping; Pick/Configure/Cancel/Delete/Prozess-Tod instrumentiert |
| P0 | Launcher-Parität | freie Seiten/Raster, Drag-and-drop, Gesten und Icon Packs mit Undo und Accessibility |
| P0 | Lokalisierung | vollständiges Deutsch/Englisch; keine hardcodierten UI-Strings; RTL-/Plural-/Screenshot-Test |
| P0 | Release Engineering | signierter APK/AAB, SBOM, Dependency-/License-/Secret-Scan, reproduzierbarer Upgrade-/Rollback-Test |
| P1 | Notification-/Profile-Härtung | Personal/Work/Private, Pause/Lock, Multi-User und OEM-Service-Restart ohne Leaks |
| P1 | Smartpen-Gerätelabor | S Pen, USI, Pixel Pen, Bluetooth; AndroidX-Ink-Evaluation; Latenz P95 ≤ 25 ms |
| P1 | API-37-Upgrade | compile/target 37, nativer Contact Picker und API-37-Instrumentierung |
| P1 | isoliertes lokales LLM | erst nach den P0-Gates: separater Prozess, SAF-Modellimport, Hash/Lizenz, Geräteprobe, Stream/Cancel/Unload, Thermikgate |

## Primärquellen

- Android 17: https://developer.android.com/about/versions/17/
- Arbeitsprofil/Quiet Mode: https://developer.android.com/reference/android/os/UserManager
- Android Enterprise und Default-Launcher: https://developer.android.com/work/versions/android-9.0
- Intent-Verträge einschließlich Systemnotiz und Profilziel: https://developer.android.com/reference/android/content/Intent
- Nova: https://novalauncher.com/ und https://novalauncher.com/nova-8-1
- Niagara: https://help.niagaralauncher.app/article/40-niagara-pro-features
- Smart Launcher 6.6: https://docs.smartlauncher.net/faq/changelog/6.6
- Microsoft Launcher: https://support.microsoft.com/en-us/office/using-microsoft-launcher-on-android
- Lawnchair: https://github.com/LawnchairLauncher/lawnchair

## Schluss

M2.5 ist besser als M2.4, aber nicht „fertig“ und nicht pauschal besser als jeder einzelne Konkurrent. KoSch führt bei local-first Systemtiefe, Sicherheitsgrenzen und professioneller Orchestrierung; Pixel führt insgesamt, Nova/Smart Launcher führen in klassischer Anpassung, Niagara in fokussierter Bedienung und Microsoft in etablierter Konto-/Produktivitätsintegration. Der nächste echte Sprung muss aus Messdaten, Laborabnahme und Launcher-Parität kommen.
