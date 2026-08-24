# FAQ – KoSch AI Android M2.2

Stand: 24. August 2026

Diese Datei dokumentiert Bedienung, Smartpen, Android-Integration, KI-Grenzen, Datenschutz, Wiederherstellung und Entwicklung. Im Launcher selbst ist eine kompaktere, vollständig lokale und durchsuchbare FAQ mit 32 Einträgen integriert: **Kontrollzentrum → FAQ & Hilfe** oder `faq` in **⌘ Ask**. Die In-App-Suche benötigt weder Konto noch Netzwerk.

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

### Ist M2.2 schon produktionsreif?

Nein. M2.2 ist `0.2.2-alpha01`. Unit-Tests, Lint und Debug-Build sind in CI grün, aber ein vollständiges OEM-Gerätelabor, instrumentierte Prozess-Tod-Tests, Macrobenchmarks, Accessibility-Abnahme, verschlüsseltes Backup und Widget-Restore fehlen noch.

## Launcher und Bedienung

### Wie findet und startet KoSch Apps?

KoSch fragt startbare Activities über Androids `LauncherApps` ab und benötigt deshalb kein `QUERY_ALL_PACKAGES`. Die Suche läuft lokal. Langer Druck zeigt – sofern die App sie veröffentlicht – Shortcuts, Pin/Unpin und App-Info.

### Werden Arbeitsprofile unterstützt?

Ja. M2.2 liest alle für den HOME-Host zugänglichen `LauncherApps.profiles`, verwendet stabile Benutzer-Seriennummern in App-Schlüsseln, zeigt systemgebadgte Icons und kennzeichnet Work-Apps. Gesperrte oder vom System verborgene Profile werden nicht umgangen.

### Unterstützt KoSch Android Private Space?

Noch nicht vollständig. KoSch deklariert bewusst kein `ACCESS_HIDDEN_PROFILES`. Android verlangt für Private Space unter anderem einen getrennten Container, Verbergen/Anzeigen sowie Sperren/Entsperren ohne Metadatenleck. Erst nach Implementierung und Gerätetests wird diese Capability erwogen.

### Was sind Szenen?

`AI`, `Work`, `Studio`, `Social` und `Evening` sind persistente Arbeitskontexte. Uhrzeit, Akku, validierte Netzverfügbarkeit und Audioausgabe können lokal einen Vorschlag beeinflussen. KoSch schaltet die Szene nicht heimlich um.

### Wie bearbeite ich den Workspace?

Oben auf **EDIT** wechseln. Karten lassen sich verschieben; ein lokaler Layoutvorschlag läuft über **Vorschau → Anwenden/Verwerfen → Undo**. Ein Modell erhält keine direkten Schreibrechte am Workspace Store.

### Was sind Smart Space und Smart-Ordner?

Der Local Core gruppiert Apps transparent anhand von App-Schlüssel, Label und Paketname. Vorschläge werden vor dem Speichern gezeigt. Es werden keine fremden App-Inhalte oder vollständigen Android-Nutzungsverläufe gelesen.

### Wie arbeitet das Smart Dock?

Bis zu fünf Plätze priorisieren manuell gepinnte Apps und ergänzen lokale, über KoSch gestartete Apps sowie szenenbezogene Vorschläge. Langer Druck öffnet Pin/Unpin. Profilzugehörigkeit ist Bestandteil des stabilen App-Schlüssels.

### Welche Seiten gibt es?

Der Home-Bereich besitzt einen frei angeordneten Workspace und einen Smart Space. Wenn ein kompatibler Stift erkannt wird, kommt **Pen Space** als zusätzliche Seite hinzu. Widgets liegen derzeit in einem eigenen Board.

### Wie füge ich Widgets hinzu?

**Kontrollzentrum → Widget +** startet Androids Widget-Auswahl. KoSch persistiert nur erfolgreich gebundene IDs und gibt abgebrochene oder entfernte IDs frei. Freie Platzierung, Resize, Provider-Restore-Mapping, Stacks und transaktionales Undo sind noch offen.

### Was lesen Notification Dots?

Nur Paketname und Anzahl badgefähiger, aktiver Benachrichtigungen werden nach separatem Android-Opt-in prozesslokal gezählt. Titel, Text, Personen, Extras, Aktionen und `RemoteViews` werden weder kopiert noch gespeichert. Ohne Zugriff bleibt der Launcher vollständig funktionsfähig.

### Kann KoSch telefonieren?

