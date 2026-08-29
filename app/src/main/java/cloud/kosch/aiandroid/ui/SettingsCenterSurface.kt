package cloud.kosch.aiandroid.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.TextButton
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
import cloud.kosch.aiandroid.model.HapticProfile
import cloud.kosch.aiandroid.model.MotionProfile
import cloud.kosch.aiandroid.model.PageTransition
import cloud.kosch.aiandroid.model.SettingMaturity
import cloud.kosch.aiandroid.model.SettingsFeatureCatalog
import cloud.kosch.aiandroid.model.SettingsFeatureDefinition
import cloud.kosch.aiandroid.model.SettingsSection
import cloud.kosch.aiandroid.model.WidgetStackSwitchMode
import cloud.kosch.aiandroid.ui.theme.DeepSurface
import cloud.kosch.aiandroid.ui.theme.Ink
import cloud.kosch.aiandroid.ui.theme.Mint
import cloud.kosch.aiandroid.ui.theme.MutedMist
import cloud.kosch.aiandroid.ui.theme.RaisedSurface
import cloud.kosch.aiandroid.ui.theme.Sky
import cloud.kosch.aiandroid.ui.theme.Warm

/**
 * Calm, searchable settings surface. The first level intentionally shows common categories only; less frequent and
 * expert areas stay one explicit step away or are found immediately through search. Runtime truth remains sourced from
 * SettingsFeatureCatalog and the authoritative controllers rather than being duplicated as decorative UI state.
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
    val visible = remember(query, descriptors) {
        val needle = query.trim().lowercase()
        if (needle.isBlank()) {
            descriptors
        } else {
            val matchedSections = SettingsFeatureCatalog.search(needle).mapTo(mutableSetOf()) { it.section }
            descriptors.filter { descriptor ->
                descriptor.section in matchedSections ||
                    descriptor.section.title.lowercase().contains(needle) ||
                    descriptor.summary.lowercase().contains(needle)
            }
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
    var showAll by rememberSaveable { mutableStateOf(false) }
    val revealAll = query.isNotBlank() || showAll || (selected != null && selected !in PRIMARY_SETTINGS_SECTIONS)
    val listed = if (revealAll) descriptors else descriptors.filter { it.section in PRIMARY_SETTINGS_SECTIONS }
    val hasHidden = query.isBlank() && descriptors.any { it.section !in PRIMARY_SETTINGS_SECTIONS }

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
                Text("Einstellungen", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("Finde schnell, was du ändern möchtest.", color = MutedMist, style = MaterialTheme.typography.labelMedium)
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
            placeholder = { Text("z. B. Raster, Assistent, Backup") },
        )

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            if (query.isBlank()) {
                item {
                    Text(
                        if (revealAll) "Alle Bereiche" else "Häufig verwendet",
                        color = MutedMist,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }

            items(listed, key = { it.section.name }) { descriptor ->
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

        if (hasHidden && query.isBlank()) {
            TextButton(
                onClick = { showAll = !showAll },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (showAll) "Weniger anzeigen" else "Weitere Einstellungen")
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
                Text(
                    descriptor.summary,
                    color = MutedMist,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
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
                SettingsSection.PAGES -> PagesSettingsEditor(settings)
                SettingsSection.APPS -> AppsSettingsEditor(settings)
                SettingsSection.DOCK -> DockSettingsEditor(settings)
                SettingsSection.FOLDERS -> FolderSettingsEditor(settings)
                SettingsSection.WIDGETS -> WidgetSettingsEditor(settings)
                SettingsSection.APPEARANCE -> AppearanceSettingsEditor(settings)
                SettingsSection.ASSISTANT -> AssistantSettingsEditor(settings, assistant, descriptor.features)
                SettingsSection.API -> KalProviderConnectionsEditor(settings)
                SettingsSection.ACCESSIBILITY -> AccessibilitySettingsEditor(settings)
                SettingsSection.PRIVACY -> PrivacySettingsEditor(settings)
                SettingsSection.BACKUP -> BackupSettingsEditor(settings)
                SettingsSection.SYSTEM -> SystemSettingsEditor(settings)
                SettingsSection.ADVANCED -> AdvancedSettingsEditor(settings)
                else -> SettingsCoverageList(descriptor.features)
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
            LiveTag("Sicheres Raster mit verlustfreiem Reflow")
            NumericSetting("Rasterspalten", draft.gridColumns, 4, 24) { draft = draft.copy(gridColumns = it) }
            NumericSetting("Rasterzeilen", draft.gridRows, 4, 32) { draft = draft.copy(gridRows = it) }
            ToggleSetting("Layout sperren", draft.lockLayout) { draft = draft.copy(lockLayout = it) }
            ToggleSetting("Seitenindikator", draft.showPageIndicator) { draft = draft.copy(showPageIndicator = it) }
            ToggleSetting("Freie Zellen automatisch füllen", draft.autoFillEmptyCells) { draft = draft.copy(autoFillEmptyCells = it) }
        }
        item {
            Surface(color = Sky.copy(alpha = 0.08f), shape = RoundedCornerShape(14.dp)) {
                Text(
                    "Rasteränderungen werden erst bei „Übernehmen“ gespeichert. Passt eine Seite nicht verlustfrei in das neue Raster, bleibt der bisherige Homescreen erhalten.",
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
private fun ColumnScope.PagesSettingsEditor(settings: LauncherSettingsController) {
    val current = settings.document.pages
    var draft by remember(current) { mutableStateOf(current) }
    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            EditableTag()
            ToggleSetting("Seiten-Looping", draft.loopingEnabled) { draft = draft.copy(loopingEnabled = it) }
            ToggleSetting("Letzte Seite merken", draft.rememberLastPage) { draft = draft.copy(rememberLastPage = it) }
            ToggleSetting("Wallpaper pro Seite erlauben", draft.allowPerPageWallpaper) { draft = draft.copy(allowPerPageWallpaper = it) }
            ToggleSetting("Raster pro Seite erlauben", draft.allowPerPageGridOverride) { draft = draft.copy(allowPerPageGridOverride = it) }
            NumericSetting("Übergangsdauer (ms)", draft.transitionDurationMs, 80, 1_200) {
                draft = draft.copy(transitionDurationMs = it)
            }
            Text("Übergang", color = Mint, style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                PageTransition.entries.forEach { transition ->
                    FilterChip(
                        selected = draft.transition == transition,
                        onClick = { draft = draft.copy(transition = transition) },
                        label = { Text(transition.name.lowercase().replaceFirstChar(Char::uppercase)) },
                    )
                }
            }
        }
        item { ApplyDiscardRow(dirty = draft != current, onApply = { settings.applyPages(draft) }, onDiscard = { draft = current }) }
    }
}

@Composable
private fun ColumnScope.AppsSettingsEditor(settings: LauncherSettingsController) {
    val current = settings.document.apps
    var draft by remember(current) { mutableStateOf(current) }
    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            EditableTag()
            ToggleSetting("Labels anzeigen", draft.showLabels) { draft = draft.copy(showLabels = it) }
            ToggleSetting("Work-Profile-Badges", draft.showWorkProfileBadges) { draft = draft.copy(showWorkProfileBadges = it) }
            ToggleSetting("System-Apps ausblenden", draft.hideSystemApps) { draft = draft.copy(hideSystemApps = it) }
            ToggleSetting("Smart Ranking", draft.smartRankingEnabled) { draft = draft.copy(smartRankingEnabled = it) }
            ToggleSetting("Alphabetischer Index", draft.alphabeticalIndexEnabled) { draft = draft.copy(alphabeticalIndexEnabled = it) }
            NumericSetting("Drawer-Spalten Hochformat", draft.drawerColumnsPortrait, 3, 12) {
                draft = draft.copy(drawerColumnsPortrait = it)
            }
            NumericSetting("Drawer-Spalten Querformat", draft.drawerColumnsLandscape, 4, 16) {
                draft = draft.copy(drawerColumnsLandscape = it)
            }
        }
        item { ApplyDiscardRow(dirty = draft != current, onApply = { settings.applyApps(draft) }, onDiscard = { draft = current }) }
    }
}

@Composable
private fun ColumnScope.DockSettingsEditor(settings: LauncherSettingsController) {
    val current = settings.document.dock
    var draft by remember(current) { mutableStateOf(current) }
    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            EditableTag()
            ToggleSetting("Dock aktiv", draft.enabled) { draft = draft.copy(enabled = it) }
            ToggleSetting("Adaptive Vorschläge", draft.adaptiveSuggestions) { draft = draft.copy(adaptiveSuggestions = it) }
            ToggleSetting("Ask/AI-Schaltfläche", draft.showAskButton) { draft = draft.copy(showAskButton = it) }
            NumericSetting("Maximale Einträge", draft.maxItems, 0, 12) { draft = draft.copy(maxItems = it) }
            SliderSetting("Icon-Skalierung", draft.iconScale, 0.5f..1.75f) { draft = draft.copy(iconScale = it) }
            SliderSetting("Hintergrund-Deckkraft", draft.backgroundOpacity, 0f..1f) { draft = draft.copy(backgroundOpacity = it) }
        }
        item { ApplyDiscardRow(dirty = draft != current, onApply = { settings.applyDock(draft) }, onDiscard = { draft = current }) }
    }
}

@Composable
private fun ColumnScope.FolderSettingsEditor(settings: LauncherSettingsController) {
    val current = settings.document.folders
    var draft by remember(current) { mutableStateOf(current) }
    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            EditableTag()
            ToggleSetting("Als Sheet öffnen", draft.openAsSheet) { draft = draft.copy(openAsSheet = it) }
            ToggleSetting("Labels anzeigen", draft.showLabels) { draft = draft.copy(showLabels = it) }
            ToggleSetting("Smart Folders", draft.smartFoldersEnabled) { draft = draft.copy(smartFoldersEnabled = it) }
            ToggleSetting("Nach App-Start schließen", draft.closeAfterLaunch) { draft = draft.copy(closeAfterLaunch = it) }
            NumericSetting("Spalten", draft.columns, 2, 10) { draft = draft.copy(columns = it) }
        }
        item { ApplyDiscardRow(dirty = draft != current, onApply = { settings.applyFolders(draft) }, onDiscard = { draft = current }) }
    }
}

@Composable
private fun ColumnScope.WidgetSettingsEditor(settings: LauncherSettingsController) {
    val current = settings.document.widgets
    var draft by remember(current) { mutableStateOf(current) }
    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            EditableTag()
            ToggleSetting("Freies Resize", draft.allowFreeResize) { draft = draft.copy(allowFreeResize = it) }
            ToggleSetting("Fehlenden Provider als Platzhalter zeigen", draft.showMissingProviderPlaceholder) {
                draft = draft.copy(showMissingProviderPlaceholder = it)
            }
            NumericSetting("Standardbreite in Zellen", draft.defaultColumnSpan, 1, 24) {
                draft = draft.copy(defaultColumnSpan = it)
            }
            NumericSetting("Standardhöhe in Zellen", draft.defaultRowSpan, 1, 32) {
                draft = draft.copy(defaultRowSpan = it)
            }
            NumericSetting("Stack Auto-Cycle (Sek.)", draft.stackAutoCycleSeconds, 0, 3_600) {
                draft = draft.copy(stackAutoCycleSeconds = it)
            }
            Text("Stack-Wechsel", color = Mint, style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                WidgetStackSwitchMode.entries.forEach { mode ->
                    FilterChip(
                        selected = draft.stackSwitchMode == mode,
                        onClick = { draft = draft.copy(stackSwitchMode = mode) },
                        label = { Text(mode.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)) },
                    )
                }
            }
            Text("Widget-Haptik", color = Mint, style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                HapticProfile.entries.forEach { profile ->
                    FilterChip(
                        selected = draft.haptics == profile,
                        onClick = { draft = draft.copy(haptics = profile) },
                        label = { Text(profile.name.lowercase().replaceFirstChar(Char::uppercase)) },
                    )
                }
            }
        }
        item { ApplyDiscardRow(dirty = draft != current, onApply = { settings.applyWidgets(draft) }, onDiscard = { draft = current }) }
    }
}

@Composable
private fun ColumnScope.AppearanceSettingsEditor(settings: LauncherSettingsController) {
    val current = settings.document.appearance
    var draft by remember(current) { mutableStateOf(current) }
    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            LiveTag("Darstellung wird sicher in deinen Launcher-Einstellungen gespeichert")
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
    features: List<SettingsFeatureDefinition>,
) {
    val current = settings.document.assistant
    var draft by remember(current) { mutableStateOf(current) }
    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            LiveTag("Darstellung hier · Verhalten im Assistant Control Center")
            Surface(color = if (assistant.settings.enabled) Mint.copy(alpha = 0.10f) else RaisedSurface, shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (assistant.settings.enabled) "Assistent aktiv" else "Assistent aus",
                        color = if (assistant.settings.enabled) Mint else MutedMist,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Charakter, Rufname, Wake Word, Stimme, Screen/Camera Awareness und Agent-Rechte verwaltest du zentral im Assistant Control Center.",
                        color = MutedMist,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedButton(onClick = assistant::open) { Text("Assistant Control Center öffnen") }
                }
            }
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
            ToggleSetting("Außerhalb Assistent-Seiten ausblenden", draft.hideOutsideAssistantPages) { draft = draft.copy(hideOutsideAssistantPages = it) }
        }
        item {
            ApplyDiscardRow(
                dirty = presentationChanged(current, draft),
                onApply = { settings.applyAssistant(draft) },
                onDiscard = { draft = current },
            )
        }
        item { Text("Weitere Assistant-Funktionen", color = Mint, style = MaterialTheme.typography.labelLarge) }
        items(features.filterNot { it.id in assistantPresentationIds }) { feature ->
            FeatureCoverageRow(feature)
        }
    }
}

@Composable
private fun ColumnScope.AccessibilitySettingsEditor(settings: LauncherSettingsController) {
    val current = settings.document.accessibility
    var draft by remember(current) { mutableStateOf(current) }
    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            EditableTag()
            ToggleSetting("Reduced Motion", draft.reducedMotion) { draft = draft.copy(reducedMotion = it) }
            ToggleSetting("Hoher Kontrast", draft.highContrast) { draft = draft.copy(highContrast = it) }
            ToggleSetting("Große Touch-Ziele", draft.largeTouchTargets) { draft = draft.copy(largeTouchTargets = it) }
            ToggleSetting("Seitenwechsel ansagen", draft.announcePageChanges) { draft = draft.copy(announcePageChanges = it) }
            ToggleSetting("Text zusätzlich zu Icons bevorzugen", draft.preferTextAlongsideIcons) {
                draft = draft.copy(preferTextAlongsideIcons = it)
            }
        }
        item { ApplyDiscardRow(dirty = draft != current, onApply = { settings.applyAccessibility(draft) }, onDiscard = { draft = current }) }
    }
}

@Composable
private fun ColumnScope.PrivacySettingsEditor(settings: LauncherSettingsController) {
    val current = settings.document.privacy
    var draft by remember(current) { mutableStateOf(current) }
    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            EditableTag()
            ToggleSetting("Lokales Usage Learning", draft.localUsageLearningEnabled) { draft = draft.copy(localUsageLearningEnabled = it) }
            ToggleSetting("Audit-Protokoll", draft.auditEnabled) { draft = draft.copy(auditEnabled = it) }
            ToggleSetting("Netzwerkfeatures erlauben", draft.allowNetworkFeatures) { draft = draft.copy(allowNetworkFeatures = it) }
            ToggleSetting("Kontext vor Provider-Handoff anzeigen", draft.requireContextPreviewBeforeProviderHandoff) {
                draft = draft.copy(requireContextPreviewBeforeProviderHandoff = it)
            }
            NumericSetting("Audit-Aufbewahrung (Tage)", draft.auditRetentionDays, 1, 365) {
                draft = draft.copy(auditRetentionDays = it)
            }
            LockedSetting("Notification-Zugriff", "Wird nur über den Android-Systemdialog erteilt")
        }
        item { ApplyDiscardRow(dirty = draft != current, onApply = { settings.applyPrivacy(draft) }, onDiscard = { draft = current }) }
    }
}

@Composable
private fun ColumnScope.BackupSettingsEditor(settings: LauncherSettingsController) {
    val current = settings.document.backup
    var draft by remember(current) { mutableStateOf(current) }
    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            EditableTag()
            ToggleSetting("Launcher-Einstellungen einschließen", draft.includeLauncherSettings) { draft = draft.copy(includeLauncherSettings = it) }
            ToggleSetting("Workspace-Layout einschließen", draft.includeWorkspaceLayout) { draft = draft.copy(includeWorkspaceLayout = it) }
            ToggleSetting("Themes einschließen", draft.includeThemes) { draft = draft.copy(includeThemes = it) }
            ToggleSetting("Assistant-Präferenzen einschließen", draft.includeAssistantPreferences) {
                draft = draft.copy(includeAssistantPreferences = it)
            }
            ToggleSetting("Usage Learning einschließen", draft.includeUsageLearning) { draft = draft.copy(includeUsageLearning = it) }
            LockedSetting("Secrets", "Werden immer ausgeschlossen")
            LockedSetting("Widget Host IDs, Freigaben und Geräte-Voice-IDs", "Bleiben immer auf diesem Gerät")
        }
        item { ApplyDiscardRow(dirty = draft != current, onApply = { settings.applyBackup(draft) }, onDiscard = { draft = current }) }
    }
}

@Composable
private fun ColumnScope.SystemSettingsEditor(settings: LauncherSettingsController) {
    val current = settings.document.system
    var draft by remember(current) { mutableStateOf(current) }
    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            EditableTag()
            ToggleSetting("Dynamic Color", draft.dynamicColorEnabled) { draft = draft.copy(dynamicColorEnabled = it) }
            ToggleSetting("Work-Profile-Integration", draft.workProfileIntegrationEnabled) {
                draft = draft.copy(workProfileIntegrationEnabled = it)
            }
            ToggleSetting("Notification Dots", draft.notificationDotsEnabled) { draft = draft.copy(notificationDotsEnabled = it) }
            ToggleSetting("System-Home-Escape sichtbar", draft.systemHomeEscapeVisible) { draft = draft.copy(systemHomeEscapeVisible = it) }
            ToggleSetting("System-Schriftgröße folgen", draft.followSystemFontScale) { draft = draft.copy(followSystemFontScale = it) }
            Surface(color = Sky.copy(alpha = 0.08f), shape = RoundedCornerShape(14.dp)) {
                Text(
                    "KAL passt sich an Fenstergröße, Foldables, externe Displays und verfügbare Eingabegeräte an.",
                    modifier = Modifier.padding(12.dp),
                    color = Sky,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item { ApplyDiscardRow(dirty = draft != current, onApply = { settings.applySystem(draft) }, onDiscard = { draft = current }) }
    }
}

@Composable
private fun ColumnScope.AdvancedSettingsEditor(settings: LauncherSettingsController) {
    val current = settings.document.advanced
    var draft by remember(current) { mutableStateOf(current) }
    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            EditableTag()
            ToggleSetting("Diagnosefunktionen", draft.diagnosticsEnabled) { draft = draft.copy(diagnosticsEnabled = it) }
            ToggleSetting("Performance-Overlay", draft.showPerformanceOverlay) { draft = draft.copy(showPerformanceOverlay = it) }
            ToggleSetting("UI-Timing lokal protokollieren", draft.logUiTimingLocally) { draft = draft.copy(logUiTimingLocally = it) }
            ToggleSetting("Experimentelle Features", draft.experimentalFeaturesEnabled) {
                draft = draft.copy(experimentalFeaturesEnabled = it)
            }
        }
        item { ApplyDiscardRow(dirty = draft != current, onApply = { settings.applyAdvanced(draft) }, onDiscard = { draft = current }) }
    }
}

private fun presentationChanged(
    current: cloud.kosch.aiandroid.model.LauncherAssistantSettings,
    draft: cloud.kosch.aiandroid.model.LauncherAssistantSettings,
): Boolean =
    current.anchor != draft.anchor ||
        current.scale != draft.scale ||
        current.opacity != draft.opacity ||
        current.portalAnimationEnabled != draft.portalAnimationEnabled ||
        current.idleMotionEnabled != draft.idleMotionEnabled ||
        current.gazeTrackingEnabled != draft.gazeTrackingEnabled ||
        current.emotionAnimationEnabled != draft.emotionAnimationEnabled ||
        current.visemeLipSyncEnabled != draft.visemeLipSyncEnabled ||
        current.hideOutsideAssistantPages != draft.hideOutsideAssistantPages

@Composable
private fun ColumnScope.SettingsCoverageList(features: List<SettingsFeatureDefinition>) {
    Text("Optionen in diesem Bereich", color = Mint, style = MaterialTheme.typography.labelLarge)
    Text(
        "Der Status zeigt transparent, was bereits nutzbar ist und was noch vorbereitet wird.",
        color = MutedMist,
        style = MaterialTheme.typography.bodySmall,
    )
    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(features, key = { it.id }) { feature -> FeatureCoverageRow(feature) }
    }
}

@Composable
private fun FeatureCoverageRow(feature: SettingsFeatureDefinition) {
    Surface(color = RaisedSurface, shape = RoundedCornerShape(15.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(feature.title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Text(
                maturityLabel(feature.maturity),
                color = maturityColor(feature.maturity),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun LiveTag(text: String) {
    Surface(color = Mint.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp)) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = Mint, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun EditableTag() {
    Surface(color = Sky.copy(alpha = 0.10f), shape = RoundedCornerShape(12.dp)) {
        Text(
            "Änderungen werden erst mit „Übernehmen“ gespeichert.",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            color = Sky,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun LockedSetting(label: String, reason: String) {
    Surface(color = RaisedSurface, shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label, fontWeight = FontWeight.SemiBold)
            Text(reason, color = MutedMist, style = MaterialTheme.typography.bodySmall)
        }
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
    val features: List<SettingsFeatureDefinition>,
)

private object SettingsCenterCatalog {
    private val summaries = mapOf(
        SettingsSection.HOME to "Raster und Homescreen-Verhalten",
        SettingsSection.PAGES to "Seiten, Räume und Übergänge",
        SettingsSection.APPS to "App Drawer, Sortierung und Icons",
        SettingsSection.DOCK to "Schnellzugriff und Vorschläge",
        SettingsSection.FOLDERS to "Ordner und intelligente Gruppen",
        SettingsSection.WIDGETS to "Widgets, Größen und Stacks",
        SettingsSection.APPEARANCE to "Farben, Motion und Darstellung",
        SettingsSection.THEMES to "Themes auswählen, importieren und exportieren",
        SettingsSection.ASSISTANT to "Charakter, Darstellung und Assistant Control Center",
        SettingsSection.AI to "Lokale und externe KI-Modelle",
        SettingsSection.API to "Provider und Verbindungen",
        SettingsSection.VOICE to "Spracheingabe und Sprachausgabe",
        SettingsSection.GESTURES to "Gesten und Eingaben",
        SettingsSection.SEARCH to "Suche und Command Palette",
        SettingsSection.NOTIFICATIONS to "Benachrichtigungen und Badges",
        SettingsSection.PEN to "Smartpen und Pen Space",
        SettingsSection.AUTOMATION to "Kontextvorschläge und Automationen",
        SettingsSection.ACCESSIBILITY to "Barrierefreiheit und Bedienhilfen",
        SettingsSection.PRIVACY to "Lokale Daten, Netzwerk und Sicherheit",
        SettingsSection.BACKUP to "Backup, Restore und Migration",
        SettingsSection.SYSTEM to "Android und Systemintegration",
        SettingsSection.ADVANCED to "Diagnose und experimentelle Funktionen",
    )

    private val order = listOf(
        SettingsSection.HOME,
        SettingsSection.APPEARANCE,
        SettingsSection.APPS,
        SettingsSection.PAGES,
        SettingsSection.DOCK,
        SettingsSection.FOLDERS,
        SettingsSection.WIDGETS,
        SettingsSection.ASSISTANT,
        SettingsSection.AI,
        SettingsSection.SEARCH,
        SettingsSection.PRIVACY,
        SettingsSection.ACCESSIBILITY,
        SettingsSection.GESTURES,
        SettingsSection.NOTIFICATIONS,
        SettingsSection.VOICE,
        SettingsSection.PEN,
        SettingsSection.AUTOMATION,
        SettingsSection.BACKUP,
        SettingsSection.THEMES,
        SettingsSection.API,
        SettingsSection.SYSTEM,
        SettingsSection.ADVANCED,
    )

    val sections: List<SettingsSectionDescriptor> = order.map { section ->
        SettingsSectionDescriptor(
            section = section,
            summary = summaries.getValue(section),
            features = SettingsFeatureCatalog.forSection(section),
        )
    }
}

private val PRIMARY_SETTINGS_SECTIONS = setOf(
    SettingsSection.HOME,
    SettingsSection.APPEARANCE,
    SettingsSection.APPS,
    SettingsSection.PAGES,
    SettingsSection.DOCK,
    SettingsSection.FOLDERS,
    SettingsSection.WIDGETS,
    SettingsSection.ASSISTANT,
    SettingsSection.AI,
    SettingsSection.SEARCH,
    SettingsSection.PRIVACY,
    SettingsSection.ACCESSIBILITY,
)

private val assistantPresentationIds = setOf(
    "assistant.anchor",
    "assistant.scale",
    "assistant.opacity",
    "assistant.portal_spawn",
    "assistant.idle_motion",
    "assistant.gaze",
    "assistant.emotion",
    "assistant.viseme",
    "pages.assistant_visibility",
)

private fun maturityLabel(maturity: SettingMaturity): String = when (maturity) {
    SettingMaturity.LIVE -> "Verfügbar"
    SettingMaturity.CORE_READY -> "Grundfunktion"
    SettingMaturity.PLANNED -> "In Vorbereitung"
    SettingMaturity.EXPERIMENTAL -> "Experimentell"
}

private fun maturityColor(maturity: SettingMaturity) = when (maturity) {
    SettingMaturity.LIVE -> Mint
    SettingMaturity.CORE_READY -> Sky
    SettingMaturity.PLANNED -> MutedMist
    SettingMaturity.EXPERIMENTAL -> Warm
}
