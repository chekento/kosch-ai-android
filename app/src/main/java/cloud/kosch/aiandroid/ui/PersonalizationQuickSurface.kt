package cloud.kosch.aiandroid.ui

import androidx.activity.ComponentActivity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import cloud.kosch.aiandroid.LauncherSettingsController
import cloud.kosch.aiandroid.LauncherViewModel
import cloud.kosch.aiandroid.data.IconPackResolver
import cloud.kosch.aiandroid.data.InstalledIconPack
import cloud.kosch.aiandroid.model.GestureAction
import cloud.kosch.aiandroid.model.GestureBinding
import cloud.kosch.aiandroid.model.GestureSettings
import cloud.kosch.aiandroid.model.GestureTrigger
import cloud.kosch.aiandroid.model.HapticProfile
import cloud.kosch.aiandroid.model.HomePage
import cloud.kosch.aiandroid.model.WorkspaceMode
import cloud.kosch.aiandroid.system.LauncherGestureBindingResolver
import cloud.kosch.aiandroid.ui.theme.DeepSurface
import cloud.kosch.aiandroid.ui.theme.Mint
import cloud.kosch.aiandroid.ui.theme.MutedMist
import cloud.kosch.aiandroid.ui.theme.RaisedSurface
import cloud.kosch.aiandroid.ui.theme.Sky
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fast, low-cognitive-load customization for the launcher features users expect to tweak most often.
 * Home Studio owns spatial editing; the full Settings Center remains authoritative for expert settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalizationQuickSurface(
    settings: LauncherSettingsController,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findComponentActivity() }
    val launcherViewModel = remember(activity) {
        activity?.let { ViewModelProvider(it)[LauncherViewModel::class.java] }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var gestureDraft by remember(settings.document.gestures) { mutableStateOf(settings.document.gestures) }
    var appearanceDraft by remember(settings.document.appearance) { mutableStateOf(settings.document.appearance) }
    var iconPacks by remember { mutableStateOf<List<InstalledIconPack>>(emptyList()) }
    var iconPacksLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        iconPacks = withContext(Dispatchers.IO) { IconPackResolver(context).discover() }
        iconPacksLoaded = true
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DeepSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Rounded.Palette, contentDescription = null, tint = Sky)
                Column(Modifier.weight(1f)) {
                    Text("Anpassen", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("Homescreen, wichtige Gesten & Icons", color = MutedMist)
                }
                TextButton(onClick = onDismiss) { Text("Fertig") }
            }

            OutlinedButton(
                onClick = {
                    launcherViewModel?.controller?.apply {
                        switchHomePage(HomePage.WORKSPACE)
                        selectWorkspaceMode(WorkspaceMode.EDIT)
                    }
                    onDismiss()
                },
                enabled = launcherViewModel != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = null)
                Text("Home Studio öffnen")
            }
            Text(
                "Apps, Widgets, Ordner und Seiten direkt anordnen – ohne den normalen Homescreen mit Bearbeitungsbuttons zu füllen.",
                color = MutedMist,
                style = MaterialTheme.typography.bodySmall,
            )

            Surface(color = RaisedSurface, shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Alltagsgesten", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Nur freie Launcher-Flächen reagieren. Widgets, Buttons und Drag & Drop haben Vorrang.",
                                color = MutedMist,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(
                            checked = gestureDraft.enabled,
                            onCheckedChange = { gestureDraft = gestureDraft.copy(enabled = it) },
                        )
                    }

                    Text("Haptik", style = MaterialTheme.typography.labelLarge, color = MutedMist)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        HapticProfile.entries.forEach { profile ->
                            FilterChip(
                                selected = gestureDraft.haptics == profile,
                                onClick = { gestureDraft = gestureDraft.copy(haptics = profile) },
                                label = { Text(profile.hapticTitle()) },
                            )
                        }
                    }

                    HorizontalDivider()
                    QUICK_GESTURES.forEach { trigger ->
                        GestureBindingRow(
                            trigger = trigger,
                            action = gestureDraft.actionFor(trigger),
                            enabled = gestureDraft.enabled,
                            onActionSelected = { action ->
                                gestureDraft = gestureDraft.withBinding(trigger, action)
                            },
                        )
                    }
                    Text(
                        "Links/rechts blättert standardmäßig durch die Seiten. Ein bewusstes „Keine Aktion“ bleibt wirklich deaktiviert.",
                        color = MutedMist,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        "Zwei-Finger-, Pinch-, Rand-, Stift- und weitere Spezialgesten findest du im vollständigen Settings Center.",
                        color = MutedMist,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            Surface(color = RaisedSurface, shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Icon Pack", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Systemicons sind immer der sichere Fallback. Arbeitsprofil-Icons behalten vorerst Androids sichtbares Profil-Badge.",
                        color = MutedMist,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = appearanceDraft.iconPackPackage == null,
                            onClick = { appearanceDraft = appearanceDraft.copy(iconPackPackage = null) },
                            label = { Text("Systemicons") },
                        )
                        iconPacks.forEach { pack ->
                            FilterChip(
                                selected = appearanceDraft.iconPackPackage == pack.packageName,
                                onClick = { appearanceDraft = appearanceDraft.copy(iconPackPackage = pack.packageName) },
                                label = { Text(pack.label) },
                            )
                        }
                    }
                    if (iconPacksLoaded && iconPacks.isEmpty()) {
                        Text("Kein kompatibles installiertes Icon Pack erkannt.", color = MutedMist, style = MaterialTheme.typography.labelSmall)
                    } else if (!iconPacksLoaded) {
                        Text("Suche installierte Icon Packs …", color = MutedMist, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        gestureDraft = GestureSettings()
                        appearanceDraft = appearanceDraft.copy(iconPackPackage = null)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = null)
                    Text("Standard")
                }
                Button(
                    onClick = {
                        if (settings.applyQuickPersonalization(gestureDraft, appearanceDraft)) onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                    Text("Übernehmen")
                }
            }

            Text(
                "Raster, Dock, Ordner, Widgets, KI, Datenschutz und Barrierefreiheit bleiben im Settings Center übersichtlich nach Bereichen getrennt.",
                color = Mint,
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(Modifier.height(18.dp))
        }
    }
}

