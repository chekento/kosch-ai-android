# KAL Release Quality Gates

Stand: 29. August 2026 · Version `0.2.5-alpha01`

Ein Zielwert über 9,5 ist kein Designlabel, sondern ein Release-Gate. Die letzte vollständig dokumentierte breite M2.5-Bewertung erreicht **8,2 allgemein** und **8,1 im Mittel von 25 simulierten Fachperspektiven** über 100 Kategorien. Der aktuelle Deep-Integration-Branch erweitert den Funktionsumfang erheblich; diese historische Bewertung wird dadurch **nicht automatisch** angehoben. Neue Bewertungswerte benötigen neue Mess- und Geräte-Evidenz.

## Automatisierte Repository-Gates

| Gate | Kriterium | Nachweis |
|---|---|---|
| Local-first | Basisfunktionen funktionieren ohne Konto, API-Key oder Modell-Download | Unit-/Instrumentation-Verträge + Produktarchitektur |
| Network boundary | `INTERNET` ist nur Paket-Capability; direkte AI-Provider-Nutzung bleibt standardmäßig AUS und verlangt Provider-Verbindung + Privacy/Cloud-Gates + Vordergrundaktion | `KalCloudAccessPolicyTest`, Provider-Tests, CI-Permission-Budget |
| Background AI | direkte Provider-Anfragen aus `BACKGROUND` werden abgelehnt | `KalCloudAccessPolicyTest` |
| Provider credential safety | Tokens/Keys liegen im Keystore-gestützten Vault und sind nicht Teil portabler Backups | Credential-/Backup-Tests |
| OAuth | OpenRouter nutzt PKCE und einen einmaligen Loopback-Callback; kein wiederverwendbares Client-Secret im APK | `OpenRouterOAuthProtocolTest` + Source Review |
| Sensitive observation | Screen/Camera sind getrennte Opt-ins; kein `RECORD_AUDIO`, Standort-, Kontakte-, Call-Log-, SMS-, Phone-State- oder `QUERY_ALL_PACKAGES`-Recht | Instrumentation + CI-Permission-Budget |
| Screen share | MediaProjection-Service ist nicht exportiert und korrekt als `mediaProjection`-Foreground-Service typisiert | `AssistantObservationManifestInstrumentationTest` |
| VPN isolation | N1 bleibt inert/debug-only; Release-APK enthält weder `KoSchConsentVpnService` noch `SecurityNetworkActivity` | `VpnConsentContractInstrumentationTest` + packaged-release gate |
| Build | Debug- und minifizierter Release-Build kompilieren | `assembleDebug assembleRelease` |
| Instrumentation package | Debug + AndroidTest APK werden reproduzierbar erzeugt | `assembleDebugAndroidTest` |
| Static quality | Android Lint ohne Fehler | `lintDebug` |
| Regression | alle lokalen Unit-Tests grün | `testDebugUnitTest` |
| API-36 device regression | Managed-Device-Instrumentation auf AOSP API 36 | `pixel2Api36DebugAndroidTest` |
| Backup confidentiality | Klartext/Secrets und gerätegebundene Grants nicht unkontrolliert im portablen Bundle | Backup-/Portable-Policy-Tests |
| Backup integrity | falsche Passphrase/manipuliertes GCM-Ciphertext bzw. ungültige Restore-Daten werden abgelehnt | Backup-Tests |
| Export-Handoff | Typbindung, Einmal-Konsum, Größenlimit, Ablauf und Traversal-Schutz | `PendingDocumentStoreTest` und verwandte Tests |
| App-Key/Profile migration | profilgebundene Schlüssel bleiben kollisionsarm und Migrationen kontrolliert | App-/Settings-Migrationstests |
| Local ranking | Count/Recency, Limits und stabile Reihenfolge | Local Usage / Search tests |
| Workspace editing | Page-/Grid-/Drag-/Resize-/Compact-Regeln deterministisch, mit kontrolliertem Undo/Reflow | Workspace model + instrumentation tests |
| Settings scopes | Global → Page → Object bleibt explizit; `inherit default` und Codec/Migrationen sind getestet | Settings model/store/instrumentation tests |
| Widgets | Host ownership, Workspace-v7-Binding, Recovery/Remap und Stacks besitzen Regressionstests | Widget unit/instrumentation tests |
| AI handoff | externe Übergabe, Kontextauswahl und Transportwechsel bleiben confirmation-first | AI Context Handoff tests |
| Capability safety | sensible/destruktive Aktionen können Bestätigung nicht verlieren | Capability-/execution-policy tests |
| Audit privacy | Audit besitzt keinen freien Prompt-/Detailkanal | Audit tests |
| Keyboard/Input | Keyboard-, Pen-, Gesture- und adaptive Input-Routen besitzen kontrollierte Fallbacks | Input/gesture tests |
| Static contrast/accessibility | zentrale Kontrast- und Semantikregeln bleiben testbar | WCAG-/Instrumentationtests |
| Startup profile | startkritischer Pfad als ART-Baseline-Profil vorhanden | CI prüft nichtleere `baseline-prof.txt` |
| Packaged permissions | Debug und Release enthalten genau die überprüften Produktrechte; `RECORD_AUDIO` darf nicht still eintreten | `aapt dump permissions` in CI |
| Supply artifact | installierbare Debug-APK und SHA-256 werden erzeugt | GitHub-Actions-Artefakt |

## Aktuelles Permission-Budget

Der aktuelle Produktions-Manifestvertrag umfasst bewusst:

