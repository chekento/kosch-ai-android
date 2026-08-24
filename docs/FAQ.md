# FAQ – KoSch AI Android M2.5 Professional Parity & Correctness

Stand: 24. August 2026

Diese Datei dokumentiert Bedienung, Smartpen, Android-Integration, Professional-Workflows, Datei-Arbeitsraum, KI-Grenzen, Datenschutz, Wiederherstellung und Entwicklung. Im Launcher selbst sind **57 vollständig lokale, kategorisierte und durchsuchbare FAQ-Einträge** integriert: **Kontrollzentrum → FAQ & Hilfe** oder `faq` in **⌘ Ask**. Die In-App-Suche benötigt weder Konto noch Netzwerk.

## Start und Sicherheit

### Funktioniert KoSch beim ersten Öffnen ohne API und Modell-Download?

Ja. App-Katalog, lokale Suche, fünf Szenen, Smart Dock, Smart-Ordner, Layoutvorschläge, Telefon- und Dateiübergabe, Widgets, Systemaktionen, FAQ und Smartpen-Erkennung arbeiten ohne API-Schlüssel. Der eingebaute `KoSch Local Core` ist deterministisch und kein umbenanntes generatives LLM.

### Wie wird KoSch zum Standard-Launcher?

Im Onboarding oder im HOME-Hinweis **Festlegen** wählen. KoSch öffnet Androids geschützten Rollendialog für `ROLE_HOME`; nur Android und die Person am Gerät treffen die Auswahl.

### Wie komme ich jederzeit zu einem anderen Launcher?

**Kontrollzentrum → Sicherheitsausgang → Anderen Launcher wählen** öffnet `Settings.ACTION_HOME_SETTINGS`. Falls ein Hersteller diesen Intent nicht anbietet, wird die allgemeine Android-Einstellung geöffnet. KoSch blockiert die Zurück-Taste nicht und versucht keinen Launcher-Lock-in.

### Was tue ich, wenn KoSch abstürzt oder nicht mehr bedienbar ist?

Androids Einstellungen öffnen und unter Apps beziehungsweise Standard-Apps eine andere Start-App wählen. Für Alpha-Tests empfiehlt sich zunächst ein Emulator oder Zweitgerät. Die HOME-Auswahl ist eine Android-Funktion und bleibt außerhalb des Launcher-Prozesses erreichbar.

### Kann ich die Einführung erneut öffnen?

Ja. **Kontrollzentrum → Einführung erneut ansehen** startet das Onboarding neu, ohne Workspace-Daten zu löschen.

### Ist M2.5 schon produktionsreif?

Nein. M2.5 bleibt eine Alpha. Eigene Ordner, Dock-Reihenfolge, Work-Profile-Steuerung, korrekte Datei-Erfolgssemantik und zusätzliche Systemwege schließen wichtige Produktlücken. Produktionsfreigabe erfordert weiterhin ein echtes OEM-/Tablet-/Foldable-/Stylus-Lab, instrumentierte Prozess-Tod-/Provider-Tests, Macrobenchmarks, vollständige Accessibility-Abnahme, Release-Signing/SBOM und unabhängiges Security Review.

## Launcher und Bedienung

### Wie findet und startet KoSch Apps?

KoSch fragt startbare Activities über Androids `LauncherApps` ab und benötigt deshalb kein `QUERY_ALL_PACKAGES`. Die Suche läuft lokal. Langer Druck zeigt – sofern die App sie veröffentlicht – Shortcuts, Pin/Unpin und App-Info.

### Werden Arbeitsprofile unterstützt?

Ja. M2.5 liest alle für den HOME-Host zugänglichen `LauncherApps.profiles`, verwendet stabile Benutzer-Seriennummern in App- und Shortcut-Schlüsseln, migriert ältere Schlüssel nur bei eindeutiger Zuordnung, zeigt systemgebadgte Icons und kennzeichnet Work-Apps. Gesperrte oder vom System verborgene Profile werden nicht umgangen.

### Wie pausiere oder aktiviere ich ein Arbeitsprofil?

Im Kontrollzentrum zeigt KoSch jedes zugängliche Arbeitsprofil samt Status. Als aktive Standard-Start-App kann KoSch Androids `UserManager.requestQuietModeEnabled` anfordern. Beim Aktivieren kann Android eine Gerätebestätigung verlangen. Pausierte Work-Apps werden markiert und nicht gestartet; Richtlinien, Daten und Authentifizierung bleiben beim System beziehungsweise Geräteadministrator.

