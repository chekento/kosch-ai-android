# KoSch AI Launcher — AI Everywhere Architecture

## Ziel

KoSch behandelt KI nicht als einzelne Chat-Seite, sondern als optionale Intelligenzschicht unter dem gesamten Launcher. Jede Funktion muss auch dann sinnvoll bleiben, wenn kein API-Key, kein Cloud-Konto und kein externes Modell verfügbar ist.

Der bevorzugte Ausführungsweg ist:

1. **Local Core** — deterministische Regeln, Ranking, Klassifikation, Parser, Reflow, Redaction und Vorschläge direkt im Launcher.
2. **Android On-device GenAI** — Gemini Nano/AICore über die offiziellen Android/ML-Kit-GenAI-Schnittstellen, wenn Gerät, Sprache und Funktion unterstützt werden. Kein API-Key. Generative Inferenz nur im zulässigen Vordergrundkontext.
3. **Optionales Local Model Pack** — später herunterladbare lokale Modelle für Geräte, die ausreichend RAM/NPU/GPU besitzen. Niemals Zwangsbestandteil der Basis-APK.
4. **Installierte KI-App** — bewusster Android-Handoff über veröffentlichte Share-, Shortcut-, Widget- oder Launch-Schnittstellen.
5. **Play Store / Web** — fehlt eine bekannte App, führt deren Karte zum verifizierten Play-Store-Eintrag. Web wird nur genutzt, wenn kein verifizierter Store-Eintrag existiert oder der Nutzer Web ausdrücklich auswählt.

Kein Accessibility-Scraping, kein automatisches Klicken in fremden Apps, keine undocumented Deep Links und kein verdecktes Kopieren fremder App-Inhalte.

## AI Everywhere — Oberflächen

### Home, Layout und Dock

- lokale Nutzungs- und Szenensignale für adaptive Dock- und Home-Vorschläge
- Layout-Optimierung nach Erreichbarkeit, Displaygröße, Eingabegerät, Haltung und Nutzung
- Smart Groups und intelligente Ordner ohne Cloud
- Natural-Language-Layout-Befehle wie „kompakter“, „mehr Platz für Arbeit“, „abends ruhiger“
- Vorschau + Undo vor automatischen Layoutänderungen
- keine selbsttätige disruptive Umordnung ohne explizit aktivierte Automation

### Theme und Hintergrund

- API-freier Theme Copilot übersetzt natürliche Sprache in echte Launcher-Tokens
- Beispiele: dunkel/hell, Neural Glass, minimal, cinematic, reduced motion, high contrast, Material You, kompakt/luftig
- Hintergrund als **Recipe** statt zwingend als generiertes Bild: Gradienten, Tiefe, Partikel, Licht, Bewegung, Tageszeit, Kontrast und parallaxfähige Ebenen werden lokal parametrisiert
- auf unterstützten Geräten kann On-device GenAI komplexere Prompts in ein validiertes Theme-Recipe übersetzen
- externe LLMs dürfen Theme-Recipes vorschlagen; Import bleibt sichtbar, schema-validiert und rückgängig machbar

### Suche und Command Palette

- lokale Intent-Erkennung für Apps, Systemfunktionen, Szenen, Theme, Settings und Assistant
- kontextuelle Befehlserweiterung, ohne den eingegebenen Text automatisch an die Cloud zu senden
- On-device Rewrite für komplexe Suchanfragen, wenn verfügbar
- task-aware Routing: Recherche, Quellenarbeit, Voice, Bild, Dateien oder allgemeiner Chat können passende installierte KI-Apps priorisieren

### AI App Hub

- bekannte AI/LLM-Apps werden gegen installierte Launcher-Apps abgeglichen
- installierte App: **Öffnen** bzw. veröffentlichte Android-Shortcuts/Widgets anzeigen
- nicht installierte App mit verifiziertem Paket: **Play Store**
- Open-Source-App ohne verifizierten Play-Eintrag: Projekt-/Release-Seite
- alle Vorschläge sind einzeln ausblendbar; Ausblendung speichert nur eine stabile Suggestion-ID
- „Ausgeblendete Vorschläge zurücksetzen“ gehört in Settings
- veröffentlichte App-Shortcuts und Widget-Provider werden zur Laufzeit entdeckt; KoSch behauptet keine Capability, die die Ziel-App nicht selbst veröffentlicht

