package cloud.kosch.aiandroid.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardVoice
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.Workspaces
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cloud.kosch.aiandroid.AssistantAgentController
import cloud.kosch.aiandroid.AssistantSessionController
import cloud.kosch.aiandroid.LauncherController
import cloud.kosch.aiandroid.WorkspaceHomeController
import cloud.kosch.aiandroid.ai.SmartCollection
import cloud.kosch.aiandroid.model.AssistantVisualState
import cloud.kosch.aiandroid.model.FolderKind
import cloud.kosch.aiandroid.model.HomePage
import cloud.kosch.aiandroid.model.LaunchableApp
import cloud.kosch.aiandroid.model.LauncherFolder
import cloud.kosch.aiandroid.model.TileAction
import cloud.kosch.aiandroid.model.WorkspaceItem
import cloud.kosch.aiandroid.model.WorkspaceItemContent
import cloud.kosch.aiandroid.ui.components.CompanionFace
import cloud.kosch.aiandroid.ui.theme.DeepSurface
import cloud.kosch.aiandroid.ui.theme.Ink
import cloud.kosch.aiandroid.ui.theme.Mint
import cloud.kosch.aiandroid.ui.theme.MutedMist
import cloud.kosch.aiandroid.ui.theme.RaisedSurface
import cloud.kosch.aiandroid.ui.theme.Sky
import cloud.kosch.aiandroid.ui.theme.Violet
import cloud.kosch.aiandroid.ui.theme.Warm
import android.content.Context
import android.view.View

private enum class KALSection(val label: String, val shortLabel: String, val icon: ImageVector) {
    HOME("Home", "Home", Icons.Rounded.Home),
    APPS("Apps", "Apps", Icons.Rounded.Apps),
    WORKSPACE("Workspace", "Space", Icons.Rounded.Workspaces),
    NEWS("AI News", "News", Icons.Rounded.Newspaper),
    TOOLS("Tools", "Tools", Icons.Rounded.GridView),
    PEN_SPACE("Pen Space", "Pen", Icons.Rounded.Edit),
    SETTINGS("Settings", "Mehr", Icons.Rounded.Settings),
}

private data class KALTool(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val action: () -> Unit,
)

private data class KALNewsSource(
    val title: String,
    val category: String,
    val url: String,
)

private val KAL_NEWS_SOURCES = listOf(
    KALNewsSource("OpenAI News", "Modelle", "https://openai.com/news/"),
    KALNewsSource("Anthropic News", "Modelle", "https://www.anthropic.com/news"),
    KALNewsSource("Google AI Blog", "Research", "https://blog.google/technology/ai/"),
    KALNewsSource("Google DeepMind", "Research", "https://deepmind.google/discover/blog/"),
    KALNewsSource("Microsoft AI", "Produkte", "https://blogs.microsoft.com/ai/"),
    KALNewsSource("Meta AI", "Open Research", "https://ai.meta.com/blog/"),
    KALNewsSource("NVIDIA AI", "Hardware", "https://blogs.nvidia.com/blog/category/deep-learning/"),
    KALNewsSource("Hugging Face", "Open Source", "https://huggingface.co/blog"),
    KALNewsSource("Mistral AI", "Modelle", "https://mistral.ai/news/"),
    KALNewsSource("Cohere", "Enterprise AI", "https://cohere.com/blog"),
    KALNewsSource("xAI News", "Modelle", "https://x.ai/news"),
    KALNewsSource("Stability AI", "Generative Media", "https://stability.ai/news"),
    KALNewsSource("AWS Machine Learning", "Cloud", "https://aws.amazon.com/blogs/machine-learning/"),
    KALNewsSource("IBM Research", "Research", "https://research.ibm.com/blog"),
    KALNewsSource("Apple ML Research", "Research", "https://machinelearning.apple.com/"),
    KALNewsSource("Mozilla AI", "Responsible AI", "https://blog.mozilla.org/en/products/ai/"),
    KALNewsSource("Allen AI", "Research", "https://allenai.org/blog"),
    KALNewsSource("arXiv · cs.AI", "Papers", "https://arxiv.org/list/cs.AI/recent"),
    KALNewsSource("EU AI Act", "Governance", "https://digital-strategy.ec.europa.eu/en/policies/regulatory-framework-ai"),
    KALNewsSource("NIST AI RMF", "Governance", "https://www.nist.gov/itl/ai-risk-management-framework"),
    KALNewsSource("OWASP LLM Top 10", "Security", "https://owasp.org/www-project-top-10-for-large-language-model-applications/"),
    KALNewsSource("OECD.AI", "Policy", "https://oecd.ai/en/"),
    KALNewsSource("UK AI Security Institute", "Safety", "https://www.aisi.gov.uk/"),
)