### Unterstützt KoSch Android Private Space?

Noch nicht vollständig. KoSch deklariert bewusst kein `ACCESS_HIDDEN_PROFILES`. Android verlangt für Private Space unter anderem einen getrennten Container, Verbergen/Anzeigen sowie Sperren/Entsperren ohne Metadatenleck. Erst nach Implementierung und Gerätetests wird diese Capability erwogen.

### Was sind Szenen?

`AI`, `Work`, `Studio`, `Social` und `Evening` sind persistente Arbeitskontexte. Uhrzeit, Akku, validierte Netzverfügbarkeit und Audioausgabe können lokal einen Vorschlag beeinflussen. KoSch schaltet die Szene nicht heimlich um.

### Wie bearbeite ich den Workspace?

Oben auf **EDIT** wechseln. Karten lassen sich verschieben; ein lokaler Layoutvorschlag läuft über **Vorschau → Anwenden/Verwerfen → Undo**. Ein Modell erhält keine direkten Schreibrechte am Workspace Store.

### Was sind Smart Space und Smart-Ordner?

Der Local Core gruppiert Apps transparent anhand von App-Schlüssel, Label und Paketname. Vorschläge werden vor dem Speichern gezeigt. **Smart Space → Neu** legt zusätzlich eine eigene persistente Sammlung an. Im Edit-Modus lassen sich Titel ändern, Apps verschieben/entfernen und Ordner nach zweiter Bestätigung löschen. Maximal 12 Ordner mit je 32 Apps begrenzen Oberfläche und Backup.

### Wie arbeitet das Smart Dock?

Bis zu fünf Plätze priorisieren manuell gepinnte Apps und ergänzen lokale, über KoSch gestartete Apps sowie szenenbezogene Vorschläge. Langer Druck öffnet Pin/Unpin und verschiebt einen Pin nach links oder rechts. Profilzugehörigkeit ist Bestandteil des stabilen App-Schlüssels.

### Wie lernt KoSch meine App-Prioritäten?

KoSch zählt ausschließlich Starts, die über KoSch erfolgen, und speichert pro profilgebundenem App-Schlüssel nur Anzahl und letzten Zeitpunkt. Die Liste ist auf 512 Schlüssel begrenzt. Es gibt keinen Zugriff auf Androids vollständigen Usage-Verlauf, fremde App-Inhalte oder ein Cloudprofil. **Kontrollzentrum → Lokales Lernen** löscht alle Signale nach zweiter Bestätigung.

### Wie sortiere ich den App-Raum?

Die Auswahl **Smart**, **A–Z**, **Häufig** und **Zuletzt** ist im App-Raum sichtbar und bleibt während einer UI-Wiederherstellung erhalten. Bei aktiver Suche hat die Suchrelevanz Vorrang; danach greift die gewählte Sortierung.

### Was bedeutet „App verbergen“?

Langer Druck → **Verbergen** entfernt die App aus normalen KoSch-Sammlungen, Dock und Ordneransichten. Unter **Apps → Verborgen** kann sie wieder eingeblendet werden. Die Funktion deaktiviert oder deinstalliert die App nicht und ist keine Android-Sicherheitsfunktion.

### Welche App-Aktionen sind verfügbar?

Der Langdruck-Aktionsraum kann App und veröffentlichte Shortcuts starten, profilbezogene App-Info und Store öffnen, Dock/konkrete Ordner/Sichtbarkeit verwalten und Androids Deinstallationsdialog für genau das gewählte persönliche oder Arbeitsprofil anfordern. KoSch deinstalliert nicht selbst und bestätigt den Systemdialog nicht.

### Welche Seiten gibt es?

**Pro Desk** ist für Neuinstallationen der professionelle Standardbereich. Hinzu kommen frei angeordneter Workspace und Smart Space. Wenn ein kompatibler Stift erkannt wird, erscheint **Pen Space**. Widgets liegen in einem eigenen, größenadaptiven Board.

### Wie füge ich Widgets hinzu?

**Kontrollzentrum → Widget +** startet Androids Widget-Auswahl. KoSch persistiert nur erfolgreich gebundene IDs und gibt abgebrochene oder entfernte IDs frei. Die Presets **Kompakt**, **Standard** und **Hoch** aktualisieren die Größenoptionen des Providers. Pfeile ändern die persistente Board-Reihenfolge; Undo stellt die vorige noch gültige Reihenfolge wieder her. Freie Board-Platzierung, Stacks und Provider-Restore-Mapping zwischen Geräten bleiben offen.

