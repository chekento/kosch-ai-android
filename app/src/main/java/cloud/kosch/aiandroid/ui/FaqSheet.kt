package cloud.kosch.aiandroid.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import cloud.kosch.aiandroid.LauncherController
import cloud.kosch.aiandroid.data.UnifiedFaqRegistry
import cloud.kosch.aiandroid.model.FaqCategory
import cloud.kosch.aiandroid.ui.theme.DeepSurface
import cloud.kosch.aiandroid.ui.theme.Mint
import cloud.kosch.aiandroid.ui.theme.MutedMist
import cloud.kosch.aiandroid.ui.theme.RaisedSurface
import cloud.kosch.aiandroid.ui.theme.Sky

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaqSheet(controller: LauncherController) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<FaqCategory?>(null) }
    var expandedId by remember { mutableStateOf<String?>("first-start") }
    val results = remember(query, category) { UnifiedFaqRegistry.search(query, category) }

    ModalBottomSheet(
        onDismissRequest = controller::closeFaq,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DeepSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    color = Mint.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(
                        Icons.Rounded.HelpOutline,
                        contentDescription = null,
                        tint = Mint,
                        modifier = Modifier.padding(11.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("FAQ & Hilfe", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text("${UnifiedFaqRegistry.entries.size} lokale Antworten · keine Websuche", color = MutedMist)
                }
                IconButton(onClick = controller::closeFaq) {
                    Icon(Icons.Rounded.Close, contentDescription = "FAQ schließen")
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("FAQ durchsuchen") },
                placeholder = { Text("z. B. Smartpen, Dateien oder Notausgang") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = category == null,
                    onClick = { category = null },
                    label = { Text("Alle") },
                )
                FaqCategory.entries.forEach { item ->
                    FilterChip(
                        selected = category == item,
                        onClick = { category = if (category == item) null else item },
                        label = { Text(item.title) },
                    )
                }
            }

            if (results.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = RaisedSurface,
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Keine direkte Antwort gefunden", fontWeight = FontWeight.SemiBold)
                        Text("Versuche einen kürzeren Begriff oder wähle „Alle“.", color = MutedMist)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    items(results, key = { it.id }) { entry ->
                        val expanded = expandedId == entry.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedId = if (expanded) null else entry.id },
                            colors = CardDefaults.cardColors(
                                containerColor = if (expanded) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                } else {
                                    RaisedSurface
                                },
                            ),
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(entry.category.title.uppercase(), color = Sky, style = MaterialTheme.typography.labelSmall)
                                        Text(entry.question, fontWeight = FontWeight.SemiBold)
                                    }
                                    Icon(
                                        if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                        contentDescription = if (expanded) "Antwort einklappen" else "Antwort anzeigen",
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                                if (expanded) {
                                    Text(entry.answer, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
