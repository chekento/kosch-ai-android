package cloud.kosch.aiandroid.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cloud.kosch.aiandroid.NewsFeedController
import cloud.kosch.aiandroid.news.NewsCategory
import cloud.kosch.aiandroid.news.NewsItem
import cloud.kosch.aiandroid.ui.theme.DeepSurface
import cloud.kosch.aiandroid.ui.theme.Mint
import cloud.kosch.aiandroid.ui.theme.MutedMist
import cloud.kosch.aiandroid.ui.theme.RaisedSurface
import cloud.kosch.aiandroid.ui.theme.Sky
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun NewsSurface(
    news: NewsFeedController,
    networkAllowed: Boolean,
    onOpenNetworkSettings: () -> Unit,
    onOpenArticle: (String) -> Unit,
) {
    BackHandler(onBack = news::close)
    var sourcesVisible by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f), shadowElevation = 6.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Rounded.Newspaper, contentDescription = null, tint = Mint)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("News", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Überschriften aus gewählten Originalquellen",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { sourcesVisible = !sourcesVisible }) {
                            Icon(Icons.Rounded.Tune, contentDescription = "News-Quellen")
                        }
                        IconButton(
                            enabled = !news.loading,
                            onClick = { news.refresh(networkAllowed) },
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "News aktualisieren")
                        }
                        IconButton(onClick = news::close) {
                            Icon(Icons.Rounded.Close, contentDescription = "News schließen")
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = news.selectedCategory == null,
                            onClick = { news.selectCategory(null) },
                            label = { Text("Alle") },
                        )
                        NewsCategory.entries.forEach { category ->
                            FilterChip(
                                selected = news.selectedCategory == category,
                                onClick = { news.selectCategory(category) },
                                label = { Text(category.label) },
                            )
                        }
                    }

                    if (sourcesVisible) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text("Quellen", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Nur Metadaten werden geladen. Artikel öffnen immer bei der Originalquelle.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                news.sources.forEach { source ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(source.title, style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                source.category.label,
                                                color = MutedMist,
                                                style = MaterialTheme.typography.labelSmall,
                                            )
                                        }
                                        Switch(
                                            checked = source.id in news.enabledSourceIds,
                                            onCheckedChange = { news.setSourceEnabled(source.id, it) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
        ) {
            if (!networkAllowed) {
                Spacer(Modifier.height(14.dp))
                Surface(
                    color = RaisedSurface,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.WifiOff, contentDescription = null, tint = Sky)
                            Text("News-Netzwerk ist aus", fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            "KAL lädt News nie im Hintergrund. Für diesen Bereich muss der globale Netzwerk-Schalter ausdrücklich an sein.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(onClick = onOpenNetworkSettings) { Text("Datenschutz & Sicherheit öffnen") }
                    }
                }
            }

            news.notice?.let { notice ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        notice,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    TextButton(onClick = news::consumeNotice) { Text("OK") }
                }
            }

            if (news.loading) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text("News werden aktualisiert …") },
                    modifier = Modifier.padding(top = 10.dp),
                )
            }

            val items = news.visibleItems
            if (items.isEmpty() && !news.loading) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 30.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Rounded.Newspaper, contentDescription = null, tint = MutedMist)
                    Text("Noch keine Meldungen", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (networkAllowed) "Aktualisiere die gewählten Quellen." else "Aktiviere Netzwerkfeatures, wenn du News laden möchtest.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item { Spacer(Modifier.height(6.dp)) }
                    items(items, key = NewsItem::id) { item ->
                        NewsCard(item = item, onOpenArticle = onOpenArticle)
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun NewsCard(
    item: NewsItem,
    onOpenArticle: (String) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenArticle(item.url) },
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(item.sourceTitle, color = Mint, style = MaterialTheme.typography.labelLarge)
                Text("· ${item.category.label}", color = MutedMist, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.OpenInNew, contentDescription = null, tint = MutedMist)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f))
            Text(
                item.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            item.publishedAt?.let { published ->
                Text(
                    NEWS_TIME_FORMAT.format(published.atZone(ZoneId.systemDefault())),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private val NEWS_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy · HH:mm")