### Was lesen Notification Dots?

Nur Paketname und Anzahl badgefähiger, aktiver Benachrichtigungen werden nach separatem Android-Opt-in prozesslokal gezählt. Titel, Text, Personen, Extras, Aktionen und `RemoteViews` werden weder kopiert noch gespeichert. Ohne Zugriff bleibt der Launcher vollständig funktionsfähig.

### Kann KoSch telefonieren?

KoSch bereitet eine geprüfte Nummer mit `ACTION_DIAL` im System-Wähler vor. Es besitzt kein `CALL_PHONE`, liest weder das gesamte Adressbuch noch die Anrufliste und bestätigt keinen Anruf selbst. Eine Person kann genau eine Telefonnummer über Androids `ACTION_PICK` wählen; Android 17s neuer System-Contact-Picker wird über `EXTRA_USE_SYSTEM_CONTACTS_PICKER` bevorzugt. Name und Nummer bleiben nur im aktuellen Vorgang.

### Kann KoSch eine SMS oder Nachricht beginnen?

Ja. Telefonraum, Pro Desk oder der lokale Befehl „SMS“ öffnen die vom System gewählte Nachrichten-App über `ACTION_SENDTO` und `smsto:`. Eine validierte Nummer kann vorausgefüllt werden. KoSch liest keine Nachrichten, setzt keinen Inhalt und versendet nichts selbst; die Ziel-App besitzt Empfänger, Text und Bestätigung.

## Professional und Produktivität

### Was ist Pro Desk?

Pro Desk bündelt HOME-Status, API-freien Local Core, Anzahl zugänglicher Arbeitsprofil-Apps, lokale Audit-Lage, relevante Apps sowie Telefon-, Kontakt-, Datei-, Widget-, Backup- und Audit-Aktionen. Es ist keine separate Cloud-Oberfläche, sondern eine adaptive Sicht auf bereits vorhandene, eng begrenzte Capabilities.

### Welche Hardware-Tastatur-Shortcuts sind verfügbar?

`Ctrl/Meta+K` fokussiert die Command Bar, `Ctrl/Meta+Leertaste` öffnet Apps, `Ctrl/Meta+H` Pro Desk, `Ctrl/Meta+,` das Kontrollzentrum, `Ctrl/Meta+D` Telefon, `Ctrl/Meta+O` die einzelne Datei-KI, `Ctrl/Meta+Shift+O` den Datei-Arbeitsraum, `Ctrl/Meta+B` Backup und `Ctrl/Meta+L` Audit. `Ctrl/Meta+Shift+P` öffnet Pen Space, sofern ein Stift erkannt wurde. `Escape` schließt genau die oberste temporäre KoSch-Fläche. Androids systemweite Shortcut-Hilfe listet dieselben Befehle.

### Welche Professional-Befehle versteht der Local Core?

Zusätzlich zu Apps, Szenen und Systembereichen versteht er deterministisch unter anderem „Pro Desk“, „Workspace sichern“, „Sicherheitsverlauf“, „Kontakt auswählen“, „Dateien verwalten“, „Arbeitsordner“, „SMS“, „Kalender“, „Wecker“, „Kamera“ und „Systemnotiz“. Dadurch bleiben zentrale Arbeitsabläufe auch ohne LLM, Netzwerk und API-Schlüssel erreichbar. Unbekannte Texte erhalten keine autonomen Rechte, sondern führen weiterhin in die bewusste Anbieterwahl.

### Wie funktioniert der verschlüsselte Export?

**Pro Desk/Kontrollzentrum → Backup** erzeugt einen versionierten JSON-Snapshot und verschlüsselt ihn lokal mit PBKDF2-HMAC-SHA-256 (210.000 Iterationen, zufälliges 128-Bit-Salt) und AES-256-GCM (zufällige 96-Bit-Nonce, 128-Bit-Tag). Formatversion und Work Factor sind als Additional Authenticated Data gebunden. Die Passphrase muss mindestens zwölf Zeichen haben, wird weder gespeichert noch auditiert und wird nach der Ableitung im verarbeitbaren Puffer überschrieben.

### Was enthält das Workspace-Backup?

Aktive Szene und Home-Seite, geprüfte Kartenpositionen, lokaler KoSch-Verlauf, Pins, Smart-Ordner und begrenzte Pen-Vektordaten. Widget-Host-IDs, Storage-Access-Freigaben, Zugangsdaten, Notification-Daten und Audit-Log sind absichtlich ausgeschlossen, weil sie geräte- oder sicherheitsgebunden sind.

