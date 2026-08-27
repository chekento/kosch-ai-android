package cloud.kosch.aiandroid.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cloud.kosch.aiandroid.ai.AiContextHandoffDraft
import cloud.kosch.aiandroid.ai.AiContextHandoffSelection
import cloud.kosch.aiandroid.ui.theme.DeepSurface
import cloud.kosch.aiandroid.ui.theme.Mint
import cloud.kosch.aiandroid.ui.theme.MutedMist
import cloud.kosch.aiandroid.ui.theme.RaisedSurface
import cloud.kosch.aiandroid.ui.theme.Sky

/**
 * Reusable explicit-consent UI for file, pen, web and screen context handoffs.
 * Nothing in this composable launches or shares content directly; the caller receives a user-authored selection only.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiContextHandoffConsentSurface(
    draft: AiContextHandoffDraft,
    onCancel: () -> Unit,
    onConfirm: (String, AiContextHandoffSelection) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var question by remember(draft.id) { mutableStateOf("") }
    var includeTitle by remember(draft.id) { mutableStateOf(true) }
    var includeMetadata by remember(draft.id) { mutableStateOf(false) }
    var includeSummary by remember(draft.id) { mutableStateOf(true) }
    var includeExcerpt by remember(draft.id) { mutableStateOf(false) }

    fun selectedCount(): Int = listOf(includeTitle, includeMetadata, includeSummary, includeExcerpt).count { it }

    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
        containerColor = DeepSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = Mint)
                Column {
                    Text("Kontext bewusst freigeben", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("${draft.kind.title} · bleibt bis zur Bestätigung lokal", color = MutedMist)
                }
            }

            Surface(color = RaisedSurface, shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(draft.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    draft.mimeType?.let { Text(it, color = Sky, style = MaterialTheme.typography.labelMedium) }
                    if (draft.localSummary.isNotBlank()) {
                        Text(draft.localSummary.take(700), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    draft.localExcerpt?.let {
                        Text("Auszug verfügbar · ${it.length} Zeichen", color = MutedMist, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Text("Was darf in den KI-Kontext?", fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = includeTitle,
                    onClick = { if (!includeTitle || selectedCount() > 1) includeTitle = !includeTitle },
                    label = { Text("Titel") },
                )
                FilterChip(
                    selected = includeSummary,
                    onClick = { if (!includeSummary || selectedCount() > 1) includeSummary = !includeSummary },
                    label = { Text("Zusammenfassung") },
                )
                FilterChip(
                    selected = includeMetadata,
                    onClick = { if (!includeMetadata || selectedCount() > 1) includeMetadata = !includeMetadata },
                    label = { Text("Typ & Größe") },
                )
                if (draft.localExcerpt != null) {
                    FilterChip(
                        selected = includeExcerpt,
                        onClick = { if (!includeExcerpt || selectedCount() > 1) includeExcerpt = !includeExcerpt },
                        label = { Text("Begrenzter Auszug") },
                    )
                }
            }

            Surface(color = Mint.copy(alpha = 0.10f), shape = RoundedCornerShape(16.dp)) {
                Text(
                    "Standard ist minimal: Titel + lokale Zusammenfassung. URI, Dateipfad, Roh-Pen-Koordinaten, Screen-Pixel oder Camera-Daten werden durch diesen Text-Handoff nicht automatisch mitgegeben.",
                    modifier = Modifier.padding(12.dp),
                    color = MutedMist,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            OutlinedTextField(
                value = question,
                onValueChange = { question = it.take(4_000) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 92.dp),
                label = { Text("Deine Frage oder Aufgabe (optional)") },
                placeholder = { Text("z. B. Fasse das zusammen und nenne mir offene Punkte") },
                minLines = 2,
                maxLines = 5,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("Abbrechen")
                }
                Button(
                    onClick = {
                        onConfirm(
                            question,
                            AiContextHandoffSelection(
                                includeTitle = includeTitle,
                                includeMetadata = includeMetadata,
                                includeSummary = includeSummary,
                                includeExcerpt = includeExcerpt,
                            ),
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                    Text("Freigeben")
                }
            }

            Text(
                "Die Freigabe öffnet zunächst den Smart Router. Erst eine weitere sichtbare Start-/Übergabe-Aktion kann Text an ein externes Ziel senden.",
                color = MutedMist,
                style = MaterialTheme.typography.labelSmall,
            )
            androidx.compose.foundation.layout.Spacer(Modifier.width(1.dp).padding(bottom = 18.dp))
        }
    }
}
