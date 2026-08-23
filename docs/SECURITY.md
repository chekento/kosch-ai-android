# Sicherheit und Datenschutz

## M1-Datennutzung

KoSch verarbeitet derzeit ausschließlich folgende lokale Informationen:

- Uhrzeit;
- Akkustand und Ladestatus;
- ob eine validierte Netzwerkverbindung verfügbar ist;
- ob ein persönlicher Audioausgang wie Kopfhörer aktiv ist;
- installierte, startbare Launcher-Activities;
- aktive Szene, Kartenpositionen und zuletzt über KoSch gestartete Pakete.

Standort, Kalender, Kontakte, Benachrichtigungsinhalte, Mikrofonstreams und Dateien werden nicht gelesen. Die Android-Spracherkennungs-Activity wird nur nach einem Nutzertipp geöffnet und liefert KoSch anschließend optional einen erkannten Text zurück.

## Berechtigungen

M1 fordert nur `ACCESS_NETWORK_STATE`. Diese Berechtigung verrät dem Context Engine lediglich, ob ein validiertes Netz existiert. Die App besitzt kein eigenes `INTERNET`-Recht und sendet selbst keine Modellanfragen.

Als HOME-App erhält KoSch über `LauncherApps` den vorgesehenen Zugriff auf startbare Activities. Das breite Paketrecht `QUERY_ALL_PACKAGES` wird nicht deklariert.

## Externe Übergaben

Freitext verlässt KoSch erst, nachdem die Person einen konkreten Anbieter gewählt und auf **Teilen** getippt hat. Ein installierter Anbieter erhält ein explizites `ACTION_SEND`; andernfalls öffnet KoSch lediglich dessen HTTPS-Seite. Ab diesem Punkt gelten Datenschutz und Kontoregeln des gewählten Anbieters.

Paketnamen sind Hinweise, keine Garantie. Jede Übergabe ist deshalb fehlertolerant und fällt bei nicht unterstützten Share-Intents sichtbar auf den Browser zurück.

## Nicht im Basissystem

- keine Accessibility Services;
- keine simulierten Berührungen;
- keine Shell-/Root-Ausführung;
- keine ungefragten Hintergrundaktionen;
- keine versteckte Kontrolle fremder Apps;
- keine Secrets in SharedPreferences oder im Repository.

## Anforderungen vor direkten KI-APIs

Vor dem ersten direkten Provider-API-Aufruf müssen vorhanden sein:

1. verschlüsselter Credential Vault mit Android Keystore;
2. pro Provider dokumentierte Datenfelder und Endpunkte;
3. klare Vorschau der übermittelten Kontextdaten;
4. Abbruch, Timeout, Rate-Limit und Offline-Verhalten;
5. lokales Audit Log ohne Prompt-Inhalte als Standard;
6. Lösch- und Exportfunktion;
7. Tests gegen Prompt-Injection in Tool-/Action-Ausgaben.

## Anforderungen vor Automationen

Aktionen erhalten Capability-Tokens mit möglichst kleinem Umfang. Riskante Aktionen benötigen Vorschau und erneute Bestätigung. Themes und Layout-Pakete sind Daten, keine vertrauenswürdigen Plugins, und können ohne separate Genehmigung weder Netzwerk noch Systemaktionen aufrufen.

Sicherheitsprobleme bitte zunächst privat und ohne echte Schlüssel, personenbezogene Daten oder Exploit-Payloads melden.