### Wie läuft ein Restore ab?

Import und Restore sind getrennt. KoSch begrenzt die Dateigröße, authentifiziert und entschlüsselt den Envelope, validiert Format, Version, Zeitstempel, Mengenlimits, Enum-Werte, Schlüssel-/Titellängen, Kartenkoordinaten sowie endliche Stiftwerte und zeigt erst dann eine Zusammenfassung. Eine zweite Checkbox-/Button-Bestätigung ist nötig. Erst danach werden die validierten Workspace-Werte in einem synchronen Preferences-Commit ersetzt; ein Fehler lässt den bestehenden Workspace unangetastet.

### Was passiert bei Rotation oder Prozess-Tod während eines Exportdialogs?

Der Controller gehört einem `LauncherViewModel` und bleibt bei Activity-Recreation erhalten. Backup-, Audit- und SVG-Exportpayloads werden zusätzlich atomar als begrenzte private Datei in `noBackupFilesDir` abgelegt; Saved State enthält nur einen zufälligen, typgebundenen Einmaltoken. Ein Payload ist höchstens 8 MiB groß, wird einmal konsumiert oder verworfen und spätestens nach 24 Stunden bereinigt. Das schützt den Android-Zieldialog-Handoff, ersetzt aber noch keinen vollständigen OEM-Prozess-Tod-Labortest.

### Was zeichnet das lokale Audit auf?

Nur drei typisierte Werte: UTC-Zeitpunkt, fest definierter Aktionstyp und Ergebnis. Das Schema hat kein Freitextfeld und kann daher keine Prompts, Namen, Telefonnummern, Datei- oder Paketziele, Pfade oder Benachrichtigungsinhalte aufnehmen. Es hält maximal 250 Ereignisse und verwirft Werte nach 90 Tagen. Export ist eine explizite SAF-CSV-Auswahl; vollständiges Löschen verlangt eine zweite Bestätigung. Audit-Daten sind nicht Teil des Backups.

### Ist KoSch ein vollständiger Dateimanager?

Noch nicht im Sinn eines rekursiven System-Explorers. M2.5 besitzt zwei getrennte sichere Routen:

1. **Datei prüfen:** genau ein read-only Dokument; Metadaten und höchstens 4.096 Zeichen bekannter Textformate.
2. **Datei-Arbeitsraum:** genau ein von der Person gewählter SAF-Dokumentbaum mit READ/WRITE.

Im Arbeitsraum kann KoSch navigieren, lokal nach Name suchen, nach A–Z/Neu/Größe/Typ sortieren, sichtbare Kategorien und bekannte Größen zusammenfassen, gleichnamige Einträge markieren, die größten Dateien nennen, Ordner erstellen, Dokumente öffnen, umbenennen und löschen. Move/Copy, Rekursion, Volltextindex, Papierkorbgarantie und Malware-Scan fehlen.

### Kann KoSch außerhalb des gewählten Arbeitsordners lesen?

Nein. Es gibt weder `MANAGE_EXTERNAL_STORAGE` noch `READ_MEDIA_*`. KoSch konstruiert nur Dokument-URIs innerhalb des gewählten SAF-Baums, listet höchstens 500 direkte Kinder und besitzt genau eine persistierte Tree-Freigabe. **Arbeitsordner vergessen** löst sie vollständig.

### Wie werden Dateiänderungen abgesichert?

Anbieter-Flags bestimmen, ob Create/Rename/Delete überhaupt angeboten werden. Neue Namen sind auf 120 Zeichen begrenzt; Separatoren, Steuerzeichen, `.` und `..` werden abgelehnt. Erstellen und Umbenennen benötigen Vorschau und Bestätigung; die letzte Umbenennung bietet Undo. Löschen hat einen separaten endgültigen Dialog und kein KoSch-Undo, weil der DocumentsProvider direkt schreibt.

### Was passiert, wenn die Dateiänderung gelingt, die Ansicht danach aber nicht lädt?

KoSch trennt Provider-Mutation, Audit und Refresh. Eine bestätigte Änderung bleibt erfolgreich und wird auch auditiert, wenn die Datei-Fläche bereits geschlossen wurde. Scheitert nur die anschließende Verzeichnisliste, erhält dieser Refresh ein eigenes Fehlerereignis und macht den Provider-Effekt nicht rückwirkend zum Fehlschlag. **Aktualisieren** bleibt im aktuellen Ordner statt unbemerkt zur Tree-Wurzel zu springen.