private fun Context.findComponentActivity(): ComponentActivity? {
    var current: Context? = this
    while (current != null) {
        when (current) {
            is ComponentActivity -> return current
            is ContextWrapper -> current = current.baseContext
            else -> return null
        }
    }
    return null
}

@Composable
private fun GestureBindingRow(
    trigger: GestureTrigger,
    action: GestureAction,
    enabled: Boolean,
    onActionSelected: (GestureAction) -> Unit,
) {
    var menuVisible by remember(trigger) { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(trigger.gestureTitle(), fontWeight = FontWeight.Medium)
            Text(action.actionTitle(), color = if (enabled) Sky else MutedMist, style = MaterialTheme.typography.labelSmall)
        }
        Column {
            OutlinedButton(onClick = { menuVisible = true }, enabled = enabled) {
                Text("Ändern")
            }
            DropdownMenu(expanded = menuVisible, onDismissRequest = { menuVisible = false }) {
                SAFE_GESTURE_ACTIONS.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.actionTitle()) },
                        onClick = {
                            menuVisible = false
                            onActionSelected(option)
                        },
                    )
                }
            }
        }
    }
}

private fun GestureSettings.actionFor(trigger: GestureTrigger): GestureAction =
    LauncherGestureBindingResolver.actionFor(this, trigger)

private fun GestureSettings.withBinding(trigger: GestureTrigger, action: GestureAction): GestureSettings {
    val next = bindings.filterNot { it.trigger == trigger }.toMutableList()
    val persistExplicitNone = trigger == GestureTrigger.SWIPE_LEFT || trigger == GestureTrigger.SWIPE_RIGHT
    if (action != GestureAction.NONE || persistExplicitNone) {
        next += GestureBinding(trigger = trigger, action = action)
    }
    return copy(bindings = next).normalized()
}

private fun GestureTrigger.gestureTitle(): String = when (this) {
    GestureTrigger.SWIPE_UP -> "Nach oben wischen"
    GestureTrigger.SWIPE_DOWN -> "Nach unten wischen"
    GestureTrigger.SWIPE_LEFT -> "Nach links wischen"
    GestureTrigger.SWIPE_RIGHT -> "Nach rechts wischen"
    GestureTrigger.DOUBLE_TAP -> "Doppeltippen"
    GestureTrigger.LONG_PRESS -> "Lange drücken"
    GestureTrigger.TWO_FINGER_TAP -> "Mit zwei Fingern tippen"
    GestureTrigger.PINCH_IN -> "Zusammenziehen"
    GestureTrigger.PINCH_OUT -> "Auseinanderziehen"
    GestureTrigger.EDGE_LEFT -> "Vom linken Rand wischen"
    GestureTrigger.EDGE_RIGHT -> "Vom rechten Rand wischen"
    GestureTrigger.STYLUS_BUTTON_PRIMARY -> "Stift-Haupttaste"
    GestureTrigger.STYLUS_BUTTON_SECONDARY -> "Stift-Zweittaste"
}

private fun GestureAction.actionTitle(): String = when (this) {
    GestureAction.NONE -> "Keine Aktion"
    GestureAction.OPEN_DRAWER -> "Apps öffnen"
    GestureAction.OPEN_SEARCH -> "Suchen"
    GestureAction.OPEN_COMMAND_PALETTE -> "Command Bar"
    GestureAction.OPEN_HOME_STUDIO -> "Home Studio"
    GestureAction.OPEN_SETTINGS -> "Einstellungen"
    GestureAction.OPEN_ASSISTANT -> "Assistent"
    GestureAction.OPEN_NOTIFICATIONS -> "Benachrichtigungen"
    GestureAction.PREVIOUS_PAGE -> "Vorherige Seite"
    GestureAction.NEXT_PAGE -> "Nächste Seite"
    GestureAction.LOCK_DEVICE_ROUTE -> "Gerät sperren"
    GestureAction.SYSTEM_QUICK_SETTINGS -> "Kontrollzentrum"
    GestureAction.CUSTOM_SHORTCUT -> "Eigene Aktion"
}

private fun HapticProfile.hapticTitle(): String = when (this) {
    HapticProfile.OFF -> "Aus"
    HapticProfile.LIGHT -> "Leicht"
    HapticProfile.STANDARD -> "Normal"
    HapticProfile.STRONG -> "Kräftig"
}

private val QUICK_GESTURES = listOf(
    GestureTrigger.SWIPE_UP,
    GestureTrigger.SWIPE_DOWN,
    GestureTrigger.SWIPE_LEFT,
    GestureTrigger.SWIPE_RIGHT,
    GestureTrigger.LONG_PRESS,
)

private val SAFE_GESTURE_ACTIONS = listOf(
    GestureAction.NONE,
    GestureAction.OPEN_DRAWER,
    GestureAction.OPEN_SEARCH,
    GestureAction.OPEN_COMMAND_PALETTE,
    GestureAction.OPEN_HOME_STUDIO,
    GestureAction.OPEN_SETTINGS,
    GestureAction.OPEN_ASSISTANT,
    GestureAction.OPEN_NOTIFICATIONS,
    GestureAction.PREVIOUS_PAGE,
    GestureAction.NEXT_PAGE,
    GestureAction.SYSTEM_QUICK_SETTINGS,
)