/**
 * The single product shell for KAL. Legacy feature surfaces remain deliberately below this layer and
 * are opened as focused tasks. This keeps the Home experience calm without removing launcher parity.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun KALLauncherShell(
    controller: LauncherController,
    home: WorkspaceHomeController,
    assistant: AssistantSessionController,
    assistantAgent: AssistantAgentController,
    requestHomeRole: () -> Unit,
    requestVoiceInput: () -> Unit,
    requestDocument: () -> Unit,
    requestFileWorkspace: () -> Unit,
    requestContact: () -> Unit,
    requestWidget: () -> Unit,
    requestBackupExport: (String) -> Unit,
    requestBackupImport: () -> Unit,
    requestAuditExport: () -> Unit,
    requestInkExport: () -> Unit,
    createWidgetView: (Context, Int, cloud.kosch.aiandroid.model.WidgetSizePreset) -> View?,
    deleteWidget: (Int) -> Unit,
    forgetDocument: () -> Unit,
) {
    var sectionName by rememberSaveable { mutableStateOf(KALSection.HOME.name) }
    var command by rememberSaveable { mutableStateOf("") }
    var appQuery by rememberSaveable { mutableStateOf("") }
    var appCollectionName by rememberSaveable { mutableStateOf(SmartCollection.ALL.name) }
    var pageDialogVisible by rememberSaveable { mutableStateOf(false) }
    var pageTitleDraft by rememberSaveable { mutableStateOf("") }
    var pageRenameDraft by rememberSaveable { mutableStateOf("") }
    val section = KALSection.entries.firstOrNull { it.name == sectionName } ?: KALSection.HOME
    val appCollection = SmartCollection.entries.firstOrNull { it.name == appCollectionName } ?: SmartCollection.ALL
    val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val hasTransientSurface = controller.drawerVisible || controller.providerChooserVisible ||
        controller.contextDetailsVisible || controller.controlCenterVisible || controller.phoneVisible ||
        controller.fileSheetVisible || controller.fileWorkspaceVisible || controller.widgetBoardVisible ||
        controller.appActionsVisible || controller.folderSheetVisible || controller.faqVisible ||
        controller.backupVisible || controller.auditVisible

    BackHandler(enabled = hasTransientSurface) { controller.closeTopSurface() }

    LaunchedEffect(controller.notice) {
        controller.notice?.let {
            snackbarHostState.showSnackbar(it)
            controller.consumeNotice()
        }
    }
    LaunchedEffect(home.statusMessage) {
        home.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            home.consumeStatus()
        }
    }
    LaunchedEffect(controller.commandFocusRequest) {
        if (controller.commandFocusRequest > 0L) keyboardController?.show()
    }

    // Keep the legacy HOME contract as a compatibility bridge for keyboard shortcuts,
    // persisted launcher state and existing integrations. The visible product remains the
    // single KAL shell; old page changes simply land in the corresponding KAL section.
    LaunchedEffect(controller.homePage) {
        sectionName = when (controller.homePage) {
            HomePage.WORKSPACE -> KALSection.WORKSPACE.name
            HomePage.PEN_SPACE -> KALSection.PEN_SPACE.name
            HomePage.PRO_DESK, HomePage.SMART_SPACE -> KALSection.HOME.name
        }
    }

    val submitCommand = {
        val text = command.trim()
        if (text.isNotEmpty()) {
            controller.submitCommand(text, requestVoiceInput, requestDocument, requestContact)
            command = ""
            keyboardController?.hide()
        }
    }
    val navigate: (KALSection) -> Unit = { target ->
        controller.closeTopSurface()
        when (target) {
            KALSection.WORKSPACE -> controller.switchHomePage(HomePage.WORKSPACE)
            KALSection.PEN_SPACE -> controller.switchHomePage(HomePage.PEN_SPACE)
            else -> Unit
        }
        sectionName = target.name
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Ink)
                .padding(padding),
        ) {
            NeuralGlassBackground(Modifier.fillMaxSize())
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val wide = maxWidth >= 840.dp || (maxWidth >= 700.dp && maxWidth > maxHeight)
                if (wide) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        KALSideRail(section, navigate, controller.isDefaultHome, requestHomeRole)
                        KALMainArea(
                            section = section,
                            controller = controller,
                            home = home,
                            assistant = assistant,
                            assistantAgent = assistantAgent,
                            command = command,
                            onCommandChange = { command = it },
                            onSubmitCommand = submitCommand,
                            appQuery = appQuery,
                            onAppQueryChange = { appQuery = it },
                            appCollection = appCollection,
                            onCollectionChange = { appCollectionName = it.name },
                            navigate = navigate,
                            requestVoiceInput = requestVoiceInput,
                            requestDocument = requestDocument,
                            requestFileWorkspace = requestFileWorkspace,
                            requestContact = requestContact,
                            requestWidget = requestWidget,
                            requestInkExport = requestInkExport,
                            onOpenAssistant = assistant::open,
                            requestHomeRole = requestHomeRole,
                            pageDialog = {
                                pageRenameDraft = home.activePage.title
                                pageDialogVisible = true
                            },
                            onOpenNews = { controller.openExternalUrl(it, "AI-News") },
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        KALTopBar(
                            controller = controller,
                            assistant = assistant,
                            assistantAgent = assistantAgent,
                            onOpenAssistant = assistant::open,
                            onOpenSettings = { navigate(KALSection.SETTINGS) },
                        )
                        KALMainArea(
                            section = section,
                            controller = controller,
                            home = home,
                            assistant = assistant,
                            assistantAgent = assistantAgent,
                            command = command,
                            onCommandChange = { command = it },
                            onSubmitCommand = submitCommand,
                            appQuery = appQuery,
                            onAppQueryChange = { appQuery = it },
                            appCollection = appCollection,
                            onCollectionChange = { appCollectionName = it.name },
                            navigate = navigate,
                            requestVoiceInput = requestVoiceInput,
                            requestDocument = requestDocument,
                            requestFileWorkspace = requestFileWorkspace,
                            requestContact = requestContact,
                            requestWidget = requestWidget,
                            requestInkExport = requestInkExport,
                            onOpenAssistant = assistant::open,
                            requestHomeRole = requestHomeRole,
                            pageDialog = {
                                pageRenameDraft = home.activePage.title
                                pageDialogVisible = true
                            },
                            onOpenNews = { controller.openExternalUrl(it, "AI-News") },
                            modifier = Modifier.weight(1f),
                        )
                        KALBottomBar(section, navigate)
                    }
                }

            }

            KALLauncherOverlays(
                controller = controller,
                requestHomeRole = requestHomeRole,
                requestVoiceInput = requestVoiceInput,
                requestDocument = requestDocument,
                requestFileWorkspace = requestFileWorkspace,
                requestContact = requestContact,
                requestWidget = requestWidget,
                requestBackupExport = requestBackupExport,
                requestBackupImport = requestBackupImport,
                requestAuditExport = requestAuditExport,
                requestInkExport = requestInkExport,
                createWidgetView = createWidgetView,
                deleteWidget = deleteWidget,
                forgetDocument = forgetDocument,
            )

            if (pageDialogVisible) {
                PageManagerDialog(
                    home = home,
                    titleDraft = pageTitleDraft,
                    onTitleDraftChange = { pageTitleDraft = it },
                    renameDraft = pageRenameDraft,
                    onRenameDraftChange = { pageRenameDraft = it },
                    onDismiss = { pageDialogVisible = false },
                    onCreate = {
                        home.createPage(pageTitleDraft)
                        pageTitleDraft = ""
                        pageDialogVisible = false
                    },
                    onRename = {
                        home.renameActivePage(pageRenameDraft)
                        pageDialogVisible = false
                    },
                    onMovePrevious = { home.moveActivePage(-1) },
                    onMoveNext = { home.moveActivePage(1) },
                    onDelete = {
                        home.deleteActiveUserPage()
                        pageDialogVisible = false
                    },
                )
            }

        }
    }
}

@Composable
private fun KALMainArea(
    section: KALSection,
    controller: LauncherController,
    home: WorkspaceHomeController,
    assistant: AssistantSessionController,
    assistantAgent: AssistantAgentController,
    command: String,
    onCommandChange: (String) -> Unit,
    onSubmitCommand: () -> Unit,
    appQuery: String,
    onAppQueryChange: (String) -> Unit,
    appCollection: SmartCollection,
    onCollectionChange: (SmartCollection) -> Unit,
    navigate: (KALSection) -> Unit,
    requestVoiceInput: () -> Unit,
    requestDocument: () -> Unit,
    requestFileWorkspace: () -> Unit,
    requestContact: () -> Unit,
    requestWidget: () -> Unit,
    requestInkExport: () -> Unit,
    onOpenAssistant: () -> Unit,
    requestHomeRole: () -> Unit,
    pageDialog: () -> Unit,
    onOpenNews: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth >= 700.dp) {
                KALTopBar(
                    controller = controller,
                    assistant = assistant,
                    assistantAgent = assistantAgent,
                    onOpenAssistant = onOpenAssistant,
                    onOpenSettings = { navigate(KALSection.SETTINGS) },
                )
            }
        }
        when (section) {
            KALSection.HOME -> KALHomeSection(
                controller = controller,
                assistant = assistant,
                assistantAgent = assistantAgent,
                command = command,
                onCommandChange = onCommandChange,
                onSubmitCommand = onSubmitCommand,
                requestVoiceInput = requestVoiceInput,
                requestDocument = requestDocument,
                requestFileWorkspace = requestFileWorkspace,
                requestContact = requestContact,
                requestWidget = requestWidget,
                onOpenAssistant = onOpenAssistant,
                navigate = navigate,
            )
            KALSection.APPS -> KALAppsSection(
                controller = controller,
                query = appQuery,
                onQueryChange = onAppQueryChange,
                collection = appCollection,
                onCollectionChange = onCollectionChange,
            )
            KALSection.WORKSPACE -> KALWorkspaceSection(
                controller = controller,
                home = home,
                pageDialog = pageDialog,
                navigate = navigate,
            )
            KALSection.NEWS -> KALNewsSection(controller, onOpenNews)
            KALSection.TOOLS -> KALToolsSection(
                controller = controller,
                navigate = navigate,
                requestDocument = requestDocument,
                requestFileWorkspace = requestFileWorkspace,
                requestContact = requestContact,
                requestVoiceInput = requestVoiceInput,
                requestWidget = requestWidget,
                requestHomeRole = requestHomeRole,
                requestInkExport = requestInkExport,
                onOpenAssistant = onOpenAssistant,
            )
            KALSection.PEN_SPACE -> PenSpaceSurface(
                controller = controller,
                onAsk = { navigate(KALSection.HOME) },
                onSystemNote = controller::createSystemNote,
                onExport = requestInkExport,
            )
            KALSection.SETTINGS -> KALSettingsSection(
                controller = controller,
                requestHomeRole = requestHomeRole,
                navigate = navigate,
                onOpenAssistant = onOpenAssistant,
            )
        }
    }
}

@Composable
private fun KALTopBar(
    controller: LauncherController,
    assistant: AssistantSessionController,
    assistantAgent: AssistantAgentController,
    onOpenAssistant: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text("KAL", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Mist())
            Text("KoSch AI Launcher", style = MaterialTheme.typography.labelMedium, color = MutedMist)
        }
        Surface(
            color = if (controller.isDefaultHome) Mint.copy(alpha = 0.13f) else Warm.copy(alpha = 0.13f),
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (controller.isDefaultHome) Icons.Rounded.CheckCircle else Icons.Rounded.Home,
                    contentDescription = null,
                    tint = if (controller.isDefaultHome) Mint else Warm,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(if (controller.isDefaultHome) "HOME aktiv" else "Testmodus", style = MaterialTheme.typography.labelSmall)
            }
        }
        Surface(
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).combinedClickable(onClick = onOpenAssistant),
            color = RaisedSurface,
            shape = RoundedCornerShape(16.dp),
        ) {
            CompanionFace(onClick = onOpenAssistant, modifier = Modifier.fillMaxSize())
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Rounded.Settings, contentDescription = "KAL Einstellungen", tint = Sky)
        }
    }
}

@Composable
private fun KALSideRail(
    section: KALSection,
    navigate: (KALSection) -> Unit,
    isDefaultHome: Boolean,
    requestHomeRole: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxHeight().widthIn(min = 190.dp, max = 230.dp),
        color = DeepSurface.copy(alpha = 0.88f),
        shape = RoundedCornerShape(26.dp),
        tonalElevation = 6.dp,
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            KALBrandMark()
            Spacer(Modifier.height(10.dp))
            KALSection.entries.forEach { item ->
                KALNavItem(item, selected = section == item, onClick = { navigate(item) }, compact = false)
            }
            Spacer(Modifier.weight(1f))
            Surface(color = Sky.copy(alpha = 0.08f), shape = RoundedCornerShape(17.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("LOCAL CORE", color = Mint, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("Offline bereit. Jede externe Übergabe bleibt sichtbar und bewusst.", color = MutedMist, style = MaterialTheme.typography.bodySmall)
                    if (!isDefaultHome) {
                        TextButton(onClick = requestHomeRole, contentPadding = PaddingValues(0.dp)) {
                            Text("Als HOME festlegen")
                        }
                    }
                }
            }
            Text("KAL · KoSch AI Launcher", color = MutedMist, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun KALBrandMark() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            modifier = Modifier.size(42.dp),
            color = Mint.copy(alpha = 0.15f),
            shape = RoundedCornerShape(14.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("K", color = Mint, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            }
        }
        Column {
            Text("KAL", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
            Text("AI Launcher", color = MutedMist, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun KALBottomBar(section: KALSection, navigate: (KALSection) -> Unit) {
    Surface(color = DeepSurface.copy(alpha = 0.97f), tonalElevation = 8.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            KALSection.entries.forEach { item ->
                KALNavItem(item, selected = section == item, onClick = { navigate(item) }, compact = true)
            }
        }
    }
}

@Composable
private fun KALNavItem(item: KALSection, selected: Boolean, onClick: () -> Unit, compact: Boolean) {
    val label = if (compact) item.shortLabel else item.label
    Surface(
        modifier = Modifier
            .fillMaxWidth(if (compact) 0.19f else 1f)
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                role = Role.Tab,
                onClick = onClick,
            )
            .semantics { contentDescription = item.label; role = Role.Tab },
        color = if (selected) Mint.copy(alpha = 0.16f) else Color.Transparent,
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = if (compact) 8.dp else 11.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(item.icon, contentDescription = null, tint = if (selected) Mint else MutedMist, modifier = Modifier.size(19.dp))
            Text(label, color = if (selected) Mist() else MutedMist, style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
private fun KALHomeSection(
    controller: LauncherController,
    assistant: AssistantSessionController,
    assistantAgent: AssistantAgentController,
    command: String,
    onCommandChange: (String) -> Unit,
    onSubmitCommand: () -> Unit,
    requestVoiceInput: () -> Unit,
    requestDocument: () -> Unit,
    requestFileWorkspace: () -> Unit,
    requestContact: () -> Unit,
    requestWidget: () -> Unit,
    onOpenAssistant: () -> Unit,
    navigate: (KALSection) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Guten Tag", color = MutedMist, style = MaterialTheme.typography.labelLarge)
                Text("Was soll KAL für dich erledigen?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
        }
        item {
            KALCommandCard(
                command = command,
                onCommandChange = onCommandChange,
                onSubmit = onSubmitCommand,
                onVoice = requestVoiceInput,
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { KALQuickAction("Telefon", Icons.Rounded.Phone, Sky) { controller.openPhone() } }
                item { KALQuickAction("Datei prüfen", Icons.Rounded.FolderOpen, Violet, requestDocument) }
                item { KALQuickAction("Kalender", Icons.Rounded.CalendarMonth, Mint, controller::openCalendar) }
                item { KALQuickAction("Kamera", Icons.Rounded.PhotoCamera, Warm, controller::openCamera) }
                item { KALQuickAction("Kontakt", Icons.Rounded.Phone, Sky, requestContact) }
            }
        }
        item {
            AssistantStatusCard(
                assistant = assistant,
                assistantAgent = assistantAgent,
                onOpen = onOpenAssistant,
            )
        }
        item {
            HomePersonalSpace(
                controller = controller,
                navigate = navigate,
                requestWidget = requestWidget,
            )
        }
        item {
            KALContextCard(controller)
        }
        item {
            KALPrivacyCard(controller)
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun KALCommandCard(
    command: String,
    onCommandChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onVoice: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = RaisedSurface.copy(alpha = 0.94f)),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Mint.copy(alpha = 0.30f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(color = Mint.copy(alpha = 0.14f), shape = CircleShape) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Mint, modifier = Modifier.padding(7.dp).size(18.dp))
                }
                Text("Command Center", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("lokal zuerst", color = Mint, style = MaterialTheme.typography.labelSmall)
            }
            OutlinedTextField(
                value = command,
                onValueChange = { onCommandChange(it.take(4_096)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Öffne Kamera, starte eine App, analysiere eine Datei …") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSubmit() }),
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    Row {
                        IconButton(onClick = onVoice) { Icon(Icons.Rounded.KeyboardVoice, contentDescription = "Spracheingabe") }
                        IconButton(onClick = onSubmit, enabled = command.isNotBlank()) { Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Befehl ausführen") }
                    }
                },
            )
            Text(
                "KAL zeigt Vorschauen und übergibt externe Aktionen erst nach deiner Entscheidung.",
                color = MutedMist,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun AssistantStatusCard(
    assistant: AssistantSessionController,
    assistantAgent: AssistantAgentController,
    onOpen: () -> Unit,
) {
    Card(
        onClick = onOpen,
        colors = CardDefaults.cardColors(containerColor = DeepSurface.copy(alpha = 0.94f)),
        shape = RoundedCornerShape(22.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Sky.copy(alpha = 0.26f)),
    ) {
        Row(Modifier.fillMaxWidth().padding(15.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Sky.copy(alpha = 0.13f), shape = RoundedCornerShape(17.dp), modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Sky, modifier = Modifier.size(26.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("KAL Assistant", fontWeight = FontWeight.SemiBold)
                Text(
                    if (assistant.settings.enabled) "${assistantAgent.character.displayName} · ${assistantVisualLabel(assistant.visualState)}" else "Opt-in · Sprache, Screen und Kamera bleiben aus",
                    color = MutedMist,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    if (assistant.settings.enabled) "Chat, Sprache und Agenten-Steuerung öffnen" else "Tippen zum Aktivieren und Konfigurieren",
                    color = Sky,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Icon(Icons.Rounded.MoreHoriz, contentDescription = null, tint = Sky)
        }
    }
}

@Composable
private fun HomePersonalSpace(
    controller: LauncherController,
    navigate: (KALSection) -> Unit,
    requestWidget: () -> Unit,
) {
    val pinned = controller.smartDockApps().take(8)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Dein Raum", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Apps, Ordner und Widgets auf einen Blick", color = MutedMist, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = { navigate(KALSection.WORKSPACE) }) { Text("Workspace") }
        }
        Surface(color = DeepSurface.copy(alpha = 0.84f), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (pinned.isEmpty() && controller.folders.isEmpty()) {
                    EmptySpaceHint(requestWidget)
                } else {
                    if (pinned.isNotEmpty()) {
                        Text("Smart Dock", color = MutedMist, style = MaterialTheme.typography.labelMedium)
                        FlowRow(maxItemsInEachRow = 4, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            pinned.forEach { app -> CompactAppChip(app, controller::launch) }
                        }
                    }
                    if (controller.folders.isNotEmpty()) {
                        Text("Ordner", color = MutedMist, style = MaterialTheme.typography.labelMedium)
                        FlowRow(maxItemsInEachRow = 3, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            controller.folders.take(6).forEach { folder -> FolderChip(folder) { controller.openFolder(folder.id) } }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { navigate(KALSection.APPS) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Apps, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Apps")
                    }
                    OutlinedButton(onClick = requestWidget, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Widgets, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Widget +")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySpaceHint(requestWidget: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Icon(Icons.Rounded.PushPin, contentDescription = null, tint = MutedMist, modifier = Modifier.size(30.dp))
        Text("Noch kein persönlicher Inhalt angepinnt", color = MutedMist, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = requestWidget) { Text("Widget hinzufügen") }
    }
}

@Composable
private fun KALContextCard(controller: LauncherController) {
    val context = controller.contextSnapshot
    Surface(color = Violet.copy(alpha = 0.09f), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Violet)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Lokaler Kontext", fontWeight = FontWeight.SemiBold)
                Text(
                    "${context.hour.toString().padStart(2, '0')}:${context.minute.toString().padStart(2, '0')} · ${context.batteryPercent?.let { "$it % Akku" } ?: "Akku unbekannt"} · ${if (context.hasNetwork) "verbunden" else "offline"}",
                    color = MutedMist,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text("Vorschlag: ${context.suggestedScene.title} · ${context.reasons.firstOrNull() ?: "keine automatische Änderung"}", color = Violet, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun KALPrivacyCard(controller: LauncherController) {
    Surface(color = Mint.copy(alpha = 0.08f), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = Mint)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Local-first by design", fontWeight = FontWeight.SemiBold)
                Text("Kein API-Key, kein Konto und kein verstecktes INTERNET-Recht im Offline-Kern.", color = MutedMist, style = MaterialTheme.typography.bodySmall)
            }
            if (controller.notificationAccessGranted) Icon(Icons.Rounded.CheckCircle, contentDescription = "Benachrichtigungszugriff aktiv", tint = Mint)
        }
    }
}

@Composable
private fun KALQuickAction(title: String, icon: ImageVector, tint: Color, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(title) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp)) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = tint.copy(alpha = 0.10f),
            labelColor = MaterialTheme.colorScheme.onSurface,
            leadingIconContentColor = tint,
        ),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KALAppsSection(
    controller: LauncherController,
    query: String,
    onQueryChange: (String) -> Unit,
    collection: SmartCollection,
    onCollectionChange: (SmartCollection) -> Unit,
) {
    val apps = controller.rankedApps(query, collection)
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Apps", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("LauncherApps · profilbewusst · lokal sortiert", color = MutedMist, style = MaterialTheme.typography.bodySmall)
            }
            Text("${apps.size}", color = Mint, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        OutlinedTextField(
            value = query,
            onValueChange = { onQueryChange(it.take(128)) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            placeholder = { Text("App oder Anbieter suchen") },
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(bottom = 10.dp)) {
            items(SmartCollection.entries.filterNot { it == SmartCollection.HIDDEN }) { item ->
                FilterChip(selected = item == collection, onClick = { onCollectionChange(item) }, label = { Text(item.title) })
            }
        }
        if (controller.appsLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Apps werden geladen …", color = MutedMist) }
        } else if (apps.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Apps, contentDescription = null, tint = MutedMist, modifier = Modifier.size(40.dp))
                    Text("Keine passende App", color = MutedMist)
                    TextButton(onClick = { onQueryChange(""); onCollectionChange(SmartCollection.ALL) }) { Text("Filter zurücksetzen") }
                }
            }
        } else {
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(92.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                items(apps.size, key = { apps[it].key }) { index ->
                    val app = apps[index]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { controller.launch(app) },
                                onLongClick = { controller.showAppActions(app) },
                            )
                            .semantics { contentDescription = "${app.label}, ${app.profile.title}" },
                        colors = CardDefaults.cardColors(containerColor = RaisedSurface.copy(alpha = 0.92f)),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Image(app.icon, contentDescription = null, modifier = Modifier.size(42.dp))
                            Text(app.label, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Text(app.profile.title, color = if (app.profile.name == "WORK") Sky else MutedMist, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactAppChip(app: LaunchableApp, onLaunch: (LaunchableApp) -> Unit) {
    AssistChip(
        onClick = { onLaunch(app) },
        label = { Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Image(app.icon, contentDescription = null, modifier = Modifier.size(19.dp)) },
    )
}

@Composable
private fun FolderChip(folder: LauncherFolder, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(folder.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, tint = Violet, modifier = Modifier.size(18.dp)) },
    )
}

@Composable
private fun KALWorkspaceSection(
    controller: LauncherController,
    home: WorkspaceHomeController,
    pageDialog: () -> Unit,
    navigate: (KALSection) -> Unit,
) {
    val page = home.activePage
    var addVisible by androidx.compose.runtime.remember { mutableStateOf(false) }
    var arrangeVisible by androidx.compose.runtime.remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Workspace", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Persönliche Seiten, Apps und Ordner", color = MutedMist, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { addVisible = true }) { Icon(Icons.Rounded.Add, contentDescription = "Workspace-Element hinzufügen") }
            AssistChip(
                onClick = { arrangeVisible = true },
                label = { Text("Anordnen") },
                leadingIcon = { Icon(Icons.Rounded.DragIndicator, contentDescription = null) },
            )
            IconButton(onClick = pageDialog) { Icon(Icons.Rounded.Edit, contentDescription = "Workspace-Seiten verwalten") }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
            items(home.document.pages, key = { it.id }) { item ->
                FilterChip(selected = item.id == page.id, onClick = { home.activatePage(item.id) }, label = { Text(item.title) })
            }
            item {
                AssistChip(onClick = pageDialog, label = { Text("Seite +") }, leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null) })
            }
        }
        Surface(color = DeepSurface.copy(alpha = 0.84f), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (page.items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Workspaces, contentDescription = null, tint = MutedMist, modifier = Modifier.size(44.dp))
                        Text("Diese Seite ist noch leer", color = MutedMist)
                        OutlinedButton(onClick = { navigate(KALSection.APPS) }) { Text("Apps hinzufügen") }
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Surface(color = Sky.copy(alpha = 0.08f), shape = RoundedCornerShape(14.dp)) {
                            Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Tune, contentDescription = null, tint = Sky, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Tippe auf ein Element zum Starten. Anordnen und Seitenwechsel bleiben im Workspace-Editor verfügbar.", color = MutedMist, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    items(page.items, key = { it.id }) { item -> WorkspaceItemRow(controller, home, item) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
    if (addVisible) {
        WorkspaceAddDialog(
            controller = controller,
            home = home,
            onDismiss = { addVisible = false },
        )
    }
    if (arrangeVisible) {
        WorkspaceArrangeDialog(
            controller = controller,
            home = home,
            onDismiss = { arrangeVisible = false },
        )
    }
}

@Composable
private fun WorkspaceItemRow(controller: LauncherController, home: WorkspaceHomeController, item: WorkspaceItem) {
    val title: String
    val subtitle: String
    val icon: ImageVector
    val action: () -> Unit
    when (val content = item.content) {
        is WorkspaceItemContent.App -> {
            val app = controller.apps.firstOrNull { it.key == content.appKey }
            title = app?.label ?: "App fehlt"
            subtitle = app?.profile?.title ?: "App"
            icon = Icons.Rounded.Apps
            action = { app?.let(controller::launch) }
        }
        is WorkspaceItemContent.Folder -> {
            val folder = controller.folders.firstOrNull { it.id == content.folderId }
            title = folder?.title ?: "Ordner nicht verfügbar"
            subtitle = "Ordner · ${folder?.appKeys?.size ?: 0} Apps"
            icon = Icons.Rounded.CreateNewFolder
            action = { folder?.let { controller.openFolder(it.id) } }
        }
        is WorkspaceItemContent.Widget -> {
            title = "Widget"
            subtitle = "Gerätegebundene Kachel"
            icon = Icons.Rounded.Widgets
            action = controller::openWidgetBoard
        }
        is WorkspaceItemContent.ActionTile -> {
            title = content.legacyTileId.substringAfterLast(':').replace('-', ' ').replaceFirstChar { it.uppercase() }
            subtitle = content.action.name.lowercase().replace('_', ' ')
            icon = Icons.Rounded.AutoAwesome
            action = { runWorkspaceAction(controller, content.action) }
        }
    }
    Card(onClick = action, colors = CardDefaults.cardColors(containerColor = RaisedSurface), shape = RoundedCornerShape(17.dp)) {
        Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(color = Sky.copy(alpha = 0.10f), shape = RoundedCornerShape(12.dp), modifier = Modifier.size(39.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = Sky, modifier = Modifier.size(21.dp)) }
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = MutedMist, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (home.isUserPage()) {
                IconButton(onClick = { home.moveItemBy(item.id, 1, 0) }) { Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Element nach rechts") }
                IconButton(onClick = { home.removeItem(item.id) }) { Icon(Icons.Rounded.DeleteOutline, contentDescription = "Element vom Workspace entfernen", tint = Warm) }
            }
        }
    }
}

@Composable
private fun WorkspaceAddDialog(
    controller: LauncherController,
    home: WorkspaceHomeController,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().padding(22.dp), color = DeepSurface, shape = RoundedCornerShape(25.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Zum Workspace hinzufügen", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("KAL legt Apps und Ordner auf der aktiven persönlichen Seite ab. Bei Bedarf wird eine neue Seite angelegt.", color = MutedMist, style = MaterialTheme.typography.bodySmall)
                LazyColumn(Modifier.fillMaxWidth().height(420.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    item { Text("Apps", color = MutedMist, style = MaterialTheme.typography.labelLarge) }
                    items(controller.apps.take(40), key = { "app:${it.key}" }) { app ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Image(app.icon, contentDescription = null, modifier = Modifier.size(30.dp))
                            Text(app.label, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            TextButton(onClick = { home.addApp(app.key); onDismiss() }) { Text("Hinzufügen") }
                        }
                    }
                    if (controller.folders.isNotEmpty()) {
                        item { Text("Ordner", color = MutedMist, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 7.dp)) }
                        items(controller.folders, key = { "folder:${it.id}" }) { folder ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, tint = Violet, modifier = Modifier.size(28.dp))
                                Text(folder.title, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                TextButton(onClick = { home.addFolder(folder.id); onDismiss() }) { Text("Hinzufügen") }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onDismiss) { Text("Schließen") } }
            }
        }
    }
}

private fun runWorkspaceAction(controller: LauncherController, action: TileAction) {
    when (action) {
        TileAction.ASK -> controller.requestCommandFocus()
        TileAction.APPS -> controller.openDrawer()
        TileAction.CONTEXT -> controller.showContextDetails()
        TileAction.PROVIDERS -> controller.openProviderChooser()
        TileAction.FOCUS -> controller.openProDesk()
        TileAction.MEDIA -> controller.openCamera()
        TileAction.COMMUNICATION -> controller.openPhone()
        TileAction.TOOLS -> controller.openControlCenter()
    }
}

@Composable
private fun KALNewsSection(controller: LauncherController, onOpenNews: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("AI News", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Quellenhub für Modelle, Produkte, Open Source und Governance", color = MutedMist, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { controller.postNotice("Quellenliste ist lokal verfügbar") }) { Icon(Icons.Rounded.Refresh, contentDescription = "Newsquellen aktualisieren") }
            }
        }
        item {
            Surface(color = Sky.copy(alpha = 0.09f), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(13.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = Sky)
                    Text("KAL lädt keine News heimlich im Hintergrund. Öffne eine Quelle bewusst im Browser; der Offline-Kern bleibt ohne INTERNET-Recht.", color = MutedMist, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        items(KAL_NEWS_SOURCES, key = { it.url }) { source ->
            Card(onClick = { onOpenNews(source.url) }, colors = CardDefaults.cardColors(containerColor = RaisedSurface), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    Surface(color = Mint.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp), modifier = Modifier.size(40.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Language, contentDescription = null, tint = Mint) }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(source.title, fontWeight = FontWeight.SemiBold)
                        Text(source.category, color = MutedMist, style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Rounded.Language, contentDescription = "Quelle öffnen", tint = Sky, modifier = Modifier.size(19.dp))
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun KALToolsSection(
    controller: LauncherController,
    navigate: (KALSection) -> Unit,
    requestDocument: () -> Unit,
    requestFileWorkspace: () -> Unit,
    requestContact: () -> Unit,
    requestVoiceInput: () -> Unit,
    requestWidget: () -> Unit,
    requestHomeRole: () -> Unit,
    requestInkExport: () -> Unit,
    onOpenAssistant: () -> Unit,
) {
    val tools = listOf(
        KALTool("Assistant", "Chat, Sprache und Agent", Icons.Rounded.AutoAwesome, Sky, onOpenAssistant),
        KALTool("Datei-KI", "Ein Dokument lokal prüfen", Icons.Rounded.FolderOpen, Violet, requestDocument),
        KALTool("Datei-Arbeitsraum", "Gewählten SAF-Ordner verwalten", Icons.Rounded.Storage, Violet, requestFileWorkspace),
        KALTool("Telefon", "Dialer und Kontakt-Picker", Icons.Rounded.Phone, Sky, controller::openPhone),
        KALTool("Kalender", "Systemkalender öffnen", Icons.Rounded.CalendarMonth, Mint, controller::openCalendar),
        KALTool("Kamera", "Systemkamera öffnen", Icons.Rounded.PhotoCamera, Warm, controller::openCamera),
        KALTool("Widgets", "Board und Größen", Icons.Rounded.Widgets, Mint, requestWidget),
        KALTool("Kontrollzentrum", "Android und KAL", Icons.Rounded.Tune, Sky, controller::openControlCenter),
        KALTool("Sicherheit", "Netzwerk und Schutz", Icons.Rounded.Shield, Mint, { controller.openSystemPanel(cloud.kosch.aiandroid.model.SystemPanel.PRIVACY) }),
        KALTool("Pen Space", "Stift, Notiz und SVG", Icons.Rounded.Edit, Violet, { navigate(KALSection.PEN_SPACE) }),
        KALTool("Sprache", "Android Speech UI", Icons.Rounded.KeyboardVoice, Sky, requestVoiceInput),
        KALTool("Backup", "Verschlüsselt sichern", Icons.Rounded.Backup, Warm, controller::openBackup),
        KALTool("Audit", "Lokale Ereignisse", Icons.Rounded.CheckCircle, Sky, controller::openAudit),
        KALTool("AI News", "Quellen und Governance", Icons.Rounded.Newspaper, Mint, { navigate(KALSection.NEWS) }),
        KALTool("HOME-Ausgang", "Android Launcher wählen", Icons.Rounded.Home, Warm, requestHomeRole),
        KALTool("Hilfe", "FAQ und Recovery", Icons.Rounded.HelpOutline, Sky, controller::openFaq),
    )
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Tools", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Alles Wichtige an einem Ort – mit klaren Grenzen", color = MutedMist, style = MaterialTheme.typography.bodySmall)
        }
        item {
            FlowRow(maxItemsInEachRow = 2, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                tools.forEach { tool -> KALToolCard(tool, Modifier.weight(1f)) }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun KALToolCard(tool: KALTool, modifier: Modifier = Modifier) {
    Card(onClick = tool.action, modifier = modifier.widthIn(min = 145.dp), colors = CardDefaults.cardColors(containerColor = RaisedSurface), shape = RoundedCornerShape(19.dp)) {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(color = tool.accent.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp), modifier = Modifier.size(35.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(tool.icon, contentDescription = null, tint = tool.accent, modifier = Modifier.size(19.dp)) }
            }
            Text(tool.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(tool.subtitle, color = MutedMist, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun KALSettingsSection(
    controller: LauncherController,
    requestHomeRole: () -> Unit,
    navigate: (KALSection) -> Unit,
    onOpenAssistant: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("KAL kontrollieren, Datenschutz verstehen, Recovery behalten", color = MutedMist, style = MaterialTheme.typography.bodySmall)
        }
        item {
            SettingsGroup("KAL") {
                SettingsRow("Kontrollzentrum", "Systembereiche, Widgets, Benachrichtigungen", Icons.Rounded.Tune) { controller.openControlCenter() }
                SettingsRow("Assistant", "Charakter, Stimmen, Wake Word, Screen/Cam opt-in", Icons.Rounded.AutoAwesome, onClick = onOpenAssistant)
                SettingsRow("Als HOME festlegen", if (controller.isDefaultHome) "KAL ist die aktive Start-App" else "Android zeigt den geschützten Dialog", Icons.Rounded.Home, enabled = !controller.isDefaultHome, onClick = requestHomeRole)
            }
        }
        item {
            SettingsGroup("Daten & Recovery") {
                SettingsRow("Verschlüsseltes Backup", "Workspace und persönliche Layoutdaten", Icons.Rounded.Backup) { controller.openBackup() }
                SettingsRow("Lokales Audit", "Keine Prompts, Namen oder Inhalte", Icons.Rounded.CheckCircle) { controller.openAudit() }
                SettingsRow("FAQ & Recovery", "Selbsthilfe und Sicherheitsgrenzen", Icons.Rounded.HelpOutline) { controller.openFaq() }
            }
        }
        item {
            SettingsGroup("Navigation") {
                SettingsRow("Apps", "LauncherApps, Suche und Profile", Icons.Rounded.Apps) { navigate(KALSection.APPS) }
                SettingsRow("Workspace", "Persönliche Seiten und Elemente", Icons.Rounded.Workspaces) { navigate(KALSection.WORKSPACE) }
                SettingsRow("AI News", "Quellenhub ohne Hintergrundnetzwerk", Icons.Rounded.Newspaper) { navigate(KALSection.NEWS) }
            }
        }
        item {
            Surface(color = Mint.copy(alpha = 0.08f), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(13.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Security, contentDescription = null, tint = Mint)
                    Text("KAL sperrt dich nicht ein: Androids Start-App-Auswahl bleibt jederzeit erreichbar.", color = MutedMist, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(title, color = MutedMist, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Surface(color = DeepSurface.copy(alpha = 0.84f), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), content = content)
        }
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, icon: ImageVector, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).combinedClickable(enabled = enabled, onClick = onClick).padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) Sky else MutedMist, modifier = Modifier.size(21.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = if (enabled) MaterialTheme.colorScheme.onSurface else MutedMist, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MutedMist, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PageManagerDialog(
    home: WorkspaceHomeController,
    titleDraft: String,
    onTitleDraftChange: (String) -> Unit,
    renameDraft: String,
    onRenameDraftChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
    onRename: () -> Unit,
    onMovePrevious: () -> Unit,
    onMoveNext: () -> Unit,
    onDelete: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().padding(22.dp), color = DeepSurface, shape = RoundedCornerShape(25.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Workspace verwalten", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Seiten bleiben dauerhaft gespeichert. Die letzte Seite kann nicht gelöscht werden.", color = MutedMist, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = titleDraft, onValueChange = { onTitleDraftChange(it.take(80)) }, modifier = Modifier.fillMaxWidth(), label = { Text("Neue Seite") }, singleLine = true)
                Button(onClick = onCreate, enabled = titleDraft.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Add, contentDescription = null); Spacer(Modifier.width(7.dp)); Text("Seite erstellen") }
                HorizontalDivider(color = RaisedSurface)
                home.document.pages.forEach { page ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(page.title, Modifier.weight(1f), fontWeight = if (page.id == home.activePage.id) FontWeight.Bold else FontWeight.Normal)
                        TextButton(onClick = { home.activatePage(page.id); onRenameDraftChange(page.title) }) { Text("Öffnen") }
                    }
                }
                if (home.isUserPage()) {
                    HorizontalDivider(color = RaisedSurface)
                    Text("Aktive Seite", color = MutedMist, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = renameDraft,
                        onValueChange = { onRenameDraftChange(it.take(80)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Name") },
                        singleLine = true,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        OutlinedButton(onClick = onRename, enabled = renameDraft.isNotBlank(), modifier = Modifier.weight(1f)) { Text("Umbenennen") }
                        OutlinedButton(onClick = onMovePrevious, modifier = Modifier.weight(1f)) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null); Text("Vor") }
                        OutlinedButton(onClick = onMoveNext, modifier = Modifier.weight(1f)) { Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null); Text("Zurück") }
                    }
                    TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text("Aktive Seite löschen", color = Warm) }
                } else {
                    Text("Szenenseiten sind geschützt. Erstelle eine freie Seite für persönliche Inhalte.", color = MutedMist, style = MaterialTheme.typography.bodySmall)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onDismiss) { Text("Schließen") } }
            }
        }
    }
}

@Composable
private fun KALLauncherOverlays(
    controller: LauncherController,
    requestHomeRole: () -> Unit,
    requestVoiceInput: () -> Unit,
    requestDocument: () -> Unit,
    requestFileWorkspace: () -> Unit,
    requestContact: () -> Unit,
    requestWidget: () -> Unit,
    requestBackupExport: (String) -> Unit,
    requestBackupImport: () -> Unit,
    requestAuditExport: () -> Unit,
    requestInkExport: () -> Unit,
    createWidgetView: (Context, Int, cloud.kosch.aiandroid.model.WidgetSizePreset) -> View?,
    deleteWidget: (Int) -> Unit,
    forgetDocument: () -> Unit,
) {
    if (controller.drawerVisible) AppDrawerSheet(controller)
    if (controller.providerChooserVisible) ProviderChooserSheet(controller)
    if (controller.contextDetailsVisible) ContextDetailsSheet(controller.contextSnapshot, controller::useSuggestedScene, controller::hideContextDetails)
    if (controller.controlCenterVisible) ControlCenterSheet(controller, requestDocument, requestContact, requestWidget)
    if (controller.phoneVisible) PhoneSheet(controller, requestContact)
    if (controller.fileSheetVisible) FileIntelligenceSheet(controller, requestDocument, forgetDocument)
    if (controller.fileWorkspaceVisible) FileWorkspaceSheet(controller, requestFileWorkspace)
    if (controller.widgetBoardVisible) WidgetBoardSheet(controller, requestWidget, createWidgetView, deleteWidget)
    if (controller.appActionsVisible) AppActionsSheet(controller)
    if (controller.folderSheetVisible) FolderSheet(controller)
    if (controller.faqVisible) FaqSheet(controller)
    if (controller.backupVisible) BackupSheet(controller, requestBackupExport, requestBackupImport)
    if (controller.auditVisible) AuditSheet(controller, requestAuditExport)
    if (controller.onboardingVisible) OnboardingExperience(controller, requestHomeRole)
}

private fun assistantVisualLabel(state: AssistantVisualState): String = when (state) {
    AssistantVisualState.DISABLED -> "aus"
    AssistantVisualState.IDLE -> "bereit"
    AssistantVisualState.LISTENING -> "hört zu"
    AssistantVisualState.THINKING -> "prüft lokal"
    AssistantVisualState.SPEAKING -> "spricht"
    AssistantVisualState.WORKING -> "führt aus"
    AssistantVisualState.OFFLINE -> "Handoff bereit"
    AssistantVisualState.ERROR -> "Fehler"
}

private fun Mist(): Color = Color(0xFFD8F3F0)
