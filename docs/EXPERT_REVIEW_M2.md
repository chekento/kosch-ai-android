# Strenges M2-Review

## Ergebnis

**Gesamturteil: 6.1 / 10.0 – technisch ernstzunehmende Alpha, nicht produktionsreif.**

KoSch M2 ist bereits mehr als ein UI-Mockup: HOME-Rolle, App-Start, Shortcuts, Widget-Host, SAF-Dateien, Dialer, Systemwege, Persistenz, lokaler Planner und CI sind real. Gegen reife Android-Launcher verliert der Build jedoch deutlich bei Ordnern, mehreren Seiten/Dock, Notification Dots, Restore, Accessibility, OEM-Tests und Betriebsreife. Gegen typische „AI Launcher“-Konzepte gewinnt er bei Local-first, Nutzerkontrolle und ehrlicher Sicherheitsarchitektur; ein integriertes generatives On-Device-Modell fehlt aber noch.

## Methodik und Einschränkung

Dies ist **kein tatsächlich einberufenes Panel aus 25 Menschen**. Es ist eine reproduzierbare, bewusst strenge Engineering-Heuristik aus 25 Fachrollen. Bewertet wurde der gebaute M2-Quellstand, die dokumentierten Vertrauensgrenzen und der Android-CI-Build. Es gab noch keine moderierte Nutzerstudie, kein physisches OEM-Gerätelabor, kein Accessibility-Audit und keine Performance-/Thermal-Messung. Unbelegte Reife erhält deshalb niedrige Werte.

- Skala: 0.1 = praktisch nicht vorhanden, 10.0 = nachweislich erstklassig und produktionsbewährt.
- 60 Kategorien × 25 Fachrollen = 1.500 Einzelwerte.
- Jede Rolle bewertet alle Kategorien; im eigenen Fachgebiet wird ein zusätzlicher Strengeabschlag angewendet.
- Der Gesamtscore ist der ungewichtete Mittelwert. Sicherheits- und Release-Gates können trotz gutem Mittelwert blockieren.
- Die vollständige Matrix steht in [expert_scores_m2.csv](expert_scores_m2.csv).

## Bereichsergebnis

| Bereich | Ø / 10.0 |
|---|---:|
| UX | 6.3 |
| Launcher | 5.2 |
| System | 5.9 |
| AI | 6.5 |
| Security | 7.9 |
| Engineering | 5.5 |
| Product | 6.0 |

## Vergleich mit einem reifen Standard-Launcher

| Dimension | KoSch M2 | Reifer Standard-Launcher | Urteil |
|---|---|---|---|
| HOME/App-Start | real und sauber angebunden | jahrelang OEM-getestet | KoSch funktional, aber ohne Felddaten |
| Erster Eindruck | geführtes Onboarding, Neural Glass, klare Local-first-Sprache | oft sofort vertraut, aber wenig erklärend | KoSch differenziert sich sichtbar |
| Grundfunktionen | Apps, Suche, Shortcuts, Widget-Board, Szenen | zusätzlich Ordner, Dock, Seiten, Badges, Restore | KoSch noch klar unvollständig |
| KI ohne API | Local Core sofort aktiv | meistens keine oder cloudgebundene KI | KoSch-Stärke, aber noch heuristisch |
| Generative KI | externe Übergaben, native Runtimes nur geplant | meist ebenfalls extern/keine | noch kein eigener Modellvorsprung |
| Datenschutz | minimales Permission-Budget, kein INTERNET | je Produkt unterschiedlich | KoSch strukturell stark |
| Accessibility | Basiskomponenten, keine formale Prüfung | bei großen OEMs breiter getestet | Release-Blocker für KoSch |
| Stabilität/OEM | CI-Build, keine Geräte-Matrix | breite Langzeiterfahrung | größter Reiferückstand |

## 25 Fachrollen

| # | Fachrolle | Gesamt |
|---:|---|---:|
| 01 | Android Launcher Architect | 6.1 |
| 02 | Android Framework Engineer | 6.1 |
| 03 | Jetpack Compose Engineer | 6.1 |
| 04 | Mobile UX Director | 6.4 |
| 05 | Visual Design Lead | 6.4 |
| 06 | Interaction Designer | 6.2 |
| 07 | Accessibility Auditor | 5.8 |
| 08 | Privacy Engineer | 6.1 |
| 09 | Mobile Security Engineer | 6.0 |
| 10 | Applied AI Architect | 6.0 |
| 11 | On-device ML Engineer | 5.9 |
| 12 | LLM Safety Researcher | 6.1 |
| 13 | Open-source Compliance Counsel | 6.2 |
| 14 | Product Manager | 6.3 |
| 15 | Android Power User | 6.4 |
| 16 | Accessibility User Advocate | 5.9 |
| 17 | SAF and File Systems Expert | 6.1 |
| 18 | Telephony Integration Engineer | 6.2 |
| 19 | Widget and Shortcut Expert | 6.0 |
| 20 | Mobile Performance Engineer | 5.9 |
| 21 | Battery and Thermal Engineer | 6.0 |
| 22 | QA Automation Lead | 5.9 |
| 23 | Reliability/SRE Engineer | 5.9 |
| 24 | Google Play Policy Reviewer | 6.1 |
| 25 | Competitive Product Analyst | 6.2 |

