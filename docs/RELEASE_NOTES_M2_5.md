# KoSch AI Android 0.2.5-alpha01

M2.5 „Professional Parity & Correctness“ · 24. August 2026

## Wichtigste Neuerungen

- eigene persistente Smart-Space-Ordner mit Create, Rename, Add/Remove, App-Reihenfolge und klaren 12×32-Grenzen;
- explizite Links-/Rechts-Reihenfolge für gepinnte Dock-Apps;
- profilbezogene App-Info und Android-Deinstallationsanfrage für genau das ausgewählte persönliche oder Arbeitsprofil;
- Arbeitsprofil im Kontrollzentrum über Androids geschützten Quiet Mode pausieren und reaktivieren;
- pausierte Work-Apps im App-Raum kennzeichnen und Startversuche kontrolliert ablehnen;
- Nachricht, Kalender, Wecker und Kamera als sichere Android-Systemübergaben;
- Android-14+-Systemnotiz mit EXTRA_USE_STYLUS_MODE bei erkanntem Smartpen und Pen-Space-Fallback;
- Dateimutationen trennen Provider-Erfolg, Audit und anschließenden Refresh;
- erfolgreiche Dateiänderungen bleiben auch nach Schließen der Fläche auditiert;
- Refresh bleibt im aktuellen Verzeichnis und springt nicht zur Tree-Wurzel;
- neue reine Regressionstests für Collection- und Datei-Erfolgssemantik;
- 57 lokale, kategorisierte In-App-FAQ-Einträge;
- modernisierte GitHub Actions auf Node-24-kompatiblen Releases mit vollständig offenem Gradle-Cache;
- zusätzliches APK-Level-Permission-Gate: ein versehentlich paketiertes INTERNET-Recht bricht den Build;
- reproduzierbarer 100-Kategorien-/25-Perspektiven-Benchmark.

## Sicherheitsgrenzen

- Kein Konto, API-Schlüssel oder Modell-Download für die Grundfunktion.
- Kein INTERNET, CALL_PHONE, READ_CONTACTS, Vollspeicher- oder stilles Deinstallationsrecht.
- Arbeitsprofiländerungen laufen über UserManager und sind nur für die aktive Standard-Start-App beziehungsweise privilegierte Systemsoftware zulässig.
- Android kann beim Reaktivieren eines Arbeitsprofils eine Gerätebestätigung verlangen.
- Nachrichteninhalt, Versand, Anrufbestätigung, Kalender, Wecker, Kamera und Systemnotiz bleiben in den zuständigen System-/Ziel-Apps.
- ACTION_CREATE_NOTE wird erst ab Android 14 verwendet; ältere oder inkompatible Geräte fallen auf Pen Space zurück.
- Datei-Create/Rename/Delete bleiben auf den explizit gewählten SAF-Baum begrenzt.
- Private Space bleibt ausgeschlossen, solange kein vollständiger Hide/Show/Lock/Unlock-Container mit Leak-Tests existiert.

## Verifikation

- Unit-Tests für Command Planner, Ordnerregeln, Datei-Mutationssemantik, Backup, Audit, Ranking, SAF-Planung, Pen-Integrität und Sicherheitsregeln;
- Android Lint;
- Debug- und minifizierter Release-Build;
- Quell- und paketiertes APK-Permission-Budget;
- nichtleeres Baseline-Profil;
- APK-Artefakt und SHA-256-Prüfsumme;
- 100 Kategorien, sieben Kandidaten und 25 simulierte Fachperspektiven;
- Ergebnis: **8,2 allgemein**, **8,1 im 25-Perspektiven-Mittel**, Rang 2;
- 9,5-Gate: **nicht erfüllt** – Geräte-, Accessibility-, Performance-, OEM-, Signing- und unabhängige Security-Gates bleiben offen.

## Installation

Die CI veröffentlicht KoSch-AI-Launcher-M2.5-debug.apk samt SHA-256-Datei im Artefakt kosch-ai-launcher-m2.5-debug. Alpha zuerst auf Emulator oder Zweitgerät testen und den **Sicherheitsausgang → Anderen Launcher wählen** vor produktiver Nutzung prüfen.
