# KoSch AI Launcher – Settings Center Architecture

## Ziel

Das Settings Center ist kein kleines Android-Einstellungsfenster, sondern die zentrale Konfigurationsoberfläche des Launchers. Nahezu jede relevante sichtbare oder funktionale Eigenschaft soll bewusst einstellbar sein, ohne die tägliche Bedienung zu überfrachten.

Dafür gelten vier Regeln:

1. **Progressive Disclosure:** einfache, häufige Optionen zuerst; Detailoptionen in Unterseiten/Advanced-Gruppen.
2. **Vererbung statt Duplikation:** globaler Standard → Seiten-Override → Objekt-Override.
3. **Preview vor destruktiver Änderung:** Raster, Theme, Layout, Icon Pack und größere visuelle Änderungen zeigen eine Vorschau mit Apply/Discard/Undo.
4. **Secrets bleiben separat:** API Keys, OAuth Tokens, Android Widget Host IDs, URI Grants und andere Geräte-Capabilities sind nie Teil eines portablen Settings-Exports.

## Informationsarchitektur – Haupttabs

### 1. Home & Raster

- Rasterspalten und Rasterzeilen
- getrennte Portrait-/Landscape-/Tablet-/Foldable-Raster
- horizontaler/vertikaler Abstand
- Innenabstand der Seiten
- freie Platzierung vs. Snap-to-grid
- Icon-Größe und Beschriftungsmodus
- Layout sperren
- Auto-Fill leerer Zellen
- Seitenindikator
- Status-/Command-/Assistant-Zonen ein-/ausblenden
- Safe-Area Verhalten
- Rastervorschau und Reflow-Dry-Run

### 2. Seiten & Räume

- Seite erstellen, löschen, duplizieren, umbenennen
- Reihenfolge und Standardseite
- Seite verstecken/einblenden
- Seiten-Looping
- Seitenübergang und Dauer
- letzte Seite merken
- pro Seite eigenes Raster
- pro Seite eigener Hintergrund / Theme-Override
- pro Seite Assistant-Sichtbarkeit
- Fokus-/Work-/Studio-/Social-Profile
- Seitenexport und Seitenimport

### 3. Apps & Drawer

- Drawer-Raster
- Sortierung: Smart, A–Z, häufig, zuletzt, manuell
- Labels, Work-Badges, Notification-Badges
- System-Apps ein-/ausblenden
- versteckte Apps verwalten
- Icon-Größe
- App-Kategorien
- Suchindex-Quellen
- App-Aktionsmenü konfigurieren
- Standardaktion bei Long Press / Double Tap

### 4. Dock & Schnellzugriff

- Dock an/aus
- Anzahl Slots
- fixe vs. adaptive Slots
- Icon-Größe
- Ask/Assistant Button
- Hintergrund, Transparenz, Blur
- Position oben/unten/seitlich bei großen Displays
- pro Seite eigenes Dock
- Gesten auf Dock-Icons
- dynamische Vorschläge

### 5. Ordner & Smart Groups

- Raster und Darstellung
- Sheet, Popup oder Fullscreen
- Labels
- Smart-Ordner an/aus
- manuelle vs. intelligente Sortierung
- Auto-Close nach App-Start
- Ordner-Icon / Icon-Stack
- Ordnergesten
- eigene Hintergründe / Farben
- Unterordner optional

### 6. Widgets & Stacks

- Standardgröße
- freie Größenänderung
- Provider-Größenhinweise
- Widget-Stacks
- Stack-Umschaltung per Swipe/Tap/Auto-Cycle
- Stack-Intervall
- Missing-/Remap-Darstellung
- Widget-Haptik
- Aktualisierungs-/Batteriehinweise
- pro Widget Rand, Hintergrund, Clip, Corner Radius
- Widget-Backup-Mapping ohne appWidgetId

### 7. Darstellung

- Light/Dark/System/Theme
- Material You Akzente
- Blur
- Transparenz
- Corner-Radius-Skalierung
- UI-/Content-Skalierung
- Tiefeneffekt
- Schatten/Elevation
- Motion Profile
- Parallax
- Haptik-Profil
- Animationen einzeln aktivieren/deaktivieren
- Wallpaper-Modus

### 8. Themes, Import & Export

- Theme-Auswahl
- Neural Glass / Minimal Work / Living AI / Cyberdeck / LCARS optional
- Theme-Vorschau
- Theme importieren
- Theme exportieren
- Layout im Export ja/nein
- Wallpaper im Export ja/nein
- Assistant Assets im Theme erlauben ja/nein
- Sounds im Theme erlauben ja/nein
- Theme-Capability-Manifest
- Rollback auf vorheriges Theme
- eigenes Theme duplizieren und bearbeiten

