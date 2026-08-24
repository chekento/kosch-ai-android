# M2.5 Professional Parity & Correctness Quality Gates

Stand: 24. August 2026

Ein Zielwert über 9,5 ist kein Designlabel, sondern ein Release-Gate. Der [grüne GitHub-Actions-Lauf #35](https://github.com/chekento/kosch-ai-android/actions/runs/32724798845) erfüllt Tests, Lint, Debug-/Release-Build sowie Quell- und APK-Permission-Budget des M2.5-Code-Stands. Die reproduzierbare Neubewertung erreicht **8,2 allgemein** und **8,1 im Mittel von 25 simulierten Fachperspektiven** über 100 Kategorien. Manuelle Beta-Gates bleiben offen; 9,5 ist daher ausdrücklich nicht erfüllt.

## Automatisierte Repository-Gates

| Gate | Kriterium | Nachweis |
|---|---|---|
| Local-first | als `uses-permission` ausschließlich `ACCESS_NETWORK_STATE` | CI-Permission-Budget |
| Build | Debug- und minifizierter Release-Build kompilieren | `assembleDebug assembleRelease` |
| Static quality | Android Lint ohne Fehler | `lintDebug` |
| Regression | alle lokalen Unit-Tests grün | `testDebugUnitTest` |
| Backup confidentiality | Klartext nicht im Envelope sichtbar | `PortableBackupCodecTest` |
| Backup integrity | falsche Passphrase und manipuliertes GCM-Ciphertext abgelehnt | `PortableBackupCodecTest` |
| Export-Handoff | Typbindung, Einmal-Konsum, Größenlimit, Ablauf und Traversal-Schutz | `PendingDocumentStoreTest` |
| App-Key-Migration | alte profilgebundene Schlüssel nur eindeutig migriert | `AppKeyMigrationTest` |
| Local ranking | Count/Recency, Limits und stabile Reihenfolge | `LocalUsageModelTest` |
| Datei-Arbeitsraum | Namensregeln, Kategorien, Größen, Duplikate und größte Dateien | `LocalFileWorkspacePlannerTest` |
| Datei-Erfolgssemantik | Mutationserfolg bleibt von Refreshfehler getrennt | `FileMutationSemanticsTest` |
| Workspace Collections | Ordnerlimits, Idempotenz, Entfernen und Reorder | `WorkspaceCollectionEditorTest` |
| Ink-Integrität | lange Striche behalten Anfang/Ende; SVG bleibt begrenzt | `InkStrokeNormalizerTest`, `InkSvgExporterTest` |
| Capability safety | sensible/destruktive Aktionen können Bestätigung nicht verlieren | `CapabilityPolicyTest` |
| Audit privacy | CSV besitzt nur Zeitpunkt, Aktion und Ergebnis | `AuditCsvTest` |
| Keyboard | Ctrl-/Meta-Routing einschließlich Datei-Arbeitsraum | `ProfessionalShortcutResolverTest` |
| FAQ | mindestens 57 Einträge und M2.5-Profil-/Datei-/Systemthemen | `FaqRegistryTest` |
| Static contrast | zentrale Fallback-Textpaare mindestens 4,5:1 | `WcagContrastTest` |
| Startup profile | startkritischer Pfad als ART-Baseline-Profil vorhanden | CI prüft nichtleere `baseline-prof.txt` |
| Packaged permission | erzeugte APK enthält kein `INTERNET`-Recht | CI prüft `aapt dump permissions` |
| Supply artifact | APK vorhanden und SHA-256 erzeugt | CI-Artefakt `kosch-ai-launcher-m2.5-debug` |

## Was der grüne Lauf nicht beweist

Unit-Tests und Compiler prüfen reine Regeln und Integrationsgrenzen, aber keine reale OEM-UI, keinen DocumentsProvider, kein TalkBack-Verhalten, keine Widget-App eines Drittanbieters und keine Stiftlatenz. Deshalb bleiben folgende Abnahmen offen:

| Bereich | Mindestmatrix | M2.5-Status |
|---|---|---|
| HOME-Recovery | Pixel/AOSP, Samsung, weiterer OEM; Neustart, Crash, HOME-Wechsel | offen |
| Lifecycle | Rotation, Fold, Multi-Window und erzwungener Prozess-Tod in offenem Picker/Sheet | quellseitig gehärtet, instrumentiert offen |
| Displays | 320 dp, Phone Portrait/Landscape, 8–11″ Tablet, Foldable/Hinge, Desktop Windowing | offen |
| Eingabe | Touch, Maus, Trackpad, Tastatur, S Pen, USI, generischer Bluetooth-Stift | offen |
| Accessibility | TalkBack, Switch Access, 200 % Schrift, Bold Text, Reduced Motion, Farbkorrektur | Semantik ergänzt, Lab offen |
| SAF | AOSP Files, Google Drive/Cloud, Samsung/weiterer OEM; Create/Rename/Delete/Grant-Verlust | offen |
| Backup | Prozess-Tod, 8-MiB-Grenze, drei Provider, Gerätewechsel, Profilkonflikte | unit-getestet, Gerät offen |
| Kontakte | API 29/33/36 Legacy-Picker und API 37 System-Picker; Personal/Work | offen |
| Widgets | Auswahl, Konfiguration, Abbruch, Reihenfolge/Undo, Provider-Removal, Neustart | offen |
| Profile | Personal/Work locked, unlocked, paused; Migrations- und Badge-Kollision | unit-/quellseitig, Gerät offen |
| Performance | Cold/Warm/Hot Start, Frame Jank, RSS, Akku, Pen-Latenz | offen |
| Security | unabhängiger Review, SBOM/Dependency-Scan, Restore-/SAF-Fuzzing | offen |
| Release | reproduzierbar signierter APK/AAB, Upgrade-/Rollback-Test, Play-Policy-Review | offen |
| Sprache | vollständiges Deutsch/Englisch, RTL, Plural- und Abschneidetests | offen |

## Verbindliche Performance-Budgets für den nächsten Lauf

- Cold Start P50 ≤ 650 ms und P95 ≤ 1.000 ms auf definierter Mittelklasse;
- Warm Start P95 ≤ 500 ms;
- Frame Time P95 ≤ 16,7 ms und P99 ≤ 33,3 ms für Standardnavigation;
- App-Raum mit 1.000 Einträgen: Such-/Sortierreaktion P95 ≤ 50 ms außerhalb Renderzeit;
- SAF-Liste mit 500 Einträgen: erste verwertbare Darstellung P95 ≤ 750 ms nach Provider-Antwort;
- Pen Input-to-Render P95 ≤ 25 ms auf unterstützter Hardware;
- keine blockierende Modellinitialisierung im HOME-Prozess;
- idle CPU nahe 0 %, keine periodische Netzarbeit;
- dokumentiertes RSS- und Akku-Budget mit und ohne optionalen Modellprozess.

## 9,5-Entscheidungsregel

Eine Gesamtwertung über 9,5 ist nur zulässig, wenn gleichzeitig:

1. alle automatisierten Gates grün sind;
2. alle manuellen Beta-Gates bestanden sind;
3. keine der 100+ Kategorien unter 8,5 liegt;
4. Security, Accessibility, Reliability, Startleistung und HOME-Recovery jeweils mindestens 9,5 erreichen;
5. Messdaten, Geräteklassen, Build-SHA und Methodik gespeichert sind;
6. mindestens ein unabhängiger Security-/Privacy-Review und ein Accessibility-Review ohne kritischen Befund vorliegen.

Bis dahin ist 8,2/8,1 der ehrliche M2.5-Befund – kein Anlass, Bewertungszahlen zu manipulieren.