KoSch bereitet eine geprüfte Nummer mit `ACTION_DIAL` im System-Wähler vor. Es besitzt kein `CALL_PHONE`, liest keine Kontakte oder Anrufliste und bestätigt keinen Anruf selbst. Notruf- und vollständige Dialer-Funktionen sind ausdrücklich nicht Teil des Launchers.

### Ist KoSch ein vollständiger Dateimanager?

Noch nicht. M2.2 ist ein sicheres Datei-Gateway: Die Person wählt über das Storage Access Framework genau ein Dokument aus. KoSch kann Metadaten und bei bekannten Textformaten höchstens 4.096 Zeichen lokal prüfen, die Datei extern öffnen und seine read-only Freigabe wieder lösen. Löschen, Verschieben und Umbenennen sind nicht implementiert.

### Welche Systemfunktionen bietet das Kontrollzentrum?

Sichtbare Android-Oberflächen für WLAN, Bluetooth, Benachrichtigungszugriff, allgemeine Einstellungen, App-Info, Widgets, Onboarding und HOME-Auswahl. Proprietäre oder nicht vorhandene Einstellungsziele werden abgefangen und als Fehler gemeldet.

### Funktioniert Spracheingabe offline?

KoSch startet Androids installierte Spracherkennungs-Activity. Ob sie offline arbeitet, hängt von Gerät, Sprache und IME/Recognizer ab. KoSch hält keinen dauerhaften Mikrofonstream und deklariert kein eigenes Mikrofonrecht.

## Smartpen und Pen Space

### Wie erkennt KoSch einen Smartpen?

`StylusMonitor` beobachtet `InputManager.InputDeviceListener` und Geräte mit `SOURCE_STYLUS` oder `SOURCE_BLUETOOTH_STYLUS`. Echte Ereignisse werden zusätzlich über `TOOL_TYPE_STYLUS` beziehungsweise `TOOL_TYPE_ERASER` bestätigt. Gerätewechsel werden live sowie beim Resume neu bewertet.

### Was wird nach der Erkennung zusätzlich angeboten?

Pen Space erscheint als weitere Home-Seite und als Schnellaktion im Kontrollzentrum. Die Oberfläche zeigt vorhandene Fähigkeiten wie Druck, Neigung, Hover und Bluetooth transparent an. `⌘ Ask` versteht außerdem lokale Befehle wie „Pen Space öffnen“.

### Was kann Pen Space heute?

Eine lokale Zeichenfläche mit druckabhängiger Breite, Stift, Marker, Hardware-/Software-Radierer, Hover-Cursor, Undo, Leeren und Autosave. Striche sind normalisierte Vektordaten und überleben Größen- und Orientierungswechsel.

### Werden Druck, Neigung, Orientierung, Hover und Stifttasten verarbeitet?

Ja, sofern Android und das Gerät Werte liefern. Druck beeinflusst die Breite; Neigung, Orientierung, Hover, Werkzeugtyp und primäre/sekundäre Stifttaste werden erkannt beziehungsweise angezeigt. KoSch erfindet keine nicht gemeldeten Fähigkeiten.

### Wie verhindert Pen Space Handballen- oder Fingerstriche?

Die Zeichenfläche akzeptiert im M2.2-Modus nur Stylus- und Eraser-Ereignisse. Fingerkontakte werden nicht gezeichnet. Zusätzliche Palm-Rejection des Geräts oder Systems bleibt davon unabhängig.

### Wie werden Zeichnungen gespeichert?

Lokal im versionierten Workspace-Schema v3 als Werkzeug plus normalisierte Punkte mit `x`, `y`, Druck und Neigung. Der Store begrenzt auf 100 Striche und 2.048 Punkte pro Strich. Es gibt keinen Upload, keine Bildkonvertierung und keine versteckte Handschriftanalyse.

### Was passiert, wenn der Stift getrennt wird?

Vorhandene Striche bleiben erhalten. Pen Space meldet den getrennten Zustand und schützt die Fläche weiterhin vor Fingereingabe. Nach erneuter Verbindung aktualisiert der Input-Listener die Fähigkeiten ohne Neustart.

### Unterstützt KoSch Samsung S Pen, USI und Bluetooth-Stifte?

Alle über Androids generische Stylus-Quellen gemeldeten Grundfunktionen sind herstellerneutral nutzbar. Samsung Air Actions oder andere proprietäre Remote-Gesten sind noch nicht Kernbestandteil; dadurch entsteht keine Pflichtabhängigkeit von einem Hersteller-SDK.

### Kann ich handschriftlich in App-Suche und FAQ schreiben?

Auf Android 14 oder neuer kann eine kompatible IME systemweite Stylus-Handschrift in regulären Textfeldern anbieten. KoSch nutzt normale Textfelder, enthält aber keine eigene Handschrifterkennung. Qualität, Sprache und Verfügbarkeit liegen bei der installierten Tastatur.