### 9. Assistent

- komplett an/aus
- Charakter/Assistant Pack wählen
- Position: links/mitte/rechts/frei
- Größe
- Transparenz
- nur auf bestimmten Seiten sichtbar
- Spawn-/Portal-Animation
- Idle Motion
- Gaze Tracking
- Augen-/Blink-Animationen
- Emotionen
- Viseme/Lip-Sync
- Live Chat
- Voice Input
- Speech Output
- Kontextuelles Auftauchen
- manuelles Wake-Verhalten
- Assistent darf Vorschläge machen ja/nein
- Assistent darf Layout nur vorschlagen, niemals ungefragt verändern
- Reduced-Motion-spezifisches Assistant-Verhalten
- Assistant Asset Pack Import/Export

### 10. KI & Modelle

- Local-first / Ask every time / Default Provider
- lokaler Command Planner
- lokales Modell an/aus
- lokale Modellverwaltung
- Provider-Reihenfolge
- Modell pro Aufgabe
- Modell pro Assistant
- Modell pro Seite / Workspace optional
- Kontextquellen einzeln aktivieren
- Dateikontext nur explizit freigegeben
- lokale Retrieval-/Embedding-Funktionen
- Kontextvorschau vor Übergabe

### 11. APIs & Provider

- Provider aktivieren/deaktivieren
- Transport: App, Web, OpenAI-kompatibel, Custom HTTP, Local Runtime
- Endpoint
- Model ID
- Credential Alias / Vault Slot
- Verbindung testen
- API Key setzen/rotieren/löschen über Vault-UI
- Netzwerknutzung global an/aus
- Provider darf Kontext standardmäßig erhalten ja/nein
- Timeout/Retry-Budgets
- per Provider Datenvorschau
- Provider-spezifische Limits

**Wichtig:** Exportiert werden nur Provider-Konfiguration und Vault-Alias. Niemals der Secret-Wert.

### 12. Sprache & Audio

- Voice Input
- Speech Output
- Sprache/Locale
- Speech Rate
- Pitch
- Stimme/TTS Engine
- unterbrechbare Ausgabe
- Viseme Sync
- Voice Button Verhalten
- Audio-Fokus
- Bluetooth-/Headset-Präferenzen
- UI Sounds
- Assistant Sounds

### 13. Gesten & Eingabe

- Swipe Up/Down/Left/Right
- Double Tap
- Long Press
- Two-Finger Tap
- Pinch In/Out
- Edge Gestures
- Stylus Button 1/2
- Aktion frei zuordnen
- Custom Shortcut als Ziel
- Gesture Sensitivity
- Konfliktprüfung
- Haptik pro Gestentyp
- Gesten pro Seite überschreibbar
- Gesten pro Element überschreibbar

### 14. Suche & Command Palette

- Fuzzy Search
- Apps
- Shortcuts
- explizit freigegebene Dateien
- Befehlsvorschläge
- Suchverlauf: aus / Session / lokal persistent
- Ranking: Smart / Alphabetisch / Nutzung
- Hardware-Keyboard Shortcut
- globale Suchgeste
- Provider-Handoff Verhalten

### 15. Benachrichtigungen & Badges

- aus / Dot / Count
- Dock-Badges
- Folder-Badges
- App-Badges
- Work Profile Badges
- Notification Access verwalten
- niemals Inhaltskopie als Default
- Ruhe-/Fokusprofile

### 16. Smartpen & Pen Space

- Pen Space an/aus
- Hover
- Druck
- Neigung
- Finger ignorieren
- Button-Aktionen
- Standardwerkzeug
- Strichstärke
- Marker
- Radierer
- Autosave
- SVG Export
- Android System Note Routing

### 17. Automationen & Kontext

- Kontextvorschläge
- Zeitkontext
- Akkukontext
- Audioausgabe-Kontext
- Work-/Focus-Suggestions
- Szenenvorschläge
- automatische Layoutänderungen standardmäßig aus
- Preview/Apply für jede intelligente Layoutänderung
- regelbasierte Aktionen mit Dry Run

### 18. Barrierefreiheit

- Reduced Motion
- High Contrast
- große Touch-Ziele
- Text neben Icons
- Page Change Announcements
- TalkBack optimierte Reihenfolge
- Switch Access
- Hardware-Tastatur vollständige Bedienbarkeit
- Schrift 200 %
- Farbsehschwächen-Profile
- haptische Alternative zu rein visuellen Zuständen