## 60 Kategorien

| ID | Kategorie | Bereich | Ø |
|---|---|---|---:|
| C01 | Erster visueller Eindruck | UX | 7.7 |
| C02 | Onboarding-Qualität | UX | 7.9 |
| C03 | Time-to-Value | UX | 7.5 |
| C04 | Interaktionsklarheit | UX | 7.0 |
| C05 | Visuelle Kohärenz | UX | 7.5 |
| C06 | Kleine Displays | UX | 5.9 |
| C07 | Tablet/Foldable/Landscape | UX | 4.7 |
| C08 | Semantische Accessibility | UX | 5.1 |
| C09 | Kontrast und Lesbarkeit | UX | 7.3 |
| C10 | Reduced Motion und Bewegung | UX | 4.5 |
| C11 | App-Entdeckung | Launcher | 7.1 |
| C12 | App-Start-Zuverlässigkeit | Launcher | 7.7 |
| C13 | Suchrelevanz | Launcher | 6.5 |
| C14 | Smart Collections | Launcher | 6.2 |
| C15 | Zuletzt verwendet | Launcher | 6.5 |
| C16 | Workspace-Anpassung | Launcher | 6.6 |
| C17 | Ordner | Launcher | 1.2 |
| C18 | Mehrere Seiten und Dock | Launcher | 2.9 |
| C19 | App-Shortcuts | Launcher | 6.7 |
| C20 | Widget Hosting | Launcher | 6.1 |
| C21 | Widget Resize/Restore/Undo | Launcher | 3.9 |
| C22 | Notification Dots/Badges | Launcher | 0.7 |
| C23 | Wallpaper- und Theme-System | Launcher | 4.2 |
| C24 | Backup und Migration | Launcher | 2.7 |
| C25 | Sicherer Launcher-Wechsel | Launcher | 8.8 |
| C26 | Telefon-Einstieg | System | 7.5 |
| C27 | Kontakte-Integration | System | 1.2 |
| C28 | Sichere Dateiauswahl | System | 8.6 |
| C29 | Nutzen der Datei-Intelligenz | System | 5.5 |
| C30 | System-Kontrollzentrum | System | 7.1 |
| C31 | Spracheingabe | System | 5.5 |
| C32 | Offline-Kern | AI | 9.2 |
| C33 | Ohne Konto oder API | AI | 9.4 |
| C34 | Lokale Open-Source-Auswahl | AI | 8.0 |
| C35 | Integriertes generatives Modell | AI | 1.3 |
| C36 | KI-Befehlsabdeckung | AI | 6.3 |
| C37 | Grounding von KI-Aktionen | AI | 7.9 |
| C38 | Kontextbewusstsein | AI | 5.7 |
| C39 | Lernen und Personalisierung | AI | 2.3 |
| C40 | Halluzinationsbegrenzung | AI | 8.6 |
| C41 | Datenminimierung | Security | 8.9 |
| C42 | Berechtigungsminimierung | Security | 9.1 |
| C43 | Secret-Vault-Grundlage | Security | 6.8 |
| C44 | Netzwerksicherheit | Security | 8.7 |
| C45 | Prompt-Injection-Abwehr | Security | 4.5 |
| C46 | Schutz vor destruktiven Aktionen | Security | 8.7 |
| C47 | Architektur und Modularität | Engineering | 7.1 |
| C48 | Testabdeckung | Engineering | 5.1 |
| C49 | Build-Reproduzierbarkeit | Engineering | 8.0 |
| C50 | Gemessene Laufzeitperformance | Engineering | 4.2 |
| C51 | Crash Recovery | Engineering | 5.3 |
| C52 | OEM-Kompatibilität | Engineering | 4.3 |
| C53 | Akku- und Energieeffizienz | Engineering | 6.5 |
| C54 | Observability und Audit | Engineering | 1.7 |
| C55 | Privacy-Kommunikation | Security | 8.4 |
| C56 | Lokalisierung | UX | 4.2 |
| C57 | Wettbewerbsdifferenzierung | Product | 7.4 |
| C58 | Produktionsreife | Product | 4.0 |
| C59 | Play-Policy-Readiness | Product | 6.7 |
| C60 | Wartbarkeit | Engineering | 6.9 |