### Dateien

- SAF und vom Nutzer gewählte Dateien bleiben Standardzugang
- lokale Dateiklassifikation, Benennungsvorschläge und Workspace-Organisation
- lokale/on-device Zusammenfassung für explizit ausgewählte Inhalte
- vor externem Handoff: Ziel, Kontextumfang und Redaction-Vorschau
- keine Vollspeicherindizierung als Voraussetzung

### Kommunikation

- Standardpfad bleibt Android `ACTION_DIAL` und `ACTION_SENDTO`; KoSch tätigt keinen versteckten Anruf und sendet keine Nachricht selbst
- Kontakte werden standardmäßig nur über einmalige Android-Auswahl übernommen
- Telefonnummern werden niemals in AI-Prompts eingebettet
- AI Call Prep: Ziele, Kernpunkte, Rückfragen
- Message Draft: Text und Ton vorbereiten, danach Übergabe an System-Composer
- Follow-up: lokale nächste Schritte, Reminder-Text, optional Calendar-/Alarm-Handoff
- Post-call Note: Nutzer schreibt oder diktiert selbst eine Notiz; KI strukturiert sie in Ergebnis, To-dos und offene Fragen
- KoSch-eigene Kommunikationshistorie darf standardmäßig nur Aktionstyp + Zeit speichern; kein systemweites Call-Log-Scraping
- optionale gepinnte Kommunikationskarten benötigen eigene explizite Persistenzentscheidung; sensible Nummern gehören dann in einen lokalen geschützten Store, nicht in portable Settings

### Benachrichtigungen

- nur nach bewusst erteiltem Notification-Listener-Zugriff
- lokale Gruppierung, Priorisierung und Badge-Logik
- keine Notification-Inhalte an externe KI im Hintergrund
- Zusammenfassung nur auf explizit geöffnete Auswahl und bevorzugt on-device/local
- externe Übergabe benötigt Inhaltsvorschau und Redaction

### Smartpen

- Gesten und Stroke-Muster lokal interpretieren
- Kreis → Auswahl/Ask, Pfeil → Verknüpfung, Stern → Priorität, Randnotiz → Follow-up als optionale lokale Semantik
- Handschrift/Skizze nur nach ausdrücklicher Aktion an generative KI geben
- On-device GenAI bevorzugen

### Screen und Camera Awareness

- capability standardmäßig AUS
- Aktivierung ausschließlich manuell
- jede aktive Session sichtbar
- Screen: MediaProjection-Systemdialog bleibt Autorität
- Kamera: Runtime-Permission + sichtbarer Zustand
- On-device Analyse bevorzugt; externer Handoff nur mit Kontextvorschau
- keine versteckte Hintergrundbeobachtung

### Accessibility

- lokale UI-Vereinfachung, große Targets und reduced motion ohne KI
- On-device Bild-/Screenbeschreibung für vom Nutzer gewählte Inhalte
- keine Accessibility-Service-Automation fremder Apps als Integrationsersatz

### Automationen

- natürliche Sprache → lokale, typisierte Regel
- vor Aktivierung immer Dry Run mit Trigger, Bedingungen, Datenzugriffen und Aktionen
- generative Modelle dürfen Regeln vorschlagen, aber nie ungeprüfte arbitrary Intents oder Shell-Kommandos erzeugen
- sensible/externe Aktionen bleiben confirmation-first

### Backup und Migration

- Restore-Diff lokal erklären
- potenzielle Konflikte und verworfene gerätegebundene Werte hervorheben
- AI darf niemals Secrets, Capture-Grants, Widget-Host-IDs oder device-local Voice-IDs in portable Daten hineinziehen

