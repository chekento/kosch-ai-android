package cloud.kosch.aiandroid.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cloud.kosch.aiandroid.ScopedSettingsController
import cloud.kosch.aiandroid.model.WorkspaceObjectStyle
import cloud.kosch.aiandroid.model.WorkspaceObjectStyleOverrides
import cloud.kosch.aiandroid.model.WorkspaceObjectStylePreset
import cloud.kosch.aiandroid.model.WorkspaceObjectStylePresets
import cloud.kosch.aiandroid.model.WorkspaceObjectStyleResolver
import cloud.kosch.aiandroid.model.WorkspaceStyleColor
import cloud.kosch.aiandroid.ui.theme.DeepSurface
import cloud.kosch.aiandroid.ui.theme.Mint
import cloud.kosch.aiandroid.ui.theme.MutedMist
import cloud.kosch.aiandroid.ui.theme.Sky
import kotlin.math.roundToInt

/**
 * One coherent Home Studio inspector for portable object styling.
 *
 * Presets and expert controls write the exact same scoped tokens. The inspector edits a memory-only draft until the
 * user explicitly applies it, avoiding continuous disk writes while dragging sliders. INHERIT removes all object-level
 * style overrides and therefore falls back to PAGE/GLOBAL without copying inherited values into the object.
 */