### Bedeutet „lokal analysiert“, dass ein Cloudordner offline ist?

Nein. KoSch sendet selbst keine API-Anfrage und analysiert nur Metadaten im Prozess. Der vom Nutzer gewählte DocumentsProvider kann aber beispielsweise Cloudspeicher darstellen und nach seinen eigenen Regeln Daten laden. Quelle und Provider bleiben daher sichtbar und unter Nutzerkontrolle.

### Welche Systemfunktionen bietet das Kontrollzentrum?

Sichtbare Android-Oberflächen für WLAN, Bluetooth, Benachrichtigungszugriff, Hintergrund, Anzeige, Ton, Akku, Datenschutz, Bedienungshilfen, Standard-Apps, Speicher, profilbezogene App-Info, Widgets, Onboarding und HOME-Auswahl. Zusätzlich öffnen Pro Desk und Kontrollzentrum Nachrichten, Kalender, Wecker, Kamera und – auf Android 14+ – die zuständige Systemnotiz-App. Proprietäre oder nicht vorhandene Ziele fallen auf dokumentierte Alternativen zurück oder werden sichtbar als Fehler gemeldet; KoSch simuliert keine Systemschalter.

### Funktioniert Spracheingabe offline?

KoSch startet Androids installierte Spracherkennungs-Activity. Ob sie offline arbeitet, hängt von Gerät, Sprache und IME/Recognizer ab. KoSch hält keinen dauerhaften Mikrofonstream und deklariert kein eigenes Mikrofonrecht.

## Smartpen und Pen Space

### Wie erkennt KoSch einen Smartpen?

`StylusMonitor` beobachtet `InputManager.InputDeviceListener` und Geräte mit `SOURCE_STYLUS` oder `SOURCE_BLUETOOTH_STYLUS`. Echte Ereignisse werden zusätzlich über `TOOL_TYPE_STYLUS` beziehungsweise `TOOL_TYPE_ERASER` bestätigt. Gerätewechsel werden live sowie beim Resume neu bewertet.

### Was wird nach der Erkennung zusätzlich angeboten?

Pen Space erscheint als weitere Home-Seite und als Schnellaktion im Kontrollzentrum. Die Oberfläche zeigt vorhandene Fähigkeiten wie Druck, Neigung, Hover und Bluetooth transparent an. Auf Android 14+ kann **Systemnotiz** zusätzlich die zuständige Notes-App mit aktivem Stylus-Modus öffnen. `⌘ Ask` versteht beide Wege.

### Was kann Pen Space heute?

Eine lokale Zeichenfläche mit druckabhängiger Breite, Stift, Marker, Hardware-/Software-Radierer, Hover-Cursor, Undo, Leeren, Autosave und SVG-Export. Striche sind normalisierte Vektordaten und überleben Größen- und Orientierungswechsel.

### Was ist der Unterschied zwischen Pen Space und Systemnotiz?

Pen Space gehört zu KoSch, speichert begrenzte Vektorstriche lokal und funktioniert ohne andere App. **Systemnotiz** verwendet ab Android 14 `ACTION_CREATE_NOTE` und `EXTRA_USE_STYLUS_MODE`; Inhalt, Rolle und Speicherung gehören der kompatiblen Notes-App. Ist keine vorhanden oder Android älter, fällt KoSch sichtbar auf Pen Space zurück.

### Werden Druck, Neigung, Orientierung, Hover und Stifttasten verarbeitet?

Ja, sofern Android und das Gerät Werte liefern. Druck beeinflusst die Breite; Neigung, Orientierung, Hover, Werkzeugtyp und primäre/sekundäre Stifttaste werden erkannt beziehungsweise angezeigt. KoSch erfindet keine nicht gemeldeten Fähigkeiten.

### Wie verhindert Pen Space Handballen- oder Fingerstriche?

Die Zeichenfläche akzeptiert nur Stylus- und Eraser-Ereignisse. Fingerkontakte werden nicht gezeichnet. Zusätzliche Palm-Rejection des Geräts oder Systems bleibt davon unabhängig.

### Wie werden Zeichnungen gespeichert?

Lokal im versionierten Workspace-Schema v6 als Werkzeug plus normalisierte Punkte mit `x`, `y`, Druck und Neigung. Der Store begrenzt auf 100 Striche und 2.048 Punkte pro Strich. Es gibt keinen Upload, keine Bildkonvertierung und keine versteckte Handschriftanalyse.