### Privacy Copilot

- lokale Redaction vor externem AI-Handoff
- Telefonnummern, E-Mail-Adressen, IDs, Tokens und optionale Eigennamen markieren/minimieren
- Kontextbudget sichtbar machen: „gesendet werden 612 Zeichen, 1 Dateiausschnitt, keine Kontakte“
- Ziel-App/Provider sichtbar benennen
- Default: lieber weniger Kontext als stilles Oversharing

## Hintergrund-KI

„Background AI“ bedeutet in KoSch nicht automatisch Cloud-LLM im Hintergrund.

Zulässig und sinnvoll im Hintergrund:

- lokale Usage-Signale
- deterministische Klassifikation
- Ranking und Kontextregeln
- Layout-/Theme-Rezepte ohne personenbezogene Inhalte
- Battery-/Thermal-/Display-Adaption
- lokale Redaction und Index-Metadaten

Generative AICore-/Gemini-Nano-Inferenz wird nur dort genutzt, wo Android sie für den aktuellen Zustand zulässt. Wenn die Plattform foreground-only verlangt, respektiert KoSch diese Grenze und fällt auf Local Core zurück.

## Provider- und App-Routing

Ein Provider-Eintrag besitzt stabile ID, bekannte Paketnamen, verifizierten Play-Store-Paketnamen und grobe Capability-Tags. Diese Tags dienen nur der UI/Routing-Vorentscheidung. **Runtime Discovery ist verbindlich.**

Reihenfolge für eine Provider-Karte:

1. installierte passende App gefunden → öffnen/teilen
2. App veröffentlicht Shortcuts → als AI Actions anbieten
3. App veröffentlicht Widgets → als einbettbare AI Surfaces anbieten
4. App fehlt, verifizierter Play-Package-Name vorhanden → Play Store
5. kein verifizierter Play-Package-Name → offizielle Web-/Projektseite

Ein Fehler bei einem Share-Intent darf nicht zu verdecktem Browser-Handoff mit sensiblen Daten führen. Der Nutzer entscheidet erneut, wenn der Transportweg wechselt.

## Datenschutzklassen

- `NON_SENSITIVE`: Theme, allgemeine Geräteeigenschaften, Provider-Metadaten
- `USER_SELECTED_CONTENT`: bewusst ausgewählte Datei, Bild, Text oder Stiftinhalt
- `PERSONAL_CONTEXT`: Nutzungsmuster, ausgewählter Kontaktname, persönliche Arbeitskontexte
- `HIGHLY_SENSITIVE`: Screen-/Camera-Inhalt, Notification-Inhalte, Kommunikationsdetails

Je höher die Klasse, desto weniger Hintergrundverarbeitung und desto stärker Preview/Redaction/Bestätigung.

## Nicht-Ziele

KoSch wird nicht:

- fremde Apps per Accessibility fernsteuern
- Chatverläufe anderer AI-Apps auslesen
- Logins/Cookies fremder Apps verwenden
- System-Call-Logs ohne klaren Produktrollenwechsel einsammeln
- Telefonnummern automatisch an LLMs senden
- API-Keys im portablen Settings-Dokument speichern
- fehlende AI-App-Capabilities erfinden
- generative Vorschläge ohne Validierung als Systemaktion ausführen

## Umsetzungsreihenfolge

1. AI App Hub: App/Play/Web-Routing + Dismiss + veröffentlichte Shortcuts
2. Theme Copilot: Local Planner → Preview → Apply/Undo
3. Context Preview + Redaction für externe Handoffs
4. Android On-device GenAI Capability Probe und Adapter
5. AI Actions aus App-Shortcuts und Widget-Providern
6. Communication Copilot im Phone Sheet
7. Notification-/File-/Pen-On-device-Features
8. optionales Local Model Pack mit Geräte-/Thermal-/RAM-Gates
9. natürliche Automationsregeln mit Dry Run
10. AI Feature Matrix im Settings Center als Live/Available/Unsupported pro Gerät
