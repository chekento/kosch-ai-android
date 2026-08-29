package cloud.kosch.aiandroid.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModelProvider
import cloud.kosch.aiandroid.LauncherViewModel
import cloud.kosch.aiandroid.model.SettingsSection
import cloud.kosch.aiandroid.ui.theme.DeepSurface

/** One quiet launcher entry keeps professional tools available without cluttering Home. */
@Composable
fun KalHomeQuickMenu(
    onSearch: () -> Unit,
    onAiHub: () -> Unit,
    onPersonalize: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = remember(context) { context.findComponentActivityForQuickMenu() }
    val viewModel = remember(activity) {
        activity?.let { ViewModelProvider(it)[LauncherViewModel::class.java] }
    }
    val news = viewModel?.news

    Box(
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 8.dp, end = 10.dp),
    ) {
        Surface(
            color = DeepSurface.copy(alpha = 0.74f),
            shape = RoundedCornerShape(18.dp),
            shadowElevation = 8.dp,
        ) {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    Icons.Rounded.MoreHoriz,
                    contentDescription = "KAL Menü",
                    tint = Color.White,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = DeepSurface.copy(alpha = 0.98f),
        ) {
            DropdownMenuItem(
                text = { Text("Suche", style = MaterialTheme.typography.bodyLarge) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                onClick = {
                    expanded = false
                    onSearch()
                },
            )
            DropdownMenuItem(
                text = { Text("News", style = MaterialTheme.typography.bodyLarge) },
                leadingIcon = { Icon(Icons.Rounded.Newspaper, contentDescription = null) },
                enabled = news != null,
                onClick = {
                    expanded = false
                    viewModel?.openNews()
                },
            )
            DropdownMenuItem(
                text = { Text("AI Hub", style = MaterialTheme.typography.bodyLarge) },
                leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                onClick = {
                    expanded = false
                    onAiHub()
                },
            )
            DropdownMenuItem(
                text = { Text("Anpassen", style = MaterialTheme.typography.bodyLarge) },
                leadingIcon = { Icon(Icons.Rounded.Palette, contentDescription = null) },
                onClick = {
                    expanded = false
                    onPersonalize()
                },
            )
            DropdownMenuItem(
                text = { Text("Einstellungen", style = MaterialTheme.typography.bodyLarge) },
                leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                onClick = {
                    expanded = false
                    onSettings()
                },
            )
        }
    }

    if (news?.visible == true && viewModel != null) {
        Dialog(
            onDismissRequest = news::close,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
            ),
        ) {
            NewsSurface(
                news = news,
                networkAllowed = viewModel.settings.document.privacy.allowNetworkFeatures,
                onOpenNetworkSettings = {
                    news.close()
                    viewModel.settings.open(SettingsSection.PRIVACY)
                },
                onOpenArticle = { url ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(intent) }
                        .onFailure { viewModel.controller.postNotice("Artikel konnte nicht im Browser geöffnet werden") }
                },
            )
        }
    }
}

private tailrec fun Context.findComponentActivityForQuickMenu(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivityForQuickMenu()
    else -> null
}
