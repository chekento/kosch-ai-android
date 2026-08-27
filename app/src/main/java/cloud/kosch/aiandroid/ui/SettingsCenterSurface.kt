package cloud.kosch.aiandroid.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cloud.kosch.aiandroid.AssistantSessionController
import cloud.kosch.aiandroid.LauncherSettingsController
import cloud.kosch.aiandroid.WorkspaceHomeController
import cloud.kosch.aiandroid.model.AssistantAnchor
import cloud.kosch.aiandroid.model.MotionProfile
import cloud.kosch.aiandroid.model.SettingsSection
import cloud.kosch.aiandroid.ui.theme.DeepSurface
import cloud.kosch.aiandroid.ui.theme.Ink
import cloud.kosch.aiandroid.ui.theme.Mint
import cloud.kosch.aiandroid.ui.theme.MutedMist
import cloud.kosch.aiandroid.ui.theme.RaisedSurface
import cloud.kosch.aiandroid.ui.theme.Sky

/**
 * Navigable Settings Center. Home/Grid, Appearance and Assistant are the first live sections; the stable catalog
 * keeps all remaining configuration domains visible while their controls are connected in later slices.
 */
@Composable
fun SettingsCenterSurface(
    settings: LauncherSettingsController,
    home: WorkspaceHomeController,
    assistant: AssistantSessionController,
    onDismiss: () -> Unit = settings::close,
    initialSection: SettingsSection? = settings.requestedSection,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf(initialSection) }
    val descriptors = remember { SettingsCenterCatalog.sections }
    val visible = remember(query) {
        val needle = query.trim().lowercase()
        if (needle.isBlank()) descriptors else descriptors.filter { descriptor ->
            descriptor.section.title.lowercase().contains(needle) ||
                descriptor.summary.lowercase().contains(needle) ||
                descriptor.topics.any { it.lowercase().contains(needle) }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(14.dp),
            ) {
                val wide = maxWidth >= 760.dp
                if (wide) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        SettingsNavigationPane(
                            query = query,
                            onQueryChange = { query = it.take(120) },
                            descriptors = visible,
                            selected = selected,
                            onSelect = { selected = it.section },
                            onDismiss = onDismiss,
                            modifier = Modifier.width(330.dp),
                        )
                        SettingsDetailPane(
                            descriptor = descriptors.firstOrNull { it.section == selected } ?: descriptors.first(),
                            settings = settings,
                            home = home,
                            assistant = assistant,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else if (selected == null) {
                    SettingsNavigationPane(
                        query = query,
                        onQueryChange = { query = it.take(120) },
                        descriptors = visible,
                        selected = null,
                        onSelect = { selected = it.section },
                        onDismiss = onDismiss,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    SettingsDetailPane(
                        descriptor = descriptors.first { it.section == selected },
                        settings = settings,
                        home = home,
                        assistant = assistant,
                        onBack = { selected = null },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsNavigationPane(
    query: String,
    onQueryChange: (String) -> Unit,
    descriptors: List<SettingsSectionDescriptor>,
    selected: SettingsSection?,
    onSelect: (SettingsSectionDescriptor) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(modifier = Modifier.size(44.dp), color = Mint.copy(alpha = 0.14f), shape = CircleShape) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Settings, contentDescription = null, tint = Mint)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Settings Center", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("22 Bereiche · tief konfigurierbar", color = MutedMist, style = MaterialTheme.typography.labelMedium)
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = "Settings Center schließen")
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            label = { Text("Einstellungen durchsuchen") },
            placeholder = { Text("Raster, Assistent, API, Theme …") },
        )
        Surface(color = Sky.copy(alpha = 0.08f), shape = RoundedCornerShape(16.dp)) {
            Text(
                "Scope: Global → Seite → Objekt · jeder Override bekommt „Standard erben“.",
                modifier = Modifier.padding(12.dp),
                color = Sky,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            items(descriptors, key = { it.section.name }) { descriptor ->
                SettingsSectionRow(
                    descriptor = descriptor,
                    selected = descriptor.section == selected,
                    onClick = { onSelect(descriptor) },
                )
            }
            if (descriptors.isEmpty()) {
                item { Text("Keine passende Einstellung gefunden.", color = MutedMist, modifier = Modifier.padding(16.dp)) }
            }
        }
    }
}

@Composable
private fun SettingsSectionRow(
    descriptor: SettingsSectionDescriptor,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = if (selected) Sky.copy(alpha = 0.15f) else DeepSurface.copy(alpha = 0.95f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Rounded.Tune, contentDescription = null, tint = if (selected) Sky else Mint)
            Column(modifier = Modifier.weight(1f)) {
                Text(descriptor.section.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(descriptor.summary, color = MutedMist, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun SettingsDetailPane(
    descriptor: SettingsSectionDescriptor,
    settings: LauncherSettingsController,
    home: WorkspaceHomeController,
    assistant: AssistantSessionController,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    Surface(modifier = modifier, color = DeepSurface.copy(alpha = 0.95f), shape = RoundedCornerShape(28.dp)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                onBack?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zurück zu allen Einstellungen")
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(descriptor.section.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text(descriptor.summary, color = MutedMist)
                }
                if (settings.canUndo) {
                    IconButton(onClick = { settings.undo(home) }) {
                        Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "Letzte Settings-Änderung rückgängig")
                    }
                }
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            when (descriptor.section) {
                SettingsSection.HOME -> HomeSettingsEditor(settings, home)
                SettingsSection.APPEARANCE -> AppearanceSettingsEditor(settings)
                SettingsSection.ASSISTANT -> AssistantSettingsEditor(settings, assistant)
                else -> PlannedSettingsTopics(descriptor)
            }

            settings.notice?.let { message ->
                Surface(color = Mint.copy(alpha = 0.10f), shape = RoundedCornerShape(14.dp)) {
                    Text(message, modifier = Modifier.fillMaxWidth().padding(11.dp), color = Mint)
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.HomeSettingsEditor(settings: LauncherSettingsController, home: WorkspaceHomeController) {
    val current = settings.document.home
    var draft by remember(current) { mutableStateOf(current) }
    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            LiveTag("LIVE · verlustfreier Workspace-Reflow")
            NumericSetting("Rasterspalten", draft.gridColumns, 4, 24) { draft = draft.copy(gridColumns = it) }
            NumericSetting("Rasterzeilen", draft.gridRows, 4, 32) { draft = draft.copy(gridRows = it) }
            ToggleSetting("Layout sperren", draft.lockLayout) { draft = draft.copy(lockLayout = it) }
            ToggleSetting("Seitenindikator", draft.showPageIndicator) { draft = draft.copy(showPageIndicator = it) }
            ToggleSetting("Freie Zellen automatisch füllen", draft.autoFillEmptyCells) { draft = draft.copy(autoFillEmptyCells = it) }
        }
        item {
            Surface(color = Sky.copy(alpha = 0.08f), shape = RoundedCornerShape(14.dp)) {
                Text(
                    "Rasteränderungen werden erst bei „Übernehmen“ gespeichert. Passt eine Seite nicht verlustfrei in das neue Raster, bleibt der bisherige Homescreen vollständig erhalten.",
                    modifier = Modifier.padding(12.dp),
                    color = Sky,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item { ApplyDiscardRow(dirty = draft != current, onApply = { settings.applyHome(draft, home) }, onDiscard = { draft = current }) }
    }
}

@Composable
private fun ColumnScope.AppearanceSettingsEditor(settings: LauncherSettingsController) {
    val current = settings.document.appearance
    var draft by remember(current) { mutableStateOf(current) }
    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            LiveTag("LIVE CORE · Material You + persistente Visual Tokens")
            ToggleSetting("Material-You-Akzente", draft.useMaterialYouAccents) { draft = draft.copy(useMaterialYouAccents = it) }
            Text("Motion", color = Mint, style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                MotionProfile.entries.forEach { profile ->
                    FilterChip(
                        selected = draft.motionProfile == profile,
                        onClick = { draft = draft.copy(motionProfile = profile) },
                        label = { Text(profile.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
            }
            SliderSetting("Blur", draft.blurStrength, 0f..1f) { draft = draft.copy(blurStrength = it) }
            SliderSetting("Oberflächen-Deckkraft", draft.surfaceOpacity, 0.25f..1f) { draft = draft.copy(surfaceOpacity = it) }
            SliderSetting("Ecken-Skalierung", draft.cornerScale, 0.5f..1.8f) { draft = draft.copy(cornerScale = it) }
            SliderSetting("Inhalts-Skalierung", draft.contentScale, 0.75f..1.5f) { draft = draft.copy(contentScale = it) }
        }
        item { ApplyDiscardRow(dirty = draft != current, onApply = { settings.applyAppearance(draft) }, onDiscard = { draft = current }) }
    }
}

@Composable
private fun ColumnScope.AssistantSettingsEditor(
    settings: LauncherSettingsController,
    assistant: AssistantSessionController,
) {
    val current = settings.document.assistant
    var draft by remember(current) { mutableStateOf(current) }
    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            LiveTag("LIVE · Sichtbarkeit, Voice und Verhalten")
            ToggleSetting("Assistent aktiv", draft.enabled) { draft = draft.copy(enabled = it) }
            Text("Position", color = Mint, style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                AssistantAnchor.entries.forEach { anchor ->
                    FilterChip(
                        selected = draft.anchor == anchor,
                        onClick = { draft = draft.copy(anchor = anchor) },
                        label = { Text(anchor.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
            }
            SliderSetting("Größe", draft.scale, 0.35f..2.5f) { draft = draft.copy(scale = it) }
            SliderSetting("Deckkraft", draft.opacity, 0.2f..1f) { draft = draft.copy(opacity = it) }
            ToggleSetting("Portal-/Spawn-Animation", draft.portalAnimationEnabled) { draft = draft.copy(portalAnimationEnabled = it) }
            ToggleSetting("Idle-Bewegung", draft.idleMotionEnabled) { draft = draft.copy(idleMotionEnabled = it) }
            ToggleSetting("Blicksteuerung", draft.gazeTrackingEnabled) { draft = draft.copy(gazeTrackingEnabled = it) }
            ToggleSetting("Emotionen", draft.emotionAnimationEnabled) { draft = draft.copy(emotionAnimationEnabled = it) }
            ToggleSetting("Viseme/Lippensynchronisation", draft.visemeLipSyncEnabled) { draft = draft.copy(visemeLipSyncEnabled = it) }
            ToggleSetting("Live Chat", draft.liveChatEnabled) { draft = draft.copy(liveChatEnabled = it) }
            ToggleSetting("Spracheingabe", draft.voiceInputEnabled) { draft = draft.copy(voiceInputEnabled = it) }
            ToggleSetting("Sprachausgabe", draft.speechOutputEnabled) { draft = draft.copy(speechOutputEnabled = it) }
            ToggleSetting("Außerhalb Assistent-Seiten ausblenden", draft.hideOutsideAssistantPages) { draft = draft.copy(hideOutsideAssistantPages = it) }
        }
        item {
            ApplyDiscardRow(
                dirty = draft != current,
                onApply = {
                    if (settings.applyAssistant(draft)) {
                        assistant.setEnabled(draft.enabled)
                        assistant.setVoiceInputEnabled(draft.voiceInputEnabled)
                        assistant.setSpeechOutputEnabled(draft.speechOutputEnabled)
                    }
                },
                onDiscard = { draft = current },
            )
        }
    }
}

@Composable
private fun ColumnScope.PlannedSettingsTopics(descriptor: SettingsSectionDescriptor) {
    Text("Geplante Unteroptionen", color = Mint, style = MaterialTheme.typography.labelLarge)
    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(descriptor.topics) { topic ->
            Surface(color = RaisedSurface, shape = RoundedCornerShape(15.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(topic, modifier = Modifier.weight(1f))
                    Text("TODO", color = Sky, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(onClick = {}, enabled = false, label = { Text("Global") })
        AssistChip(onClick = {}, enabled = false, label = { Text("Seite") })
        AssistChip(onClick = {}, enabled = false, label = { Text("Objekt") })
    }
}

@Composable
private fun LiveTag(text: String) {
    Surface(color = Mint.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp)) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = Mint, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun NumericSetting(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, modifier = Modifier.weight(1f))
        IconButton(enabled = value > min, onClick = { onChange((value - 1).coerceAtLeast(min)) }) {
            Icon(Icons.Rounded.Remove, contentDescription = "$label verringern")
        }
        Text(value.toString(), fontWeight = FontWeight.SemiBold)
        IconButton(enabled = value < max, onClick = { onChange((value + 1).coerceAtMost(max)) }) {
            Icon(Icons.Rounded.Add, contentDescription = "$label erhöhen")
        }
    }
}

@Composable
private fun ToggleSetting(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SliderSetting(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text(String.format("%.2f", value), color = MutedMist, style = MaterialTheme.typography.labelMedium)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun ApplyDiscardRow(dirty: Boolean, onApply: () -> Unit, onDiscard: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onDiscard, enabled = dirty, modifier = Modifier.weight(1f)) { Text("Verwerfen") }
        Button(onClick = onApply, enabled = dirty, modifier = Modifier.weight(1f)) { Text("Übernehmen") }
    }
}

private data class SettingsSectionDescriptor(
    val section: SettingsSection,
    val summary: String,
    val topics: List<String>,
)

private object SettingsCenterCatalog {
    val sections = listOf(
        section(SettingsSection.HOME, "Raster, Abstände und Homescreen-Verhalten", "Raster Portrait/Landscape", "Abstände", "Icon-Größe", "Labels", "Layout sperren", "Reflow-Vorschau"),
        section(SettingsSection.PAGES, "Seiten, Räume und Übergänge", "Standardseite", "Duplizieren", "Verstecken", "Seiten-Looping", "Übergang", "Per-Page Raster", "Per-Page Wallpaper"),
        section(SettingsSection.APPS, "App Drawer, Sortierung und Darstellung", "Drawer-Raster", "Smart/A–Z/Zuletzt", "System-Apps", "Work-Badges", "Eigene Verknüpfungen", "App-Aktionsmenü"),
        section(SettingsSection.DOCK, "Schnellzugriff und adaptive Vorschläge", "Slots", "Adaptive Apps", "Ask Button", "Position", "Transparenz", "Per-Page Dock", "Eigene Links/Actions"),
        section(SettingsSection.FOLDERS, "Manuelle und intelligente App-Gruppen", "Raster", "Sheet/Popup", "Smart Groups", "Sortierung", "Ordnergesten", "Icon-Stack", "Links in Ordnern"),
        section(SettingsSection.WIDGETS, "Widgets, Größen und Stacks", "Standardgröße", "Freies Resize", "Widget Stacks", "Stack-Geste", "Missing/Remap", "Provider Size Hints"),
        section(SettingsSection.APPEARANCE, "Motion, Tiefe, Material You und Oberflächen", "Light/Dark", "Material You", "Blur", "Transparenz", "Corner Radius", "Motion Profile", "Parallax"),
        section(SettingsSection.THEMES, "Theme Wahl, Import, Export und Rollback", "Theme wählen", "Preview", "Import", "Export", "Wallpaper einschließen", "Layout einschließen", "Rollback"),
        section(SettingsSection.ASSISTANT, "Charakter, Verhalten, Animation und Sichtbarkeit", "Assistent an/aus", "Charakter", "Position", "Größe", "Spawn/Portal", "Idle", "Gaze", "Emotion", "Viseme", "Per-Page Sichtbarkeit"),
        section(SettingsSection.AI, "Lokale und externe Modelle kontrollieren", "Local-first", "Ask every time", "Default Provider", "Lokales Modell", "Modell pro Aufgabe", "Kontextquellen"),
        section(SettingsSection.API, "Provider, Endpoints und Vault-Referenzen", "Provider aktivieren", "Endpoint", "Model ID", "Vault Slot", "Verbindung testen", "Kontextvorschau", "Timeout/Retry"),
        section(SettingsSection.VOICE, "STT, TTS und Audioverhalten", "Voice Input", "Speech Output", "Locale", "Speech Rate", "Pitch", "TTS Engine", "Audio-Fokus", "Viseme Sync"),
        section(SettingsSection.GESTURES, "Gesten frei auf Aktionen mappen", "Swipe", "Double Tap", "Long Press", "Pinch", "Edge", "Stylus Buttons", "Per-Page Override", "Per-Item Override", "Eigene Verknüpfung ausführen"),
        section(SettingsSection.SEARCH, "Suche, Ranking und Command Palette", "Fuzzy Search", "Apps", "Shortcuts", "Eigene Links", "Dateien", "History", "Ranking", "Keyboard Shortcut"),
        section(SettingsSection.NOTIFICATIONS, "Badges und Systemzugriff", "Dot/Count", "Dock Badges", "Folder Badges", "Work Badges", "Notification Access"),
        section(SettingsSection.PEN, "Smartpen und Pen Space", "Hover", "Druck", "Neigung", "Buttons", "Standardwerkzeug", "Autosave", "SVG Export"),
        section(SettingsSection.AUTOMATION, "Kontextvorschläge und Regeln", "Zeit", "Akku", "Audio", "Szenenvorschläge", "Layout Preview", "Rule Dry Run", "Eigene Verknüpfung als Aktion"),
        section(SettingsSection.ACCESSIBILITY, "Bedienbarkeit als echte Designvariante", "Reduced Motion", "High Contrast", "Große Touch-Ziele", "TalkBack", "Switch Access", "200% Text", "Farbprofile"),
        section(SettingsSection.PRIVACY, "Lokale Daten, Netzwerk und Security Controls", "Usage Learning", "Audit", "Retention", "Netzwerkfeatures", "Context Preview", "Vault", "Daten löschen"),
        section(SettingsSection.BACKUP, "Teil-Backup, Restore und Migration", "Settings", "Workspace", "Themes", "Assistent", "Eigene Verknüpfungen", "Dry Run", "Konflikte", "Secrets immer ausschließen"),
        section(SettingsSection.SYSTEM, "Android und Geräteintegration", "Standard Launcher", "Dynamic Color", "Work Profile", "Default Apps", "System Font Scale", "Android Settings"),
        section(SettingsSection.ADVANCED, "Diagnose, Feature Flags und Reset", "Diagnosemodus", "Performance Overlay", "UI Timing", "Experimente", "Cache Reset", "Teil-Reset", "Diagnoseexport"),
    )

    private fun section(
        section: SettingsSection,
        summary: String,
        vararg topics: String,
    ) = SettingsSectionDescriptor(section, summary, topics.toList())
}