## Stärken

1. **API-/kontofreier Start:** Local Core bleibt selbst ohne Netzwerk, Modell oder Dritt-App nützlich.
2. **Sicherheitsausgang:** Androids HOME-Auswahl ist prominent statt versteckt; kein Launcher-Lock-in.
3. **Berechtigungsdisziplin:** SAF, ACTION_DIAL, LauncherApps und System-Settings ersetzen gefährliche Vollrechte.
4. **Destruktionsschutz:** Dateinamens- und Layoutvorschläge sind Vorschauen; Telefonate bleiben Systemaktionen.
5. **Open-Source-Strategie:** PocketPal, ChatterUI, Maid sowie llama.cpp/LiteRT-LM/MLC sind sichtbar getrennt nach „nutzbar“, „Adapter“ und „Evaluation“.
6. **Ehrlichkeit:** Der regelbasierte Kern wird nicht fälschlich als generatives LLM vermarktet.

## Release-Blocker

1. Keine instrumentierten HOME-/Widget-/SAF-Tests auf echten oder virtuellen Android-Geräten.
2. Kein vollständiger Widget-Restore, Resize, freie Platzierung oder transaktionales Undo.
3. Ordner, mehrere Seiten, persistentes Dock und Notification Dots fehlen.
4. Kein getesteter State-/Prozess-Restore; Controller ist noch Activity-nah.
5. Accessibility nicht gegen TalkBack, Switch Access, 200-%-Schrift und Reduced Motion validiert.
6. Kein Geräte-/OEM-/Foldable-/Landscape-Labor.
7. Keine Startzeit-, Jank-, Akku-, Speicher- oder Thermalmessung.
8. Kein natives generatives Modell; damit ist der außergewöhnliche KI-Anspruch erst teilweise erfüllt.
9. Kein Audit Log oder Capability-System für künftige Agentenaktionen.
10. Keine externe Security-/Privacy-Prüfung.

## Verbesserungen für den nächsten Run

| Prio | Verbesserung | Akzeptanzkriterium | erwarteter Hebel |
|---:|---|---|---|
| P0 | State und Persistenz härten | versioniertes Schema, Migrationstest, Prozess-Tod/Restore ohne Verlust | Produktionsreife, Crash Recovery |
| P0 | Instrumentierte Systemtests | HOME, SAF-Cancel/Grant, Widget-Pick/Configure/Cancel/Delete auf API 29/33/36 | Zuverlässigkeit, OEM-Vertrauen |
| P0 | Accessibility-Pass | TalkBack-Flows, 48-dp-Ziele, 200-%-Schrift, Reduced Motion, Kontrastreport | UX und Inklusion |
| P1 | echte Launcher-Basis vervollständigen | Ordner, mehrere Seiten, Dock, Pinning, Widget Resize/Restore | Alltagsersatz für Standard-Launcher |
| P1 | isoliertes lokales LLM | GGUF-Import, Geräteprobe, stream/cancel/unload, Local-Core-Fallback | KI-Differenzierung |
| P1 | strukturierter Action Planner | JSON-Schema, Allowlist, Preview, Capability-Klasse, negative Evals | Agentensicherheit |
| P1 | semantische lokale Suche | löschbarer opt-in Index für Apps/Shortcuts/ausgewählte Dateien | Kernnutzen statt Chat-Gimmick |
| P2 | Performance Engineering | Baseline Profile; kalter Start, Jank, RSS, Akku/Thermal als CI-Budgets | Launcher-Gefühl und Stabilität |
| P2 | adaptive Layouts | kompakt, Tablet, Foldable, Landscape mit Screenshot-/Interaction-Tests | Geräteabdeckung |
| P2 | Audit und Erklärbarkeit | lokale Metadatenhistorie, Datenvorschau, Export/Löschen | Vertrauen und API-Bereitschaft |
| P3 | PMDD/Theme Designer | Asset-Vorschau, manuelles Setzen, Rollback, keine Diagnosebehauptung | emotionale Differenzierung |

## Professionelles Fazit

M2 ist als **Architektur- und Funktions-Alpha** überzeugend: Der Launcher tut bereits echte Launcher-Dinge und wählt für Telefon, Dateien und HOME-Wechsel die richtigen Android-Grenzen. Er ist noch kein vollwertiger Ersatz für Pixel/Nova/Niagara/One-UI-Klassen und kein „vollständiges KI-Android“. Der nächste Run sollte deshalb nicht mehr Features flach addieren, sondern drei Tiefenbeweise liefern: belastbarer Prozess-/Widget-Restore, geprüfte Accessibility/Performance und ein isoliertes kleines On-Device-LLM mit Local-Core-Fallback.

