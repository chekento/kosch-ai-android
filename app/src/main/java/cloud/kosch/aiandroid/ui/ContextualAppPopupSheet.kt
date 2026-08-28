package cloud.kosch.aiandroid.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cloud.kosch.aiandroid.LauncherController
import cloud.kosch.aiandroid.ai.ContextPopupInput
import cloud.kosch.aiandroid.ai.ContextPopupItem
import cloud.kosch.aiandroid.ai.ContextPopupItemKind
import cloud.kosch.aiandroid.ai.ContextPopupPolicy
import cloud.kosch.aiandroid.ai.ContextPopupShortcut
import cloud.kosch.aiandroid.system.AppWidgetProviderDiscovery
import cloud.kosch.aiandroid.ui.theme.DeepSurface
import cloud.kosch.aiandroid.ui.theme.Mint
import cloud.kosch.aiandroid.ui.theme.MutedMist
import cloud.kosch.aiandroid.ui.theme.RaisedSurface
import cloud.kosch.aiandroid.ui.theme.Sky
import cloud.kosch.aiandroid.ui.theme.Violet

/**
 * Niagara-style contextual density without notification-content ingestion or silent widget binding.
 *
 * Quick items are produced by ContextPopupPolicy. Every executable item delegates to an existing LauncherController
 * route; widget discovery is read-only and opening the Widget Board still leaves picker/binding consent to Android.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextualAppPopupSheet(controller: LauncherController) {
    val app = controller.selectedApp ?: return
    val context = LocalContext.current
    val widgetDiscovery = remember(context) { AppWidgetProviderDiscovery(context) }
    var widgetProviderCount by remember(app.key) { mutableIntStateOf(0) }
    var confirmUninstall by remember(app.key) { mutableStateOf(false) }

    LaunchedEffect(app.key) {
        widgetProviderCount = widgetDiscovery.countFor(app)
    }

    val profileAmbiguousForBadges = controller.apps.count { it.packageName == app.packageName } > 1
    val badgeCount = if (profileAmbiguousForBadges) 0 else controller.notificationCounts[app.packageName] ?: 0
    val popupItems = remember(
        app.key,
        app.label,
        controller.appShortcuts,
        controller.pinnedAppKeys,
        controller.hiddenAppKeys,
        badgeCount,
        widgetProviderCount,
    ) {
        ContextPopupPolicy.build(
            ContextPopupInput(
                appKey = app.key,
                appLabel = app.label,
                isPinned = controller.isPinned(app),
                isHidden = controller.isHidden(app),
                badgeCount = badgeCount,
                publishedShortcuts = controller.appShortcuts.map {
                    ContextPopupShortcut(id = it.id, label = it.label)
                },
                publishedWidgetCount = widgetProviderCount,
            ),
        )
    }

    ModalBottomSheet(
        onDismissRequest = controller::hideAppActions,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DeepSurface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SheetHeader(
                title = app.label,
                subtitle = "Kontext-Popup · ${app.profile.title} · lokal priorisiert",
                onClose = controller::hideAppActions,
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(app.icon, contentDescription = null, modifier = Modifier.size(58.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        app.packageName,
                        color = MutedMist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "Nur Paket-Zähler, veröffentlichte Android-Shortcuts und Widget-Capabilities",
                        color = Mint,
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    )
                }
            }

            Text("Schnellzugriff", fontWeight = FontWeight.SemiBold)
            popupItems.forEach { item ->
                when (item) {
                    is ContextPopupItem.Shortcut -> {
                        val shortcut = controller.appShortcuts.firstOrNull { it.id == item.shortcutId }
                        if (shortcut != null) {
                            Card(
                                onClick = { controller.launch(shortcut) },
                                colors = CardDefaults.cardColors(containerColor = RaisedSurface),
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    shortcut.icon?.let {
                                        Image(it, contentDescription = null, modifier = Modifier.size(36.dp))
                                    }
                                    Text(item.title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = Sky)
                                }
                            }
                        }
                    }

                    is ContextPopupItem.BadgeSummary -> Surface(
                        color = Violet.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(Icons.Rounded.Notifications, contentDescription = null, tint = Violet)
                            Text(item.title, modifier = Modifier.weight(1f))
                            Text("nur Anzahl", color = MutedMist)
                        }
                    }

                    is ContextPopupItem.WidgetEntry -> OutlinedButton(
                        onClick = {
                            controller.hideAppActions()
                            controller.openWidgetBoard()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Widgets, contentDescription = null)
                        Spacer(Modifier.width(7.dp))
                        Text(item.title)
                    }

                    is ContextPopupItem.Action -> if (item.kind == ContextPopupItemKind.OPEN_APP) {
                        Button(onClick = { controller.launch(app) }, modifier = Modifier.fillMaxWidth()) {
                            Text(item.title)
                        }
                    }
                }
            }

            if (controller.shortcutsLoading) {
                Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(26.dp))
                }
            }
            if (profileAmbiguousForBadges && controller.notificationAccessGranted) {
                Text(
                    "Badge-Zahl ausgeblendet: dasselbe Paket ist in mehreren Android-Profilen vorhanden.",
                    color = MutedMist,
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                )
            }

            HorizontalDivider()
            Text("Verwalten", fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = controller::openSelectedAppInfo, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Info, contentDescription = null)
                    Spacer(Modifier.width(5.dp))
                    Text("App-Info")
                }
                OutlinedButton(onClick = controller::openSelectedAppStore, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Storefront, contentDescription = null)
                    Spacer(Modifier.width(5.dp))
                    Text("Store")
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = controller::toggleSelectedAppPin, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.PushPin, contentDescription = null)
                    Spacer(Modifier.width(5.dp))
                    Text(if (controller.isPinned(app)) "Dock lösen" else "Ins Dock")
                }
                OutlinedButton(onClick = controller::toggleSelectedAppHidden, modifier = Modifier.weight(1f)) {
                    Icon(
                        if (controller.isHidden(app)) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(if (controller.isHidden(app)) "Einblenden" else "Verbergen")
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = controller::addSelectedAppToSmartFolder, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.CreateNewFolder, contentDescription = null)
                    Spacer(Modifier.width(5.dp))
                    Text("In Ordner")
                }
                TextButton(
                    onClick = {
                        if (confirmUninstall) controller.requestSelectedAppUninstall(confirmed = true)
                        else confirmUninstall = true
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null)
                    Spacer(Modifier.width(5.dp))
                    Text(if (confirmUninstall) "Android-Dialog" else "Deinstallieren …")
                }
            }

            if (controller.isPinned(app)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { controller.moveSelectedPinnedApp(-1) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.ArrowUpward, contentDescription = null)
                        Spacer(Modifier.width(5.dp))
                        Text("Dock links")
                    }
                    OutlinedButton(
                        onClick = { controller.moveSelectedPinnedApp(1) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.ArrowDownward, contentDescription = null)
                        Spacer(Modifier.width(5.dp))
                        Text("Dock rechts")
                    }
                }
            }

            if (controller.folders.isNotEmpty()) {
                Text("Sammlungen", color = MutedMist)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    controller.folders.forEach { folder ->
                        AssistChip(
                            onClick = { controller.addSelectedAppToFolder(folder.id) },
                            label = { Text(folder.title, maxLines = 1) },
                            leadingIcon = { Text(folder.kind.glyph, color = Mint) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