### Wie exportiere ich Pen Space?

**SVG exportieren** bereitet lokal eine portable Vektordatei vor und öffnet Androids Zieldialog. Sehr lange Striche werden gleichmäßig auf höchstens 2.048 Punkte reduziert; der erste und der letzte sichtbare Punkt bleiben erhalten. Ungültige oder nicht endliche Sensorwerte werden verworfen. Das SVG enthält keine Gerätekennung und löst keinen Upload durch KoSch aus.

### Was passiert, wenn der Stift getrennt wird?

Vorhandene Striche bleiben erhalten. Pen Space meldet den getrennten Zustand und schützt die Fläche weiterhin vor Fingereingabe. Nach erneuter Verbindung aktualisiert der Input-Listener die Fähigkeiten ohne Neustart.

### Unterstützt KoSch Samsung S Pen, USI und Bluetooth-Stifte?

Alle über Androids generische Stylus-Quellen gemeldeten Grundfunktionen sind herstellerneutral nutzbar. Samsung Air Actions oder andere proprietäre Remote-Gesten sind noch nicht Kernbestandteil; dadurch entsteht keine Pflichtabhängigkeit von einem Hersteller-SDK.

### Kann ich handschriftlich in App-Suche und FAQ schreiben?

Auf Android 14 oder neuer kann eine kompatible IME systemweite Stylus-Handschrift in regulären Textfeldern anbieten. KoSch nutzt normale Textfelder, enthält aber keine eigene Handschrifterkennung. Qualität, Sprache und Verfügbarkeit liegen bei der installierten Tastatur.

### Verwendet M2.5 Jetpack Ink?

Noch nicht. M2.5 verwendet eine kleine eigene `PressureInkView`, damit der Launcher ohne zusätzlichen Download und mit klar begrenzter Persistenz funktioniert. Resampling und SVG-Export sind vorhanden. Die freie AndroidX-Ink-API bleibt eine Kandidatin für Latenz-, Brush- und Historical-Event-Verbesserungen nach Gerätebenchmarks.

### Versteht die KI meine Zeichnung bereits semantisch?

Nein. **An Ask** öffnet den lokalen Intent-Eingang, überträgt aber keine Zeichnung. Handschrift-zu-Text, Diagrammverständnis, „Skizze zu Layout“ und pen-basierte Aktionsgesten benötigen ein optionales lokales Modell, explizite Vorschau und Löschkontrollen.

## Modernes Android und Design

### Nutzt KoSch Material You?

Ja. Ab Android 12 übernimmt M2.5 dynamische Systemfarben in ein dunkles Neural-Glass-System. Ältere Geräte erhalten eine kuratierte, kontrastreiche Palette. LCARS ist keine Kernabhängigkeit und kann später als deklaratives Theme entstehen.

### Wie passt sich die Oberfläche an Display und Haltung an?

Die Compose-Shell entscheidet aus aktuellen Fenstermaßen. Kompakte Fenster bleiben gestapelt; breite Fenster verwenden eine geteilte Navigation/Arbeitsfläche, insbesondere ab 840 dp oder ab 720 dp im Querformat. Edge-to-edge-Inset-Schutz bleibt aktiv. Vollständige Foldable-Hinge-, 320-dp- und 200-%-Schrifttests sind noch ein Abnahme-Gate.

### Werden deaktivierte Animationen respektiert?

Ja. KoSch liest Androids Animator-Dauer. Bei deaktivierten Systemanimationen wird Neural Glass statisch und wichtige Zustände bleiben textlich beziehungsweise strukturell erkennbar.

### Welche Accessibility-Verbesserungen enthält M2.5?

App- und Dock-Kacheln besitzen zusammengeführte Button-Semantik mit Zustands- und Langdruckbeschreibung. Pen Space veröffentlicht Undo und Clear als Accessibility-Custom-Actions und aktualisiert seine Inhaltsbeschreibung mit Werkzeug und Strichzahl. Reduced Motion und statische Kontrasttests sind aktiv. Eine vollständige TalkBack-, Switch-Access- und 200-%-Schrift-Geräteabnahme bleibt offen.

### Welche modernen Android-Funktionen nutzt M2.5 außerdem?

