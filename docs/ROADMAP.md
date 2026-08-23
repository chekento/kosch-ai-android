# Roadmap

Die Reihenfolge folgt Vertrauensgrenzen. Ein Feature gilt erst als fertig, wenn Abbruch, Fehlerfall, Restore und Nutzerkontrolle mitgebaut sind.

## M1 – Launcher Vertical Slice

- [x] HOME-Rolle und App-Start
- [x] lokaler App-Index ohne breite Paketberechtigung
- [x] freie, persistente Kartenpositionen
- [x] PLAY/EDIT und fünf Szenen
- [x] lokaler Command Planner und App-Suche
- [x] explizite KI-Provider-Auswahl
- [x] lokale Kontextvorschläge
- [x] Layout-Vorschau, Anwenden/Verwerfen und Undo
- [x] Begleiter als ehrlicher Sprach-Einstieg
- [x] CI, Tests und APK-Artefakt

## M2 – Local-first System Shell

- [x] professionelles Onboarding und local-first Erklärung
- [x] jederzeit erreichbare Android-HOME-Auswahl als Sicherheitsausgang
- [x] Telefon über `ACTION_DIAL` ohne Anrufrecht
- [x] SAF-Dateiauswahl und begrenzte lokale Datei-Intelligenz
- [x] System-Kontrollzentrum
- [x] App-Shortcuts über `LauncherApps`
- [x] Widget-Auswahl, Provider-Konfiguration, Hosting, Persistenz und Löschen
- [x] Open-Source-App-Routen: PocketPal, ChatterUI, Maid
- [x] Registry für llama.cpp, LiteRT-LM und MLC LLM
- [x] ruhender Android-Keystore-Vault und Endpoint-Policy
- [x] code-nativer Neural-Glass-Hintergrund
- [ ] Widget-Resize, freie Platzierung, Restore-Mapping und Undo
- [x] lokale Smart-Ordner, zwei Home-Räume, adaptives App-Dock und Pinning
- [x] opt-in Notification Dots ohne Inhaltskopie
- [ ] vollständige Accessibility- und OEM-Gerätetests

## M2.1 – Resilient Smart Home

- [x] Persistenzschema mit Version und JSON-sicheren Collections
- [x] Pending-Widget-ID über Activity-Recreation erhalten
- [x] verspätete Shortcut-Ergebnisse bei Auswahlwechsel verwerfen
- [x] persistierte Dokumentfreigabe ersetzen und lösen
- [x] Suchnormalisierung für Trennzeichen/Leerzeichen härten
- [x] lokale Smart-Ordner mit Preview/Apply/Discard
- [x] szenenadaptives Dock mit Pin/Unpin
- [x] Reduced-Motion-Fallback
- [x] 7-Launcher-/60-Kategorien- und 25-Rollen-Review

## Nächster Lauf – M2.2 Messbare Reife und lokales Modell

- [ ] Migrationstests und vollständiger Prozess-Tod-/State-Restore
- [ ] ViewModel/StateFlow statt Activity-gebundener Controller-Lebensdauer
- [ ] instrumentierte Tests für HOME, SAF, Widget-Abbruch und Konfiguration
- [ ] TalkBack, Switch Access, Schrift 200 %, Reduced Motion und Kontrastprüfung
- [ ] Foldable/Tablet-/Landscape-Layouts
- [ ] `LocalModelBackend` in isolierter Service-/Prozessgrenze
- [ ] Geräteprobe und optionaler kleiner GGUF-Modell-Pack über llama.cpp
- [ ] Modellimport per SAF, Lizenzanzeige, Hashprüfung, Load/Cancel/Unload
- [ ] lokale Zusammenfassung und strukturierte Befehle mit Schema-Validator
- [ ] lokaler Embedding-Index für Apps, Shortcuts und explizit gewählte Dateien
- [ ] manuelle Ordner/Seiten, Drag/Drop, Dock-Reorder, Widget-Stacks und Export/Import
- [ ] Notification Dots für Channels, Multi-User, Work/Private Profile und OEM-Restart härten
- [ ] Macrobenchmark-/Baseline-Profile-Budgets für Start, Jank, RSS, Akku und Badge-Updates
- [ ] lokales Audit mit Retention, Export und vollständigem Löschen
- [ ] Expert-Review-Gaps aus `COMPETITOR_REVIEW_M2_1.md` schließen

## M3 – Sichere Agenten und optionale APIs

- [ ] capability-basierter Action Planner
- [ ] Kontextvorschau vor jeder Modell-/Providerübertragung
- [ ] getrennter Netzwerk-Flavour mit granularer Provider-Aktivierung
- [ ] Vault-UI, Löschung, Rotation und optionale Geräteauthentifizierung
- [ ] lokales Audit Log, Export und Retention
- [ ] Prompt-Injection-/Tool-Output-Evals
- [ ] Notification Triage jeweils opt-in
- [ ] Kalender-/Task-Integrationen jeweils opt-in
- [ ] Rule Engine mit Dry Run, Risikoklassen und Undo

## M4 – Workspace Engine und persönliche KI

- [ ] Ebenen, Z-Order, Skalierung, Rotation, Gruppen und Portale
- [ ] vollständige Action Matrix pro Objekt
- [ ] Creator-Inspektor und persistenter Undo/Redo-Stack
- [ ] persönlicher verschlüsselter Memory-Vault
- [ ] lokale STT/TTS-Abstraktion und unterbrechbare Dialoge
- [ ] adaptive Accessibility- und Fokusprofile
- [ ] Theme-/Wallpaper-Designer einschließlich PMDD-gestützter Vorschläge

## M5 – Theme Programs

- [ ] deklaratives Theme-/Layout-Paketformat
- [ ] Sandbox, Signatur und Capability-Manifest
- [ ] KI-generierte Themes mit Vorschau und Rollback
- [ ] Neural Glass, Minimal Work, Living AI und Cyberdeck
- [ ] LCARS als optionales Theme – nicht als Kernabhängigkeit
