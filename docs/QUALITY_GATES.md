# M2.3 Professional Quality Gates

Stand: 24. August 2026

Ein Zielwert über 9,5 ist kein Designlabel, sondern ein Release-Gate. Diese Datei trennt bereits automatisierte Nachweise von offenen Geräte- und Produktprüfungen.

Der M2.3-CI-Lauf #23 erfüllt alle automatisierten Repository-Gates. Die strenge Neubewertung erreicht 7,6 allgemein und 7,4 im Mittel der 25 simulierten Fachperspektiven. Alle manuellen Beta-Gates bleiben offen; das 9,5-Gate ist daher nicht erfüllt.

## Automatisierte Repository-Gates

| Gate | Kriterium | Nachweis |
|---|---|---|
| Local-first | Manifest enthält als `uses-permission` ausschließlich `ACCESS_NETWORK_STATE` | CI bricht bei jeder weiteren Deklaration ab |
| Build | Debug- und minifizierter Release-Build kompilieren | GitHub Actions `assembleDebug assembleRelease` |
| Static quality | Android Lint ohne Fehler | `lintDebug` |
| Regression | alle lokalen Unit-Tests grün | `testDebugUnitTest` |
| Backup confidentiality | Klartext ist nicht im Envelope sichtbar | `PortableBackupCodecTest` |
| Backup integrity | falsche Passphrase und manipuliertes GCM-Ciphertext werden abgelehnt | `PortableBackupCodecTest` |
| Capability safety | sensible/destruktive Aktionen können Bestätigung nicht aus ihrer Policy verlieren | `CapabilityPolicyTest` |
| Audit privacy | CSV besitzt nur Zeitpunkt, Aktion und Ergebnis | `AuditCsvTest` plus geschlossenes Datenmodell |
| Keyboard | Ctrl-/Meta-Routing fängt keine normalen Tasten ab | `ProfessionalShortcutResolverTest` |
| FAQ | Professional-, Backup-, Audit-, Kontakt- und Recovery-Themen vorhanden | `FaqRegistryTest` |
| Static contrast | zentrale Fallback-Textpaare erreichen mindestens 4,5:1 | `WcagContrastTest` |
| Startup profile | startkritischer Pfad ist als ART-Baseline-Profil vorhanden | CI prüft nichtleere `baseline-prof.txt` |

## Manuelle Abnahme vor Beta

| Bereich | Mindestmatrix | Bestehender Status |
|---|---|---|
| HOME-Recovery | Pixel/AOSP + Samsung + mindestens ein weiterer OEM; Neustart, Crash, HOME-Wechsel | offen |
| Displays | 320 dp, Phone Portrait/Landscape, 8–11″ Tablet, Foldable/Hinge, Desktop Windowing | offen |
| Eingabe | Touch, Maus, Trackpad, Hardware-Tastatur, S Pen, USI, generischer Bluetooth-Stift | offen |
| Accessibility | TalkBack, Switch Access, 200 % Schrift, Bold Text, Reduced Motion, Farbkorrektur | offen |
| Backup | falsche Passphrase, Tamper, 5-MB-Grenze, Prozess-Tod, drei DocumentsProvider, Gerätewechsel | teilweise unit-getestet |
| Kontakte | API 29/33/36 Legacy-Picker und API 37 System-Contact-Picker, Personal/Work | offen |
| Widgets | Auswahl, Konfiguration, Abbruch, Resize-Presets, Provider-Removal, Neustart | offen |
| Profile | Personal, Work locked/unlocked/paused; keine Schlüssel- oder Badge-Kollision | offen |
| Performance | Cold/Warm/Hot Start, Frame Jank, RSS, Akku, Pen-Latenz | offen |
| Security | unabhängiger Code-/Threat-Model-Review, Dependency-/SBOM-Scan, Restore-Fuzzing | offen |

## Harte Performance-Budgets für M2.4

Diese Budgets werden erst als erfüllt markiert, wenn Macrobenchmark-Ergebnisse und Geräteklasse gespeichert sind:

- Cold Start P50 ≤ 650 ms und P95 ≤ 1.000 ms auf einer definierten Mittelklasse;
- Warm Start P95 ≤ 500 ms;
- keine blockierende Modellinitialisierung im HOME-Prozess;
- Frame Time P95 ≤ 16,7 ms bei Standardnavigation, P99 ≤ 33,3 ms;
- Pen Input-to-Render P95 ≤ 25 ms auf unterstützter Hardware;
- idle CPU nahe 0 %, keine periodische Netzarbeit, da kein `INTERNET`;
- dokumentiertes RSS-Budget mit und ohne optionalen Modellprozess.

## 9,5-Entscheidungsregel

Eine Gesamtwertung >9,5 ist nur zulässig, wenn gleichzeitig:

1. alle automatisierten Gates grün sind;
2. alle manuellen Beta-Gates mindestens „bestanden“ statt „offen“ tragen;
3. keine der 65+ Vergleichskategorien unter 8,5 liegt;
4. Sicherheit, Accessibility, Reliability, Startleistung und HOME-Recovery jeweils mindestens 9,5 erreichen;
5. die Wertung aus gespeicherten Messdaten und klar bezeichneten Expertenperspektiven ableitbar ist.

Bis dahin ist jede niedrigere Bewertung ein Befund, kein Misserfolg und kein Anlass, Zahlen zu manipulieren.
