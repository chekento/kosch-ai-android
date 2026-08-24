package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.FaqCategory
import cloud.kosch.aiandroid.model.FaqEntry
import java.text.Normalizer
import java.util.Locale

object FaqRegistry {
    val entries = listOf(
        faq("first-start", FaqCategory.START, "Funktioniert KoSch schon beim ersten Öffnen?", "Ja. App-Start, Suche, Szenen, Smart Dock, Smart-Ordner, Telefonübergabe, Dateiprüfung und Systemaktionen laufen im Local Core ohne Konto, API-Schlüssel oder Modell-Download.", "offline api frei local core"),
        faq("default-home", FaqCategory.START, "Wie mache ich KoSch zum Standard-Launcher?", "Tippe im Onboarding oder im Hinweis auf „Festlegen“. KoSch öffnet ausschließlich Androids geschützten HOME-Rollendialog; die Entscheidung bleibt beim System und bei dir.", "home rolle start app"),
        faq("escape", FaqCategory.START, "Wie komme ich im Notfall zu einem anderen Launcher?", "Öffne Kontrollzentrum → Sicherheitsausgang → „Anderen Launcher wählen“. Das führt direkt zu Androids Start-App-Auswahl; als Fallback werden die Android-Einstellungen geöffnet.", "notausgang crash launcher auswahl"),
        faq("reopen-tour", FaqCategory.START, "Kann ich die Einführung erneut ansehen?", "Ja. Im Kontrollzentrum findest du „Einführung erneut ansehen“. Deine Workspace-Daten werden dadurch nicht gelöscht.", "onboarding tour"),
        faq("apps", FaqCategory.LAUNCHER, "Wie findet und startet KoSch Apps?", "KoSch nutzt LauncherApps statt einer breiten Paketabfrage. Tippe auf Apps, suche lokal und tippe zum Starten. Ein langer Druck öffnet veröffentlichte App-Shortcuts, Pinning und App-Info.", "drawer launcherapps shortcuts"),
        faq("scenes", FaqCategory.LAUNCHER, "Was sind Szenen?", "AI, Work, Studio, Social und Evening sind getrennte, persistente Arbeitskontexte. Der Local Core kann anhand von Uhrzeit, Akku, Verbindung und Audioausgabe eine Szene vorschlagen, schaltet sie aber nie heimlich um.", "szene kontext"),
        faq("layout", FaqCategory.LAUNCHER, "Wie bearbeite ich das Layout?", "Wechsle oben in EDIT, ziehe Karten oder erzeuge eine lokale Layout-Vorschau. Vorschläge müssen angewendet werden, können verworfen und anschließend rückgängig gemacht werden.", "edit ziehen undo vorschau"),
        faq("smart-space", FaqCategory.LAUNCHER, "Was macht Smart Space?", "Smart Space gruppiert Apps deterministisch und lokal nach Paketname und Label. Vorschläge werden vor dem Speichern gezeigt. Inhalte oder Nutzungsverläufe anderer Apps werden nicht gelesen.", "ordner collections"),
        faq("dock", FaqCategory.LAUNCHER, "Wie arbeitet das Smart Dock?", "Bis zu fünf Plätze kombinieren deine Pins, lokale zuletzt gestartete Apps und die aktive Szene. Langer Druck auf eine App öffnet Pin/Unpin. Benachrichtigungspunkte sind separat opt-in.", "dock pin recent"),
        faq("widgets", FaqCategory.LAUNCHER, "Wie füge ich Widgets hinzu und ändere ihre Größe?", "Kontrollzentrum → Widget + öffnet Androids Widget-Auswahl. KoSch speichert erst erfolgreich gebundene IDs, gibt abgebrochene IDs frei und bietet die validierten Presets Kompakt, Standard und Hoch. Provider-Restore-Mapping zwischen Geräten bleibt absichtlich ausgeschlossen.", "appwidget board resize größe"),
        faq("phone", FaqCategory.LAUNCHER, "Kann KoSch telefonieren?", "KoSch öffnet per ACTION_DIAL den System-Wähler und kann eine geprüfte Nummer vorausfüllen. Es besitzt kein CALL_PHONE-Recht; du bestätigst den Anruf selbst. Eine Nummer kann einmalig über Androids Kontakt-Picker gewählt werden, ohne globales READ_CONTACTS.", "dialer anruf telefon kontakt"),
        faq("files", FaqCategory.LAUNCHER, "Was kann die Datei-KI?", "Du wählst genau ein Dokument über Androids Storage Access Framework. KoSch liest lokal Metadaten und höchstens 4.096 Zeichen erkannter Textformate, erstellt Hinweise und kann die gespeicherte read-only Freigabe wieder lösen.", "saf dateimanager dokument"),
        faq("ask", FaqCategory.AI, "Was versteht ⌘ Ask ohne LLM?", "Der lokale Planner versteht unter anderem App-Start, Apps, Telefon, Kontakt, Datei, Widgets, Backup, Audit, Pro Desk, WLAN, Bluetooth, Einstellungen, Szenen, Pen Space, FAQ und den Sicherheitsausgang. Unbekannte Texte landen erst in der bewussten Anbieterauswahl.", "command befehl prompt backup audit"),
        faq("ai-routing", FaqCategory.AI, "Wann verlassen Daten das Gerät?", "Nur wenn du einen externen Anbieter auswählst und eine sichtbare App-, Share- oder Browserübergabe auslöst. Der Launcher selbst besitzt in dieser Variante kein INTERNET-Recht.", "cloud teilen datenschutz"),
        faq("open-source", FaqCategory.AI, "Welche freien KI-Varianten sind vorgesehen?", "PocketPal AI, ChatterUI und Maid sind freie Übergabeziele. llama.cpp, LiteRT-LM und MLC LLM sind als lokale Laufzeitrouten dokumentiert. Ein natives generatives Modell ist noch nicht ungefragt gebündelt.", "opensource lokal llama gguf"),
        faq("api-keys", FaqCategory.AI, "Wie werden spätere API-Schlüssel geschützt?", "Direkte APIs sind derzeit deaktiviert. Vorbereitet sind ein nicht exportierbarer Android-Keystore-Schlüssel, AES-GCM, Provider-Bindung und eine HTTPS-/Loopback-Policy. Eine spätere Netzwerkvariante muss zusätzlich Vorschau, Löschung, Rotation, Audit und Geräteauthentifizierung liefern.", "key keystore sicherheit"),
        faq("pro-desk", FaqCategory.PROFESSIONAL, "Was ist Pro Desk?", "Pro Desk ist der neue professionelle Standardbereich: Sicherheitslage, HOME-Status, zugängliche Work-Apps, lokale Audit-Anzahl, zuletzt relevante Apps und die wichtigsten Arbeitsaktionen sind ohne Cloud auf einer adaptiven Fläche gebündelt.", "dashboard professional arbeit command center"),
        faq("keyboard", FaqCategory.PROFESSIONAL, "Welche Hardware-Tastaturbefehle gibt es?", "Ctrl oder Meta plus K fokussiert die Command Bar; Leertaste öffnet Apps; H Pro Desk; Komma das Kontrollzentrum; D Telefon; O Datei-KI; B Backup; L Audit; Shift+P Pen Space. Androids Shortcut-Hilfe zeigt die Liste systemweit an; Escape schließt die oberste KoSch-Fläche.", "shortcut keyboard tastatur ctrl meta escape"),
        faq("contact-picker", FaqCategory.PROFESSIONAL, "Wie funktioniert die sichere Kontaktauswahl?", "KoSch startet ACTION_PICK ausschließlich für Telefonnummern und fordert kein READ_CONTACTS. Auf Android 17 wird der datenschutzfreundliche System-Picker ausdrücklich bevorzugt; ältere Geräte geben nur die gewählte Telefonzeile temporär frei. Name und Nummer werden nicht persistiert oder auditiert.", "android 17 contacts picker work profile"),
        faq("secure-backup", FaqCategory.PROFESSIONAL, "Wie exportiere ich ein verschlüsseltes Workspace-Backup?", "Pro Desk oder Kontrollzentrum → Backup. Eine Passphrase mit mindestens 12 Zeichen verschlüsselt den versionierten Snapshot mittels PBKDF2-HMAC-SHA-256 und AES-256-GCM. Die Passphrase wird nicht gespeichert; ohne sie ist das Backup absichtlich nicht wiederherstellbar.", "aes gcm pbkdf2 export sicherung"),
        faq("restore", FaqCategory.PROFESSIONAL, "Wie sicher ist ein Restore?", "Import lädt nur eine begrenzte Datei, entschlüsselt sie lokal, prüft Format, Version, Größen, Enums, App-Schlüssel, Positionen und Stiftwerte und zeigt eine Vorschau. Erst eine zweite ausdrückliche Bestätigung ersetzt Layout, Verlauf, Pins, Ordner und Pen-Inhalt atomar.", "restore import validierung vorschau atomar"),
        faq("backup-exclusions", FaqCategory.PROFESSIONAL, "Was ist nicht im Backup enthalten?", "Widget-Host-IDs, Storage-Access-Freigaben, Zugangsdaten, Notification-Daten und das lokale Audit sind geräte- oder sicherheitsgebunden und werden nicht exportiert. Widgets müssen nach einem Gerätewechsel neu hinzugefügt werden.", "widget id grant credentials ausgeschlossen"),
        faq("audit", FaqCategory.PROFESSIONAL, "Was speichert das lokale Audit?", "Nur Zeitpunkt, einen fest definierten Aktionstyp und das Ergebnis – maximal 250 Ereignisse für 90 Tage. Das Schema besitzt absichtlich kein Freitextfeld: keine Prompts, Kontakte, Nummern, Dateinamen, Pfade, App-Inhalte oder Benachrichtigungstexte.", "audit datenschutz metadaten retention"),
        faq("audit-control", FaqCategory.PROFESSIONAL, "Kann ich das Audit exportieren oder löschen?", "Ja. Die Audit-Fläche exportiert eine lokale CSV mit drei Spalten oder löscht nach einer zweiten Bestätigung alle Ereignisse. Ein Backup nimmt das Audit nicht mit.", "csv export löschen audit"),
        faq("pen-detection", FaqCategory.PEN, "Wie erkennt KoSch einen Smartpen?", "KoSch beobachtet Android InputManager und SOURCE_STYLUS/SOURCE_BLUETOOTH_STYLUS. Bei echten Ereignissen prüft es zusätzlich TOOL_TYPE_STYLUS oder TOOL_TYPE_ERASER. Ein erkannter Stift schaltet Pen Space und zusätzliche Schnellaktionen frei.", "smartpen s pen usi bluetooth"),
        faq("pen-space", FaqCategory.PEN, "Was bietet Pen Space?", "Pen Space ist eine lokale, druckempfindliche Zeichenfläche mit Stift, Marker, Radierer, Hover-Vorschau, Undo und Löschen. Striche werden normalisiert und lokal gespeichert, damit Größen- und Orientierungswechsel sie nicht zerstören.", "zeichnen notiz druck marker radierer"),
        faq("pen-pressure", FaqCategory.PEN, "Werden Druck und Neigung genutzt?", "Wenn das Gerät sie liefert, beeinflusst Druck die Strichbreite; Neigung, Orientierung, Hover und Stifttasten werden live erkannt und transparent angezeigt. Fehlende Sensorwerte werden nicht erfunden.", "pressure tilt orientation hover button"),
        faq("handwriting", FaqCategory.PEN, "Kann ich in Suchfeldern handschriftlich schreiben?", "Auf Android 14 oder neuer kann eine kompatible IME die systemweite Stylus-Handschrift in regulären Textfeldern bereitstellen. KoSch behauptet keine eigene Handschrifterkennung; Verfügbarkeit und Erkennung liegen bei deiner Tastatur/IME.", "ime handwriting gboard"),
        faq("s-pen", FaqCategory.PEN, "Unterstützt KoSch Samsung S Pen?", "Grundfunktionen eines S Pen laufen über Androids generische Stylus-Events. Herstellergebundene Air Actions oder Remote-Gesten sind derzeit kein Kernbestandteil, damit KoSch portabel und ohne Samsung-SDK-Abhängigkeit bleibt.", "samsung air actions"),
        faq("palm", FaqCategory.PEN, "Wie verhindert Pen Space Handballen-Striche?", "Sobald ein Stift erkannt ist, zeichnet die Fläche nur Stylus-/Eraser-Ereignisse und ignoriert Fingerkontakte. System- und Geräte-Palm-Rejection können zusätzlich helfen.", "palm rejection finger"),
        faq("profiles", FaqCategory.ANDROID, "Unterstützt KoSch Arbeitsprofile?", "Ja. KoSch fragt alle über LauncherApps zugänglichen Profile ab, verwendet systemgebadgte Icons und kennzeichnet Arbeitsprofil-Apps. Gesperrte oder nicht zugängliche Profile werden weder umgangen noch durchsucht.", "work managed profile badge"),
        faq("private-space", FaqCategory.ANDROID, "Wie ist Android Private Space behandelt?", "KoSch fordert ACCESS_HIDDEN_PROFILES noch nicht an. Android verlangt dafür einen getrennten, ausblendbaren und sperr-/entsperrbaren Container. Bis diese Schutzlogik vollständig implementiert und getestet ist, bleibt Private Space absichtlich dem System-Launcher vorbehalten.", "hidden private space android 15"),
        faq("dynamic-color", FaqCategory.ANDROID, "Nutzt KoSch Material You?", "Ja. Auf Android 12 oder neuer übernimmt KoSch dynamische Gerätefarben in sein dunkles Neural-Glass-System; ältere Geräte erhalten eine kuratierte, kontrastreiche Farbpalette.", "material you dynamic color"),
        faq("adaptive", FaqCategory.ANDROID, "Funktioniert die Oberfläche auf Tablets und im Querformat?", "M2.2 passt die Shell an die aktuelle Fensterbreite an: kompakte Geräte nutzen eine gestapelte Oberfläche, größere und breite Fenster trennen Navigation und Arbeitsfläche. Edge-to-edge-Inset-Schutz bleibt aktiv.", "tablet foldable landscape adaptive"),
        faq("motion", FaqCategory.ANDROID, "Was passiert bei deaktivierten Animationen?", "KoSch respektiert Androids Animator-Dauer. Ist Bewegung systemweit deaktiviert, bleibt Neural Glass statisch und wichtige Zustände werden nicht nur durch Animation vermittelt.", "reduced motion barrierefreiheit"),
        faq("notifications", FaqCategory.ANDROID, "Was lesen Benachrichtigungspunkte?", "Nur Paketname und Anzahl badgefähiger aktiver Meldungen werden prozesslokal gezählt. Titel, Text, Personen, Extras und Aktionen werden weder kopiert noch gespeichert. Ohne Opt-in bleibt die Funktion aus.", "notification dots privacy"),
        faq("pen-missing", FaqCategory.TROUBLESHOOTING, "Mein Stift wird nicht erkannt – was nun?", "Verbinde oder entnehme den Stift erneut und öffne das Kontrollzentrum. KoSch aktualisiert Eingabegeräte live und beim Resume. Prüfe außerdem, ob Android den Stift als Eingabegerät meldet; proprietäre Fernbedienfunktionen allein gelten nicht als Zeichenstift.", "stift fehlt troubleshooting"),
        faq("app-missing", FaqCategory.TROUBLESHOOTING, "Eine App oder ein Shortcut fehlt", "Entsperre gegebenenfalls das Arbeitsprofil, warte auf die Paketaktualisierung und öffne den App-Raum erneut. KoSch zeigt nur startbare Activities und veröffentlichte Shortcuts, auf die Android dem aktuellen HOME-Host Zugriff gibt.", "refresh shortcut work profile"),
        faq("reset", FaqCategory.TROUBLESHOOTING, "Wie setze ich ein fehlerhaftes Layout zurück?", "Wechsle in EDIT und tippe auf Zurücksetzen. Für einen vorherigen Schritt steht Undo bereit. Widgets und Dokumentfreigaben werden dadurch nicht heimlich gelöscht.", "recovery layout"),
        faq("backup", FaqCategory.TROUBLESHOOTING, "Was tue ich, wenn ein Backup nicht geöffnet wird?", "Prüfe Passphrase und Dateityp. Manipulierte Daten, falsche Passphrasen, unbekannte Versionen, zu große Dateien oder ungültige Workspace-Werte werden vor jeder Änderung abgelehnt. Schließe die Fläche und wähle die Datei erneut; das vorhandene Workspace bleibt bei einem Fehler unverändert.", "export import backup restore fehler"),
    )

    fun search(query: String, category: FaqCategory? = null): List<FaqEntry> {
        val needle = query.searchKey()
        return entries.filter { entry ->
            (category == null || entry.category == category) &&
                (needle.isBlank() || entry.searchText().contains(needle))
        }
    }

    private fun FaqEntry.searchText(): String =
        listOf(question, answer, keywords.joinToString(" "), category.title).joinToString(" ").searchKey()

    private fun String.searchKey(): String = Normalizer
        .normalize(lowercase(Locale.GERMAN), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .replace("[^a-z0-9]+".toRegex(), " ")
        .trim()

    private fun faq(
        id: String,
        category: FaqCategory,
        question: String,
        answer: String,
        keywords: String,
    ) = FaqEntry(id, category, question, answer, keywords.split(' '))
}
