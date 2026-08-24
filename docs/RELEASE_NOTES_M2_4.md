# KoSch AI Android 0.2.4-alpha01

M2.4 „Evidence & Resilience“ · 24. August 2026

## Wichtigste Neuerungen

- echter, vom Nutzer gewählter SAF-Datei-Arbeitsraum mit Navigation, Suche, vier Sortierungen, Metadatenübersicht, Ordnererstellung, Rename/Undo und separat bestätigtem Löschen;
- Activity-unabhängiger Launcher-Controller im ViewModel;
- wiederaufnehmbarer Backup-/Audit-/SVG-Export über private, begrenzte No-Backup-Datei und Saved-State-Einmaltoken;
- eindeutige Migration alter profilgebundener App-Schlüssel;
- transparentes, vollständig löschbares lokales App-Start-Ranking;
- App-Raum-Sortierungen Smart, A–Z, Häufig, Zuletzt und Verwaltung verborgener Apps;
- erweiterter App-Aktionsraum mit Store, Ordner, Sichtbarkeit und Android-Deinstallationsdialog;
- zusätzliche Android-Systemwege mit sicherem Einstellungs-Fallback;
- Widget-Reihenfolge und Undo;
- Pen-SVG-Export, endpoint-erhaltendes Resampling langer Striche und Accessibility-Custom-Actions;
- 52 lokale FAQ-Einträge und aktualisierte Sicherheits-/Architektur-/Benchmark-Dokumentation.

## Sicherheitsgrenzen

- kein `INTERNET`, `CALL_PHONE`, `READ_CONTACTS`, `READ_MEDIA_*`, `MANAGE_EXTERNAL_STORAGE` oder `QUERY_ALL_PACKAGES`;
- Datei-Arbeitsraum ausschließlich im gewählten SAF-Baum;
- Delete immer separat bestätigt und ohne vorgetäuschtes Undo;
- lokale Lernsignale enthalten nur App-Schlüssel, Startanzahl und letzten Zeitpunkt, maximal 512 Einträge;
- Exporte höchstens 8 MiB, einmal konsumierbar, 24 Stunden gültig und vom Android-Backup ausgeschlossen;
- direkte Modell-APIs und ein natives generatives LLM bleiben deaktiviert.

## Verifikation

- GitHub Actions: [Lauf #30](https://github.com/chekento/kosch-ai-android/actions/runs/32709807342) – Tests, Lint, Debug-/Release-Build, Permission-Budget, Baseline-Profil und APK-Prüfsumme grün.
- Benchmark: 8,1 allgemein, 7,9 im Mittel von 25 simulierten Fachperspektiven, Rang 2 in 90 Kategorien.
- Das Ziel über 9,5 ist nicht erreicht; offene Gates stehen in [QUALITY_GATES.md](QUALITY_GATES.md).

## Installation

Die CI veröffentlicht `KoSch-AI-Launcher-M2.4-debug.apk` samt SHA-256-Datei im Artefakt `kosch-ai-launcher-m2.4-debug`. Alpha zuerst auf Emulator oder Zweitgerät testen und den **Sicherheitsausgang → Anderen Launcher wählen** vor produktiver Nutzung prüfen.
