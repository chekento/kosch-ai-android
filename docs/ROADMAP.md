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
- [x] Widget-Reihenfolge und Undo
- [ ] freie Widget-Platzierung, Stacks und Restore-Mapping
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

## M2.4 – Evidence & Resilience

- [x] Controller-Lebensdauer in `LauncherViewModel` aus der Activity lösen
- [x] Backup-/Audit-/SVG-Export über privaten No-Backup-Payload und Saved-State-Einmaltoken wiederaufnehmbar machen
- [x] Pending-Export auf 8 MiB, einen Konsum, 24 Stunden, Typ- und Pfadvalidierung begrenzen
- [x] alte UserHandle-basierte App-Schlüssel eindeutig auf stabile User-Seriennummern migrieren
- [x] lokale App-Startsignale mit 512er-Limit, transparenter Verwendung und vollständigem Reset
- [x] Workspace-Schema v6 mit begrenzten Lernsignalen und verborgenen Apps im validierten Backup
- [x] App-Raum-Sortierungen Smart, A–Z, Häufig und Zuletzt
- [x] lokal verborgene Apps mit eigener Verwaltungsansicht
- [x] App-Aktionsraum um Store, Ordner, Sichtbarkeit und Android-Deinstallationsdialog erweitern
- [x] einen user-selektierten SAF-Datei-Arbeitsraum ohne breite Speicherrechte integrieren
- [x] Datei-Metadatenanalyse, lokale Suche/Sortierung, Create, Rename mit Undo und bestätigtes Delete
- [x] zusätzliche Android-Systemwege für Hintergrund, Anzeige, Ton, Akku, Datenschutz, Accessibility, Standard-Apps und Speicher
- [x] Widget-Reihenfolge und gültigkeitsgeprüftes Undo
- [x] Pen-SVG-Export und endpoint-erhaltendes Resampling langer Striche
- [x] Accessibility-Semantik für App-/Dock-Kacheln und Pen-Custom-Actions
- [x] mehr als 50 lokale FAQ-Einträge einschließlich aller M2.4-Grenzen
- [x] Unit-Tests für Pending Export, Migration, lokales Ranking, Datei-Planung und Pen-Integrität
- [x] CI #30 mit Tests, Lint, Debug-/Release-Build, Permission-Budget und APK-Prüfsumme grün
- [x] 7-Launcher-/90-Kategorien-Review, 25×90-Fachmatrix und formatierte XLSX-Arbeitsmappe

## M2.5 – Professional Parity & Correctness

- [x] eigene persistente Ordner mit Create/Rename/Add/Remove/Reorder und 12×32-Grenzen
- [x] Dock-Pin-Reihenfolge links/rechts steuerbar machen
- [x] App-Info und System-Uninstall an das ausgewählte UserHandle binden
- [x] Arbeitsprofil über Android Quiet Mode pausieren/aktivieren und pausierte Starts abweisen
- [x] Nachricht, Kalender, Wecker und Kamera als sichere Android-Systemübergaben
- [x] Android-14+-Systemnotiz mit Stylus-Modus und Pen-Space-Fallback
- [x] Datei-Mutation, Audit und Refresh als getrennte Ergebnisse modellieren und testen
- [x] eingereichte Dateiänderungen auch nach Schließen der Fläche auditieren
- [x] Datei-Refresh im aktuellen Verzeichnis statt an der Tree-Wurzel
- [x] Shortcut-Schlüssel auf stabile User-Seriennummern umstellen
- [x] 57 lokale FAQ-Einträge einschließlich aller M2.5-Grenzen
- [x] GitHub Actions modernisieren und Quell-/APK-Permission-Budget prüfen
- [x] 7-Launcher-/100-Kategorien-Review, 25×100-Fachmatrix und formatierte XLSX-Arbeitsmappe

## Nächster Lauf – M2.6 Measured Professional Beta

- [ ] instrumentierte Tests für HOME, SAF, Widget-Abbruch/-Konfiguration und Stifteingabe auf API 29/33/36/37
- [ ] Prozess-Tod-/Saved-State-Testmatrix während Backup-, Audit-, SVG-, Kontakt-, Widget- und Tree-Picker
- [ ] TalkBack, Switch Access, Hardwaretastatur, Schrift 200 %, Reduced Motion und Kontrast auf realen Geräten
- [ ] Foldable-Hinge-, Multi-Window-, 320-dp- und Screenshot-Golden-Matrix
- [ ] SAF-Provider-Lab für lokale, Cloud- und OEM-Provider einschließlich Grant-Verlust und Mutations-/Refresh-Semantik
- [ ] vollständige Widget-Engine: freie Platzierung, Stacks und Restore-Mapping
- [ ] vollständige Launcher-Parität: freie Seiten/Raster, Drag/Drop, Icon-Packs und Gesten
- [ ] Private-Space-Container erst nach Hide/Show/Lock/Unlock und Leak-Tests
- [ ] nativer API-37-Mehrfeld-Contact-Picker nach Upgrade auf compileSdk/targetSdk 37
- [ ] Pen-Latenz-Benchmark, Historical Events, Geräte-Lab und AndroidX-Ink-Evaluation
- [ ] Macrobenchmark-Budgets für Start, Jank, App-Raum, SAF, RSS, Akku, Ink und Badge-Updates
- [ ] reproduzierbare Release-Signierung, SBOM, Dependency-/License-Scan und Upgrade/Rollback
- [ ] unabhängiges Privacy-/Security-Review von Backup, Audit, SAF und lokalen Lernsignalen
- [ ] vollständige Lokalisierung Deutsch/Englisch, RTL-/Plural-Tests und Diagnoseexport ohne Nutzdaten
- [ ] erst hinter diesen Gates: `LocalModelBackend` in isolierter Service-/Prozessgrenze
- [ ] Geräteprobe, lizenzgeprüfter GGUF-Import, Hashprüfung, Load/Cancel/Unload und Thermikschutz
- [ ] lokale Zusammenfassung und strukturierte Befehle mit Schema-Validator
- [ ] lokaler, je Quelle löschbarer Embedding-Index für Apps, Shortcuts und explizit gewählte Dateien
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
