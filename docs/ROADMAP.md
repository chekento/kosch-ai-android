# Roadmap

Die Reihenfolge folgt technischen Vertrauensgrenzen, nicht einer möglichst langen Featureliste.

## M1 – Launcher Vertical Slice

- [x] HOME-Rolle und App-Start
- [x] lokaler App-Index ohne breite Paketberechtigung
- [x] freie, persistente Kartenpositionen
- [x] PLAY/EDIT und fünf Szenen
- [x] lokaler Command Planner und App-Suche
- [x] explizite KI-Provider-Auswahl
- [x] lokale Kontextvorschläge
- [x] Layout-Vorschau, Verwerfen, Anwenden und Undo
- [x] Begleiter als ehrlicher Sprach-Einstieg
- [x] CI, Tests und APK-Artefakt

## M2 – Echter Workspace

- [ ] persistentes Datenmodell mit Versionen und Migrationen
- [ ] Ebenen, Z-Order, Skalierung, Rotation und Gruppen
- [ ] mehrere Seiten, Dock, Ordner und Portale
- [ ] vollständige Action Matrix pro Objekt
- [ ] Undo/Redo-Stack und Creator-Inspektor
- [ ] App-Shortcuts über `LauncherApps`
- [ ] Widget-Auswahl, Bindung, Hosting, Restore und Löschen

## M3 – AI Core

- [ ] Capability-basierter Provider Router
- [ ] lokales/on-device Modell als No-Key-Option
- [ ] verschlüsselter Provider-Credential-Vault
- [ ] kontextuelle Datenvorschau vor jeder Übertragung
- [ ] strukturierter Action Planner mit Schema-Validierung
- [ ] AI Layout Designer auf derselben Preview/Apply/Undo-Schiene
- [ ] Audit Log und Evals für gefährliche Aktionen

## M4 – Automation und Begleiter

- [ ] Rule Engine mit Zeit-, Akku-, Audio- und Netztriggern
- [ ] Kalender, Ort und Benachrichtigungsmetadaten jeweils opt-in
- [ ] STT/TTS-Abstraktion und unterbrechbare Dialoge
- [ ] Begleiterzustände, die reale Systemzustände abbilden
- [ ] Bestätigungsklassen für reversible und irreversible Aktionen

## M5 – Theme Programs

- [ ] deklaratives Theme-/Layout-Paketformat
- [ ] Sandbox und Signaturprüfung
- [ ] KI-generierte Themes mit Vorschau
- [ ] Neural Glass, Minimal Work, Living AI und Cyberdeck
- [ ] LCARS als optionales Theme – nicht als Kernabhängigkeit

