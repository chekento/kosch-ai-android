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
- [x] Widget-Größenpresets und Provider-Size-Update
- [ ] freie Widget-Platzierung, Stacks, Restore-Mapping und Undo
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

## M2.2 – Adaptive Pen Space

- [x] adaptive kompakte/geteilte Shell für breite, Landscape-, Tablet- und Foldable-Fenster
- [x] Material-You-Dynamic-Color auf Android 12+ bei erhaltener kontrastreicher Fallback-Palette
- [x] generische Smartpen-Erkennung über `InputManager` mit Live-Gerätewechsel
- [x] Druck, Neigung, Orientierung, Hover, Stylus-/Eraser-Werkzeug und Stifttasten erfassen
- [x] zusätzlicher Pen Space mit Stift, Marker, Radierer, Hover, Undo, Clear und lokalem Autosave
- [x] Workspace-Schema v3 mit begrenzten, normalisierten Vektorstrichen
- [x] Fingerkontakte auf Pen Space ignorieren; Android-IME-Handschrift ehrlich als Systemfähigkeit behandeln
- [x] profilbewusster App-Katalog mit stabilen User-Schlüsseln, Work-Labels und gebadgten Icons
- [x] `ACCESS_HIDDEN_PROFILES` bis zum vollständigen Private-Space-Container bewusst nicht anfordern
- [x] 32 Einträge umfassende lokale, kategorisierte und durchsuchbare In-App-FAQ
- [x] FAQ und Pen Space im Local Command Planner und Kontrollzentrum verankern
- [x] 7-Launcher-/65-Kategorien-Review, 25×65-Fachmatrix und formatierte XLSX-Benchmarkmappe
- [x] Unit-Test/Lint/Debug-APK in GitHub Actions grün

## M2.3 – Professional Command Center

- [x] Pro Desk als professioneller Standardbereich für Neuinstallationen
- [x] Hardware-Tastatur-Shortcuts, Android-Shortcut-Hilfe und Escape-Recovery
- [x] sichere Einmalkontaktauswahl ohne `READ_CONTACTS`, mit Android-17-System-Picker-Hinweis
- [x] verschlüsselter Workspace-Export via PBKDF2-HMAC-SHA-256 und AES-256-GCM
- [x] Restore-Dry-Run, strikte Schema-/Limitvalidierung, zweite Bestätigung und atomarer Commit
- [x] gerätegebundene Widget-IDs, URI-Grants, Secrets, Notification-Daten und Audit vom Backup ausschließen
- [x] metadatenarmes lokales Audit mit 90-Tage-/250-Event-Grenze, CSV-Export und vollständigem Löschen
- [x] Widget-Größenpresets Kompakt/Standard/Hoch persistieren und Provider informieren
- [x] Professional-Befehle im API-freien Local Command Planner
- [x] Workspace-Schema v5 und mindestens 40 lokale FAQ-Einträge
- [x] Unit-Tests für Backup-Roundtrip, falsche Passphrase, Manipulation, Audit-CSV und Shortcut-Routing
- [x] CI, Lint, Debug-/Release-Build und Debug-APK-Artefakt dieses finalen M2.3-Stands grün
- [x] M2.3-Konkurrenz-/25-Rollen-Neubewertung in 75 Kategorien und formatierte Arbeitsmappe

## Nächster Lauf – M2.4 Nachweisbare Spitzenreife

- [ ] Migrationstests v1–v3 und vollständiger Prozess-Tod-/State-Restore einschließlich Ink
- [ ] ViewModel/StateFlow statt Activity-gebundener Controller-Lebensdauer
- [ ] instrumentierte Tests für HOME, SAF, Widget-Abbruch/-Konfiguration und Stifteingabe auf API 29/33/36/37
- [ ] TalkBack, Switch Access, Hardwaretastatur, Schrift 200 %, Reduced Motion und Kontrastprüfung
- [ ] Foldable-Hinge-, Multi-Window-, 320-dp- und Screenshot-Testmatrix
- [ ] vollständige Widget-Engine: freie Platzierung, Stacks, Restore-Mapping und Undo
- [ ] instrumentierte Backup-/Restore-Tests mit Prozess-Tod, falschem Provider und Profil-/App-Konflikten
- [ ] nativer API-37-Mehrfeld-Contact-Picker nach Upgrade auf compileSdk/targetSdk 37
- [ ] Pen-Latenz-Benchmark, Historical Events, Geräte-Lab und AndroidX-Ink-Evaluation
- [ ] `LocalModelBackend` in isolierter Service-/Prozessgrenze
- [ ] Geräteprobe und optionaler kleiner GGUF-Modell-Pack über llama.cpp
- [ ] Modellimport per SAF, Lizenzanzeige, Hashprüfung, Load/Cancel/Unload
- [ ] lokale Zusammenfassung und strukturierte Befehle mit Schema-Validator
- [ ] lokaler Embedding-Index für Apps, Shortcuts und explizit gewählte Dateien
- [ ] manuelle Ordner/Seiten, Drag/Drop, Dock-Reorder und Icon-Pack-/Gesten-Parität
- [ ] Notification Dots für Multi-User, Work/Private Profile und OEM-Restart härten
- [ ] Macrobenchmark-/Baseline-Profile-Budgets für Start, Jank, RSS, Akku, Ink und Badge-Updates
- [ ] unabhängiges Privacy-/Security-Review von Backup und Audit
- [ ] Lokalisierung Deutsch/Englisch, RTL-/Plural-Tests und Diagnoseexport ohne Nutzdaten
- [ ] 9,5-Gate erst nach Messwerten, Accessibility-/OEM-Lab und geschlossenen Führungs-Gaps neu prüfen

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
