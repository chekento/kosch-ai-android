# Sicherheit und Datenschutz

Stand: 29. August 2026 · aktueller Integrationsbranch `0.2.5-alpha01`

Dieses Dokument beschreibt die **aktuelle Sicherheitsgrenze** von KAL. Historische M2.5-Texte, die noch von einem Paket ohne `INTERNET` oder ohne Screen-/Camera-Awareness ausgingen, sind für den aktuellen Integrationsstand nicht mehr maßgeblich. Die maschinenlesbare Capability-Wahrheit liegt in `ReleaseComplianceCatalog`; die Play-/Data-Safety-Sicht in `PLAY_DATA_SAFETY_MATRIX.md`.

## Grundprinzip

KAL ist **local-first, nicht network-free**. Der Launcher-Kern funktioniert ohne Konto, API-Key oder Cloud-Modell. Netzwerkzugriff für direkte AI-Provider ist optional, standardmäßig AUS und darf nur über die explizite Provider-/Privacy-Grenze erfolgen.

Wesentliche Invarianten:

- keine automatische Hintergrund-LLM-Kommunikation;
- keine direkte Provider-Anfrage allein deshalb, weil Android `INTERNET` erlaubt;
- ein direkter Provider muss ausdrücklich verbunden sein;
- Cloud-/Privacy-Gates müssen aktiv sein;
- Nutzerdaten verlassen KAL nur nach einer bestätigten Vordergrundaktion bzw. einer Assistentenaktion, die die vorgesehenen Bestätigungsregeln passiert hat;
- Screen- und Camera-Awareness sind getrennte Opt-ins und werden durch eine Provider-Verbindung nicht automatisch freigegeben;
- LLM-Ausgaben sind Daten, keine Autorität; sie dürfen keine Capability erzeugen oder erweitern.

## Aktuelles Permission-Budget

Die Produktions-App deklariert bewusst:

- `android.permission.INTERNET` – technische Voraussetzung für optionale Direct Provider Connections;
- `android.permission.ACCESS_NETWORK_STATE`;
- `android.permission.CAMERA` – für die separat opt-in geschützte Camera Awareness;
- `android.permission.FOREGROUND_SERVICE`;
- `android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION` – für sichtbare Screen-Awareness-Sessions nach MediaProjection-Consent.

Nicht Teil des vorgesehenen Budgets sind insbesondere:

- `RECORD_AUDIO`;
- Standortrechte;
- `READ_CONTACTS` / `WRITE_CONTACTS`;
- Call-Log-Rechte;
- SMS-Rechte;
- `READ_PHONE_STATE`;
- `QUERY_ALL_PACKAGES`;
- ein Accessibility Service zur Steuerung oder zum Scraping fremder Apps.

CI prüft sowohl den Quellmanifest-Vertrag als auch die tatsächlich paketierten Debug-/Release-APKs. Sensitive Permission-Erweiterungen müssen den Build brechen, statt still einzutreten.

## Direct Provider Connections

`KalCloudAccessPolicy` trennt Androids technische Netzwerkfähigkeit von der Produktfreigabe. Für einen direkten Netzwerkprovider gilt:

1. Providerprofil muss bekannt sein.
2. Cloud Access / AI Provider Networking muss aktiviert sein.
3. die breitere Privacy-/Network-Freigabe muss aktiviert sein.
4. der Provider muss ausdrücklich verbunden sein.
5. `BACKGROUND`-Requests werden abgelehnt.
6. bei Nutzerinhalt muss die entsprechende Inhaltsfreigabe sichtbar behandelt werden.

Lokale Routen mit `networkBoundary = NONE` benötigen keine Cloudfreigabe.

### OpenRouter

OpenRouter ist der erste direkt implementierte OAuth-/Inference-Pfad:

