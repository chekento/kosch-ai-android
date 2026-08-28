package cloud.kosch.aiandroid.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cloud.kosch.aiandroid.UniversalSearchController
import cloud.kosch.aiandroid.ai.SearchMatchReason
import cloud.kosch.aiandroid.ai.UniversalQueryResult
import cloud.kosch.aiandroid.ai.UniversalSearchKind
import cloud.kosch.aiandroid.ui.theme.DeepSurface
import cloud.kosch.aiandroid.ui.theme.Ink
import cloud.kosch.aiandroid.ui.theme.Mint
import cloud.kosch.aiandroid.ui.theme.MutedMist
import cloud.kosch.aiandroid.ui.theme.RaisedSurface
import cloud.kosch.aiandroid.ui.theme.Sky
import cloud.kosch.aiandroid.ui.theme.Violet

/**
 * Launcher-wide, memory-only command/search palette.
 *
 * Results are typed before they reach this surface. The UI never creates an Android Intent and never treats a local
 * calculation as an executable target. Actual execution stays with MainActivity/ViewModel routes that already own
 * profile, permission, AI-handoff and launcher capability gates.
 */
@Composable
fun UniversalSearchSurface(
    search: UniversalSearchController,
    onExecute: (UniversalQueryResult.Entity) -> Unit,
    onDismiss: () -> Unit,
) {
    val focusRequester = FocusRequester()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Ink.copy(alpha = 0.985f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        color = Mint.copy(alpha = 0.14f),
                        shape = CircleShape,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Search, contentDescription = null, tint = Mint)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Universal Search",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "Apps · Seiten · Settings · Aktionen · KI · lokale Tools",
                            color = MutedMist,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Universal Search schließen")
                    }
                }

                OutlinedTextField(
                    value = search.query,
                    onValueChange = search::updateQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    label = { Text("Suchen oder lokal berechnen") },
                    placeholder = { Text("z. B. Datenschutz, Studio, 12*8 oder 5 km in mi") },
                )

                if (search.query.isBlank()) {
                    UniversalSearchEmptyHint()
                } else if (search.results.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Kein lokaler Treffer", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Die Suche überträgt die Eingabe nicht an einen Anbieter.",
                                color = MutedMist,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            items = search.results,
                            key = { result -> resultKey(result) },
                        ) { result ->
                            when (result) {
                                is UniversalQueryResult.Utility -> UniversalUtilityResult(result)
                                is UniversalQueryResult.Entity -> UniversalEntityResult(
                                    result = result,
                                    onClick = { onExecute(result) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun UniversalSearchEntryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(48.dp),
        color = DeepSurface.copy(alpha = 0.94f),
        shape = CircleShape,
        tonalElevation = 8.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            IconButton(onClick = onClick) {
                Icon(Icons.Rounded.Search, contentDescription = "Universal Search öffnen", tint = Sky)
            }
        }
    }
}

@Composable
private fun UniversalSearchEmptyHint() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DeepSurface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text("Ein Suchfeld für den ganzen Launcher", fontWeight = FontWeight.SemiBold)
            Text(
                "Findet lokale Launcher-Ziele und rechnet einfache Ausdrücke/Einheiten offline. " +
                    "Die Suche speichert keinen Verlauf und führt Treffer erst nach deinem Tipp aus.",
                color = MutedMist,
                style = MaterialTheme.typography.bodySmall,
            )
            Text("Ctrl+K · Suchgeste · Search-Button", color = Mint, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun UniversalUtilityResult(result: UniversalQueryResult.Utility) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Mint.copy(alpha = 0.10f)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(modifier = Modifier.size(38.dp), color = Mint.copy(alpha = 0.15f), shape = CircleShape) {
                Box(contentAlignment = Alignment.Center) {
                    Text("=", color = Mint, fontWeight = FontWeight.Bold)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(result.result.display, fontWeight = FontWeight.SemiBold)
                Text("Lokales Ergebnis · keine externe Aktion", color = MutedMist, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun UniversalEntityResult(
    result: UniversalQueryResult.Entity,
    onClick: () -> Unit,
) {
    val entry = result.ranked.entry
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = RaisedSurface.copy(alpha = 0.94f)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                color = kindTint(entry.kind).copy(alpha = 0.12f),
                shape = CircleShape,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(kindIcon(entry.kind), contentDescription = null, tint = kindTint(entry.kind))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (entry.subtitle.isNotBlank()) {
                    Text(
                        entry.subtitle,
                        color = MutedMist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(kindTitle(entry.kind), color = kindTint(entry.kind), style = MaterialTheme.typography.labelSmall)
                Text(matchTitle(result.ranked.reason), color = MutedMist, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun resultKey(result: UniversalQueryResult): String = when (result) {
    is UniversalQueryResult.Utility -> "utility:${result.result.display}"
    is UniversalQueryResult.Entity -> result.ranked.entry.id
}

private fun kindTitle(kind: UniversalSearchKind): String = when (kind) {
    UniversalSearchKind.APP -> "APP"
    UniversalSearchKind.APP_SHORTCUT -> "SHORTCUT"
    UniversalSearchKind.FOLDER -> "ORDNER"
    UniversalSearchKind.PAGE -> "SEITE"
    UniversalSearchKind.SETTING -> "SETTING"
    UniversalSearchKind.CUSTOM_ACTION -> "AKTION"
    UniversalSearchKind.AI_ROUTE -> "KI"
}

private fun matchTitle(reason: SearchMatchReason): String = when (reason) {
    SearchMatchReason.EXACT -> "Exakt"
    SearchMatchReason.TRANSLITERATED -> "Schrift"
    SearchMatchReason.COMPACT_EXACT -> "Exakt kompakt"
    SearchMatchReason.TOKEN_PREFIX -> "Wortfolge"
    SearchMatchReason.PREFIX -> "Präfix"
    SearchMatchReason.WORD_PREFIX -> "Wortanfang"
    SearchMatchReason.CONTAINS -> "Enthält"
    SearchMatchReason.ACRONYM -> "Kürzel"
    SearchMatchReason.TYPO -> "Tippfehler"
    SearchMatchReason.SUBSEQUENCE -> "Ähnlich"
}

private fun kindTint(kind: UniversalSearchKind): Color = when (kind) {
    UniversalSearchKind.APP, UniversalSearchKind.APP_SHORTCUT -> Sky
    UniversalSearchKind.FOLDER -> Violet
    UniversalSearchKind.PAGE -> Mint
    UniversalSearchKind.SETTING -> Sky
    UniversalSearchKind.CUSTOM_ACTION -> Violet
    UniversalSearchKind.AI_ROUTE -> Mint
}

@Composable
private fun kindIcon(kind: UniversalSearchKind) = when (kind) {
    UniversalSearchKind.APP, UniversalSearchKind.APP_SHORTCUT -> Icons.Rounded.Apps
    UniversalSearchKind.FOLDER -> Icons.Rounded.Folder
    UniversalSearchKind.PAGE -> Icons.Rounded.Home
    UniversalSearchKind.SETTING -> Icons.Rounded.Settings
    UniversalSearchKind.CUSTOM_ACTION -> Icons.Rounded.AutoAwesome
    UniversalSearchKind.AI_ROUTE -> Icons.Rounded.AutoAwesome
}