HOME-Rolle, vorausschauende Zurück-Navigation, Edge-to-edge, `LauncherApps`, profilgebundene Shortcuts/App-Aktionen, Work-Profile-Quiet-Mode, `AppWidgetHost` samt Größenoptionen, Storage Access Framework mit Dokument- und Tree-Routen, privacy-preserving Kontakt-Picker-Route, Notification Listener als Opt-in, Activity-Result-Verträge, ViewModel, Saved State, Android Keystore, dynamische Material-3-Farben, Hardware-Keyboard-Shortcut-Hilfe, Compose-Semantik, Systemnotiz mit Stylus-Modus und die generische Eingabegeräte-Pipeline.

## KI, freie Modelle und optionale APIs

### Was ist der Local Core?

Ein offline arbeitender Planner für App- und Systembefehle, lokale Suche, Szenen, Dock-/Ordnerlogik, Layoutvorschläge und begrenzte Dateihinweise. Er ist reproduzierbar und bleibt verfügbar, wenn kein Modell, Konto oder Netzwerk existiert.

### Ist bereits ein generatives LLM eingebaut?

Nein. Ein ungefragt gebündeltes Modell würde APK-Größe, RAM, Thermik, Lizenz und Gerätekompatibilität verschlechtern. Die geplante Default-Route ist ein optionaler, klar lizenzierter GGUF-Pack in einer getrennten Service-/Prozessgrenze; bei Ausfall übernimmt der Local Core.

### Welche freien und Open-Source-Varianten sind vorgesehen?

PocketPal AI, ChatterUI und Maid sind sichtbare Übergabeziele. Für eine spätere native Laufzeit werden llama.cpp, LiteRT-LM und MLC LLM evaluiert. Engine- und Modelllizenz werden getrennt behandelt.

### Wann verlassen Eingaben das Gerät?

Erst wenn die Person einen externen Anbieter auswählt und eine sichtbare App-, Share- oder Browserübergabe bestätigt. Der M2.5-Launcher selbst deklariert kein `INTERNET`-Recht und führt keine Modell-API-Anfrage aus.

### Wie können APIs später sicher hinzukommen?

Nur als separater Netzwerk-Flavour beziehungsweise Modul. Vor Aktivierung sind mindestens Kontextvorschau, enge TLS-Host-Policy, Timeout/Abbruch/Backoff, Kostenlimits, logfreie Secrets, Löschung/Rotation, optionale Geräteauthentifizierung, metadatenarmes Audit und Prompt-Injection-/Tool-Output-Evals erforderlich. Der vorbereitete Vault nutzt einen nicht exportierbaren Android-Keystore-Schlüssel und AES-256-GCM mit Provider-Bindung.

### Darf ein LLM Apps bedienen oder Daten löschen?

Nicht direkt. Modelloutput ist untrusted data. Aktionen benötigen eine feste Capability, Risikoklasse und – je nach Wirkung – Auswahl, Vorschau, Bestätigung und Undo. Accessibility-Fernsteuerung, simulierte Berührungen und ein autonomer Lösch-/Kauf-/Sendeagent sind ausgeschlossen.

### Ist PMDD-Wallpaper Teil des Kerns?

Nein. Stimmung, Zyklus oder Fokus können später freiwillige Designparameter eines Theme-/Wallpaper-Programms sein. Gesundheitsnahe Daten dürfen nicht implizit abgeleitet werden; ein generiertes Asset braucht Vorschau, lokale Kontrolle und Rollback.

## Datenschutz und Wiederherstellung

### Welche Android-Berechtigungen fordert M2.5 an?

Als `uses-permission` nur `ACCESS_NETWORK_STATE`, um validierte Netzverfügbarkeit lokal als Kontext zu erkennen. Es gibt kein `INTERNET`, `CALL_PHONE`, `READ_CONTACTS`, `READ_MEDIA_*`, `MANAGE_EXTERNAL_STORAGE`, `QUERY_ALL_PACKAGES`, Standort-, Kalender- oder Mikrofonrecht und keinen Accessibility Service. Die CI prüft das Quellmanifest und zusätzlich die tatsächlich paketierte APK.

### Werden Stift- oder Gerätekennungen gespeichert?

Nein. Persistiert werden nur die begrenzten Vektorstriche. Aktuelle Fähigkeiten, Anzahl erkannter Stifte, Druck, Neigung, Hover und Tastenstatus leben im UI-Zustand; Namen, Vendor-ID, Product-ID und Seriennummer eines Eingabegeräts werden nicht in den Workspace geschrieben.

### Gibt es Backup und Export?