- Authorization Code + PKCE/S256;
- zufälliger Code Verifier und zufälliger Session-/Callback-Pfad;
- einmaliger Callback-Listener ausschließlich auf `127.0.0.1` und einem zufälligen Port;
- begrenzte Callback-, Request- und Response-Größen;
- Netzwerk- und Read-Timeouts;
- kein wiederverwendbares OAuth-Client-Secret im APK;
- HTTPS für Key Exchange und Inference;
- Provider-Request erst nach den KAL-Cloud-Gates;
- OpenRouter-Request setzt `provider.data_collection = deny` und sendet keinen Prompt im Hintergrund.

Ein lokaler fremder Prozess könnte einen Loopback-Callback höchstens vorzeitig stören; der zufällige Pfad und PKCE verhindern, dass daraus eine gültige KAL-Autorisierung oder ein verwendbarer Schlüssel entsteht. OAuth-/Loopback-Handling bleibt trotzdem Teil des unabhängigen Security-/Fuzzing-Gates vor öffentlichem Release.

### Andere Provider / eigene Endpoints

Providerprofile dürfen Capability und Reifegrad getrennt ausdrücken. `SUPPORTED` bedeutet nicht, dass jede Organisation ohne weitere Konfiguration sofort eingeloggt werden kann. OAuth-Client-/Tenant-Konfigurationen bleiben dort `CONFIGURATION_REQUIRED`, wo dies providerseitig nötig ist.

Remote-Endpunkte müssen HTTPS verwenden. Unverschlüsseltes HTTP ist nur für explizit erlaubte Loopback-Fälle vorgesehen. Eigene OpenAI-kompatible Endpoints sind nutzer-/organisationsgesteuert; Host, Modell und Authentisierung dürfen nicht still geraten werden.

## Credential Vault

Provider-Secrets werden gerätelokal im `SecureCredentialVault` gehalten:

- nicht exportierbarer AES-256-Schlüssel aus `AndroidKeyStore`;
- `AES/GCM/NoPadding` mit zufälliger IV;
- Associated Data bindet Ciphertext an Provider-ID und Credential-Typ;
- getrennte Typen für API-Key, Access Token, Refresh Token, OAuth-generierten Key und ID Token;
- portable KAL-Backups enthalten diese Werte nicht;
- Disconnect/Delete entfernt die von KAL gehaltenen Provider-Credentials;
- mutable Secret-Puffer werden nach Verwendung bestmöglich überschrieben.

Android-App-Backup ist für KAL deaktiviert; gerätegebundene Credentials, Widget-Host-IDs, URI-Grants und Capture-Zustände gehören nicht in portable Daten.

## Screen Awareness

Screen Awareness ist standardmäßig AUS.

- Androids MediaProjection-Systemdialog bleibt Autorität.
- Die Session läuft über einen sichtbaren Foreground Service vom Typ `mediaProjection`.
- Der Service ist nicht exportiert.
- Screen-Inhalt wird nicht automatisch durch eine AI- oder Provider-Verbindung freigegeben.
- Eine externe/direkte AI-Verarbeitung benötigt zusätzlich die dafür vorgesehene Kontext-/Provider-Freigabe.
- Es gibt keine versteckte dauerhafte Hintergrundbeobachtung.

## Camera Awareness

Camera Awareness ist standardmäßig AUS.

- Runtime-Camera-Permission und sichtbare CameraX-Session sind erforderlich.
- KAL erfasst Kontext nur nach dem vorgesehenen Nutzer-Opt-in/-Request.
- Kamera-Inhalt wird nicht automatisch persistiert oder an einen Provider gesendet.
- Eine Provider-Verbindung ersetzt niemals den Camera-Consent.

## Audio / Wake Word

Der aktuelle Permission-Vertrag enthält bewusst **kein `RECORD_AUDIO`**. Sprachinteraktion darf daher nicht durch einen heimlichen permanenten Mikrofonstream entstehen. Solange ein späteres lokales Wake-Word-Modul noch nicht als eigene Capability mit sichtbarem Opt-in, Mikrofonindikator, Retention-Regeln, Tests und Store-Offenlegung umgesetzt ist, darf `RECORD_AUDIO` nicht still in das Paket gelangen.