### Verwendet M2.2 Jetpack Ink?

Noch nicht. M2.2 verwendet eine kleine eigene `PressureInkView`, damit der Launcher ohne zusätzlichen Download und mit klar begrenzter Persistenz funktioniert. Die freie AndroidX-Ink-API bleibt eine Kandidatin für Latenz-, Brush- und Exportverbesserungen nach Gerätebenchmarks.

### Versteht die KI meine Zeichnung bereits semantisch?

Nein. **An Ask** öffnet den lokalen Intent-Eingang, überträgt aber keine Zeichnung. Handschrift-zu-Text, Diagrammverständnis, „Skizze zu Layout“ und pen-basierte Aktionsgesten benötigen ein optionales lokales Modell, explizite Vorschau und Löschkontrollen.

## Modernes Android und Design

### Nutzt KoSch Material You?

Ja. Ab Android 12 übernimmt M2.2 dynamische Systemfarben in ein dunkles Neural-Glass-System. Ältere Geräte erhalten eine kuratierte, kontrastreiche Palette. LCARS ist keine Kernabhängigkeit und kann später als deklaratives Theme entstehen.

### Wie passt sich die Oberfläche an Display und Haltung an?

Die Compose-Shell entscheidet aus aktuellen Fenstermaßen. Kompakte Fenster bleiben gestapelt; breite Fenster verwenden eine geteilte Navigation/Arbeitsfläche, insbesondere ab 840 dp oder ab 720 dp im Querformat. Edge-to-edge-Inset-Schutz bleibt aktiv. Vollständige Foldable-Hinge-, 320-dp- und 200-%-Schrifttests sind noch ein Abnahme-Gate.

### Werden deaktivierte Animationen respektiert?

Ja. KoSch liest Androids Animator-Dauer. Bei deaktivierten Systemanimationen wird Neural Glass statisch und wichtige Zustände bleiben textlich beziehungsweise strukturell erkennbar.

### Welche modernen Android-Funktionen nutzt M2.2 außerdem?

HOME-Rolle, vorausschauende Zurück-Navigation, Edge-to-edge, `LauncherApps`, Multi-Profile-App-Katalog, App-Shortcuts, `AppWidgetHost`, Storage Access Framework, Notification Listener als Opt-in, Activity-Result-Verträge, Android Keystore, dynamische Material-3-Farben und die generische Eingabegeräte-Pipeline.

## KI, freie Modelle und optionale APIs

### Was ist der Local Core?

Ein offline arbeitender Planner für App- und Systembefehle, lokale Suche, Szenen, Dock-/Ordnerlogik, Layoutvorschläge und begrenzte Dateihinweise. Er ist reproduzierbar und bleibt verfügbar, wenn kein Modell, Konto oder Netzwerk existiert.

### Ist bereits ein generatives LLM eingebaut?

Nein. Ein ungefragt gebündeltes Modell würde APK-Größe, RAM, Thermik, Lizenz und Gerätekompatibilität verschlechtern. Die geplante Default-Route ist ein optionaler, klar lizenzierter GGUF-Pack in einer getrennten Service-/Prozessgrenze; bei Ausfall übernimmt der Local Core.

### Welche freien und Open-Source-Varianten sind vorgesehen?

PocketPal AI, ChatterUI und Maid sind sichtbare Übergabeziele. Für eine spätere native Laufzeit werden llama.cpp, LiteRT-LM und MLC LLM evaluiert. Engine- und Modelllizenz werden getrennt behandelt.

### Wann verlassen Eingaben das Gerät?

Erst wenn die Person einen externen Anbieter auswählt und eine sichtbare App-, Share- oder Browserübergabe bestätigt. Der M2.2-Launcher selbst deklariert kein `INTERNET`-Recht und führt keine Modell-API-Anfrage aus.

### Wie können APIs später sicher hinzukommen?

Nur als separater Netzwerk-Flavour beziehungsweise Modul. Vor Aktivierung sind mindestens Kontextvorschau, enge TLS-Host-Policy, Timeout/Abbruch/Backoff, Kostenlimits, logfreie Secrets, Löschung/Rotation, optionale Geräteauthentifizierung, metadatenarmes Audit und Prompt-Injection-/Tool-Output-Evals erforderlich. Der vorbereitete Vault nutzt einen nicht exportierbaren Android-Keystore-Schlüssel und AES-256-GCM mit Provider-Bindung.

### Darf ein LLM Apps bedienen oder Daten löschen?