- `android.permission.INTERNET` – ausschließlich als technische Voraussetzung für die optionale, explizit gegatete Direct-Provider-Schicht;
- `android.permission.ACCESS_NETWORK_STATE`;
- `android.permission.CAMERA` – für die separat opt-in geschützte Camera Awareness;
- `android.permission.FOREGROUND_SERVICE`;
- `android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION` – für die sichtbar laufende Screen-Awareness-Session nach Android-MediaProjection-Consent.

Nicht Teil des Budgets sind insbesondere `RECORD_AUDIO`, Standort, Kontakte, Anruflisten, SMS, Phone State, `QUERY_ALL_PACKAGES` oder ein Accessibility Service zur Fremd-App-Steuerung.

Die Existenz von `INTERNET` ist **keine Cloud-Freigabe**. KALs Product-Level Policy muss direkte Provider-Anfragen weiterhin ablehnen, solange Cloud Access/Privacy-Gates oder eine ausdrücklich verbundene Provider-Credential fehlen. Background-Anfragen bleiben verboten.

## Was ein grüner CI-Lauf nicht beweist

Unit-Tests, Instrumentation und Compiler prüfen Regeln und Integrationsgrenzen, aber keine vollständige reale OEM-UI oder reale Langzeitnutzung. Folgende Abnahmen bleiben für einen echten >9,5-Release verbindlich:

| Bereich | Mindestmatrix | Status |
|---|---|---|
| HOME-Recovery | Pixel/AOSP, Samsung, weiterer OEM; Neustart, Crash, HOME-Wechsel | realer Gerätetest offen |
| Lifecycle | Rotation, Fold, Multi-Window und erzwungener Prozess-Tod in Picker/Sheet/Editor | Gerätetest offen |
| Displays | 320 dp, Phone Portrait/Landscape, 8–11″ Tablet, Foldable/Hinge, Desktop Windowing | Lab offen |
| Eingabe | Touch, Maus, Trackpad, Tastatur, S Pen, USI, generischer Bluetooth-Stift | Hardware-Lab offen |
| Accessibility | TalkBack, Switch Access, 200 % Schrift, Bold Text, Reduced Motion, Farbkorrektur | Lab offen |
| SAF | AOSP Files, Google Drive/Cloud, Samsung/weiterer OEM; Create/Rename/Delete/Grant-Verlust | Provider-Matrix offen |
| Backup | Prozess-Tod, Größenlimits, mehrere Provider, Gerätewechsel, Profilkonflikte | reales Restore-Lab offen |
| Kontakte | API 29/33/36 Legacy-Picker und neuere System-Picker; Personal/Work | Lab offen |
| Widgets | Auswahl, Konfiguration, Abbruch, Resize, Stack, Provider-Removal, Neustart, Restore | OEM/App-Matrix offen |
| Settings/Home Studio | Scope-Vererbung, Page-Operationen, Drag/Resize/Undo auf mehreren Formfaktoren | Lab offen |
| Search/AI Hub | 1.000+ Apps/Shortcuts/Settings, Offline-Fallback, Handoff-Abbruch, fehlende Provider | Last-/Gerätetest offen |
| Provider Connections | frische Installation, Gate OFF/ON, Connect/Disconnect, Timeout, Offline, Credential-Löschung | reale Provider-/Netztests offen |
| Screen/Camera | Permission deny/revoke, Prozess-Tod, sichtbare Session, Capture-Abbruch, Provider-Freigabe | reale Geräteabnahme offen |
| Performance | Cold/Warm/Hot Start, Frame Jank, RSS, Akku, Search-Latenz, Pen-Latenz | Messdaten offen |
| Security | unabhängiger Review, SBOM/Dependency-Scan, Restore-/SAF-/OAuth-Fuzzing | unabhängig offen |
| Release | reproduzierbar signierter APK/AAB, Upgrade-/Rollback-Test, Play-Policy-Review | öffentliches Release-Gate offen |
| Sprache | vollständiges Deutsch/Englisch, RTL, Plural- und Abschneidetests | offen |

## Verbindliche Performance-Budgets

- Cold Start P50 ≤ 650 ms und P95 ≤ 1.000 ms auf definierter Mittelklasse;
- Warm Start P95 ≤ 500 ms;
- Frame Time P95 ≤ 16,7 ms und P99 ≤ 33,3 ms für Standardnavigation;
- App-/Search-Raum mit 1.000 Einträgen: Such-/Sortierreaktion P95 ≤ 50 ms außerhalb Renderzeit;
- SAF-Liste mit 500 Einträgen: erste verwertbare Darstellung P95 ≤ 750 ms nach Provider-Antwort;
- Pen Input-to-Render P95 ≤ 25 ms auf unterstützter Hardware;
- keine blockierende Modellinitialisierung im HOME-Prozess;
- idle CPU nahe 0 %, keine periodische Provider-Netzarbeit;
- dokumentiertes RSS- und Akku-Budget mit und ohne optionale KI-Funktionen.

## 9,5-Entscheidungsregel

Eine Gesamtwertung über 9,5 ist nur zulässig, wenn gleichzeitig:

1. alle automatisierten Gates grün sind;
2. alle relevanten manuellen Beta-Gates bestanden sind;
3. keine der 100+ Bewertungs-Kategorien unter 8,5 liegt;
4. Security, Accessibility, Reliability, Startleistung und HOME-Recovery jeweils mindestens 9,5 erreichen;
5. Messdaten, Geräteklassen, Build-SHA und Methodik gespeichert sind;
6. mindestens ein unabhängiger Security-/Privacy-Review und ein Accessibility-Review ohne kritischen Befund vorliegen;
7. Play/Data-Safety-/Privacy-Aussagen dem tatsächlich ausgelieferten Binary entsprechen.

Bis diese Evidenz vorliegt, bleiben frühere Scores historische Messpunkte und keine Behauptung über den aktuellen Integrationsbranch.