### 19. Datenschutz & Sicherheit

- lokale Nutzungslernwerte
- Notification Access
- Audit an/aus
- Audit Retention
- Netzwerkfeatures global
- Kontextvorschau vor Provider-Handoff
- lokale Datenquellen einzeln löschen
- Vault verwalten
- App-/Datei-/Kontakt-Capabilities verwalten
- Sicherheitszentrum als Detailunterseite, nicht als Launcher-Hauptzweck

### 20. Backup, Restore & Migration

- Launcher Settings
- Workspace Layout
- Themes
- Assistant Preferences
- lokale Lernwerte optional
- Secrets immer ausgeschlossen
- Widget Host IDs immer ausgeschlossen
- Dry Run vor Restore
- Konfliktauflösung
- Gerätewechsel / Restore Mapping
- einzelne Teilbereiche importieren
- einzelne Teilbereiche exportieren

### 21. Android & Systemintegration

- Standard-Launcher-Auswahl
- Dynamic Color
- Work Profile
- Notification Dots
- Android Settings Routen
- System Font Scale
- Default Apps
- Akku-/Display-/Sound-/Privacy-Systemseiten
- Private Space erst bei vollständigem Container-Support

### 22. Erweitert & Diagnose

- Diagnosemodus
- Performance Overlay
- lokale UI-Timing-Metriken
- experimentelle Features
- Feature Flags
- Cache zurücksetzen
- Suchindex neu aufbauen
- Settings auf Standard zurücksetzen
- Teilbereich zurücksetzen
- Diagnoseexport ohne Nutzdaten

## Vererbungsmodell: nahezu jeder Punkt frei konfigurierbar

Nicht jede Einstellung muss überall überschreibbar sein. Aber alle visuellen und Interaktionsoptionen verwenden ein klares Scope-Modell:

1. **Global:** Standard für den gesamten Launcher.
2. **Page:** überschreibt den globalen Wert nur auf einer Home-Seite.
3. **Folder/Widget/App Item:** überschreibt den geerbten Wert nur für dieses Objekt.
4. **Session/Context:** temporäre Zustände dürfen niemals die persistierte Nutzerkonfiguration heimlich verändern.

Beispiele:

- Globales Raster 12×12, Work-Seite 16×12.
- Global Neural Glass, Studio-Seite mit eigenem Theme-Override.
- Global Labels `Smart`, ein bestimmtes App-Icon ohne Label.
- Global Assistant rechts sichtbar, eine Präsentationsseite komplett ohne Assistant.
- Global Swipe Down = Suche, Work-Seite Swipe Down = Notifications.

Jeder Override braucht **„Standard erben“** als explizite Option, damit Konfiguration wieder vereinfacht werden kann.

## Settings UX

Das Settings Center bekommt:

- permanente Suche über alle Einstellungen
- Favoriten / angeheftete Einstellungen
- „zuletzt geändert“
- Breadcrumbs
- Untertabs und gruppierte Cards
- sofortige Vorschau bei visuellen Optionen
- Apply/Discard für größere Änderungen
- Undo
- „nur diese Seite“ / „global“ Scope-Chip
- Reset je Einstellung, Gruppe, Tab oder komplett
- Export ausgewählter Teilbereiche
- Import mit Dry Run und Konfliktanzeige
- keine versteckten Änderungen durch Assistant oder Automationen

## Technischer Vertrag

`LauncherSettingsDocument` ist portabel und versioniert. Das Modell enthält niemals:

- API Secret Values
- OAuth Access/Refresh Tokens
- Android `appWidgetId`
- persistierte URI Grants
- Notification-Inhalte
- Geräteauthentifizierungsdaten
- private Runtime Handles

Dafür existieren getrennte lokale Stores/Vaults. Portable Konfiguration verweist höchstens auf stabile Alias-Namen.

## Implementierungsreihenfolge

1. Settings Models + Schema + Tests.
2. Settings Store mit Migration und atomarem Save.
3. Settings Center Shell mit Suche und 22 Haupttabs.
4. Home/Raster + Appearance + Assistant als erste live angebundene Tabs.
5. Theme Import/Export mit Preview/Rollback.
6. Gesture Matrix und per-page Overrides.
7. AI/API Provider Settings + Vault UI.
8. Widget/Folder/App Item Inspector Overrides.
9. Backup Teilbereichsauswahl und Settings Migration.
10. Vollständige Accessibility-/OEM-/Process-death-Tests.