@Composable
fun HomeStudioObjectStylePanel(
    scopedSettings: ScopedSettingsController,
    pageId: String,
    itemId: String,
    globalIconScale: Float,
    globalShowLabels: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resolved = WorkspaceObjectStyleResolver.resolve(
        document = scopedSettings.document,
        pageId = pageId,
        itemId = itemId,
        globalIconScale = globalIconScale,
        globalShowLabels = globalShowLabels,
    )
    var draft by remember(pageId, itemId) { mutableStateOf(resolved) }
    var backgroundText by remember(pageId, itemId) { mutableStateOf(WorkspaceStyleColor.format(resolved.backgroundArgb)) }
    var borderText by remember(pageId, itemId) { mutableStateOf(WorkspaceStyleColor.format(resolved.borderArgb)) }
    var validationMessage by remember(pageId, itemId) { mutableStateOf<String?>(null) }

    LaunchedEffect(scopedSettings.document, pageId, itemId) {
        val latest = WorkspaceObjectStyleResolver.resolve(
            document = scopedSettings.document,
            pageId = pageId,
            itemId = itemId,
            globalIconScale = globalIconScale,
            globalShowLabels = globalShowLabels,
        )
        draft = latest
        backgroundText = WorkspaceStyleColor.format(latest.backgroundArgb)
        borderText = WorkspaceStyleColor.format(latest.borderArgb)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DeepSurface.copy(alpha = 0.98f),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 430.dp)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.Palette, contentDescription = null, tint = Sky)
                Column(Modifier.weight(1f)) {
                    Text("Objekt-Stil", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Preset oder volle Feinsteuerung · portabel · vererbbar", color = MutedMist, style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = "Stil-Inspector schließen")
                }
            }

            Text("Schnellstile", style = MaterialTheme.typography.labelLarge, color = MutedMist)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                WorkspaceObjectStylePreset.entries.forEach { preset ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            validationMessage = null
                            if (scopedSettings.applyObjectStylePreset(itemId, preset)) {
                                val latest = WorkspaceObjectStyleResolver.resolve(
                                    document = scopedSettings.document,
                                    pageId = pageId,
                                    itemId = itemId,
                                    globalIconScale = globalIconScale,
                                    globalShowLabels = globalShowLabels,
                                )
                                draft = latest
                                backgroundText = WorkspaceStyleColor.format(latest.backgroundArgb)
                                borderText = WorkspaceStyleColor.format(latest.borderArgb)
                            }
                        },
                        label = { Text(preset.title) },
                    )
                }
            }

            HorizontalDivider()
            ToggleStyleRow(
                title = "Objekt sichtbar",
                subtitle = "Ausgeblendete Objekte bleiben im Home Studio wiederherstellbar.",
                checked = draft.visible,
                onCheckedChange = { draft = draft.copy(visible = it) },
            )
            ToggleStyleRow(
                title = "Label anzeigen",
                subtitle = "Unabhängig vom globalen Label-Modus für dieses Objekt.",
                checked = draft.showLabel,
                onCheckedChange = { draft = draft.copy(showLabel = it) },
            )

            StyleSlider("Icon-Skalierung", draft.iconScale, 0.25f..2.5f, "%.2f×") { draft = draft.copy(iconScale = it) }
            StyleSlider("Inhalt-Skalierung", draft.contentScale, 0.25f..2.5f, "%.2f×") { draft = draft.copy(contentScale = it) }
            StyleSlider("Label-Skalierung", draft.labelScale, 0.5f..2f, "%.2f×") { draft = draft.copy(labelScale = it) }
            StyleSlider("Deckkraft", draft.opacity, 0.05f..1f, "%d %%") { draft = draft.copy(opacity = it) }
            StyleSlider("Rotation", draft.rotationDegrees, -180f..180f, "%d°") { draft = draft.copy(rotationDegrees = it) }
            StyleSlider("Versatz X", draft.offsetXDp, -128f..128f, "%d dp") { draft = draft.copy(offsetXDp = it) }
            StyleSlider("Versatz Y", draft.offsetYDp, -128f..128f, "%d dp") { draft = draft.copy(offsetYDp = it) }
            StyleSlider("Ebene / Z", draft.zIndex, -32f..32f, "%d") { draft = draft.copy(zIndex = it.roundToInt().toFloat()) }
            StyleSlider("Eckenradius", draft.cornerDp.toFloat(), 0f..96f, "%d dp") { draft = draft.copy(cornerDp = it.roundToInt()) }
            StyleSlider("Innenabstand", draft.contentPaddingDp.toFloat(), 0f..48f, "%d dp") { draft = draft.copy(contentPaddingDp = it.roundToInt()) }
            StyleSlider("Hintergrunddeckkraft", draft.backgroundAlpha, 0f..1f, "%d %%") { draft = draft.copy(backgroundAlpha = it) }
            StyleSlider("Rahmenbreite", draft.borderWidthDp, 0f..12f, "%.1f dp") { draft = draft.copy(borderWidthDp = it) }
            StyleSlider("Elevation", draft.elevationDp, 0f..32f, "%.1f dp") { draft = draft.copy(elevationDp = it) }

            OutlinedTextField(
                value = backgroundText,
                onValueChange = { backgroundText = it.take(10); validationMessage = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Hintergrundfarbe") },
                supportingText = { Text("Leer = vererben · #RRGGBB oder #AARRGGBB") },
                singleLine = true,
            )
            OutlinedTextField(
                value = borderText,
                onValueChange = { borderText = it.take(10); validationMessage = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Rahmenfarbe") },
                supportingText = { Text("Leer = vererben · #RRGGBB oder #AARRGGBB") },
                singleLine = true,
            )

            validationMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        validationMessage = null
                        if (scopedSettings.applyObjectStylePreset(itemId, WorkspaceObjectStylePreset.INHERIT)) onClose()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = null)
                    Text("Vererben")
                }
                Button(
                    onClick = {
                        val background = parseOptionalColor(backgroundText)
                        val border = parseOptionalColor(borderText)
                        when {
                            backgroundText.isNotBlank() && background == null -> validationMessage = "Ungültige Hintergrundfarbe"
                            borderText.isNotBlank() && border == null -> validationMessage = "Ungültige Rahmenfarbe"
                            else -> {
                                val finalDraft = draft.copy(backgroundArgb = background, borderArgb = border)
                                if (scopedSettings.setObjectOverrides(itemId, WorkspaceObjectStyleOverrides.from(finalDraft))) {
                                    validationMessage = null
                                    onClose()
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Text("Übernehmen")
                }
            }

            Text(
                "Alle Regler schreiben ausschließlich portable Home-Studio-Style-Tokens. Android-Grants, Widget-Host-IDs und App-Daten bleiben davon unberührt.",
                color = Mint,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun ToggleStyleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, color = MutedMist, style = MaterialTheme.typography.labelSmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun StyleSlider(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueFormat: String,
    onValueChange: (Float) -> Unit,
) {
    val display = when {
        valueFormat.contains("%%") -> valueFormat.format((value * 100f).roundToInt())
        valueFormat.contains("%d") -> valueFormat.format(value.roundToInt())
        else -> valueFormat.format(value)
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(display, color = Sky, style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.semantics { contentDescription = "$title · $display" },
        )
    }
}

private fun parseOptionalColor(raw: String): Int? = raw.takeIf(String::isNotBlank)?.let(WorkspaceStyleColor::parse)