Ja, als manueller verschlüsselter Workspace-Export mit validierter Vorschau und bewusstem Restore. `allowBackup=false` verhindert weiterhin unkontrolliertes Android-Cloud-Backup. Das Workspace-Schema v6 ist migrationsfähig. Widget-Provider-Zuordnung, Einzeldatei- und Tree-Freigaben, Pending-Exports, Secrets, Notification-Daten und Audit werden nicht portiert; ein geräteübergreifend vollständiges Systemabbild wird ausdrücklich nicht zugesagt.

### Wie setze ich ein fehlerhaftes Layout zurück?

In **EDIT** auf **Zurücksetzen** tippen oder für den letzten angewendeten Vorschlag Undo verwenden. Dokumentfreigaben und Widget-IDs werden dabei nicht heimlich gelöscht.

### Wie lösche ich lokale Daten?

Granular stehen **Lokales Lernen zurücksetzen**, **Audit löschen**, **Dateizugriff vergessen**, **Arbeitsordner vergessen**, **Pen Space leeren** und das Wiedereinblenden verborgener Apps bereit. Für einen vollständigen Reset Android **App-Info → Speicher und Cache → Speicherinhalt löschen** beziehungsweise Deinstallation verwenden. Zuvor einen anderen Standard-Launcher wählen. Der verschlüsselte Workspace-Export ist kein vollständiges Geräteabbild.

## Entwicklung, Prüfung und Benchmark

### Wie wird das Projekt gebaut?

Mit JDK 17, Android SDK 36 und AGP 8.13:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
```

Die Debug-APK entsteht unter `app/build/outputs/apk/debug/app-debug.apk`; GitHub Actions veröffentlicht `kosch-ai-launcher-m2.5-debug` mit `KoSch-AI-Launcher-M2.5-debug.apk` und SHA-256-Datei.

### Was prüft die CI?

Unit-Tests einschließlich Backup/Tamper, Pending-Export, App-Key-Migration, lokalem Ranking, Collection-Regeln, Datei-Arbeitsraum-Planer und Datei-Mutationssemantik, Ink-Resampling/SVG, Audit, Capability, Keyboard, Kontrast, FAQ und Planner; außerdem Android Lint, Quell- und APK-Permission-Budget, nichtleeres Baseline-Profil, Debug-/minifizierter Release-Build und APK-Prüfsumme. Der grüne [Lauf #35](https://github.com/chekento/kosch-ai-android/actions/runs/32724798845) beweist Buildbarkeit dieses Code-Stands, ersetzt aber keine instrumentierten Geräte-, Provider-, Accessibility-, Performance- oder OEM-Tests.

### Wie wurde M2.5 mit anderen Launchern verglichen?

Die strenge Matrix bewertet KoSch, Pixel/Android 17 als Systemreferenz sowie Nova, Niagara, Smart Launcher, Microsoft Launcher und Lawnchair in 100 Kategorien von 0,1 bis 10,0. Zusätzlich werden 25 reproduzierbare Fachperspektiven mit je 100 KoSch-Einzelwerten simuliert. Das sind keine tatsächlich befragten Personen und kein identischer Sieben-Geräte-Labortest.

### Hat M2.5 die gewünschte Wertung über 9,5 erreicht?

Nein. Der belegbare Stand erreicht 8,2 allgemein und 8,1 im Rollenmittel. Das ist Rang 2 der breiten Matrix, aber kein Produktionsbeweis. Eine Wertung über 9,5 ist erst zulässig, wenn alle manuellen Geräte-, Accessibility-, Recovery-, Performance- und Security-Gates bestanden sind, keine Kategorie unter 8,5 liegt und die kritischen Qualitätsbereiche jeweils mindestens 9,5 erreichen.

### Wo liegen die vollständigen Bewertungsdaten?

In `COMPETITOR_REVIEW_M2_5.md`, den drei M2.5-CSV-Dateien und der formatierten `launcher_benchmark_m2_5.xlsx` mit Summary, 100-Kategorien-Vergleich, Expertenmatrizen, Feature-/Evidenzkarte, Next-Run-Plan und Quellen.

### Was ist das nächste professionelle Ziel?

M2.6 priorisiert instrumentierte HOME-/SAF-/Widget-/Prozess-Tod-Tests, Macrobenchmarks mit Budgets, vollständige Widget-/Launcher-Parität, Accessibility-/OEM-/Stylus-Lab, API-37-Upgrade, Release-Signing/SBOM, Deutsch-/Englisch-Lokalisierung und erst danach ein isoliertes lokales LLM. Der Evidenzplan liegt zusätzlich als eigenes Blatt in der M2.5-Arbeitsmappe.