Nicht direkt. Modelloutput ist untrusted data. Aktionen benötigen eine feste Capability, Risikoklasse und – je nach Wirkung – Auswahl, Vorschau, Bestätigung und Undo. Accessibility-Fernsteuerung, simulierte Berührungen und ein autonomer Lösch-/Kauf-/Sendeagent sind ausgeschlossen.

### Ist PMDD-Wallpaper Teil des Kerns?

Nein. Stimmung, Zyklus oder Fokus können später freiwillige Designparameter eines Theme-/Wallpaper-Programms sein. Gesundheitsnahe Daten dürfen nicht implizit abgeleitet werden; ein generiertes Asset braucht Vorschau, lokale Kontrolle und Rollback.

## Datenschutz und Wiederherstellung

### Welche Android-Berechtigungen fordert M2.2 an?

Als `uses-permission` nur `ACCESS_NETWORK_STATE`, um validierte Netzverfügbarkeit lokal als Kontext zu erkennen. Es gibt kein `INTERNET`, `CALL_PHONE`, `READ_CONTACTS`, `READ_MEDIA_*`, `MANAGE_EXTERNAL_STORAGE`, `QUERY_ALL_PACKAGES`, Standort-, Kalender- oder Mikrofonrecht und keinen Accessibility Service.

### Werden Stift- oder Gerätekennungen gespeichert?

Nein. Persistiert werden nur die begrenzten Vektorstriche. Aktuelle Fähigkeiten, Anzahl erkannter Stifte, Druck, Neigung, Hover und Tastenstatus leben im UI-Zustand; Namen, Vendor-ID, Product-ID und Seriennummer eines Eingabegeräts werden nicht in den Workspace geschrieben.

### Gibt es Backup und Export?

Noch nicht vollständig. `allowBackup=false` verhindert unkontrolliertes App-Daten-Backup. Schema v3 ist migrationsfähig, aber verschlüsselter Export/Import, Dry Run, stale App-Keys und Widget-Restore-Mapping fehlen. Für M2.2 darf keine Sicherungszusage gemacht werden.

### Wie setze ich ein fehlerhaftes Layout zurück?

In **EDIT** auf **Zurücksetzen** tippen oder für den letzten angewendeten Vorschlag Undo verwenden. Dokumentfreigaben und Widget-IDs werden dabei nicht heimlich gelöscht.

### Wie lösche ich alle lokalen Daten?

Bis eine granulare Datenverwaltung existiert, über Android **App-Info → Speicher und Cache → Speicherinhalt löschen** beziehungsweise Deinstallation. Zuvor sollte ein anderer Standard-Launcher gewählt werden. Ein verschlüsselter selektiver Export ist für M2.3 geplant.

## Entwicklung, Prüfung und Benchmark

### Wie wird das Projekt gebaut?

Mit JDK 17, Android SDK 36 und AGP 8.13:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Die Debug-APK entsteht unter `app/build/outputs/apk/debug/app-debug.apk`; GitHub Actions veröffentlicht `kosch-ai-launcher-debug`.

### Was prüft die CI?

Unit-Tests einschließlich FAQ-Registry und Command Planner, Android Lint sowie `assembleDebug`. Der grüne Lauf beweist Buildbarkeit dieses Quellstands, ersetzt aber keine instrumentierten Geräte-, Eingabe-, Accessibility-, Performance- oder OEM-Tests.

### Wie wurde M2.2 mit anderen Launchern verglichen?

Die strenge Matrix bewertet KoSch, Pixel/Android 17 als Systemreferenz sowie Nova, Niagara, Smart Launcher, Microsoft Launcher und Lawnchair in 65 Kategorien von 0,1 bis 10,0. Zusätzlich werden 25 reproduzierbare Fachperspektiven mit je 65 KoSch-Einzelwerten simuliert. Das sind keine tatsächlich befragten Personen und kein identischer Sieben-Geräte-Labortest.

### Wo liegen die vollständigen Bewertungsdaten?

In `COMPETITOR_REVIEW_M2_2.md`, den drei M2.2-CSV-Dateien und der formatierten `launcher_benchmark_m2_2.xlsx` mit Summary, Vergleich, Expertenmatrizen und Quellen.

### Was ist das nächste professionelle Ziel?

M2.3 priorisiert instrumentierte HOME-/SAF-/Widget-/Prozess-Tod-Tests, Macrobenchmarks mit Budgets, vollständige Widget-Engine, verschlüsseltes Backup/Restore, Accessibility- und OEM-Lab, sichere Kontakte über Android 17s Picker sowie erst danach ein isoliertes lokales LLM und einen validierten Capability Planner.