## Notification Dots

Notification-Zugriff ist ein separates Android-Special-Access-Opt-in. Die produktive Badge-Grenze verarbeitet nur paketbezogene Zähler/Metadaten im Prozesszustand. Titel, Text, Personen, Extras, Aktionen und RemoteViews werden nicht als Badge-Datenmodell übernommen und nicht im Hintergrund an AI-Provider weitergegeben.

Eine spätere inhaltliche Notification-Zusammenfassung wäre eine neue sensitive Capability und müsste eigene Auswahl-, Vorschau-, Redaction-, Provider- und Retention-Gates besitzen.

## Telefon, Nachrichten und Kontakte

- Telefon nutzt `ACTION_DIAL`; KAL führt keinen versteckten Anruf aus.
- Nachrichten nutzen `ACTION_SENDTO`/sichtbare Zieloberflächen; KAL sendet keine SMS selbst.
- Kontakte werden über einmalige Android-Auswahl statt `READ_CONTACTS` eingebunden.
- Telefonnummern gehören nicht automatisch in AI-Prompts oder Audit-Freitext.
- KAL übernimmt keine Default-Dialer-, `InCallService`- oder Notruffunktion.

## Dateien und SAF

KAL arbeitet mit vom Nutzer ausgewählten Dateien bzw. SAF-Bäumen statt mit Vollspeicherrechten.

- keine `MANAGE_EXTERNAL_STORAGE`-Anforderung;
- Provider-/Tree-Grenzen werden respektiert;
- destruktive Mutationen benötigen die vorgesehenen Bestätigungen;
- ein erfolgreicher Provider-Effekt darf nicht durch einen nachfolgenden Refreshfehler fälschlich als fehlgeschlagen dargestellt werden;
- Cloud-SAF-Provider können Daten nach ihren eigenen Regeln laden – „KAL lädt nicht selbst hoch“ bedeutet nicht, dass ein gewählter Dokumentprovider rein lokal arbeitet;
- kein impliziter Malware-Scan und keine Garantie über Papierkorb-/Provider-Verhalten.

## Portables Backup und Restore

Portable Workspace-/Settings-Backups sind versioniert, validiert und von gerätegebundenen Secrets getrennt. Bestehende verschlüsselte Backup-Pfade nutzen PBKDF2-HMAC-SHA-256 und AES-256-GCM mit Authentizitätsprüfung. Restore-Flows benötigen Dry Run/Validierung und dürfen ungültige, übergroße oder manipulierte Daten nicht übernehmen.

Aus portablen Backups ausgeschlossen bleiben insbesondere:

- Provider-Secrets und OAuth-Tokens;
- Capture-/MediaProjection-Grants;
- persistierte URI-Grants;
- Widget-Host-IDs bzw. andere gerätegebundene Bindings;
- Notification-Inhalte;
- lokale Audit-Daten, soweit der definierte Backupvertrag dies ausschließt.

## Lokales Audit

Das Audit besitzt bewusst keinen freien Prompt-/Detailkanal. Es speichert begrenzte Aktions-/Ergebnis-Metadaten und ist vom portablen Workspace-Backup getrennt. Prompts, Telefonnummern, Kontaktwerte, Dateiinhalte und Provider-Secrets dürfen nicht über eine generische Detailspalte einsickern.

## Home, Settings und Automationen

Launcher-Änderungen folgen einer Capability-Grenze:

| Klasse | Beispiele | Regel |
|---|---|---|
| A – lesend/reversibel | App suchen, lokalen Vorschlag erzeugen | direkte lokale Ausführung möglich |
| B – externe Ansicht | Dialer, Datei öffnen, Einstellungen | sichtbares Ziel / Android-UI |
| C – schreibend/reversibel | Layout, Theme, Dateiname | Vorschau, Bestätigung und Undo, wo technisch ehrlich |
| D – Kommunikation/Kosten/Provider | Nachricht, kostenpflichtige API-Anfrage | Kontext-/Zielvorschau + explizite Bestätigung |
| E – destruktiv/privilegiert | Löschen, Geräte-/Rollenwechsel | separate Capability, kein autonomer Default |

Temporärer Kontext darf persistente Nutzerentscheidungen nicht still überschreiben. Globale, Page- und Object-Settings müssen ihre Vererbung explizit behalten.

## N1 VPN-Prototyp

`KoSchConsentVpnService` und `SecurityNetworkActivity` sind Entwicklungskomponenten und **debug-only**. Im aktuellen N1-Zustand findet keine reale Traffic-Verarbeitung statt.

Release-Gate:

- Release-APK darf weder den N1-`VpnService` noch die N1-Security-Activity enthalten;
- gewöhnliche HTTPS-Providerverbindungen rechtfertigen keinen Produktions-`VpnService`;
- ein späteres echtes VPN-/Firewall-Produktfeature wäre eine eigene regulatorische, Play-, Security- und Teststufe.

## Externe AI-App-Handoffs

Ein Android-Handoff und eine Direct Provider Connection sind zwei unterschiedliche Datenwege:

- **App-Handoff:** KAL übergibt nach Nutzeraktion Text/Kontext an eine ausgewählte Ziel-App; die weitere Verarbeitung liegt bei dieser App.
- **Direct Provider:** KAL selbst überträgt nach den Provider-/Cloud-Gates Inhalt über HTTPS an den verbundenen Provider.

UI, Privacy Policy und Play Data Safety dürfen diese Wege nicht vermischen. Ein Fehler eines Share-/App-Intents darf nicht heimlich zu einer direkten Provider- oder Browser-Übertragung mit sensiblen Daten führen.

## Release- und Play-Gate

Vor jedem öffentlichen Release:

1. Quellmanifest und gemergtes Release-Manifest prüfen.
2. Paketierte Permissions mit `aapt` verifizieren.
3. bestätigen, dass N1-VPN-Komponenten im Release fehlen.
4. `ReleaseComplianceCatalog`, `PLAY_DATA_SAFETY_MATRIX.md`, öffentliche Privacy Policy und Play Console Data Safety gegeneinander abgleichen.
5. frische Installation testen: Cloud Access AUS, keine Provider-Credentials, keine Screen-/Camera-Session.
6. Connect → Request → Disconnect inklusive Offline-, Timeout-, Abbruch- und Prozess-Tod-Fällen testen.
7. sicherstellen, dass portable Backups keine Credentials oder Capture-Grants enthalten.
8. Dependency-/SBOM-Scan sowie unabhängigen Security-/Privacy-Review durchführen.
9. OAuth-/Loopback-, SAF-/Restore- und Parser-Grenzen fuzz-/negativtesten.
10. erst danach Release-Signing/AAB/Upgrade-/Rollback- und Play-Policy-Abnahme durchführen.

## Marketing-sichere Aussage

Zulässig ist beispielsweise:

> KAL arbeitet local-first. Der Launcher-Kern benötigt keinen Cloud-Provider. Optionale direkte AI-Provider-Verbindungen sind standardmäßig deaktiviert und werden erst nach ausdrücklicher Verbindung, Cloud-/Privacy-Freigabe und bestätigter Vordergrundaktion genutzt. Screen und Camera Awareness bleiben separate Opt-ins.

Nicht zulässig ist für den aktuellen Stand die pauschale Aussage:

> Alles bleibt immer auf dem Gerät.

Sobald der Nutzer bewusst einen externen AI-Handoff oder einen direkt verbundenen Provider verwendet, können ausgewählte Daten das Gerät verlassen.
